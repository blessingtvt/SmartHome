package com.smarthome.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 告警记录表 alarm 对应实体（归属 S4）。
 * 字段名与数据库 snake_case 自动映射（map-underscore-to-camel-case）。
 *
 * <p>alarm_code 为对外唯一业务编码；alarm_type 取值 ENV_ABNORMAL / MANUAL_HELP；
 * status 取值 PENDING / PROCESSING / PROCESSED。</p>
 */
@TableName("alarm")
public class Alarm {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 告警编码（唯一） */
    private String alarmCode;

    /** 告警类型（ENV_ABNORMAL / MANUAL_HELP） */
    private String alarmType;

    /** 相关设备编码（ENV_ABNORMAL 必填，MANUAL_HELP 可空） */
    private String deviceCode;

    /** 告警内容 */
    private String alarmContent;

    /** 告警发生时间 */
    private LocalDateTime alarmTime;

    /** 处理状态（PENDING / PROCESSING / PROCESSED） */
    private String status;

    /** 处理时间（仅 PROCESSED 时写入） */
    private LocalDateTime handleTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlarmCode() {
        return alarmCode;
    }

    public void setAlarmCode(String alarmCode) {
        this.alarmCode = alarmCode;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getAlarmContent() {
        return alarmContent;
    }

    public void setAlarmContent(String alarmContent) {
        this.alarmContent = alarmContent;
    }

    public LocalDateTime getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(LocalDateTime alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }
}
