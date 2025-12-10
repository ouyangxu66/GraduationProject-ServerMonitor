<template>
  <div class="ssh-wrapper">
    <!-- 未连接：显示表单 -->
    <div v-if="!connected" class="connect-panel">
      <div class="panel-content">
        <h2 class="panel-title">建立远程连接</h2>
        <el-form :model="form" label-width="0" class="ssh-form">
          <el-form-item>
            <el-input v-model="form.host" placeholder="IP 地址" size="large">
              <template #prefix><el-icon><Monitor /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.port" placeholder="端口 (22)" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="connect-btn" :loading="loading" @click="initSsh">
            立即连接
          </el-button>
        </el-form>
      </div>
    </div>

    <!-- 已连接：显示终端 -->
    <div v-else class="terminal-container">
      <div class="terminal-header">
        <div class="status-box">
          <span class="status-dot"></span>
          <span class="status-text">{{ form.username }}@{{ form.host }}</span>
        </div>
        <el-button type="danger" size="small" plain @click="disconnect">断开连接</el-button>
      </div>
      <!-- 终端挂载点 -->
      <div id="xterm" class="xterm-box"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount, nextTick, onMounted, onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
import 'xterm/css/xterm.css'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { Monitor, User, Lock } from '@element-plus/icons-vue'

// 🟢 关键：定义组件名称以支持 keep-alive
defineOptions({
  name: 'WebSsh'
})

const userStore = useUserStore()
const route = useRoute()
const connected = ref(false)
const loading = ref(false)

const form = reactive({
  host: route.query.ip || '',
  port: route.query.port || 22,
  username: route.query.user || '',
  password: route.query.pwd || ''
})

let term = null
let socket = null
let fitAddon = null

// 🟢 关键：切换 Tab 回来时重新调整终端大小
onActivated(() => {
  if (connected.value && fitAddon) {
    setTimeout(() => {
      fitAddon.fit()
      term?.focus()
    }, 100) // 微小延迟确保 DOM 渲染
  }
})

const initSsh = () => {
  if (!form.host || !form.password) return ElMessage.warning('信息不完整')
  loading.value = true

  // 创建 WebSocket
  const wsUrl = `ws://localhost:8080/ws/ssh?token=${userStore.token}`
  socket = new WebSocket(wsUrl)

  socket.onopen = () => {
    // 发送认证包
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
    if (!connected.value) {
      // 第一次收到消息，说明连接成功
      connected.value = true
      loading.value = false
      nextTick(() => initXterm(e.data))
    } else {
      term.write(e.data)
    }
  }

  socket.onclose = () => {
    connected.value = false
    loading.value = false
    term?.dispose()
    ElMessage.warning('连接已断开')
  }

  socket.onerror = () => {
    connected.value = false
    loading.value = false
    ElMessage.error('连接失败')
  }
}

const initXterm = (initMsg) => {
  term = new Terminal({
    fontSize: 15,
    cursorBlink: true,
    fontFamily: 'Menlo, Monaco, Consolas, monospace',
    theme: {
      background: '#1e1e1e', // 终端保持深色背景
      foreground: '#ffffff',
    }
  })

  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  term.open(document.getElementById('xterm'))

  // 写入初始消息
  if (initMsg) term.write(initMsg)
  fitAddon.fit()

  term.onData(data => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ operate: 'command', command: data }))
    }
  })

  window.addEventListener('resize', () => fitAddon.fit())
}

const disconnect = () => {
  socket?.close()
  connected.value = false
}

onBeforeUnmount(() => {
  disconnect()
  window.removeEventListener('resize', fitAddon?.fit)
})

onMounted(() => {
  if (form.host && form.password) initSsh()
})
</script>

<style scoped>
.ssh-wrapper {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 连接面板适配暗黑模式 */
.connect-panel {
  width: 420px;
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 40px;
  box-shadow: var(--el-box-shadow-light);
}

.panel-title {
  text-align: center;
  margin-bottom: 30px;
  color: var(--el-text-color-primary);
  font-weight: 800;
}

.connect-btn {
  width: 100%;
  font-weight: 700;
  margin-top: 10px;
}

/* 终端容器 */
.terminal-container {
  width: 100%;
  height: 85vh;
  background-color: #1e1e1e; /* 终端背景始终为深色 */
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 2px solid var(--el-border-color-darker);
}

.terminal-header {
  height: 44px;
  background-color: #252526;
  display: flex;
  align-items: center;
  padding: 0 15px;
  justify-content: space-between;
  border-bottom: 1px solid #333;
}

.status-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  width: 10px;
  height: 10px;
  background-color: #2ecc71;
  border-radius: 50%;
  box-shadow: 0 0 8px #2ecc71;
}

.status-text {
  color: #ccc;
  font-size: 14px;
  font-family: monospace;
}

.xterm-box {
  flex: 1;
  padding: 8px;
  background-color: #1e1e1e;
  /* 修复滚动条样式 */
  &::-webkit-scrollbar {
    width: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background: #444;
    border-radius: 4px;
  }
}
</style>