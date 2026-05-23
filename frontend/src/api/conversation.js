import axiosInstance from './axios'

export const conversationApi = {
  async getList(page = 0, size = 20) {
    const res = await axiosInstance.get('/api/conversations', { params: { page, size } })
    return res.data.data
  },

  async create() {
    const res = await axiosInstance.post('/api/conversations')
    return res.data.data
  },

  async getOne(id) {
    const res = await axiosInstance.get(`/api/conversations/${id}`)
    return res.data.data
  },

  async updateTitle(id, title) {
    const res = await axiosInstance.patch(`/api/conversations/${id}/title`, { title })
    return res.data.data
  },

  async remove(id) {
    await axiosInstance.delete(`/api/conversations/${id}`)
  }
}
