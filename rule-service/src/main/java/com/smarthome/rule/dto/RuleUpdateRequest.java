package com.smarthome.rule.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改自动化规则请求（S2-02）。
 *
 * <p>PUT 语义：全量提交，所有可修改字段重新完整校验；enabled 必须显式指定。</p>
 */
public class RuleUpdateRequest {

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

    /** 是否启用 */
    @NotNull(message = "是否启用不能为空")
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
