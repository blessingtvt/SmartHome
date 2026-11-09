/**
 * G0 网关服务。
 *
 * <p>职责：统一 API 入口，将前端和 ESP32 请求按路径路由到 S1/S2/S3/S4。</p>
 * <p>基于 Spring Cloud Gateway（WebFlux）+ Nacos 服务发现，通过 lb://服务名 转发；
 * 内部接口 /internal/** 不对外暴露。注意不要引入 spring-boot-starter-web（webmvc）。</p>
 */
package com.smarthome.gateway;
