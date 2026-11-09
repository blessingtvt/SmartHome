package com.smarthome.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 设备控制指令表 device_command 对应实体（归属 S3）。
 * 字段名与数据库 snake_case 自动映射（map-underscore-to-camel-case）。
 *
 * <p>指令通过 device_code 关联目标设备（不引用 device.id），
 * 状态流转：PENDING -> SUCCESS / FAILED。</p>
 */
@TableName("device_command")
public class DeviceCommand {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指令编码（唯一） */
    private String commandCode;

    /** 目标设备编码 */
    private String deviceCode;

    /** 控制动作（TURN_ON/TURN_OFF/SET_LEVEL） */
    private String action;

    /** 控制来源（RULE/REMOTE） */
    private String source;

    /** 指令状态（PENDING/SUCCESS/FAILED） */
    private String status;

    /** 执行结果说明 */
    private String resultMessage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(String commandCode) {
        this.commandCode = commandCode;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }
}
