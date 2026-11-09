package com.smarthome.alarm.service;

import com.smarthome.alarm.dto.AlarmCreateRequest;
import com.smarthome.alarm.vo.AlarmVO;

/**
 * 告警业务逻辑接口（S4）。
 */
public interface AlarmService {

    /**
     * INT03：内部创建环境异常告警（ENV_ABNORMAL）。
     *
     * @param request 告警创建请求（deviceCode / content 必填）
     * @return 创建后的告警
     */
    AlarmVO createEnvAbnormal(AlarmCreateRequest request);
}
