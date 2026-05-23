import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

export function useAuth() {
  const authStore = useAuthStore()
  const router = useRouter()

  async function handleOAuthCallback(code) {
    const redirectUri = import.meta.env.VITE_GOOGLE_REDIRECT_URI
    await authStore.loginWithGoogle(code, redirectUri)
    router.push('/dashboard')
  }

  async function logout() {
    await authStore.logout()
    router.push('/login')
  }

  function initiateGoogleLogin() {
    const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''
    const redirectUri = encodeURIComponent(import.meta.env.VITE_GOOGLE_REDIRECT_URI)
    const scopes = [
      'https://www.googleapis.com/auth/gmail.readonly',
      'https://www.googleapis.com/auth/gmail.compose',
      'https://www.googleapis.com/auth/calendar',
      'https://www.googleapis.com/auth/drive.readonly',
      'https://www.googleapis.com/auth/userinfo.email',
      'https://www.googleapis.com/auth/userinfo.profile',
      'openid'
    ].join(' ')
    const scope = encodeURIComponent(scopes)
    window.location.href =
      `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=${scope}&access_type=offline&prompt=consent`
  }

  return { handleOAuthCallback, logout, initiateGoogleLogin }
}
