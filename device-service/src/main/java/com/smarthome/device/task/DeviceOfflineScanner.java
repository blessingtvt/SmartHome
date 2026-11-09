package com.smarthome.device.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.device.entity.Device;
import com.smarthome.device.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时离线扫描：把超过阈值未心跳的 ONLINE 设备翻转为 OFFLINE。
 *
 * <p>只做 ONLINE -> OFFLINE 单向翻转；last_heartbeat_time 为空的设备自创建起即 OFFLINE，
 * 无需处理。阈值与扫描间隔均从配置读取，Java 侧计算 cutoff，规避时区/DB 函数差异。</p>
 */
@Component
public class DeviceOfflineScanner {

    private static final Logger log = LoggerFactory.getLogger(DeviceOfflineScanner.class);

    private static final String ONLINE = "ONLINE";
    private static final String OFFLINE = "OFFLINE";

    private final DeviceMapper deviceMapper;
    private final long thresholdSeconds;

    public DeviceOfflineScanner(DeviceMapper deviceMapper,
                                @Value("${smart-home.device.offline-threshold-seconds:90}") long thresholdSeconds) {
        this.deviceMapper = deviceMapper;
        this.thresholdSeconds = thresholdSeconds;
    }

    @Scheduled(fixedDelayString = "${smart-home.device.offline-scan-interval-ms:30000}")
    public void scanOfflineDevices() {
        LocalDateTime cutoff = TimeUtils.now().minusSeconds(thresholdSeconds);
        int updated = deviceMapper.update(null, new UpdateWrapper<Device>()
                .set("online_status", OFFLINE)
                .eq("online_status", ONLINE)
                .lt("last_heartbeat_time", cutoff));
        if (updated > 0) {
            log.info("离线扫描：{} 台设备超时判为 OFFLINE（阈值 {}s）", updated, thresholdSeconds);
        }
    }
}
