import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/oauth/callback',
    name: 'OAuthCallback',
    component: () => import('@/views/OAuthCallbackView.vue'),
    meta: { public: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue')
  },
  {
    path: '/chat/:id?',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue')
  },
  {
    path: '/',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isAuthenticated) {
    return next('/login')
  }
  if (to.path === '/login' && authStore.isAuthenticated) {
    return next('/dashboard')
  }
  next()
})

const pageTitles = {
  Login: 'SmartFlow — 로그인',
  Dashboard: 'SmartFlow — 대시보드',
  Chat: 'SmartFlow — 채팅',
  OAuthCallback: 'SmartFlow',
}

router.afterEach((to) => {
  document.title = pageTitles[to.name] || 'SmartFlow'
})

export default router
