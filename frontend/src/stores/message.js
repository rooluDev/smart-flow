import { defineStore } from 'pinia'
import { ref } from 'vue'
import { conversationApi } from '@/api/conversation'

export const useMessageStore = defineStore('message', () => {
  const messages = ref([])
  const isStreaming = ref(false)
  const streamingContent = ref('')
  const mcpCallName = ref(null)

  async function fetchHistory(conversationId) {
    const data = await conversationApi.getOne(conversationId)
    messages.value = data.messages
  }

  function startStreaming() {
    isStreaming.value = true
    streamingContent.value = ''
    mcpCallName.value = null
  }

  function appendChunk(content) {
    streamingContent.value += content
  }

  function setMcpCall(name) {
    mcpCallName.value = name
  }

  function finishStreaming(messageId) {
    if (streamingContent.value) {
      messages.value.push({
        id: messageId,
        role: 'ASSISTANT',
        content: streamingContent.value,
        mcpToolsUsed: null,
        createdAt: new Date().toISOString()
      })
    }
    isStreaming.value = false
    streamingContent.value = ''
    mcpCallName.value = null
  }

  function handleError(errorMessage) {
    messages.value.push({
      id: null,
      role: 'ASSISTANT',
      content: errorMessage,
      isError: true,
      createdAt: new Date().toISOString()
    })
    isStreaming.value = false
    streamingContent.value = ''
    mcpCallName.value = null
  }

  function addUserMessage(content) {
    messages.value.push({
      id: null,
      role: 'USER',
      content,
      createdAt: new Date().toISOString()
    })
  }

  function clearMessages() {
    messages.value = []
    isStreaming.value = false
    streamingContent.value = ''
    mcpCallName.value = null
  }

  return {
    messages, isStreaming, streamingContent, mcpCallName,
    fetchHistory, startStreaming, appendChunk, setMcpCall,
    finishStreaming, handleError, addUserMessage, clearMessages
  }
})
