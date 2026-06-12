import request from '@/utils/request'

// 获取会话列表
export function getConversations() {
  return request.get('/messages/conversations')
}

// 获取聊天记录
export function getChatHistory(userId, params) {
  return request.get(`/messages/conversations/${userId}`, { params })
}

// 获取未读消息数
export function getUnreadCount() {
  return request.get('/messages/unread-count')
}

// 标记已读
export function markAsRead(userId) {
  return request.put(`/messages/conversations/${userId}/read`)
}
