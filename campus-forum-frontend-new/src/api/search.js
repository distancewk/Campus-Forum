import request from '@/utils/request'

// 搜索帖子
export function searchPosts(params) {
  return request.get('/search', { params })
}
