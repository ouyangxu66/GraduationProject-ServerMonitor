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
    <div v-else class="terminal-container" ref="terminalContainerRef">
      <div class="terminal-header">
        <div class="status-box">
          <span class="status-dot"></span>
          <span class="status-text">{{ form.username }}@{{ form.host }}</span>
        </div>
        <div class="header-actions">
          <el-button type="primary" size="small" plain @click="toggleFilePanel">目录</el-button>
          <el-button type="danger" size="small" plain @click="disconnect">断开连接</el-button>
        </div>
      </div>

      <div class="terminal-body">
        <div id="xterm" class="xterm-box"></div>

        <div
          v-if="showFilePanel"
          class="file-panel"
          :style="{ width: filePanelWidth + 'px' }"
        >
          <!-- 拖拽条 -->
          <div class="file-panel-resizer" @mousedown="startResize"></div>

          <div class="file-panel-header">
            <!-- 同一行：返回 + 路径 + 刷新 + 上传 -->
            <div class="toolbar-row">
              <el-button
                class="back-btn"
                size="small"
                circle
                :disabled="currentPath === '/'"
                @click="goParent"
                title="返回上级"
              >
                <span class="back-icon">←</span>
              </el-button>

              <el-input v-model="currentPath" size="small" class="path-input" @keyup.enter="refreshList" />

              <el-button size="small" @click="refreshList" :loading="fileLoading">刷新</el-button>

              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                :before-upload="() => false"
                :on-change="handleUploadChange"
              >
                <el-button size="small" type="success" :loading="uploading">上传</el-button>
              </el-upload>
            </div>
          </div>

          <div class="file-list">
            <div v-if="fileLoading" class="hint">加载中...</div>
            <div v-else-if="fileList.length === 0" class="hint">空目录</div>
            <div v-else>
              <div
                v-for="item in fileList"
                :key="item.path"
                class="file-row"
                @dblclick="item.type === 'DIR' ? enterDir(item.path) : null"
              >
                <div class="file-name">
                  <span class="file-type">{{ item.type === 'DIR' ? '📁' : '📄' }}</span>
                  <span class="name-text" :title="item.name">{{ item.name }}</span>
                </div>
                <div class="file-actions">
                  <el-button v-if="item.type === 'DIR'" size="small" @click="enterDir(item.path)">进入</el-button>
                  <el-button v-else size="small" type="primary" @click="downloadFile(item)">下载</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
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
import { getServerSshConfig } from '@/api/monitor'
import { getServerSftpTicket, sftpList, sftpUpload, sftpDownload } from '@/api/sftp'

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

// ticket 模式：用于“从服务器列表一键进入终端并自动连接”
const sshTicket = ref('')

const loadSshConfigByServerId = async () => {
  const serverId = route.query.serverId
  if (!serverId) return false

  try {
    const resp = await getServerSshConfig(serverId)
    // request.js 可能直接返回 data 或 {data:...}，兼容一下
    const data = resp?.data || resp
    if (!data) return false

    form.host = data.host
    form.port = data.port || 22
    form.username = data.username
    form.authType = data.authPreferred || 'password'
    sshTicket.value = data.sshTicket || ''
    return true
  } catch (e) {
    // 这里需要 catch：避免自动连接流程阻塞页面渲染；并给用户明确反馈
    console.error('loadSshConfigByServerId failed', e)
    ElMessage.error('获取终端连接信息失败，请稍后重试')
    return false
  }
}

let term = null
let socket = null
let fitAddon = null
let resizeHandler = null
const terminalContainerRef = ref(null)

// 目录面板宽度（可拖拽）
const filePanelWidth = ref(360)
let resizing = false
let resizeStartX = 0
let resizeStartWidth = 360

const clamp = (n, min, max) => Math.min(Math.max(n, min), max)

const startResize = (e) => {
  // 仅左键
  if (e.button !== 0) return
  resizing = true
  resizeStartX = e.clientX
  resizeStartWidth = filePanelWidth.value

  document.addEventListener('mousemove', onResizing)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

const onResizing = (e) => {
  if (!resizing) return
  const containerWidth = terminalContainerRef.value?.clientWidth || 0
  const maxWidth = Math.max(320, Math.floor(containerWidth / 2)) // 最大为黑窗口一半
  const minWidth = 280

  // resizer 在面板左侧：鼠标往左拖 => 面板变宽
  const delta = resizeStartX - e.clientX
  filePanelWidth.value = clamp(resizeStartWidth + delta, minWidth, maxWidth)
}

const stopResize = () => {
  resizing = false
  document.removeEventListener('mousemove', onResizing)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
}

onBeforeUnmount(() => {
  stopResize()
})

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

  // ticket 模式：只要有 ticket，就不需要前端再填密码/私钥
  if (sshTicket.value) return null

  if (form.authType === 'password') {
    if (!form.password) return '请填写密码'
    return null
  }

  // authType=publicKey
  if (!form.privateKey) return '请粘贴私钥内容'
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
    // 发送 connect 包
    const connectData = {
      operate: 'connect'
    }

    // ticket 模式优先：不下发明文凭证
    if (sshTicket.value) {
      connectData.ticket = sshTicket.value
    } else {
      // 兼容手动模式
      connectData.authType = form.authType
      connectData.host = form.host
      connectData.port = Number(form.port)
      connectData.username = form.username

      if (form.authType === 'password') {
        connectData.password = form.password
      } else {
        connectData.privateKey = form.privateKey
        if (form.passphrase) connectData.passphrase = form.passphrase
      }
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
  } catch (e) {
    // 兼容：后端正常输出可能包含以 '{' 开头但不是 JSON 的 ANSI/终端片段。
    // 这里做轻量 debug，避免吞异常导致排查困难，同时不打扰用户。
    console.debug('tryParseEvent: non-json payload', e)
    return { isEvent: false, event: null }
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
  if (socket) {
    // WebSocket.close() 本身不会抛出业务可处理异常；让浏览器按标准处理即可
    socket.close()
  }
  socket = null
  connected.value = false
}

onBeforeUnmount(() => {
  disconnect()
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})

onMounted(async () => {
  // 1) serverId 模式：自动拉取配置并自动连接
  const loaded = await loadSshConfigByServerId()
  if (loaded && sshTicket.value) {
    initSsh()
    return
  }

  // 2) 兼容老模式：如果从 query 带了 ip+pwd，也可以自动连接
  if (form.host && form.password) initSsh()
})

const showFilePanel = ref(false)
const currentPath = ref('/')
const fileList = ref([])
const fileLoading = ref(false)
const uploading = ref(false)

const getServerId = () => {
  const serverId = route.query.serverId
  return serverId ? Number(serverId) : null
}

const toggleFilePanel = async () => {
  showFilePanel.value = !showFilePanel.value
  if (showFilePanel.value) {
    await refreshList()
  }
}

const fetchOneTimeSftpTicket = async () => {
  const serverId = getServerId()
  if (!serverId) {
    throw new Error('缺少 serverId，无法使用目录功能')
  }
  const data = await getServerSftpTicket(serverId)
  return data?.sftpTicket
}

const refreshList = async () => {
  try {
    fileLoading.value = true
    const ticket = await fetchOneTimeSftpTicket()
    const list = await sftpList({ ticket, path: currentPath.value })
    fileList.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.error('refreshList failed', e)
    ElMessage.error(e?.message || '获取目录失败')
  } finally {
    fileLoading.value = false
  }
}

const enterDir = async (path) => {
  currentPath.value = path
  await refreshList()
}

const goParent = async () => {
  if (currentPath.value === '/') return
  const p = currentPath.value.replace(/\\/g, '/')
  const idx = p.lastIndexOf('/')
  currentPath.value = idx <= 0 ? '/' : p.substring(0, idx)
  await refreshList()
}

const handleUploadChange = async (uploadFile) => {
  try {
    const raw = uploadFile?.raw
    if (!raw) return

    uploading.value = true
    const ticket = await fetchOneTimeSftpTicket()

    const fd = new FormData()
    fd.append('ticket', ticket)
    fd.append('targetDir', currentPath.value)
    fd.append('overwrite', 'false')
    fd.append('file', raw)

    await sftpUpload(fd)
    ElMessage.success('上传成功')
    await refreshList()
  } catch (e) {
    console.error('upload failed', e)
    ElMessage.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const downloadFile = async (item) => {
  try {
    const ticket = await fetchOneTimeSftpTicket()
    const blob = await sftpDownload({ ticket, path: item.path })

    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = item.name || 'download'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    console.error('download failed', e)
    ElMessage.error(e?.message || '下载失败')
  }
}
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

/* 终端容器：限制最大宽度，避免在大屏上被拉伸导致布局异常 */
.terminal-container {
  width: 100%;
  max-width: 1400px;
  height: 85vh;
  background-color: #1e1e1e;
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.terminal-body {
  flex: 1;
  display: flex;
  min-height: 0; /* 关键：让子元素的 overflow 生效 */
  min-width: 0;
}

/* 左侧终端区域：在 flex 中允许收缩，避免被右侧面板挤出/覆盖 */
.xterm-box {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
  padding: 8px;
  background-color: #1e1e1e;
  overflow: hidden; /* 终端自身滚动由 xterm 控制 */
}

/* 右侧文件面板：强制深色背景，避免被 Element Plus 默认白底影响 */
.file-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background-color: #ffffff;
  color: #303133;
  border-left: 1px solid #dcdfe6;
  position: relative;
}

/* 可拖拽调整宽度的拖拽条（在面板左侧） */
.file-panel-resizer {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  background: transparent;
}

.file-panel-resizer:hover {
  background: rgba(0, 0, 0, 0.06);
}

/* header 白底 */
.file-panel-header {
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
  background-color: #ffffff;
}

/* 工具栏同一行 */
.toolbar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-icon {
  font-size: 14px;
  line-height: 1;
}

/* 输入框占满剩余空间 */
.path-input {
  flex: 1;
}

/* 列表区域白底 + 可滚动 */
.file-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
  background-color: #ffffff;
}

.file-row:hover {
  background: #f5f7fa;
}

.hint {
  padding: 10px;
  color: #909399;
}

/* 黑窗口顶部 root@host 文字变为白色 */
.status-text {
  color: #ffffff;
  font-size: 14px;
  font-family: monospace;
}
</style>

