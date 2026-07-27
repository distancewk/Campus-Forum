import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const latest = ref(null)

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data
    } catch (error) {
      console.error('获取未读通知数失败:', error)
    }
  }

  function pushNotification(n) {
    latest.value = n
    if (!n.isRead) {
      unreadCount.value++
    }
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  function clearUnreadOne() {
    if (unreadCount.value > 0) {
      unreadCount.value--
    }
  }

  return {
    unreadCount,
    latest,
    fetchUnreadCount,
    pushNotification,
    clearUnread,
    clearUnreadOne
  }
})
