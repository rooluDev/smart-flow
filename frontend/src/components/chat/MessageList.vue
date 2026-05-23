<template>
  <div class="message-list" ref="listRef">
    <div v-if="displayMessages.length === 0" class="empty-state">
      <p class="empty-title">무엇을 도와드릴까요?</p>
      <div class="prompt-cards">
        <button
          v-for="card in promptCards"
          :key="card.text"
          class="prompt-card"
          @click="$emit('select-prompt', card.text)"
        >
          <span class="card-icon">{{ card.icon }}</span>
          <span class="card-text">{{ card.text }}</span>
        </button>
      </div>
    </div>

    <MessageItem
      v-for="(msg, idx) in displayMessages"
      :key="msg.id ?? 'streaming-' + idx"
      :message="msg"
      @retry="$emit('retry')"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import MessageItem from './MessageItem.vue'
import { useStreamingMessage } from '@/composables/useStreamingMessage'

const emit = defineEmits(['select-prompt', 'retry'])
const { displayMessages } = useStreamingMessage()
const listRef = ref(null)

const promptCards = [
  { icon: '✉️', text: '오늘 받은 중요한 이메일 요약해줘' },
  { icon: '📅', text: '이번 주 일정 정리해줘' },
  { icon: '📄', text: '최근에 수정한 문서 보여줘' }
]

watch(displayMessages, () => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}, { deep: true })
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 40px;
}

.empty-title {
  font-size: 18px;
  color: var(--color-text-secondary);
}

.prompt-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 480px;
}

.prompt-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border: 1px solid var(--color-border-primary);
  border-radius: var(--border-radius-md);
  background: var(--color-background-secondary);
  color: var(--color-text-primary);
  font-size: 14px;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
}

.prompt-card:hover {
  border-color: var(--color-accent);
  background: var(--color-background-primary);
}

.card-icon {
  font-size: 18px;
}
</style>
