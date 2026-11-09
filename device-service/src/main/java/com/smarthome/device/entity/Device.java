package com.smarthome.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 设备信息表 device 对应实体（归属 S3）。
 * 字段名与数据库 snake_case 自动映射（map-underscore-to-camel-case）。
 *
 * <p>device_code 为跨服务设备标识（ESP32/前端持有），数字主键 id 仅在 S3 内部使用；
 * room_id 为逻辑引用 S1.room 的房间编号，不建外键、不跨服务查询。</p>
 */
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编码（唯一，ESP32/前端使用） */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型 */
    private String deviceType;

    /** 所属房间编号（逻辑引用 S1.room，可为空） */
    private Long roomId;

    /** 在线状态（ONLINE/OFFLINE） */
    private String onlineStatus;

    /** 运行状态（ON/OFF） */
    private String deviceStatus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后心跳/活动时间（ESP32 网络连接状态判定，可空） */
    private LocalDateTime lastHeartbeatTime;

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }
}
