package com.smarthome.alarm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 内部告警创建请求（归属 S4，INT03）。
 * 供 S2 规则服务调用；alarm_type 由服务端固定为 ENV_ABNORMAL，不接受客户端指定。
 */
public class AlarmCreateRequest {

    /** 相关设备编码（ENV_ABNORMAL 必填） */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /** 告警内容（必填） */
    @NotBlank(message = "告警内容不能为空")
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
