import request from '@/utils/request'

// 获取用户公开信息
export function getUserProfile(id) {
  return request.get(`/users/${id}`)
}
