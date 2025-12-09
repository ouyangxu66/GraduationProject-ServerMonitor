<template>
  <div class="ssh-wrapper">
    <!-- 状态1：未连接，显示连接表单 -->
    <div v-if="!connected" class="connect-panel">
      <div class="panel-content">
        <h2 class="panel-title">建立远程连接</h2>
        <el-form :model="form" label-width="0" class="ssh-form">
          <el-form-item>
            <el-input v-model="form.host" placeholder="服务器 IP 地址" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.port" placeholder="端口 (默认22)" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large" />
          </el-form-item>
          <el-button type="primary" size="large" class="connect-btn" :loading="loading" @click="initSsh">
            开始连接
          </el-button>
        </el-form>
      </div>
    </div>

    <!-- 状态2：已连接，全屏显示终端 -->
    <div v-else class="terminal-container">
      <div class="terminal-header">
        <span class="status-dot"></span>
        <span>{{ form.username }}@{{ form.host }}</span>
        <el-button type="danger" size="small" style="margin-left: auto" @click="disconnect">断开连接</el-button>
      </div>
      <div id="xterm" class="xterm-box"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { onActivated }  from "vue";

//显式定义组件名称，匹配 keep-alive 的 include
defineOptions({
  name: 'WebSsh'
})
const userStore = useUserStore()
const route = useRoute()

const connected = ref(false)
const loading = ref(false)

// 1. 默认不填内容 (除非从路由参数传过来)
const form = reactive({
  host: route.query.ip || '',
  port: route.query.port || 22,
  username: route.query.user || '',
  password: route.query.pwd || ''
})

let term = null
let socket = null
let fitAddon = null

const initSsh = () => {
  if (!form.host || !form.username || !form.password) {
    ElMessage.warning('请填写完整的连接信息')
    return
  }
  loading.value = true

  // WebSocket 连接
  const wsUrl = `ws://localhost:8080/ws/ssh?token=${userStore.token}`
  socket = new WebSocket(wsUrl)

  socket.onopen = () => {
    // 认证
    const authData = {
      operate: 'connect',
      host: form.host,
      port: Number(form.port),
      username: form.username,
      password: form.password
    }
    socket.send(JSON.stringify(authData))
  }

  socket.onmessage = (e) => {
    // 收到消息意味着连接有反馈，切换视图
    if (!connected.value) {
      connected.value = true
      loading.value = false
      // 必须在 DOM 更新后初始化 xterm
      nextTick(() => {
        initXterm()
        // 将第一条消息写入
        term.write(e.data)
      })
    } else {
      term.write(e.data)
    }
  }

  socket.onclose = () => {
    connected.value = false
    loading.value = false
    if (term) term.dispose()
    ElMessage.warning('连接已断开')
  }

  socket.onerror = () => {
    loading.value = false
    connected.value = false
    ElMessage.error('连接失败，请检查网络或服务器信息')
  }
}

const initXterm = () => {
  term = new Terminal({
    fontSize: 15,
    cursorBlink: true,
    fontFamily: 'Menlo, Monaco, "Courier New", monospace', // 漂亮的等宽字体
    theme: {
      background: '#1e1e1e',
      foreground: '#ffffff',
    }
  })
  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  term.open(document.getElementById('xterm'))
  fitAddon.fit()

  term.onData((data) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      const command = { operate: 'command', command: data }
      socket.send(JSON.stringify(command))
    }
  })

  window.addEventListener('resize', fitAddon.fit)
}

const disconnect = () => {
  if (socket) socket.close()
  connected.value = false
}

// 增加这个钩子：当页面从缓存中被激活时触发
onActivated(() => {
  if (fitAddon) {
    // 重新适应终端大小，防止切回来显示错位
    fitAddon.fit()
    // 聚焦输入框，方便直接打字
    term.focus()
  }
})

onBeforeUnmount(() => {
  disconnect()
  if (fitAddon) window.removeEventListener('resize', fitAddon.fit)
})

// 如果路由带参数，自动尝试连接
onMounted(() => {
  if (form.host && form.password) {
    initSsh()
  }
})
</script>

<style scoped>
.ssh-wrapper {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 连接面板：扁平卡片 */
.connect-panel {
  width: 400px;
  background: #fff;
  border: 2px solid #ecf0f1;
  border-radius: 16px;
  padding: 40px;
}

.panel-title {
  text-align: center;
  margin-bottom: 30px;
  color: #2c3e50;
  font-weight: 800;
}

.connect-btn {
  width: 100%;
  font-weight: 700;
  background-color: #2c3e50;
  border-color: #2c3e50;
}
.connect-btn:hover {
  background-color: #34495e;
  border-color: #34495e;
}

/* 终端区域 */
.terminal-container {
  width: 100%;
  height: 85vh; /* 占据大部分高度 */
  background-color: #1e1e1e;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2); /* 终端可以稍微有点阴影增加沉浸感 */
}

.terminal-header {
  height: 40px;
  background-color: #2d2d2d;
  display: flex;
  align-items: center;
  padding: 0 15px;
  color: #ccc;
  font-size: 14px;
}

.status-dot {
  width: 10px;
  height: 10px;
  background-color: #2ecc71;
  border-radius: 50%;
  margin-right: 10px;
}

.xterm-box {
  flex: 1;
  padding: 10px;
}
</style>