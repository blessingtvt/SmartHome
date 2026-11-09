package com.smarthome.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 房间信息新增 / 修改请求（归属 S1）。
 */
public class RoomRequest {

    @NotNull(message = "所属家庭编号不能为空")
    private Long familyId;

    @NotBlank(message = "房间名称不能为空")
    private String roomName;

    public Long getFamilyId() {
        return familyId;
    }

    public void setFamilyId(Long familyId) {
        this.familyId = familyId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
