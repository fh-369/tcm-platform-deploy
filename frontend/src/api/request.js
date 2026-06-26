import axios from 'axios'
import { ElMessage } from 'element-plus'

import { clearSession, getBrowserStorage, loadSession } from '../stores/authSession'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const { token } = loadSession(getBrowserStorage())

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status

    if (status === 401) {
      error.message = '登录已过期，请重新登录'
      clearSession(getBrowserStorage())
      ElMessage.warning('登录已过期，请重新登录')

      if (typeof window !== 'undefined' && !window.location?.pathname?.startsWith('/login')) {
        window.location.assign('/login')
      }
    } else if (status === 403) {
      error.message = error?.response?.data?.message || '当前账号无权执行此操作'
      ElMessage.warning(error.message)
    }

    return Promise.reject(error)
  },
)

export default request
