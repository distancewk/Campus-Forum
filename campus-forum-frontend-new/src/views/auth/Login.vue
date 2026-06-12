<template>
  <div class="login-page">
    <div class="auth-shell">
      <section class="auth-intro">
        <div class="brand-mark">校</div>
        <h1>校园论坛</h1>
        <p>回到校园讨论现场。</p>
      </section>

      <section class="login-card">
        <h2>登录账号</h2>
        <p class="form-subtitle">使用学号进入你的社区空间</p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="studentNo">
          <el-input
            v-model="form.studentNo"
            placeholder="请输入学号"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            @click="handleLogin"
            :loading="loading"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <router-link to="/forgot-password">忘记密码？</router-link>
        <span>还没有账号？<router-link to="/register">立即注册</router-link></span>
      </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = ref({
  studentNo: '',
  password: ''
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.value.studentNo, form.value.password)
    ElMessage.success('登录成功')
    // 防止开放重定向：只允许站内路径
    const redirect = route.query.redirect
    const safeRedirect = (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) ? redirect : '/'
    router.push(safeRedirect)
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/tokens.scss' as *;

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(15, 159, 143, 0.1)),
    $bg-page;
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(280px, 380px) minmax(340px, 440px);
  width: min(900px, 100%);
  overflow: hidden;
  border: 1px solid $border-light;
  border-radius: $radius-xl;
  background-color: #fff;
  box-shadow: $shadow-md;
}

.auth-intro {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 520px;
  padding: 36px;
  color: #fff;
  background:
    linear-gradient(160deg, rgba(16, 32, 51, 0.86), rgba(37, 99, 235, 0.72)),
    linear-gradient(135deg, $primary-color, $accent-color);

  .brand-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 48px;
    height: 48px;
    margin-bottom: 22px;
    border-radius: $radius-lg;
    background-color: rgba(255, 255, 255, 0.18);
    font-size: 22px;
    font-weight: 800;
  }

  h1 {
    margin: 0;
    font-size: 34px;
    line-height: 1.2;
  }

  p {
    margin: 10px 0 0;
    color: rgba(255, 255, 255, 0.78);
    font-size: 16px;
  }
}

.login-card {
  padding: 44px 42px;

  h2 {
    margin: 0;
    font-size: 28px;
    line-height: 1.25;
    color: $text-primary;
  }

  .form-subtitle {
    margin: 8px 0 28px;
    color: $text-secondary;
  }

  .login-btn {
    width: 100%;
    height: 42px;
    margin-top: 6px;
  }

  .login-footer {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-top: 12px;
    font-size: 14px;
    color: $text-secondary;

    a {
      color: $primary-color;
    }
  }
}

@include mobile {
  .login-page {
    padding: 16px;
  }

  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-intro {
    min-height: 180px;
    padding: 28px;

    h1 {
      font-size: 28px;
    }
  }

  .login-card {
    padding: 28px 22px;

    .login-footer {
      align-items: flex-start;
      flex-direction: column;
    }
  }
}
</style>
