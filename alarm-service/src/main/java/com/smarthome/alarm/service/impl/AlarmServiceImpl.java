package com.smarthome.alarm.service.impl;

import com.smarthome.alarm.dto.AlarmCreateRequest;
import com.smarthome.alarm.entity.Alarm;
import com.smarthome.alarm.mapper.AlarmMapper;
import com.smarthome.alarm.service.AlarmService;
import com.smarthome.alarm.vo.AlarmVO;
import com.smarthome.common.constant.CommonConstant;
import com.smarthome.common.utils.TimeUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 告警业务逻辑实现（S4）。
 */
@Service
public class AlarmServiceImpl implements AlarmService {

    /** alarm_code 唯一键冲突时最大重试次数 */
    private static final int MAX_ALARM_CODE_RETRY = 3;

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
