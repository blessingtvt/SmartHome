package com.smarthome.rule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Set;

/**
 * 规则 JSON 配置校验器（S2-02）。
 *
 * <p>conditionConfig / actionConfig 均以 JsonNode 语义化校验，不依赖字符串格式
 * （MySQL JSON 列可能重排键序、规范空白）。所有校验失败统一抛 {@link BusinessException}(400)。</p>
 */
@Component
public class RuleConfigValidator {

    /** 第一版支持的触发类型 */
    private static final Set<String> VALID_TRIGGER_TYPES =
            Set.of("TEMPERATURE", "HUMIDITY", "GAS_RAW", "PIR");

    /** 数值型指标支持的比较运算符 */
    private static final Set<String> NUMERIC_OPERATORS = Set.of(">", "<", ">=", "<=", "==");

    /** Boolean 型指标（PIR）支持的比较运算符 */
    private static final Set<String> BOOLEAN_OPERATORS = Set.of("==", "!=");

    /** DEVICE_COMMAND 允许的动作 */
    private static final Set<String> DEVICE_COMMAND_ACTIONS = Set.of("TURN_ON", "TURN_OFF");

    /** 校验触发类型枚举 */
    public void validateTriggerType(String triggerType) {
        if (triggerType == null || !VALID_TRIGGER_TYPES.contains(triggerType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "triggerType 非法，仅支持 TEMPERATURE/HUMIDITY/GAS_RAW/PIR");
        }
    }

    /** 校验 conditionConfig 与 triggerType 匹配且结构、取值合法 */
    public void validateConditionConfig(String triggerType, JsonNode config) {
        requireObject(config, "conditionConfig");
        rejectUnknownFields(config, Set.of("metric", "operator", "threshold"), "conditionConfig");

        String metric = requireText(config, "metric", "conditionConfig").asText();
        String operator = requireText(config, "operator", "conditionConfig").asText();
        requirePresent(config, "threshold", "conditionConfig");
        JsonNode threshold = config.get("threshold");

        String expectedMetric = expectedMetric(triggerType);
        if (!expectedMetric.equals(metric)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "triggerType " + triggerType + " 与 metric " + metric + " 不匹配（应为 " + expectedMetric + "）");
        }

        validateOperator(metric, operator);
        validateThreshold(metric, threshold);
    }

    /** 校验 actionConfig 结构、actionType、必需字段与取值范围 */
    public void validateActionConfig(JsonNode config) {
        requireObject(config, "actionConfig");
        String actionType = requireText(config, "actionType", "actionConfig").asText();

        if ("DEVICE_COMMAND".equals(actionType)) {
            rejectUnknownFields(config, Set.of("actionType", "deviceCode", "action"), "actionConfig");
            String deviceCode = requireText(config, "deviceCode", "actionConfig").asText();
            if (deviceCode.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "actionConfig.deviceCode 不能为空");
            }
            String action = requireText(config, "action", "actionConfig").asText();
            if (!DEVICE_COMMAND_ACTIONS.contains(action)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "action 非法，仅支持 TURN_ON/TURN_OFF（不支持 SET_LEVEL）");
            }
        } else if ("ALARM".equals(actionType)) {
            rejectUnknownFields(config, Set.of("actionType", "content"), "actionConfig");
            String content = requireText(config, "content", "actionConfig").asText();
            if (content.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "actionConfig.content 不能为空");
            }
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "actionType 非法，仅支持 DEVICE_COMMAND/ALARM");
        }
    }

    private String expectedMetric(String triggerType) {
        switch (triggerType) {
            case "TEMPERATURE":
                return "temperature";
            case "HUMIDITY":
                return "humidity";
            case "GAS_RAW":
                return "gasRaw";
            case "PIR":
                return "pirTriggered";
            default:
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "triggerType 非法: " + triggerType);
        }
    }

    private void validateOperator(String metric, String operator) {
        if ("pirTriggered".equals(metric)) {
            if (!BOOLEAN_OPERATORS.contains(operator)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "PIR 指标 operator 仅支持 ==/!=");
            }
        } else {
            if (!NUMERIC_OPERATORS.contains(operator)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "数值型指标 operator 仅支持 >、<、>=、<=、==");
            }
        }
    }

    private void validateThreshold(String metric, JsonNode threshold) {
        switch (metric) {
            case "temperature":
                requireNumber(threshold, "temperature");
                double t = threshold.asDouble();
                if (t < -40 || t > 80) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "temperature threshold 必须在 -40~80 之间");
                }
                break;
            case "humidity":
                requireNumber(threshold, "humidity");
                double h = threshold.asDouble();
                if (h < 0 || h > 100) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "humidity threshold 必须在 0~100 之间");
                }
                break;
            case "gasRaw":
                if (!threshold.isIntegralNumber() || !threshold.canConvertToLong() || threshold.asLong() < 0) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "gasRaw threshold 必须是非负整数");
                }
                break;
            case "pirTriggered":
                if (!threshold.isBoolean()) {
                    throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                            "pirTriggered threshold 必须是 Boolean");
                }
                break;
            default:
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "metric 非法: " + metric);
        }
    }

    private void requireNumber(JsonNode threshold, String metric) {
        if (threshold == null || !threshold.isNumber()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    metric + " threshold 必须是数值");
        }
    }

    private void requireObject(JsonNode config, String fieldName) {
        if (config == null || !config.isObject()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), fieldName + " 必须是 JSON 对象");
        }
    }

    private JsonNode requireText(JsonNode obj, String key, String fieldName) {
        JsonNode node = obj.get(key);
        if (node == null || node.isNull() || !node.isTextual()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    fieldName + "." + key + " 缺失或不是字符串");
        }
        return node;
    }

    private void requirePresent(JsonNode obj, String key, String fieldName) {
        JsonNode node = obj.get(key);
        if (node == null || node.isNull()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), fieldName + "." + key + " 缺失");
        }
    }

    private void rejectUnknownFields(JsonNode obj, Set<String> allowed, String fieldName) {
        Iterator<String> names = obj.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed.contains(name)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        fieldName + " 包含未知字段: " + name);
            }
        }
    }
}
