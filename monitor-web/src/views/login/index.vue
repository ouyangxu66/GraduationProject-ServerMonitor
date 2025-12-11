<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 1. 左侧/顶部：品牌区域 -->
      <div class="brand-section">
        <div class="logo-box">🚀</div>
        <h1 class="sys-title">Monitor System</h1>
        <p class="sys-desc">分布式服务器运维监控平台</p>
      </div>

      <!-- 2. 右侧/底部：表单区域 -->
      <div class="form-section">
        <h2 class="form-title">用户登录</h2>

        <!-- 登录表单 -->
        <el-form ref="formRef" :model="form" :rules="rules" size="large" class="flat-form">
          <!-- 账号输入 -->
          <el-form-item prop="username">
            <el-input
                v-model="form.username"
                placeholder="请输入账号"
                class="flat-input"
            />
          </el-form-item>

          <!-- 密码输入 -->
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

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button type="primary" :loading="loading" class="flat-btn" @click="handleLogin">
              立即登录
            </el-button>
          </el-form-item>

          <!-- 🟢 新增：注册跳转链接 -->
          <div class="footer-links">
            <span class="text-gray">还没有账号？</span>
            <span class="link-text" @click="toRegister">立即注册</span>
          </div>
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

const toRegister = () => {
  console.log('1. 点击了注册按钮') // 调试日志
  router.push('/register')
      .then(() => {
        console.log('2. 跳转成功')
      })
      .catch(err => {
        console.error('3. 跳转失败:', err)
      })
}

// 表单数据绑定
const form = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录逻辑处理
const handleLogin = () => {
  if (!formRef.value) return

  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 1. 调用 Store 的登录 Action
        await userStore.login(form)

        // 2. 成功提示
        ElMessage.success('登录成功，欢迎回来')

        // 3. 跳转到首页
        router.push('/')

      } catch (error) {
        // 4. 错误处理 (错误信息通常来自拦截器或后端)
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
/* --- 全局容器布局 --- */
/* 容器：纯白背景，Flex居中 */
.login-container {
  height: 100vh;
  width: 100%;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #2c3e50;
}

/* 登录框主体：无阴影，使用圆滑边框 */
.login-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 400px;
  padding: 40px;
  border: 2px solid #ecf0f1; /* 浅灰边框，符合扁平化 */
  border-radius: 24px;       /* 大圆角 */
  background-color: #ffffff;
}

/* --- 品牌区域样式 --- */
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
  font-weight: 800; /* 加粗强调 */
  margin: 0;
  color: #2c3e50;
  letter-spacing: 1px;
}

.sys-desc {
  color: #95a5a6; /* 浅灰色描述 */
  margin-top: 8px;
  font-weight: 500;
}

/* --- 表单区域样式 --- */
.form-section {
  width: 100%;
}

.form-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #2c3e50;
  border-left: 4px solid #3498db; /* 蓝色竖条装饰 */
  padding-left: 10px;
}

/* --- 扁平化组件重写 --- */

/* 输入框样式重写 */
:deep(.flat-input .el-input__wrapper) {
  box-shadow: none !important; /* 去除默认阴影 */
  border: 2px solid #bdc3c7;   /* 实线边框 */
  border-radius: 8px;
  background-color: #f9f9f9;
  transition: all 0.2s;
}

/* 输入框悬停和聚焦状态 */
:deep(.flat-input .el-input__wrapper:hover),
:deep(.flat-input .el-input__wrapper.is-focus) {
  border-color: #3498db; /* 聚焦变成主色调蓝 */
  background-color: #fff;
}

/* 登录按钮样式 */
.flat-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 8px;
  background-color: #3498db; /* 纯蓝 */
  border: none;
  transition: background-color 0.2s;
}

.flat-btn:hover {
  background-color: #2980b9; /* 悬停变深 */
}

.flat-btn:active {
  transform: scale(0.98); /* 点击微缩反馈 */
}

/* --- 🟢 底部链接样式 (新增) --- */
.footer-links {
  margin-top: 15px;
  text-align: center;
  font-size: 14px;
}

.text-gray {
  color: #95a5a6;
}

.link-text {
  color: #3498db;
  text-decoration: none;
  font-weight: 600;
  margin-left: 5px;
  transition: color 0.2s;
  cursor: pointer; /* 🟢 必须加这个，否则用户不知道可以点 */
}

.link-text:hover {
  color: #2980b9;
  text-decoration: underline; /* 悬停加下划线 */
}
</style>