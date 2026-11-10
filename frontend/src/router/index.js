import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

const routes = [
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '家庭总览', icon: 'HomeFilled' },
      },
      {
        path: 'environment',
        name: 'environment',
        component: () => import('@/views/environment/index.vue'),
        meta: { title: '环境监测', icon: 'Odometer' },
      },
      {
        path: 'devices',
        name: 'devices',
        component: () => import('@/views/devices/index.vue'),
        meta: { title: '设备管理', icon: 'Cpu' },
      },
      {
        path: 'alarms',
        name: 'alarms',
        component: () => import('@/views/alarms/index.vue'),
        meta: { title: '告警管理', icon: 'BellFilled' },
      },
      {
        path: 'rules',
        name: 'rules',
        component: () => import('@/views/rules/index.vue'),
        meta: { title: '自动化规则', icon: 'SetUp' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '家属端'} · 智能家居`
})

export default router
