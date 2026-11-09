package com.smarthome.rule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S2 自动化规则服务启动入口。
 * scanBasePackages 指向 com.smarthome，加载 common 模块共享组件。
 */
@SpringBootApplication(scanBasePackages = "com.smarthome")
public class RuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuleApplication.class, args);
    }
}
