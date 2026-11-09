package com.smarthome.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 规则执行记录表 rule_execution_log 对应实体（归属 S2）。
 *
 * <p>device_command_id 为逻辑引用 S3.device_command.id，不建跨服务外键。</p>
 */
@TableName("rule_execution_log")
public class RuleExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对应规则编号 */
    private Long ruleId;

    /** 触发时数据值 */
    private String triggerValue;

    /** 规则评估结果（MATCHED / NOT_MATCHED / ERROR） */
    private String evaluationResult;

    /** 动作提交结果 */
    private String executionResult;

    /** 关联的设备指令 ID（逻辑引用 S3.device_command.id） */
    private Long deviceCommandId;

    /** 规则评估或动作提交失败原因 */
    private String failureReason;

    /** 执行时间 */
    private LocalDateTime executionTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    public String getEvaluationResult() {
        return evaluationResult;
    }

    public void setEvaluationResult(String evaluationResult) {
        this.evaluationResult = evaluationResult;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public Long getDeviceCommandId() {
        return deviceCommandId;
    }

    public void setDeviceCommandId(Long deviceCommandId) {
        this.deviceCommandId = deviceCommandId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }
}
