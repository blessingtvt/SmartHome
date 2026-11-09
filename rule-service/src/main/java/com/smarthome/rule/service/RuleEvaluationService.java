package com.smarthome.rule.service;

import com.smarthome.rule.dto.RuleEvaluationRequest;
import com.smarthome.rule.vo.EvaluationSummaryVO;

/**
 * 规则评估接口（S2-03）。
 */
public interface RuleEvaluationService {

    /**
     * 对一次环境上报数据执行规则评估。
     *
     * <p>查询所有 enabled=true 的规则，按 triggerType 匹配对应环境数据，
     * 逐条评估并将结果（MATCHED/NOT_MATCHED/ERROR）写入 rule_execution_log。
     * 单条规则评估异常不阻断其余规则。</p>
     *
     * @return 评估汇总（参与数/匹配数/未匹配数/异常数）
     */
    EvaluationSummaryVO evaluate(RuleEvaluationRequest request);
}
