<template>
  <div>
    <PageHeader title="设备管理" subtitle="主控板在线状态由心跳决定，执行器运行状态由最近一次成功指令反馈决定">
      <template #extra>
        <el-button :icon="Refresh" :loading="loading" @click="load()">刷新</el-button>
      </template>
    </PageHeader>

    <!-- 加载失败：给出可重试的明确提示 -->
    <el-result v-if="error" icon="error" title="加载失败" sub-title="无法获取设备列表，请确认网关与后端服务已启动。">
      <template #extra>
        <el-button type="primary" @click="load()">重试</el-button>
      </template>
    </el-result>

    <el-card v-else shadow="never">
      <template #header>
        <div class="card-head">
          <span>设备总览</span>
          <span class="card-head__sub">
            共 {{ list.length }} 台设备 · 执行器「未知」表示尚未获得有效反馈，不代表故障
          </span>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" empty-text="暂无设备数据" @row-click="openDetail">
        <el-table-column label="设备" min-width="200">
          <template #default="{ row }">
            <div class="dev">
              <div class="dev__icon" :style="iconStyle(row)">
                <el-icon :size="18"><component :is="typeMeta(row.deviceType).icon" /></el-icon>
              </div>
              <div class="dev__meta">
                <div class="dev__name">{{ row.deviceName }}</div>
                <code class="mono">{{ row.deviceCode }}</code>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" size="small">{{ typeMeta(row.deviceType).label }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="运行状态" min-width="150">
          <template #default="{ row }">
            <template v-if="isController(row)">
              <el-tag :type="isOnline(row) ? 'success' : 'danger'" size="small" effect="dark">
                {{ isOnline(row) ? '在线' : '离线' }}
              </el-tag>
              <div class="cell-sub">心跳 {{ fmtClock(row.lastHeartbeatTime) }}</div>
            </template>
            <template v-else>
              <el-tag :type="runState(row.deviceCode).tag" size="small" effect="light">
                {{ runState(row.deviceCode).label }}
              </el-tag>
              <div class="cell-sub">{{ feedbackHint(row.deviceCode) }}</div>
            </template>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <div class="op-cell" @click.stop>
              <template v-if="isController(row)">
                <el-button size="small" @click="openDetail(row)">查看</el-button>
              </template>
              <template v-else>
                <el-tooltip :content="switchTooltip(row.deviceCode)" placement="top">
                  <el-switch
                    :model-value="switchOn(row.deviceCode)"
                    :loading="controlling.has(row.deviceCode)"
                    :disabled="controlling.has(row.deviceCode)"
                    :active-color="'#67c23a'"
                    :inactive-color="runState(row.deviceCode).label === '未知' ? '#e6a23c' : '#a8abb2'"
                    @change="(val) => onToggle(row, val)"
                  />
                </el-tooltip>
                <el-button size="small" text type="primary" @click="openDetail(row)">详情</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 第二层：设备详情抽屉 -->
    <el-drawer
      v-model="detailVisible"
      :title="detailDevice?.deviceName || '设备详情'"
      size="480px"
      @closed="onDrawerClosed"
    >
      <div v-if="detailDevice" class="detail">
        <div class="detail__head">
          <div class="detail__icon" :style="iconStyle(detailDevice)">
            <el-icon :size="22"><component :is="typeMeta(detailDevice.deviceType).icon" /></el-icon>
          </div>
          <div class="detail__title">
            <div class="detail__name">{{ detailDevice.deviceName }}</div>
            <code class="mono">{{ detailDevice.deviceCode }}</code>
          </div>
          <el-tag type="info" effect="plain" size="small">{{ typeMeta(detailDevice.deviceType).label }}</el-tag>
        </div>

        <el-descriptions :column="1" border size="small" class="detail__desc">
          <el-descriptions-item label="运行状态">
            <el-tag :type="detailRunState.tag" size="small">{{ detailRunState.label }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="isController(detailDevice) ? '最近心跳' : '最近控制时间'">
            {{ isController(detailDevice) ? fmtDateTime(detailDevice.lastHeartbeatTime) : lastControlTime }}
          </el-descriptions-item>
          <el-descriptions-item label="最近指令结果">
            {{ isController(detailDevice) ? '主控板不产生控制指令' : lastCommandResult }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 远程控制（仅执行器） -->
        <div v-if="!isController(detailDevice)" class="detail__section">
          <div class="section-title">远程控制</div>
          <div class="ctrl">
            <el-button
              type="success"
              :loading="controlling.has(detailDevice.deviceCode)"
              :disabled="controlling.has(detailDevice.deviceCode)"
              @click="sendControl(detailDevice, 'TURN_ON')"
            >开启</el-button>
            <el-button
              type="info"
              plain
              :loading="controlling.has(detailDevice.deviceCode)"
              :disabled="controlling.has(detailDevice.deviceCode)"
              @click="sendControl(detailDevice, 'TURN_OFF')"
            >关闭</el-button>
          </div>
          <div class="ctrl-hint">指令下发后设备异步执行，页面会轮询直到收到反馈，请稍候。</div>
        </div>
        <div v-else class="detail__section">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="主控板不接收远程控制"
            description="主控板负责汇聚传感器数据并执行规则联动，其在线状态由心跳决定。"
          />
        </div>

        <!-- 历史控制记录 -->
        <div class="detail__section">
          <div class="section-title">
            历史控制记录
            <span class="section-title__sub">{{ isController(detailDevice) ? '' : `（${detailCommands.length} 条）` }}</span>
          </div>
          <div v-loading="detailLoading">
            <div v-if="detailCommands.length === 0" class="event-empty">暂无控制记录</div>
            <div v-else class="event-list">
              <div v-for="c in detailCommands" :key="c.id" class="event-item">
                <span class="event-dot" :style="{ background: cmdDot(c) }"></span>
                <div class="event-item__body">
                  <div class="event-item__title">{{ actionLabel(c.action) }} · {{ sourceLabel(c.source) }}</div>
                  <div class="event-item__meta">{{ fmtDateTime(c.createTime) }}</div>
                </div>
                <div class="cmd-right">
                  <el-tag :type="cmdStatusMeta(c.status).tag" size="small" effect="plain">
                    {{ cmdStatusMeta(c.status).label }}
                  </el-tag>
                  <div v-if="c.resultMessage" class="event-item__meta">{{ c.resultMessage }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getDevices, getDeviceCommands, sendDeviceCommand } from '@/api/device'
import { formatDateTime } from '@/utils/format'

// —— 设备类型元数据（图标全局注册，名称与后端 deviceType 对齐）——
const DEVICE_TYPE_META = {
  CONTROLLER: { label: '主控板', icon: 'Cpu' },
  FAN: { label: '风扇', icon: 'WindPower' },
  WINDOW: { label: '窗户', icon: 'Grid' },
  DOOR: { label: '门', icon: 'Lock' },
  LED: { label: '照明灯', icon: 'Lightning' },
}
function typeMeta(type) {
  return DEVICE_TYPE_META[type] || { label: type || '--', icon: 'Monitor' }
}

const ACTION_META = { TURN_ON: { label: '开启', tag: 'success' }, TURN_OFF: { label: '关闭', tag: 'info' } }
const SOURCE_META = { REMOTE: '家属远程', RULE: '规则联动', AUTO_RULE: '自动规则' }
const CMD_STATUS_META = {
  PENDING: { label: '执行中', tag: 'warning' },
  SUCCESS: { label: '成功', tag: 'success' },
  FAILED: { label: '失败', tag: 'danger' },
}
function actionLabel(a) { return ACTION_META[a]?.label || a || '--' }
function sourceLabel(s) { return SOURCE_META[s] || s || '--' }
function cmdStatusMeta(s) { return CMD_STATUS_META[s] || { label: s || '--', tag: 'info' } }

// —— 状态 ——
const list = ref([])
const loading = ref(false)
const error = ref(false)
const controlling = ref(new Set())
// stateMap[deviceCode] = { latest: 最近一条指令, lastSuccess: 最近一条成功指令 }
const stateMap = ref({})

// 详情抽屉
const detailVisible = ref(false)
const detailDevice = ref(null)
const detailCommands = ref([])
const detailLoading = ref(false)

let refreshTimer = null
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// —— 基础判断 ——
function isController(row) {
  return String(row.deviceType).toUpperCase() === 'CONTROLLER'
}
function isOnline(row) {
  return String(row.onlineStatus).toUpperCase() === 'ONLINE'
}

// —— 执行器运行状态（由指令反馈推导，绝不使用已停用的 deviceStatus）——
function runState(code) {
  const info = stateMap.value[code]
  if (info?.latest && info.latest.status === 'PENDING') {
    return { label: '执行中', tag: 'primary' }
  }
  if (info?.lastSuccess) {
    return info.lastSuccess.action === 'TURN_ON'
      ? { label: '开启', tag: 'success' }
      : { label: '关闭', tag: 'info' }
  }
  return { label: '未知', tag: 'warning' }
}

function switchOn(code) {
  const info = stateMap.value[code]
  return info?.lastSuccess ? info.lastSuccess.action === 'TURN_ON' : false
}

function switchTooltip(code) {
  const s = runState(code).label
  if (s === '未知') return '尚未获得有效反馈，可点击下发指令以确认状态'
  if (s === '执行中') return '指令执行中，等待设备反馈'
  return s === '开启' ? '当前开启，点击关闭' : '当前关闭，点击开启'
}

function feedbackHint(code) {
  const latest = stateMap.value[code]?.latest
  if (!latest) return '暂无有效反馈'
  const ts = latest.finishTime || latest.createTime
  return `最近反馈 ${fmtClock(ts)}`
}

// —— 格式化 ——
function fmtDateTime(ms) { return ms ? formatDateTime(ms) : '--' }
function fmtClock(ms) {
  if (!ms) return '--'
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// —— 详情派生 ——
const detailRunState = computed(() => {
  const d = detailDevice.value
  if (!d) return { label: '--', tag: 'info' }
  if (isController(d)) return isOnline(d) ? { label: '在线', tag: 'success' } : { label: '离线', tag: 'danger' }
  return runState(d.deviceCode)
})

const lastControlTime = computed(() => {
  const d = detailDevice.value
  if (!d) return '--'
  const latest = stateMap.value[d.deviceCode]?.latest
  return latest ? formatDateTime(latest.createTime) : '从未控制'
})

const lastCommandResult = computed(() => {
  const d = detailDevice.value
  if (!d) return '--'
  const latest = stateMap.value[d.deviceCode]?.latest
  if (!latest) return '暂无指令记录'
  const m = cmdStatusMeta(latest.status)
  return latest.resultMessage ? `${m.label}（${latest.resultMessage}）` : m.label
})

function cmdDot(c) {
  if (c.status === 'SUCCESS') return '#67c23a'
  if (c.status === 'FAILED') return '#f56c6c'
  return '#e6a23c'
}

function iconStyle(row) {
  const palette = {
    CONTROLLER: { bg: '#ecf5ff', color: '#409eff' },
    FAN: { bg: '#f0f9eb', color: '#67c23a' },
    WINDOW: { bg: '#fdf6ec', color: '#e6a23c' },
    DOOR: { bg: '#fef0f0', color: '#f56c6c' },
    LED: { bg: '#f5f7fa', color: '#909399' },
  }
  const p = palette[row.deviceType] || palette.CONTROLLER
  return { background: p.bg, color: p.color }
}

// —— 数据加载 ——
function mergeState(code, commands) {
  const arr = commands || []
  const lastSuccess = arr.find((c) => c.status === 'SUCCESS')
  stateMap.value[code] = { latest: arr[0] || null, lastSuccess: lastSuccess || null }
}

async function loadAllStates() {
  const actuators = list.value.filter((d) => !isController(d))
  const results = await Promise.allSettled(actuators.map((d) => getDeviceCommands(d.deviceCode, 50)))
  results.forEach((r, i) => {
    const code = actuators[i].deviceCode
    if (r.status === 'fulfilled' && r.value?.length) {
      mergeState(code, r.value)
    } else {
      stateMap.value[code] = { latest: null, lastSuccess: null }
    }
  })
}

async function load(silent = false) {
  if (!silent) loading.value = true
  try {
    const devices = await getDevices()
    list.value = (devices || []).slice().sort((a, b) => (isController(a) ? 0 : 1) - (isController(b) ? 0 : 1))
    error.value = false
    await loadAllStates()
  } catch (e) {
    if (!silent) error.value = true
  } finally {
    if (!silent) loading.value = false
  }
}

async function refreshDevice(code) {
  try {
    const cmds = await getDeviceCommands(code, 50)
    mergeState(code, cmds)
  } catch (e) {
    /* 静默：下次轮询会重试 */
  }
}

// —— 远程控制 ——
async function waitForResult(deviceCode, commandCode, timeoutMs = 15000) {
  const start = Date.now()
  let lastCmd = null
  while (Date.now() - start < timeoutMs) {
    await sleep(1200)
    try {
      const cmds = await getDeviceCommands(deviceCode, 10)
      const cmd = cmds.find((c) => c.commandCode === commandCode) || cmds[0]
      if (cmd) lastCmd = cmd
      if (cmd && cmd.status !== 'PENDING') return cmd
    } catch (e) {
      /* 轮询中的瞬时失败忽略，继续等待 */
    }
  }
  return lastCmd && lastCmd.status !== 'PENDING' ? lastCmd : null
}

async function sendControl(device, action) {
  const code = device.deviceCode
  if (controlling.value.has(code)) return
  controlling.value.add(code)

  const label = action === 'TURN_ON' ? '开启' : '关闭'
  const loadingMsg = ElMessage({
    message: `${device.deviceName}：已下发「${label}」指令，等待设备反馈…`,
    duration: 0,
    type: 'info',
  })

  try {
    const cmd = await sendDeviceCommand(device.id, action)
    const result = await waitForResult(code, cmd.commandCode, 15000)
    loadingMsg.close()

    if (result) {
      if (result.status === 'SUCCESS') {
        ElMessage.success(`${device.deviceName} 已${label}`)
      } else {
        ElMessage.error(`${device.deviceName} ${label}失败：${result.resultMessage || '设备执行失败'}`)
      }
      mergeState(code, [result])
    } else {
      ElMessage.warning(`${device.deviceName} 未在预期时间内反馈，请稍后刷新查看实际状态`)
      await refreshDevice(code)
    }

    // 若详情抽屉正打开且针对同一设备，同步刷新其历史记录
    if (detailVisible.value && detailDevice.value?.deviceCode === code) {
      loadDetailCommands()
    }
  } catch (e) {
    loadingMsg.close()
    ElMessage.error(`${device.deviceName} ${label}失败：${e?.message || '请确认设备在线'}`)
  } finally {
    controlling.value.delete(code)
  }
}

function onToggle(row, val) {
  sendControl(row, val ? 'TURN_ON' : 'TURN_OFF')
}

// —— 详情抽屉 ——
function openDetail(row) {
  detailDevice.value = row
  detailVisible.value = true
  loadDetailCommands()
}

async function loadDetailCommands() {
  const d = detailDevice.value
  if (!d) return
  detailLoading.value = true
  try {
    const cmds = await getDeviceCommands(d.deviceCode, 50)
    detailCommands.value = cmds || []
    if (cmds?.length) mergeState(d.deviceCode, cmds)
  } catch (e) {
    detailCommands.value = []
  } finally {
    detailLoading.value = false
  }
}

function onDrawerClosed() {
  detailDevice.value = null
  detailCommands.value = []
}

// —— 生命周期 ——
onMounted(() => {
  load()
  refreshTimer = setInterval(() => load(true), 30000)
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
})
</script>

<style scoped>
.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.card-head__sub {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
}

.dev {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dev__icon,
.detail__icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dev__meta {
  min-width: 0;
}

.dev__name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.mono {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 12px;
  color: #909399;
}

.cell-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #a8abb2;
}

.op-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.detail__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail__title {
  flex: 1;
  min-width: 0;
}

.detail__name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail__desc {
  margin-bottom: 20px;
}

.detail__section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.section-title__sub {
  font-weight: normal;
  color: #909399;
}

.ctrl {
  display: flex;
  gap: 12px;
}

.ctrl-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #a8abb2;
}

.event-empty {
  padding: 28px 0;
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
}

.event-list {
  max-height: 360px;
  overflow-y: auto;
}

.event-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid #f2f3f5;
}

.event-item:last-child {
  border-bottom: none;
}

.event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.event-item__body {
  flex: 1;
  min-width: 0;
}

.event-item__title {
  font-size: 14px;
  color: #303133;
}

.event-item__meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  word-break: break-all;
}

.cmd-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
</style>
