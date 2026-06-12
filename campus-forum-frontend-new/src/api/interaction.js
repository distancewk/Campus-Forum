import request from '@/utils/request'

// 点赞/取消点赞
export function toggleLike(data) {
  return request.post('/likes', data)
}

// 收藏/取消收藏
export function toggleFavorite(data) {
  return request.post('/favorites', data)
}

// 获取我的收藏列表
export function getMyFavorites(params) {
  return request.get('/favorites', { params })
}
