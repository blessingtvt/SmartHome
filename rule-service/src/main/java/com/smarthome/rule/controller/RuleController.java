package com.smarthome.rule.controller;

import com.smarthome.common.result.Result;
import com.smarthome.rule.dto.RuleCreateRequest;
import com.smarthome.rule.dto.RuleEnabledRequest;
import com.smarthome.rule.dto.RuleUpdateRequest;
import com.smarthome.rule.service.RuleService;
import com.smarthome.rule.vo.AutomationRuleVO;
import com.smarthome.rule.vo.PageVO;
import com.smarthome.rule.vo.RuleExecutionLogVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S2 自动化规则管理接口（/api/v1/rules）。
 */
@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /** 创建规则 */
    @PostMapping
    public Result<AutomationRuleVO> create(@Valid @RequestBody RuleCreateRequest request) {
        return Result.success("创建成功", ruleService.create(request));
    }

    /** 分页查询规则 */
    @GetMapping
    public Result<PageVO<AutomationRuleVO>> page(
            @RequestParam(value = "ruleName", required = false) String ruleName,
            @RequestParam(value = "triggerType", required = false) String triggerType,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(PageVO.of(ruleService.page(ruleName, triggerType, enabled, pageNum, pageSize)));
    }

    /** 查询规则详情 */
    @GetMapping("/{id}")
    public Result<AutomationRuleVO> getById(@PathVariable("id") Long id) {
        return Result.success(ruleService.getById(id));
    }

    /** 执行日志（只读分页，阶段五）。ruleId 可空，为空查询全部规则日志。 */
    @GetMapping("/execution-logs")
    public Result<PageVO<RuleExecutionLogVO>> logs(
            @RequestParam(value = "ruleId", required = false) Long ruleId,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        return Result.success(PageVO.of(ruleService.pageLogs(ruleId, pageNum, pageSize)));
    }

    /** 修改规则 */
    @PutMapping("/{id}")
    public Result<AutomationRuleVO> update(@PathVariable("id") Long id,
                                           @Valid @RequestBody RuleUpdateRequest request) {
        return Result.success("修改成功", ruleService.update(id, request));
    }

    /** 删除规则 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        ruleService.delete(id);
        return Result.success();
    }

    /** 启用/禁用规则 */
    @PatchMapping("/{id}/enabled")
    public Result<AutomationRuleVO> updateEnabled(@PathVariable("id") Long id,
                                                  @Valid @RequestBody RuleEnabledRequest request) {
        return Result.success("状态更新成功", ruleService.updateEnabled(id, request));
    }
}
