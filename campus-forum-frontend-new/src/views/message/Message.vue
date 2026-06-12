<template>
  <div class="message-page">
    <div class="message-container">
      <!-- 会话列表 -->
      <div class="conversation-list">
        <div class="list-header">
          <h3>私信</h3>
        </div>
        <div class="list-content" v-loading="loadingConversations">
          <div
            v-for="conv in conversations"
            :key="conv.userId"
            class="conversation-item"
            :class="{ active: selectedUserId === conv.userId }"
            @click="selectConversation(conv.userId)"
          >
            <el-avatar :size="40" :src="conv.avatar" />
            <div class="conv-info">
              <div class="conv-header">
                <span class="conv-name">{{ conv.nickname }}</span>
                <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
              </div>
              <div class="conv-preview">
                <span class="conv-message">{{ conv.lastMessage }}</span>
                <el-badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" />
              </div>
            </div>
          </div>
          <el-empty v-if="!loadingConversations && conversations.length === 0" description="暂无会话" />
        </div>
      </div>

      <!-- 聊天区域 -->
      <div class="chat-area">
        <template v-if="selectedUserId">
          <div class="chat-header">
            <h3>{{ selectedUserName }}</h3>
          </div>
          <div class="chat-messages" ref="messagesContainer" v-loading="loadingMessages">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ 'message-self': msg.senderId === currentUserId }"
            >
              <el-avatar :size="36" :src="msg.senderAvatar" />
              <div class="message-content">
                <div class="message-text">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <el-input
              v-model="messageInput"
              placeholder="输入消息..."
              @keyup.enter="handleSend"
              :disabled="!isConnected"
            >
              <template #append>
                <el-button type="primary" @click="handleSend" :disabled="!messageInput.trim()">
                  发送
                </el-button>
              </template>
            </el-input>
          </div>
        </template>
        <div v-else class="no-chat">
          <el-icon :size="64" color="#c0c4cc"><ChatDotRound /></el-icon>
          <p>选择一个会话开始聊天</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { getConversations, getChatHistory, markAsRead } from '@/api/message'
import { sendMessage, isConnected } from '@/utils/websocket'
import { ChatDotRound } from '@element-plus/icons-vue'

const userStore = useUserStore()
const messageStore = useMessageStore()

const conversations = ref([])
const messages = ref([])
const selectedUserId = ref(null)
const selectedUserName = ref('')
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const messageInput = ref('')
const messagesContainer = ref(null)
const currentPage = ref(1)
const currentUserId = computed(() => userStore.user?.id)

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN')
}

const fetchConversations = async () => {
  loadingConversations.value = true
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch (error) {
    console.error('获取会话列表失败:', error)
  } finally {
    loadingConversations.value = false
  }
}

const selectConversation = async (userId) => {
  selectedUserId.value = userId
  const conv = conversations.value.find(c => c.userId === userId)
  selectedUserName.value = conv?.nickname || ''
  currentPage.value = 1
  await fetchMessages()
  // 标记已读
  await markAsRead(userId)
  messageStore.fetchUnreadCount()
  // 更新会话列表中的未读数
  if (conv) {
    conv.unreadCount = 0
  }
}

const fetchMessages = async () => {
  if (!selectedUserId.value) return
  loadingMessages.value = true
  try {
    const res = await getChatHistory(selectedUserId.value, {
      page: currentPage.value,
      size: 50
    })
    messages.value = res.data?.records || []
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('获取聊天记录失败:', error)
  } finally {
    loadingMessages.value = false
  }
}

const handleSend = () => {
  if (!messageInput.value.trim() || !isConnected()) return

  const success = sendMessage(selectedUserId.value, messageInput.value)
  if (success) {
    messageInput.value = ''
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 监听新消息
const handleNewMessage = (message) => {
  if (message.senderId === selectedUserId.value || message.receiverId === selectedUserId.value) {
    messages.value.push(message)
    nextTick(() => scrollToBottom())
  }
  // 更新会话列表
  fetchConversations()
}

onMounted(() => {
  fetchConversations()
  // 注册消息回调
  window.__messageCallback = handleNewMessage
})

// 清理
onUnmounted(() => {
  window.__messageCallback = null
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.message-page {
  height: calc(100vh - 100px);
}

.message-container {
  display: flex;
  height: 100%;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.conversation-list {
  width: 300px;
  border-right: 1px solid $border-lighter;
  display: flex;
  flex-direction: column;

  @include mobile {
    width: 100%;
  }
}

.list-header {
  padding: 16px;
  border-bottom: 1px solid $border-lighter;

  h3 {
    margin: 0;
  }
}

.list-content {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: $bg-page;
  }

  &.active {
    background-color: $primary-light;
  }
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.conv-name {
  font-weight: 500;
}

.conv-time {
  font-size: 12px;
  color: $text-secondary;
}

.conv-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conv-message {
  font-size: 13px;
  color: $text-secondary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;

  @include mobile {
    display: none;
  }
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid $border-lighter;

  h3 {
    margin: 0;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;

  &.message-self {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-text {
      background-color: $primary-lighter;
    }
  }
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message-text {
  padding: 8px 12px;
  background-color: $bg-page;
  border-radius: 8px;
  max-width: 300px;
  word-break: break-word;
}

.message-time {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 4px;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid $border-lighter;
}

.no-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $text-secondary;

  p {
    margin-top: 16px;
  }
}
</style>
