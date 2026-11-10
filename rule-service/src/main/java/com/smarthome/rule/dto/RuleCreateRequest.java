package com.smarthome.rule.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建自动化规则请求（S2-02）。
 *
 * <p>conditionConfig / actionConfig 用 JsonNode 接收 JSON 对象，
 * 由 Service 层完成校验后再序列化（toString）存入数据库；enabled 未传时默认 true。</p>
 */
public class RuleCreateRequest {

    /** 规则名称 */
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100")
    private String ruleName;

    /** 触发类型（TEMPERATURE/HUMIDITY/GAS_RAW/PIR/WATER） */
    @NotBlank(message = "触发类型不能为空")
    private String triggerType;

    /** 触发条件配置（JSON 对象） */
    private JsonNode conditionConfig;

    /** 执行动作配置（JSON 对象） */
    private JsonNode actionConfig;

    /** 是否启用（未传默认 true） */
    private Boolean enabled;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public JsonNode getConditionConfig() {
        return conditionConfig;
    }

    public void setConditionConfig(JsonNode conditionConfig) {
        this.conditionConfig = conditionConfig;
    }

    public JsonNode getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(JsonNode actionConfig) {
        this.actionConfig = actionConfig;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
