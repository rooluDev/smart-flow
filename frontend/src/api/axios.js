import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { useToast } from '@/composables/useToast'

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

axiosInstance.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

let isRefreshing = false
let failedQueue = []

function processQueue(error, token = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token)
  })
  failedQueue = []
}

axiosInstance.interceptors.response.use(
  response => response,
  async error => {
    const { status, data } = error.response ?? {}
    const errorCode = data?.error?.code
    const originalRequest = error.config

    if (status === 401 && errorCode === 'AUTH_004' && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return axiosInstance(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const authStore = useAuthStore()
        const newToken = await authStore.refreshToken()
        processQueue(null, newToken)
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return axiosInstance(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        const authStore = useAuthStore()
        authStore.clearAuth()
        router.push('/login')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    if (status === 401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push('/login')
    }

    if (status === 403) {
      const message = data?.error?.message || '접근 권한이 없습니다.'
      useToast().error(message)
    }

    if (status >= 500) {
      useToast().error(data?.error?.message || '서버 오류가 발생했습니다.')
    }

    return Promise.reject(error)
  }
)

export default axiosInstance
