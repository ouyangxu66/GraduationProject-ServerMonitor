<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 品牌区域 -->
      <div class="brand-section">
        <div class="logo-box">🚀</div>
        <h1 class="sys-title">Monitor System</h1>
        <p class="sys-desc">创建您的新账户</p>
      </div>

      <!-- 表单区域 -->
      <div class="form-section">
        <h2 class="form-title">注册账号</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="flat-form">
          <el-form-item prop="username">
            <el-input
                v-model="form.username"
                placeholder="设置用户名"
                class="flat-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                v-model="form.password"
                type="password"
                placeholder="设置密码"
                show-password
                class="flat-input"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="确认密码"
                show-password
                class="flat-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="flat-btn" @click="handleRegister">
              立即注册
            </el-button>
          </el-form-item>

          <div class="footer-links">
            <router-link to="/login" class="link">已有账号？去登录</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

const handleRegister = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await register(form)
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (e) {
        // 错误由拦截器处理
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 复用登录页的样式，保持风格统一 */
.login-container {
  height: 100vh;
  width: 100%;
  background-color: var(--el-bg-color);
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--el-text-color-primary);
}

.login-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 420px;
  padding: 40px;
  border: 2px solid var(--el-border-color-light);
  border-radius: 24px;
  background-color: var(--el-bg-color-overlay);
}

.brand-section {
  text-align: center;
  margin-bottom: 30px;
}

.logo-box { font-size: 48px; margin-bottom: 10px; }

.sys-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0;
  color: var(--el-text-color-primary);
}

.sys-desc {
  color: var(--el-text-color-secondary);
  margin-top: 8px;
  font-weight: 500;
}

.form-section { width: 100%; }

.form-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--el-text-color-primary);
  border-left: 4px solid var(--el-color-primary);
  padding-left: 10px;
}

:deep(.flat-input .el-input__wrapper) {
  box-shadow: none !important;
  border: 2px solid var(--el-border-color);
  border-radius: 8px;
  background-color: var(--el-fill-color-light);
  transition: all 0.2s;
}

:deep(.flat-input .el-input__wrapper.is-focus) {
  border-color: var(--el-color-primary);
  background-color: var(--el-bg-color);
}

.flat-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 8px;
}

.footer-links {
  margin-top: 20px;
  text-align: center;
}

.link {
  color: var(--el-color-primary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
}
.link:hover { text-decoration: underline; }
</style>