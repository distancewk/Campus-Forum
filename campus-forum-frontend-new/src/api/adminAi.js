import request from '@/utils/request'

export function uploadAiDocument(formData) {
  return request.post('/admin/ai/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getAiDocuments(params) {
  return request.get('/admin/ai/documents', { params })
}

export function deleteAiDocument(id) {
  return request.delete(`/admin/ai/documents/${id}`)
}

export function reindexAiDocument(id) {
  return request.post(`/admin/ai/documents/${id}/reindex`)
}

export function getAiModeration(params) {
  return request.get('/admin/ai/moderation', { params })
}
