import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * Axios 统一实例。
 *
 * - 开发环境 baseURL 为空字符串，接口路径统一写 /api/v1/...，由 Vite 代理转发到网关 8080。
 * - 生产环境可通过 VITE_API_BASE_URL 指定网关地址（例如 http://<host>:8080）。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
})

// 响应拦截器：后端统一返回 Result<T> = { code, message, data }，且 HTTP 状态码恒为 200。
// 注意：此约定需在正式对接前，结合实际 Controller / GlobalExceptionHandler 再次核对，勿凭猜测使用。
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res.data
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
