<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 左侧/顶部：品牌区域 -->
      <div class="brand-section">
        <div class="logo-box">🚀</div>
        <h1 class="sys-title">Monitor System</h1>
        <p class="sys-desc">分布式服务器运维监控平台</p>
      </div>

      <!-- 右侧/底部：表单区域 -->
      <div class="form-section">
        <h2 class="form-title">用户登录</h2>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="flat-form">
          <el-form-item prop="username">
            <el-input
                v-model="form.username"
                placeholder="请输入账号"
                class="flat-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                @keyup.enter="handleLogin"
                class="flat-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="flat-btn" @click="handleLogin">
              立即登录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from "element-plus";

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
  if (!formRef.value) return

  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 调用 Store 登录
        await userStore.login(form)

        // 登录成功提示
        ElMessage.success('登录成功，欢迎回来')

        // 跳转首页
        router.push('/')

      } catch (error) {
        // 🟢 2. 捕获错误并弹窗
        // error.message 来自 request.js 拦截器中 new Error(res.msg)
        // 后端通常返回 "账号或密码错误"
        console.error('登录失败:', error)
        ElMessage.error(error.message || '登录失败，请检查账号密码')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
/* 容器：纯白背景，居中 */
.login-container {
  height: 100vh;
  width: 100%;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #2c3e50;
}

/* 登录框主体：无阴影，仅用边框区分 */
.login-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 400px;
  padding: 40px;
  border: 2px solid #ecf0f1; /* 浅灰边框 */
  border-radius: 24px;       /* 圆润线条 */
  background-color: #ffffff;
}

/* 品牌区 */
.brand-section {
  text-align: center;
  margin-bottom: 40px;
}

.logo-box {
  font-size: 48px;
  margin-bottom: 10px;
}

.sys-title {
  font-size: 28px;
  font-weight: 800; /* 加粗 */
  margin: 0;
  color: #2c3e50;
  letter-spacing: 1px;
}

.sys-desc {
  color: #95a5a6;
  margin-top: 8px;
  font-weight: 500;
}

/* 表单区 */
.form-section {
  width: 100%;
}

.form-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #2c3e50;
  border-left: 4px solid #3498db; /* 扁平化装饰条 */
  padding-left: 10px;
}

/* 扁平化输入框重写 Element 样式 */
:deep(.flat-input .el-input__wrapper) {
  box-shadow: none !important;
  border: 2px solid #bdc3c7;
  border-radius: 8px;
  background-color: #f9f9f9;
  transition: all 0.2s;
}

:deep(.flat-input .el-input__wrapper:hover),
:deep(.flat-input .el-input__wrapper.is-focus) {
  border-color: #3498db; /* 聚焦时的高对比色 */
  background-color: #fff;
}

/* 扁平化按钮 */
.flat-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 8px;
  background-color: #3498db; /* 纯色 */
  border: none;
  transition: background-color 0.2s;
}

.flat-btn:hover {
  background-color: #2980b9; /* 点击变深 */
}

.flat-btn:active {
  transform: scale(0.98); /* 极简的点击反馈 */
}
</style>