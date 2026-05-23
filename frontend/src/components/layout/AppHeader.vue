<template>
  <header class="app-header">
    <span class="header-title">{{ title || 'SmartFlow' }}</span>
    <div class="header-actions">
      <span class="user-name">{{ authStore.user?.name }}</span>
      <button class="logout-btn" @click="logout">로그아웃</button>
    </div>
  </header>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useAuth } from '@/composables/useAuth'

defineProps({ title: String })

const authStore = useAuthStore()
const { logout } = useAuth()
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-border-primary);
  background: var(--color-background-primary);
  min-height: 52px;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.logout-btn {
  padding: 6px 12px;
  border: 1px solid var(--color-border-primary);
  border-radius: var(--border-radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  border-color: var(--color-error);
  color: var(--color-error);
}
</style>
