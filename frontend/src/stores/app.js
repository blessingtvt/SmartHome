import { defineStore } from 'pinia'

/**
 * 应用级状态（Pinia）。当前仅存放侧边栏折叠状态与家庭名称占位，
 * 正式对接后再补充用户/家庭等全局信息。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    // 当前家庭名称（占位，后续从后端获取）
    familyName: '我的家庭',
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
  },
})
