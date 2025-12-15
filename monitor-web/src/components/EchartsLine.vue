<template>
  <div ref="chartRef" :style="{ height: height, width: '100%' }"></div>
</template>

<script setup>
import {ref, onMounted, onBeforeUnmount, watch} from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 单线模式数据
  data: {type: Array, default: () => []},
  // 🟢 新增：多线模式数据 (格式：[{ name: '1min', data: [], color: '#xxx' }, ...])
  multiData: {type: Array, default: () => []},

  height: {type: String, default: '350px'},
  color: {type: String, default: '#3498db'},
  seriesName: {type: String, default: '数值'},
  unit: {type: String, default: ''}
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateOption()
}

const updateOption = () => {
  // 判断是单线还是多线
  const isMulti = props.multiData && props.multiData.length > 0

  // 提取 X 轴 (取第一个非空数据的 time)
  let xAxisData = []
  if (isMulti) {
    // 找到第一个有数据的 series 作为 X 轴基准
    const validSeries = props.multiData.find(s => s.data && s.data.length > 0)
    if (validSeries) xAxisData = validSeries.data.map(item => item.time)
  } else {
    xAxisData = props.data.map(item => item.time)
  }

  // 构建 Series
  let seriesList = []
  if (isMulti) {
    // 多线模式
    seriesList = props.multiData.map(item => ({
      name: item.name,
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: {width: 2, color: item.color}, // 线条稍细一点以便区分
      itemStyle: {color: item.color},
      data: item.data.map(d => d.value)
    }))
  } else {
    // 单线模式 (保持原有逻辑)
    seriesList = [{
      name: props.seriesName,
      type: 'line',
      smooth: true,
      symbol: 'none',
      lineStyle: {width: 3, color: props.color},
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: hexToRgba(props.color, 0.3)},
          {offset: 1, color: hexToRgba(props.color, 0)}
        ])
      },
      data: props.data.map(item => item.value || 0)
    }]
  }

  const option = {
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: isMulti?'30px':'10px',
      containLabel: true
    },
    legend: {
      show: isMulti,
      top: 0,
      right: 10,
      itemGap: 20,
      bottom: 0,
      icon: 'circle',
      textStyle: {
        color: '#7f8c8d',
        fontWeight: 'bold'
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      textStyle: {color: '#2c3e50', fontWeight: 'bold'},
      axisPointer: {lineStyle: {color: '#bdc3c7', type: 'dashed'}},
      formatter: (params) => {
        let html = `<div style="font-size: 12px; color: #7f8c8d; margin-bottom: 4px;">${params[0].name}</div>`
        params.forEach(item => {
          html += `
            <div style="display: flex; align-items: center; margin-bottom: 3px;">
              ${item.marker}
              <span style="width: 50px; display:inline-block;">${item.seriesName}: </span>
              <span style="font-weight: 800; margin-left: 8px;">${item.value} ${props.unit}</span>
            </div>`
        })
        return html
      }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLine: {lineStyle: {color: '#bdc3c7'}},
      axisLabel: {color: '#7f8c8d'}
    },
    yAxis: {
      type: 'value',
      splitLine: {lineStyle: {color: 'rgba(0,0,0,0.05)'}},
      axisLabel: {color: '#7f8c8d'}
    },
    series: seriesList
  }

  chartInstance.setOption(option, true) // true 表示不合并，重绘
}

// Hex转RGBA工具
const hexToRgba = (hex, alpha) => {
  let r = 0, g = 0, b = 0
  if (!hex) return `rgba(0,0,0,${alpha})`
  if (hex.length === 4) {
    r = Number.parseInt("0x" + hex[1] + hex[1]);
    g = Number.parseInt("0x" + hex[2] + hex[2]);
    b = Number.parseInt("0x" + hex[3] + hex[3]);
  } else if (hex.length === 7) {
    r = Number.parseInt("0x" + hex[1] + hex[2]);
    g = Number.parseInt("0x" + hex[3] + hex[4]);
    b = Number.parseInt("0x" + hex[5] + hex[6]);
  }
  return `rgba(${r},${g},${b},${alpha})`
}

// 监听数据变化
watch(() => [props.data, props.multiData], () => {
  if (chartInstance) updateOption()
}, {deep: true})

const handleResize = () => chartInstance && chartInstance.resize()

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  chartInstance && chartInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>