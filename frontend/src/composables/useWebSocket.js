import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useMessageStore } from '@/stores/message'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'

const MAX_RECONNECT = 3

export function useWebSocket() {
  const messageStore = useMessageStore()
  const authStore = useAuthStore()

  const connectionStatus = ref('disconnected')
  const reconnectCount = ref(0)
  let stompClient = null
  let currentSubscription = null
  let reconnectTimer = null

  function connect(conversationId) {
    if (stompClient?.connected) {
      subscribe(conversationId)
      return
    }

    stompClient = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL),
      connectHeaders: {
        Authorization: `Bearer ${authStore.accessToken}`
      },
      reconnectDelay: 0,
      onConnect: () => {
        connectionStatus.value = 'connected'
        reconnectCount.value = 0
        subscribe(conversationId)
      },
      onDisconnect: () => {
        connectionStatus.value = 'disconnected'
        scheduleReconnect(conversationId)
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame)
        connectionStatus.value = 'error'
      }
    })

    connectionStatus.value = 'connecting'
    stompClient.activate()
  }

  function subscribe(conversationId) {
    currentSubscription?.unsubscribe()
    currentSubscription = stompClient.subscribe(
      `/topic/chat/${conversationId}`,
      (frame) => {
        const chunk = JSON.parse(frame.body)
        switch (chunk.type) {
          case 'CHUNK':
            messageStore.appendChunk(chunk.content)
            break
          case 'MCP_CALL':
            messageStore.setMcpCall(chunk.content)
            break
          case 'DONE':
            messageStore.finishStreaming(chunk.messageId)
            break
          case 'ERROR':
            messageStore.handleError(chunk.content)
            useToast().error(chunk.content)
            break
        }
      }
    )
  }

  function sendMessage(conversationId, content) {
    if (!stompClient?.connected) return
    stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ conversationId, content })
    })
    messageStore.startStreaming()
  }

  function scheduleReconnect(conversationId) {
    if (reconnectCount.value >= MAX_RECONNECT) return
    reconnectCount.value++
    connectionStatus.value = 'reconnecting'
    reconnectTimer = setTimeout(() => {
      connect(conversationId)
    }, 3000)
  }

  function disconnect() {
    clearTimeout(reconnectTimer)
    currentSubscription?.unsubscribe()
    stompClient?.deactivate()
    stompClient = null
    connectionStatus.value = 'disconnected'
  }

  onUnmounted(disconnect)

  return { connectionStatus, reconnectCount, connect, sendMessage, disconnect }
}
