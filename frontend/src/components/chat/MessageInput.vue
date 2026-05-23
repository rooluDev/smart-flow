<template>
  <div class="input-area">
    <div class="input-wrapper">
      <textarea
        ref="textareaRef"
        v-model="inputText"
        :disabled="isStreaming"
        placeholder="메시지를 입력하세요... (Shift+Enter: 줄바꿈)"
        class="message-textarea"
        rows="1"
        @keydown.enter.exact.prevent="send"
        @input="autoResize"
      ></textarea>
      <button
        class="send-btn"
        :disabled="!canSend"
        @click="send"
      >
        <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
          <path d="M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8-8-8z"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useMessageStore } from '@/stores/message'

const emit = defineEmits(['send'])
const props = defineProps({
  modelValue: String
})

const messageStore = useMessageStore()
const inputText = ref(props.modelValue || '')
const textareaRef = ref(null)

const isStreaming = computed(() => messageStore.isStreaming)
const canSend = computed(() => inputText.value.trim().length > 0 && !isStreaming.value)

function send() {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return
  emit('send', text)
  inputText.value = ''
  nextResize()
}

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 160) + 'px'
}

function nextResize() {
  setTimeout(() => {
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  }, 0)
}

function setContent(text) {
  inputText.value = text
  setTimeout(autoResize, 0)
}

defineExpose({ setContent })
</script>

<style scoped>
.input-area {
  border-top: 0.5px solid var(--color-border-primary);
  padding: 12px 16px;
  background: var(--color-background-primary);
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  border: 1px solid var(--color-border-primary);
  border-radius: 12px;
  padding: 8px 8px 8px 14px;
  background: var(--color-background-secondary);
  transition: border-color 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--color-accent);
}

.message-textarea {
  flex: 1;
  resize: none;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  color: var(--color-text-primary);
  min-height: 24px;
  max-height: 160px;
  overflow-y: auto;
  font-family: inherit;
}

.message-textarea::placeholder {
  color: var(--color-text-secondary);
}

.message-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--color-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.send-btn:disabled {
  background: var(--color-border-primary);
  cursor: not-allowed;
}
</style>
