import request from '@/utils/request'

export function askAi(question) {
  return request.post('/ai/ask', { question })
}

export function getAiSessions(params) {
  return request.get('/ai/sessions', { params })
}

export function getAiSession(id) {
  return request.get(`/ai/sessions/${id}`)
}
