<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>{{ isSelf ? '个人中心' : '用户主页' }}</span>
        </div>
      </template>

      <div class="profile-info">
        <el-avatar :size="80" :src="userProfile?.avatar" />
        <div class="info-detail">
          <h2>{{ userProfile?.nickname }}</h2>
          <p class="bio">{{ userProfile?.bio || '这个人很懒，什么都没写...' }}</p>
          <div class="stats">
            <span>帖子: {{ userProfile?.postCount || 0 }}</span>
            <span>获赞: {{ userProfile?.likeCount || 0 }}</span>
            <span>注册于: {{ formatTime(userProfile?.createdAt) }}</span>
          </div>
        </div>
        <el-button v-if="isSelf" type="primary" @click="showEditDialog = true">
          编辑资料
        </el-button>
      </div>
    </el-card>

    <!-- 我的帖子 -->
    <el-card class="section-card" v-if="isSelf">
      <template #header>
        <span>我的帖子</span>
      </template>
      <div v-loading="loadingPosts">
        <PostCard v-for="post in myPosts" :key="post.id" :post="post" />
        <el-empty v-if="!loadingPosts && myPosts.length === 0" description="暂无帖子" />
      </div>
    </el-card>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑资料" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="个人简介" prop="bio">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
          >
            <el-avatar :size="80" :src="editForm.avatar" />
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile" :loading="updating">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserProfile } from '@/api/user'
import { updateProfile, uploadAvatar } from '@/api/auth'
import PostCard from '@/components/PostCard.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()

const userProfile = ref(null)
const myPosts = ref([])
const loadingPosts = ref(false)
const showEditDialog = ref(false)
const updating = ref(false)
const editFormRef = ref(null)

const userId = computed(() => route.params.id || userStore.user?.id)
const isSelf = computed(() => !route.params.id || Number(route.params.id) === userStore.user?.id)

const editForm = ref({
  nickname: '',
  bio: '',
  avatar: ''
})

const editRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' }
  ]
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN')
}

const fetchProfile = async () => {
  try {
    const res = await getUserProfile(userId.value)
    userProfile.value = res.data
    editForm.value.nickname = res.data.nickname
    editForm.value.bio = res.data.bio
    editForm.value.avatar = res.data.avatar
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const beforeAvatarUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
  }
  return isImage && isLt5M
}

const handleAvatarUpload = async (options) => {
  try {
    const res = await uploadAvatar(options.file)
    editForm.value.avatar = res.data
    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('上传头像失败:', error)
  }
}

const handleUpdateProfile = async () => {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  updating.value = true
  try {
    await updateProfile({
      nickname: editForm.value.nickname,
      bio: editForm.value.bio
    })
    ElMessage.success('更新成功')
    showEditDialog.value = false
    fetchProfile()
    userStore.fetchUserInfo()
  } catch (error) {
    console.error('更新失败:', error)
  } finally {
    updating.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.profile-page {
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 20px;
}

.profile-info {
  display: flex;
  gap: 20px;
  align-items: flex-start;

  .info-detail {
    flex: 1;

    h2 {
      margin: 0 0 8px;
    }

    .bio {
      color: $text-secondary;
      margin: 0 0 12px;
    }

    .stats {
      display: flex;
      gap: 20px;
      color: $text-secondary;
      font-size: 14px;
    }
  }
}

.section-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.avatar-uploader {
  :deep(.el-upload) {
    cursor: pointer;
  }
}
</style>
