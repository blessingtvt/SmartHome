package com.smarthome.device.controller;

import com.smarthome.common.result.Result;
import com.smarthome.device.service.DeviceCommandService;
import com.smarthome.device.vo.DeviceCommandVO;
import com.smarthome.device.vo.DeviceStateVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INT03/INT04：内部查询接口（供 S2 规则服务调用）。
 *
 * <p>INT03：设备当前执行状态（由指令历史推导，不读 device.device_status 陈旧字段）；
 * INT04：单条指令详情（供 S2 回填设备实际执行结果）。</p>
 */
@RestController
@RequestMapping("/internal")
public class InternalDeviceController {

    private final DeviceCommandService deviceCommandService;

    public InternalDeviceController(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    @GetMapping("/devices/{deviceCode}/state")
    public Result<DeviceStateVO> state(@PathVariable String deviceCode) {
        return Result.success("设备状态查询成功", deviceCommandService.getDeviceState(deviceCode));
    }

    @GetMapping("/device-commands/{commandId}")
    public Result<DeviceCommandVO> command(@PathVariable Long commandId) {
        return Result.success("指令查询成功", deviceCommandService.getCommand(commandId));
    }
}
