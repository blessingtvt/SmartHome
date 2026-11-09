package com.smarthome.device.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 设备控制指令创建请求（归属 S3）。
 *
 * <p>外部接口 POST /api/v1/devices/{deviceId}/commands 由路径 deviceId 解析目标设备，
 * 此处的 deviceCode 可空；内部接口 POST /internal/device-commands 由 S2 以
 * device_code 指定目标设备，此时 deviceCode 必填（在 Service 层校验）。</p>
 */
public class DeviceCommandCreateRequest {

    /** 目标设备编码（内部接口使用；外部接口由 deviceId 解析，可空） */
    private String deviceCode;

    /** 控制动作（TURN_ON/TURN_OFF/SET_LEVEL） */
    @NotBlank(message = "控制动作不能为空")
    private String action;

    /** 控制来源（RULE/REMOTE） */
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
