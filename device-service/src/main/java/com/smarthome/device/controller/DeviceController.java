package com.smarthome.device.controller;

import com.smarthome.common.result.Result;
import com.smarthome.device.dto.DeviceRequest;
import com.smarthome.device.service.DeviceService;
import com.smarthome.device.vo.DeviceVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3 设备信息管理接口。
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public Result<List<DeviceVO>> list() {
        return Result.success(deviceService.list());
    }

    @PostMapping
    public Result<DeviceVO> create(@Valid @RequestBody DeviceRequest request) {
        return Result.success("创建成功", deviceService.create(request));
    }

    @GetMapping("/{id}")
    public Result<DeviceVO> getById(@PathVariable("id") Long id) {
        return Result.success(deviceService.getById(id));
    }

    @PutMapping("/{id}")
    public Result<DeviceVO> update(@PathVariable("id") Long id, @Valid @RequestBody DeviceRequest request) {
        return Result.success("修改成功", deviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        deviceService.delete(id);
        return Result.success();
    }

    @PostMapping("/{deviceCode}/heartbeat")
    public Result<DeviceVO> heartbeat(@PathVariable("deviceCode") String deviceCode) {
        return Result.success("心跳成功", deviceService.heartbeat(deviceCode));
    }
}
