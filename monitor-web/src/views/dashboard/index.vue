<template>
  <div class="dashboard-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <span class="label">选择服务器：</span>
      <el-select v-model="currentServerIp" placeholder="请选择服务器" size="large" style="width: 240px" @change="handleServerChange">
        <el-option
            v-for="item in serverList"
            :key="item.id"
            :label="item.name + ' (' + item.ip + ')'"
            :value="item.ip"
        />
      </el-select>
      <el-button type="primary" size="large" class="refresh-btn" @click="loadData">
        刷新数据
      </el-button>
    </div>

    <!-- 数据面板 -->
    <div class="chart-grid">
      <div class="flat-card">
        <div class="card-header">
          <h3>CPU 实时监控</h3>
          <span class="tag">Live</span>
        </div>
        <div class="card-body">
          <EchartsLine :data="cpuData" height="350px" />
        </div>
      </div>

      <!-- 这里可以加内存图表等 -->
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount} from 'vue'
import {getCpuHistory, getServerList} from '@/api/monitor.js'
import EchartsLine from '@/components/EchartsLine.vue'
import {ElMessage} from 'element-plus'

const serverList = ref([])
const currentServerIp = ref('')
const cpuData = ref([])
let timer = null

// 1. 初始化：先加载服务器列表，默认选中第一个
const init = async () => {
  try {
    const res = await getServerList()
    if (res.data && res.data.length > 0) {
      serverList.value = res.data
      currentServerIp.value = res.data[0].ip // 默认选中第一个
      startPolling()
    } else {
      ElMessage.warning('暂无服务器，请先去添加')
    }
  } catch (e) {
    console.error(e)
  }
}

// 2. 获取监控数据
const loadData = async () => {
  if (!currentServerIp.value) return
  try {
    // 假设后端接口支持 ?ip=xxx 参数
    // 如果后端还没改，这里传参数不会报错，只是后端可能忽略
    const res = await getCpuHistory({ip: currentServerIp.value})
    if (res.data) {
      cpuData.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

// 3. 轮询
const startPolling = () => {
  loadData()
  timer = setInterval(loadData, 5000)
}

const handleServerChange = () => {
  // 切换服务器，立即刷新
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
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  background: #fff;
  padding: 20px;
  border: 1px solid #ecf0f1;
  border-radius: 12px;
}

.label {
  font-weight: 700;
  color: #2c3e50;
  margin-right: 12px;
}

.refresh-btn {
  margin-left: auto;
  background-color: #2c3e50;
  border: none;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

/* 扁平卡片风格 */
.flat-card {
  background: #ffffff;
  border: 2px solid #ecf0f1; /* 粗边框 */
  border-radius: 16px;
  padding: 24px;
  transition: transform 0.2s;
}

.flat-card:hover {
  border-color: #bdc3c7; /* 悬停边框加深 */
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #2c3e50;
}

.tag {
  background-color: #e74c3c;
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
</style>