package com.smarthome.device.controller;

import com.smarthome.common.result.Result;
import com.smarthome.device.dto.DeviceCommandResultRequest;
import com.smarthome.device.service.DeviceCommandService;
import com.smarthome.device.vo.DeviceCommandVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3-04 设备控制指令接口（API02 / API03）。
 */
@RestController
@RequestMapping("/api/v1/device-commands")
public class DeviceCommandController {

    private final DeviceCommandService deviceCommandService;

    public DeviceCommandController(DeviceCommandService deviceCommandService) {
        this.deviceCommandService = deviceCommandService;
    }

    /** API02：查询设备待执行指令 */
    @GetMapping("/pending")
    public Result<List<DeviceCommandVO>> pending(@RequestParam(value = "deviceCode", required = false) String deviceCode) {
        return Result.success(deviceCommandService.listPending(deviceCode));
    }

    /** API（新增）：查询设备指令历史（按 create_time 倒序，最多 size 条） */
    @GetMapping
    public Result<List<DeviceCommandVO>> history(
            @RequestParam("deviceCode") String deviceCode,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        return Result.success(deviceCommandService.listHistory(deviceCode, size));
    }

    /** API03：上报指令执行结果 */
    @PostMapping("/{commandId}/result")
    public Result<DeviceCommandVO> reportResult(@PathVariable("commandId") Long commandId,
                                                @Valid @RequestBody DeviceCommandResultRequest request) {
        return Result.success("结果上报成功", deviceCommandService.reportResult(commandId, request));
    }
}
