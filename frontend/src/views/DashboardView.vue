<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="dashboard-container">
      <AppHeader title="Dashboard" />

      <div v-if="dashStore.error" class="error-banner">
        대시보드 데이터를 불러오지 못했습니다.
        <button class="error-retry" @click="dashStore.fetch()">다시 시도</button>
      </div>

      <div class="dashboard-body">
        <!-- Quick Input Bar -->
        <div class="quick-input-section">
          <div class="quick-input-wrapper">
            <textarea
              v-model="quickInput"
              class="quick-input"
              placeholder="AI에게 무엇이든 물어보세요..."
              rows="1"
              @input="autoResize"
              @keydown.enter.exact.prevent="sendQuickMessage"
              @keydown.enter.shift.exact="() => {}"
            />
            <button
              class="quick-send-btn"
              :disabled="!quickInput.trim() || sending"
              @click="sendQuickMessage"
            >
              <span v-if="sending">...</span>
              <span v-else>→</span>
            </button>
          </div>
        </div>

        <!-- Widget Grid -->
        <div class="widget-grid">
          <EmailWidget :email="dashStore.email" :loading="dashStore.loading" />
          <CalendarWidget :calendar="dashStore.calendar" :loading="dashStore.loading" />
          <DriveWidget :drive="dashStore.drive" :loading="dashStore.loading" />
        </div>

        <!-- Recent Conversations -->
        <div class="recent-section">
          <div class="section-header">
            <h3 class="section-title">최근 대화</h3>
            <button class="refresh-btn" :disabled="dashStore.loading" @click="dashStore.refresh()">
              새로고침
            </button>
          </div>

          <div v-if="dashStore.loading" class="conv-list">
            <div v-for="i in 5" :key="i" class="skeleton-conv" />
          </div>

          <div v-else-if="dashStore.conversations.length === 0" class="empty-conv">
            아직 대화가 없습니다. 위에서 AI에게 말을 걸어보세요!
          </div>

          <div v-else class="conv-list">
            <router-link
              v-for="conv in dashStore.conversations"
              :key="conv.id"
              :to="`/chat/${conv.id}`"
              class="conv-item"
            >
              <div class="conv-title">{{ conv.title }}</div>
              <div class="conv-preview">{{ conv.lastMessage || '메시지 없음' }}</div>
              <div class="conv-time">{{ formatRelative(conv.updatedAt) }}</div>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import EmailWidget from '@/components/dashboard/EmailWidget.vue'
import CalendarWidget from '@/components/dashboard/CalendarWidget.vue'
import DriveWidget from '@/components/dashboard/DriveWidget.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useConversationStore } from '@/stores/conversation'

const router = useRouter()
const dashStore = useDashboardStore()
const convStore = useConversationStore()

const quickInput = ref('')
const sending = ref(false)

onMounted(() => {
  dashStore.fetch()
})

function autoResize(e) {
  e.target.style.height = 'auto'
  e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px'
}

async function sendQuickMessage() {
  const text = quickInput.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    const { id } = await convStore.create()
    router.push({ path: `/chat/${id}`, query: { msg: text } })
  } finally {
    sending.value = false
  }
}

function formatRelative(dateTime) {
  if (!dateTime) return ''
  const diff = Date.now() - new Date(dateTime).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 60) return `${minutes}분 전`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`
  const days = Math.floor(hours / 24)
  return `${days}일 전`
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
}

.dashboard-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.error-banner {
  background: rgba(229, 62, 62, 0.1);
  border-bottom: 1px solid rgba(229, 62, 62, 0.3);
  color: #fc8181;
  padding: 10px 24px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.error-retry {
  background: transparent;
  border: 1px solid #fc8181;
  color: #fc8181;
  padding: 3px 10px;
  border-radius: 5px;
  font-size: 12px;
  cursor: pointer;
}

.dashboard-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Quick Input */
.quick-input-section {
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
}

.quick-input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #1a1a2e;
  border: 1px solid #2d9d92;
  border-radius: 12px;
  padding: 12px 14px;
}

.quick-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #e2e8f0;
  font-size: 15px;
  resize: none;
  line-height: 1.5;
  min-height: 24px;
  max-height: 120px;
  overflow-y: auto;
}

.quick-input::placeholder { color: #4a5568; }

.quick-send-btn {
  background: #2d9d92;
  border: none;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.quick-send-btn:disabled {
  background: #2d2d44;
  color: #4a5568;
  cursor: not-allowed;
}

.quick-send-btn:not(:disabled):hover { background: #38b2ac; }

/* Widget Grid */
.widget-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

@media (max-width: 900px) {
  .widget-grid { grid-template-columns: 1fr; }
}

/* Recent Conversations */
.recent-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #a0aec0;
  margin: 0;
}

.refresh-btn {
  background: transparent;
  border: 1px solid #2d2d44;
  color: #718096;
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  color: #81e6d9;
  border-color: #2d9d92;
}

.refresh-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.conv-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conv-item {
  background: #1a1a2e;
  border: 1px solid #2d2d44;
  border-radius: 10px;
  padding: 14px 16px;
  text-decoration: none;
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  gap: 4px 12px;
  transition: border-color 0.2s;
}

.conv-item:hover { border-color: #2d9d92; }

.conv-title {
  font-size: 14px;
  font-weight: 500;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-preview {
  font-size: 12px;
  color: #718096;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  grid-column: 1;
}

.conv-time {
  font-size: 11px;
  color: #4a5568;
  grid-row: 1;
  grid-column: 2;
  align-self: start;
  white-space: nowrap;
}

.empty-conv {
  color: #4a5568;
  font-size: 14px;
  text-align: center;
  padding: 24px;
}

.skeleton-conv {
  height: 62px;
  background: linear-gradient(90deg, #1a1a2e 25%, #2d2d44 50%, #1a1a2e 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 10px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
