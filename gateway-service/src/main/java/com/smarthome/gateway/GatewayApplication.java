package com.smarthome.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * G0 网关服务启动入口（阶段6）。
 *
 * <p>基于 Spring Cloud Gateway（WebFlux 响应式），作为统一 API 入口，按路径前缀
 * 通过 Nacos 服务发现（lb://服务名）转发到 S1~S4 各业务服务。</p>
 *
 * <p>注意：本模块不得引入 spring-boot-starter-web（webmvc），否则与 WebFlux 启动冲突；
 * 本模块也不依赖 smart-home-common，故默认只扫描 com.smarthome.gateway 包。</p>
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
