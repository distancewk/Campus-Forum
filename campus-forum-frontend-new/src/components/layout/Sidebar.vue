<template>
  <div class="sidebar">
    <div class="sidebar-header">
      <span class="section-label">社区分区</span>
      <h3>板块导航</h3>
    </div>
    <el-menu
      :default-active="activeBoard"
      @select="handleSelect"
      class="sidebar-menu"
    >
      <el-menu-item index="all">
        <span class="nav-icon"><el-icon><HomeFilled /></el-icon></span>
        <span>全部帖子</span>
      </el-menu-item>
      <el-menu-item
        v-for="board in boards"
        :key="board.id"
        :index="String(board.id)"
      >
        <span class="nav-icon">
          <el-icon><component :is="getBoardIcon(board)" /></el-icon>
        </span>
        <span>{{ board.name }}</span>
        <el-badge :value="board.postCount" :max="999" class="board-badge" />
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ChatDotRound,
  CoffeeCup,
  Document,
  HomeFilled,
  Location,
  Notification,
  Reading,
  Search,
  ShoppingCart
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

// 临时使用本地状态，后续可改为 store
const boards = ref([])
const activeBoard = computed(() => {
  if (route.path === '/') return 'all'
  const match = route.path.match(/\/board\/(\d+)/)
  return match ? match[1] : 'all'
})

const handleSelect = (index) => {
  if (index === 'all') {
    router.push('/')
  } else {
    router.push(`/board/${index}`)
  }
}

const iconMap = {
  校园公告: Notification,
  二手交易: ShoppingCart,
  失物招领: Search,
  表白墙: ChatDotRound,
  学习交流: Reading,
  灌水区: CoffeeCup,
  default: Document
}

const getBoardIcon = (board) => {
  if (!board) return Document
  if (board.name?.includes('失物')) return Location
  return iconMap[board.name] || iconMap.default
}

// 获取板块列表
onMounted(async () => {
  try {
    const { getBoardList } = await import('@/api/board')
    const res = await getBoardList()
    boards.value = res.data || []
  } catch (error) {
    console.error('获取板块列表失败:', error)
  }
})

</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.sidebar {
  padding: 18px 12px 24px;
}

.sidebar-header {
  padding: 0 10px 14px;

  .section-label {
    display: block;
    margin-bottom: 4px;
    font-size: 12px;
    font-weight: 700;
    color: $accent-color;
  }

  h3 {
    margin: 0;
    font-size: 18px;
    line-height: 1.25;
    color: $text-primary;
  }
}

.sidebar-menu {
  border-right: none;
  background-color: transparent;

  .el-menu-item {
    height: 44px;
    margin: 4px 0;
    padding: 0 10px !important;
    line-height: 44px;
    border-radius: $radius-md;
    color: $text-regular;
    font-weight: 600;
    transition: background-color $transition-fast, color $transition-fast;

    &:hover {
      background-color: $bg-soft;
      color: $text-primary;
    }

    &.is-active {
      background-color: $primary-light;
      color: $primary-color;

      .nav-icon {
        color: #fff;
        background-color: $primary-color;
      }
    }
  }

  .nav-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    margin-right: 10px;
    border-radius: $radius-sm;
    color: $text-secondary;
    background-color: $bg-muted;
    transition: background-color $transition-fast, color $transition-fast;
  }

  .board-badge {
    margin-left: auto;

    :deep(.el-badge__content) {
      font-size: 11px;
      border: none;
      background-color: $bg-muted;
      color: $text-secondary;
    }
  }
}
</style>
