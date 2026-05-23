<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">
        <div class="logo-mark">SF</div>
        <span class="logo-text">SmartFlow</span>
      </div>
      <p class="tagline">Gmail · Calendar · Drive를 AI로 한번에</p>

      <ul class="feature-list">
        <li><span class="feature-icon">✉</span> 이메일 요약 및 답장 초안</li>
        <li><span class="feature-icon">📅</span> 일정 조회 및 자동 생성</li>
        <li><span class="feature-icon">📁</span> 파일 검색 및 내용 요약</li>
      </ul>

      <button class="google-btn" :disabled="loading" @click="login">
        <svg class="google-icon" viewBox="0 0 24 24">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
        </svg>
        {{ loading ? '로그인 중...' : 'Google로 계속하기' }}
      </button>

      <p class="privacy-note">로그인 시 Google 계정 데이터 접근에 동의합니다</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { initiateGoogleLogin } = useAuth()
const loading = ref(false)

function login() {
  loading.value = true
  initiateGoogleLogin()
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-background-primary);
  background-image: radial-gradient(ellipse at 20% 50%, rgba(29, 158, 117, 0.08) 0%, transparent 60%),
                    radial-gradient(ellipse at 80% 20%, rgba(29, 158, 117, 0.05) 0%, transparent 50%);
}

.login-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 48px 52px;
  background: var(--color-background-secondary);
  border-radius: var(--border-radius-lg);
  border: 1px solid var(--color-border-primary);
  min-width: 380px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--color-accent), #17875f);
  color: white;
  font-weight: 800;
  font-size: 16px;
  border-radius: 14px;
  box-shadow: 0 4px 12px rgba(29, 158, 117, 0.3);
}

.logo-text {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
}

.tagline {
  color: var(--color-text-secondary);
  font-size: 14px;
  text-align: center;
  margin-top: -4px;
}

.feature-list {
  list-style: none;
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 0;
  border-top: 1px solid var(--color-border-primary);
  border-bottom: 1px solid var(--color-border-primary);
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.feature-icon {
  font-size: 16px;
  width: 24px;
  text-align: center;
}

.google-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 24px;
  border: 1px solid var(--color-border-primary);
  border-radius: var(--border-radius-sm);
  background: var(--color-background-primary);
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  justify-content: center;
}

.google-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(29, 158, 117, 0.1);
}

.google-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.google-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.privacy-note {
  font-size: 12px;
  color: var(--color-text-secondary);
  opacity: 0.6;
  text-align: center;
}
</style>
