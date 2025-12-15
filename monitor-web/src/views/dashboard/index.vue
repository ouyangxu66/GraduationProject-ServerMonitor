<template>
  <div class="dashboard-container">
    <!-- 1. 顶部工具栏 -->
    <div class="toolbar">
      <div class="left-tools">
        <span class="label">当前服务器：</span>
        <!-- 服务器选择下拉框 -->
        <el-select
            v-model="currentServerIp"
            placeholder="请选择服务器"
            size="large"
            style="width: 240px"
            @change="handleServerChange"
        >
          <!-- 遍历服务器列表生成选项 -->
          <el-option
              v-for="item in serverList"
              :key="item.id"
              :label="item.name"
              :value="item.ip"
          >
            <span style="float: left">{{ item.name }}</span>
            <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">
              {{ item.ip }}
            </span>
          </el-option>
        </el-select>
      </div>

      <div class="right-tools">
        <!-- 清空数据按钮 -->
        <el-button type="danger" plain size="large" class="action-btn" @click="handleClearData">
          <el-icon style="margin-right: 5px"><Delete /></el-icon> 清空数据
        </el-button>
        <!-- 刷新数据按钮 -->
        <el-button type="primary" size="large" class="action-btn" @click="loadChartsData" :loading="loading">
          <el-icon style="margin-right: 5px"><Refresh /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 2. 基础信息卡片 -->
    <div class="info-card flat-card" v-if="serverInfo.ip">
      <!-- 操作系统信息 -->
      <div class="info-item os-item">
        <div class="info-label">操作系统</div>
        <div class="info-value hover-expand">
          <el-icon style="margin-right: 5px; flex-shrink: 0;"><Platform /></el-icon>
          <span class="os-text">{{ serverInfo.osName || 'Unknown' }}</span>
        </div>
      </div>

      <!-- 主机名称 -->
      <div class="info-item">
        <div class="info-label">主机名称</div>
        <div class="info-value">{{ serverInfo.hostName || 'Unknown' }}</div>
      </div>
      <!-- IP地址 -->
      <div class="info-item">
        <div class="info-label">IP 地址</div>
        <div class="info-value">{{ serverInfo.ip }}</div>
      </div>
      <!-- 内存总量 -->
      <div class="info-item">
        <div class="info-label">内存总量</div>
        <div class="info-value highlight">{{ serverInfo.memoryTotal }} GB</div>
      </div>
      <!-- 磁盘总量 -->
      <div class="info-item">
        <div class="info-label">磁盘总量</div>
        <div class="info-value highlight">{{ serverInfo.diskTotal }} GB</div>
      </div>
      <!-- 持续运行时间 -->
      <div class="info-item">
        <div class="info-label">持续运行</div>
        <div class="info-value highlight">{{ formatUptime(serverInfo.uptime) }}</div>
      </div>
    </div>

    <!-- 3. 数据图表区域 -->
    <div class="chart-grid">
      <!-- 第一行左：CPU 监控 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(52, 152, 219, 0.1); color: #3498db;"><Cpu /></el-icon>
            <h3>CPU 负载监控 (%)</h3>
          </div>
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <!-- CPU使用率折线图 -->
          <EchartsLine
              :data="cpuData"
              height="300px"
              color="#3498db"
              series-name="CPU使用率"
              unit="%"
          />
        </div>
      </div>

      <!-- 第一行右：磁盘监控 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(155, 89, 182, 0.1); color: #9b59b6;"><Files /></el-icon>
            <h3>磁盘使用率 (%)</h3>
          </div>
          <el-tag type="warning" effect="dark" round>Storage</el-tag>
        </div>
        <div class="card-body">
          <!-- 磁盘使用率折线图 -->
          <EchartsLine
              :data="diskData"
              height="300px"
              color="#9b59b6"
              series-name="磁盘使用率"
              unit="%"
          />
        </div>
      </div>

      <!-- 第二行左：网络监控 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(46, 204, 113, 0.1); color: #2ecc71;"><Connection /></el-icon>
            <h3>网络下行速率 (KB/s)</h3>
          </div>
          <el-tag type="info" effect="dark" round>Network</el-tag>
        </div>
        <div class="card-body">
          <!-- 网络下载速率折线图 -->
          <EchartsLine
              :data="networkData"
              height="300px"
              color="#2ecc71"
              series-name="网络下载速率"
              unit="KB/s"
          />
        </div>
      </div>

      <!-- 第二行右：系统负载 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(230, 126, 34, 0.1); color: #e67e22;"><Odometer /></el-icon>
            <h3>系统平均负载 (Load Avg)</h3>
          </div>
          <el-tag type="warning" effect="dark" round>Load</el-tag>
        </div>
        <div class="card-body">
          <!-- 系统负载折线图（包含1分钟、5分钟、15分钟三个指标） -->
          <EchartsLine
              :multi-data="loadMultiData"
              height="300px"
              unit=""
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount, reactive} from 'vue'
import {
  getCpuHistory,
  getDiskHistory,
  getNetHistory,
  getServerBaseInfo,
  getSystemLoadHistory,
  getServerList
} from '@/api/monitor.js'
import EchartsLine from '@/components/EchartsLine.vue'
import {ElMessage} from 'element-plus'
import {Refresh, Delete, Cpu, Platform, Files, Connection, Odometer} from '@element-plus/icons-vue'

// 服务器列表数据
const serverList = ref([])
// 当前选中的服务器IP
const currentServerIp = ref('')
// 加载状态标识
const loading = ref(false)
// 定时器引用
let timer = null
// 数据清除时间点（用于过滤历史数据）
const clearTime = ref(0)

// 各项监控数据
const cpuData = ref([])
const diskData = ref([])
const networkData = ref([])
const loadMultiData = ref([])

/**
 * 格式化运行时间
 * @param {number} seconds - 运行秒数
 * @returns {string} 格式化后的时间字符串
 */
const formatUptime = (seconds) => {
  if (!seconds || seconds < 0) return 'Unknown'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}天 ${hours}小时 ${minutes}分`
}

// 服务器基础信息对象
const serverInfo = reactive({
  osName: '', hostName: '', ip: '', memoryTotal: 0, diskTotal: 0, uptime: 0
})

/**
 * 初始化函数 - 获取服务器列表并设置默认选中项
 */
const init = async () => {
  try {
    const res = await getServerList()
    const list = Array.isArray(res) ? res : (res.data || [])
    if (list.length > 0) {
      serverList.value = list
      currentServerIp.value = list[0].ip
      startPolling()
    } else {
      ElMessage.warning('暂无服务器')
    }
  } catch (e) {
    console.error(e)
  }
}

/**
 * 加载服务器基础信息
 */
const loadBaseInfo = async () => {
  if (!currentServerIp.value) return
  try {
    const res = await getServerBaseInfo({ip: currentServerIp.value})
    const info = res.data || res || {}
    Object.assign(serverInfo, info)
    serverInfo.ip = currentServerIp.value
  } catch (e) {
  }
}

/**
 * 加载所有图表数据
 */
const loadChartsData = async () => {
  if (!currentServerIp.value) return
  loading.value = true
  try {
    // 并行获取各项监控数据
    const [cpuRes, diskRes, netRes, loadRes] = await Promise.all([
      getCpuHistory({ip: currentServerIp.value}),
      getDiskHistory({ip: currentServerIp.value}),
      getNetHistory({ip: currentServerIp.value}),
      getSystemLoadHistory({ip: currentServerIp.value})
    ])

    // 处理各项数据并更新到响应式变量
    cpuData.value = processData(cpuRes)
    diskData.value = processData(diskRes)
    networkData.value = processData(netRes)

    // 处理系统负载数据（包含1分钟、5分钟、15分钟三个指标）
    const loadMap = loadRes.data || loadRes || {}
    const data1 = processData(loadMap.load1 || [])
    const data5 = processData(loadMap.load5 || [])
    const data15 = processData(loadMap.load15 || [])

    loadMultiData.value = [
      {name: '1分钟', data: data1, color: '#e67e22'},
      {name: '5分钟', data: data5, color: '#f1c40f'},
      {name: '15分钟', data: data15, color: '#2ecc71'}
    ]
  } catch (e) {
    console.error('图表加载失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 处理从API获取的数据
 * @param {Array|Object} res - 原始数据
 * @returns {Array} 处理后的数据数组
 */
const processData = (res) => {
  const list = Array.isArray(res) ? res : (res.data || [])
  return list
      .filter(item => new Date(item.time).getTime() > clearTime.value)
      .map(item => ({
        time: new Date(item.time).toLocaleTimeString('zh-CN', {hour12: false}),
        value: item.value
      }))
}

/**
 * 开始轮询数据
 */
const startPolling = () => {
  loadBaseInfo()
  loadChartsData()
  if (timer) clearInterval(timer)
  timer = setInterval(loadChartsData, 5000)
}

/**
 * 服务器切换处理函数
 */
const handleServerChange = () => {
  clearTime.value = 0
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  startPolling()
}

/**
 * 清空历史数据处理函数
 */
const handleClearData = () => {
  clearTime.value = Date.now()
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  ElMessage.success('已清空历史记录，将重新绘制')
  loadChartsData()
}

// 组件挂载时初始化
onMounted(() => init())
// 组件卸载前清理定时器
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* 仪表板容器 */
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  background: var(--el-bg-color);
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  transition: all 0.3s;
}

/* 工具栏左侧 */
.left-tools {
  display: flex;
  align-items: center;
}

/* 标签文本样式 */
.label {
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-right: 12px;
}

/* 工具栏右侧 */
.right-tools {
  display: flex;
  gap: 10px;
}

/* 操作按钮样式 */
.action-btn {
  font-weight: 600;
}

/* 信息卡片布局 */
.info-card {
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 20px;
}

/* 单个信息项 */
.info-item {
  text-align: center;
  min-width: 120px;
}

/* 操作系统信息项特殊样式 */
.os-item {
  max-width: 300px;
}

/* 信息标签样式 */
.info-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

/* 信息值样式 */
.info-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

/* 操作系统文本样式（支持悬停展开） */
.os-text {
  display: inline-block;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  transition: all 0.3s ease;
  border-radius: 4px;
}

/* 悬停展开效果 */
.hover-expand:hover .os-text {
  max-width: none;
  white-space: normal;
  overflow: visible;
  background-color: var(--el-fill-color);
  padding: 0 5px;
  position: relative;
  z-index: 10;
}

/* 高亮信息值样式 */
.info-value.highlight {
  color: var(--el-color-primary);
}

/* 图表网格布局（2列） */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

/* 卡片样式 */
.flat-card {
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s;
}

/* 卡片头部样式 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

/* 标题区域样式 */
.title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 卡片图标样式 */
.card-icon {
  font-size: 20px;
  padding: 8px;
  border-radius: 8px;
}

/* 卡片标题样式 */
.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: var(--el-text-color-primary);
}

/* 响应式设计 - 小屏幕下变为单列布局 */
@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>