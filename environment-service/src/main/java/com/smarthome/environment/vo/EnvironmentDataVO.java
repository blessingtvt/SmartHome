package com.smarthome.environment.vo;

import java.math.BigDecimal;

/**
 * 环境数据响应（对外接口统一使用 epoch 毫秒时间戳）。
 */
public class EnvironmentDataVO {

    private Long id;

    private String deviceCode;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private Integer gasRaw;

    private Boolean pirTriggered;

    /** 采集时间（epoch 毫秒） */
    private Long timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
