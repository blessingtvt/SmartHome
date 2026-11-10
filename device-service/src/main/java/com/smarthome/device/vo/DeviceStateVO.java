package com.smarthome.device.vo;

/**
 * 设备当前执行状态（S3 内部接口返回，供 S2 自动关灯判断）。
 *
 * <p>由最近一次 SUCCESS 指令推导：action=TURN_ON 视为「开」，其余视为「关」。
 * onSource 为最近成功指令的来源（AUTO_RULE/REMOTE），用于区分自动/手动开启。</p>
 */
public class DeviceStateVO {

    /** 设备当前是否开启（true=开，false=关/无成功指令） */
    private Boolean on;

    /** 最近成功指令来源（AUTO_RULE/REMOTE），无成功指令时为 null */
    private String onSource;

    /** 最近成功指令 ID */
    private Long lastSuccessCommandId;

    /** 最近成功指令完成时间（epoch 毫秒），优先 finish_time，缺省回退 create_time */
    private Long lastSuccessTime;

    /** 是否存在待执行（PENDING）指令，用于 S2 关灯判断「无 PENDING」条件 */
    private Boolean pending;

    public Boolean getOn() {
        return on;
    }

    public void setOn(Boolean on) {
        this.on = on;
    }

    public String getOnSource() {
        return onSource;
    }

    public void setOnSource(String onSource) {
        this.onSource = onSource;
    }

    public Long getLastSuccessCommandId() {
        return lastSuccessCommandId;
    }

    public void setLastSuccessCommandId(Long lastSuccessCommandId) {
        this.lastSuccessCommandId = lastSuccessCommandId;
    }

    public Long getLastSuccessTime() {
        return lastSuccessTime;
    }

    public void setLastSuccessTime(Long lastSuccessTime) {
        this.lastSuccessTime = lastSuccessTime;
    }

    public Boolean getPending() {
        return pending;
    }

    public void setPending(Boolean pending) {
        this.pending = pending;
    }
}
