package com.smarthome.alarm.vo;

/**
 * 告警响应（对外接口统一使用 epoch 毫秒时间戳）。
 * 只包含 alarm 表实际存在的字段。
 */
public class AlarmVO {

    private Long id;

    private String alarmCode;

    private String alarmType;

    private String deviceCode;

    private String alarmContent;

    /** 告警发生时间（epoch 毫秒） */
    private Long alarmTime;

    private String status;

    /** 处理时间（epoch 毫秒，未处理时为 null） */
    private Long handleTime;

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

    public Long getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(Long alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(Long handleTime) {
        this.handleTime = handleTime;
    }
}
