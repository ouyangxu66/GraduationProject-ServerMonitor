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
        <!-- 🟢 新增：清空数据按钮 -->
        <el-button type="danger" plain size="large" class="action-btn" @click="handleClearData">
          <el-icon style="margin-right: 5px"><Delete /></el-icon> 清空数据
        </el-button>
        <el-button type="primary" size="large" class="action-btn" @click="loadData" :loading="loading">
          <el-icon style="margin-right: 5px"><Refresh /></el-icon> 刷新数据
        </el-button>
      </div>
    </div>

    <!-- 🟢 新增：服务器基础信息卡片 -->
    <div class="info-card flat-card" v-if="serverInfo.ip">
      <div class="info-item">
        <div class="info-label">操作系统</div>
        <div class="info-value">
          <el-icon><Platform /></el-icon> {{ serverInfo.osName || 'Unknown' }}
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

    <!-- 数据图表区域 -->
    <div class="chart-grid">
      <!-- Chart 1: CPU -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(52, 152, 219, 0.1); color: #3498db;"><Cpu /></el-icon>
            <h3>CPU 负载监控</h3>
          </div>
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="cpuData" height="300px" color="#3498db" />
        </div>
      </div>

      <!-- 🟢 Chart 2: 磁盘使用率 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(155, 89, 182, 0.1); color: #9b59b6;"><Files /></el-icon>
            <h3>磁盘使用率</h3>
          </div>
          <el-tag type="warning" effect="dark" round>Storage</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="diskData" height="300px" color="#9b59b6" />
        </div>
      </div>

      <!-- 🟢 Chart 3: 网络速率 -->
      <div class="flat-card full-width">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" style="background: rgba(46, 204, 113, 0.1); color: #2ecc71;"><Connection /></el-icon>
            <h3>网络下行速率 (KB/s)</h3>
          </div>
          <el-tag type="info" effect="dark" round>Network</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="networkData" height="300px" color="#2ecc71" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue'
import { getCpuHistory, getServerList } from '@/api/monitor.js'
import EchartsLine from '@/components/EchartsLine.vue'
import { ElMessage } from 'element-plus'
import { Refresh, Delete, Cpu, Platform, Files, Connection } from '@element-plus/icons-vue'

const serverList = ref([])
const currentServerIp = ref('')
const loading = ref(false)
let timer = null

// --- 数据状态定义 ---
const cpuData = ref([])
const diskData = ref([])
const networkData = ref([])

// 服务器基础信息
const serverInfo = reactive({
  osName: '',
  hostName: '',
  ip: '',
  memoryTotal: 0,
  diskTotal: 0
})

// 1. 初始化加载
const init = async () => {
  try {
    const res = await getServerList()
    const list = Array.isArray(res) ? res : (res.data || [])

    if (list.length > 0) {
      serverList.value = list
      currentServerIp.value = list[0].ip
      startPolling()
    } else {
      ElMessage.warning('暂无服务器，请先在服务器管理中添加')
    }
  } catch (e) {
    console.error(e)
  }
}

// 2. 加载核心数据
const loadData = async () => {
  if (!currentServerIp.value) return
  loading.value = true
  try {
    const res = await getCpuHistory({ ip: currentServerIp.value })

    // 兼容处理
    const rawData = Array.isArray(res) ? res : (res.data || [])

    if (rawData.length > 0) {
      // 🟢 1. 修复图表数据映射
      // 后端返回的是 "value" 字段，不是 "cpuLoad"
      cpuData.value = mapData(rawData, 'value')

      // ⚠️ 注意：目前的接口 /cpu-history 只返回了 CPU 数据
      // 所以磁盘和网络图表暂时没有数据，为了防止报错，我们先置空
      // 后续需要在后端写专门的 getDiskHistory 和 getNetHistory 接口
      diskData.value = []
      networkData.value = []

      // 🟢 2. 尝试获取基础信息 (如果有的话)
      // 由于目前的 time-series 接口只返回 time 和 value，这里可能拿不到 info
      // 建议后续单独写一个 /api/server/info 接口来获取这些静态信息
      // 这里先做一个简单的容错处理
      const latest = rawData[rawData.length - 1]
      // 只有当字段存在时才更新，避免把 'Unknown' 覆盖成 undefined
      if (latest.osName) serverInfo.osName = latest.osName
      if (latest.hostName) serverInfo.hostName = latest.hostName
      if (latest.memoryTotal) serverInfo.memoryTotal = latest.memoryTotal
      if (latest.diskTotal) serverInfo.diskTotal = latest.diskTotal
      serverInfo.ip = currentServerIp.value
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 辅助函数：映射数据
const mapData = (list, key) => {
  return list.map(item => ({
    time: new Date(item.createTime || item.time).toLocaleTimeString('zh-CN', { hour12: false }),
    value: item[key] // 动态获取 value (cpuLoad, diskUsage 等)
  }))
}

// 3. 🟢 需求2：清空数据
const handleClearData = () => {
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  // 也可以选择是否重置基础信息
  // Object.keys(serverInfo).forEach(k => serverInfo[k] = '')
  ElMessage.success('当前视图数据已清空')
  // 重新加载一次最新数据
  loadData()
}

// 4. 轮询逻辑
const startPolling = () => {
  loadData()
  if (timer) clearInterval(timer)
  timer = setInterval(loadData, 5000)
}

const handleServerChange = () => {
  // 切换服务器时先清空旧图表，防止数据混淆
  cpuData.value = []
  diskData.value = []
  networkData.value = []
  loadData()
}

onMounted(() => {
  init()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
}

/* --- 工具栏 --- */
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

/* --- 🟢 基础信息卡片 --- */
.info-card {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 20px;
}

.info-item {
  text-align: center;
  min-width: 120px;
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

.info-value.highlight {
  color: var(--el-color-primary);
}

/* --- 图表网格 --- */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 两列布局 */
  gap: 24px;
}

.full-width {
  grid-column: span 2; /* 网络监控占满一行 */
}

/* --- 扁平卡片通用样式 --- */
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

/* 响应式调整 */
@media (max-width: 1000px) {
  .chart-grid {
    grid-template-columns: 1fr; /* 小屏幕单列 */
  }
  .full-width {
    grid-column: span 1;
  }
}
</style>