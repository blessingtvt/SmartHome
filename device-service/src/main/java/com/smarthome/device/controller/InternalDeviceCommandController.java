package com.smarthome.device.controller;

import com.smarthome.common.result.Result;
import com.smarthome.device.dto.DeviceCommandCreateRequest;
import com.smarthome.device.service.DeviceCommandService;
import com.smarthome.device.vo.DeviceCommandVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INT02：内部指令创建接口（供 S2 规则服务调用，当前无需接入 Nacos）。
 */
@RestController
@RequestMapping("/internal/device-commands")
public class InternalDeviceCommandController {

    private final DeviceCommandService deviceCommandService;

    public InternalDeviceCommandController(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    @PostMapping
    public Result<DeviceCommandVO> create(@Valid @RequestBody DeviceCommandCreateRequest request) {
        return Result.success("指令创建成功", deviceCommandService.createInternalCommand(request));
    }
}
