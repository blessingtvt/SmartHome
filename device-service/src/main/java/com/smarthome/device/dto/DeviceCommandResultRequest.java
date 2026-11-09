package com.smarthome.device.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 设备指令执行结果上报请求（归属 S3，API03）。
 * ESP32 执行完指令后上报 SUCCESS 或 FAILED。
 */
public class DeviceCommandResultRequest {

    /** 目标设备编码（需与指令所属设备一致） */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /** 执行结果（SUCCESS/FAILED），Service 层校验是否合法 */
    @NotBlank(message = "执行结果不能为空")
    private String status;

    /** 执行结果说明（可为空） */
    private String resultMessage;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }
}
