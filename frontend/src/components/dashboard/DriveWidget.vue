<template>
  <div class="widget">
    <div class="widget-header">
      <span class="widget-icon">📁</span>
      <span class="widget-title">최근 Drive 파일</span>
    </div>

    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="skeleton-file" />
    </template>

    <template v-else>
      <div v-if="files.length === 0" class="empty-state">
        최근 파일이 없습니다
      </div>

      <a
        v-for="file in files"
        :key="file.id"
        :href="file.webViewLink"
        target="_blank"
        rel="noopener"
        class="file-item"
      >
        <span class="file-icon">{{ mimeIcon(file.mimeType) }}</span>
        <div class="file-body">
          <div class="file-name">{{ file.name }}</div>
          <div class="file-time">{{ formatRelative(file.modifiedTime) }}</div>
        </div>
      </a>

      <button class="widget-action" @click="askAI('최근 드라이브 파일 목록을 정리해줘')">
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
  drive: { type: Object, default: () => ({ recentFiles: [] }) },
  loading: Boolean,
})

const files = computed(() => props.drive?.recentFiles ?? [])

const router = useRouter()
const convStore = useConversationStore()

function mimeIcon(mimeType) {
  if (!mimeType) return '📄'
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel')) return '📊'
  if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return '📊'
  if (mimeType.includes('document') || mimeType.includes('word')) return '📝'
  if (mimeType.includes('pdf')) return '📕'
  if (mimeType.includes('image')) return '🖼'
  if (mimeType.includes('video')) return '🎬'
  if (mimeType.includes('folder')) return '📂'
  return '📄'
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

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #2d2d44;
  text-decoration: none;
  transition: opacity 0.15s;
}

.file-item:last-of-type { border-bottom: none; }
.file-item:hover { opacity: 0.8; }

.file-icon { font-size: 20px; flex-shrink: 0; }

.file-name {
  font-size: 13px;
  color: #e2e8f0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
}

.file-time {
  font-size: 11px;
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

.skeleton-file {
  height: 44px;
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
