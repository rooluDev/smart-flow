<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo">
        <span class="logo-mark">SF</span>
        <span class="logo-name">SmartFlow</span>
      </div>
      <button class="new-chat-btn" @click="createChat">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
          <path d="M19 11H13V5a1 1 0 00-2 0v6H5a1 1 0 000 2h6v6a1 1 0 002 0v-6h6a1 1 0 000-2z"/>
        </svg>
        새 대화
      </button>
    </div>

    <nav class="sidebar-nav">
      <p class="section-label">최근 대화</p>
      <p v-if="convStore.conversations.length === 0" class="empty-hint">
        아직 대화가 없습니다
      </p>
      <div
        v-for="conv in convStore.conversations"
        :key="conv.id"
        class="conv-item"
        :class="{ active: convStore.currentId === conv.id }"
        @click="openChat(conv.id)"
      >
        <div class="conv-info">
          <span class="conv-title">{{ conv.title }}</span>
          <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
        </div>
        <button class="delete-btn" @click.stop="confirmDelete(conv)" title="삭제">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
          </svg>
        </button>
      </div>
    </nav>

    <div class="sidebar-footer">
      <div v-if="authStore.user" class="user-info">
        <div class="user-avatar">{{ authStore.user.name[0] }}</div>
        <span class="user-name">{{ authStore.user.name }}</span>
      </div>
    </div>

    <!-- 삭제 확인 모달 -->
    <div v-if="deleteTarget" class="modal-overlay" @click.self="deleteTarget = null">
      <div class="modal">
        <p class="modal-title">대화를 삭제할까요?</p>
        <p class="modal-desc">"{{ deleteTarget.title }}" 대화가 영구 삭제됩니다.</p>
        <div class="modal-actions">
          <button class="btn-cancel" @click="deleteTarget = null">취소</button>
          <button class="btn-delete" @click="doDelete">삭제</button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useConversationStore } from '@/stores/conversation'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const convStore = useConversationStore()
const authStore = useAuthStore()
const deleteTarget = ref(null)

onMounted(() => convStore.fetchList())

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diffDays = Math.floor((now - d) / 86400000)
  if (diffDays === 0) return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
  if (diffDays === 1) return '어제'
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

async function createChat() {
  const data = await convStore.create()
  router.push(`/chat/${data.id}`)
}

function openChat(id) {
  convStore.setCurrentId(id)
  router.push(`/chat/${id}`)
}

function confirmDelete(conv) {
  deleteTarget.value = conv
}

async function doDelete() {
  if (!deleteTarget.value) return
  const id = deleteTarget.value.id
  deleteTarget.value = null
  await convStore.remove(id)
  if (router.currentRoute.value.params.id == id) {
    router.push('/dashboard')
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  background: var(--color-background-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--color-accent);
  color: white;
  font-weight: 700;
  font-size: 13px;
  border-radius: 8px;
}

.logo-name {
  color: white;
  font-weight: 600;
  font-size: 15px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.08);
  border: 0.5px solid rgba(255, 255, 255, 0.15);
  border-radius: var(--border-radius-sm);
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  width: 100%;
}

.new-chat-btn:hover {
  background: rgba(255, 255, 255, 0.14);
}

.sidebar-nav {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}

.section-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.35);
  padding: 8px 8px 4px;
}

.empty-hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.3);
  padding: 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 8px;
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  transition: background 0.15s;
  border-left: 2px solid transparent;
}

.conv-item:hover {
  background: rgba(255, 255, 255, 0.07);
}

.conv-item.active {
  background: rgba(255, 255, 255, 0.1);
  border-left-color: var(--color-accent);
}

.conv-item:hover .delete-btn {
  opacity: 1;
}

.conv-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.conv-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

.delete-btn {
  opacity: 0;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.15s;
  flex-shrink: 0;
}

.delete-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fc8181;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* 삭제 모달 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal {
  background: var(--color-background-primary);
  border-radius: var(--border-radius-lg);
  padding: 24px;
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.modal-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.btn-cancel {
  padding: 8px 16px;
  border: 1px solid var(--color-border-primary);
  border-radius: var(--border-radius-sm);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 14px;
}

.btn-delete {
  padding: 8px 16px;
  border: none;
  border-radius: var(--border-radius-sm);
  background: var(--color-error);
  color: white;
  cursor: pointer;
  font-size: 14px;
}
</style>
