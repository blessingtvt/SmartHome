package com.smarthome.device.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 家属远程控制设备请求（API08）。
 * 目标设备由路径 deviceId 解析，source 固定为 REMOTE（服务端设置）。
 */
public class DeviceRemoteCommandRequest {

    /** 控制动作（TURN_ON/TURN_OFF） */
    @NotBlank(message = "控制动作不能为空")
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
