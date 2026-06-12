import request from '@/utils/request'

// 获取用户列表
export function getUserList(params) {
  return request.get('/admin/users', { params })
}

// 禁用/启用用户
export function updateUserStatus(id, status) {
  return request.put(`/admin/users/${id}/status`, { status })
}

// 获取待审核帖子
export function getPendingPosts(params) {
  return request.get('/admin/posts/pending', { params })
}

// 审核帖子
export function auditPost(id, approved) {
  return request.put(`/admin/posts/${id}/audit`, { approved })
}

// 获取待审核评论
export function getPendingComments(params) {
  return request.get('/admin/comments/pending', { params })
}

// 审核评论
export function auditComment(id, approved) {
  return request.put(`/admin/comments/${id}/audit`, { approved })
}

// 精华/取消精华评论
export function toggleCommentFeature(id) {
  return request.put(`/admin/comments/${id}/feature`)
}

// 置顶/取消置顶
export function togglePin(id) {
  return request.put(`/admin/posts/${id}/pin`)
}

// 精华/取消精华
export function toggleFeature(id) {
  return request.put(`/admin/posts/${id}/feature`)
}

// 管理员删除帖子
export function adminDeletePost(id) {
  return request.delete(`/admin/posts/${id}`)
}

// 获取数据统计
export function getDashboard() {
  return request.get('/admin/dashboard')
}
