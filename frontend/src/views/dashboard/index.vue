<template>
  <div>
    <PageHeader title="家庭总览" subtitle="汇总环境、主控、设备、告警与自动化运行状态">
      <template #extra>
        <span v-if="lastUpdated" class="head-updated">数据更新于 {{ fmtClock(lastUpdated) }}</span>
        <el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHeader>

    <!-- 加载失败 -->
    <el-result v-if="error" icon="error" title="加载失败" sub-title="无法获取总览数据，请确认网关与后端服务已启动。">
      <template #extra>
        <el-button type="primary" @click="load()">重试</el-button>
      </template>
    </el-result>

    <template v-else>
      <!-- 第一行：环境概况 + 主控状态 -->
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-head">
                <span class="panel-title">环境概况</span>
                <el-button text type="primary" size="small" @click="go('/environment')">查看详情</el-button>
              </div>
            </template>
            <div v-if="env" class="env-grid">
              <div class="env-item">
                <div class="env-item__value">{{ fmtTemp }}</div>
                <div class="env-item__label">温度</div>
              </div>
              <div class="env-item">
                <div class="env-item__value">{{ fmtHumidity }}</div>
                <div class="env-item__label">湿度</div>
              </div>
              <div class="env-item">
                <div class="env-item__value">
                  <el-tag :type="pirState().tag" size="small" effect="light">{{ pirState().label }}</el-tag>
                </div>
                <div class="env-item__label">人体活动</div>
              </div>
              <div class="env-item">
                <div class="env-item__value">{{ waterText }}</div>
                <div class="env-item__label">水滴原始值</div>
              </div>
            </div>
            <el-empty v-else description="暂无环境数据" :image-size="60" />
            <div class="panel-foot">
              采集时间 {{ env?.timestamp ? formatDateTime(env.timestamp) : '--' }}
            </div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-head">
                <span class="panel-title">主控状态</span>
                <el-button text type="primary" size="small" @click="go('/devices')">查看详情</el-button>
              </div>
            </template>
            <div v-if="controller" class="ctrl-body">
              <div class="ctrl-main">
                <div class="ctrl-icon" :style="{ background: controllerOnline() ? '#f0f9eb' : '#fef0f0', color: controllerOnline() ? '#67c23a' : '#f56c6c' }">
                  <el-icon :size="26"><component :is="'Cpu'" /></el-icon>
                </div>
                <div class="ctrl-meta">
                  <div class="ctrl-name">{{ controller.deviceName }}</div>
                  <code class="mono">{{ controller.deviceCode }}</code>
                </div>
                <el-tag :type="controllerOnline() ? 'success' : 'danger'" size="small" effect="dark">
                  {{ controllerOnline() ? '在线' : '离线' }}
                </el-tag>
              </div>
              <div class="ctrl-row">
                <span class="ctrl-row__label">最近心跳</span>
                <span class="ctrl-row__value">{{ controller.lastHeartbeatTime ? formatDateTime(controller.lastHeartbeatTime) : '从未' }}</span>
              </div>
            </div>
            <el-empty v-else description="未找到主控板" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 第二行：设备概况 + 安全告警 -->
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-head">
                <span class="panel-title">设备概况</span>
                <el-button text type="primary" size="small" @click="go('/devices')">查看详情</el-button>
              </div>
            </template>
            <div v-if="actuators.length" class="dev-list">
              <div v-for="d in actuators" :key="d.deviceCode" class="dev-row">
                <div class="dev-row__icon" :style="iconStyle(d.deviceType)">
                  <el-icon :size="16"><component :is="typeMeta(d.deviceType).icon" /></el-icon>
                </div>
                <span class="dev-row__name">{{ d.deviceName }}</span>
                <code class="mono">{{ d.deviceCode }}</code>
                <el-tag :type="runState(d.deviceCode).tag" size="small" effect="plain">
                  {{ runState(d.deviceCode).label }}
                </el-tag>
              </div>
            </div>
            <el-empty v-else description="暂无设备" :image-size="60" />
            <div class="panel-foot">执行器状态由最近一次成功指令反馈推导，「未知」表示尚未获得有效反馈</div>
          </el-card>
        </el-col>

        <el-col :xs="24" :md="12">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-head">
                <span class="panel-title">安全告警</span>
                <el-button text type="primary" size="small" @click="go('/alarms')">查看详情</el-button>
              </div>
            </template>
            <div class="alarm-count">
              <span class="alarm-count__num">{{ pendingAlarmCount }}</span>
              <span class="alarm-count__label">待处理告警</span>
            </div>
            <div v-if="recentAlarms.length" class="mini-list">
              <div v-for="a in recentAlarms" :key="a.id" class="mini-item">
                <span class="mini-item__dot" :style="{ background: alarmDot(a.alarmType) }"></span>
                <div class="mini-item__body">
                  <div class="mini-item__title">{{ a.alarmContent || typeLabel(a.alarmType) }}</div>
                  <div class="mini-item__meta">{{ formatDateTime(a.alarmTime) }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无告警" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 第三行：自动化概况 -->
      <el-row :gutter="16">
        <el-col :xs="24">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-head">
                <span class="panel-title">自动化概况</span>
                <el-button text type="primary" size="small" @click="go('/rules')">查看详情</el-button>
              </div>
            </template>
            <div class="auto-summary">
              <div class="auto-count">
                <span class="auto-count__num">{{ enabledRuleCount }}</span>
                <span class="auto-count__label">启用规则</span>
              </div>
              <div class="auto-logs">
                <div v-if="recentRuleLogs.length" class="mini-list">
                  <div v-for="l in recentRuleLogs" :key="l.id" class="mini-item">
                    <span class="mini-item__dot" :style="{ background: evalDot(l.evaluationResult) }"></span>
                    <div class="mini-item__body">
                      <div class="mini-item__title">{{ l.ruleName || `规则 #${l.ruleId}` }}</div>
                      <div class="mini-item__meta">{{ formatDateTime(l.executionTime) }}</div>
                    </div>
                    <el-tag :type="evalMeta(l.evaluationResult).tag" size="small" effect="plain">
                      {{ evalMeta(l.evaluationResult).label }}
                    </el-tag>
                  </div>
                </div>
                <el-empty v-else description="暂无执行记录" :image-size="60" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第四行：最近动态 -->
      <el-row :gutter="16">
        <el-col :xs="24">
          <el-card shadow="never" class="panel">
            <template #header>
              <div class="panel-head">
                <span class="panel-title">最近动态</span>
                <span class="panel-sub">告警 · 设备控制 · 自动化执行</span>
              </div>
            </template>
            <div v-if="activity.length" class="timeline">
              <div v-for="it in activity" :key="it.key" class="tl-item">
                <span class="tl-item__dot" :style="{ background: it.dot }"></span>
                <div class="tl-item__body">
                  <div class="tl-item__head">
                    <el-tag :type="it.tag" size="small" effect="plain">{{ it.typeLabel }}</el-tag>
                    <span class="tl-item__title">{{ it.title }}</span>
                  </div>
                  <div class="tl-item__meta">{{ it.sub }} · {{ formatDateTime(it.time) }}</div>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无动态" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getDevices, getDeviceCommands } from '@/api/device'
import { getCurrentEnvironment } from '@/api/environment'
import { getAlarms } from '@/api/alarm'
import { listRules, listRuleLogs } from '@/api/rule'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

// —— 元数据 ——
const DEVICE_TYPE_META = {
  CONTROLLER: { label: '主控板', icon: 'Cpu' },
  FAN: { label: '风扇', icon: 'WindPower' },
  WINDOW: { label: '窗户', icon: 'Grid' },
  DOOR: { label: '门', icon: 'Lock' },
  LED: { label: '照明灯', icon: 'Lightning' },
}
const ACTION_LABEL = { TURN_ON: '开启', TURN_OFF: '关闭' }
const SOURCE_LABEL = { REMOTE: '家属远程', RULE: '规则联动', AUTO_RULE: '自动规则' }
const CMD_STATUS_META = {
  PENDING: { label: '执行中', tag: 'warning' },
  SUCCESS: { label: '成功', tag: 'success' },
  FAILED: { label: '失败', tag: 'danger' },
}
const ALARM_TYPE_META = {
  MANUAL_HELP: { label: '一键求助', tag: 'danger', dot: '#f56c6c' },
  ENV_ABNORMAL: { label: '环境异常', tag: 'warning', dot: '#e6a23c' },
}
const EVAL_META = {
  MATCHED: { label: '命中', tag: 'success', dot: '#67c23a' },
  NOT_MATCHED: { label: '未命中', tag: 'info', dot: '#c0c4cc' },
  BLOCKED: { label: '已拦截', tag: 'warning', dot: '#e6a23c' },
  ERROR: { label: '错误', tag: 'danger', dot: '#f56c6c' },
}
const ACTIVITY_TYPE = {
  alarm: { label: '告警', dot: '#f56c6c' },
  command: { label: '设备控制', dot: '#409eff' },
  rule: { label: '自动化', dot: '#8e6bc1' },
}

function typeMeta(t) { return DEVICE_TYPE_META[t] || { label: t || '--', icon: 'Monitor' } }
function typeLabel(t) { return ALARM_TYPE_META[t]?.label || t || '--' }
function alarmDot(t) { return ALARM_TYPE_META[t]?.dot || '#c0c4cc' }
function evalMeta(s) { return EVAL_META[s] || { label: s || '--', tag: 'info', dot: '#c0c4cc' } }
function evalDot(s) { return EVAL_META[s]?.dot || '#c0c4cc' }

// —— 状态 ——
const devices = ref([])
const env = ref(null)
const actuatorStates = ref({}) // code -> { latest, lastSuccess }
const pendingAlarmCount = ref(0)
const recentAlarms = ref([])
const enabledRuleCount = ref(0)
const recentRuleLogs = ref([])
const recentCommands = ref([])
const loading = ref(false)
const error = ref(false)
const lastUpdated = ref(null)

let refreshTimer = null

// —— 派生 ——
const controller = computed(() => devices.value.find((d) => String(d.deviceType).toUpperCase() === 'CONTROLLER') || null)
const actuators = computed(() => devices.value.filter((d) => String(d.deviceType).toUpperCase() !== 'CONTROLLER'))

function deviceNameOf(code) {
  const d = devices.value.find((x) => x.deviceCode === code)
  return d ? d.deviceName : code
}

function controllerOnline() {
  return controller.value && String(controller.value.onlineStatus).toUpperCase() === 'ONLINE'
}

function runState(code) {
  const s = actuatorStates.value[code]
  if (s?.lastSuccess) {
    return s.lastSuccess.action === 'TURN_ON'
      ? { label: '开启', tag: 'success' }
      : { label: '关闭', tag: 'info' }
  }
  return { label: '未知', tag: 'warning' }
}

function iconStyle(type) {
  const palette = {
    CONTROLLER: { bg: '#ecf5ff', color: '#409eff' },
    FAN: { bg: '#f0f9eb', color: '#67c23a' },
    WINDOW: { bg: '#fdf6ec', color: '#e6a23c' },
    DOOR: { bg: '#fef0f0', color: '#f56c6c' },
    LED: { bg: '#f5f7fa', color: '#909399' },
  }
  const p = palette[type] || palette.CONTROLLER
  return { background: p.bg, color: p.color }
}

// —— 环境格式化 ——
const fmtTemp = computed(() => (env.value?.temperature != null ? `${env.value.temperature} ℃` : '--'))
const fmtHumidity = computed(() => (env.value?.humidity != null ? `${env.value.humidity} %` : '--'))
const waterText = computed(() => (env.value?.water != null ? String(env.value.water) : '--'))
function pirState() {
  const v = env.value?.pirTriggered
  if (v === true) return { label: '检测到活动', tag: 'danger' }
  if (v === false) return { label: '无活动', tag: 'info' }
  return { label: '未知', tag: 'info' }
}

// —— 最近动态合并 ——
const activity = computed(() => {
  const items = []
  recentAlarms.value.forEach((a) => items.push({
    key: `a-${a.id}`,
    type: 'alarm',
    time: a.alarmTime,
    title: a.alarmContent || typeLabel(a.alarmType),
    sub: typeLabel(a.alarmType),
    typeLabel: ACTIVITY_TYPE.alarm.label,
    dot: ACTIVITY_TYPE.alarm.dot,
    tag: ALARM_TYPE_META[a.alarmType]?.tag || 'info',
  }))
  recentCommands.value.forEach((c) => items.push({
    key: `c-${c.id}`,
    type: 'command',
    time: c.createTime,
    title: `${deviceNameOf(c.deviceCode)} ${ACTION_LABEL[c.action] || c.action}`,
    sub: SOURCE_LABEL[c.source] || c.source || '--',
    typeLabel: ACTIVITY_TYPE.command.label,
    dot: ACTIVITY_TYPE.command.dot,
    tag: CMD_STATUS_META[c.status]?.tag || 'info',
  }))
  recentRuleLogs.value.forEach((l) => items.push({
    key: `r-${l.id}`,
    type: 'rule',
    time: l.executionTime,
    title: l.ruleName || `规则 #${l.ruleId}`,
    sub: evalMeta(l.evaluationResult).label,
    typeLabel: ACTIVITY_TYPE.rule.label,
    dot: ACTIVITY_TYPE.rule.dot,
    tag: EVAL_META[l.evaluationResult]?.tag || 'info',
  }))
  return items.sort((a, b) => (b.time || 0) - (a.time || 0)).slice(0, 10)
})

// —— 数据加载 ——
async function loadEnvironment() {
  const c = controller.value
  if (!c) { env.value = null; return }
  try {
    env.value = await getCurrentEnvironment(c.deviceCode)
  } catch (e) {
    env.value = null
  }
}

async function loadActuatorStates() {
  const acts = actuators.value
  if (!acts.length) { recentCommands.value = []; return }
  const results = await Promise.allSettled(acts.map((d) => getDeviceCommands(d.deviceCode, 10)))
  const merged = []
  results.forEach((r, i) => {
    const code = acts[i].deviceCode
    const cmds = r.status === 'fulfilled' ? r.value || [] : []
    const lastSuccess = cmds.find((c) => c.status === 'SUCCESS') || null
    actuatorStates.value[code] = { latest: cmds[0] || null, lastSuccess }
    cmds.slice(0, 3).forEach((c) => merged.push(c))
  })
  recentCommands.value = merged
}

async function loadAlarms() {
  try {
    const [pending, recent] = await Promise.allSettled([
      getAlarms({ status: 'PENDING', pageNum: 1, pageSize: 1 }),
      getAlarms({ pageNum: 1, pageSize: 5 }),
    ])
    pendingAlarmCount.value = pending.status === 'fulfilled' ? pending.value?.total || 0 : 0
    recentAlarms.value = recent.status === 'fulfilled' ? recent.value?.records || [] : []
  } catch (e) {
    /* 单源失败不阻断总览 */
  }
}

async function loadRules() {
  try {
    const res = await listRules({ enabled: true, pageNum: 1, pageSize: 1 })
    enabledRuleCount.value = res?.total || 0
  } catch (e) {
    enabledRuleCount.value = 0
  }
}

async function loadRuleLogs() {
  try {
    const res = await listRuleLogs({ pageNum: 1, pageSize: 5 })
    recentRuleLogs.value = res?.records || []
  } catch (e) {
    recentRuleLogs.value = []
  }
}

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const list = await getDevices()
    devices.value = list || []
    error.value = false
    await Promise.allSettled([loadEnvironment(), loadActuatorStates(), loadAlarms(), loadRules(), loadRuleLogs()])
    lastUpdated.value = Date.now()
  } catch (e) {
    if (!silent) error.value = true
  } finally {
    if (!silent) loading.value = false
  }
}

function go(path) {
  router.push(path)
}

function fmtClock(ms) {
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(() => {
  load()
  refreshTimer = setInterval(() => load(true), 30000)
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
})
</script>

<style scoped>
.head-updated {
  font-size: 12px;
  color: #a8abb2;
  margin-right: 4px;
}

.panel {
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.panel-sub {
  font-size: 12px;
  color: #909399;
}

.panel-foot {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #f2f3f5;
  font-size: 12px;
  color: #a8abb2;
}

/* 环境概况 */
.env-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.env-item {
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  text-align: center;
}

.env-item__value {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.env-item__label {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

/* 主控状态 */
.ctrl-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ctrl-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ctrl-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ctrl-meta {
  flex: 1;
  min-width: 0;
}

.ctrl-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.ctrl-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f7f8fa;
  border-radius: 8px;
}

.ctrl-row__label {
  font-size: 13px;
  color: #909399;
}

.ctrl-row__value {
  font-size: 13px;
  color: #303133;
}

/* 设备概况 */
.dev-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dev-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f7f8fa;
}

.dev-row__icon {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dev-row__name {
  font-size: 14px;
  color: #303133;
  min-width: 48px;
}

.mono {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 12px;
  color: #909399;
  margin-right: auto;
}

/* 安全告警 / 自动化 */
.alarm-count,
.auto-count {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.alarm-count__num,
.auto-count__num {
  font-size: 32px;
  font-weight: 700;
  color: #1f2d3d;
  line-height: 1;
}

.alarm-count__label,
.auto-count__label {
  font-size: 13px;
  color: #909399;
}

.auto-summary {
  display: flex;
  gap: 24px;
}

.auto-count {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  margin: 0;
  min-width: 110px;
}

.auto-logs {
  flex: 1;
  min-width: 0;
}

/* 迷你列表 */
.mini-list {
  display: flex;
  flex-direction: column;
}

.mini-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f2f3f5;
}

.mini-item:last-child {
  border-bottom: none;
}

.mini-item__dot,
.tl-item__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.mini-item__body {
  flex: 1;
  min-width: 0;
}

.mini-item__title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-item__meta {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 2px;
}

/* 最近动态 */
.timeline {
  display: flex;
  flex-direction: column;
}

.tl-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f3f5;
}

.tl-item:last-child {
  border-bottom: none;
}

.tl-item__dot {
  margin-top: 6px;
}

.tl-item__body {
  flex: 1;
  min-width: 0;
}

.tl-item__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tl-item__title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.tl-item__meta {
  font-size: 12px;
  color: #a8abb2;
  margin-top: 3px;
}
</style>
