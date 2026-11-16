import api from './api'
import type { ApiResponse } from '../types/api'
import type { NotificationItem } from '../types/notification'

const notificationService = {
  async listMyNotifications(): Promise<NotificationItem[]> {
    const response = await api.get<ApiResponse<NotificationItem[]>>('/notifications/me')
    return Array.isArray(response.data.data) ? response.data.data : []
  },

  async markAsRead(id: string): Promise<void> {
    await api.patch(`/notifications/${encodeURIComponent(id)}/read`)
  },
}

export default notificationService
