<template>
  <header class="navbar">
    <div class="navbar-left">
      <router-link to="/" class="logo">
        <el-icon><ChatDotRound /></el-icon>
        <span>校园论坛</span>
      </router-link>
    </div>

    <div class="navbar-center">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索帖子、板块或同学..."
        clearable
        aria-label="搜索帖子"
        @keyup.enter="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="navbar-right">
      <template v-if="userStore.isLoggedIn">
        <el-button class="ai-link" @click="$router.push('/ai')">
          <el-icon><ChatDotRound /></el-icon>
          <span class="btn-text">AI 问答</span>
        </el-button>

        <el-button type="primary" class="post-button" @click="$router.push('/post/create')">
          <el-icon><EditPen /></el-icon>
          <span class="btn-text">发帖</span>
        </el-button>

        <el-badge :value="messageStore.unreadCount" :hidden="messageStore.unreadCount === 0" class="message-badge">
          <el-button :icon="Bell" circle title="消息" aria-label="消息" @click="$router.push('/messages')" />
        </el-badge>

        <el-dropdown trigger="click" @command="handleCommand">
          <div class="user-info">
            <el-avatar :size="32" :src="userStore.user?.avatar || defaultAvatar" />
            <span class="username">{{ userStore.user?.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item v-if="userStore.isAdmin" command="admin">
                <el-icon><Setting /></el-icon>
                管理后台
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>

      <template v-else>
        <el-button @click="$router.push('/login')">登录</el-button>
        <el-button type="primary" @click="$router.push('/register')">注册</el-button>
      </template>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import {
  ChatDotRound, Search, EditPen, Bell, ArrowDown,
  User, Setting, SwitchButton
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

const searchKeyword = ref('')
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value.trim() } })
  }
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'admin':
      router.push('/admin')
      break
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        await userStore.logout()
        router.push('/login')
      }).catch(() => {})
      break
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: $navbar-height;
  background-color: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid $border-light;
  backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 28px;
  z-index: 1000;
  box-shadow: $shadow-xs;
}

.navbar-left {
  flex: 0 0 $sidebar-width - 28px;

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 19px;
    font-weight: 800;
    letter-spacing: 0;
    color: $text-primary;
    text-decoration: none;

    .el-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: $radius-md;
      font-size: 20px;
      color: #fff;
      background: linear-gradient(135deg, $primary-color, $accent-color);
      box-shadow: 0 10px 22px rgba(37, 99, 235, 0.2);
    }
  }
}

.navbar-center {
  flex: 1;
  display: flex;
  justify-content: flex-start;
  min-width: 180px;

  .search-input {
    max-width: 520px;
    width: 100%;

    :deep(.el-input__wrapper) {
      height: 42px;
      padding: 0 14px;
      border-radius: 999px;
      background-color: $bg-soft;
    }
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 10px;

  .post-button {
    height: 40px;
    padding: 0 16px;
    border-radius: 999px;
    box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
  }

  .ai-link {
    height: 40px;
    padding: 0 14px;
    border-radius: 999px;
    color: $primary-color;
    background-color: $primary-lighter;
    border-color: $primary-light;

    &:hover {
      color: #fff;
      background-color: $primary-color;
      border-color: $primary-color;
    }
  }

  .btn-text {
    @include mobile {
      display: none;
    }
  }

  .message-badge {
    line-height: 1;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    min-height: 42px;
    padding: 5px 8px 5px 5px;
    border: 1px solid transparent;
    border-radius: 999px;
    transition: background-color $transition-fast, border-color $transition-fast;

    &:hover {
      background-color: $bg-soft;
      border-color: $border-light;
    }

    .username {
      font-size: 14px;
      color: $text-primary;

      @include mobile {
        display: none;
      }
    }
  }
}

@include mobile {
  .navbar {
    height: 60px;
    gap: 10px;
    padding: 0 12px;
  }

  .navbar-left {
    flex: 0 0 auto;

    .logo {
      span {
        display: none;
      }
    }
  }

  .navbar-center {
    min-width: 0;

    .search-input {
      :deep(.el-input__wrapper) {
        height: 38px;
      }
    }
  }

  .navbar-right {
    gap: 6px;

    :deep(.el-button) {
      padding: 8px 10px;
    }
  }
}
</style>
