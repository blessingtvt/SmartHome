package com.smarthome.alarm.controller;

import com.smarthome.alarm.dto.AlarmHandleRequest;
import com.smarthome.alarm.dto.AlarmHelpRequest;
import com.smarthome.alarm.service.AlarmService;
import com.smarthome.alarm.vo.AlarmVO;
import com.smarthome.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * S4 告警对外接口：老人一键求助（API04）与告警状态处理（API10）。
 */
@RestController
@RequestMapping("/api/v1/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    /** API04 老人一键求助 */
    @PostMapping("/help")
    public Result<AlarmVO> help(@Valid @RequestBody AlarmHelpRequest request) {
        return Result.success("求助已受理", alarmService.createManualHelp(request));
    }

    /** API10 告警状态更新 */
    @PatchMapping("/{alarmId}/status")
    public Result<AlarmVO> updateStatus(@PathVariable("alarmId") Long alarmId,
                                        @Valid @RequestBody AlarmHandleRequest request) {
        return Result.success("状态更新成功", alarmService.updateStatus(alarmId, request));
    }
}
