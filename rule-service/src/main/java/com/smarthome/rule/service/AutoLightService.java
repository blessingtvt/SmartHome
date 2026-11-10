package com.smarthome.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.rule.client.ActionCallException;
import com.smarthome.rule.client.DeviceStateResult;
import com.smarthome.rule.client.ManualProtectedException;
import com.smarthome.rule.client.RuleActionClient;
import com.smarthome.rule.entity.AutoLightState;
import com.smarthome.rule.entity.AutomationRule;
import com.smarthome.rule.entity.RuleExecutionLog;
import com.smarthome.rule.mapper.AutoLightStateMapper;
import com.smarthome.rule.mapper.AutomationRuleMapper;
import com.smarthome.rule.mapper.RuleExecutionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动照明业务（阶段四 pir_light，归属 S2）。
 *
 * <p>职责：PIR 活动时间持久化（auto_light_state）、开灯去重、60 秒无活动自动关灯判断。
 * 定时任务由 {@code AutoLightOffScanner} 每 10 秒触发 {@link #scanAndTurnOff()}。</p>
 *
 * <p>关键边界：<ul>
 *   <li>绝不自动关闭家属手动开启的灯：依据 S3 最近成功指令来源（onSource）判断，而非猜测历史；</li>
 *   <li>命令创建成功 ≠ 设备执行成功：device_exec_result 由 CommandResultRefresher 回填；</li>
 *   <li>状态持久化在 auto_light_state 表，S2 重启不丢活动时间。</li>
 * </ul></p>
 */
@Service
public class AutoLightService {

    private static final Logger log = LoggerFactory.getLogger(AutoLightService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String EVAL_MATCHED = "MATCHED";
    private static final String EVAL_NOT_MATCHED = "NOT_MATCHED";
    private static final String EVAL_BLOCKED = "BLOCKED";

    private static final String EXEC_SUCCESS = "SUCCESS";
    private static final String EXEC_FAILED = "FAILED";
    private static final String EXEC_NOT_EXECUTED = "NOT_EXECUTED";

    private static final String SOURCE_AUTO_RULE = "AUTO_RULE";
    private static final String ACTION_TURN_OFF = "TURN_OFF";

    private static final int FAILURE_REASON_MAX = 255;

    private final AutomationRuleMapper ruleMapper;
    private final AutoLightStateMapper stateMapper;
    private final RuleExecutionLogMapper logMapper;
    private final RuleActionClient actionClient;

    /** 无活动自动关灯延时（秒），默认 60 */
    private final long offDelaySeconds;

    public AutoLightService(AutomationRuleMapper ruleMapper,
                            AutoLightStateMapper stateMapper,
                            RuleExecutionLogMapper logMapper,
                            RuleActionClient actionClient,
                            @Value("${smart-home.auto-light.off-delay-seconds:60}") long offDelaySeconds) {
        this.ruleMapper = ruleMapper;
        this.stateMapper = stateMapper;
        this.logMapper = logMapper;
        this.actionClient = actionClient;
        this.offDelaySeconds = offDelaySeconds;
    }

    /**
     * 记录一次 PIR 活动：将指定照明设备的最近活动时间更新为当前时间（upsert）。
     * 每次新活动都重置 60 秒关灯计时，且独立于开灯命令是否成功。
     */
    public void recordActivity(String deviceCode) {
        LocalDateTime now = TimeUtils.now();
        AutoLightState existing = stateMapper.selectOne(new QueryWrapper<AutoLightState>()
                .eq("device_code", deviceCode));
        if (existing == null) {
            AutoLightState state = new AutoLightState();
            state.setDeviceCode(deviceCode);
            state.setLastActivityTime(now);
            state.setUpdateTime(now);
            stateMapper.insert(state);
        } else {
            existing.setLastActivityTime(now);
            existing.setUpdateTime(now);
            stateMapper.updateById(existing);
        }
    }

    /**
     * 判断 PIR 命中后是否应跳过开灯动作。返回非 null 的跳过原因（灯已开 / 有 PENDING），
     * 返回 null 表示可以创建 TURN_ON。S3 状态查询失败时返回 null，交由后续 createCommand
     * 统一处理（失败记 FAILED）。
     */
    public String reasonToSkipTurnOn(String deviceCode) {
        DeviceStateResult state;
        try {
            state = actionClient.getDeviceState(deviceCode);
        } catch (ActionCallException e) {
            log.warn("[自动照明] 设备 {} 状态查询失败，交由创建指令流程处理: {}", deviceCode, e.getMessage());
            return null;
        }
        if (Boolean.TRUE.equals(state.getOn())) {
            return "灯已开启，跳过重复开灯";
        }
        if (Boolean.TRUE.equals(state.getPending())) {
            return "存在待执行指令，跳过重复开灯";
        }
        return null;
    }

    /**
     * 判断规则是否为「PIR 触发 + 设备控制动作」的自动照明规则，并返回其照明设备编码；
     * 非照明规则（如 PIR 告警）或配置非法时返回 null。
     */
    public String lightDeviceCodeOf(AutomationRule rule) {
        if (!"PIR".equals(rule.getTriggerType())) {
            return null;
        }
        try {
            JsonNode action = JSON.readTree(rule.getActionConfig());
            if (action == null || !action.isObject()) {
                return null;
            }
            JsonNode type = action.get("actionType");
            if (type == null || !type.isTextual() || !"DEVICE_COMMAND".equals(type.asText())) {
                return null;
            }
            JsonNode code = action.get("deviceCode");
            if (code == null || !code.isTextual() || code.asText().isBlank()) {
                return null;
            }
            return code.asText();
        } catch (JsonProcessingException e) {
            log.warn("[自动照明] 规则 {} action_config 解析失败: {}", rule.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 定时关灯扫描：遍历启用的 PIR 照明规则，对「60 秒无活动」的设备执行关灯判断。
     * 由 AutoLightOffScanner 每 10 秒调用一次。
     */
    public void scanAndTurnOff() {
        List<AutomationRule> rules = ruleMapper.selectList(new QueryWrapper<AutomationRule>()
                .eq("enabled", true)
                .eq("trigger_type", "PIR")
                .orderByAsc("id"));
        if (rules.isEmpty()) {
            return;
        }
        LocalDateTime now = TimeUtils.now();
        for (AutomationRule rule : rules) {
            String deviceCode = lightDeviceCodeOf(rule);
            if (deviceCode == null) {
                continue;
            }
            AutoLightState state = stateMapper.selectOne(new QueryWrapper<AutoLightState>()
                    .eq("device_code", deviceCode));
            if (state == null || state.getLastActivityTime() == null) {
                continue; // 无活动记录，无需关灯
            }
            long elapsed = Duration.between(state.getLastActivityTime(), now).getSeconds();
            if (elapsed < offDelaySeconds) {
                log.info("[自动关灯] 设备 {} 距上次活动 {} 秒，未到 {} 秒，继续等待", deviceCode, elapsed, offDelaySeconds);
                continue;
            }
            processExpiredLight(rule.getId(), deviceCode);
        }
    }

    /** 对已满足「60 秒无活动」的设备执行关灯判断 */
    private void processExpiredLight(Long ruleId, String deviceCode) {
        DeviceStateResult state;
        try {
            state = actionClient.getDeviceState(deviceCode);
        } catch (ActionCallException e) {
            log.warn("[自动关灯] 设备 {} 状态查询失败，下轮重试: {}", deviceCode, e.getMessage());
            return;
        }

        // 1) 有待执行指令：跳过（等其完成，避免重复/冲突命令）
        if (Boolean.TRUE.equals(state.getPending())) {
            writeOffLog(ruleId, EVAL_NOT_MATCHED, EXEC_NOT_EXECUTED, null, "存在待执行指令，跳过自动关灯");
            return;
        }
        // 2) 灯已关闭：无需再关，清空活动时间停止后续检查
        if (Boolean.FALSE.equals(state.getOn())) {
            writeOffLog(ruleId, EVAL_NOT_MATCHED, EXEC_NOT_EXECUTED, null, "灯已关闭，无需再关");
            clearActivity(deviceCode);
            return;
        }
        // 3) 灯为家属手动开启：绝不自动关闭，清空活动时间停止后续检查
        if (!SOURCE_AUTO_RULE.equals(state.getOnSource())) {
            writeOffLog(ruleId, EVAL_NOT_MATCHED, EXEC_NOT_EXECUTED, null, "灯为家属手动开启，跳过自动关灯");
            clearActivity(deviceCode);
            return;
        }
        // 4) 灯为自动开启且无待执行指令：通过 S3 创建 TURN_OFF（source=AUTO_RULE）
        try {
            Long commandId = actionClient.createCommand(deviceCode, ACTION_TURN_OFF);
            writeOffLog(ruleId, EVAL_MATCHED, EXEC_SUCCESS, commandId, null);
        } catch (ManualProtectedException e) {
            writeOffLog(ruleId, EVAL_BLOCKED, EXEC_NOT_EXECUTED, null, truncate(e.getMessage()));
        } catch (ActionCallException e) {
            writeOffLog(ruleId, EVAL_MATCHED, EXEC_FAILED, null, truncate(e.getMessage()));
        }
    }

    /** 清空活动时间（灯已关/手动开启后停止继续检查，直到下一次 PIR 活动）。用 UpdateWrapper 显式置 null（updateById 默认忽略 null 字段）。 */
    private void clearActivity(String deviceCode) {
        stateMapper.update(null, new UpdateWrapper<AutoLightState>()
                .set("last_activity_time", null)
                .set("update_time", TimeUtils.now())
                .eq("device_code", deviceCode));
    }

    private void writeOffLog(Long ruleId, String evaluationResult, String executionResult,
                             Long deviceCommandId, String failureReason) {
        RuleExecutionLog record = new RuleExecutionLog();
        record.setRuleId(ruleId);
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
