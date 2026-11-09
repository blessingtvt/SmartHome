package com.smarthome.environment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * S1 环境感知服务启动入口。
 * scanBasePackages 指向 com.smarthome，以便同时加载 common 模块中的共享组件
 * （如统一异常处理 GlobalExceptionHandler）。
 */
@SpringBootApplication(scanBasePackages = "com.smarthome")
public class EnvironmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvironmentApplication.class, args);
    }
}
