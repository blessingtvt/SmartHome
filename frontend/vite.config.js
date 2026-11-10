import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 前端基础工程（阶段七）：Vue 3 + Vite + Element Plus
// 开发环境统一通过 /api 前缀代理到本地 Gateway 8080，后端无需单独配置 CORS。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      // 将 /api 开头的请求转发到本地网关，路径原样透传：/api/v1/xxx -> http://127.0.0.1:8080/api/v1/xxx
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
