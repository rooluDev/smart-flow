import axiosInstance from './axios'

export const authApi = {
  async googleLogin(code, redirectUri) {
    const res = await axiosInstance.post('/api/auth/google', { code, redirectUri })
    return res.data.data
  },

  async refresh(refreshToken) {
    const res = await axiosInstance.post('/api/auth/refresh', { refreshToken })
    return res.data.data
  },

  async logout() {
    const res = await axiosInstance.post('/api/auth/logout')
    return res.data
  },

  async getMe() {
    const res = await axiosInstance.get('/api/users/me')
    return res.data.data
  }
}
