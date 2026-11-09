package com.smarthome.rule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smarthome.rule.dto.RuleCreateRequest;
import com.smarthome.rule.dto.RuleEnabledRequest;
import com.smarthome.rule.dto.RuleUpdateRequest;
import com.smarthome.rule.vo.AutomationRuleVO;

/**
 * 自动化规则管理接口（S2-02）。
 */
public interface RuleService {

    /**
     * 创建规则。
     */
    AutomationRuleVO create(RuleCreateRequest request);

    /**
     * 分页查询规则。
     *
     * @param ruleName    规则名称（模糊匹配，可空）
     * @param triggerType 触发类型（精确匹配，可空）
     * @param enabled     是否启用（精确匹配，可空）
     */
    IPage<AutomationRuleVO> page(String ruleName, String triggerType, Boolean enabled, long pageNum, long pageSize);

    /**
     * 查询规则详情。
     */
    AutomationRuleVO getById(Long id);

    /**
     * 修改规则（全量提交，重新完整校验）。
     */
    AutomationRuleVO update(Long id, RuleUpdateRequest request);

    /**
     * 删除规则（不级联清理历史 rule_execution_log）。
     */
    void delete(Long id);

    /**
     * 启用/禁用规则（仅修改 enabled）。
     */
    AutomationRuleVO updateEnabled(Long id, RuleEnabledRequest request);
}
