<template>
  <div class="ssh-container">
    <!-- 顶部操作栏 -->
    <el-card class="box-card" shadow="never">
      <el-form :inline="true" :model="form" class="demo-form-inline" size="default">
        <el-form-item label="服务器IP">
          <el-input v-model="form.host" placeholder="192.168.x.x" style="width: 150px" />
        </el-form-item>
        <el-form-item label="端口">
          <el-input v-model="form.port" placeholder="22" style="width: 80px" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="root" style="width: 100px" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="Password" style="width: 150px" show-password />
        </el-form-item>
        <el-form-item>
          <el-button
              type="primary"
              :icon="Connection"
              @click="initSsh"
              :disabled="connected"
              :loading="loading">
            {{ connected ? '已连接' : '立即连接' }}
          </el-button>
          <el-button
              type="danger"
              :icon="Close"
              @click="disconnect"
              v-if="connected">
            断开
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Xterm 终端挂载点 -->
    <div class="terminal-wrapper">
      <div id="xterm" class="xterm-box"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'
import { Connection, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user' // 1. 引入 UserStore

// --- 状态定义 ---
const userStore = useUserStore() // 2. 初始化 Store
const connected = ref(false)
const loading = ref(false)
const form = reactive({
  host: '192.168.1.10', // 默认值，方便调试
  port: 22,
  username: 'root',
  password: ''
})

let term = null
let socket = null
let fitAddon = null

// --- 核心连接逻辑 ---
const initSsh = () => {
  if (!form.host || !form.password) {
    ElMessage.warning('请输入完整的主机信息')
    return
  }
  loading.value = true

  // 1. 初始化终端界面
  initXterm()

  // 2. 建立 WebSocket 连接
  // 🟢 关键点：从 Store 中获取 Token 并拼接到 URL 中
  const wsUrl = `ws://localhost:8080/ws/ssh?token=${userStore.token}`
  socket = new WebSocket(wsUrl)

  // 3. 绑定 WebSocket 事件
  socket.onopen = () => {
    loading.value = false
    connected.value = true
    term.write('\r\n\x1b[32m正在连接远程服务器...\x1b[0m\r\n')

    // 发送认证数据 (连接 Linux 的账号密码)
    const authData = {
      operate: 'connect',
      host: form.host,
      port: form.port,
      username: form.username,
      password: form.password
    }
    socket.send(JSON.stringify(authData))
  }

  socket.onmessage = (e) => {
    // 接收后端返回的流数据，写入终端
    term.write(e.data)
  }

  socket.onclose = () => {
    connected.value = false
    loading.value = false
    term.write('\r\n\x1b[31m连接已断开\x1b[0m\r\n')
  }

  socket.onerror = () => {
    connected.value = false
    loading.value = false
    term.write('\r\n\x1b[31m连接发生错误，请检查网络或Token\x1b[0m\r\n')
  }
}

// --- Xterm 初始化与输入监听 ---
const initXterm = () => {
  if (term) term.dispose()

  term = new Terminal({
    fontSize: 14,
    cursorBlink: true,
    fontFamily: 'Menlo, Monaco, "Courier New", monospace',
    theme: {
      background: '#1e1e1e',
      foreground: '#ffffff'
    }
  })

  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  term.open(document.getElementById('xterm'))
  fitAddon.fit()

  // 监听终端输入 -> 发送给后端
  term.onData((data) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      // 封装为 JSON 发送 (与后端 Handler 对应)
      const command = {
        operate: 'command',
        command: data
      }
      socket.send(JSON.stringify(command))
    }
  })

  // 窗口大小改变时自适应
  window.addEventListener('resize', fitAddon.fit)
}

const disconnect = () => {
  if (socket) socket.close()
  if (term) term.dispose()
  connected.value = false
}

onBeforeUnmount(() => {
  disconnect()
  window.removeEventListener('resize', fitAddon ? fitAddon.fit : null)
})
</script>

<style scoped>
.ssh-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 10px;
  background-color: #f0f2f5;
}

.box-card {
  margin-bottom: 10px;
}

.terminal-wrapper {
  flex: 1;
  background-color: #1e1e1e;
  padding: 10px;
  border-radius: 4px;
  overflow: hidden;
  position: relative; /* 这一步很重要，确保xterm能正确计算高度 */
}

.xterm-box {
  width: 100%;
  height: 100%;
}
</style>