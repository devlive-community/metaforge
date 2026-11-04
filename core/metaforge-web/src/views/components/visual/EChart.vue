<template>
  <div ref="host" class="min-h-0 min-w-0"/>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{ option: echarts.EChartsOption }>()
const emit = defineEmits<{ point: [data: unknown]; error: [] }>()
const host = ref<HTMLElement>()
let chart: echarts.ECharts | undefined
let observer: ResizeObserver | undefined

function render() {
  const element = host.value
  if (!element?.clientWidth || !element.clientHeight) return
  try {
    if (!chart) {
      chart = echarts.init(element)
      chart.on('click', event => emit('point', event.data))
    }
    chart.setOption(props.option, { notMerge: true })
  }
  catch {
    chart?.dispose()
    chart = undefined
    emit('error')
  }
}

watch(host, element => {
  observer?.disconnect()
  chart?.dispose()
  chart = undefined
  if (!element) return
  observer = new ResizeObserver(() => {
    if (chart) chart.resize()
    else render()
  })
  observer.observe(element)
  render()
}, { flush: 'post' })
watch(() => props.option, render, { flush: 'post' })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
})

defineExpose({
  exportImage(name: string, type: 'png' | 'svg') {
    if (!chart) return
    const link = document.createElement('a')
    link.download = name + '.' + type
    if (type === 'png') {
      link.href = chart.getDataURL({ type: 'png', pixelRatio: 2 })
      link.click()
      return
    }
    const svg = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width: chart.getWidth(), height: chart.getHeight() })
    try {
      const option = chart.getOption() as echarts.EChartsOption
      svg.setOption({ ...option, animation: false })
      const blob = new Blob([svg.renderToSVGString()], { type: 'image/svg+xml;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      link.href = url
      link.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    }
    finally { svg.dispose() }
  }
})
</script>
