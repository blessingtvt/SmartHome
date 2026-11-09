package com.smarthome.environment.controller;

import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.Result;
import com.smarthome.common.result.ResultCode;
import com.smarthome.environment.dto.EnvironmentReadingRequest;
import com.smarthome.environment.service.EnvironmentDataService;
import com.smarthome.environment.vo.EnvironmentDataVO;
import com.smarthome.environment.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S1 环境数据接口。
 */
@RestController
@RequestMapping("/api/v1/environment")
public class EnvironmentController {

    private final EnvironmentDataService environmentDataService;

    public EnvironmentController(EnvironmentDataService environmentDataService) {
        this.environmentDataService = environmentDataService;
    }

    /** API01 环境数据上报 */
    @PostMapping("/readings")
    public Result<EnvironmentDataVO> upload(@Valid @RequestBody EnvironmentReadingRequest request) {
        return Result.success("上报成功", environmentDataService.save(request));
    }

    /** API05 查询设备当前环境 */
    @GetMapping("/current")
    public Result<EnvironmentDataVO> current(@RequestParam("deviceCode") String deviceCode) {
        EnvironmentDataVO vo = environmentDataService.current(deviceCode);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(),
                    "该设备暂无环境数据: " + deviceCode);
        }
        return Result.success(vo);
    }

    /** API06 分页查询环境历史数据 */
    @GetMapping("/history")
    public Result<PageVO<EnvironmentDataVO>> history(
            @RequestParam(value = "deviceCode", required = false) String deviceCode,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "20") long size) {
        return Result.success(PageVO.of(environmentDataService.history(
                deviceCode, startTime, endTime, page, size)));
    }
}
