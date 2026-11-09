/**
 * G0 网关服务。
 *
 * <p>职责：统一 API 入口，将前端和 ESP32 请求按路径路由到 S1/S2/S3/S4。</p>
 * <p>当前为占位模块：阶段6 才引入 Spring Cloud Gateway 与 Nacos 服务发现，
 * 并新增 GatewayApplication 启动类与路由配置。注意 Gateway 基于 WebFlux，
 * 届时不要引入 spring-boot-starter-web（webmvc），否则会启动冲突。</p>
 */
package com.smarthome.gateway;
