<template>
  <div class="ssh-wrapper">
    <!-- 未连接：显示表单 -->
    <div v-if="!connected" class="connect-panel">
      <div class="panel-content">
        <h2 class="panel-title">建立远程连接</h2>

        <el-form :model="form" label-width="0" class="ssh-form">
          <!-- 认证方式 -->
          <el-form-item>
            <el-radio-group v-model="form.authType" class="auth-type">
              <el-radio-button value="password">密码登录</el-radio-button>
              <el-radio-button value="publicKey">私钥登录</el-radio-button>
            </el-radio-group>
          </el-form-item>

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

          <!-- 密码模式 -->
          <template v-if="form.authType === 'password'">
            <el-form-item>
              <el-input v-model="form.password" type="password" placeholder="密码" show-password size="large">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
          </template>

          <!-- 私钥模式 -->
          <template v-else>
            <el-form-item>
              <el-input
                v-model="form.privateKey"
                type="textarea"
                :autosize="{ minRows: 6, maxRows: 10 }"
                placeholder="粘贴 PEM 格式私钥（包含 BEGIN/END 行）"
                class="privatekey-input"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.passphrase"
                type="password"
                placeholder="私钥口令（如有）"
                show-password
                size="large"
              />
            </el-form-item>
          </template>

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
  authType: 'password',
  host: route.query.ip || '',
  port: route.query.port || 22,
  username: route.query.user || '',
  password: route.query.pwd || '',
  privateKey: '',
  passphrase: ''
})

let term = null
let socket = null
let fitAddon = null
let resizeHandler = null

// 🟢 关键：切换 Tab 回来时重新调整终端大小
onActivated(() => {
  if (connected.value && fitAddon) {
    setTimeout(() => {
      fitAddon.fit()
      term?.focus()
    }, 100) // 微小延迟确保 DOM 渲染
  }
})

const validateForm = () => {
  if (!form.host) return '请填写 IP 地址'
  if (!form.username) return '请填写用户名'
  if (!form.port) form.port = 22

  if (form.authType === 'password') {
    if (!form.password) return '请填写密码'
  } else {
    if (!form.privateKey) return '请粘贴私钥内容'
  }
  return null
}

const initSsh = () => {
  const err = validateForm()
  if (err) return ElMessage.warning(err)

  loading.value = true

  // 创建 WebSocket
  const wsUrl = `ws://localhost:8080/ws/ssh?token=${userStore.token}`
  socket = new WebSocket(wsUrl)

  socket.onopen = () => {
    // 发送认证包
    const connectData = {
      operate: 'connect',
      authType: form.authType,
      host: form.host,
      port: Number(form.port),
      username: form.username
    }

    if (form.authType === 'password') {
      connectData.password = form.password
    } else {
      connectData.privateKey = form.privateKey
      if (form.passphrase) connectData.passphrase = form.passphrase
    }

    socket.send(JSON.stringify(connectData))
  }

  socket.onmessage = (e) => {
    // 兼容：后端既可能发纯文本，也可能发 JSON 事件
    const { isEvent, event } = tryParseEvent(e.data)

    if (isEvent) {
      if (event.type === 'ready') {
        connected.value = true
        loading.value = false
        nextTick(() => initXterm())
        return
      }

      if (event.type === 'error') {
        loading.value = false
        connected.value = false
        const msg = event.message || '连接失败'
        ElMessage.error(msg)

        // 如果终端已经初始化，把错误也写进去
        if (term) {
          term.write(`\r\n\x1b[31m${msg}\x1b[0m\r\n`)
        }

        // 由客户端主动断开，避免残留状态
        disconnect()
        return
      }
    }

    // 非事件：当作终端输出
    if (term) {
      term.write(e.data)
    }
  }

  socket.onclose = () => {
    loading.value = false

    if (connected.value) {
      ElMessage.warning('连接已断开')
    }

    connected.value = false
    term?.dispose()
    term = null
  }

  socket.onerror = () => {
    connected.value = false
    loading.value = false
    ElMessage.error('连接失败：WebSocket 建立异常')
  }
}

const tryParseEvent = (raw) => {
  if (typeof raw !== 'string') return { isEvent: false, event: null }
  if (!raw.startsWith('{')) return { isEvent: false, event: null }
  try {
    const obj = JSON.parse(raw)
    if (obj && obj.type) return { isEvent: true, event: obj }
  } catch (_) {
    // ignore
  }
  return { isEvent: false, event: null }
}

const initXterm = () => {
  // 只初始化一次
  if (term) return

  term = new Terminal({
    fontSize: 15,
    cursorBlink: true,
    fontFamily: 'Menlo, Monaco, Consolas, monospace',
    theme: {
      background: '#1e1e1e',
      foreground: '#ffffff'
    }
  })

  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)
  term.open(document.getElementById('xterm'))

  fitAddon.fit()
  term.focus()

  term.onData((data) => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ operate: 'command', command: data }))
    }
  })

  resizeHandler = () => fitAddon?.fit()
  window.addEventListener('resize', resizeHandler)
}

const disconnect = () => {
  try {
    socket?.close()
  } catch (_) {
    // ignore
  }
  socket = null
  connected.value = false
}

onBeforeUnmount(() => {
  disconnect()
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
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

.panel-content {
  max-height: 85vh;
  overflow: auto;
}

.panel-title {
  text-align: center;
  margin-bottom: 20px;
  color: var(--el-text-color-primary);
  font-weight: 800;
}

.auth-type {
  width: 100%;
  display: flex;
  justify-content: center;
}

.privatekey-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
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