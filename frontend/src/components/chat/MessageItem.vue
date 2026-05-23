<template>
  <div class="message-wrapper" :class="message.role.toLowerCase()">
    <template v-if="message.role === 'ASSISTANT'">
      <div class="avatar">SF</div>
      <div class="bubble-group">
        <div v-if="message.mcpCallName && message.isStreaming" class="mcp-loading">
          <div class="dots">
            <span></span><span></span><span></span>
          </div>
          <span class="mcp-text">{{ mcpLabel(message.mcpCallName) }} 확인 중...</span>
        </div>
        <div
          class="bubble assistant"
          :class="{ error: message.isError }"
        >
          <span v-if="message.mcpCallName && !message.isStreaming" class="mcp-badge">
            {{ mcpLabel(message.mcpCallName) }} 조회됨
          </span>
          <div class="content" v-html="renderContent(message.content)"></div>
          <StreamingCursor v-if="message.isStreaming" />
          <button v-if="message.isError" class="retry-btn" @click="$emit('retry')">
            다시 시도
          </button>
        </div>
        <span class="timestamp">{{ formatTime(message.createdAt) }}</span>
      </div>
    </template>

    <template v-else>
      <div class="bubble-group user-group">
        <div class="bubble user">
          <div class="content">{{ message.content }}</div>
        </div>
        <span class="timestamp">{{ formatTime(message.createdAt) }}</span>
      </div>
    </template>
  </div>
</template>

<script setup>
import StreamingCursor from './StreamingCursor.vue'

defineEmits(['retry'])

const props = defineProps({
  message: { type: Object, required: true }
})

function renderContent(content) {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

function formatTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

function mcpLabel(name) {
  const labels = { gmail: 'Gmail', calendar: 'Calendar', drive: 'Drive' }
  return labels[name] || name
}
</script>

<style scoped>
.message-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 6px 20px;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 2px;
}

.bubble-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 85%;
}

.user-group {
  align-items: flex-end;
  max-width: 70%;
}

.bubble {
  padding: 10px 14px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble.user {
  background: var(--color-background-secondary);
  border-radius: 20px 20px 4px 20px;
  color: var(--color-text-primary);
}

.bubble.assistant {
  background: var(--color-background-primary);
  border: 0.5px solid var(--color-border-tertiary);
  border-radius: 4px 20px 20px 20px;
  color: var(--color-text-primary);
}

.bubble.assistant.error {
  border-color: var(--color-error);
  background: #fff5f5;
}

.mcp-badge {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(29, 158, 117, 0.12);
  color: var(--color-accent);
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 6px;
}

.mcp-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--color-background-secondary);
  border-radius: 4px 20px 20px 20px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.dots {
  display: flex;
  gap: 3px;
}

.dots span {
  width: 6px;
  height: 6px;
  background: var(--color-accent);
  border-radius: 50%;
  animation: bounce 1.2s ease-in-out infinite;
}

.dots span:nth-child(2) { animation-delay: 0.2s; }
.dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.retry-btn {
  display: inline-flex;
  margin-top: 8px;
  padding: 5px 12px;
  background: transparent;
  border: 1px solid var(--color-error);
  color: var(--color-error);
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: rgba(229, 62, 62, 0.08);
}

.timestamp {
  font-size: 11px;
  color: var(--color-text-secondary);
  padding: 0 4px;
}
</style>
