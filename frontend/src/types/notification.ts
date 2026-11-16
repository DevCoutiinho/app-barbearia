export type NotificationType =
  | 'NEW_APPOINTMENT'
  | 'APPOINTMENT_CANCELED'
  | 'APPOINTMENT_REMINDER'
  | 'LOW_STOCK'
  | 'SYSTEM'

export interface NotificationItem {
  id: string
  title: string
  message: string
  type: NotificationType
  read: boolean
  createdAt: string
}
