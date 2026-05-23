import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDashboard, invalidateDashboardCache } from '@/api/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const email = ref({ unreadCount: 0 })
  const calendar = ref({ todayEvents: [] })
  const drive = ref({ recentFiles: [] })
  const conversations = ref([])
  const loading = ref(false)
  const error = ref(null)

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      const res = await getDashboard()
      const data = res.data.data
      email.value = data.email ?? { unreadCount: 0 }
      calendar.value = data.calendar ?? { todayEvents: [] }
      drive.value = data.drive ?? { recentFiles: [] }
      conversations.value = data.conversations ?? []
    } catch (e) {
      error.value = e.message
    } finally {
      loading.value = false
    }
  }

  async function refresh() {
    await invalidateDashboardCache()
    await fetch()
  }

  return { email, calendar, drive, conversations, loading, error, fetch, refresh }
})
