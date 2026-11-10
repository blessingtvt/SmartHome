# smart-home-frontend —— 智能家居家属端

面向老年人的智能家居主动感知系统的 Vue 3 家属端（阶段七）。

## 技术栈

- Vue 3 + Vite（JavaScript）
- Element Plus（UI 组件库）
- Vue Router（路由）
- Pinia（状态管理）
- Axios（HTTP 请求）

## 安装

```bash
cd frontend
npm install
```

## 启动（开发）

```bash
npm run dev
```

启动后访问：<http://localhost:5173>

开发环境通过 Vite 代理把 `/api` 开头的请求转发到本地网关 `http://127.0.0.1:8080`，因此启动前请先确保后端 Gateway（8080）已运行。

## 构建（生产）

```bash
npm run build     # 产物输出到 dist/
npm run preview   # 本地预览构建产物
```

生产环境如需指定网关地址，可通过环境变量 `VITE_API_BASE_URL` 配置（未配置时开发环境走 `/api` 代理）。

## 目录约定

```
frontend/
├── index.html
├── vite.config.js
└── src/
    ├── api/         # Axios 实例与接口封装（正式对接后再补充）
    ├── assets/      # 全局样式
    ├── components/  # 可复用组件
    ├── layouts/     # 布局（侧边导航 + 顶部状态栏）
    ├── router/      # 路由
    ├── stores/      # Pinia 状态
    ├── utils/       # 工具函数
    └── views/       # 页面（dashboard/environment/devices/alarms/rules）
```

> 注意：本阶段只做基础工程，页面使用模拟数据与空状态，暂未调用后端接口。正式对接前须逐个核对后端 Controller / DTO / VO。
