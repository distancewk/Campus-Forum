<template>
  <div class="comment-list">
    <!-- 发表评论 -->
    <div class="comment-input" v-if="userStore.isLoggedIn">
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="3"
        placeholder="写下你的评论..."
        maxlength="500"
        show-word-limit
      />
      <el-button
        type="primary"
        @click="handleSubmitComment"
        :loading="submitting"
        :disabled="!commentContent.trim()"
        class="submit-btn"
      >
        发表评论
      </el-button>
    </div>
    <div class="login-tip" v-else>
      <router-link to="/login">登录</router-link>后参与讨论
    </div>

    <!-- 评论列表 -->
    <div class="comments" v-loading="loading">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <el-avatar :size="32" :src="comment.author?.avatar" />
          <div class="comment-info">
            <span class="comment-author">{{ comment.author?.nickname }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <el-button
            v-if="canDelete(comment)"
            type="danger"
            text
            size="small"
            @click="handleDeleteComment(comment.id)"
          >
            删除
          </el-button>
        </div>
        <div class="comment-content">{{ comment.content }}</div>
        <div class="comment-actions">
          <el-button text size="small" @click="handleReply(comment)">
            <el-icon><ChatRound /></el-icon>
            回复
          </el-button>
          <el-button text size="small" :type="comment.isLiked ? 'primary' : 'default'" @click="handleLikeComment(comment)">
            <el-icon><Star /></el-icon>
            {{ comment.likeCount || 0 }}
          </el-button>
        </div>

        <!-- 回复列表 -->
        <div v-if="comment.replies && comment.replies.length > 0" class="replies">
          <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
            <div class="reply-header">
              <el-avatar :size="24" :src="reply.author?.avatar" />
              <span class="reply-author">{{ reply.author?.nickname }}</span>
              <span v-if="reply.replyToUser" class="reply-to">
                回复 <span class="reply-to-name">{{ reply.replyToUser.nickname }}</span>
              </span>
              <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
            </div>
            <div class="reply-content">{{ reply.content }}</div>
          </div>
        </div>

        <!-- 回复输入框 -->
        <div v-if="replyTo === comment.id" class="reply-input">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="2"
            :placeholder="`回复 ${comment.author?.nickname}...`"
            maxlength="500"
            show-word-limit
          />
          <div class="reply-actions">
            <el-button size="small" @click="replyTo = null">取消</el-button>
            <el-button
              type="primary"
              size="small"
              @click="handleSubmitReply(comment.id, comment.author?.id)"
              :loading="submitting"
              :disabled="!replyContent.trim()"
            >
              回复
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && comments.length === 0" description="暂无评论，快来抢沙发吧~" />
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more">
      <el-button @click="loadMore" :loading="loadingMore">加载更多评论</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getComments, createComment, deleteComment } from '@/api/post'
import { toggleLike } from '@/api/interaction'
import { ChatRound, Star } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'


const props = defineProps({
  postId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['changed'])

const userStore = useUserStore()

const comments = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const commentContent = ref('')
const replyContent = ref('')
const replyTo = ref(null)
const currentPage = ref(1)
const hasMore = ref(true)

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

const canDelete = (comment) => {
  return userStore.user && (userStore.user.id === comment.author?.id || userStore.isAdmin)
}

const fetchComments = async (page = 1) => {
  if (page === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const res = await getComments(props.postId, { page, size: 20 })
    const records = res.data?.records || []

    if (page === 1) {
      comments.value = records
    } else {
      comments.value.push(...records)
    }

    hasMore.value = comments.value.length < (res.data?.total || 0)
    currentPage.value = page
  } catch (error) {
    console.error('获取评论失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  fetchComments(currentPage.value + 1)
}

const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) return

  submitting.value = true
  try {
    const res = await createComment(props.postId, {
      content: commentContent.value
    })
    if (res.data?.pendingReview) {
      ElMessage.success('评论已提交，等待管理员审核')
      commentContent.value = ''
      return
    }
    ElMessage.success('评论成功')
    commentContent.value = ''
    emit('changed', 1)
    fetchComments(1)
  } catch (error) {
    console.error('评论失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleReply = (comment) => {
  replyTo.value = comment.id
  replyContent.value = ''
}

const handleSubmitReply = async (parentId, replyToUserId) => {
  if (!replyContent.value.trim()) return

  submitting.value = true
  try {
    const res = await createComment(props.postId, {
      content: replyContent.value,
      parentId,
      replyToUserId
    })
    if (res.data?.pendingReview) {
      ElMessage.success('回复已提交，等待管理员审核')
      replyContent.value = ''
      replyTo.value = null
      return
    }
    ElMessage.success('回复成功')
    replyContent.value = ''
    replyTo.value = null
    emit('changed', 1)
    fetchComments(1)
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    submitting.value = false
  }
}

const handleDeleteComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteComment(props.postId, commentId)
    ElMessage.success('删除成功')
    emit('changed', -1)
    fetchComments(1)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleLikeComment = async (comment) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleLike({ targetType: 'COMMENT', targetId: comment.id })
    comment.isLiked = res.data.active
    comment.likeCount = res.data.count
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

onMounted(() => {
  fetchComments()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.comment-list {
  margin-top: 0;
}

.comment-input {
  margin-bottom: 24px;

  .submit-btn {
    margin-top: 12px;
    border-radius: 999px;
  }
}

.login-tip {
  text-align: center;
  padding: 20px;
  color: $text-secondary;
  background-color: $bg-soft;
  border: 1px solid $border-lighter;
  border-radius: $radius-md;
  margin-bottom: 24px;

  a {
    color: $primary-color;
  }
}

.comment-item {
  padding: 18px 0;
  border-bottom: 1px solid $border-lighter;

  &:last-child {
    border-bottom: none;
  }
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-info {
  flex: 1;

  .comment-author {
    color: $text-primary;
    font-weight: 500;
    margin-right: 8px;
  }

  .comment-time {
    font-size: 12px;
    color: $text-secondary;
  }
}

.comment-content {
  margin-left: 44px;
  line-height: 1.6;
  color: $text-regular;
}

.comment-actions {
  margin-left: 44px;
  margin-top: 8px;
}

.replies {
  margin-left: 44px;
  margin-top: 12px;
  padding: 12px;
  background-color: $bg-soft;
  border: 1px solid $border-lighter;
  border-radius: $radius-md;
}

.reply-item {
  padding: 8px 0;

  &:not(:last-child) {
    border-bottom: 1px solid $border-lighter;
  }
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;

  .reply-author {
    font-weight: 500;
  }

  .reply-to {
    color: $text-secondary;

    .reply-to-name {
      color: $primary-color;
    }
  }

  .reply-time {
    margin-left: auto;
    font-size: 12px;
    color: $text-secondary;
  }
}

.reply-content {
  margin-left: 32px;
  font-size: 13px;
  line-height: 1.5;
}

.reply-input {
  margin-left: 44px;
  margin-top: 12px;

  .reply-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 8px;
  }
}

.load-more {
  text-align: center;
  margin-top: 20px;
}

@include mobile {
  .comment-content,
  .comment-actions,
  .replies,
  .reply-input {
    margin-left: 0;
  }
}
</style>
