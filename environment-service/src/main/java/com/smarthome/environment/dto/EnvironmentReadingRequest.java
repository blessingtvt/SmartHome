package com.smarthome.environment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 环境数据上报请求（API01）。ESP32 / 前端通过该结构上传一次采集数据。
 * 除 deviceCode 外其余字段均可空（传感器未接入时不上报），但为空时跳过对应校验。
 */
public class EnvironmentReadingRequest {

    /** 数据来源设备编码 */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /** 温度(℃)，范围 [-40, 80] */
    @DecimalMin(value = "-40", message = "温度不能低于 -40℃")
    @DecimalMax(value = "80", message = "温度不能高于 80℃")
    private BigDecimal temperature;

    /** 湿度(%)，范围 [0, 100] */
    @DecimalMin(value = "0", message = "湿度不能低于 0%")
    @DecimalMax(value = "100", message = "湿度不能高于 100%")
    private BigDecimal humidity;

    /** MQ-2 原始读数，非负 */
    @Min(value = 0, message = "MQ-2 原始读数不能为负")
    private Integer gasRaw;

    /** 水滴感应原始值(0-4095,ADC)，非负 */
    @Min(value = 0, message = "水滴感应原始值不能为负")
    private Integer water;

    /** PIR 是否触发 */
    private Boolean pirTriggered;

    /** 采集时间（epoch 毫秒）。为空时按服务端当前时间处理 */
    @Positive(message = "时间戳必须为正数")
    private Long timestamp;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    public Integer getGasRaw() {
        return gasRaw;
    }

    public void setGasRaw(Integer gasRaw) {
        this.gasRaw = gasRaw;
    }

    public Integer getWater() {
        return water;
    }

    public void setWater(Integer water) {
        this.water = water;
    }

    public Boolean getPirTriggered() {
        return pirTriggered;
    }

    public void setPirTriggered(Boolean pirTriggered) {
        this.pirTriggered = pirTriggered;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
