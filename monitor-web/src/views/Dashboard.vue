<template>
  <div class="dashboard-container">
    <h1>🚀 服务器实时监控</h1>

    <!-- 👇 修改点 1：把 @click 绑定到一个具体的函数上，而不是写 $router.push -->
    <div style="margin-bottom: 20px;">
      <el-button type="primary" @click="goToSsh">👉 去连接服务器终端</el-button>
    </div>

    <div ref="chartRef" style="width: 100%; height: 400px; margin-top: 20px;"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
// 👇 修改点 2：引入 useRouter
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getCpuHistory } from '@/api/monitor'

// 👇 修改点 3：初始化路由实例
const router = useRouter()

const chartRef = ref(null)
let myChart = null
let timer = null

// 👇 修改点 4：定义跳转函数
const goToSsh = () => {
  router.push('/ssh')
}

// ... 下面的代码保持不变 ...
const initChart = (data) => {
  const xData = data.map(item => item.time.substring(11, 19))
  const yData = data.map(item => item.value)

  const option = {
    title: { text: 'CPU 使用率趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [
      {
        data: yData,
        type: 'line',
        smooth: true,
        areaStyle: {},
        itemStyle: { color: '#409EFF' }
      }
    ]
  }

  if (!myChart) {
    myChart = echarts.init(chartRef.value)
  }
  myChart.setOption(option)
}

const loadData = async () => {
  try {
    const res = await getCpuHistory()
    if (res.data) {
      initChart(res.data)
    }
  } catch (error) {
    console.error("获取数据失败:", error)
  }
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (myChart) myChart.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
</style>