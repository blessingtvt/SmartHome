package com.smarthome.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 自动化规则表 automation_rule 对应实体（归属 S2）。
 * 字段名与数据库 snake_case 自动映射（map-underscore-to-camel-case）。
 *
 * <p>condition_config / action_config 为 MySQL JSON 列，第一版用 String 直接收发 JSON 文本，
 * 不做复杂 JSON 转换器；JSON 内容格式将在 S2-02 统一校验。</p>
 */
@TableName("automation_rule")
public class AutomationRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 触发类型 */
    private String triggerType;

    /** 触发条件配置（JSON 文本） */
    private String conditionConfig;

    /** 执行动作配置（JSON 文本） */
    private String actionConfig;

    /** 是否启用（1/0） */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

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

    public String getConditionConfig() {
        return conditionConfig;
    }

    public void setConditionConfig(String conditionConfig) {
        this.conditionConfig = conditionConfig;
    }

    public String getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(String actionConfig) {
        this.actionConfig = actionConfig;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
