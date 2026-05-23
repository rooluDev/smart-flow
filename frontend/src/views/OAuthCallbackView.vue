<template>
  <div class="callback-page">
    <div v-if="error" class="error-box">
      <p>로그인에 실패했습니다: {{ error }}</p>
      <button @click="$router.push('/login')">다시 시도</button>
    </div>
    <div v-else class="loading-box">
      <div class="spinner"></div>
      <p>로그인 처리 중...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const route = useRoute()
const router = useRouter()
const { handleOAuthCallback } = useAuth()
const error = ref(null)

onMounted(async () => {
  const code = route.query.code
  if (!code) {
    error.value = 'Authorization code가 없습니다.'
    return
  }
  try {
    await handleOAuthCallback(code)
  } catch (e) {
    error.value = e?.response?.data?.error?.message || '알 수 없는 오류가 발생했습니다.'
    setTimeout(() => router.push('/login'), 3000)
  }
})
</script>

<style scoped>
.callback-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

.loading-box, .error-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border-primary);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-box button {
  padding: 8px 16px;
  background: var(--color-accent);
  color: white;
  border: none;
  border-radius: var(--border-radius-sm);
  cursor: pointer;
}
</style>
