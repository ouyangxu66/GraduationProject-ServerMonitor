<template>
  <div ref="chartRef" :style="{ height: height, width: '100%' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 接收后端返回的数据列表
  data: {
    type: Array,
    default: () => []
  },
  height: {
    type: String,
    default: '350px'
  }
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = {
    // 扁平化风格：纯色、无阴影、清晰的网格
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#ecf0f1',
      borderWidth: 2,
      textStyle: {
        color: '#2c3e50',
        fontWeight: 'bold'
      },
      // 扁平化指示器
      axisPointer: {
        lineStyle: {
          color: '#bdc3c7',
          width: 2,
          type: 'dashed'
        }
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      // 假设后端数据包含 time 字段
      data: props.data.map(item => item.time || ''),
      axisLine: {
        lineStyle: {
          color: '#bdc3c7',
          width: 2
        }
      },
      axisLabel: {
        color: '#7f8c8d',
        fontWeight: 'bold'
      }
    },
    yAxis: {
      type: 'value',
      max: 100, // CPU 百分比最大 100
      splitLine: {
        lineStyle: {
          color: '#ecf0f1', // 极淡的分割线
          width: 1
        }
      },
      axisLabel: {
        color: '#7f8c8d'
      }
    },
    series: [
      {
        name: 'CPU使用率',
        type: 'line',
        smooth: true, // 平滑曲线
        symbol: 'none', // 去掉折线上的圆点，更极简
        lineStyle: {
          width: 4, // 加粗线条
          color: '#3498db' // 纯粹的蓝色
        },
        areaStyle: {
          // 极淡的区域填充，或者直接去掉 areaStyle 变成纯折线
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(52, 152, 219, 0.2)' },
            { offset: 1, color: 'rgba(52, 152, 219, 0)' }
          ])
        },
        // 假设后端数据包含 value 或 usage 字段
        data: props.data.map(item => item.usage || item.value || 0)
      }
    ]
  }

  chartInstance.setOption(option)
}

// 监听数据变化，动态更新图表
watch(() => props.data, () => {
  if (chartInstance) {
    chartInstance.setOption({
      xAxis: {
        data: props.data.map(item => item.time)
      },
      series: [
        {
          data: props.data.map(item => item.usage || item.value)
        }
      ]
    })
  }
}, { deep: true })

// 窗口大小改变时自适应
const handleResize = () => {
  chartInstance && chartInstance.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  window.removeEventListener('resize', handleResize)
})
</script>