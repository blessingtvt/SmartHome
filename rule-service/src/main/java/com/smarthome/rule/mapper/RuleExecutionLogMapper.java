package com.smarthome.rule.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthome.rule.entity.RuleExecutionLog;
import com.smarthome.rule.vo.RuleExecutionLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 规则执行记录表 Mapper（归属 S2）。
 */
@Mapper
public interface RuleExecutionLogMapper extends BaseMapper<RuleExecutionLog> {

    /**
     * 查询指定规则最近一条非 ERROR 的评估结果（MATCHED/NOT_MATCHED/BLOCKED），
     * 用于 S2-04 重复触发判断。无记录时返回 null。
     * BLOCKED 表示上轮条件命中但被手动控制保护拦截，应允许后续重新触发。
     */
    @Select("SELECT evaluation_result FROM rule_execution_log "
            + "WHERE rule_id = #{ruleId} AND evaluation_result IN ('MATCHED','NOT_MATCHED','BLOCKED') "
            + "ORDER BY id DESC LIMIT 1")
    String selectLatestNonErrorResult(@Param("ruleId") Long ruleId);

    /**
     * 查询指定规则最近 limit 条非 ERROR 评估结果（MATCHED/NOT_MATCHED/BLOCKED，按 id 倒序）。
     * 仅返回 evaluation_result / execution_result 两列，供 WATER 去抖规则的连续命中计数
     * 与「已执行不重复触发」判断使用。
     */
    @Select("SELECT evaluation_result, execution_result FROM rule_execution_log "
            + "WHERE rule_id = #{ruleId} AND evaluation_result IN ('MATCHED','NOT_MATCHED','BLOCKED') "
            + "ORDER BY id DESC LIMIT #{limit}")
    List<RuleExecutionLog> selectRecentNonError(@Param("ruleId") Long ruleId, @Param("limit") int limit);

    /**
     * 查询待回填设备实际执行结果的日志行（已创建指令、但 device_exec_result 仍为空）。
     * 仅返回 id / device_command_id 两列，供 S2 定时轮询 S3 指令状态后回填 SUCCESS/FAILED。
     */
    @Select("SELECT id, device_command_id FROM rule_execution_log "
            + "WHERE device_command_id IS NOT NULL AND device_exec_result IS NULL "
            + "ORDER BY id DESC LIMIT #{limit}")
    List<RuleExecutionLog> selectPendingExecBackfill(@Param("limit") int limit);

    /**
     * 分页查询规则执行日志（阶段五只读日志接口，联表 automation_rule 取规则名）。
     * 按 id 倒序；ruleId 可空表示查询全部规则。execution_time 转为 epoch 毫秒。
     */
    @Select("<script>"
            + "SELECT l.id, l.rule_id, r.rule_name, l.trigger_value, l.evaluation_result, "
            + "l.execution_result, l.device_command_id, l.device_exec_result, l.failure_reason, "
            + "UNIX_TIMESTAMP(l.execution_time) * 1000 AS execution_time "
            + "FROM rule_execution_log l "
            + "LEFT JOIN automation_rule r ON r.id = l.rule_id "
            + "<where>"
            + "<if test='ruleId != null'> AND l.rule_id = #{ruleId} </if>"
            + "</where> "
            + "ORDER BY l.id DESC "
            + "</script>")
    IPage<RuleExecutionLogVO> selectLogPage(Page<RuleExecutionLogVO> page, @Param("ruleId") Long ruleId);
}
