<template>
  <div class="post-create-page">
    <div class="page-header">
      <h1>{{ isEdit ? '编辑帖子' : '发布帖子' }}</h1>
      <p>选择合适的板块，让内容更容易被同学看到。</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="post-form"
    >
      <el-form-item label="板块" prop="boardId">
        <el-select v-model="form.boardId" placeholder="请选择板块">
          <el-option
            v-for="board in boards"
            :key="board.id"
            :label="board.name"
            :value="board.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入帖子标题"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="内容" prop="content">
        <RichEditor v-model="form.content" />
      </el-form-item>

      <el-form-item class="form-actions">
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存修改' : '发布帖子' }}
        </el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { defineAsyncComponent, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPost, updatePost, getPostDetail } from '@/api/post'
import { getBoardList } from '@/api/board'
import { ElMessage } from 'element-plus'

const RichEditor = defineAsyncComponent(() => import('@/components/RichEditor.vue'))

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const submitting = ref(false)
const boards = ref([])

const form = ref({
  boardId: null,
  title: '',
  content: ''
})

const rules = {
  boardId: [{ required: true, message: '请选择板块', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度为2-100个字符', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const fetchBoards = async () => {
  try {
    const res = await getBoardList()
    boards.value = res.data || []
  } catch (error) {
    console.error('获取板块列表失败:', error)
  }
}

const fetchPost = async () => {
  if (!isEdit.value) return
  try {
    const res = await getPostDetail(route.params.id)
    form.value.boardId = res.data.board.id
    form.value.title = res.data.title
    form.value.content = res.data.content
  } catch (error) {
    console.error('获取帖子详情失败:', error)
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updatePost(route.params.id, form.value)
      ElMessage.success('修改成功')
    } else {
      const res = await createPost(form.value)
      if (res.data?.pendingReview) {
        ElMessage.success('内容已提交，等待管理员审核')
        router.push('/')
        return
      }
      ElMessage.success('发布成功')
    }
    router.push('/')
  } catch (error) {
    console.error('发布失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchBoards()
  fetchPost()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.post-create-page {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
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

.post-form {
  background-color: #fff;
  padding: 26px;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  box-shadow: $shadow-xs;

  :deep(.el-form-item__label) {
    margin-bottom: 7px;
    color: $text-primary;
    font-weight: 700;
  }

  :deep(.el-select) {
    width: 100%;
  }
}

.form-actions {
  margin-bottom: 0;

  :deep(.el-form-item__content) {
    display: flex;
    justify-content: flex-end;
  }
}

@include mobile {
  .page-header h1 {
    font-size: 22px;
  }

  .post-form {
    padding: 18px;
  }
}
</style>
