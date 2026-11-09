package com.smarthome.device.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 设备新增 / 修改请求（归属 S3）。
 *
 * <p>online_status / device_status 不对外暴露为入参：
 * 新增时由服务端置为 OFFLINE / OFF，运行/在线状态只能通过心跳等接口驱动。</p>
 */
public class DeviceRequest {

    /** 设备编码（唯一） */
    @NotBlank(message = "设备编码不能为空")
    private String deviceCode;

    /** 设备名称 */
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;

    /** 设备类型 */
    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    /** 所属房间编号（逻辑引用 S1.room，可为空） */
    private Long roomId;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
