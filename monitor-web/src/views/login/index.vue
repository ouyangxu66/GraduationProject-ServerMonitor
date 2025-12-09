<template>
  <div class="login-container">
    <div class="login-box">
      <!-- 头部标题 -->
      <div class="login-header">
        <div class="logo-icon">🚀</div>
        <h2>Monitor System</h2>
        <p>分布式服务器运维监控平台</p>
      </div>

      <!-- 表单区域 -->
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="请输入账号"
              prefix-icon="User"
              class="custom-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
              class="custom-input"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer-text">
        <span>© 2025 Monitor System Tech.</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(form)
        router.push('/')
      } catch (e) {
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 1. 背景：深色渐变 */
.login-container {
  height: 100vh;
  width: 100%;
  /* 漂亮的蓝紫渐变背景 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  position: relative;
}

/* 增加一些背景装饰球 (可选) */
.login-container::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.login-container::after {
  content: '';
  position: absolute;
  bottom: -50px;
  left: -50px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

/* 2. 登录卡片：毛玻璃效果 */
.login-box {
  width: 420px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.95); /* 白色背景，轻微透明 */
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  z-index: 1;
  text-align: center;
}

.login-header {
  margin-bottom: 30px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 10px;
  display: inline-block;
  animation: float 3s ease-in-out infinite;
}

.login-header h2 {
  font-size: 28px;
  color: #333;
  font-weight: 700;
  margin: 0;
  background: -webkit-linear-gradient(45deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.login-header p {
  color: #999;
  margin-top: 10px;
  font-size: 14px;
}

/* 按钮样式 */
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
}

.login-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.4);
}

.footer-text {
  margin-top: 20px;
  color: #ccc;
  font-size: 12px;
}

/* 简单浮动动画 */
@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
  100% { transform: translateY(0px); }
}
</style>