import request from './request'

/**
 * 设备管理接口（S3 device-service，经网关 8080）。
 * 契约已核对后端 DeviceController / DeviceCommandController / DeviceVO / DeviceCommandVO：
 *   GET  /api/v1/devices                        设备列表（不分页）
 *   POST /api/v1/devices/{id}/commands          家属远程控制（body { action }）
 *   GET  /api/v1/device-commands?deviceCode=&size=  设备指令历史（按 create_time 倒序，新增接口）
 *
 * DeviceVO 字段：id, deviceCode, deviceName, deviceType, roomId,
 *   onlineStatus(ONLINE/OFFLINE), deviceStatus(ON/OFF),
 *   createTime(epoch ms), lastHeartbeatTime(epoch ms, 可为 null)
 *
 * 注意：deviceStatus 为创建时初始值 OFF，且后端不回写（指令反馈只更新 device_command），
 * 不能用于表示执行器真实运行状态。执行器运行状态须由 device_command 历史中「最近一次 SUCCESS 指令」推导。
 *
 * DeviceCommandVO 字段：id, commandCode, deviceCode, action(TURN_ON/TURN_OFF),
 *   source(REMOTE/RULE/AUTO_RULE), status(PENDING/SUCCESS/FAILED),
 *   resultMessage, createTime(ms), finishTime(ms, 可为 null)
 */
export function getDevices() {
  return request.get('/api/v1/devices')
}

/**
 * 查询设备指令历史（按 create_time 倒序，最多 size 条）。
 * @param {string} deviceCode
 * @param {number} size
 * @returns {Promise<DeviceCommandVO[]>}
 */
export function getDeviceCommands(deviceCode, size = 50) {
  return request.get('/api/v1/device-commands', { params: { deviceCode, size } })
}

/**
 * 下发远程控制指令（返回 PENDING 指令，设备异步执行后回报结果）。
 * @param {number} id 设备主键
 * @param {'TURN_ON'|'TURN_OFF'} action
 * @returns {Promise<DeviceCommandVO>}
 */
export function sendDeviceCommand(id, action) {
  return request.post(`/api/v1/devices/${id}/commands`, { action })
}
