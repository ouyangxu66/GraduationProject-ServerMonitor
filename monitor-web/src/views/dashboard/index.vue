<template>
  <!-- 仪表板容器 -->
  <div class="dashboard-container">

    <!-- 1. 顶部工具栏 -->
    <div class="toolbar">
      <!-- 左侧工具区 -->
      <div class="left-tools">
        <span class="label">当前服务器：</span>
        <el-select
            v-model="currentServerIp"
            placeholder="请选择服务器"
            size="large"
            style="width: 240px"
            @change="handleServerChange"
        >
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

        <!-- 时间范围选择 -->
        <span class="label" style="margin-left: 20px">时间范围：</span>
        <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            size="large"
            :shortcuts="shortcuts"
            @change="handleTimeChange"
        />
        <el-button v-if="timeRange && timeRange.length" type="warning" plain size="large" style="margin-left: 10px" @click="handleResetTime">
          返回实时
        </el-button>
      </div>

      <!-- 右侧工具区 -->
      <div class="right-tools">
        <el-button type="danger" plain size="large" class="action-btn" @click="handleClearData">
          <el-icon style="margin-right: 5px"><Delete /></el-icon> 清空数据
        </el-button>
        <el-button type="primary" size="large" class="action-btn" @click="loadChartsData" :loading="loading">
          <el-icon style="margin-right: 5px"><Refresh /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 2. 基础信息卡片 (3列 x 2行) -->
    <div class="info-card flat-card" v-if="serverInfo.ip">
      <!-- Row 1 -->
      <div class="info-item os-item">
        <div class="info-label">操作系统</div>
        <div class="info-value">
          <el-icon style="margin-right: 4px; flex-shrink: 0;"><Platform /></el-icon>
          <!-- 直接显示全称，允许换行 -->
          <span class="os-text">{{ serverInfo.osName || 'Unknown' }}</span>
        </div>
      </div>
      <div class="info-item">
        <div class="info-label">主机名称</div>
        <div class="info-value">{{ serverInfo.hostName || 'Unknown' }}</div>
      </div>
      <div class="info-item">
        <div class="info-label">IP 地址</div>
        <div class="info-value">{{ serverInfo.ip }}</div>
      </div>

      <!-- Row 2 -->
      <div class="info-item">
        <div class="info-label">内存总量</div>
        <div class="info-value highlight">{{ serverInfo.memoryTotal }} GB</div>
      </div>
      <div class="info-item">
        <div class="info-label">磁盘总量</div>
        <div class="info-value highlight">{{ serverInfo.diskTotal }} GB</div>
      </div>
      <div class="info-item">
        <div class="info-label">持续运行</div>
        <div class="info-value highlight">{{ formatUptime(serverInfo.uptime) }}</div>
      </div>
    </div>

    <!-- 3. 数据图表区域 (Grid 布局) -->
    <div class="chart-grid">

      <!-- 第一行左: CPU 使用率 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(52, 152, 219, 0.1); color: #3498db;"><Cpu /></el-icon>
            <h3>CPU 负载监控 (%)</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('cpu')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="cpuData" height="300px" color="#3498db" series-name="CPU使用率" unit="%" />
        </div>
      </div>

      <!-- 第一行右: CPU 温度 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(231, 76, 60, 0.1); color: #e74c3c;"><Sunny /></el-icon>
            <h3>CPU 温度 (°C)</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('cpu')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="danger" effect="dark" round>Temp</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="tempData" height="300px" color="#e74c3c" series-name="温度" unit="°C" />
        </div>
      </div>

      <!-- 第二行左: 磁盘使用率 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(155, 89, 182, 0.1); color: #9b59b6;"><Files /></el-icon>
            <h3>磁盘使用率 (%)</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('disk')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="warning" effect="dark" round>Storage</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="diskData" height="300px" color="#9b59b6" series-name="磁盘使用率" unit="%" />
        </div>
      </div>

      <!-- 第二行右: 磁盘 I/O -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(22, 160, 133, 0.1); color: #16a085;"><Sort /></el-icon>
            <h3>磁盘 I/O 速率 (KB/s)</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('io')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="warning" effect="dark" round>I/O</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :multi-data="diskIoData" height="300px" unit="KB/s" />
        </div>
      </div>

      <!-- 🟢 第三行左: 网络监控 (调整位置) -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(46, 204, 113, 0.1); color: #2ecc71;"><Connection /></el-icon>
            <h3>网络下行速率 (KB/s)</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('net')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="info" effect="dark" round>Network</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="networkData" height="300px" color="#2ecc71" series-name="网络下载速率" unit="KB/s" />
        </div>
      </div>

      <!-- 🟢 第三行右: 系统负载 (调整位置) -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(230, 126, 34, 0.1); color: #e67e22;"><Odometer /></el-icon>
            <h3>系统平均负载 (Load)</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('load')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="warning" effect="dark" round>Load</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :multi-data="loadMultiData" height="300px" unit="" />
        </div>
      </div>

      <!-- 第四行: 进程排行 (占据整行) -->
      <div class="flat-card full-width">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(231, 76, 60, 0.1); color: #e74c3c;"><List /></el-icon>
            <h3>资源占用 Top 5 进程</h3>
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('process')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <el-tag type="danger" effect="dark" round>Process</el-tag>
        </div>
        <div class="card-body" style="height: 300px; overflow-y: auto;">
          <el-table :data="processList" style="width: 100%" size="small" :border="false">
            <el-table-column prop="pid" label="PID" width="70" />
            <el-table-column prop="name" label="进程名称" show-overflow-tooltip />
            <el-table-column label="CPU %" width="150">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.cpu > 100 ? 100 : row.cpu)" :color="customColorMethod" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="内存 %" width="150">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.mem > 100 ? 100 : row.mem)" color="#e67e22" :stroke-width="8" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

    </div>

    <!-- 4. 指标详情弹窗 -->
    <el-dialog
        v-model="detailVisible"
        :title="currentDetail.title"
        width="500px"
        align-center
        class="metric-dialog"
    >
      <div class="detail-content">
        <div class="detail-item">
          <h4>💡 指标含义</h4>
          <p>{{ currentDetail.meaning }}</p>
        </div>
        <div class="detail-item">
          <h4>🔧 核心作用</h4>
          <p>{{ currentDetail.usage }}</p>
        </div>
        <div class="detail-item">
          <h4>🧮 计算方式</h4>
          <p>{{ currentDetail.calc }}</p>
        </div>
        <div class="detail-item">
          <h4>✅ 健康范围</h4>
          <p class="range-text">{{ currentDetail.range }}</p>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
import { useRoute } from 'vue-router' // 🟢 引入 useRoute
import { getCpuHistory, getDiskHistory, getNetHistory, getServerBaseInfo, getSystemLoadHistory, getServerList, getDiskIoHistory, getTempHistory } from '@/api/monitor.js'
import EchartsLine from '@/components/EchartsLine.vue'
import { ElMessage } from 'element-plus'
import { Refresh, Delete, Cpu, Platform, Files, Connection, Odometer, Sort, List, InfoFilled, Sunny } from '@element-plus/icons-vue'

const route = useRoute() // 🟢 获取路由信息
const serverList = ref([])
const currentServerIp = ref('')
const loading = ref(false)
let timer = null
const clearTime = ref(0)

// 数据状态
const cpuData = ref([])
const diskData = ref([])
const networkData = ref([])
const loadMultiData = ref([])
const diskIoData = ref([])
const processList = ref([])
const tempData = ref([])
const timeRange = ref([])

const serverInfo = reactive({
  osName: '', hostName: '', ip: '', memoryTotal: 0, diskTotal: 0, uptime: 0
})

const detailVisible = ref(false)
const currentDetail = ref({})

// 指标字典
const metricMap = {
  cpu: {
    title: 'CPU 负载监控',
    meaning: '指一段时间内 CPU 正在处理和等待处理的任务占用时间比率。',
    usage: '衡量服务器计算能力是否饱和，是否存在死循环或高密集计算任务。',
    calc: '基于 System Cpu Load Ticks 差值计算：(Total - Idle) / Total * 100%。',
    range: '正常 < 70%，繁忙 70%-90%，告警 > 90%。'
  },
  disk: {
    title: '磁盘使用率',
    meaning: '磁盘已存储数据占总容量的百分比。',
    usage: '防止磁盘空间耗尽导致日志无法写入、数据库宕机。',
    calc: '(Used Bytes / Total Bytes) * 100%。',
    range: '建议保持在 85% 以下，超过 90% 需立即清理。'
  },
  net: {
    title: '网络下行速率',
    meaning: '服务器单位时间内接收数据的数据量 (Inbound Traffic)。',
    usage: '监控带宽占用，发现流量攻击(DDoS)或大文件下载行为。',
    calc: '采集两次间隔 1秒 的接收字节数差值除以 1024 (KB)。',
    range: '取决于服务器带宽带宽 (如 5Mbps 带宽约 600KB/s)。'
  },
  load: {
    title: '系统平均负载 (Load Average)',
    meaning: '特定时间间隔内（1/5/15分钟）运行队列中的平均进程数。',
    usage: '反映系统繁忙程度，比 CPU 使用率更真实地反映系统压力（包含 IO 等待）。',
    calc: '操作系统内核计算的运行队列长度。',
    range: '理想值 < CPU 核心数 * 0.7。例如 4核 CPU，Load < 2.8 为健康。'
  },
  io: {
    title: '磁盘 I/O 速率',
    meaning: '磁盘每秒进行读写操作的数据量。',
    usage: '判断磁盘读写性能瓶颈，数据库卡顿时通常重点关注此指标。',
    calc: '采集两次间隔 1秒 的读/写字节数差值。',
    range: '视磁盘类型而定 (SSD > HDD)。持续高吞吐可能导致系统响应变慢。'
  },
  process: {
    title: 'Top 5 进程',
    meaning: '当前系统中 CPU 或内存占用最高的 5 个进程。',
    usage: '快速定位导致服务器卡顿的“元凶”进程。',
    calc: '按 CPU 占用率降序排序截取前 5 名。',
    range: '单一进程 CPU 长期 > 90% 可能是异常死循环或挖矿病毒。'
  }
}

const showMetricDetail = (type) => {
  currentDetail.value = metricMap[type]
  detailVisible.value = true
}

// 初始化
const init = async () => {
  try {
    const res = await getServerList()
    const list = Array.isArray(res) ? res : (res.data || [])
    if (list.length > 0) {
      serverList.value = list
      // 🟢 优化：优先使用路由传参的IP，否则使用列表第一个
      if (route.query.ip) {
        // 检查该IP是否在列表中
        const exists = list.find(s => s.ip === route.query.ip)
        currentServerIp.value = exists ? exists.ip : list[0].ip
      } else {
        currentServerIp.value = list[0].ip
      }
      startPolling()
    } else {
      ElMessage.warning('暂无服务器')
    }
  } catch (e) {}
}

// 加载基础信息
const loadBaseInfo = async () => {
  if (!currentServerIp.value) return
  try {
    const res = await getServerBaseInfo({ ip: currentServerIp.value })
    const info = res.data || res || {}
    Object.assign(serverInfo, info)
    serverInfo.ip = currentServerIp.value

    // 解析进程列表
    if (info.top_processes || info.topProcesses) {
      try {
        const raw = info.top_processes || info.topProcesses
        processList.value = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (err) {
        processList.value = []
      }
    }
  } catch (e) {}
}

// 加载图表数据
const loadChartsData = async () => {
  if (!currentServerIp.value) return
  loading.value = true

  let start = '-1h'
  let end = 'now()'

  if (timeRange.value && timeRange.value.length === 2) {
    start = new Date(timeRange.value[0]).toISOString()
    end = new Date(timeRange.value[1]).toISOString()
  }

  try {
    const params = {ip:currentServerIp.value, start, end}

    // 🟢 Promise.all 并发获取所有数据
    const [cpuRes, tempRes, diskRes, netRes, loadRes, diskIoRes] = await Promise.all([
      getCpuHistory(params),
      getTempHistory(params),
      getDiskHistory(params),
      getNetHistory(params),
      getSystemLoadHistory({ip: currentServerIp.value}), // 负载一般只看实时
      getDiskIoHistory(params)
    ])

    // 单线图表
    cpuData.value = processData(Array.isArray(cpuRes) ? cpuRes : (cpuRes.data || []))
    diskData.value = processData(Array.isArray(diskRes) ? diskRes : (diskRes.data || []))
    networkData.value = processData(Array.isArray(netRes) ? netRes : (netRes.data || []))
    tempData.value = processData(Array.isArray(tempRes) ? tempRes : (tempRes.data || []))

    // 多线图表 - 系统负载
    const loadMap = loadRes.data || loadRes || {}
    loadMultiData.value = [
      { name: '1分钟', data: processData(loadMap.load1 || []), color: '#e67e22' },
      { name: '5分钟', data: processData(loadMap.load5 || []), color: '#f1c40f' },
      { name: '15分钟', data: processData(loadMap.load15 || []), color: '#2ecc71' }
    ]

    // 多线图表 - 磁盘 IO
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

const customColorMethod = (percentage) => {
  if (percentage < 50) return '#2ecc71'
  if (percentage < 80) return '#e67e22'
  return '#e74c3c'
}

const processData = (list) => {
  return list
      .filter(item => new Date(item.time).getTime() > clearTime.value)
      .map(item => ({
        time: new Date(item.time).toLocaleTimeString('zh-CN', { hour12: false }),
        value: item.value
      }))
}

const formatUptime = (seconds) => {
  if (!seconds || seconds < 0) return 'Unknown'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}天 ${hours}小时 ${minutes}分`
}

const startPolling = () => {
  loadBaseInfo()
  loadChartsData()
  if (timer) clearInterval(timer)
  timer = setInterval(loadChartsData, 5000)
}

const handleServerChange = () => {
  clearTime.value = 0
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  diskIoData.value = []
  tempData.value = []
  startPolling()
}

const handleClearData = () => {
  clearTime.value = Date.now()
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  diskIoData.value = []
  tempData.value = []
  ElMessage.success('已清空历史记录，将重新绘制')
  loadChartsData()
}

const handleTimeChange = () => {
  if (timer) clearInterval(timer)
  loadChartsData()
  ElMessage.success('已切换至历史回溯模式')
}

const handleResetTime = () => {
  timeRange.value = []
  startPolling()
  ElMessage.success('已切换回实时监控模式')
}

const shortcuts = [
  {
    text: '最近1小时',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000);
      return [start, end]
    }
  },
  {
    text: '最近24小时',
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24);
      return [start, end]
    }
  },
]

onMounted(() => init())
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* 仪表板容器样式 */
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* 顶部工具栏样式 */
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

.left-tools {
  display: flex;
  align-items: center;
}

.label {
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-right: 12px;
}

.right-tools {
  display: flex;
  gap: 10px;
}

.action-btn {
  font-weight: 600;
}

/* 基础信息卡片 (紧凑型表格布局) */
.info-card {
  display: grid !important;
  grid-template-columns: repeat(3, 1fr);
  gap: 0;
  padding: 0;
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 12px 5px;
  border-right: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-bg-color);
}

.info-item:nth-child(3n) {
  border-right: none;
}

.info-item:nth-last-child(-n+3) {
  border-bottom: none;
}

.os-item {
  grid-column: span 1;
}

.os-text {
  white-space: normal;
  word-break: break-word;
  text-align: center;
  line-height: 1.2;
  font-size: 13px;
}

.info-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.info-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  width: 100%;
}

.info-value.highlight {
  color: var(--el-color-primary);
}

/* 图表网格布局 */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

/* 🟢 修改：Process 占满全宽 */
.full-width {
  grid-column: span 2;
}

/* 卡片样式 */
.flat-card {
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s;
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-icon {
  font-size: 20px;
  padding: 8px;
  border-radius: 8px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: var(--el-text-color-primary);
}

.card-body {
  flex: 1;
}

.info-btn {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  margin-left: 8px;
  transition: color 0.2s;
}

.info-btn:hover {
  color: var(--el-color-primary);
}

.detail-item {
  margin-bottom: 20px;
}

.detail-item h4 {
  margin: 0 0 8px 0;
  color: var(--el-color-primary);
  font-size: 15px;
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}

.detail-item p {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  font-size: 14px;
}

.range-text {
  color: #67c23a;
  font-weight: bold;
  background: var(--el-fill-color-light);
  padding: 5px 10px;
  border-radius: 4px;
  display: inline-block;
}

@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: span 1;
  }
}
</style>