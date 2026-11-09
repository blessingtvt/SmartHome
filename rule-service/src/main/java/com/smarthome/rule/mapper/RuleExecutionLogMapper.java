package com.smarthome.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthome.rule.entity.RuleExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 规则执行记录表 Mapper（归属 S2）。
 */
@Mapper
public interface RuleExecutionLogMapper extends BaseMapper<RuleExecutionLog> {

    /**
     * 查询指定规则最近一条非 ERROR 的评估结果（MATCHED/NOT_MATCHED），
     * 用于 S2-04 重复触发判断。无记录时返回 null。
     */
    @Select("SELECT evaluation_result FROM rule_execution_log "
            + "WHERE rule_id = #{ruleId} AND evaluation_result IN ('MATCHED','NOT_MATCHED') "
            + "ORDER BY id DESC LIMIT 1")
    String selectLatestNonErrorResult(@Param("ruleId") Long ruleId);
}
