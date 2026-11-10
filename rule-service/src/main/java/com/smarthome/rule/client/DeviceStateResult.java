package com.smarthome.rule.client;

/**
 * S3 内部设备状态查询结果（rule-service 本地轻量 DTO，不依赖 device-service）。
 *
 * <p>对应 device-service 的 DeviceStateVO 中 S2 关心的字段：
 * on（是否开启）、onSource（最近成功指令来源）、pending（是否存在待执行指令）。</p>
 */
public class DeviceStateResult {

    private Boolean on;

    private String onSource;

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

    public Boolean getPending() {
        return pending;
    }

    public void setPending(Boolean pending) {
        this.pending = pending;
    }
}
