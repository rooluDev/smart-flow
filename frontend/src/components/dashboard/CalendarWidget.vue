<template>
  <div class="widget">
    <div class="widget-header">
      <span class="widget-icon">📅</span>
      <span class="widget-title">오늘 일정</span>
    </div>

    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="skeleton-event" />
    </template>

    <template v-else>
      <div v-if="events.length === 0" class="empty-state">
        오늘 일정이 없습니다
      </div>

      <div v-for="event in events" :key="event.id" class="event-item">
        <div class="event-time">{{ formatTime(event.startTime) }}</div>
        <div class="event-body">
          <div class="event-title">{{ event.title }}</div>
          <div v-if="event.location" class="event-location">📍 {{ event.location }}</div>
        </div>
      </div>

      <button class="widget-action" @click="askAI('오늘 일정을 알려주고 준비할 것을 정리해줘')">
        AI로 정리하기
      </button>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useConversationStore } from '@/stores/conversation'

const props = defineProps({
  calendar: { type: Object, default: () => ({ todayEvents: [] }) },
  loading: Boolean,
})

const events = computed(() => props.calendar?.todayEvents ?? [])

const router = useRouter()
const convStore = useConversationStore()

function formatTime(dateTime) {
  if (!dateTime) return '종일'
  const d = new Date(dateTime)
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false })
}

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

.widget-icon { font-size: 18px; }

.widget-title {
  font-size: 14px;
  font-weight: 600;
  color: #a0aec0;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.empty-state {
  color: #4a5568;
  font-size: 14px;
  padding: 12px 0;
  text-align: center;
}

.event-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #2d2d44;
}

.event-item:last-of-type {
  border-bottom: none;
}

.event-time {
  font-size: 12px;
  color: #81e6d9;
  white-space: nowrap;
  padding-top: 2px;
  min-width: 40px;
}

.event-title {
  font-size: 14px;
  color: #e2e8f0;
  font-weight: 500;
}

.event-location {
  font-size: 12px;
  color: #718096;
  margin-top: 2px;
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

.skeleton-event {
  height: 48px;
  background: linear-gradient(90deg, #2d2d44 25%, #3a3a56 50%, #2d2d44 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
