import request from './request'

/**
 * 环境数据接口（S1 environment-service，经网关 8080）。
 * 契约已核对后端 EnvironmentController / EnvironmentDataVO / PageVO：
 *   GET /api/v1/environment/current?deviceCode=  查询设备最近一次环境数据
 *   GET /api/v1/environment/history              分页查询历史（page/size，注意非 pageNum/pageSize）
 *
 * EnvironmentDataVO 字段：id, deviceCode, temperature(数字), humidity(数字),
 *   gasRaw(整数, 可空), water(整数, 可空, 水滴感应原始值 0-4095), pirTriggered(布尔), timestamp(epoch ms)
 * PageVO 字段：records, total, page, size；history 按 collect_time 倒序返回。
 */

/**
 * 查询设备最近一次环境数据。
 * @param {string} deviceCode
 * @returns {Promise<EnvironmentDataVO|null>}
 */
export function getCurrentEnvironment(deviceCode) {
  return request.get('/api/v1/environment/current', { params: { deviceCode } })
}

/**
 * 分页查询环境历史数据（倒序）。
 * @param {object} params { deviceCode, startTime, endTime, page, size }
 * @returns {Promise<{records: EnvironmentDataVO[], total: number, page: number, size: number}>}
 */
export function getEnvironmentHistory(params = {}) {
  return request.get('/api/v1/environment/history', { params })
}
