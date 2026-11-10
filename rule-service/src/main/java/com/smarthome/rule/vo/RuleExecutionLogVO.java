package com.smarthome.rule.vo;

/**
 * 规则执行记录对外视图（S2，阶段五只读日志接口）。
 *
 * <p>用于前端「执行日志」展示：区分「规则匹配结果」「命令创建结果」「设备实际执行结果」三层语义：
 * <ul>
 *   <li>evaluationResult：规则匹配结果（MATCHED / NOT_MATCHED / BLOCKED / ERROR）；</li>
 *   <li>executionResult：动作提交/命令创建结果（SUCCESS=已创建指令或告警 / FAILED=提交失败 / NOT_EXECUTED=未执行）；</li>
 *   <li>deviceExecResult：设备实际执行结果（SUCCESS / FAILED，PENDING 不回填故可能为 null）。</li>
 * </ul>
 * executionTime 为 epoch 毫秒；ruleName 来自 automation_rule 联表，便于前端直接展示规则名。</p>
 */
public class RuleExecutionLogVO {

    private Long id;

    private Long ruleId;

    /** 规则名称（联表 automation_rule.rule_name） */
    private String ruleName;

    /** 触发时数据值 */
    private String triggerValue;

    /** 规则匹配结果 */
    private String evaluationResult;

    /** 命令创建结果 */
    private String executionResult;

    /** 关联设备指令 ID（逻辑引用 S3.device_command.id） */
    private Long deviceCommandId;

    /** 设备实际执行结果 */
    private String deviceExecResult;

    /** 失败原因 */
    private String failureReason;

    /** 执行时间（epoch 毫秒） */
    private Long executionTime;

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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public String getDeviceExecResult() {
        return deviceExecResult;
    }

    public void setDeviceExecResult(String deviceExecResult) {
        this.deviceExecResult = deviceExecResult;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
}
