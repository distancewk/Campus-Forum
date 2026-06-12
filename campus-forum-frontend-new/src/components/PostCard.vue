<template>
  <article
    class="post-card"
    role="button"
    tabindex="0"
    @click="$router.push(`/post/${post.id}`)"
    @keyup.enter="$router.push(`/post/${post.id}`)"
  >
    <div class="post-header">
      <div class="post-author">
        <el-avatar :size="36" :src="post.authorAvatar" />
        <div class="author-copy">
          <span class="author-name">{{ post.authorNickname || '校园用户' }}</span>
          <span class="post-time">{{ formatTime(post.createdAt) }}</span>
        </div>
      </div>
      <div class="post-meta">
        <span v-if="post.boardName" class="board-pill">{{ post.boardName }}</span>
        <span v-if="post.isPinned" class="state-pill state-pin">置顶</span>
        <span v-if="post.isFeatured" class="state-pill state-featured">精华</span>
      </div>
    </div>

    <div class="post-body">
      <h3 class="post-title">
        {{ post.title }}
      </h3>
      <p class="post-summary" v-if="post.summary">{{ post.summary }}</p>
    </div>

    <div class="post-footer">
      <div class="stats">
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ post.viewCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ post.likeCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><ChatDotRound /></el-icon>
          {{ post.commentCount || 0 }}
        </span>
      </div>
      <span class="read-more">
        查看
        <el-icon><ArrowRight /></el-icon>
      </span>
    </div>
  </article>
</template>

<script setup>
import { ArrowRight, ChatDotRound, Star, View } from '@element-plus/icons-vue'

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString('zh-CN')
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.post-card {
  background-color: #fff;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: 18px 20px;
  margin-bottom: 14px;
  cursor: pointer;
  box-shadow: $shadow-xs;
  outline: none;
  transition: border-color $transition-base, box-shadow $transition-base, transform $transition-base;

  &:hover,
  &:focus-visible {
    border-color: rgba(37, 99, 235, 0.24);
    box-shadow: $shadow-sm;
    transform: translateY(-2px);
  }
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.post-author {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;

  .author-copy {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 1px;
  }

  .author-name {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }

  .post-time {
    font-size: 12px;
    color: $text-tertiary;
  }
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;

  .board-pill,
  .state-pill {
    display: inline-flex;
    align-items: center;
    height: 24px;
    padding: 0 8px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
  }

  .board-pill {
    color: $primary-color;
    background-color: $primary-lighter;
  }

  .state-pin {
    color: $danger-color;
    background-color: #fff1f2;
  }

  .state-featured {
    color: $warning-color;
    background-color: #fff7ed;
  }
}

.post-body {
  margin-bottom: 14px;
}

.post-title {
  margin: 0 0 9px;
  font-size: 18px;
  line-height: 1.45;
  font-weight: 750;
  color: $text-primary;
}

.post-summary {
  margin: 0;
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid $border-lighter;

  .stats {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
  }

  .stat-item {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 13px;
    color: $text-secondary;
  }

  .read-more {
    display: inline-flex;
    align-items: center;
    gap: 2px;
    font-size: 13px;
    font-weight: 700;
    color: $primary-color;
  }
}

@include mobile {
  .post-card {
    padding: 16px;
  }

  .post-header {
    flex-direction: column;
    gap: 10px;
  }

  .post-meta {
    justify-content: flex-start;
  }

  .post-title {
    font-size: 16px;
  }
}
</style>
