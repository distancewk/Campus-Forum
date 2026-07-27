import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { useMessageStore } from '@/stores/message'
import { useNotificationStore } from '@/stores/notification'

let stompClient = null
let currentToken = ''

export function connectWebSocket(token, onMessage) {
  if (!token) {
    return
  }

  if (stompClient && stompClient.connected && currentToken === token) {
    return
  }

  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  currentToken = token

  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: {
      Authorization: `Bearer ${token}`
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      // 订阅个人消息队列
      stompClient.subscribe('/user/queue/messages', (message) => {
        try {
          const body = JSON.parse(message.body)
          if (onMessage) {
            onMessage(body)
          }
          // 推送到 Pinia store（更新未读消息数并暴露给 Message 页面）
          const messageStore = useMessageStore()
          messageStore.pushMessage(body)
        } catch (e) {
          console.error('解析 WebSocket 消息失败:', e)
        }
      })

      // 订阅个人通知队列（点赞 / 评论 / 回复 / 审核结果）
      stompClient.subscribe('/user/queue/notifications', (message) => {
        try {
          const body = JSON.parse(message.body)
          const notificationStore = useNotificationStore()
          notificationStore.pushNotification(body)
        } catch (e) {
          console.error('解析 WebSocket 通知失败:', e)
        }
      })
    },
    onStompError: (frame) => {
      console.error('WebSocket 错误:', frame.headers['message'])
    }
  })

  stompClient.activate()
}

export function sendMessage(receiverId, content) {
  if (stompClient && stompClient.connected) {
    stompClient.publish({
      destination: '/app/message/send',
      body: JSON.stringify({ receiverId, content })
    })
    return true
  }
  console.warn('WebSocket 未连接')
  return false
}

export function disconnectWebSocket() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  currentToken = ''
}

export function isConnected() {
  return Boolean(stompClient && stompClient.connected)
}
