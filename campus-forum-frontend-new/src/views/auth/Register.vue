<template>
  <div class="register-page">
    <div class="register-card">
      <h2>注册校园论坛</h2>
      <el-steps :active="step" finish-status="success" class="steps">
        <el-step title="填写信息" />
        <el-step title="验证邮箱" />
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
          <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleNext" :loading="loading">
            下一步
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 第二步：邮箱验证 -->
      <el-form v-if="step === 1" ref="formRef2" :model="form" :rules="rules2">
        <el-form-item>
          <p class="verify-tip">验证码已发送至 {{ form.email }}</p>
        </el-form-item>
        <el-form-item prop="verifyCode">
          <el-input v-model="form.verifyCode" placeholder="请输入6位验证码" maxlength="6">
            <template #append>
              <el-button
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleVerify" :loading="loading">
            验证
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 第三步：注册成功 -->
      <div v-if="step === 2" class="success-step">
        <el-icon class="success-icon" color="#67c23a" :size="64"><CircleCheck /></el-icon>
        <h3>注册成功！</h3>
        <p>欢迎加入校园论坛</p>
        <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
      </div>

      <div class="register-footer" v-if="step < 2">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'
import { User, UserFilled, Message, Lock, CircleCheck } from '@element-plus/icons-vue'
import { register, verifyRegister } from '@/api/auth'
import { ElMessage } from 'element-plus'

const step = ref(0)
const formRef1 = ref(null)
const formRef2 = ref(null)
const loading = ref(false)
const countdown = ref(0)

const form = ref({
  studentNo: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
  verifyCode: ''
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
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
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

const rules2 = {
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
}

const registerPayload = () => ({
  studentNo: form.value.studentNo,
  nickname: form.value.nickname,
  email: form.value.email,
  password: form.value.password
})

const handleNext = async () => {
  const valid = await formRef1.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(registerPayload())
    ElMessage.success('验证码已发送')
    step.value = 1
    startCountdown()
  } catch (error) {
    console.error('发送验证码失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSendCode = async () => {
  try {
    await register(registerPayload())
    ElMessage.success('验证码已发送')
    startCountdown()
  } catch (error) {
    console.error('发送验证码失败:', error)
  }
}

const handleVerify = async () => {
  const valid = await formRef2.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await verifyRegister({
      studentNo: form.value.studentNo,
      nickname: form.value.nickname,
      email: form.value.email,
      password: form.value.password,
      code: form.value.verifyCode
    })
    step.value = 2
  } catch (error) {
    console.error('验证失败:', error)
  } finally {
    loading.value = false
  }
}

let countdownTimer = null

const startCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
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
