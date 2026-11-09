package com.smarthome.rule.controller;

import com.smarthome.common.result.Result;
import com.smarthome.rule.dto.RuleEvaluationRequest;
import com.smarthome.rule.service.RuleEvaluationService;
import com.smarthome.rule.vo.EvaluationSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S2 内部规则评估接口（/internal/rules）。
 *
 * <p>供 S1 上报环境数据后调用，对启用规则进行评估并写入执行日志。</p>
 */
@RestController
@RequestMapping("/internal/rules")
public class InternalRuleController {

    private final RuleEvaluationService ruleEvaluationService;

    public InternalRuleController(RuleEvaluationService ruleEvaluationService) {
        this.ruleEvaluationService = ruleEvaluationService;
    }

    /** 规则评估 */
    @PostMapping("/evaluate")
    public Result<EvaluationSummaryVO> evaluate(@Valid @RequestBody RuleEvaluationRequest request) {
        return Result.success("评估完成", ruleEvaluationService.evaluate(request));
    }
}
