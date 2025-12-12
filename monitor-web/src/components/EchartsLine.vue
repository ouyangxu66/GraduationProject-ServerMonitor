<template>
  <div ref="chartRef" :style="{ height: height, width: '100%' }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

// 作用: 获取 props.data 的值，并返回一个数组，数组的元素是 props.data 的每个元素的 value 属性
const props = defineProps({
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
  },
  seriesName: {
    type: String,
    default: '数值'
  },
  unit: {
    type: String,
    default: ''
  }
})

// 数据可视化图表
const chartRef = ref(null)
let chartInstance = null

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: props.color,
      borderWidth: 1,
      textStyle: {
        color: '#2c3e50',
        fontWeight: 'bold'
      },
      axisPointer: {
        lineStyle: {
          color: '#bdc3c7',
          width: 2,
          type: 'dashed'
        }
      },
      // 🟢 核心修改：自定义 Tooltip 内容格式
      // params 是一个数组，包含了当前 Hover 点的所有信息
      formatter: (params) => {
        const item = params[0]
        // 格式：时间 <br/> 圆点 名称 : 数值 单位
        return `
          <div style="font-size: 12px; color: #7f8c8d; margin-bottom: 4px;">${item.name}</div>
          <div style="display: flex; align-items: center;">
            ${item.marker}
            <span style="margin-left: 2px">${item.seriesName}: </span>
            <span style="margin-left: 8px; font-weight: 800; color: ${props.color}">${item.value} ${props.unit}</span>
          </div>
        `
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.data.map(item => item.time || ''),
      axisLine: { lineStyle: { color: '#bdc3c7' } },
      axisLabel: { color: '#7f8c8d' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(0,0,0,0.05)' } },
      axisLabel: { color: '#7f8c8d' }
    },
    series: [
      {
        name: props.seriesName,
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: {
          width: 3,
          color: props.color
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: hexToRgba(props.color, 0.3) },
            { offset: 1, color: hexToRgba(props.color, 0) }
          ])
        },
        data: props.data.map(item => item.value || 0)
      }
    ]
  }

  chartInstance.setOption(option)
}

// 颜色转换：将十六进制颜色转换为 rgba 格式
const hexToRgba = (hex, alpha) => {
  let r = 0, g = 0, b = 0
  if (hex.length === 4) {
    r = Number.parseInt("0x" + hex[1] + hex[1])
    g = Number.parseInt("0x" + hex[2] + hex[2])
    b = Number.parseInt("0x" + hex[3] + hex[3])
  } else if (hex.length === 7) {
    r = Number.parseInt("0x" + hex[1] + hex[2])
    g = Number.parseInt("0x" + hex[3] + hex[4])
    b = Number.parseInt("0x" + hex[5] + hex[6])
  }
  return `rgba(${r},${g},${b},${alpha})`
}

// 监听 props.data 的变化,更新图表
watch(() => props.data, () => {
  if (chartInstance) {
    chartInstance.setOption({
      xAxis: {data: props.data.map(item => item.time)},
      series: [{data: props.data.map(item => item.value)}]
    })
  }
}, {deep: true})

// 监听窗口大小变化
const handleResize = () => chartInstance && chartInstance.resize()

// 挂载图表
onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

// 销毁图表
onBeforeUnmount(() => {
  chartInstance && chartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>