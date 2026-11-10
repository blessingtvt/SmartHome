package com.smarthome.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.rule.client.ActionCallException;
import com.smarthome.rule.client.ManualProtectedException;
import com.smarthome.rule.client.RuleActionClient;
import com.smarthome.rule.dto.RuleEvaluationRequest;
import com.smarthome.rule.entity.AutomationRule;
import com.smarthome.rule.entity.RuleExecutionLog;
import com.smarthome.rule.mapper.AutomationRuleMapper;
import com.smarthome.rule.mapper.RuleExecutionLogMapper;
import com.smarthome.rule.service.AutoLightService;
import com.smarthome.rule.service.RuleEvaluationService;
import com.smarthome.rule.vo.EvaluationSummaryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则评估业务实现（S2-03 + S2-04）。
 *
 * <p>S2-03 只做「评估 + 日志」；S2-04 在命中规则上追加「去重判断 + 自动动作调用」：
 * <ul>
 *   <li>命中（MATCHED）且满足触发条件时调用 S3 创建设备指令 / S4 创建告警，
 *       成功记 execution_result=SUCCESS（设备指令回填 device_command_id）；</li>
 *   <li>动作调用失败记 FAILED 并写入 failure_reason，不阻断其余规则；</li>
 *   <li>持续命中（上一非 ERROR 结果已是 MATCHED）跳过动作，记 MATCHED + NOT_EXECUTED；</li>
 *   <li>未命中记 NOT_MATCHED + NOT_EXECUTED；评估异常记 ERROR + NOT_EXECUTED。</li>
 * </ul>
 * 同一 rule_id 的「去重判断 + 动作调用 + 日志写入」串行执行（单实例内存锁），
 * 不同规则互不影响。</p>
 */
@Service
public class RuleEvaluationServiceImpl implements RuleEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluationServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String RESULT_MATCHED = "MATCHED";
    private static final String RESULT_NOT_MATCHED = "NOT_MATCHED";
    /** 条件命中但动作被手动控制保护拦截（S3 返回 409） */
    private static final String RESULT_BLOCKED = "BLOCKED";
    private static final String RESULT_ERROR = "ERROR";

    private static final String EXEC_SUCCESS = "SUCCESS";
    private static final String EXEC_FAILED = "FAILED";
    private static final String EXEC_NOT_EXECUTED = "NOT_EXECUTED";

    /** WATER 去抖规则默认连续确认次数 */
    private static final int DEFAULT_CONFIRM_COUNT = 3;

    private static final int FAILURE_REASON_MAX = 255;

    /** 按 rule_id 分片的并发保护锁（单实例，不引入分布式锁） */
    private final ConcurrentHashMap<Long, Object> ruleLocks = new ConcurrentHashMap<>();

    private final AutomationRuleMapper ruleMapper;
    private final RuleExecutionLogMapper logMapper;
    private final RuleActionClient actionClient;
    private final AutoLightService autoLightService;

    public RuleEvaluationServiceImpl(AutomationRuleMapper ruleMapper,
                                     RuleExecutionLogMapper logMapper,
                                     RuleActionClient actionClient,
                                     AutoLightService autoLightService) {
        this.ruleMapper = ruleMapper;
        this.logMapper = logMapper;
        this.actionClient = actionClient;
        this.autoLightService = autoLightService;
    }

    @Override
    public EvaluationSummaryVO evaluate(RuleEvaluationRequest request) {
        List<AutomationRule> rules = ruleMapper.selectList(
                new QueryWrapper<AutomationRule>()
                        .eq("enabled", true)
                        .orderByAsc("id"));

        int matched = 0;
        int notMatched = 0;
        int error = 0;

        for (AutomationRule rule : rules) {
            Object lock = ruleLocks.computeIfAbsent(rule.getId(), k -> new Object());
            String outcome;
            synchronized (lock) {
                outcome = processRule(rule, request);
            }
            switch (outcome) {
                case RESULT_MATCHED:
                    matched++;
                    break;
                case RESULT_NOT_MATCHED:
                    notMatched++;
                    break;
                default:
                    error++;
                    break;
            }
        }

        return new EvaluationSummaryVO(rules.size(), matched, notMatched, error);
    }

    /**
     * 处理单条规则：评估 → 去重判断 → 动作调用 → 写日志。
     * 返回评估结果（MATCHED/NOT_MATCHED/ERROR），由调用方统计。
     */
    private String processRule(AutomationRule rule, RuleEvaluationRequest request) {
        String triggerValue = triggerValueOf(rule, request);
        boolean isMatched;
        try {
            isMatched = matches(rule, request);
        } catch (Exception e) {
            try {
                writeLog(rule.getId(), RESULT_ERROR, triggerValue, EXEC_NOT_EXECUTED, null, truncate(e.getMessage()));
            } catch (Exception e2) {
                log.warn("规则 {} 评估异常且 ERROR 日志写入失败: {}", rule.getId(), e2.getMessage());
            }
            return RESULT_ERROR;
        }

        if (!isMatched) {
            writeLog(rule.getId(), RESULT_NOT_MATCHED, triggerValue, EXEC_NOT_EXECUTED, null, null);
            return RESULT_NOT_MATCHED;
        }

        // PIR 照明规则：每次命中都重置 60 秒关灯计时（独立于开灯是否成功/去重）
        String lightDeviceCode = autoLightService.lightDeviceCodeOf(rule);
        if (lightDeviceCode != null) {
            autoLightService.recordActivity(lightDeviceCode);
        }

        // 命中规则：去重判断——最近一条非 ERROR 结果已是 MATCHED 则跳过动作
        if (!shouldTrigger(rule)) {
            writeLog(rule.getId(), RESULT_MATCHED, triggerValue, EXEC_NOT_EXECUTED, null, null);
            return RESULT_MATCHED;
        }

        // PIR 照明规则：开灯去重（灯已开 / 有 PENDING 则跳过，避免覆盖手动开启状态）
        if (lightDeviceCode != null) {
            String skipReason = autoLightService.reasonToSkipTurnOn(lightDeviceCode);
            if (skipReason != null) {
                writeLog(rule.getId(), RESULT_MATCHED, triggerValue, EXEC_NOT_EXECUTED, null, truncate(skipReason));
                return RESULT_MATCHED;
            }
        }

        // 触发动作（S3 设备指令 / S4 告警）
        try {
            Long commandId = executeAction(rule, request);
            writeLog(rule.getId(), RESULT_MATCHED, triggerValue, EXEC_SUCCESS, commandId, null);
        } catch (ManualProtectedException e) {
            // 手动控制保护拦截：记 BLOCKED（非 FAILED），保护期过后可重新触发
            try {
                writeLog(rule.getId(), RESULT_BLOCKED, triggerValue, EXEC_NOT_EXECUTED, null, truncate(e.getMessage()));
            } catch (Exception e2) {
                log.warn("规则 {} 手动保护拦截且 BLOCKED 日志写入失败: {}", rule.getId(), e2.getMessage());
            }
        } catch (Exception e) {
            try {
                writeLog(rule.getId(), RESULT_MATCHED, triggerValue, EXEC_FAILED, null, truncate(e.getMessage()));
            } catch (Exception e2) {
                log.warn("规则 {} 动作失败且 FAILED 日志写入失败: {}", rule.getId(), e2.getMessage());
            }
        }
        return RESULT_MATCHED;
    }

    /**
     * 重复触发判断：查询最近一条非 ERROR 评估结果，
     * 无历史或为 NOT_MATCHED 时允许触发；为 MATCHED 时跳过（持续命中不重复动作）。
     */
    private boolean shouldTrigger(AutomationRule rule) {
        int confirmCount = confirmCountOf(rule);
        // 边沿触发（confirmCount <= 1）：最近一条非 ERROR 结果为 null/NOT_MATCHED/BLOCKED 时触发
        if (confirmCount <= 1) {
            String last = logMapper.selectLatestNonErrorResult(rule.getId());
            // NOT_MATCHED：新上升沿，允许触发；BLOCKED：上轮被手动保护拦截，允许重试（保护期过后可成功）
            return last == null || RESULT_NOT_MATCHED.equals(last) || RESULT_BLOCKED.equals(last);
        }

        // 去抖触发（confirmCount >= 2，WATER 关窗）：连续 confirmCount 次命中才触发，触发后持续命中不重复
        List<RuleExecutionLog> recent = logMapper.selectRecentNonError(rule.getId(), confirmCount);
        if (!recent.isEmpty()) {
            RuleExecutionLog latest = recent.get(0);
            // 最近一次已命中且已执行（SUCCESS/FAILED）：持续命中不重复触发
            if (RESULT_MATCHED.equals(latest.getEvaluationResult())
                    && !EXEC_NOT_EXECUTED.equals(latest.getExecutionResult())) {
                return false;
            }
        }
        int consecutive = 0;
        for (RuleExecutionLog r : recent) {
            if (RESULT_MATCHED.equals(r.getEvaluationResult())) {
                consecutive++;
            } else {
                break;
            }
        }
        // 当前这次已命中（调用方保证），故累计 +1 达到 confirmCount 即触发
        return (consecutive + 1) >= confirmCount;
    }

    /** WATER 规则的去抖次数（连续命中 confirmCount 次才触发），其余规则边沿触发返回 1 */
    private int confirmCountOf(AutomationRule rule) {
        if (!"WATER".equals(rule.getTriggerType())) {
            return 1;
        }
        try {
            JsonNode condition = parseCondition(rule.getConditionConfig());
            JsonNode cc = condition.get("confirmCount");
            if (cc != null && !cc.isNull() && cc.isIntegralNumber()) {
                int n = cc.asInt();
                if (n >= 1) {
                    return n;
                }
            }
        } catch (Exception e) {
            log.warn("规则 {} confirmCount 解析失败，使用默认 {}: {}", rule.getId(), DEFAULT_CONFIRM_COUNT, e.getMessage());
        }
        return DEFAULT_CONFIRM_COUNT;
    }

    /**
     * 解析 action_config 并调用对应动作。
     * <ul>
     *   <li>DEVICE_COMMAND：读取 deviceCode + action，调用 S3，返回指令 ID；</li>
     *   <li>ALARM：读取 content，deviceCode 取 RuleEvaluationRequest.deviceCode，调用 S4。</li>
     * </ul>
     * 解析失败抛 {@link IllegalStateException}，调用失败抛 {@link ActionCallException}。
     */
    private Long executeAction(AutomationRule rule, RuleEvaluationRequest request) {
        JsonNode action = parseActionConfig(rule.getActionConfig());
        String actionType = requireText(action, "actionType");
        switch (actionType) {
            case "DEVICE_COMMAND":
                String deviceCode = requireText(action, "deviceCode");
                String actionName = requireText(action, "action");
                return actionClient.createCommand(deviceCode, actionName);
            case "ALARM":
                String content = requireText(action, "content");
                actionClient.createAlarm(request.getDeviceCode(), content);
                return null;
            default:
                throw new IllegalStateException("未知 actionType: " + actionType);
        }
    }

    /**
     * 判断单条规则是否命中。condition_config 解析失败、字段缺失、类型不符、
     * 运算符不支持等一律抛异常，由调用方捕获并记录为 ERROR。
     */
    private boolean matches(AutomationRule rule, RuleEvaluationRequest request) {
        JsonNode condition = parseCondition(rule.getConditionConfig());
        String operator = requireText(condition, "operator");
        JsonNode threshold = condition.get("threshold");

        switch (rule.getTriggerType()) {
            case "TEMPERATURE":
                return compareDecimal(request.getTemperature(), operator, threshold);
            case "HUMIDITY":
                return compareDecimal(request.getHumidity(), operator, threshold);
            case "GAS_RAW":
                BigDecimal gas = request.getGasRaw() == null
                        ? null : BigDecimal.valueOf(request.getGasRaw());
                return compareDecimal(gas, operator, threshold);
            case "WATER":
                BigDecimal water = request.getWater() == null
                        ? null : BigDecimal.valueOf(request.getWater());
                return compareDecimal(water, operator, threshold);
            case "PIR":
                return compareBoolean(request.getPirTriggered(), operator, threshold);
            default:
                throw new IllegalStateException("未知 triggerType: " + rule.getTriggerType());
        }
    }

    /** 取该规则对应指标的实际值，用于写入 trigger_value（缺失/未知类型返回 null） */
    private String triggerValueOf(AutomationRule rule, RuleEvaluationRequest request) {
        switch (rule.getTriggerType()) {
            case "TEMPERATURE":
                return request.getTemperature() == null
                        ? null : request.getTemperature().toPlainString();
            case "HUMIDITY":
                return request.getHumidity() == null
                        ? null : request.getHumidity().toPlainString();
            case "GAS_RAW":
                return request.getGasRaw() == null
                        ? null : String.valueOf(request.getGasRaw());
            case "WATER":
                return request.getWater() == null
                        ? null : String.valueOf(request.getWater());
            case "PIR":
                return request.getPirTriggered() == null
                        ? null : String.valueOf(request.getPirTriggered());
            default:
                return null;
        }
    }

    /** 数值比较：使用 BigDecimal，缺失数据返回 false（视为未命中），运算符非法抛异常 */
    private boolean compareDecimal(BigDecimal value, String operator, JsonNode thresholdNode) {
        if (value == null) {
            return false;
        }
        if (thresholdNode == null || !thresholdNode.isNumber()) {
            throw new IllegalStateException("threshold 缺失或非数值");
        }
        BigDecimal threshold = thresholdNode.decimalValue();
        int cmp = value.compareTo(threshold);
        switch (operator) {
            case ">":
                return cmp > 0;
            case "<":
                return cmp < 0;
            case ">=":
                return cmp >= 0;
            case "<=":
                return cmp <= 0;
            case "==":
                return cmp == 0;
            default:
                throw new IllegalStateException("不支持的数值运算符: " + operator);
        }
    }

    /** 布尔比较：缺失数据返回 false（视为未命中），运算符非法抛异常 */
    private boolean compareBoolean(Boolean value, String operator, JsonNode thresholdNode) {
        if (value == null) {
            return false;
        }
        if (thresholdNode == null || !thresholdNode.isBoolean()) {
            throw new IllegalStateException("threshold 缺失或非布尔");
        }
        boolean threshold = thresholdNode.asBoolean();
        switch (operator) {
            case "==":
                return value == threshold;
            case "!=":
                return value != threshold;
            default:
                throw new IllegalStateException("PIR 不支持的运算符: " + operator);
        }
    }

    private JsonNode parseCondition(String conditionConfig) {
        if (conditionConfig == null || conditionConfig.isBlank()) {
            throw new IllegalStateException("condition_config 为空");
        }
        try {
            JsonNode node = JSON.readTree(conditionConfig);
            if (node == null || !node.isObject()) {
                throw new IllegalStateException("condition_config 不是 JSON 对象");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("condition_config 解析失败", e);
        }
    }

    private JsonNode parseActionConfig(String actionConfig) {
        if (actionConfig == null || actionConfig.isBlank()) {
            throw new IllegalStateException("action_config 为空");
        }
        try {
            JsonNode node = JSON.readTree(actionConfig);
            if (node == null || !node.isObject()) {
                throw new IllegalStateException("action_config 不是 JSON 对象");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("action_config 解析失败", e);
        }
    }

    private String requireText(JsonNode obj, String key) {
        JsonNode node = obj.get(key);
        if (node == null || !node.isTextual()) {
            throw new IllegalStateException(key + " 缺失或非字符串");
        }
        return node.asText();
    }

    private void writeLog(Long ruleId, String evaluationResult, String triggerValue,
                          String executionResult, Long deviceCommandId, String failureReason) {
        RuleExecutionLog record = new RuleExecutionLog();
        record.setRuleId(ruleId);
        record.setTriggerValue(triggerValue);
        record.setEvaluationResult(evaluationResult);
        record.setExecutionResult(executionResult);
        record.setDeviceCommandId(deviceCommandId);
        record.setFailureReason(failureReason);
        record.setExecutionTime(TimeUtils.now());
        logMapper.insert(record);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > FAILURE_REASON_MAX
                ? message.substring(0, FAILURE_REASON_MAX) : message;
    }
}
