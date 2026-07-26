<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册校园论坛</h2>
      <el-steps :active="step === 0 ? 0 : 1" finish-status="success" class="steps">
        <el-step title="填写信息" />
        <el-step title="完成注册" />
      </el-steps>

      <!-- 第一步：填写基本信息 -->
      <el-form
        v-if="step === 0"
        ref="formRef1"
        :model="form"
        :rules="rules1"
      >
        <el-form-item prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="学号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleSubmit" :loading="loading">
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 第二步：注册成功 -->
      <div v-if="step === 1" class="success-step">
        <el-icon class="success-icon" color="#67c23a" :size="64"><CircleCheck /></el-icon>
        <h3>注册成功！</h3>
        <p>欢迎加入校园论坛</p>
        <el-button type="primary" @click="$router.push('/')">进入论坛</el-button>
      </div>

      <div class="register-footer" v-if="step === 0">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { User, UserFilled, Message, Lock, CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const step = ref(0)
const formRef1 = ref(null)
const loading = ref(false)

const form = ref({
  studentNo: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules1 = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const registerPayload = () => ({
  studentNo: form.value.studentNo,
  nickname: form.value.nickname,
  email: form.value.email,
  password: form.value.password
})

// 邮箱验证暂未启用：一步完成注册并直接落地登录态
const handleSubmit = async () => {
  const valid = await formRef1.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register(registerPayload())
    ElMessage.success('注册成功')
    step.value = 1
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error?.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(15, 159, 143, 0.1)),
    $bg-page;
}

.register-card {
  width: min(520px, 100%);
  padding: 38px;
  background-color: #fff;
  border: 1px solid $border-light;
  border-radius: $radius-xl;
  box-shadow: $shadow-md;

  h2 {
    text-align: center;
    margin: 0 0 26px;
    font-size: 28px;
    line-height: 1.25;
    color: $text-primary;
  }
}

.steps {
  margin-bottom: 28px;

  :deep(.el-step__title) {
    font-size: 13px;
    font-weight: 700;
  }
}

.submit-btn {
  width: 100%;
  height: 42px;
}

.verify-tip {
  color: $text-secondary;
  margin: 0;
}

.success-step {
  text-align: center;
  padding: 40px 0;

  h3 {
    margin: 16px 0 8px;
    font-size: 20px;
  }

  p {
    color: $text-secondary;
    margin-bottom: 24px;
  }
}

.register-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: $text-secondary;

  a {
    color: $primary-color;
  }
}

@include mobile {
  .register-page {
    padding: 16px;
  }

  .register-card {
    padding: 28px 20px;
  }
}
</style>
