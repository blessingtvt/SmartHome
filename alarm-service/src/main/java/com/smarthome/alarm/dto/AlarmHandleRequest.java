package com.smarthome.alarm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 告警状态更新请求（归属 S4，API10）。
 * status 仅允许 PROCESSING / PROCESSED，具体流转合法性在 Service 层校验。
 */
public class AlarmHandleRequest {

    /** 目标处理状态（PROCESSING / PROCESSED） */
    @NotBlank(message = "处理状态不能为空")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
