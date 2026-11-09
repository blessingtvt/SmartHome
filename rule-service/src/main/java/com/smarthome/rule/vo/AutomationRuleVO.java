package com.smarthome.rule.vo;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 自动化规则对外视图（S2-02）。
 *
 * <p>conditionConfig / actionConfig 以 JsonNode 形式返回 JSON 对象；
 * createTime 为 epoch 毫秒。</p>
 */
public class AutomationRuleVO {

    private Long id;

    private String ruleName;

    private String triggerType;

    /** 触发条件配置（JSON 对象） */
    private JsonNode conditionConfig;

    /** 执行动作配置（JSON 对象） */
    private JsonNode actionConfig;

    private Boolean enabled;

    /** 创建时间（epoch 毫秒） */
    private Long createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
