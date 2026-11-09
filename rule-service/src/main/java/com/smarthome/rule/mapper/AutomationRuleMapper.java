package com.smarthome.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.rule.entity.AutomationRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自动化规则表 Mapper（归属 S2）。
 */
@Mapper
public interface AutomationRuleMapper extends BaseMapper<AutomationRule> {
}
