<template>
  <div class="post-list-page">
    <div class="page-header">
      <div>
        <h1>{{ boardName || '板块帖子' }}</h1>
        <p>按板块筛选校园讨论，快速找到相关内容。</p>
      </div>
      <span class="count-pill">{{ total }} 篇帖子</span>
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
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPostList } from '@/api/post'
import { getBoardList } from '@/api/board'
import PostCard from '@/components/PostCard.vue'

const route = useRoute()

const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const boardName = ref('')

const boardId = computed(() => route.params.id ? Number(route.params.id) : null)

const fetchPosts = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (boardId.value) {
      params.boardId = boardId.value
    }
    const res = await getPostList(params)
    posts.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('获取帖子列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchBoardName = async () => {
  if (!boardId.value) {
    boardName.value = '全部帖子'
    return
  }
  try {
    const res = await getBoardList()
    const board = (res.data || []).find((item) => item.id === boardId.value)
    boardName.value = board?.name || '板块帖子'
  } catch (error) {
    boardName.value = '板块帖子'
    console.error('获取板块信息失败:', error)
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchPosts()
}

watch(boardId, () => {
  currentPage.value = 1
  fetchBoardName()
  fetchPosts()
})

onMounted(() => {
  fetchBoardName()
  fetchPosts()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.post-list-page {
  max-width: 840px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
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
}

.count-pill {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid $border-light;
  border-radius: 999px;
  background-color: #fff;
  color: $text-secondary;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.post-list {
  min-height: 200px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

@include mobile {
  .page-header {
    align-items: flex-start;
    flex-direction: column;

    h1 {
      font-size: 22px;
    }
  }
}
</style>
