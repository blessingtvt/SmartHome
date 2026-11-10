package com.smarthome.environment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 环境数据表 environment_data 对应实体（归属 S1）。
 * 字段名与数据库 snake_case 自动映射（map-underscore-to-camel-case）。
 */
@TableName("environment_data")
public class EnvironmentData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据来源设备编码（字符串引用设备，遵守数据隔离，不引用 S3 的 device.id） */
    private String deviceCode;

    /** 温度(℃) */
    private BigDecimal temperature;

    /** 湿度(%) */
    private BigDecimal humidity;

    /** MQ-2 原始读数 */
    private Integer gasRaw;

    /** 水滴感应原始值(0-4095,ADC) */
    private Integer water;

    /** PIR 是否触发(0/1) */
    private Boolean pirTriggered;

    /** 采集时间 */
    private LocalDateTime collectTime;

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

    public LocalDateTime getCollectTime() {
        return collectTime;
    }

    public void setCollectTime(LocalDateTime collectTime) {
        this.collectTime = collectTime;
    }
}
