package com.smarthome.alarm.dto;

/**
 * 老人一键求助请求（归属 S4，API04）。
 * device_code 与 content 均可为空。
 */
public class AlarmHelpRequest {

    /** 相关设备编码（可空） */
    private String deviceCode;

    /** 求助内容（可空，空时服务端使用默认文案） */
    private String content;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
