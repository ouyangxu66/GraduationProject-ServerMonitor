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
          <!-- 遍历服务器列表，生成选项 -->
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
      <!-- 主机名信息 -->
      <div class="info-item">
        <div class="info-label">主机名称</div>
        <div class="info-value">{{ serverInfo.hostName || 'Unknown' }}</div>
      </div>
      <!-- IP地址信息 -->
      <div class="info-item">
        <div class="info-label">IP 地址</div>
        <div class="info-value">{{ serverInfo.ip }}</div>
      </div>
      <!-- 内存总量信息 -->
      <div class="info-item">
        <div class="info-label">内存总量</div>
        <div class="info-value highlight">{{ serverInfo.memoryTotal }} GB</div>
      </div>
      <!-- 磁盘总量信息 -->
      <div class="info-item">
        <div class="info-label">磁盘总量</div>
        <div class="info-value highlight">{{ serverInfo.diskTotal }} GB</div>
      </div>
      <!-- 系统运行时间信息 -->
      <div class="info-item">
        <div class="info-label">持续运行</div>
        <div class="info-value highlight">{{ formatUptime(serverInfo.uptime) }}</div>
      </div>
    </div>

    <!-- 3. 数据图表区域 (2列布局) -->
    <div class="chart-grid">

      <!-- 第一行左：CPU使用率图表 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(52, 152, 219, 0.1); color: #3498db;"><Cpu /></el-icon>
            <h3>CPU 负载监控 (%)</h3>
          </div>
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <!-- 使用EchartsLine组件显示CPU使用率折线图 -->
          <EchartsLine :data="cpuData" height="300px" color="#3498db" series-name="CPU使用率" unit="%" />
        </div>
      </div>

      <!-- 第一行右：磁盘使用率图表 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(155, 89, 182, 0.1); color: #9b59b6;"><Files /></el-icon>
            <h3>磁盘使用率 (%)</h3>
          </div>
          <el-tag type="warning" effect="dark" round>Storage</el-tag>
        </div>
        <div class="card-body">
          <!-- 使用EchartsLine组件显示磁盘使用率折线图 -->
          <EchartsLine :data="diskData" height="300px" color="#9b59b6" series-name="磁盘使用率" unit="%" />
        </div>
      </div>

      <!-- 第二行左：网络下行速率图表 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(46, 204, 113, 0.1); color: #2ecc71;"><Connection /></el-icon>
            <h3>网络下行速率 (KB/s)</h3>
          </div>
          <el-tag type="info" effect="dark" round>Net</el-tag>
        </div>
        <div class="card-body">
          <!-- 使用EchartsLine组件显示网络下行速率折线图 -->
          <EchartsLine :data="networkData" height="300px" color="#2ecc71" series-name="网络下载" unit="KB/s" />
        </div>
      </div>

      <!-- 第二行右：系统负载图表 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(230, 126, 34, 0.1); color: #e67e22;"><Odometer /></el-icon>
            <h3>系统平均负载 (Load)</h3>
          </div>
          <el-tag type="warning" effect="dark" round>Load</el-tag>
        </div>
        <div class="card-body">
          <!-- 使用EchartsLine组件显示多条系统负载折线图（1分钟、5分钟、15分钟） -->
          <EchartsLine :multi-data="loadMultiData" height="300px" unit="" />
        </div>
      </div>

      <!-- 第三行左：磁盘 I/O 速率图表 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(22, 160, 133, 0.1); color: #16a085;"><Sort /></el-icon>
            <h3>磁盘 I/O 速率 (KB/s)</h3>
          </div>
          <el-tag type="warning" effect="dark" round>I/O</el-tag>
        </div>
        <div class="card-body">
          <!-- 使用EchartsLine组件显示磁盘I/O读写速率折线图 -->
          <EchartsLine :multi-data="diskIoData" height="300px" unit="KB/s" />
        </div>
      </div>

      <!-- 第三行右：进程排行 Top 5 表格 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(231, 76, 60, 0.1); color: #e74c3c;"><List /></el-icon>
            <h3>资源占用 Top 5 进程</h3>
          </div>
          <el-tag type="danger" effect="dark" round>Process</el-tag>
        </div>
        <div class="card-body" style="height: 300px; overflow-y: auto;">
          <!-- 使用表格展示资源占用前5的进程信息 -->
          <el-table :data="processList" style="width: 100%" size="small" :border="false">
            <!-- 进程ID列 -->
            <el-table-column prop="pid" label="PID" width="70" />
            <!-- 进程名称列 -->
            <el-table-column prop="name" label="进程名称" show-overflow-tooltip />
            <!-- CPU使用率列，以进度条形式展示 -->
            <el-table-column label="CPU %" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.cpu > 100 ? 100 : row.cpu)" :color="customColorMethod" :stroke-width="10" />
              </template>
            </el-table-column>
            <!-- 内存使用率列，以进度条形式展示 -->
            <el-table-column label="内存 %" width="120">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.mem > 100 ? 100 : row.mem)" color="#e67e22" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
// 引入Vue相关功能
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
// 引入监控相关的API接口函数
import { getCpuHistory, getDiskHistory, getNetHistory, getServerBaseInfo, getSystemLoadHistory, getServerList, getDiskIoHistory } from '@/api/monitor.js'
// 引入EchartsLine图表组件
import EchartsLine from '@/components/EchartsLine.vue'
// 引入Element Plus的消息提示组件
import { ElMessage } from 'element-plus'
// 引入Element Plus图标组件
import { Refresh, Delete, Cpu, Platform, Files, Connection, Odometer, Sort, List } from '@element-plus/icons-vue'

// 定义响应式变量
const serverList = ref([])             // 服务器列表
const currentServerIp = ref('')        // 当前选中的服务器IP
const loading = ref(false)             // 加载状态标识
let timer = null                       // 定时器引用
const clearTime = ref(0)               // 数据清空时间戳

// 图表数据状态
const cpuData = ref([])                // CPU使用率数据
const diskData = ref([])               // 磁盘使用率数据
const networkData = ref([])            // 网络速率数据
const loadMultiData = ref([])          // 系统负载数据（多条线）
const diskIoData = ref([])             // 磁盘I/O数据（多条线）
const processList = ref([])            // 进程列表数据

// 服务器基础信息对象
const serverInfo = reactive({
  osName: '', hostName: '', ip: '', memoryTotal: 0, diskTotal: 0, uptime: 0
})

/**
 * 初始化函数，获取服务器列表并设置默认选中项
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
  } catch (e) {}
}

/**
 * 加载服务器基础信息
 */
const loadBaseInfo = async () => {
  if (!currentServerIp.value) return
  try {
    const res = await getServerBaseInfo({ ip: currentServerIp.value })
    const info = res.data || res || {}
    Object.assign(serverInfo, info)
    serverInfo.ip = currentServerIp.value

    // 解析进程列表数据
    if (info.top_processes || info.topProcesses) {
      try {
        const raw = info.top_processes || info.topProcesses
        // 如果是字符串则解析，如果是对象则直接用
        processList.value = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (err) {
        processList.value = []
      }
    }
  } catch (e) {}
}

/**
 * 加载所有图表数据
 */
const loadChartsData = async () => {
  if (!currentServerIp.value) return
  loading.value = true
  try {
    // 并行请求所有图表数据
    const [cpuRes, diskRes, netRes, loadRes, diskIoRes] = await Promise.all([
      getCpuHistory({ ip: currentServerIp.value }),
      getDiskHistory({ ip: currentServerIp.value }),
      getNetHistory({ ip: currentServerIp.value }),
      getSystemLoadHistory({ ip: currentServerIp.value }),
      getDiskIoHistory({ ip: currentServerIp.value })
    ])

    // 处理单线图表数据
    cpuData.value = processData(Array.isArray(cpuRes) ? cpuRes : (cpuRes.data || []))
    diskData.value = processData(Array.isArray(diskRes) ? diskRes : (diskRes.data || []))
    networkData.value = processData(Array.isArray(netRes) ? netRes : (netRes.data || []))

    // 处理多线图表数据: 系统负载（1分钟、5分钟、15分钟）
    const loadMap = loadRes.data || loadRes || {}
    loadMultiData.value = [
      { name: '1分钟', data: processData(loadMap.load1 || []), color: '#e67e22' },
      { name: '5分钟', data: processData(loadMap.load5 || []), color: '#f1c40f' },
      { name: '15分钟', data: processData(loadMap.load15 || []), color: '#2ecc71' }
    ]

    // 处理多线图表数据: 磁盘I/O（读取、写入）
    const ioMap = diskIoRes.data || diskIoRes || {}
    diskIoData.value = [
      { name: '读取', data: processData(ioMap.read || []), color: '#8e44ad' },
      { name: '写入', data: processData(ioMap.write || []), color: '#16a085' }
    ]

  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

/**
 * 自定义进度条颜色方法
 * @param {number} percentage - 百分比值
 * @returns {string} 颜色值
 */
const customColorMethod = (percentage) => {
  if (percentage < 50) return '#2ecc71'
  if (percentage < 80) return '#e67e22'
  return '#e74c3c'
}

/**
 * 处理数据，过滤掉清空时间之前的数据，并格式化时间显示
 * @param {Array} list - 原始数据列表
 * @returns {Array} 处理后的数据列表
 */
const processData = (list) => {
  return list
      .filter(item => new Date(item.time).getTime() > clearTime.value)
      .map(item => ({
        time: new Date(item.time).toLocaleTimeString('zh-CN', { hour12: false }),
        value: item.value
      }))
}

/**
 * 格式化系统运行时间
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时间字符串
 */
const formatUptime = (seconds) => {
  if (!seconds || seconds < 0) return 'Unknown'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}天 ${hours}小时 ${minutes}分`
}

/**
 * 启动轮询机制，定时刷新数据
 */
const startPolling = () => {
  loadBaseInfo()
  loadChartsData()
  if (timer) clearInterval(timer)
  timer = setInterval(loadChartsData, 5000)
}

/**
 * 处理服务器切换事件
 */
const handleServerChange = () => {
  clearTime.value = 0
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  diskIoData.value = []
  startPolling()
}

/**
 * 处理清空数据事件
 */
const handleClearData = () => {
  clearTime.value = Date.now()
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  diskIoData.value = []
  ElMessage.success('数据已清空')
  loadChartsData()
}

// 组件挂载时初始化
onMounted(() => init())
// 组件卸载前清理定时器
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
/* 容器样式 */
.dashboard-container { max-width: 1400px; margin: 0 auto; }

/* 工具栏样式 */
.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; background: var(--el-bg-color); padding: 20px;
  border: 1px solid var(--el-border-color-light); border-radius: 12px;
  transition: all 0.3s;
}
.left-tools {
  display: flex;
  align-items: center;
}
.label {
  ont-weight: 700;
  color: var(--el-text-color-primary);
  margin-right: 12px; }
.right-tools { display: flex; gap: 10px;
}
.action-btn { font-weight: 600; }

/* 信息卡片样式 */
.info-card {
  display: flex; justify-content: space-around; align-items: flex-start;
  margin-bottom: 24px; flex-wrap: wrap; gap: 20px;
}
.info-item {
  text-align: center;
  min-width: 120px;
}
.os-item { max-width: 300px; }
.info-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.info-value { font-size: 18px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px; }
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
.hover-expand:hover .os-text {
  max-width: none;
  white-space: normal;
  overflow: visible;
  background-color: var(--el-fill-color);
  padding: 0 5px;
  position: relative;
  z-index: 10;
}
.info-value.highlight { color: var(--el-color-primary); }

/* 图表网格布局 */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 2列布局 */
  gap: 24px;
}

/* 卡片样式 */
.flat-card {
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s;
  /* 确保高度一致 */
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

/* 卡片头部样式 */
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid var(--el-border-color-lighter);
}
.title-area { display: flex; align-items: center; gap: 10px; }
.card-icon { font-size: 20px; padding: 8px; border-radius: 8px; }
.card-header h3 { margin: 0; font-size: 16px; font-weight: 800; color: var(--el-text-color-primary); }
.card-body { flex: 1; }

/* 响应式设计，小屏幕时改为单列布局 */
@media (max-width: 1000px) {
  .chart-grid { grid-template-columns: 1fr; }
}
</style>