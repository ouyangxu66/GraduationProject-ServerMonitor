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
  },
  color: {
    type: String,
    default: '#3498db'
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
          color:  props.color // 纯粹的蓝色
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: props.color }, // 使用传入的颜色 (需要转rgba，简单起见可以直接用 props.color，或者用 hexToRgba 工具函数)
            // 简便写法：直接用 opacity
            { offset: 0, color: props.color.replace(')', ', 0.2)').replace('rgb', 'rgba') }, // 这里的转换比较复杂，建议直接去掉 areaStyle 或者只用线条色
          ]),
          opacity: 0.1 // 简单的透明度
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