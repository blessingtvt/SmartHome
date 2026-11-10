<template>
  <el-container class="layout">
    <!-- 侧边导航 -->
    <el-aside class="layout__aside" :width="collapsed ? '64px' : '220px'">
      <div class="brand" :class="{ 'brand--collapsed': collapsed }">
        <el-icon class="brand__icon" :size="24"><HomeFilled /></el-icon>
        <span v-if="!collapsed" class="brand__name">智能家居</span>
      </div>
      <el-menu
        class="layout__menu"
        :default-active="route.path"
        :collapse="collapsed"
        :collapse-transition="false"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="'/' + item.path">
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <template #title>{{ item.meta.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部状态栏 -->
      <el-header class="layout__header" height="56px">
        <div class="header__left">
          <el-button text class="collapse-btn" @click="appStore.toggleSidebar()">
            <el-icon :size="18">
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>家属端</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header__right">
          <!-- 服务状态：占位，正式对接后接入网关健康检查，勿凭猜测显示真实状态 -->
          <el-tag type="success" effect="light" size="small">
            <el-icon style="vertical-align: -2px"><CircleCheck /></el-icon>
            <span class="status-text">网关已就绪（示例）</span>
          </el-tag>
          <span class="header__clock">{{ now }}</span>
          <el-divider direction="vertical" />
          <el-dropdown>
            <span class="user">
              <el-icon :size="18"><UserFilled /></el-icon>
              <span class="user__name">{{ appStore.familyName }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>账号设置（待实现）</el-dropdown-item>
                <el-dropdown-item disabled>退出登录（待实现）</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

const collapsed = computed(() => appStore.sidebarCollapsed)

// 侧边菜单与路由保持单一来源：取根路由的 children
const menuItems = computed(
  () => router.options.routes.find((r) => r.path === '/')?.children ?? []
)

const now = ref('')
let timer = null
function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  now.value =
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
onMounted(() => {
  tick()
  timer = setInterval(tick, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.layout {
  height: 100vh;
}

.layout__aside {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  transition: width 0.2s;
  overflow: hidden;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 20px;
  white-space: nowrap;
}

.brand--collapsed {
  justify-content: center;
  padding: 0;
}

.brand__icon {
  color: var(--el-color-primary);
}

.brand__name {
  font-size: 17px;
  font-weight: 600;
  color: #1f2d3d;
}

.layout__menu {
  flex: 1;
  border-right: none;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.status-text {
  margin-left: 4px;
}

.header__clock {
  font-size: 13px;
  color: #606266;
  font-variant-numeric: tabular-nums;
}

.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
  outline: none;
}

.user__name {
  font-size: 14px;
}

.layout__main {
  background: #f5f7fa;
  padding: 20px 24px;
  overflow-y: auto;
}
</style>
