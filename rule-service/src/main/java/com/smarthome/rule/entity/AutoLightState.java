package com.smarthome.rule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 自动照明状态表 auto_light_state 对应实体（归属 S2）。
 *
 * <p>阶段四 pir_light：持久化每个照明设备的最近一次 PIR 活动时间，
 * 供 S2 定时关灯任务判断「60 秒无活动」。落在数据库中（而非内存），
 * 保证 S2 重启后不丢失状态、不会因状态丢失而立即错误关灯。</p>
 */
@TableName("auto_light_state")
public class AutoLightState {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 照明设备编码（唯一） */
    private String deviceCode;

    /** 最近一次 PIR 活动时间（无活动记录时为 null） */
    private LocalDateTime lastActivityTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

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

    public LocalDateTime getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(LocalDateTime lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
