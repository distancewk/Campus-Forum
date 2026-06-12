<template>
  <div class="home-page">
    <section class="feed-main">
      <div class="page-header">
        <div>
          <h1>{{ activeTitle }}</h1>
          <p>校园里的新鲜讨论、经验分享和即时互助</p>
        </div>
        <el-radio-group v-model="sortType" @change="handleSortChange">
          <el-radio-button value="latest">最新</el-radio-button>
          <el-radio-button value="hot">热门</el-radio-button>
          <el-radio-button value="featured">精华</el-radio-button>
        </el-radio-group>
      </div>

      <div class="post-list" v-loading="loading">
        <PostCard
          v-for="post in posts"
          :key="post.id"
          :post="post"
        />
        <el-empty v-if="!loading && posts.length === 0" description="暂无帖子" />
      </div>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <aside class="insight-rail">
      <div class="insight-panel publish-panel">
        <div class="panel-icon">
          <el-icon><EditPen /></el-icon>
        </div>
        <h2>发布新讨论</h2>
        <p>分享课程经验、活动信息或校园见闻。</p>
        <el-button type="primary" @click="$router.push('/post/create')">去发帖</el-button>
      </div>

      <div class="insight-panel">
        <h2>社区概览</h2>
        <div class="metric-row">
          <span>当前帖子</span>
          <strong>{{ total }}</strong>
        </div>
        <div class="metric-row">
          <span>本页展示</span>
          <strong>{{ posts.length }}</strong>
        </div>
        <div class="metric-row">
          <span>排序方式</span>
          <strong>{{ activeTitle }}</strong>
        </div>
      </div>

      <div class="insight-panel compact-panel">
        <h2>浏览建议</h2>
        <button
          v-for="option in sortOptions"
          :key="option.value"
          class="quick-filter"
          :class="{ active: sortType === option.value }"
          type="button"
          @click="setSort(option.value)"
        >
          <span>{{ option.label }}</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { getPostList } from '@/api/post'
import PostCard from '@/components/PostCard.vue'
import { ArrowRight, EditPen } from '@element-plus/icons-vue'

const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const sortType = ref('latest')
const sortOptions = [
  { label: '最新帖子', value: 'latest' },
  { label: '热门讨论', value: 'hot' },
  { label: '精华内容', value: 'featured' }
]
const activeTitle = computed(() => sortOptions.find((item) => item.value === sortType.value)?.label || '最新帖子')

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await getPostList({
      page: currentPage.value,
      size: pageSize.value,
      sort: sortType.value
    })
    posts.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取帖子列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSortChange = () => {
  currentPage.value = 1
  fetchPosts()
}

const setSort = (value) => {
  if (sortType.value === value) return
  sortType.value = value
  handleSortChange()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchPosts()
}

onMounted(() => {
  fetchPosts()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.home-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  width: 100%;
  max-width: $content-max-width;
  margin: 0 auto;
}

.feed-main {
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 18px;

  h1 {
    margin: 0;
    font-size: 26px;
    line-height: 1.25;
    font-weight: 800;
    color: $text-primary;
  }

  p {
    margin: 6px 0 0;
    color: $text-secondary;
  }

  :deep(.el-radio-group) {
    padding: 4px;
    border: 1px solid $border-light;
    border-radius: 999px;
    background-color: #fff;
    box-shadow: $shadow-xs;
  }

  :deep(.el-radio-button__inner) {
    border: 0;
    border-radius: 999px;
    background: transparent;
    box-shadow: none;
    font-weight: 700;
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background-color: $primary-color;
    box-shadow: none;
  }
}

.post-list {
  min-height: 200px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.insight-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.insight-panel {
  padding: 18px;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  background-color: #fff;
  box-shadow: $shadow-xs;

  h2 {
    margin: 0 0 12px;
    font-size: 16px;
    line-height: 1.35;
    color: $text-primary;
  }

  p {
    margin: 0 0 16px;
    color: $text-secondary;
  }
}

.publish-panel {
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.09), rgba(15, 159, 143, 0.1)),
    #fff;

  .panel-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 38px;
    height: 38px;
    margin-bottom: 14px;
    border-radius: $radius-md;
    color: #fff;
    background: linear-gradient(135deg, $primary-color, $accent-color);
  }
}

.metric-row,
.quick-filter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-row {
  padding: 10px 0;
  border-top: 1px solid $border-lighter;
  color: $text-secondary;

  strong {
    color: $text-primary;
    font-size: 18px;
    font-variant-numeric: tabular-nums;
  }
}

.compact-panel {
  padding: 14px;
}

.quick-filter {
  width: 100%;
  height: 40px;
  padding: 0 10px;
  border: 0;
  border-radius: $radius-md;
  background-color: transparent;
  color: $text-secondary;
  cursor: pointer;
  font-weight: 700;
  text-align: left;
  transition: background-color $transition-fast, color $transition-fast;

  &:hover,
  &.active {
    background-color: $primary-lighter;
    color: $primary-color;
  }
}

@include tablet {
  .home-page {
    grid-template-columns: 1fr;
  }

  .insight-rail {
    display: none;
  }
}

@include mobile {
  .home-page {
    grid-template-columns: 1fr;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;

    h1 {
      font-size: 22px;
    }
  }

  .insight-rail {
    display: none;
  }
}
</style>
