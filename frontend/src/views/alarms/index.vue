<template>
  <div>
    <PageHeader title="告警管理" subtitle="查看与处理告警事件">
      <template #extra>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </template>
    </PageHeader>

    <!-- 筛选 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="告警类型">
          <el-select v-model="query.alarmType" placeholder="全部" clearable style="width: 150px">
            <el-option label="一键求助" value="MANUAL_HELP" />
            <el-option label="环境异常" value="ENV_ABNORMAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已处理" value="PROCESSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码">
          <el-input v-model="query.deviceCode" placeholder="如 ESP32_001" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <!-- 加载失败：给出可重试的明确提示 -->
      <el-result v-if="error" icon="error" title="加载失败" sub-title="无法获取告警列表，请确认网关与后端服务已启动。">
        <template #extra>
          <el-button type="primary" @click="load">重试</el-button>
        </template>
      </el-result>

      <template v-else>
        <el-table v-loading="loading" :data="list" empty-text="暂无告警数据">
          <el-table-column label="告警编码" min-width="210" show-overflow-tooltip>
            <template #default="{ row }">
              <code class="mono">{{ row.alarmCode }}</code>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="typeTag(row.alarmType)" effect="plain">{{ typeLabel(row.alarmType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="设备" width="120">
            <template #default="{ row }">
              <code v-if="row.deviceCode" class="mono">{{ row.deviceCode }}</code>
              <span v-else class="muted">--</span>
            </template>
          </el-table-column>
          <el-table-column prop="alarmContent" label="内容" min-width="180" show-overflow-tooltip />
          <el-table-column label="发生时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.alarmTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" effect="dark">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理时间" min-width="170">
            <template #default="{ row }">
              <span v-if="row.handleTime">{{ formatDateTime(row.handleTime) }}</span>
              <span v-else class="muted">--</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                size="small"
                :loading="isActing(row.id, 'PROCESSING')"
                @click="handle(row, 'PROCESSING')"
              >开始处理</el-button>
              <el-button
                v-if="row.status === 'PENDING' || row.status === 'PROCESSING'"
                size="small"
                type="primary"
                :loading="isActing(row.id, 'PROCESSED')"
                @click="handle(row, 'PROCESSED')"
              >标记完成</el-button>
              <span v-if="row.status === 'PROCESSED'" class="muted">已归档</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :current-page="query.pageNum"
            :page-size="query.pageSize"
            :page-sizes="[10, 20, 50]"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getAlarms, updateAlarmStatus } from '@/api/alarm'
import { formatDateTime } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const error = ref(false)

// 正在处理的告警（按钮 loading，防重复点击/并发提交）
const actingId = ref(null)
const actingTarget = ref('')

const query = reactive({
  alarmType: '',
  status: '',
  deviceCode: '',
  pageNum: 1,
  pageSize: 20,
})

const TYPE_META = {
  MANUAL_HELP: { label: '一键求助', tag: 'danger' },
  ENV_ABNORMAL: { label: '环境异常', tag: 'warning' },
}
const STATUS_META = {
  PENDING: { label: '待处理', tag: 'danger' },
  PROCESSING: { label: '处理中', tag: 'warning' },
  PROCESSED: { label: '已处理', tag: 'success' },
}

function typeLabel(t) { return TYPE_META[t]?.label || t || '--' }
function typeTag(t) { return TYPE_META[t]?.tag || 'info' }
function statusLabel(s) { return STATUS_META[s]?.label || s || '--' }
function statusTag(s) { return STATUS_META[s]?.tag || 'info' }
function isActing(id, target) { return actingId.value === id && actingTarget.value === target }

async function load() {
  loading.value = true
  error.value = false
  try {
    const data = await getAlarms({
      alarmType: query.alarmType || undefined,
      status: query.status || undefined,
      deviceCode: query.deviceCode || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    })
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    error.value = true
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  load()
}

function reset() {
  query.alarmType = ''
  query.status = ''
  query.deviceCode = ''
  query.pageNum = 1
  load()
}

function onPageChange(p) {
  query.pageNum = p
  load()
}

function onSizeChange(s) {
  query.pageSize = s
  query.pageNum = 1
  load()
}

async function handle(row, target) {
  const tip = target === 'PROCESSED'
    ? '确认将该告警标记为「已处理」？'
    : '确认开始处理该告警？'
  try {
    await ElMessageBox.confirm(tip, '提示', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' })
  } catch {
    return
  }

  actingId.value = row.id
  actingTarget.value = target
  try {
    await updateAlarmStatus(row.id, target)
    ElMessage.success(target === 'PROCESSED' ? '已标记为已处理' : '已开始处理')
    await load()
  } catch (e) {
    // 业务错误（如状态已被并发修改、非法跳转）由响应拦截器提示；这里刷新列表展示最新状态
    await load()
  } finally {
    actingId.value = null
    actingTarget.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.mono {
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 13px;
  color: #606266;
}

.muted {
  color: #c0c4cc;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
