<template>
  <div class="search-page">
    <div class="search-header">
      <h2>搜索结果: {{ keyword }}</h2>
      <p>共找到 {{ total }} 个结果</p>
    </div>

    <div class="search-results" v-loading="loading">
      <PostCard v-for="post in posts" :key="post.id" :post="post" />
      <el-empty v-if="!loading && posts.length === 0" description="未找到相关帖子" />
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
import { searchPosts } from '@/api/search'
import PostCard from '@/components/PostCard.vue'

const route = useRoute()

const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const keyword = computed(() => route.query.keyword || '')

const fetchResults = async () => {
  if (!keyword.value) return
  loading.value = true
  try {
    const res = await searchPosts({
      keyword: keyword.value,
      page: currentPage.value,
      size: pageSize.value
    })
    posts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchResults()
}

watch(keyword, () => {
  currentPage.value = 1
  fetchResults()
})

onMounted(() => {
  fetchResults()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.search-page {
  max-width: 800px;
  margin: 0 auto;
}

.search-header {
  margin-bottom: 20px;

  h2 {
    margin: 0 0 8px;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: $text-secondary;
  }
}

.search-results {
  min-height: 200px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
