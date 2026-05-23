import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!accessToken.value)

  function setAuth(token, userData) {
    accessToken.value = token
    user.value = userData
    localStorage.setItem('accessToken', token)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  function clearAuth() {
    accessToken.value = null
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('user')
    localStorage.removeItem('refreshToken')
  }

  async function loginWithGoogle(code, redirectUri) {
    const data = await authApi.googleLogin(code, redirectUri)
    localStorage.setItem('refreshToken', data.accessToken)
    setAuth(data.accessToken, data.user)
    return data
  }

  async function refreshToken() {
    const stored = localStorage.getItem('refreshToken')
    if (!stored) throw new Error('No refresh token')
    const data = await authApi.refresh(stored)
    accessToken.value = data.accessToken
    localStorage.setItem('accessToken', data.accessToken)
    return data.accessToken
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // ignore
    } finally {
      clearAuth()
    }
  }

  return { accessToken, user, isAuthenticated, setAuth, clearAuth, loginWithGoogle, refreshToken, logout }
})
