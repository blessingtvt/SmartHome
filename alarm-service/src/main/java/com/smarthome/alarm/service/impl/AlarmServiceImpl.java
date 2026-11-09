package com.smarthome.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smarthome.alarm.dto.AlarmCreateRequest;
import com.smarthome.alarm.dto.AlarmHandleRequest;
import com.smarthome.alarm.dto.AlarmHelpRequest;
import com.smarthome.alarm.entity.Alarm;
import com.smarthome.alarm.mapper.AlarmMapper;
import com.smarthome.alarm.service.AlarmService;
import com.smarthome.alarm.vo.AlarmVO;
import com.smarthome.common.constant.CommonConstant;
import com.smarthome.common.exception.BusinessException;
import com.smarthome.common.result.ResultCode;
import com.smarthome.common.utils.TimeUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 告警业务逻辑实现（S4）。
 *
 * <p>状态流转仅允许 PENDING->PROCESSING、PROCESSING->PROCESSED、PENDING->PROCESSED；
 * 更新采用条件 UPDATE（WHERE status=当前值）保证并发安全，已处理完成的告警不可被覆盖。</p>
 */
@Service
public class AlarmServiceImpl implements AlarmService {

    /** alarm_code 唯一键冲突时最大重试次数 */
    private static final int MAX_ALARM_CODE_RETRY = 3;

    /** 一键求助默认内容 */
    private static final String DEFAULT_HELP_CONTENT = "老人一键求助";

    private final AlarmMapper alarmMapper;

    public AlarmServiceImpl(AlarmMapper alarmMapper) {
        this.alarmMapper = alarmMapper;
    }

    @Override
    public AlarmVO createEnvAbnormal(AlarmCreateRequest request) {
        Alarm alarm = new Alarm();
        alarm.setAlarmType(CommonConstant.ALARM_ENV_ABNORMAL);
        alarm.setDeviceCode(request.getDeviceCode());
        alarm.setAlarmContent(request.getContent());
        alarm.setAlarmTime(TimeUtils.now());
        alarm.setStatus(CommonConstant.ALARM_STATUS_PENDING);
        insertWithUniqueCode(alarm);
        return toVO(alarm);
    }

    @Override
    public AlarmVO createManualHelp(AlarmHelpRequest request) {
        Alarm alarm = new Alarm();
        alarm.setAlarmType(CommonConstant.ALARM_MANUAL_HELP);
        alarm.setDeviceCode(request.getDeviceCode());
        String content = request.getContent();
        alarm.setAlarmContent(content == null || content.isBlank() ? DEFAULT_HELP_CONTENT : content);
        alarm.setAlarmTime(TimeUtils.now());
        alarm.setStatus(CommonConstant.ALARM_STATUS_PENDING);
        insertWithUniqueCode(alarm);
        return toVO(alarm);
    }

    @Override
    public AlarmVO updateStatus(Long alarmId, AlarmHandleRequest request) {
        String target = request.getStatus();
        if (!CommonConstant.ALARM_STATUS_PROCESSING.equals(target)
                && !CommonConstant.ALARM_STATUS_PROCESSED.equals(target)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "处理状态仅支持 PROCESSING/PROCESSED");
        }

        Alarm alarm = alarmMapper.selectById(alarmId);
        if (alarm == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "告警不存在: alarmId=" + alarmId);
        }

        String current = alarm.getStatus();
        requireValidTransition(current, target);

        UpdateWrapper<Alarm> update = new UpdateWrapper<Alarm>()
                .set("status", target)
                .eq("id", alarmId)
                .eq("status", current);
        if (CommonConstant.ALARM_STATUS_PROCESSED.equals(target)) {
            update.set("handle_time", TimeUtils.now());
        }
        int updated = alarmMapper.update(null, update);
        if (updated == 0) {
            // 并发下状态已被抢先更新，读回当前状态给出明确错误，不覆盖最终状态
            Alarm latest = alarmMapper.selectById(alarmId);
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "告警状态已变更（当前 " + latest.getStatus() + "），无法流转为 " + target);
        }
        return toVO(alarmMapper.selectById(alarmId));
    }

    @Override
    public IPage<AlarmVO> page(String alarmType, String status, String deviceCode, long pageNum, long pageSize) {
        if (alarmType != null && !alarmType.isBlank()) {
            requireValidAlarmType(alarmType);
        }
        if (status != null && !status.isBlank()) {
            requireValidStatus(status);
        }

        QueryWrapper<Alarm> wrapper = new QueryWrapper<>();
        if (alarmType != null && !alarmType.isBlank()) {
            wrapper.eq("alarm_type", alarmType);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (deviceCode != null && !deviceCode.isBlank()) {
            wrapper.eq("device_code", deviceCode);
        }
        wrapper.orderByDesc("alarm_time");

        Page<Alarm> pageParam = new Page<>(pageNum, pageSize);
        IPage<Alarm> result = alarmMapper.selectPage(pageParam, wrapper);
        return result.convert(AlarmServiceImpl::toVO);
    }

    /** 校验合法状态流转：PENDING->PROCESSING、PROCESSING->PROCESSED、PENDING->PROCESSED */
    private void requireValidTransition(String current, String target) {
        boolean valid;
        if (CommonConstant.ALARM_STATUS_PROCESSING.equals(target)) {
            valid = CommonConstant.ALARM_STATUS_PENDING.equals(current);
        } else {
            valid = CommonConstant.ALARM_STATUS_PENDING.equals(current)
                    || CommonConstant.ALARM_STATUS_PROCESSING.equals(current);
        }
        if (!valid) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "非法状态跳转: " + current + " -> " + target);
        }
    }

    /** 校验告警类型筛选值合法 */
    private void requireValidAlarmType(String alarmType) {
        if (!CommonConstant.ALARM_ENV_ABNORMAL.equals(alarmType)
                && !CommonConstant.ALARM_MANUAL_HELP.equals(alarmType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "告警类型仅支持 ENV_ABNORMAL/MANUAL_HELP");
        }
    }

    /** 校验告警状态筛选值合法 */
    private void requireValidStatus(String status) {
        if (!CommonConstant.ALARM_STATUS_PENDING.equals(status)
                && !CommonConstant.ALARM_STATUS_PROCESSING.equals(status)
                && !CommonConstant.ALARM_STATUS_PROCESSED.equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "告警状态仅支持 PENDING/PROCESSING/PROCESSED");
        }
    }

    /** 插入告警，alarm_code 唯一键冲突时有限次重新生成 */
    private void insertWithUniqueCode(Alarm alarm) {
        for (int i = 0; i < MAX_ALARM_CODE_RETRY; i++) {
            alarm.setAlarmCode(generateAlarmCode());
            try {
                alarmMapper.insert(alarm);
                return;
            } catch (DuplicateKeyException e) {
                if (i == MAX_ALARM_CODE_RETRY - 1) {
                    throw e;
                }
            }
        }
    }

    /** 生成唯一告警编码：ALM + 毫秒时间戳 + UUID 随机片段 */
    private String generateAlarmCode() {
        String uuidFragment = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "ALM" + System.currentTimeMillis() + uuidFragment;
    }

    /** 实体 -> 对外 VO（时间转 epoch 毫秒） */
    static AlarmVO toVO(Alarm entity) {
        AlarmVO vo = new AlarmVO();
        vo.setId(entity.getId());
        vo.setAlarmCode(entity.getAlarmCode());
        vo.setAlarmType(entity.getAlarmType());
        vo.setDeviceCode(entity.getDeviceCode());
        vo.setAlarmContent(entity.getAlarmContent());
        vo.setAlarmTime(TimeUtils.toEpochMillis(entity.getAlarmTime()));
        vo.setStatus(entity.getStatus());
        vo.setHandleTime(entity.getHandleTime() == null ? null : TimeUtils.toEpochMillis(entity.getHandleTime()));
        return vo;
    }
}
