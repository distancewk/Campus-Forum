<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <el-icon><Setting /></el-icon>
        <span>管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/content">
          <el-icon><Document /></el-icon>
          <span>内容审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/ai-knowledge">
          <el-icon><Files /></el-icon>
          <span>AI 知识库</span>
        </el-menu-item>
      </el-menu>
      <div class="admin-footer">
        <el-button @click="$router.push('/')">
          <el-icon><Back /></el-icon>
          返回前台
        </el-button>
      </div>
    </aside>
    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">管理后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="admin-header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="admin-user">
              <el-avatar :size="32" :src="userStore.user?.avatar" />
              <span>{{ userStore.user?.nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <div class="admin-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Setting, DataAnalysis, User, Document, Back, Files } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const currentPageTitle = computed(() => {
  const titles = {
    '/admin': '数据概览',
    '/admin/users': '用户管理',
    '/admin/content': '内容审核',
    '/admin/ai-knowledge': 'AI 知识库'
  }
  return titles[route.path] || ''
})

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.admin-layout {
  display: flex;
  min-height: 100vh;
}

.admin-sidebar {
  width: 240px;
  background-color: #304156;
  display: flex;
  flex-direction: column;

  @include mobile {
    width: 64px;
  }
}

.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);

  @include mobile {
    span {
      display: none;
    }
  }
}

.admin-menu {
  flex: 1;
  border-right: none;
  background-color: transparent;

  .el-menu-item {
    color: #bfcbd9;

    &:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }

    &.is-active {
      color: #409eff;
      background-color: rgba(64, 158, 255, 0.1);
    }
  }
}

.admin-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  .el-button {
    width: 100%;
  }
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: $bg-page;
}

.admin-header {
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid $border-lighter;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.admin-content {
  flex: 1;
  padding: 20px;
}
</style>
