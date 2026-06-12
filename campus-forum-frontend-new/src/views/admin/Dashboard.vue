<template>
  <div class="dashboard-page" v-loading="loading">
    <div class="stat-cards">
      <el-card class="stat-card">
        <div class="stat-icon" style="background-color: #409eff">
          <el-icon :size="32"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboard.totalUsers || 0 }}</div>
          <div class="stat-label">总用户数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon" style="background-color: #67c23a">
          <el-icon :size="32"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboard.totalPosts || 0 }}</div>
          <div class="stat-label">总帖子数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon" style="background-color: #e6a23c">
          <el-icon :size="32"><ChatDotRound /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboard.totalComments || 0 }}</div>
          <div class="stat-label">总评论数</div>
        </div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-icon" style="background-color: #f56c6c">
          <el-icon :size="32"><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboard.activeUsers || 0 }}</div>
          <div class="stat-label">7日活跃用户</div>
        </div>
      </el-card>
    </div>

    <div class="detail-cards">
      <el-card class="detail-card">
        <template #header>
          <span>今日数据</span>
        </template>
        <div class="today-stats">
          <div class="today-item">
            <span class="label">新增用户</span>
            <span class="value">{{ dashboard.todayNewUsers || 0 }}</span>
          </div>
          <div class="today-item">
            <span class="label">新增帖子</span>
            <span class="value">{{ dashboard.todayNewPosts || 0 }}</span>
          </div>
        </div>
      </el-card>

      <el-card class="detail-card">
        <template #header>
          <span>板块统计</span>
        </template>
        <div class="board-stats">
          <div
            v-for="stat in dashboard.boardStats"
            :key="stat.boardName"
            class="board-item"
          >
            <span class="board-name">{{ stat.boardName }}</span>
            <el-progress
              :percentage="getPercentage(stat.postCount)"
              :stroke-width="20"
              :text-inside="true"
            />
            <span class="board-count">{{ stat.postCount }} 帖</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboard } from '@/api/admin'
import { User, Document, ChatDotRound, TrendCharts } from '@element-plus/icons-vue'

const dashboard = ref({})
const loading = ref(false)

const totalPosts = computed(() => {
  return dashboard.value.boardStats?.reduce((sum, stat) => sum + stat.postCount, 0) || 1
})

const getPercentage = (count) => {
  return Math.round((count / totalPosts.value) * 100)
}

const fetchDashboard = async () => {
  loading.value = true
  try {
    const res = await getDashboard()
    dashboard.value = res.data || {}
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;

  @include tablet {
    grid-template-columns: repeat(2, 1fr);
  }

  @include mobile {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
  }
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-info {
  .stat-value {
    font-size: 28px;
    font-weight: bold;
    color: $text-primary;
  }

  .stat-label {
    font-size: 14px;
    color: $text-secondary;
  }
}

.detail-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;

  @include mobile {
    grid-template-columns: 1fr;
  }
}

.today-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.today-item {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .label {
    color: $text-secondary;
  }

  .value {
    font-size: 24px;
    font-weight: bold;
    color: $primary-color;
  }
}

.board-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.board-item {
  display: flex;
  align-items: center;
  gap: 12px;

  .board-name {
    width: 80px;
    flex-shrink: 0;
  }

  .el-progress {
    flex: 1;
  }

  .board-count {
    width: 60px;
    text-align: right;
    color: $text-secondary;
  }
}
</style>
