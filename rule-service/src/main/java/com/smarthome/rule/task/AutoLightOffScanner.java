package com.smarthome.rule.task;

import com.smarthome.rule.service.AutoLightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 自动关灯定时扫描任务（阶段四，归属 S2）。
 *
 * <p>每 10 秒扫描一次「60 秒无活动」的照明设备，执行自动关灯判断。
 * 与设备执行结果回填（CommandResultRefresher）相互独立。</p>
 */
@Component
public class AutoLightOffScanner {

    private static final Logger log = LoggerFactory.getLogger(AutoLightOffScanner.class);

    private final AutoLightService autoLightService;

    public AutoLightOffScanner(AutoLightService autoLightService) {
        this.autoLightService = autoLightService;
    }

    @Scheduled(fixedDelayString = "${smart-home.auto-light.off-scan-interval-ms:10000}")
    public void scan() {
        try {
            autoLightService.scanAndTurnOff();
        } catch (Exception e) {
            // 单轮异常不中断后续调度
            log.warn("[自动关灯] 扫描异常: {}", e.getMessage(), e);
        }
    }
}
