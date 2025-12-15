<template>
  <!-- 仪表板容器 -->
  <div class="dashboard-container">
    <!-- 1. 顶部工具栏 -->
    <div class="toolbar">
      <!-- 左侧工具区：服务器选择器 -->
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
            <!-- 自定义选项显示格式：左侧显示名称，右侧显示IP地址 -->
            <span style="float: left">{{ item.name }}</span>
            <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">
              {{ item.ip }}
            </span>
          </el-option>
        </el-select>
      </div>

      <!-- 右侧工具区：操作按钮 -->
      <div class="right-tools">
        <!-- 清空数据按钮 -->
        <el-button type="danger" plain size="large" class="action-btn" @click="handleClearData">
          <el-icon style="margin-right: 5px"><Delete /></el-icon> 清空数据
        </el-button>
        <!-- 刷新数据按钮，带有加载状态 -->
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
      <!-- CPU 监控卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <!-- CPU图标 -->
            <el-icon class="card-icon" style="background: rgba(52, 152, 219, 0.1); color: #3498db;"><Cpu /></el-icon>
            <h3>CPU 负载监控 (%)</h3>
            <!-- 指标详情按钮 -->
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('cpu')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <!-- 状态标签 -->
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <!-- CPU使用率折线图 -->
          <EchartsLine :data="cpuData" height="300px" color="#3498db" series-name="CPU使用率" unit="%" />
        </div>
      </div>

      <!-- 磁盘监控卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <!-- 磁盘图标 -->
            <el-icon class="card-icon" style="background: rgba(155, 89, 182, 0.1); color: #9b59b6;"><Files /></el-icon>
            <h3>磁盘使用率 (%)</h3>
            <!-- 指标详情按钮 -->
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('disk')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <!-- 存储标签 -->
          <el-tag type="warning" effect="dark" round>Storage</el-tag>
        </div>
        <div class="card-body">
          <!-- 磁盘使用率折线图 -->
          <EchartsLine :data="diskData" height="300px" color="#9b59b6" series-name="磁盘使用率" unit="%" />
        </div>
      </div>

      <!-- 网络监控卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <!-- 网络连接图标 -->
            <el-icon class="card-icon" style="background: rgba(46, 204, 113, 0.1); color: #2ecc71;"><Connection /></el-icon>
            <h3>网络下行速率 (KB/s)</h3>
            <!-- 指标详情按钮 -->
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('net')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <!-- 网络标签 -->
          <el-tag type="info" effect="dark" round>Network</el-tag>
        </div>
        <div class="card-body">
          <!-- 网络下载速率折线图 -->
          <EchartsLine :data="networkData" height="300px" color="#2ecc71" series-name="网络下载速率" unit="KB/s" />
        </div>
      </div>

      <!-- 系统负载监控卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <!-- 里程表图标 -->
            <el-icon class="card-icon" style="background: rgba(230, 126, 34, 0.1); color: #e67e22;"><Odometer /></el-icon>
            <h3>系统平均负载 (Load)</h3>
            <!-- 指标详情按钮 -->
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('load')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <!-- 负载标签 -->
          <el-tag type="warning" effect="dark" round>Load</el-tag>
        </div>
        <div class="card-body">
          <!-- 多系列系统负载折线图（1分钟、5分钟、15分钟） -->
          <EchartsLine :multi-data="loadMultiData" height="300px" unit="" />
        </div>
      </div>

      <!-- 磁盘 I/O 监控卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <!-- 排序图标 -->
            <el-icon class="card-icon" style="background: rgba(22, 160, 133, 0.1); color: #16a085;"><Sort /></el-icon>
            <h3>磁盘 I/O 速率 (KB/s)</h3>
            <!-- 指标详情按钮 -->
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('io')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <!-- I/O标签 -->
          <el-tag type="warning" effect="dark" round>I/O</el-tag>
        </div>
        <div class="card-body">
          <!-- 多系列磁盘IO速率折线图（读取、写入） -->
          <EchartsLine :multi-data="diskIoData" height="300px" unit="KB/s" />
        </div>
      </div>

      <!-- 进程排行卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <!-- 列表图标 -->
            <el-icon class="card-icon" style="background: rgba(231, 76, 60, 0.1); color: #e74c3c;"><List /></el-icon>
            <h3>资源占用 Top 5 进程</h3>
            <!-- 指标详情按钮 -->
            <el-tooltip content="指标详情" placement="top">
              <el-icon class="info-btn" @click="showMetricDetail('process')"><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <!-- 进程标签 -->
          <el-tag type="danger" effect="dark" round>Process</el-tag>
        </div>
        <div class="card-body" style="height: 300px; overflow-y: auto;">
          <!-- 进程表格展示 -->
          <el-table :data="processList" style="width: 100%" size="small" :border="false">
            <!-- PID列 -->
            <el-table-column prop="pid" label="PID" width="70" />
            <!-- 进程名称列，支持文本溢出提示 -->
            <el-table-column prop="name" label="进程名称" show-overflow-tooltip />
            <!-- CPU使用率列，使用进度条展示 -->
            <el-table-column label="CPU %" width="100">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.cpu > 100 ? 100 : row.cpu)" :color="customColorMethod" :stroke-width="8" />
              </template>
            </el-table-column>
            <!-- 内存使用率列，使用进度条展示 -->
            <el-table-column label="内存 %" width="100">
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
        <!-- 指标含义 -->
        <div class="detail-item">
          <h4>💡 指标含义</h4>
          <p>{{ currentDetail.meaning }}</p>
        </div>
        <!-- 核心作用 -->
        <div class="detail-item">
          <h4>🔧 核心作用</h4>
          <p>{{ currentDetail.usage }}</p>
        </div>
        <!-- 计算方式 -->
        <div class="detail-item">
          <h4>🧮 计算方式</h4>
          <p>{{ currentDetail.calc }}</p>
        </div>
        <!-- 健康范围 -->
        <div class="detail-item">
          <h4>✅ 健康范围</h4>
          <p class="range-text">{{ currentDetail.range }}</p>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
// 导入Vue相关API
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
// 导入监控相关的API接口
import { getCpuHistory, getDiskHistory, getNetHistory, getServerBaseInfo, getSystemLoadHistory, getServerList, getDiskIoHistory } from '@/api/monitor.js'
// 导入自定义Echarts折线图组件
import EchartsLine from '@/components/EchartsLine.vue'
// 导入Element Plus消息组件
import { ElMessage } from 'element-plus'
// 导入Element Plus图标组件
import { Refresh, Delete, Cpu, Platform, Files, Connection, Odometer, Sort, List, InfoFilled } from '@element-plus/icons-vue'

// 服务器列表数据
const serverList = ref([])
// 当前选中的服务器IP
const currentServerIp = ref('')
// 加载状态标识
const loading = ref(false)
// 定时器引用
let timer = null
// 数据清除时间戳（用于过滤历史数据）
const clearTime = ref(0)

// 各类监控数据
const cpuData = ref([])           // CPU使用率数据
const diskData = ref([])          // 磁盘使用率数据
const networkData = ref([])       // 网络速率数据
const loadMultiData = ref([])     // 系统负载数据（多系列）
const diskIoData = ref([])        // 磁盘IO数据（多系列）
const processList = ref([])       // 进程列表数据

// 服务器基础信息（使用reactive创建响应式对象）
const serverInfo = reactive({
  osName: '', hostName: '', ip: '', memoryTotal: 0, diskTotal: 0, uptime: 0
})

// 指标详情弹窗相关变量
const detailVisible = ref(false)  // 弹窗显示状态
const currentDetail = ref({})     // 当前显示的指标详情

// 指标字典：存储各类监控指标的详细说明
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

/**
 * 显示指标详情弹窗
 * @param {string} type - 指标类型
 */
const showMetricDetail = (type) => {
  currentDetail.value = metricMap[type]
  detailVisible.value = true
}

/**
 * 初始化函数：获取服务器列表并开始轮询
 */
const init = async () => {
  try {
    // 获取服务器列表
    const res = await getServerList()
    const list = Array.isArray(res) ? res : (res.data || [])
    if (list.length > 0) {
      // 设置服务器列表和默认选中第一个服务器
      serverList.value = list
      currentServerIp.value = list[0].ip
      // 开始轮询更新数据
      startPolling()
    } else {
      // 如果没有服务器则显示警告消息
      ElMessage.warning('暂无服务器')
    }
  } catch (e) {}
}

/**
 * 加载服务器基础信息
 */
const loadBaseInfo = async () => {
  // 如果没有选中服务器则直接返回
  if (!currentServerIp.value) return
  try {
    // 获取指定服务器的基础信息
    const res = await getServerBaseInfo({ ip: currentServerIp.value })
    const info = res.data || res || {}
    // 更新服务器信息
    Object.assign(serverInfo, info)
    serverInfo.ip = currentServerIp.value

    // 处理进程列表数据
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

/**
 * 加载所有图表数据
 */
const loadChartsData = async () => {
  // 如果没有选中服务器则直接返回
  if (!currentServerIp.value) return
  loading.value = true
  try {
    // 并行获取所有监控数据
    const [cpuRes, diskRes, netRes, loadRes, diskIoRes] = await Promise.all([
      getCpuHistory({ ip: currentServerIp.value }),
      getDiskHistory({ ip: currentServerIp.value }),
      getNetHistory({ ip: currentServerIp.value }),
      getSystemLoadHistory({ ip: currentServerIp.value }),
      getDiskIoHistory({ ip: currentServerIp.value })
    ])

    // 处理CPU数据
    cpuData.value = processData(Array.isArray(cpuRes) ? cpuRes : (cpuRes.data || []))
    // 处理磁盘数据
    diskData.value = processData(Array.isArray(diskRes) ? diskRes : (diskRes.data || []))
    // 处理网络数据
    networkData.value = processData(Array.isArray(netRes) ? netRes : (netRes.data || []))

    // 处理系统负载数据（包含1分钟、5分钟、15分钟三个系列）
    const loadMap = loadRes.data || loadRes || {}
    loadMultiData.value = [
      { name: '1分钟', data: processData(loadMap.load1 || []), color: '#e67e22' },
      { name: '5分钟', data: processData(loadMap.load5 || []), color: '#f1c40f' },
      { name: '15分钟', data: processData(loadMap.load15 || []), color: '#2ecc71' }
    ]

    // 处理磁盘IO数据（包含读取和写入两个系列）
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
 * @param {number} percentage - 百分比数值
 * @returns {string} 颜色值
 */
const customColorMethod = (percentage) => {
  if (percentage < 50) return '#2ecc71'  // 低负载绿色
  if (percentage < 80) return '#e67e22'  // 中等负载橙色
  return '#e74c3c'  // 高负载红色
}

/**
 * 处理监控数据：过滤和格式化时间
 * @param {Array} list - 原始数据列表
 * @returns {Array} 处理后的数据列表
 */
const processData = (list) => {
  return list
      // 过滤掉清除时间之前的数据
      .filter(item => new Date(item.time).getTime() > clearTime.value)
      // 格式化时间为本地时间字符串
      .map(item => ({
        time: new Date(item.time).toLocaleTimeString('zh-CN', { hour12: false }),
        value: item.value
      }))
}

/**
 * 格式化运行时间
 * @param {number} seconds - 秒数
 * @returns {string} 格式化后的时间字符串
 */
const formatUptime = (seconds) => {
  if (!seconds || seconds < 0) return 'Unknown'
  const days = Math.floor(seconds / 86400)      // 计算天数
  const hours = Math.floor((seconds % 86400) / 3600)  // 计算小时数
  const minutes = Math.floor((seconds % 3600) / 60)   // 计算分钟数
  return `${days}天 ${hours}小时 ${minutes}分`
}

/**
 * 开始轮询更新数据
 */
const startPolling = () => {
  // 加载基础信息和图表数据
  loadBaseInfo()
  loadChartsData()
  // 清除之前的定时器
  if (timer) clearInterval(timer)
  // 设置新的定时器，每5秒刷新一次数据
  timer = setInterval(loadChartsData, 5000)
}

/**
 * 处理服务器切换事件
 */
const handleServerChange = () => {
  // 重置清除时间和其他数据
  clearTime.value = 0
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  diskIoData.value = []
  // 重新开始轮询
  startPolling()
}

/**
 * 处理清空数据事件
 */
const handleClearData = () => {
  // 设置新的清除时间戳
  clearTime.value = Date.now()
  // 清空所有图表数据
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadMultiData.value = []
  diskIoData.value = []
  // 显示成功消息
  ElMessage.success('已清空历史记录，将重新绘制')
  // 重新加载数据
  loadChartsData()
}

// 组件挂载时执行初始化
onMounted(() => init())
// 组件卸载前清除定时器
onBeforeUnmount(() => { if (timer) clearInterval(timer) })
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

/* 左侧工具区样式 */
.left-tools {
  display: flex;
  align-items: center;
}

/* 标签样式 */
.label {
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-right: 12px;
}

/* 右侧工具区样式 */
.right-tools {
  display: flex;
  gap: 10px;
}

/* 操作按钮样式 */
.action-btn {
  font-weight: 600;
}

/* 信息卡片样式 */
.info-card {
  display: flex;
  justify-content: space-around;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 20px;
}

/* 单个信息项样式 */
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

/* 操作系统文本样式 */
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

/* 图表网格布局 */
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
  min-height: 400px;
  display: flex;
  flex-direction: column;
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

/* 卡片主体样式 */
.card-body {
  flex: 1;
}

/* 指标详情按钮样式 */
.info-btn {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  margin-left: 8px;
  transition: color 0.2s;
}

/* 指标详情按钮悬停效果 */
.info-btn:hover {
  color: var(--el-color-primary);
}

/* 详情项样式 */
.detail-item {
  margin-bottom: 20px;
}

/* 详情项标题样式 */
.detail-item h4 {
  margin: 0 0 8px 0;
  color: var(--el-color-primary);
  font-size: 15px;
  border-left: 3px solid var(--el-color-primary);
  padding-left: 8px;
}

/* 详情项段落样式 */
.detail-item p {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  font-size: 14px;
}

/* 范围文本样式 */
.range-text {
  color: #67c23a;
  font-weight: bold;
  background: var(--el-fill-color-light);
  padding: 5px 10px;
  border-radius: 4px;
  display: inline-block;
}

/* 响应式设计：小屏幕设备 */
@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>