<template>
  <div class="dashboard-container">
    <h1>🚀 服务器实时监控</h1>

    <!-- ECharts 容器，必须给高度 -->
    <div ref="chartRef" style="width: 100%; height: 400px; margin-top: 20px;"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getCpuHistory } from '@/api/monitor' // 引入刚才封装的 API

const chartRef = ref(null) // 对应 HTML 里的 div
let myChart = null
let timer = null

// 初始化图表的方法
const initChart = (data) => {
  // 1. 准备 X 轴 (时间) 和 Y 轴 (数值) 数据
  // 后端返回的是List<Map>: [{"time": "...", "value": 12.5}, ...]
  const xData = data.map(item => item.time.substring(11, 19)) // 只截取 HH:mm:ss
  const yData = data.map(item => item.value)

  // 2. ECharts 配置项
  const option = {
    title: { text: 'CPU 使用率趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value', min: 0, max: 100 }, // CPU 0-100%
    series: [
      {
        data: yData,
        type: 'line',
        smooth: true, // 平滑曲线
        areaStyle: {}, // 面积图效果
        itemStyle: { color: '#409EFF' }
      }
    ]
  }

  // 3. 渲染
  if (!myChart) {
    myChart = echarts.init(chartRef.value)
  }
  myChart.setOption(option)
}

// 获取数据的方法
const loadData = async () => {
  try {
    const res = await getCpuHistory()
    // axios 默认把后端数据放在 res.data 里
    if (res.data) {
      initChart(res.data)
    }
  } catch (error) {
    console.error("获取数据失败:", error)
  }
}

// 页面加载完成后
onMounted(() => {
  loadData() // 先加载一次

  // 开启定时刷新：每 5 秒刷新一次图表
  timer = setInterval(loadData, 5000)
})

// 页面销毁时
onUnmounted(() => {
  // 清理定时器，防止内存泄漏
  if (timer) clearInterval(timer)
  if (myChart) myChart.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
</style>