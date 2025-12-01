<template>
  <div class="terminal-wrapper">
    <!-- 终端挂载点，必须指定高度 -->
    <div ref="terminalRef" class="xterm-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
// 引入 xterm 核心库和样式
import { Terminal } from 'xterm'
import 'xterm/css/xterm.css'
// 引入自适应插件
import { FitAddon } from '@xterm/addon-fit'

// 定义 Props，接收父组件传来的连接信息
const props = defineProps({
  sshInfo: {
    type: Object,
    required: true
  }
})

const terminalRef = ref(null)
let term = null
let socket = null
let fitAddon = null

onMounted(() => {
  initTerminal()
  initWebSocket()
})

onUnmounted(() => {
  // 页面销毁时，断开连接，防止资源泄漏
  if (socket) socket.close()
  if (term) term.dispose()
})

// 1. 初始化黑窗口
const initTerminal = () => {
  term = new Terminal({
    fontSize: 14,
    cursorBlink: true, // 光标闪烁
    theme: {
      background: '#1e1e1e', // VS Code 风格深色背景
      foreground: '#ffffff'
    }
  })

  // 加载自适应插件
  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)

  // 挂载到 DOM
  term.open(terminalRef.value)
  // 自动调整大小
  fitAddon.fit()

  // 监听窗口大小变化
  window.addEventListener('resize', () => fitAddon.fit())

  // 核心交互：监听用户在终端的键盘输入
  term.onData((data) => {
    // 如果 WebSocket 是连接状态，就把字符发给后端
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(data)
    }
  })
}

// 2. 初始化 WebSocket 连接
const initWebSocket = () => {
  const { ip, username, password } = props.sshInfo
  // 拼装 WebSocket 地址，注意协议是 ws://
  // 这里假设后端在 localhost:8080
  const wsUrl = `ws://localhost:8080/ws/ssh?ip=${ip}&user=${username}&pwd=${password}`

  term.write('\r\n\x1b[33mConnecting to ' + ip + '...\x1b[0m\r\n')

  socket = new WebSocket(wsUrl)

  // 连接成功
  socket.onopen = () => {
    term.write('\r\n\x1b[32mConnection established! Welcome to WebSSH.\x1b[0m\r\n\r\n')
  }

  // 收到后端发来的 SSH 回显数据
  socket.onmessage = (e) => {
    // 把后端的数据写入 xterm，展示在屏幕上
    term.write(e.data)
  }

  // 连接关闭
  socket.onclose = () => {
    term.write('\r\n\x1b[31mConnection closed.\x1b[0m\r\n')
  }

  // 连接错误
  socket.onerror = (e) => {
    term.write('\r\n\x1b[31mConnection error.\x1b[0m\r\n')
  }
}
</script>

<style scoped>
.terminal-wrapper {
  width: 100%;
  height: 100%;
  background-color: #1e1e1e;
  padding: 10px;
  box-sizing: border-box;
}
.xterm-container {
  width: 100%;
  height: 100%; /* 撑满父容器 */
}
</style>