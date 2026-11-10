package com.smarthome.rule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * S2 自动化规则服务启动入口。
 * scanBasePackages 指向 com.smarthome，加载 common 模块共享组件。
 * 阶段四：启用定时任务（自动关灯扫描 + 设备执行结果回填）。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.smarthome")
public class RuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleApplication.class, args);
    }
}
