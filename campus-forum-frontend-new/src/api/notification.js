import request from '@/utils/request'

// 获取我的通知列表（分页）
export function getNotifications(params) {
  return request.get('/notifications', { params })
}

// 未读通知数
export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

// 标记单条已读
export function markNotificationRead(id) {
  return request.post(`/notifications/${id}/read`)
}

// 全部标记为已读
export function markAllRead() {
  return request.post('/notifications/read-all')
}
