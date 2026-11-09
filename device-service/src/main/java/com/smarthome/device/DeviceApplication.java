package com.smarthome.device;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * S3 设备服务启动入口。
 * scanBasePackages 指向 com.smarthome，加载 common 模块共享组件。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.smarthome")
public class DeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceApplication.class, args);
    }
}
