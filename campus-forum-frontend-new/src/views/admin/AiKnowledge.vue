<template>
  <div class="ai-knowledge-page">
    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="资料库" name="documents">
        <section class="admin-panel">
          <div class="panel-header">
            <div>
              <h2>AI 知识库资料</h2>
              <p>上传 PDF、Word、Markdown 或文本资料后自动索引。</p>
            </div>
            <el-button :icon="Refresh" circle title="刷新资料" @click="fetchDocuments" />
          </div>

          <el-form class="upload-form" @submit.prevent>
            <el-input v-model="title" placeholder="资料标题" clearable />
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="1"
              :on-change="onFileChange"
              :on-remove="onFileRemove"
              :show-file-list="true"
            >
              <el-button>
                <el-icon><UploadFilled /></el-icon>
                选择文件
              </el-button>
            </el-upload>
            <el-button type="primary" :loading="uploading" @click="submitUpload">上传并索引</el-button>
          </el-form>

          <el-table :data="documents" v-loading="loading" stripe>
            <el-table-column prop="title" label="标题" min-width="220" />
            <el-table-column prop="fileType" label="格式" width="90" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleReindex(row.id)">重建</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="documentPage"
              :page-size="pageSize"
              :total="documentTotal"
              layout="prev, pager, next"
              @current-change="fetchDocuments"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 审核建议" name="moderation">
        <section class="admin-panel">
          <div class="panel-header">
            <div>
              <h2>AI 审核建议</h2>
              <p>模型只提供风险提示，封禁和删除仍由管理员处理。</p>
            </div>
            <el-button :icon="Refresh" circle title="刷新审核建议" @click="fetchModeration" />
          </div>

          <div class="filters">
            <el-select v-model="moderationFilters.targetType" clearable placeholder="内容类型">
              <el-option label="帖子" value="POST" />
              <el-option label="评论" value="COMMENT" />
            </el-select>
            <el-select v-model="moderationFilters.riskLevel" clearable placeholder="风险等级">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
            </el-select>
            <el-select v-model="moderationFilters.riskType" clearable placeholder="风险类型">
              <el-option label="广告" value="ADVERTISEMENT" />
              <el-option label="辱骂" value="ABUSE" />
              <el-option label="诈骗" value="SCAM" />
              <el-option label="联系方式引流" value="CONTACT_DIVERSION" />
              <el-option label="敏感信息" value="SENSITIVE_INFO" />
              <el-option label="灌水" value="FLOODING" />
            </el-select>
            <el-button type="primary" @click="fetchModeration">筛选</el-button>
          </div>

          <el-table :data="moderationRecords" v-loading="moderationLoading" stripe>
            <el-table-column prop="targetType" label="类型" width="90">
              <template #default="{ row }">{{ sourceLabel(row.targetType) }}</template>
            </el-table-column>
            <el-table-column prop="targetId" label="内容 ID" width="100" />
            <el-table-column prop="riskLevel" label="风险" width="100">
              <template #default="{ row }">
                <el-tag :type="riskTag(row.riskLevel)">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险类型" min-width="170">
              <template #default="{ row }">{{ listText(row.riskTypes) }}</template>
            </el-table-column>
            <el-table-column label="AI 原因" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">{{ listText(row.reasons) }}</template>
            </el-table-column>
            <el-table-column prop="suggestedAction" label="建议" width="100" />
            <el-table-column prop="status" label="状态" width="140" />
            <el-table-column prop="createdAt" label="时间" width="180">
              <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="moderationPage"
              :page-size="pageSize"
              :total="moderationTotal"
              layout="prev, pager, next"
              @current-change="fetchModeration"
            />
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, UploadFilled } from '@element-plus/icons-vue'
import {
  deleteAiDocument,
  getAiDocuments,
  getAiModeration,
  reindexAiDocument,
  uploadAiDocument
} from '@/api/adminAi'

const activeTab = ref('documents')
const title = ref('')
const selectedFile = ref(null)
const uploadRef = ref(null)
const documents = ref([])
const loading = ref(false)
const uploading = ref(false)
const documentPage = ref(1)
const documentTotal = ref(0)
const pageSize = 20

const moderationRecords = ref([])
const moderationLoading = ref(false)
const moderationPage = ref(1)
const moderationTotal = ref(0)
const moderationFilters = reactive({
  targetType: '',
  riskLevel: '',
  riskType: ''
})

const onFileChange = (file) => {
  selectedFile.value = file
}

const onFileRemove = () => {
  selectedFile.value = null
}

const submitUpload = async () => {
  if (!selectedFile.value?.raw) {
    ElMessage.warning('请选择资料文件')
    return
  }
  const formData = new FormData()
  formData.append('file', selectedFile.value.raw)
  formData.append('title', title.value.trim() || selectedFile.value.name)

  uploading.value = true
  try {
    await uploadAiDocument(formData)
    ElMessage.success('资料已上传并开始索引')
    title.value = ''
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    fetchDocuments()
  } catch (error) {
    console.error('上传资料失败:', error)
  } finally {
    uploading.value = false
  }
}

const fetchDocuments = async () => {
  loading.value = true
  try {
    const res = await getAiDocuments({ page: documentPage.value, size: pageSize })
    documents.value = res.data?.records || []
    documentTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('获取资料列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchModeration = async () => {
  moderationLoading.value = true
  try {
    const res = await getAiModeration({
      page: moderationPage.value,
      size: pageSize,
      ...moderationFilters
    })
    moderationRecords.value = res.data?.records || []
    moderationTotal.value = res.data?.total || 0
  } catch (error) {
    console.error('获取 AI 审核建议失败:', error)
  } finally {
    moderationLoading.value = false
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这份资料吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteAiDocument(id)
    ElMessage.success('已删除')
    fetchDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除资料失败:', error)
    }
  }
}

const handleReindex = async (id) => {
  try {
    await reindexAiDocument(id)
    ElMessage.success('已提交重建')
    fetchDocuments()
  } catch (error) {
    console.error('重建索引失败:', error)
  }
}

const statusTag = (status) => {
  if (status === 'ACTIVE') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const riskTag = (riskLevel) => {
  if (riskLevel === 'LOW') return 'success'
  if (riskLevel === 'HIGH') return 'danger'
  return 'warning'
}

const sourceLabel = (type) => {
  const labels = {
    POST: '帖子',
    COMMENT: '评论'
  }
  return labels[type] || type
}

const listText = (value) => {
  if (!value) return '-'
  if (Array.isArray(value)) return value.join('、') || '-'
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.join('、') || '-' : String(parsed)
  } catch {
    return String(value)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

watch(activeTab, (tab) => {
  if (tab === 'moderation' && moderationRecords.value.length === 0) {
    fetchModeration()
  }
})

onMounted(() => {
  fetchDocuments()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.ai-knowledge-page {
  display: grid;
  gap: 16px;
}

.admin-panel {
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  background-color: #fff;
  padding: 20px;
  box-shadow: $shadow-xs;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: $text-primary;
  }

  p {
    margin: 6px 0 0;
    color: $text-secondary;
  }
}

.upload-form,
.filters {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.filters {
  grid-template-columns: repeat(3, minmax(150px, 180px)) auto;
}

.pagination {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@include mobile {
  .admin-panel {
    padding: 14px;
  }

  .upload-form,
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
