<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="chat-container">
      <AppHeader :title="currentTitle" />

      <!-- WebSocket 재연결 배너 -->
      <div v-if="connectionStatus === 'reconnecting'" class="reconnect-banner">
        ● 연결이 끊겼습니다. 재연결 중... ({{ reconnectCount }}/3)
      </div>

      <McpErrorBanner />
      <MessageList @select-prompt="handlePromptSelect" @retry="handleRetry" />
      <MessageInput ref="inputRef" @send="handleSend" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import MessageList from '@/components/chat/MessageList.vue'
import MessageInput from '@/components/chat/MessageInput.vue'
import McpErrorBanner from '@/components/McpErrorBanner.vue'
import { useConversationStore } from '@/stores/conversation'
import { useMessageStore } from '@/stores/message'
import { useWebSocket } from '@/composables/useWebSocket'
import { useToast } from '@/composables/useToast'

const { error: showError } = useToast()

const route = useRoute()
const router = useRouter()
const convStore = useConversationStore()
const messageStore = useMessageStore()
const { connectionStatus, reconnectCount, connect, sendMessage, disconnect } = useWebSocket()

const inputRef = ref(null)

const conversationId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : null
})

const currentTitle = computed(() => {
  if (!conversationId.value) return 'SmartFlow'
  const conv = convStore.conversations.find(c => c.id === conversationId.value)
  return conv?.title || '새 대화'
})

watch(conversationId, async (id) => {
  messageStore.clearMessages()
  if (id) {
    convStore.setCurrentId(id)
    await messageStore.fetchHistory(id)
    connect(id)
    await nextTick()
    const autoMsg = route.query.msg
    if (autoMsg) {
      router.replace({ query: {} })
      await handleSend(String(autoMsg))
    }
  }
}, { immediate: true })

onUnmounted(() => {
  convStore.setCurrentId(null)
  disconnect()
})

async function handleSend(text) {
  if (!conversationId.value) return
  messageStore.addUserMessage(text)
  sendMessage(conversationId.value, text)
}

function handlePromptSelect(text) {
  inputRef.value?.setContent(text)
}

function handleRetry() {
  const lastUserMsg = [...messageStore.messages].reverse().find(m => m.role === 'USER')
  if (lastUserMsg) handleSend(lastUserMsg.content)
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.reconnect-banner {
  background: #744210;
  color: #fefcbf;
  text-align: center;
  padding: 8px;
  font-size: 13px;
}
</style>
