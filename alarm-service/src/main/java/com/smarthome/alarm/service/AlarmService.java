package com.smarthome.alarm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smarthome.alarm.dto.AlarmCreateRequest;
import com.smarthome.alarm.dto.AlarmHandleRequest;
import com.smarthome.alarm.dto.AlarmHelpRequest;
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

    /**
     * API04：老人一键求助（MANUAL_HELP）。
     *
     * @param request 求助请求（deviceCode / content 可空）
     * @return 创建后的告警
     */
    AlarmVO createManualHelp(AlarmHelpRequest request);

    /**
     * API10：更新告警处理状态。
     *
     * @param alarmId 告警主键
     * @param request 目标状态（PROCESSING / PROCESSED）
     * @return 更新后的告警
     */
    AlarmVO updateStatus(Long alarmId, AlarmHandleRequest request);

    /**
     * API09：分页查询告警（按 alarm_time 倒序）。
     *
     * @param alarmType  告警类型（可空）
     * @param status     处理状态（可空）
     * @param deviceCode 设备编码（可空）
     * @param pageNum    页码（从 1 开始）
     * @param pageSize   每页条数
     * @return 分页结果
     */
    IPage<AlarmVO> page(String alarmType, String status, String deviceCode, long pageNum, long pageSize);
}
