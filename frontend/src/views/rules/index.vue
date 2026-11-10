<template>
  <div>
    <PageHeader title="自动化规则" subtitle="配置触发条件与联动动作，规则由主控板按上报数据实时评估">
      <template #extra>
        <el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建规则</el-button>
      </template>
    </PageHeader>

    <!-- 加载失败 -->
    <el-result v-if="error" icon="error" title="加载失败" sub-title="无法获取规则列表，请确认网关与规则服务已启动。">
      <template #extra>
        <el-button type="primary" @click="load()">重试</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never">
      <!-- 筛选栏 -->
      <div class="toolbar">
        <el-input
          v-model="queryName"
          placeholder="按规则名称搜索"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-select v-model="queryType" placeholder="全部类型" clearable style="width: 160px" @change="onSearch">
          <el-option v-for="(m, k) in TRIGGER_META" :key="k" :label="m.label" :value="k" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <div class="toolbar__right">
          <el-button :icon="Document" @click="openLogs(null)">执行日志</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" empty-text="暂无自动化规则">
        <el-table-column label="名称" min-width="150">
          <template #default="{ row }">
            <div class="name">{{ row.ruleName }}</div>
            <div class="cell-sub">ID {{ row.id }}</div>
          </template>
        </el-table-column>

        <el-table-column label="规则类型" width="120">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ triggerMeta(row.triggerType).label }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="条件" min-width="200">
          <template #default="{ row }">
            <span class="cond">{{ conditionText(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="动作" min-width="180">
          <template #default="{ row }">
            <span class="cond">{{ actionText(row) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="启用状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              :loading="toggling.has(row.id)"
              :disabled="toggling.has(row.id)"
              @change="(val) => onToggleEnabled(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text type="primary" @click="openLogs(row)">日志</el-button>
            <el-button size="small" text type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 新建/编辑规则 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建规则' : '编辑规则'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="96px" class="rule-form">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.ruleName" maxlength="100" placeholder="例如 temperature_fan" />
        </el-form-item>

        <el-form-item label="触发类型" required>
          <el-select v-model="form.triggerType" style="width: 100%" @change="onTypeChange">
            <el-option v-for="(m, k) in TRIGGER_META" :key="k" :label="m.label" :value="k" />
          </el-select>
        </el-form-item>

        <el-form-item label="触发条件" required>
          <div class="cond-builder">
            <el-select v-model="form.operator" style="width: 110px">
              <el-option
                v-for="op in currentOperators"
                :key="op"
                :label="OP_LABEL[op]"
                :value="op"
              />
            </el-select>

            <!-- 数值型指标 -->
            <el-input-number
              v-if="form.triggerType !== 'PIR'"
              v-model="form.threshold"
              :min="currentMeta.min"
              :max="currentMeta.max"
              :precision="currentMeta.precision"
              :step="currentMeta.step"
              controls-position="right"
              style="flex: 1"
            />
            <!-- 布尔型指标（PIR） -->
            <el-switch
              v-else
              v-model="form.pirValue"
              :active-text="'检测到活动'"
              :inactive-text="'无活动'"
              style="margin-left: 12px"
            />

            <span v-if="currentMeta.unit" class="cond-unit">{{ currentMeta.unit }}</span>
          </div>
          <div v-if="form.triggerType === 'WATER'" class="confirm-row">
            <span class="confirm-label">连续确认次数</span>
            <el-input-number v-model="form.confirmCount" :min="1" :precision="0" controls-position="right" />
            <span class="cond-hint">连续满足才触发，用于水滴去抖</span>
          </div>
        </el-form-item>

        <el-form-item label="动作类型" required>
          <el-radio-group v-model="form.actionType">
            <el-radio value="DEVICE_COMMAND">设备指令</el-radio>
            <el-radio value="ALARM">告警</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.actionType === 'DEVICE_COMMAND'" label="执行动作" required>
          <div class="cond-builder">
            <el-select v-model="form.deviceCode" placeholder="选择执行器" style="flex: 1">
              <el-option
                v-for="d in deviceOptions"
                :key="d.deviceCode"
                :label="`${d.deviceName}（${d.deviceCode}）`"
                :value="d.deviceCode"
              />
            </el-select>
            <el-select v-model="form.action" style="width: 110px">
              <el-option label="开启" value="TURN_ON" />
              <el-option label="关闭" value="TURN_OFF" />
            </el-select>
          </div>
        </el-form-item>

        <el-form-item v-else label="告警内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            maxlength="200"
            placeholder="例如：检测到持续滴水，请检查窗户是否关闭"
          />
        </el-form-item>

        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
          <span class="cond-hint" style="margin-left: 10px">停用后规则不再参与评估</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 执行日志抽屉 -->
    <el-drawer v-model="logsVisible" :title="logTitle" size="760px">
      <div class="log-filter">
        <el-select
          v-model="logRuleId"
          placeholder="全部规则"
          clearable
          style="width: 220px"
          @change="loadLogs(1)"
        >
          <el-option label="全部规则" :value="null" />
          <el-option v-for="r in list" :key="r.id" :label="r.ruleName" :value="r.id" />
        </el-select>
      </div>

      <el-table v-loading="logLoading" :data="logList" empty-text="暂无执行日志">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.executionTime) }}</template>
        </el-table-column>
        <el-table-column label="规则" min-width="120">
          <template #default="{ row }">{{ row.ruleName || '--' }}</template>
        </el-table-column>
        <el-table-column label="触发值" width="90">
          <template #default="{ row }">{{ row.triggerValue ?? '--' }}</template>
        </el-table-column>
        <el-table-column label="规则匹配结果" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="evalMeta(row.evaluationResult).tag" size="small" effect="plain">
              {{ evalMeta(row.evaluationResult).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命令创建结果" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="execMeta(row.executionResult).tag" size="small" effect="plain">
              {{ execMeta(row.executionResult).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设备实际执行结果" width="130" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.deviceExecResult" :type="deviceExecMeta(row.deviceExecResult).tag" size="small" effect="plain">
              {{ deviceExecMeta(row.deviceExecResult).label }}
            </el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.failureReason || '--' }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="logPage"
          v-model:page-size="logPageSize"
          :total="logTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadLogs"
          @size-change="() => loadLogs(1)"
        />
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="三层结果说明"
        description="规则匹配结果：条件是否命中；命令创建结果：指令/告警是否成功创建；设备实际执行结果：设备回报的实际执行状态（未回填时显示 —）。"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { listRules, createRule, updateRule, deleteRule, updateRuleEnabled, listRuleLogs } from '@/api/rule'
import { getDevices } from '@/api/device'
import { formatDateTime } from '@/utils/format'

// —— 规则类型 / 触发指标元数据（与后端 RuleConfigValidator 对齐）——
const TRIGGER_META = {
  TEMPERATURE: { label: '温度', metric: 'temperature', unit: '℃', min: -40, max: 80, precision: 1, step: 0.5 },
  HUMIDITY: { label: '湿度', metric: 'humidity', unit: '%', min: 0, max: 100, precision: 1, step: 1 },
  GAS_RAW: { label: '燃气传感器', metric: 'gasRaw', unit: '', min: 0, max: 4095, precision: 0, step: 1 },
  PIR: { label: '人体活动', metric: 'pirTriggered', unit: '', min: null, max: null, precision: 0, step: 1 },
  WATER: { label: '水滴', metric: 'water', unit: '', min: 0, max: 4095, precision: 0, step: 1 },
}
const NUMERIC_OPERATORS = ['>', '<', '>=', '<=', '==']
const BOOLEAN_OPERATORS = ['==', '!=']
const OP_LABEL = { '>': '大于', '<': '小于', '>=': '大于等于', '<=': '小于等于', '==': '等于', '!=': '不等于' }
const ACTION_LABEL = { TURN_ON: '开启', TURN_OFF: '关闭' }

const EVAL_META = {
  MATCHED: { label: '命中', tag: 'success' },
  NOT_MATCHED: { label: '未命中', tag: 'info' },
  BLOCKED: { label: '已拦截', tag: 'warning' },
  ERROR: { label: '错误', tag: 'danger' },
}
const EXEC_META = {
  SUCCESS: { label: '已创建', tag: 'success' },
  FAILED: { label: '创建失败', tag: 'danger' },
  NOT_EXECUTED: { label: '未执行', tag: 'info' },
}
const DEVICE_EXEC_META = {
  SUCCESS: { label: '成功', tag: 'success' },
  FAILED: { label: '失败', tag: 'danger' },
}

function triggerMeta(t) { return TRIGGER_META[t] || { label: t || '--' } }
function evalMeta(s) { return EVAL_META[s] || { label: s || '--', tag: 'info' } }
function execMeta(s) { return EXEC_META[s] || { label: s || '--', tag: 'info' } }
function deviceExecMeta(s) { return DEVICE_EXEC_META[s] || { label: s || '--', tag: 'info' } }

// —— 列表状态 ——
const list = ref([])
const loading = ref(false)
const error = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const queryName = ref('')
const queryType = ref('')
const toggling = ref(new Set())

// —— 执行器下拉 ——
const deviceOptions = ref([])

// —— 新建/编辑表单 ——
const dialogVisible = ref(false)
const dialogMode = ref('create')
const submitting = ref(false)
const form = reactive({
  id: null,
  ruleName: '',
  triggerType: 'TEMPERATURE',
  operator: '>',
  threshold: null,
  pirValue: true,
  confirmCount: 3,
  actionType: 'DEVICE_COMMAND',
  deviceCode: '',
  action: 'TURN_ON',
  content: '',
  enabled: true,
})

// —— 日志抽屉 ——
const logsVisible = ref(false)
const logList = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const logPageSize = ref(10)
const logLoading = ref(false)
const logRuleId = ref(null)

const currentMeta = computed(() => triggerMeta(form.triggerType))
const currentOperators = computed(() => (form.triggerType === 'PIR' ? BOOLEAN_OPERATORS : NUMERIC_OPERATORS))
const logTitle = computed(() => (logRuleId.value ? `执行日志 · ${ruleNameOf(logRuleId.value)}` : '执行日志'))

function ruleNameOf(id) {
  const r = list.value.find((x) => x.id === id)
  return r ? r.ruleName : ''
}
function deviceNameOf(code) {
  const d = deviceOptions.value.find((x) => x.deviceCode === code)
  return d ? d.deviceName : code
}

// —— 列表加载 ——
async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const res = await listRules({
      ruleName: queryName.value || undefined,
      triggerType: queryType.value || undefined,
      pageNum: page.value,
      pageSize: pageSize.value,
    })
    list.value = res.records || []
    total.value = res.total || 0
    error.value = false
  } catch (e) {
    if (!silent) error.value = true
  } finally {
    if (!silent) loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}
function onSizeChange() {
  page.value = 1
  load()
}

async function loadDevices() {
  try {
    const devices = await getDevices()
    deviceOptions.value = (devices || []).filter((d) => String(d.deviceType).toUpperCase() !== 'CONTROLLER')
  } catch (e) {
    deviceOptions.value = []
  }
}

// —— 条件 / 动作展示 ——
function conditionText(rule) {
  const c = rule.conditionConfig || {}
  const meta = triggerMeta(rule.triggerType)
  const op = OP_LABEL[c.operator] || c.operator || '--'
  if (rule.triggerType === 'PIR') {
    const v = c.threshold === true ? '是' : c.threshold === false ? '否' : '--'
    return `${meta.label} ${op} ${v}`
  }
  let s = `${meta.label} ${op} ${c.threshold ?? '--'}`
  if (meta.unit) s += meta.unit
  if (rule.triggerType === 'WATER' && c.confirmCount) s += `（连续 ${c.confirmCount} 次）`
  return s
}

function actionText(rule) {
  const a = rule.actionConfig || {}
  if (a.actionType === 'ALARM') return `告警：${a.content || '--'}`
  const act = ACTION_LABEL[a.action] || a.action || '--'
  return `${deviceNameOf(a.deviceCode)} ${act}`
}

// —— 新建/编辑 ——
async function openCreate() {
  dialogMode.value = 'create'
  if (!deviceOptions.value.length) await loadDevices()
  dialogVisible.value = true
}

function openEdit(row) {
  dialogMode.value = 'edit'
  fillForm(row)
  if (!deviceOptions.value.length) loadDevices()
  dialogVisible.value = true
}

function fillForm(rule) {
  const c = rule.conditionConfig || {}
  const a = rule.actionConfig || {}
  form.id = rule.id
  form.ruleName = rule.ruleName
  form.triggerType = rule.triggerType
  form.operator = c.operator || (rule.triggerType === 'PIR' ? '==' : '>')
  form.pirValue = c.threshold === true
  form.threshold = rule.triggerType === 'PIR' ? null : c.threshold ?? null
  form.confirmCount = c.confirmCount ?? 3
  form.actionType = a.actionType || 'DEVICE_COMMAND'
  form.deviceCode = a.deviceCode || ''
  form.action = a.action || 'TURN_ON'
  form.content = a.content || ''
  form.enabled = rule.enabled
}

function onTypeChange() {
  // 布尔型与数值型的运算符集合不同，切换后回落到合法默认值
  form.operator = form.triggerType === 'PIR' ? '==' : '>'
  form.threshold = null
  form.pirValue = true
  if (form.triggerType !== 'WATER') form.confirmCount = 3
}

function resetForm() {
  Object.assign(form, {
    id: null,
    ruleName: '',
    triggerType: 'TEMPERATURE',
    operator: '>',
    threshold: null,
    pirValue: true,
    confirmCount: 3,
    actionType: 'DEVICE_COMMAND',
    deviceCode: '',
    action: 'TURN_ON',
    content: '',
    enabled: true,
  })
}

function buildCondition() {
  const meta = currentMeta.value
  const config = { metric: meta.metric, operator: form.operator }
  if (form.triggerType === 'PIR') {
    config.threshold = !!form.pirValue
  } else {
    config.threshold = form.threshold
  }
  if (form.triggerType === 'WATER') {
    config.confirmCount = form.confirmCount
  }
  return config
}

function buildAction() {
  if (form.actionType === 'DEVICE_COMMAND') {
    return { actionType: 'DEVICE_COMMAND', deviceCode: form.deviceCode, action: form.action }
  }
  return { actionType: 'ALARM', content: form.content }
}

function validateForm() {
  if (!form.ruleName.trim()) return '请填写规则名称'
  if (form.triggerType !== 'PIR' && (form.threshold === null || form.threshold === undefined || form.threshold === '')) {
    return '请填写触发阈值'
  }
  if (form.actionType === 'DEVICE_COMMAND' && !form.deviceCode) return '请选择执行设备'
  if (form.actionType === 'ALARM' && !form.content.trim()) return '请填写告警内容'
  return null
}

async function submit() {
  const msg = validateForm()
  if (msg) {
    ElMessage.warning(msg)
    return
  }
  const payload = {
    ruleName: form.ruleName.trim(),
    triggerType: form.triggerType,
    conditionConfig: buildCondition(),
    actionConfig: buildAction(),
    enabled: form.enabled,
  }
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createRule(payload)
      ElMessage.success('创建成功')
    } else {
      await updateRule(form.id, payload)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    /* 拦截器已弹出后端校验错误 */
  } finally {
    submitting.value = false
  }
}

// —— 启用/停用 ——
async function onToggleEnabled(row, val) {
  const prev = row.enabled
  row.enabled = val
  toggling.value = new Set([...toggling.value, row.id])
  try {
    await updateRuleEnabled(row.id, val)
    ElMessage.success(val ? '已启用' : '已停用')
  } catch (e) {
    row.enabled = prev
  } finally {
    const s = new Set(toggling.value)
    s.delete(row.id)
    toggling.value = s
  }
}

// —— 删除 ——
async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除规则「${row.ruleName}」吗？历史执行日志会保留。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteRule(row.id)
    ElMessage.success('删除成功')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    load()
  } catch (e) {
    /* 拦截器已弹出错误 */
  }
}

// —— 执行日志 ——
function openLogs(rule) {
  logRuleId.value = rule ? rule.id : null
  logsVisible.value = true
  loadLogs(1)
}

async function loadLogs(p) {
  logPage.value = p || logPage.value
  logLoading.value = true
  try {
    const res = await listRuleLogs({
      ruleId: logRuleId.value || undefined,
      pageNum: logPage.value,
      pageSize: logPageSize.value,
    })
    logList.value = res.records || []
    logTotal.value = res.total || 0
  } catch (e) {
    logList.value = []
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  load()
  loadDevices()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar__right {
  margin-left: auto;
}

.name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.cell-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #a8abb2;
}

.cond {
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.rule-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}

.cond-builder {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.cond-unit {
  font-size: 13px;
  color: #909399;
  min-width: 20px;
}

.confirm-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.confirm-label {
  font-size: 13px;
  color: #606266;
}

.cond-hint {
  font-size: 12px;
  color: #a8abb2;
}

.log-filter {
  margin-bottom: 16px;
}

.muted {
  color: #c0c4cc;
}
</style>
