package com.smarthome.device.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 内部指令创建请求（归属 S3，INT02）。
 *
 * <p>供 S2 规则服务调用：通过 device_code 定位目标设备，source 仅允许 AUTO_RULE。</p>
 */
public class DeviceCommandCreateRequest {

    /** 目标设备编码（必填） */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /** 控制动作（TURN_ON/TURN_OFF） */
    @NotBlank(message = "控制动作不能为空")
    private String action;

    /** 控制来源（INT02 仅允许 AUTO_RULE） */
    @NotBlank(message = "控制来源不能为空")
    private String source;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
