import api from './axios'

export const getDashboard = () => api.get('/api/dashboard')
export const invalidateDashboardCache = () => api.delete('/api/dashboard/cache')
