package com.smarthome.environment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 家庭信息新增 / 修改请求（归属 S1）。
 */
public class FamilyRequest {

    @NotBlank(message = "家庭名称不能为空")
    private String familyName;

    private String address;

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
