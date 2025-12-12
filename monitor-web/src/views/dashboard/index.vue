<template>
  <div class="dashboard-container">
    <!-- 1. 顶部工具栏 -->
    <div class="toolbar">
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
      </div>

      <div class="right-tools">
        <el-button type="danger" plain size="large" class="action-btn" @click="handleClearData">
          <el-icon style="margin-right: 5px"><Delete /></el-icon> 清空数据
        </el-button>
        <el-button type="primary" size="large" class="action-btn" @click="loadChartsData" :loading="loading">
          <el-icon style="margin-right: 5px"><Refresh /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 2. 基础信息卡片 -->
    <div class="info-card flat-card" v-if="serverInfo.ip">
      <div class="info-item os-item">
        <div class="info-label">操作系统</div>
        <div class="info-value hover-expand">
          <el-icon style="margin-right: 5px; flex-shrink: 0;"><Platform /></el-icon>
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
      <div class="info-item">
        <div class="info-label">内存总量</div>
        <div class="info-value highlight">{{ serverInfo.memoryTotal }} GB</div>
      </div>
      <div class="info-item">
        <div class="info-label">磁盘总量</div>
        <div class="info-value highlight">{{ serverInfo.diskTotal }} GB</div>
      </div>
    </div>

    <!-- 3. 数据图表区域 -->
    <div class="chart-grid">
      <!-- CPU 监控 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(52, 152, 219, 0.1); color: #3498db;"><Cpu /></el-icon>
            <h3>CPU 负载监控 (%)</h3>
          </div>
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine
              :data="cpuData"
              height="300px"
              color="#3498db"
              series-name="CPU使用率"
              unit="%"
          />
        </div>
      </div>

      <!-- 磁盘监控 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(155, 89, 182, 0.1); color: #9b59b6;"><Files /></el-icon>
            <h3>磁盘使用率 (%)</h3>
          </div>
          <el-tag type="warning" effect="dark" round>Storage</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine
              :data="diskData"
              height="300px"
              color="#9b59b6"
              series-name="磁盘使用率"
              unit="%"
          />
        </div>
      </div>

      <!-- 网络监控 -->
      <div class="flat-card full-width">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(46, 204, 113, 0.1); color: #2ecc71;"><Connection /></el-icon>
            <h3>网络下行速率 (KB/s)</h3>
          </div>
          <el-tag type="info" effect="dark" round>Network</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine
              :data="networkData"
              height="300px"
              color="#2ecc71"
              series-name="网络下载速率"
              unit="KB/s"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount, reactive} from 'vue'
import {getCpuHistory, getDiskHistory, getNetHistory, getServerBaseInfo, getServerList} from '@/api/monitor.js'
import EchartsLine from '@/components/EchartsLine.vue'
import {ElMessage} from 'element-plus'
import {Refresh, Delete, Cpu, Platform, Files, Connection} from '@element-plus/icons-vue'

const serverList = ref([])
const currentServerIp = ref('')
const loading = ref(false)
let timer = null
const clearTime = ref(0)

const cpuData = ref([])
const diskData = ref([])
const networkData = ref([])

// 基础信息
const serverInfo = reactive({
  osName: '', hostName: '', ip: '', memoryTotal: 0, diskTotal: 0
})

// 初始化
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

// 加载基础信息
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

// 加载图表数据
const loadChartsData = async () => {
  if (!currentServerIp.value) return
  loading.value = true
  try {
    const [cpuRes, diskRes, netRes] = await Promise.all([
      getCpuHistory({ip: currentServerIp.value}),
      getDiskHistory({ip: currentServerIp.value}),
      getNetHistory({ip: currentServerIp.value})
    ])
    cpuData.value = processData(cpuRes)
    diskData.value = processData(diskRes)
    networkData.value = processData(netRes)
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 处理数据
const processData = (res) => {
  const list = Array.isArray(res) ? res : (res.data || [])
  return list
      .filter(item => new Date(item.time).getTime() > clearTime.value)
      .map(item => ({
        time: new Date(item.time).toLocaleTimeString('zh-CN', {hour12: false}),
        value: item.value
      }))
}

// 开始轮询
const startPolling = () => {
  loadBaseInfo()
  loadChartsData()
  if (timer) clearInterval(timer)
  timer = setInterval(loadChartsData, 5000)
}

// 切换服务器
const handleServerChange = () => {
  clearTime.value = 0
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  startPolling()
}

// 清空数据
const handleClearData = () => {
  clearTime.value = Date.now()
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  ElMessage.success('已清空历史记录，将重新绘制')
  loadChartsData()
}

// 页面加载时初始化
onMounted(() => init())
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
}

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

.info-card {
  display: flex;
  justify-content: space-around;
  align-items: flex-start; /* 顶部对齐，防止展开时高度跳动 */
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 20px;
}

.info-item {
  text-align: center;
  min-width: 120px;
}

/* 🟢 需求1 实现：操作系统栏 */
.os-item {
  max-width: 300px; /* 限制最大宽度 */
}

.info-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.info-value {
  font-size: 18px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

/* 🟢 交互逻辑：默认截断，悬停展开 */
.os-text {
  display: inline-block;
  max-width: 200px; /* 默认最大宽度 */
  white-space: nowrap; /* 不换行 */
  overflow: hidden; /* 隐藏溢出 */
  text-overflow: ellipsis; /* 显示省略号 */
  vertical-align: bottom;
  transition: all 0.3s ease; /* 平滑动画 */
  border-radius: 4px;
}

.hover-expand:hover .os-text {
  max-width: none; /* 取消宽度限制 */
  white-space: normal; /* 允许换行 */
  overflow: visible; /* 显示全部 */
  background-color: var(--el-fill-color); /* 加个底色突出显示 */
  padding: 0 5px;
  position: relative;
  z-index: 10;
}

.info-value.highlight {
  color: var(--el-color-primary);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.full-width {
  grid-column: span 2;
}

/* 扁平卡片通用 */
.flat-card {
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s;
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

@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .full-width {
    grid-column: span 1;
  }
}
</style>