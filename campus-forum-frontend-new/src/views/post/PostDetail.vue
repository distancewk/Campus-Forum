<template>
  <div class="post-detail-page" v-loading="loading">
    <template v-if="post">
      <article class="post-article">
      <header class="post-header">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <el-avatar :size="40" :src="post.author?.avatar" />
          <div class="meta-info">
            <span class="author-name">{{ post.author?.nickname }}</span>
            <span class="post-time">发布于 {{ formatTime(post.createdAt) }}</span>
          </div>
          <el-tag v-if="post.board" size="small">{{ post.board.name }}</el-tag>
        </div>
      </header>

      <div class="post-content" v-html="sanitizedContent"></div>

      <div class="post-actions">
        <el-button
          :type="post.isLiked ? 'primary' : 'default'"
          @click="handleLike"
        >
          <el-icon><Star /></el-icon>
          点赞 {{ post.likeCount }}
        </el-button>
        <el-button
          :type="post.isFavorited ? 'warning' : 'default'"
          @click="handleFavorite"
        >
          <el-icon><Collection /></el-icon>
          收藏 {{ post.favCount }}
        </el-button>
        <span class="view-count">
          <el-icon><View /></el-icon>
          {{ post.viewCount }} 浏览
        </span>
      </div>
      </article>

      <div class="comment-section">
        <h3>评论 ({{ post.commentCount }})</h3>
        <CommentList :post-id="post.id" @changed="handleCommentCountChanged" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPostDetail } from '@/api/post'
import { toggleLike, toggleFavorite } from '@/api/interaction'
import CommentList from '@/components/CommentList.vue'
import { Star, Collection, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const post = ref(null)
const loading = ref(false)

const sanitizedContent = computed(() => {
  if (!post.value?.content) return ''
  return DOMPurify.sanitize(post.value.content)
})

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

const fetchPost = async () => {
  loading.value = true
  try {
    const res = await getPostDetail(route.params.id)
    post.value = res.data
  } catch (error) {
    console.error('获取帖子详情失败:', error)
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleLike({ targetType: 'POST', targetId: post.value.id })
    post.value.isLiked = res.data.active
    post.value.likeCount = res.data.count
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

const handleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleFavorite({ postId: post.value.id })
    post.value.isFavorited = res.data.active
    post.value.favCount = res.data.count
    ElMessage.success(post.value.isFavorited ? '已收藏' : '已取消收藏')
  } catch (error) {
    console.error('收藏失败:', error)
  }
}

const handleCommentCountChanged = (delta) => {
  if (!post.value) return
  post.value.commentCount = Math.max(0, (post.value.commentCount || 0) + delta)
}

onMounted(() => {
  fetchPost()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.post-detail-page {
  max-width: 900px;
  margin: 0 auto;
}

.post-article,
.comment-section {
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  background-color: #fff;
  box-shadow: $shadow-xs;
}

.post-article {
  overflow: hidden;
  margin-bottom: 18px;
}

.post-header {
  padding: 28px 30px 20px;
  border-bottom: 1px solid $border-lighter;
}

.post-title {
  font-size: 30px;
  line-height: 1.32;
  font-weight: 800;
  margin: 0 0 18px;
  color: $text-primary;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-info {
  display: flex;
  flex-direction: column;

  .author-name {
    color: $text-primary;
    font-weight: 500;
  }

  .post-time {
    font-size: 12px;
    color: $text-secondary;
  }
}

.post-content {
  padding: 28px 30px;
  line-height: 1.8;
  color: $text-regular;

  :deep(p) {
    margin: 0 0 16px;
  }

  :deep(h2),
  :deep(h3) {
    margin: 24px 0 12px;
    color: $text-primary;
    line-height: 1.35;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
  }
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 30px;
  border-top: 1px solid $border-lighter;
  background-color: $bg-soft;

  .view-count {
    margin-left: auto;
    color: $text-secondary;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.comment-section {
  padding: 24px 30px;

  h3 {
    margin: 0 0 16px;
    font-size: 18px;
    color: $text-primary;
  }
}

@include mobile {
  .post-header,
  .post-content,
  .post-actions,
  .comment-section {
    padding-left: 16px;
    padding-right: 16px;
  }

  .post-title {
    font-size: 22px;
  }

  .post-actions {
    align-items: flex-start;
    flex-direction: column;

    .view-count {
      margin-left: 0;
    }
  }
}
</style>
