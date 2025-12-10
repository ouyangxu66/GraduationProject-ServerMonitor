<template>
  <div class="dashboard-container">
    <!-- 顶部工具栏 -->
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

      <el-button type="primary" size="large" class="refresh-btn" @click="loadData" :loading="loading">
        <el-icon style="margin-right: 5px"><Refresh /></el-icon> 刷新数据
      </el-button>
    </div>

    <!-- 数据面板 -->
    <div class="chart-grid">
      <!-- CPU 卡片 -->
      <div class="flat-card">
        <div class="card-header">
          <div class="title-area">
            <el-icon class="card-icon" color="#3498db"><Cpu /></el-icon>
            <h3>CPU 负载监控</h3>
          </div>
          <el-tag type="success" effect="dark" round>Live</el-tag>
        </div>
        <div class="card-body">
          <EchartsLine :data="cpuData" height="380px" />
        </div>
      </div>

      <!-- 预留内存卡片位置 -->
      <!-- <div class="flat-card">...</div> -->
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getCpuHistory, getServerList } from '@/api/monitor.js'
import EchartsLine from '@/components/EchartsLine.vue'
import { ElMessage } from 'element-plus'
import { Refresh, Cpu } from '@element-plus/icons-vue'

const serverList = ref([])
const currentServerIp = ref('')
const cpuData = ref([])
const loading = ref(false)
let timer = null

// 1. 初始化
const init = async () => {
  try {
    const res = await getServerList()
    // 兼容逻辑：request.js 可能返回数组，也可能返回 {data: []}
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

// 2. 加载数据
const loadData = async () => {
  if (!currentServerIp.value) return
  loading.value = true
  try {
    // 传递 IP 参数
    const res = await getCpuHistory({ ip: currentServerIp.value })
    const rawData = Array.isArray(res) ? res : (res.data || [])

    // 数据清洗：格式化时间
    cpuData.value = rawData.map(item => ({
      ...item,
      // 假设 item.time 是 "2025-12-09T08:37:10Z"
      // 转为 "16:37:10"
      time: new Date(item.time).toLocaleTimeString('zh-CN', { hour12: false })
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// 3. 轮询
const startPolling = () => {
  loadData()
  if (timer) clearInterval(timer)
  timer = setInterval(loadData, 5000)
}

const handleServerChange = () => {
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

/* 顶部工具栏适配暗黑模式 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  background: var(--el-bg-color); /* 自动适配 */
  padding: 20px;
  border: 1px solid var(--el-border-color-light); /* 自动适配 */
  border-radius: 12px;
  transition: all 0.3s;
}

.left-tools {
  display: flex;
  align-items: center;
}

.label {
  font-weight: 700;
  color: var(--el-text-color-primary); /* 自动适配 */
  margin-right: 12px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr; /* 单列布局，后续可改为 repeat(2, 1fr) */
  gap: 24px;
}

/* 扁平卡片适配 */
.flat-card {
  background: var(--el-bg-color); /* 自动适配 */
  border: 2px solid var(--el-border-color-light);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.title-area {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-icon {
  font-size: 24px;
  background: var(--el-fill-color-light);
  padding: 8px;
  border-radius: 8px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--el-text-color-primary);
}
</style>