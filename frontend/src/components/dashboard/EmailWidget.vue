<template>
  <div class="widget">
    <div class="widget-header">
      <span class="widget-icon">✉</span>
      <span class="widget-title">Gmail</span>
    </div>

    <template v-if="loading">
      <div class="skeleton skeleton-lg" />
      <div class="skeleton skeleton-sm" />
    </template>

    <template v-else>
      <div class="unread-count">{{ email.unreadCount }}</div>
      <div class="unread-label">읽지 않은 메일</div>

      <button class="widget-action" @click="askAI('받은 편지함에서 중요한 메일을 요약해줘')">
        AI로 요약하기
      </button>
    </template>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useConversationStore } from '@/stores/conversation'

const props = defineProps({
  email: { type: Object, default: () => ({ unreadCount: 0 }) },
  loading: Boolean,
})

const router = useRouter()
const convStore = useConversationStore()

async function askAI(text) {
  const { id } = await convStore.create()
  router.push({ path: `/chat/${id}`, query: { msg: text } })
}
</script>

<style scoped>
.widget {
  background: #1a1a2e;
  border: 1px solid #2d2d44;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.widget-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.widget-icon {
  font-size: 18px;
}

.widget-title {
  font-size: 14px;
  font-weight: 600;
  color: #a0aec0;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.unread-count {
  font-size: 48px;
  font-weight: 700;
  color: #81e6d9;
  line-height: 1;
}

.unread-label {
  font-size: 13px;
  color: #718096;
}

.widget-action {
  margin-top: 12px;
  background: transparent;
  border: 1px solid #2d9d92;
  color: #81e6d9;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  align-self: flex-start;
}

.widget-action:hover {
  background: rgba(45, 157, 146, 0.15);
}

.skeleton {
  background: linear-gradient(90deg, #2d2d44 25%, #3a3a56 50%, #2d2d44 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
}

.skeleton-lg { height: 56px; width: 80px; }
.skeleton-sm { height: 16px; width: 100px; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
