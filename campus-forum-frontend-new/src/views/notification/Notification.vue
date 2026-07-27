<template>
  <div class="notification-page">
    <div class="page-header">
      <h1>通知</h1>
      <el-button
        v-if="list.length"
        size="small"
        :disabled="unreadCount === 0"
        @click="handleMarkAll"
      >
        全部已读
      </el-button>
    </div>

    <div v-if="loading" class="state-tip">加载中…</div>

    <div v-else-if="list.length === 0" class="state-tip empty">
      <el-icon><Bell /></el-icon>
      <p>暂时没有通知</p>
    </div>

    <ul v-else class="notification-list">
      <li
        v-for="item in list"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleOpen(item)"
      >
        <el-avatar :size="40" :src="item.actorAvatar || defaultAvatar" />
        <div class="notification-body">
          <div class="notification-line">
            <span class="notification-title">{{ item.title }}</span>
            <span class="notification-time">{{ formatTime(item.createdAt) }}</span>
          </div>
          <p class="notification-content">{{ item.content }}</p>
        </div>
        <span v-if="!item.isRead" class="unread-dot" />
      </li>
    </ul>

    <div v-if="total > list.length" class="load-more">
      <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { getNotifications, markNotificationRead, markAllRead } from '@/api/notification'
import { formatTime } from '@/utils/format'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'

const router = useRouter()
const notificationStore = useNotificationStore()

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const loadingMore = ref(false)

const unreadCount = computed(() => notificationStore.unreadCount)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

async function load(reset = true) {
  if (reset) {
    loading.value = true
    page.value = 1
  } else {
    loadingMore.value = true
  }
  try {
    const res = await getNotifications({ page: page.value, size })
    const data = res.data || { records: [], total: 0 }
    if (reset) {
      list.value = data.records || []
    } else {
      list.value.push(...(data.records || []))
    }
    total.value = data.total || 0
  } catch (e) {
    console.error('加载通知失败:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  page.value++
  await load(false)
}

async function handleOpen(item) {
  if (!item.isRead) {
    try {
      await markNotificationRead(item.id)
      item.isRead = true
      notificationStore.clearUnreadOne()
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }
  if (item.targetUrl) {
    router.push(item.targetUrl)
  }
}

async function handleMarkAll() {
  try {
    await markAllRead()
    list.value.forEach((it) => (it.isRead = true))
    notificationStore.clearUnread()
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    console.error('全部已读失败:', e)
  }
}

onMounted(() => {
  load()
})
</script>

<style lang="scss" scoped>
.notification-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h1 {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
  }
}

.state-tip {
  text-align: center;
  color: #909399;
  padding: 48px 0;

  &.empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;

    .el-icon {
      font-size: 40px;
    }
  }
}

.notification-list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.15s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background-color: #fafafa;
  }

  &.unread {
    background-color: #f5f8ff;
  }
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.notification-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.notification-time {
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
}

.notification-content {
  margin: 4px 0 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  word-break: break-word;
}

.unread-dot {
  align-self: center;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  flex: 0 0 auto;
}

.load-more {
  text-align: center;
  margin-top: 20px;
}
</style>
