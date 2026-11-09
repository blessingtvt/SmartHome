package com.smarthome.alarm.controller;

import com.smarthome.alarm.dto.AlarmCreateRequest;
import com.smarthome.alarm.service.AlarmService;
import com.smarthome.alarm.vo.AlarmVO;
import com.smarthome.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INT03：内部创建环境异常告警接口（供 S2 规则服务调用，当前无需接入 Nacos）。
 */
@RestController
@RequestMapping("/internal/alarms")
public class InternalAlarmController {

    private final AlarmService alarmService;

    public InternalAlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @PostMapping
    public Result<AlarmVO> create(@Valid @RequestBody AlarmCreateRequest request) {
        return Result.success("告警创建成功", alarmService.createEnvAbnormal(request));
    }
}
