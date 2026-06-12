<template>
  <div class="forgot-password-page">
    <div class="card">
      <h2>重置密码</h2>
      <el-steps :active="step" finish-status="success" class="steps">
        <el-step title="验证邮箱" />
        <el-step title="重置密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- 第一步：输入邮箱 -->
      <el-form v-if="step === 0" ref="formRef1" :model="form" :rules="rules1">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="请输入注册邮箱" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="verifyCode">
          <el-input v-model="form.verifyCode" placeholder="请输入6位验证码" maxlength="6">
            <template #append>
              <el-button :disabled="countdown > 0" @click="handleSendCode">
                {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleNext" :loading="loading">
            下一步
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 第二步：重置密码 -->
      <el-form v-if="step === 1" ref="formRef2" :model="form" :rules="rules2">
        <el-form-item prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="新密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleReset" :loading="loading">
            重置密码
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 第三步：成功 -->
      <div v-if="step === 2" class="success-step">
        <el-icon class="success-icon" color="#67c23a" :size="64"><CircleCheck /></el-icon>
        <h3>密码重置成功！</h3>
        <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
      </div>

      <div class="footer" v-if="step < 2">
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Message, Lock, CircleCheck } from '@element-plus/icons-vue'
import { forgotPassword, resetPassword } from '@/api/auth'
import { ElMessage } from 'element-plus'

const step = ref(0)
const formRef1 = ref(null)
const formRef2 = ref(null)
const loading = ref(false)
const countdown = ref(0)

const form = ref({
  email: '',
  verifyCode: '',
  newPassword: '',
  confirmPassword: ''
})

const rules1 = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
}

const rules2 = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' }
  ]
}

const handleSendCode = async () => {
  try {
    await forgotPassword({ email: form.value.email })
    ElMessage.success('验证码已发送')
    startCountdown()
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

const handleNext = async () => {
  const valid = await formRef1.value.validate().catch(() => false)
  if (!valid) return
  step.value = 1
}

const handleReset = async () => {
  const valid = await formRef2.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPassword({
      email: form.value.email,
      code: form.value.verifyCode,
      newPassword: form.value.newPassword
    })
    step.value = 2
  } catch (error) {
    console.error('重置密码失败:', error)
  } finally {
    loading.value = false
  }
}

const startCountdown = () => {
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.forgot-password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(15, 159, 143, 0.1)),
    $bg-page;
}

.card {
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

.success-step {
  text-align: center;
  padding: 40px 0;

  h3 {
    margin: 16px 0 24px;
  }
}

.footer {
  text-align: center;
  margin-top: 20px;

  a {
    color: $primary-color;
  }
}

@include mobile {
  .forgot-password-page {
    padding: 16px;
  }

  .card {
    padding: 28px 20px;
  }
}
</style>
