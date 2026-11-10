<template>
  <div>
    <PageHeader title="环境监测" subtitle="温湿度、水滴感应、人体活动与历史趋势">
      <template #extra>
        <el-button :icon="Refresh" :loading="loading" @click="reload">刷新</el-button>
      </template>
    </PageHeader>

    <!-- 设备加载失败：顶部横幅，不卸载下方图表 -->
    <el-alert
      v-if="deviceError"
      class="banner"
      type="error"
      title="无法获取设备列表"
      show-icon
      :closable="false"
    >
      <div class="banner__body">
        <span>请确认网关与后端服务已启动。</span>
        <el-button size="small" type="primary" @click="loadDevices">重试</el-button>
      </div>
    </el-alert>

    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">设备</span>
          <el-select
            v-model="deviceCode"
            placeholder="选择设备"
            :loading="devicesLoading"
            style="width: 220px"
            @change="onDeviceChange"
          >
            <el-option
              v-for="d in devices"
              :key="d.deviceCode"
              :label="`${d.deviceName}（${d.deviceCode}）`"
              :value="d.deviceCode"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">时间范围</span>
          <el-radio-group v-model="timeRange" size="small" @change="onRangeChange">
            <el-radio-button label="1h">近1小时</el-radio-button>
            <el-radio-button label="6h">近6小时</el-radio-button>
            <el-radio-button label="24h">近24小时</el-radio-button>
            <el-radio-button label="7d">近7天</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <!-- 实时环境卡片 -->
    <el-row :gutter="16" class="hero-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="hero-card">
          <div class="hero">
            <div class="hero__icon" :style="{ background: TEMP_BG, color: TEMP_COLOR }">
              <el-icon :size="26"><component :is="'Sunny'" /></el-icon>
            </div>
            <div class="hero__meta">
              <div class="hero__value">{{ fmtTemp(current?.temperature) }}<span class="hero__unit">℃</span></div>
              <div class="hero__label">当前温度</div>
            </div>
          </div>
          <div class="hero__foot">更新于 {{ fmtClock(current?.timestamp) }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="hero-card">
          <div class="hero">
            <div class="hero__icon" :style="{ background: HUM_BG, color: HUM_COLOR }">
              <el-icon :size="26"><component :is="'Cloudy'" /></el-icon>
            </div>
            <div class="hero__meta">
              <div class="hero__value">{{ fmtHum(current?.humidity) }}<span class="hero__unit">%</span></div>
              <div class="hero__label">当前湿度</div>
            </div>
          </div>
          <div class="hero__foot">更新于 {{ fmtClock(current?.timestamp) }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="hero-card">
          <div class="hero">
            <div class="hero__icon" :style="{ background: rain.bg, color: rain.color }">
              <el-icon :size="26"><component :is="rain.icon" /></el-icon>
            </div>
            <div class="hero__meta">
              <div class="hero__value">{{ rain.levelText }}</div>
              <div class="hero__label">当前天气</div>
            </div>
          </div>
          <div class="rain-reminder">{{ rain.reminder }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="hero-card hero-card--flat">
          <div class="hero hero--column">
            <div class="hero__label">最近采集时间</div>
            <div class="hero__subvalue">{{ fmtDateTime(current?.timestamp) }}</div>
          </div>
          <div class="hero__foot">
            <span :class="['presence-dot', current?.pirTriggered ? 'is-on' : '']"></span>
            {{ current?.pirTriggered ? '最近检测到人体活动' : '近期无人体活动' }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史趋势 -->
    <el-card shadow="never" class="block" v-loading="historyLoading">
      <template #header>
        <div class="card-head">
          <span>历史趋势</span>
          <span class="card-head__sub">{{ trendSummary }}</span>
        </div>
      </template>

      <div class="chart-head">
        <span class="chart-dot" :style="{ background: TEMP_COLOR }"></span>
        <span>温度（℃）</span>
      </div>
      <div ref="tempChartRef" class="chart"></div>

      <div class="chart-head chart-head--gap">
        <span class="chart-dot" :style="{ background: HUM_COLOR }"></span>
        <span>湿度（%）</span>
      </div>
      <div ref="humChartRef" class="chart"></div>
    </el-card>

    <!-- 传感器状态 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="card-head">
          <span>传感器状态</span>
          <span class="card-head__sub">最近采集 {{ fmtDateTime(current?.timestamp) }}</span>
        </div>
      </template>

      <el-row :gutter="16">
        <el-col v-for="s in sensors" :key="s.name" :xs="12" :sm="12" :md="6">
          <div class="sensor">
            <div class="sensor__top">
              <el-icon :size="18" :color="s.valid ? '#409eff' : '#c0c4cc'">
                <component :is="s.icon" />
              </el-icon>
              <span class="sensor__name">{{ s.name }}</span>
              <el-tag :type="s.valid ? 'success' : 'info'" size="small" effect="plain">
                {{ s.valid ? '有效' : '未接入' }}
              </el-tag>
            </div>
            <div class="sensor__value" :class="{ 'is-muted': !s.valid }">{{ s.text }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 环境事件 -->
    <el-card shadow="never" class="block">
      <template #header><span>环境事件</span></template>

      <el-tabs v-model="eventTab">
        <el-tab-pane :label="`温湿度异常（${anomalies.length}）`" name="anomaly">
          <div v-if="anomalies.length === 0" class="event-empty">暂无温湿度异常记录</div>
          <div v-else class="event-list">
            <div v-for="a in anomalies" :key="a.id" class="event-item">
              <span class="event-dot event-dot--danger"></span>
              <div class="event-item__body">
                <div class="event-item__title">{{ a.alarmContent || '温湿度异常' }}</div>
                <div class="event-item__meta">{{ a.deviceCode || '--' }} · {{ fmtDateTime(a.alarmTime) }}</div>
              </div>
              <el-tag :type="statusTag(a.status)" size="small" effect="plain">{{ statusLabel(a.status) }}</el-tag>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="`人体活动（${pirEvents.length}）`" name="activity">
          <div v-if="pirEvents.length === 0" class="event-empty">暂未检测到人体活动</div>
          <div v-else class="event-list">
            <div v-for="r in pirEvents" :key="r.id" class="event-item">
              <span class="event-dot event-dot--activity"></span>
              <div class="event-item__body">
                <div class="event-item__title">检测到人体活动</div>
                <div class="event-item__meta">{{ r.deviceCode || '--' }} · {{ fmtDateTime(r.timestamp) }}</div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getDevices } from '@/api/device'
import { getCurrentEnvironment, getEnvironmentHistory } from '@/api/environment'
import { getAlarms } from '@/api/alarm'
import { formatDateTime } from '@/utils/format'

// —— 配色（温度=暖琥珀，湿度=蓝；单系列折线，无需图例，标题+色点标注实体）——
const TEMP_COLOR = '#e6a23c'
const HUM_COLOR = '#409eff'
const TEMP_BG = '#fdf6ec'
const HUM_BG = '#ecf5ff'

const RANGE_HOURS = { '1h': 1, '6h': 6, '24h': 24, '7d': 168 }
const HISTORY_SIZE = 1000 // 单页最多取 1000 条（当前数据量远小于此）

// —— 状态 ——
const devices = ref([])
const devicesLoading = ref(false)
const deviceError = ref(false)
const deviceCode = ref('')
const timeRange = ref('24h')

const current = ref(null)
const historyRecords = ref([])
const anomalies = ref([])

const loading = ref(false)
const historyLoading = ref(false)
const eventTab = ref('anomaly')

// —— 图表 ——
const tempChartRef = ref(null)
const humChartRef = ref(null)
let tempChart = null
let humChart = null
let resizeObserver = null
let refreshTimer = null

// —— 派生数据 ——
const sensors = computed(() => {
  const c = current.value || {}
  const has = (v) => v !== null && v !== undefined
  return [
    { name: '温度传感器', icon: 'Sunny', valid: has(c.temperature), text: has(c.temperature) ? `${Number(c.temperature).toFixed(1)} ℃` : '未接入' },
    { name: '湿度传感器', icon: 'Cloudy', valid: has(c.humidity), text: has(c.humidity) ? `${Number(c.humidity).toFixed(1)} %` : '未接入' },
    { name: '燃气传感器', icon: 'Warning', valid: has(c.gasRaw), text: has(c.gasRaw) ? `原始值 ${c.gasRaw}` : '未接入' },
    { name: '水滴感应', icon: 'Pouring', valid: has(c.water), text: has(c.water) ? `原始值 ${c.water}` : '未接入' },
    { name: '人体感应', icon: 'Aim', valid: has(c.pirTriggered), text: !has(c.pirTriggered) ? '未接入' : c.pirTriggered ? '检测到活动' : '无活动' },
  ]
})

// —— 雨水卡片：按水滴感应原始 ADC 值分级 ——
// 接线约定：水越多读数越大，>1500 视为有雨（沿用 auto_window 的 1500 基线）。
// 小/中/大分档为基于 1500 的估计值，可在实测后按 RAIN_NONE/LIGHT/MODERATE 微调。
const RAIN_NONE = 1500
const RAIN_LIGHT = 2500
const RAIN_MODERATE = 3500

const RAIN_META = {
  none: { levelText: '无降雨', icon: 'Sunny', color: '#67c23a', bg: '#f0f9eb', reminder: '天气晴好，无需带伞' },
  light: { levelText: '小雨', icon: 'Cloudy', color: '#79bbff', bg: '#ecf5ff', reminder: '有小雨，出门记得带伞' },
  moderate: { levelText: '中雨', icon: 'Umbrella', color: '#409eff', bg: '#ecf5ff', reminder: '中雨，出门带伞，记得收衣物' },
  heavy: { levelText: '大雨', icon: 'Lightning', color: '#2f6fbf', bg: '#e8f1fc', reminder: '大雨，尽量少外出，带伞并收好衣物' },
  none_data: { levelText: '未接入', icon: 'Umbrella', color: '#c0c4cc', bg: '#f4f4f5', reminder: '水滴传感器未接入' },
}

const rain = computed(() => {
  const w = current.value?.water
  if (w == null) return RAIN_META.none_data
  if (w <= RAIN_NONE) return RAIN_META.none
  if (w <= RAIN_LIGHT) return RAIN_META.light
  if (w <= RAIN_MODERATE) return RAIN_META.moderate
  return RAIN_META.heavy
})

const pirEvents = computed(() => historyRecords.value.filter((r) => r.pirTriggered === true))

const trendSummary = computed(() => {
  if (historyRecords.value.length === 0) return '暂无历史数据'
  return `近 ${RANGE_HOURS[timeRange.value]} 小时 · ${historyRecords.value.length} 条采样`
})

// —— 格式化 ——
function fmtTemp(v) { return v == null ? '--' : Number(v).toFixed(1) }
function fmtHum(v) { return v == null ? '--' : Number(v).toFixed(1) }
function fmtDateTime(ms) { return ms ? formatDateTime(ms) : '--' }
function fmtClock(ms) {
  if (!ms) return '--'
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
function fmtAxis(ms) {
  const d = new Date(ms)
  const p = (n) => String(n).padStart(2, '0')
  if (RANGE_HOURS[timeRange.value] >= 24) return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  return `${p(d.getHours())}:${p(d.getMinutes())}`
}

const STATUS_META = {
  PENDING: { label: '待处理', tag: 'danger' },
  PROCESSING: { label: '处理中', tag: 'warning' },
  PROCESSED: { label: '已处理', tag: 'success' },
}
function statusLabel(s) { return STATUS_META[s]?.label || s || '--' }
function statusTag(s) { return STATUS_META[s]?.tag || 'info' }

// —— 图表 ——
function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

function buildSeriesOption(records, key, unit, name, color) {
  const labels = records.map((r) => fmtAxis(r.timestamp))
  const values = records.map((r) => (r[key] == null ? null : Number(r[key])))
  return {
    grid: { left: 8, right: 20, top: 16, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'line', lineStyle: { color: '#c0c4cc', type: 'dashed' } },
      formatter: (params) => {
        const p = params[0]
        const v = p.value == null ? '--' : `${p.value}${unit}`
        return `${p.axisValue}<br/>${p.marker}${name}：${v}`
      },
    },
    xAxis: {
      type: 'category',
      data: labels,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11, hideOverlap: true },
    },
    yAxis: {
      type: 'value',
      scale: true,
      splitLine: { lineStyle: { color: '#ebeef5' } },
      axisLabel: { color: '#909399', fontSize: 11, formatter: `{value}` },
    },
    series: [
      {
        name,
        type: 'line',
        data: values,
        showSymbol: false,
        smooth: false,
        connectNulls: false,
        lineStyle: { width: 2, color },
        itemStyle: { color },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: hexToRgba(color, 0.22) },
              { offset: 1, color: hexToRgba(color, 0) },
            ],
          },
        },
        emphasis: { focus: 'series' },
      },
    ],
  }
}

function renderCharts() {
  if (!tempChartRef.value || !humChartRef.value) return
  if (!tempChart || tempChart.isDisposed()) tempChart = echarts.init(tempChartRef.value)
  if (!humChart || humChart.isDisposed()) humChart = echarts.init(humChartRef.value)

  const records = historyRecords.value
  tempChart.setOption(buildSeriesOption(records, 'temperature', '℃', '温度', TEMP_COLOR), true)
  humChart.setOption(buildSeriesOption(records, 'humidity', '%', '湿度', HUM_COLOR), true)
}

function initCharts() {
  if (!tempChartRef.value || !humChartRef.value) return
  tempChart = echarts.init(tempChartRef.value)
  humChart = echarts.init(humChartRef.value)
  resizeObserver = new ResizeObserver(() => {
    tempChart?.resize()
    humChart?.resize()
  })
  resizeObserver.observe(tempChartRef.value)
  resizeObserver.observe(humChartRef.value)
}

// —— 数据加载 ——
function historyParams() {
  const params = {
    deviceCode: deviceCode.value,
    page: 1,
    size: HISTORY_SIZE,
  }
  const hours = RANGE_HOURS[timeRange.value]
  if (hours) {
    params.endTime = Date.now()
    params.startTime = Date.now() - hours * 3600 * 1000
  }
  return params
}

async function loadDevices() {
  devicesLoading.value = true
  deviceError.value = false
  try {
    const list = await getDevices()
    devices.value = list || []
    // 默认选中主控板（传感器汇聚点）；否则取第一个设备
    if (!deviceCode.value) {
      const ctrl = devices.value.find((d) => d.deviceType === 'CONTROLLER')
      deviceCode.value = ctrl?.deviceCode || devices.value[0]?.deviceCode || ''
    }
    if (deviceCode.value) await loadData(false)
  } catch (e) {
    deviceError.value = true
  } finally {
    devicesLoading.value = false
  }
}

async function loadData(silent) {
  if (!deviceCode.value) return
  if (!silent) loading.value = true
  historyLoading.value = true
  try {
    const [cur, hist, ano] = await Promise.allSettled([
      getCurrentEnvironment(deviceCode.value),
      getEnvironmentHistory(historyParams()),
      getAlarms({ alarmType: 'ENV_ABNORMAL', pageNum: 1, pageSize: 50 }),
    ])
    current.value = cur.status === 'fulfilled' ? cur.value : null
    historyRecords.value = hist.status === 'fulfilled' ? (hist.value.records || []).slice().reverse() : []
    anomalies.value = ano.status === 'fulfilled' ? (ano.value.records || []) : []
  } finally {
    if (!silent) loading.value = false
    historyLoading.value = false
  }
  renderCharts()
}

function reload() {
  loadData(false)
}

function onDeviceChange() {
  current.value = null
  historyRecords.value = []
  anomalies.value = []
  loadData(false)
}

function onRangeChange() {
  loadData(false)
}

// —— 生命周期 ——
onMounted(() => {
  initCharts()
  loadDevices()
  // 实时刷新：与设备 30s 上报周期对齐
  refreshTimer = setInterval(() => loadData(true), 30000)
})

onBeforeUnmount(() => {
  clearInterval(refreshTimer)
  resizeObserver?.disconnect()
  tempChart?.dispose()
  humChart?.dispose()
})
</script>

<style scoped>
.banner {
  margin-bottom: 16px;
}

.banner__body {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.hero-row {
  margin-bottom: 16px;
}

.hero-card {
  height: 100%;
}

.hero {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero--column {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.hero__icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hero__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hero__value {
  font-size: 30px;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.hero__subvalue {
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
  font-variant-numeric: tabular-nums;
}

.hero__unit {
  font-size: 16px;
  font-weight: 500;
  color: #909399;
  margin-left: 4px;
}

.hero__label {
  font-size: 13px;
  color: #909399;
}

.hero__foot {
  margin-top: 12px;
  font-size: 12px;
  color: #a8abb2;
  display: flex;
  align-items: center;
  gap: 6px;
}

.rain-reminder {
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.presence-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
}

.presence-dot.is-on {
  background: #67c23a;
}

.block {
  margin-bottom: 16px;
}

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

.chart-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.chart-head--gap {
  margin-top: 16px;
}

.chart-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.chart {
  width: 100%;
  height: 240px;
}

.sensor {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 12px;
}

.sensor__top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.sensor__name {
  flex: 1;
  font-size: 13px;
  color: #606266;
}

.sensor__value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.sensor__value.is-muted {
  color: #c0c4cc;
  font-weight: normal;
}

.event-empty {
  padding: 32px 0;
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
  align-items: center;
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
  flex-shrink: 0;
}

.event-dot--danger {
  background: #f56c6c;
}

.event-dot--activity {
  background: #67c23a;
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
}
</style>
