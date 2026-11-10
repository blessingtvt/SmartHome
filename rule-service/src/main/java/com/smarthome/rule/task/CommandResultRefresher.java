package com.smarthome.rule.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smarthome.rule.client.ActionCallException;
import com.smarthome.rule.client.RuleActionClient;
import com.smarthome.rule.entity.RuleExecutionLog;
import com.smarthome.rule.mapper.RuleExecutionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备执行结果回填任务（阶段四，归属 S2）。
 *
 * <p>S2 定时轮询 S3 指令状态，把 rule_execution_log 中「已创建指令但 device_exec_result 为空」
 * 的日志行回填为 SUCCESS / FAILED；PENDING 不回填。由此区分「命令创建成功」与「设备实际执行成功」。</p>
 */
@Component
public class CommandResultRefresher {

    private static final Logger log = LoggerFactory.getLogger(CommandResultRefresher.class);

    private static final String COMMAND_PENDING = "PENDING";

    private final RuleExecutionLogMapper logMapper;
    private final RuleActionClient actionClient;
    private final int batchSize;

    public CommandResultRefresher(RuleExecutionLogMapper logMapper,
                                  RuleActionClient actionClient,
                                  @Value("${smart-home.auto-light.result-refresh-batch-size:100}") int batchSize) {
        this.logMapper = logMapper;
        this.actionClient = actionClient;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${smart-home.auto-light.result-refresh-interval-ms:10000}")
    public void refresh() {
        List<RuleExecutionLog> pendingRows = logMapper.selectPendingExecBackfill(batchSize);
        for (RuleExecutionLog row : pendingRows) {
            if (row.getId() == null || row.getDeviceCommandId() == null) {
                continue;
            }
            String status;
            try {
                status = actionClient.getCommandStatus(row.getDeviceCommandId());
            } catch (ActionCallException e) {
                log.warn("[执行结果回填] 指令 {} 状态查询失败，下轮重试: {}", row.getDeviceCommandId(), e.getMessage());
                continue;
            }
            if (COMMAND_PENDING.equals(status)) {
                continue; // 仍在执行，不回填
            }
            // 终态（SUCCESS/FAILED）回填；条件更新保证并发下不覆盖
            logMapper.update(null, new UpdateWrapper<RuleExecutionLog>()
                    .set("device_exec_result", status)
                    .eq("id", row.getId())
                    .isNull("device_exec_result"));
        }
    }
}
