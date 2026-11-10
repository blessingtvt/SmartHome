package com.smarthome.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.rule.dto.RuleCreateRequest;
import com.smarthome.rule.dto.RuleEnabledRequest;
import com.smarthome.rule.dto.RuleUpdateRequest;
import com.smarthome.rule.entity.AutomationRule;
import com.smarthome.rule.mapper.AutomationRuleMapper;
import com.smarthome.rule.mapper.RuleExecutionLogMapper;
import com.smarthome.rule.service.RuleConfigValidator;
import com.smarthome.rule.service.RuleService;
import com.smarthome.rule.vo.AutomationRuleVO;
import com.smarthome.rule.vo.RuleExecutionLogVO;
import org.springframework.stereotype.Service;

/**
 * 自动化规则业务逻辑实现（S2-02）。
 *
 * <p>JSON 配置由 {@link RuleConfigValidator} 校验后，以紧凑 JSON 文本存入数据库；
 * 读取时再解析回 JsonNode 对外返回。本阶段不实现规则评估、指令/告警创建。</p>
 */
@Service
public class RuleServiceImpl implements RuleService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final AutomationRuleMapper ruleMapper;
    private final RuleConfigValidator ruleConfigValidator;
    private final RuleExecutionLogMapper logMapper;

    public RuleServiceImpl(AutomationRuleMapper ruleMapper,
                           RuleConfigValidator ruleConfigValidator,
                           RuleExecutionLogMapper logMapper) {
        this.ruleMapper = ruleMapper;
        this.ruleConfigValidator = ruleConfigValidator;
        this.logMapper = logMapper;
    }

    @Override
    public AutomationRuleVO create(RuleCreateRequest request) {
        ruleConfigValidator.validateTriggerType(request.getTriggerType());
        ruleConfigValidator.validateConditionConfig(request.getTriggerType(), request.getConditionConfig());
        ruleConfigValidator.validateActionConfig(request.getActionConfig());

        AutomationRule entity = new AutomationRule();
        entity.setRuleName(request.getRuleName());
        entity.setTriggerType(request.getTriggerType());
        entity.setConditionConfig(request.getConditionConfig().toString());
        entity.setActionConfig(request.getActionConfig().toString());
        entity.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
        entity.setCreateTime(TimeUtils.now());
        ruleMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public IPage<AutomationRuleVO> page(String ruleName, String triggerType, Boolean enabled,
                                        long pageNum, long pageSize) {
        if (triggerType != null && !triggerType.isBlank()) {
            ruleConfigValidator.validateTriggerType(triggerType);
        }

        QueryWrapper<AutomationRule> wrapper = new QueryWrapper<>();
        if (ruleName != null && !ruleName.isBlank()) {
            wrapper.like("rule_name", ruleName);
        }
        if (triggerType != null && !triggerType.isBlank()) {
            wrapper.eq("trigger_type", triggerType);
        }
        if (enabled != null) {
            wrapper.eq("enabled", enabled);
        }
        wrapper.orderByDesc("id");

        Page<AutomationRule> pageParam = new Page<>(pageNum, pageSize);
        IPage<AutomationRule> result = ruleMapper.selectPage(pageParam, wrapper);
        return result.convert(RuleServiceImpl::toVO);
    }

    @Override
    public AutomationRuleVO getById(Long id) {
        return toVO(requireEntity(id));
    }

    @Override
    public AutomationRuleVO update(Long id, RuleUpdateRequest request) {
        requireEntity(id);
        ruleConfigValidator.validateTriggerType(request.getTriggerType());
        ruleConfigValidator.validateConditionConfig(request.getTriggerType(), request.getConditionConfig());
        ruleConfigValidator.validateActionConfig(request.getActionConfig());

        UpdateWrapper<AutomationRule> update = new UpdateWrapper<AutomationRule>()
                .set("rule_name", request.getRuleName())
                .set("trigger_type", request.getTriggerType())
                .set("condition_config", request.getConditionConfig().toString())
                .set("action_config", request.getActionConfig().toString())
                .set("enabled", request.getEnabled())
                .eq("id", id);
        ruleMapper.update(null, update);
        return toVO(ruleMapper.selectById(id));
    }

    @Override
    public void delete(Long id) {
        requireEntity(id);
        ruleMapper.deleteById(id);
    }

    @Override
    public AutomationRuleVO updateEnabled(Long id, RuleEnabledRequest request) {
        requireEntity(id);
        UpdateWrapper<AutomationRule> update = new UpdateWrapper<AutomationRule>()
                .set("enabled", request.getEnabled())
                .eq("id", id);
        ruleMapper.update(null, update);
        return toVO(ruleMapper.selectById(id));
    }

    @Override
    public IPage<RuleExecutionLogVO> pageLogs(Long ruleId, long pageNum, long pageSize) {
        Page<RuleExecutionLogVO> pageParam = new Page<>(pageNum, pageSize);
        return logMapper.selectLogPage(pageParam, ruleId);
    }

    private AutomationRule requireEntity(Long id) {
        AutomationRule entity = ruleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "规则不存在: id=" + id);
        }
        return entity;
    }

    /** 实体 -> 对外 VO（JSON 文本解析回 JsonNode，时间转 epoch 毫秒） */
    static AutomationRuleVO toVO(AutomationRule entity) {
        AutomationRuleVO vo = new AutomationRuleVO();
        vo.setId(entity.getId());
        vo.setRuleName(entity.getRuleName());
        vo.setTriggerType(entity.getTriggerType());
        vo.setConditionConfig(parseJson(entity.getConditionConfig()));
        vo.setActionConfig(parseJson(entity.getActionConfig()));
        vo.setEnabled(entity.getEnabled());
        vo.setCreateTime(TimeUtils.toEpochMillis(entity.getCreateTime()));
        return vo;
    }

    private static JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("存储的规则 JSON 配置非法", e);
        }
    }
}
