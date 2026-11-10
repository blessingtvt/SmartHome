import request from './request'

/**
 * 自动化规则接口（S2 rule-service，经网关 8080）。
 * 契约已核对后端 RuleController / RuleServiceImpl / AutomationRuleVO / RuleExecutionLogVO / PageVO：
 *   GET    /api/v1/rules                             分页查询（ruleName/triggerType/enabled/pageNum/pageSize）
 *   POST   /api/v1/rules                             创建
 *   GET    /api/v1/rules/{id}                        详情
 *   PUT    /api/v1/rules/{id}                        全量修改（enabled 必填）
 *   DELETE /api/v1/rules/{id}                        删除
 *   PATCH  /api/v1/rules/{id}/enabled                启用/停用（body { enabled }）
 *   GET    /api/v1/rules/execution-logs              执行日志分页（ruleId/pageNum/pageSize）
 *
 * AutomationRuleVO 字段：id, ruleName, triggerType(TEMPERATURE/HUMIDITY/GAS_RAW/PIR/WATER),
 *   conditionConfig(对象), actionConfig(对象), enabled(布尔), createTime(epoch ms)
 * RuleExecutionLogVO 字段：id, ruleId, ruleName, triggerValue,
 *   evaluationResult(MATCHED/NOT_MATCHED/BLOCKED/ERROR),
 *   executionResult(SUCCESS/FAILED/NOT_EXECUTED),
 *   deviceCommandId, deviceExecResult(SUCCESS/FAILED/可空), failureReason, executionTime(epoch ms)
 * PageVO 字段：records, total, page, size
 */

/**
 * 分页查询规则（按 id 倒序）。
 * @param {object} params { ruleName, triggerType, enabled, pageNum, pageSize }
 * @returns {Promise<{records: AutomationRuleVO[], total: number, page: number, size: number}>}
 */
export function listRules(params = {}) {
  return request.get('/api/v1/rules', { params })
}

/**
 * 查询规则详情。
 * @param {number} id
 * @returns {Promise<AutomationRuleVO>}
 */
export function getRule(id) {
  return request.get(`/api/v1/rules/${id}`)
}

/**
 * 创建规则。
 * @param {object} data { ruleName, triggerType, conditionConfig, actionConfig, enabled? }
 * @returns {Promise<AutomationRuleVO>}
 */
export function createRule(data) {
  return request.post('/api/v1/rules', data)
}

/**
 * 全量修改规则。
 * @param {number} id
 * @param {object} data { ruleName, triggerType, conditionConfig, actionConfig, enabled }
 * @returns {Promise<AutomationRuleVO>}
 */
export function updateRule(id, data) {
  return request.put(`/api/v1/rules/${id}`, data)
}

/**
 * 删除规则（不级联清理历史执行日志）。
 * @param {number} id
 * @returns {Promise<void>}
 */
export function deleteRule(id) {
  return request.delete(`/api/v1/rules/${id}`)
}

/**
 * 启用/停用规则。
 * @param {number} id
 * @param {boolean} enabled
 * @returns {Promise<AutomationRuleVO>}
 */
export function updateRuleEnabled(id, enabled) {
  return request.patch(`/api/v1/rules/${id}/enabled`, { enabled })
}

/**
 * 分页查询规则执行日志（按 id 倒序）。
 * @param {object} params { ruleId, pageNum, pageSize }  ruleId 可空（空则查全部）
 * @returns {Promise<{records: RuleExecutionLogVO[], total: number, page: number, size: number}>}
 */
export function listRuleLogs(params = {}) {
  return request.get('/api/v1/rules/execution-logs', { params })
}
