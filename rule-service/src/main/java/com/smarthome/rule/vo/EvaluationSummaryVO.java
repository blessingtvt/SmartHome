package com.smarthome.rule.vo;

/**
 * 规则评估结果汇总（S2-03）。
 *
 * <p>evaluated = matched + notMatched + error，等于本次参与的启用规则总数。</p>
 */
public class EvaluationSummaryVO {

    /** 参与评估的启用规则总数 */
    private int evaluated;

    /** 匹配的规则数 */
    private int matched;

    /** 未匹配的规则数 */
    private int notMatched;

    /** 评估异常的规则数 */
    private int error;

    public EvaluationSummaryVO() {
    }

    public EvaluationSummaryVO(int evaluated, int matched, int notMatched, int error) {
        this.evaluated = evaluated;
        this.matched = matched;
        this.notMatched = notMatched;
        this.error = error;
    }

    public int getEvaluated() {
        return evaluated;
    }

    public void setEvaluated(int evaluated) {
        this.evaluated = evaluated;
    }

    public int getMatched() {
        return matched;
    }

    public void setMatched(int matched) {
        this.matched = matched;
    }

    public int getNotMatched() {
        return notMatched;
    }

    public void setNotMatched(int notMatched) {
        this.notMatched = notMatched;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }
}
