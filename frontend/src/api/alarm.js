import request from './request'

/**
 * 告警管理接口（S4 alarm-service，经网关 8080）。
 * 契约已核对后端 AlarmController / AlarmVO / PageVO：
 *   GET   /api/v1/alarms                     分页查询（alarmType/status/deviceCode/pageNum/pageSize）
 *   PATCH /api/v1/alarms/{alarmId}/status    状态更新（PROCESSING / PROCESSED）
 *
 * AlarmVO 字段：id, alarmCode, alarmType(ENV_ABNORMAL/MANUAL_HELP), deviceCode,
 *   alarmContent, alarmTime(epoch ms), status(PENDING/PROCESSING/PROCESSED),
 *   handleTime(epoch ms, 可为 null)
 * PageVO 字段：records, total, page, size
 */

/**
 * 分页查询告警（按 alarm_time 倒序）。
 * @param {object} params { alarmType, status, deviceCode, pageNum, pageSize }
 * @returns {Promise<{records: AlarmVO[], total: number, page: number, size: number}>}
 */
export function getAlarms(params = {}) {
  return request.get('/api/v1/alarms', { params })
}

/**
 * 更新告警处理状态。
 * @param {number} alarmId 告警主键
 * @param {'PROCESSING'|'PROCESSED'} status 目标状态
 * @returns {Promise<AlarmVO>}
 */
export function updateAlarmStatus(alarmId, status) {
  return request.patch(`/api/v1/alarms/${alarmId}/status`, { status })
}
