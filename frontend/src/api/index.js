/**
 * 家属端 API 集中定义（占位）。
 *
 * 阶段七当前只做基础工程，页面使用模拟数据 / 空状态，暂不真正调用接口。
 * 正式对接前必须先逐个核对后端 Controller / DTO / VO，禁止凭猜测调用，禁止自造不存在的接口。
 *
 * 已核实（来自后端 Controller @RequestMapping/@*Mapping）的接口路径，统一经网关 8080：
 *  环境：   POST /api/v1/environment/readings         上报环境数据
 *           GET  /api/v1/environment/current?deviceCode= 当前环境
 *           GET  /api/v1/environment/history          历史环境
 *  家庭/房间：/api/v1/families、/api/v1/rooms 的 CRUD
 *  设备：   GET/POST /api/v1/devices
 *           POST /api/v1/devices/{deviceCode}/heartbeat  设备心跳
 *           POST /api/v1/devices/{id}/commands           家属远程控制
 *  指令：   GET  /api/v1/device-commands/pending?deviceCode= 待执行指令
 *           POST /api/v1/device-commands/{commandId}/result 指令结果反馈
 *  告警：   GET  /api/v1/alarms（分页）
 *           PATCH /api/v1/alarms/{alarmId}/status        状态更新
 *           POST /api/v1/alarms/help                     一键求助
 *  规则：   /api/v1/rules 的 CRUD + PATCH /api/v1/rules/{id}/enabled
 */
import request from './request'

// 各业务模块 API 拆分为子文件（device.js 等），这里统一再导出。
export { getDevices } from './device'

export { request }
