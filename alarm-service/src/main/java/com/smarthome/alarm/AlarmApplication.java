package com.smarthome.alarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S4 告警服务启动入口。
 * scanBasePackages 指向 com.smarthome，加载 common 模块共享组件。
 */
@SpringBootApplication(scanBasePackages = "com.smarthome")
public class AlarmApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlarmApplication.class, args);
    }
}
