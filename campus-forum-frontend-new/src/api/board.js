import request from '@/utils/request'

// 获取板块列表
export function getBoardList() {
  return request.get('/boards')
}

// 创建板块（管理员）
export function createBoard(data) {
  return request.post('/admin/boards', data)
}

// 更新板块（管理员）
export function updateBoard(id, data) {
  return request.put(`/admin/boards/${id}`, data)
}

// 删除板块（管理员）
export function deleteBoard(id) {
  return request.delete(`/admin/boards/${id}`)
}
