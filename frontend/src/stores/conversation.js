import { defineStore } from 'pinia'
import { ref } from 'vue'
import { conversationApi } from '@/api/conversation'

export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref([])
  const currentId = ref(null)
  const isLoading = ref(false)

  async function fetchList() {
    isLoading.value = true
    try {
      const data = await conversationApi.getList()
      conversations.value = data.conversations
    } finally {
      isLoading.value = false
    }
  }

  async function create() {
    const data = await conversationApi.create()
    await fetchList()
    currentId.value = data.id
    return data
  }

  async function remove(id) {
    await conversationApi.remove(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    if (currentId.value === id) {
      currentId.value = null
    }
  }

  async function updateTitle(id, title) {
    const data = await conversationApi.updateTitle(id, title)
    const conv = conversations.value.find(c => c.id === id)
    if (conv) conv.title = data.title
    return data
  }

  function setCurrentId(id) {
    currentId.value = id
  }

  return { conversations, currentId, isLoading, fetchList, create, remove, updateTitle, setCurrentId }
})
