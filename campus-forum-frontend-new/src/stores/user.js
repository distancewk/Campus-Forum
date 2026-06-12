import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, refreshToken as refreshApi, getUserInfo, logout as logoutApi } from '@/api/auth'
import { getToken, setToken, removeToken, getUser, setUser, removeUser } from '@/utils/auth'
import { connectWebSocket, disconnectWebSocket } from '@/utils/websocket'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(getToken() || '')
  const user = ref(getUser())

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function login(studentNo, password) {
    const res = await loginApi({ studentNo, password })
    accessToken.value = res.data.accessToken
    user.value = res.data.user
    setToken(accessToken.value)
    setUser(user.value)
    // 连接 WebSocket
    connectWebSocket(accessToken.value)
  }

  async function refreshAccessToken() {
    const res = await refreshApi()
    accessToken.value = res.data.accessToken
    setToken(accessToken.value)
    connectWebSocket(accessToken.value)
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    user.value = res.data
    setUser(user.value)
  }

  async function ensureSession() {
    if (accessToken.value) {
      if (!user.value) {
        await fetchUserInfo()
      }
      return true
    }

    try {
      await refreshAccessToken()
      await fetchUserInfo()
      return true
    } catch (error) {
      clearSession()
      return false
    }
  }

  function clearSession() {
    accessToken.value = ''
    user.value = null
    removeToken()
    removeUser()
    disconnectWebSocket()
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearSession()
    }
  }

  // 初始化时如果已登录则连接 WebSocket
  if (isLoggedIn.value) {
    connectWebSocket(accessToken.value)
  }

  return {
    accessToken,
    user,
    isLoggedIn,
    isAdmin,
    login,
    ensureSession,
    refreshAccessToken,
    fetchUserInfo,
    logout,
    clearSession
  }
})
