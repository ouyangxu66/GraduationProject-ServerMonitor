<!-- 显示SSH终端, 基于 xterm.js和WebSocket-->
<template>
  <div class="ssh-container">
    <!-- 1. 顶部：连接配置表单 -->
    <el-card class="box-card" v-if="!isConnected">
      <template #header>
        <div class="card-header">
          <span>🔌 建立 SSH 连接</span>
        </div>
      </template>
      <el-form :inline="true" :model="form" class="demo-form-inline">
        <el-form-item label="IP地址">
          <el-input v-model="form.ip" placeholder="192.168.x.x" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="root" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="******" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="connectSsh">连接</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 2. 终端显示区域 -->
    <div v-show="isConnected" class="terminal-wrapper">
      <div class="header-bar">
        <span>🟢 {{ form.username }}@{{ form.ip }}</span>
        <el-button type="danger" size="small" @click="disconnect">断开连接</el-button>
      </div>
      <!-- xterm 挂载点，必须指定 ID 或 ref -->
      <div ref="terminalRef" class="xterm-box"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { Terminal } from 'xterm'
import { FitAddon } from 'xterm-addon-fit'
// 引入 xterm 的样式，否则看起来会乱七八糟
import 'xterm/css/xterm.css'

// --- 状态定义 ---
const isConnected = ref(false) // 是否已连接
const terminalRef = ref(null)  // DOM 引用
const form = reactive({
  ip: '',       // 测试时填入你的虚拟机或服务器IP
  username: 'root',
  password: ''
})

// --- 核心变量 ---
let term = null      // xterm 实例
let socket = null    // WebSocket 实例
let fitAddon = null  // 自适应插件

// --- 核心方法：建立连接 ---
const connectSsh = () => {
  if (!form.ip || !form.password) {
    alert('请填写完整信息')
    return
  }

  // 1. 初始化 xterm 界面
  initTerm()

  // 2. 建立 WebSocket 连接
  // 注意：地址是 ws://，参数通过 URL 传递
  const wsUrl = `ws://localhost:8080/ws/ssh?ip=${form.ip}&user=${form.username}&pwd=${form.password}`
  socket = new WebSocket(wsUrl)

  // 3. 绑定 WebSocket 事件
  socket.onopen = () => {
    isConnected.value = true
    term.write('\r\n\x1b[32m✨ 连接成功! 开始你的表演...\x1b[0m\r\n')
  }

  socket.onmessage = (event) => {
    // 收到后端发来的 Linux 结果 -> 写到 xterm 屏幕上
    // 注意：后端发来的是字符串，xterm 会自动解析里面的颜色代码
    term.write(event.data)
  }

  socket.onclose = () => {
    term.write('\r\n\x1b[31m💥 连接已断开\x1b[0m\r\n')
    isConnected.value = false
  }

  socket.onerror = () => {
    term.write('\r\n\x1b[31m❌ 连接发生错误\x1b[0m\r\n')
  }
}

// --- 核心方法：初始化 xterm ---
const initTerm = () => {
  // 如果已经初始化过，先销毁
  if (term) term.dispose()

  // 创建 xterm 实例
  term = new Terminal({
    fontSize: 14,
    fontFamily: 'Consolas, monospace', // 编程字体
    cursorBlink: true, // 光标闪烁
    theme: {
      background: '#1e1e1e', // VSCode 风格黑背景
      foreground: '#ffffff'
    }
  })

  // 加载自适应插件
  fitAddon = new FitAddon()
  term.loadAddon(fitAddon)

  // 挂载到 DOM 上
  term.open(terminalRef.value)
  fitAddon.fit() // 自动调整大小

  // --- 关键：监听用户输入 ---
  term.onData((data) => {
    // 用户在键盘按了个键 -> 发给后端
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(data)
    }
  })

  // 聚焦，让用户可以直接打字
  term.focus()
}

// --- 核心方法：断开连接 ---
const disconnect = () => {
  if (socket) socket.close()
  if (term) term.dispose()
  isConnected.value = false
}

// 页面销毁时自动清理
onBeforeUnmount(() => {
  disconnect()
})
</script>

<style scoped>
.ssh-container {
  padding: 20px;
}
.terminal-wrapper {
  margin-top: 20px;
}
.header-bar {
  background: #333;
  color: white;
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
}
.xterm-box {
  height: 600px; /* 终端高度 */
  background: #1e1e1e;
  padding: 10px;
  /* 隐藏滚动条但保留功能 */
  overflow: hidden;
}
</style>