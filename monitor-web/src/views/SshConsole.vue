<template>
  <div class="ssh-page">
    <!-- 1. 连接表单：未连接时显示 -->
    <div v-if="!isConnected" class="login-box">
      <h2>🔌 远程连接服务器</h2>
      <el-form :model="form" label-width="80px">
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" placeholder="例如 192.168.1.100" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="例如 root" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="connect" style="width: 100%">立即连接</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 2. 终端窗口：连接成功后显示 -->
    <div v-else class="terminal-box">
      <!-- 顶部工具栏 -->
      <div class="toolbar">
        <span>当前连接: {{ form.username }}@{{ form.ip }}</span>
        <el-button type="danger" size="small" @click="disconnect">断开连接</el-button>
      </div>

      <!-- 引入刚才封装的组件 -->
      <Terminal :ssh-info="form" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import Terminal from '@/components/Terminal.vue'

const isConnected = ref(false)

// 默认填一些数据方便测试 (你可以改成空的)
const form = reactive({
  ip: '',
  username: 'root',
  password: ''
})

const connect = () => {
  if (!form.ip || !form.password) {
    alert('请补全信息')
    return
  }
  isConnected.value = true
}

const disconnect = () => {
  isConnected.value = false
  // 可以在这里刷新页面或者重置状态
}
</script>

<style scoped>
.ssh-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.login-box {
  width: 400px;
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.terminal-box {
  width: 100%;
  height: 100vh; /* 全屏显示 */
  display: flex;
  flex-direction: column;
}

.toolbar {
  background: #333;
  color: white;
  padding: 10px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>