/**
 * 通用格式化工具（占位，后续按需扩展）。
 */

// 将 epoch 毫秒或可解析的时间值格式化为 YYYY-MM-DD HH:mm:ss
export function formatDateTime(value) {
  if (value === null || value === undefined || value === '') return '--'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const p = (n) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ` +
    `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  )
}
