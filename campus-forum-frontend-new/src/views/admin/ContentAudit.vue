<template>
  <div class="content-audit-page">
    <el-card>
      <template #header>
        <span>内容审核</span>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审帖子" name="posts" />
        <el-tab-pane label="待审评论" name="comments" />
      </el-tabs>

      <el-table :data="records" v-loading="loading" stripe>
        <template v-if="activeTab === 'posts'">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="220">
            <template #default="{ row }">
              <router-link :to="`/post/${row.id}`" class="post-link">
                {{ row.title }}
              </router-link>
            </template>
          </el-table-column>
          <el-table-column prop="authorNickname" label="作者" width="120" />
          <el-table-column prop="boardName" label="板块" width="120" />
          <el-table-column prop="createdAt" label="发布时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </template>

        <template v-else>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="作者" width="140">
            <template #default="{ row }">{{ row.author?.nickname || row.author?.id || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" width="180">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </template>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleAudit(row.id, true)">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="handleAudit(row.id, false)">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchRecords"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPendingPosts, auditPost, getPendingComments, auditComment } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const records = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const activeTab = ref('posts')

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    const res = activeTab.value === 'posts'
      ? await getPendingPosts(params)
      : await getPendingComments(params)
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取待审核内容失败:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchRecords()
}

const handleAudit = async (id, approved) => {
  const action = approved ? '通过' : '拒绝'
  const target = activeTab.value === 'posts' ? '帖子' : '评论'
  try {
    await ElMessageBox.confirm(`确定要${action}这个${target}吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    if (activeTab.value === 'posts') {
      await auditPost(id, approved)
    } else {
      await auditComment(id, approved)
    }
    ElMessage.success(`已${action}`)
    fetchRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`审核失败:`, error)
    }
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.post-link {
  color: $primary-color;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
