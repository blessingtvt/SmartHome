package com.smarthome.rule.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 启用/禁用规则请求（S2-02）。
 */
public class RuleEnabledRequest {

    /** 是否启用 */
    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
