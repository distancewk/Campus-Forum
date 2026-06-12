import request from '@/utils/request'

// 发送注册验证码
export function sendCode(data) {
  return request.post('/auth/send-code', data)
}

// 注册
export function register(data) {
  return request.post('/auth/register', data)
}

// 验证码校验完成注册
export function verifyRegister(data) {
  return request.post('/auth/register/verify', data)
}

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 刷新 Token（标记 _retry 防止刷新失败时无限重试）
export function refreshToken() {
  return request.post('/auth/refresh', null, { _retry: true, _skipAuthRefresh: true, _silent: true })
}

// 登出
export function logout() {
  return request.post('/auth/logout', null, { _skipAuthRefresh: true })
}

// 忘记密码
export function forgotPassword(data) {
  return request.post('/auth/forgot-password', data)
}

// 重置密码
export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}

// 获取当前用户信息
export function getUserInfo() {
  return request.get('/users/me')
}

// 更新个人信息
export function updateProfile(data) {
  return request.put('/users/me', data)
}

// 上传头像
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/users/me/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
