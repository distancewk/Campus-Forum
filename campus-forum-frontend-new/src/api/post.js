import request from '@/utils/request'

// 获取帖子列表
export function getPostList(params) {
  return request.get('/posts', { params })
}

// 获取帖子详情
export function getPostDetail(id) {
  return request.get(`/posts/${id}`)
}

// 发布帖子
export function createPost(data) {
  return request.post('/posts', data)
}

// 编辑帖子
export function updatePost(id, data) {
  return request.put(`/posts/${id}`, data)
}

// 删除帖子
export function deletePost(id) {
  return request.delete(`/posts/${id}`)
}

// 上传帖子图片
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/posts/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取评论列表
export function getComments(postId, params) {
  return request.get(`/posts/${postId}/comments`, { params })
}

// 发表评论
export function createComment(postId, data) {
  return request.post(`/posts/${postId}/comments`, data)
}

// 删除评论
export function deleteComment(postId, commentId) {
  return request.delete(`/posts/${postId}/comments/${commentId}`)
}
