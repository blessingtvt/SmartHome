package com.smarthome.device.vo;

/**
 * 设备信息响应（对外接口统一使用 epoch 毫秒时间戳）。
 */
public class DeviceVO {

    private Long id;

    private String deviceCode;

    private String deviceName;

    private String deviceType;

    private Long roomId;

    /** 在线状态（ONLINE/OFFLINE） */
    private String onlineStatus;

    /** 运行状态（ON/OFF） */
    private String deviceStatus;

    /** 创建时间（epoch 毫秒） */
    private Long createTime;

    /** 最后心跳/活动时间（epoch 毫秒，未上报过心跳时为 null） */
    private Long lastHeartbeatTime;

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

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(Long lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }
}
