<template>
  <div class="ai-ask-page">
    <section class="ask-panel">
      <div class="ask-heading">
        <div>
          <h1>校园智能问答</h1>
          <p>基于历史帖子、精华评论、校园通知和资料生成带引用的回答。</p>
        </div>
        <el-button class="refresh-button" :icon="Refresh" circle title="刷新历史" @click="loadSessions" />
      </div>

      <el-input
        v-model="question"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="例如：学校附近哪里可以打印论文？"
        @keydown.meta.enter.prevent="submitQuestion"
        @keydown.ctrl.enter.prevent="submitQuestion"
      />

      <div class="examples">
        <button
          v-for="item in examples"
          :key="item"
          type="button"
          @click="question = item"
        >
          {{ item }}
        </button>
      </div>

      <div class="ask-actions">
        <el-button type="primary" :loading="loading" :disabled="!question.trim()" @click="submitQuestion">
          <el-icon><Promotion /></el-icon>
          提问
        </el-button>
      </div>
    </section>

    <div class="answer-layout">
      <section class="answer-panel">
        <div v-if="!answerStatus" class="empty-state">
          <el-icon><MagicStick /></el-icon>
          <h2>等待你的问题</h2>
          <p>回答会优先引用论坛内容和管理员资料。</p>
        </div>

        <template v-else>
          <div class="answer-header">
            <span :class="['status-dot', statusClass]"></span>
            <div>
              <h2>{{ statusTitle }}</h2>
              <p v-if="activeSessionTime">{{ activeSessionTime }}</p>
            </div>
          </div>

          <el-alert
            v-if="answerStatus === 'INSUFFICIENT_SOURCES'"
            type="warning"
            :closable="false"
            title="暂未找到足够可靠的校园资料"
            class="answer-alert"
          />

          <el-alert
            v-else-if="answerStatus === 'FAILED'"
            type="error"
            :closable="false"
            title="AI 服务暂时不可用"
            class="answer-alert"
          />

          <div class="answer-text">{{ answer }}</div>

          <div v-if="citations.length" class="citations">
            <div class="section-title">
              <el-icon><Link /></el-icon>
              <span>引用来源</span>
            </div>
            <button
              v-for="citation in citations"
              :key="`${citation.sourceType}-${citation.sourceId}-${citation.chunkId}`"
              class="citation"
              type="button"
              @click="openCitation(citation)"
            >
              <span class="citation-title">{{ citation.title }}</span>
              <small>{{ sourceLabel(citation.sourceType) }} · 相关度 {{ Math.round((citation.score || 0) * 100) }}%</small>
              <p>{{ citation.snippet }}</p>
            </button>
          </div>
        </template>
      </section>

      <aside class="history-panel" v-loading="historyLoading">
        <div class="section-title">
          <el-icon><Clock /></el-icon>
          <span>最近提问</span>
        </div>

        <div v-if="sessions.length" class="history-list">
          <button
            v-for="session in sessions"
            :key="session.id"
            :class="['history-item', { active: activeSessionId === session.id }]"
            type="button"
            @click="openSession(session.id)"
          >
            <strong>{{ session.question }}</strong>
            <span>{{ formatTime(session.createdAt) }}</span>
          </button>
        </div>
        <div v-else class="history-empty">暂无记录</div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { askAi, getAiSession, getAiSessions } from '@/api/ai'
import { Clock, Link, MagicStick, Promotion, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const question = ref('')
const loading = ref(false)
const historyLoading = ref(false)
const answer = ref('')
const answerStatus = ref('')
const citations = ref([])
const sessions = ref([])
const activeSessionId = ref(null)
const activeSessionCreatedAt = ref('')

const examples = [
  '学校附近哪里可以打印论文？',
  '计算机学院研究生选课有什么经验？',
  '宿舍报修后一般多久有人处理？',
  '有没有往年 Java 校招面经？'
]

const statusTitle = computed(() => {
  if (answerStatus.value === 'ANSWERED') return '已生成回答'
  if (answerStatus.value === 'INSUFFICIENT_SOURCES') return '资料不足'
  if (answerStatus.value === 'FAILED') return '生成失败'
  return '回答'
})

const statusClass = computed(() => {
  if (answerStatus.value === 'ANSWERED') return 'success'
  if (answerStatus.value === 'INSUFFICIENT_SOURCES') return 'warning'
  if (answerStatus.value === 'FAILED') return 'danger'
  return ''
})

const activeSessionTime = computed(() => formatTime(activeSessionCreatedAt.value))

const submitQuestion = async () => {
  const text = question.value.trim()
  if (!text) {
    ElMessage.warning('请输入问题')
    return
  }

  loading.value = true
  activeSessionId.value = null
  activeSessionCreatedAt.value = ''
  try {
    const res = await askAi(text)
    applyAnswer(res.data || {})
    await loadSessions()
  } catch (error) {
    console.error('AI 问答失败:', error)
  } finally {
    loading.value = false
  }
}

const loadSessions = async () => {
  historyLoading.value = true
  try {
    const res = await getAiSessions({ page: 1, size: 8 })
    sessions.value = res.data?.records || []
  } catch (error) {
    console.error('获取问答历史失败:', error)
  } finally {
    historyLoading.value = false
  }
}

const openSession = async (id) => {
  historyLoading.value = true
  try {
    const res = await getAiSession(id)
    const session = res.data || {}
    question.value = session.question || ''
    activeSessionId.value = session.id || id
    activeSessionCreatedAt.value = session.createdAt || ''
    applyAnswer(session)
  } catch (error) {
    console.error('获取问答记录失败:', error)
  } finally {
    historyLoading.value = false
  }
}

const applyAnswer = (data) => {
  answerStatus.value = data.answerStatus || ''
  answer.value = data.answer || ''
  citations.value = data.citations || []
}

const openCitation = (citation) => {
  if (citation.sourceType === 'POST') {
    router.push(`/post/${citation.sourceId}`)
    return
  }
  if (citation.sourceType === 'COMMENT') {
    ElMessage.info('精华评论来源暂不支持直接跳转')
    return
  }
  ElMessage.info('资料来源请在管理员知识库中查看')
}

const sourceLabel = (sourceType) => {
  const labels = {
    POST: '历史帖子',
    COMMENT: '精华评论',
    DOCUMENT: '校园资料'
  }
  return labels[sourceType] || sourceType || '来源'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadSessions()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.ai-ask-page {
  max-width: 1080px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.ask-panel,
.answer-panel,
.history-panel {
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  background-color: #fff;
  box-shadow: $shadow-xs;
}

.ask-panel {
  padding: 24px;
}

.ask-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h1 {
    margin: 0;
    font-size: 28px;
    line-height: 1.25;
    color: $text-primary;
  }

  p {
    margin: 8px 0 0;
    color: $text-secondary;
    line-height: 1.6;
  }
}

.refresh-button {
  flex: 0 0 auto;
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;

  button {
    min-height: 32px;
    border: 1px solid $border-light;
    border-radius: 999px;
    background-color: $bg-soft;
    color: $text-regular;
    padding: 6px 12px;
    font-size: 13px;
    cursor: pointer;
    transition: border-color $transition-fast, color $transition-fast, background-color $transition-fast;

    &:hover {
      border-color: $primary-color;
      color: $primary-color;
      background-color: $primary-lighter;
    }
  }
}

.ask-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;

  .el-button {
    min-width: 110px;
    height: 40px;
    border-radius: 999px;
  }
}

.answer-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.answer-panel {
  min-height: 360px;
  padding: 24px;
}

.empty-state {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: $text-secondary;

  .el-icon {
    width: 54px;
    height: 54px;
    margin-bottom: 14px;
    border-radius: $radius-lg;
    color: $primary-color;
    background-color: $primary-lighter;
    font-size: 28px;
  }

  h2 {
    margin: 0;
    font-size: 20px;
    color: $text-primary;
  }

  p {
    margin: 8px 0 0;
  }
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: $text-primary;
  }

  p {
    margin: 4px 0 0;
    color: $text-secondary;
    font-size: 12px;
  }
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background-color: $info-color;

  &.success {
    background-color: $success-color;
  }

  &.warning {
    background-color: $warning-color;
  }

  &.danger {
    background-color: $danger-color;
  }
}

.answer-alert {
  margin-bottom: 14px;
}

.answer-text {
  white-space: pre-wrap;
  color: $text-regular;
  line-height: 1.85;
  font-size: 15px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: $text-primary;
  font-weight: 700;

  .el-icon {
    color: $primary-color;
  }
}

.citations {
  margin-top: 22px;
  display: grid;
  gap: 10px;
}

.citation {
  width: 100%;
  text-align: left;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  background-color: #fff;
  padding: 14px;
  cursor: pointer;
  transition: border-color $transition-fast, box-shadow $transition-fast, transform $transition-fast;

  &:hover {
    border-color: rgba(37, 99, 235, 0.42);
    box-shadow: $shadow-sm;
    transform: translateY(-1px);
  }

  .citation-title {
    display: block;
    color: $text-primary;
    font-weight: 700;
  }

  small {
    display: block;
    margin-top: 5px;
    color: $text-secondary;
  }

  p {
    margin: 8px 0 0;
    color: $text-regular;
    line-height: 1.6;
  }
}

.history-panel {
  padding: 18px;
  position: sticky;
  top: calc($navbar-height + 20px);
}

.history-list {
  display: grid;
  gap: 8px;
}

.history-item {
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  border-radius: $radius-md;
  background-color: $bg-soft;
  padding: 12px;
  cursor: pointer;
  transition: background-color $transition-fast, border-color $transition-fast;

  &:hover,
  &.active {
    background-color: $primary-lighter;
    border-color: $primary-light;
  }

  strong {
    display: -webkit-box;
    overflow: hidden;
    color: $text-primary;
    font-size: 13px;
    line-height: 1.5;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  span {
    display: block;
    margin-top: 6px;
    color: $text-secondary;
    font-size: 12px;
  }
}

.history-empty {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-secondary;
  background-color: $bg-soft;
  border-radius: $radius-md;
}

@include tablet {
  .answer-layout {
    grid-template-columns: 1fr;
  }

  .history-panel {
    position: static;
  }
}

@include mobile {
  .ask-panel,
  .answer-panel,
  .history-panel {
    padding: 16px;
  }

  .ask-heading {
    align-items: center;

    h1 {
      font-size: 22px;
    }

    p {
      font-size: 13px;
    }
  }

  .examples {
    display: grid;
    grid-template-columns: 1fr;
  }

  .ask-actions {
    justify-content: stretch;

    .el-button {
      width: 100%;
    }
  }
}
</style>
