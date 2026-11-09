package com.smarthome.device.config;

import com.smarthome.device.entity.Device;
import com.smarthome.device.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * S3-01 最小化数据库读写验证：通过 MyBatis-Plus 对 device 表做一次
 * 插入 -> 查询 -> 删除 的完整往返，验证 Mapper 映射与数据库默认值均正常。
 * 使用独立临时 device_code，结束后清理，不污染业务数据。
 */
@Component
@Order(1)
public class DeviceDataAccessVerifier implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DeviceDataAccessVerifier.class);

    private final DeviceMapper deviceMapper;

    public DeviceDataAccessVerifier(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    public void run(String... args) {
        String tempCode = "__verify_" + System.currentTimeMillis();
        Device device = new Device();
        device.setDeviceCode(tempCode);
        device.setDeviceName("读写验证临时设备");
        device.setDeviceType("TEMP");

        deviceMapper.insert(device);
        log.info("S3 写验证成功：插入临时设备 id={}, deviceCode={}", device.getId(), device.getDeviceCode());

        Device read = deviceMapper.selectById(device.getId());
        log.info("S3 读验证成功：回读设备 deviceCode={}, onlineStatus={}, deviceStatus={}",
                read.getDeviceCode(), read.getOnlineStatus(), read.getDeviceStatus());

        deviceMapper.deleteById(device.getId());
        log.info("S3 删除清理成功：移除临时设备 id={}", device.getId());
    }
}
