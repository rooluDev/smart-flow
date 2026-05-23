import { computed } from 'vue'
import { useMessageStore } from '@/stores/message'

export function useStreamingMessage() {
  const messageStore = useMessageStore()

  const displayMessages = computed(() => {
    const list = [...messageStore.messages]
    if (messageStore.isStreaming) {
      list.push({
        id: null,
        role: 'ASSISTANT',
        content: messageStore.streamingContent,
        mcpCallName: messageStore.mcpCallName,
        isStreaming: true,
        createdAt: new Date().toISOString()
      })
    }
    return list
  })

  return { displayMessages }
}
