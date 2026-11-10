package com.smarthome.environment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthome.common.utils.TimeUtils;
import com.smarthome.environment.client.RuleClient;
import com.smarthome.environment.dto.EnvironmentReadingRequest;
import com.smarthome.environment.entity.EnvironmentData;
import com.smarthome.environment.mapper.EnvironmentDataMapper;
import com.smarthome.environment.vo.EnvironmentDataVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 环境数据业务逻辑（S1）。
 */
@Service
public class EnvironmentDataService {

    private final EnvironmentDataMapper environmentDataMapper;
    private final RuleClient ruleClient;

    public EnvironmentDataService(EnvironmentDataMapper environmentDataMapper, RuleClient ruleClient) {
        this.environmentDataMapper = environmentDataMapper;
        this.ruleClient = ruleClient;
    }

    /**
     * 保存一次环境数据上报，随后尽力通知 S2 做规则评估（容错降级）。
     */
    @Transactional
    public EnvironmentDataVO save(EnvironmentReadingRequest request) {
        EnvironmentData entity = new EnvironmentData();
        entity.setDeviceCode(request.getDeviceCode());
        entity.setTemperature(request.getTemperature());
        entity.setHumidity(request.getHumidity());
        entity.setGasRaw(request.getGasRaw());
        entity.setWater(request.getWater());
        entity.setPirTriggered(request.getPirTriggered());
        LocalDateTime collectTime = request.getTimestamp() == null
                ? TimeUtils.now()
                : TimeUtils.toLocalDateTime(request.getTimestamp());
        entity.setCollectTime(collectTime);

        environmentDataMapper.insert(entity);

        // 尽力而为：通知 S2 规则评估，失败不影响保存
        ruleClient.evaluate(request);

        return toVO(entity);
    }

    /**
     * 查询某设备最近一次环境数据（API05）。
     */
    public EnvironmentDataVO current(String deviceCode) {
        QueryWrapper<EnvironmentData> wrapper = new QueryWrapper<>();
        wrapper.eq("device_code", deviceCode)
                .orderByDesc("collect_time")
                .last("LIMIT 1");
        EnvironmentData entity = environmentDataMapper.selectOne(wrapper);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 分页查询环境历史数据（API06）。
     *
     * @param deviceCode 设备编码，可空（空表示查全部设备）
     * @param startTime  起始时间（epoch 毫秒），可空
     * @param endTime    结束时间（epoch 毫秒），可空
     */
    public IPage<EnvironmentDataVO> history(String deviceCode, Long startTime, Long endTime,
                                            long page, long size) {
        QueryWrapper<EnvironmentData> wrapper = new QueryWrapper<>();
        if (deviceCode != null && !deviceCode.isBlank()) {
            wrapper.eq("device_code", deviceCode);
        }
        if (startTime != null) {
            wrapper.ge("collect_time", TimeUtils.toLocalDateTime(startTime));
        }
        if (endTime != null) {
            wrapper.le("collect_time", TimeUtils.toLocalDateTime(endTime));
        }
        wrapper.orderByDesc("collect_time");

        Page<EnvironmentData> pageParam = new Page<>(page, size);
        IPage<EnvironmentData> result = environmentDataMapper.selectPage(pageParam, wrapper);
        return result.convert(EnvironmentDataService::toVO);
    }

    /** 实体 -> 对外 VO（collect_time 转 epoch 毫秒） */
    static EnvironmentDataVO toVO(EnvironmentData entity) {
        EnvironmentDataVO vo = new EnvironmentDataVO();
        vo.setId(entity.getId());
        vo.setDeviceCode(entity.getDeviceCode());
        vo.setTemperature(entity.getTemperature());
        vo.setHumidity(entity.getHumidity());
        vo.setGasRaw(entity.getGasRaw());
        vo.setWater(entity.getWater());
        vo.setPirTriggered(entity.getPirTriggered());
        vo.setTimestamp(TimeUtils.toEpochMillis(entity.getCollectTime()));
        return vo;
    }
}
