import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data.count
    } catch (error) {
      console.error('获取未读消息数失败:', error)
    }
  }

  function incrementUnread() {
    unreadCount.value++
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  return {
    unreadCount,
    fetchUnreadCount,
    incrementUnread,
    clearUnread
  }
})
