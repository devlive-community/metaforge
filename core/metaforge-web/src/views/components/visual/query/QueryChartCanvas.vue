<template>
  <div ref="host" class="h-full min-h-0 w-full min-w-0"/>
</template>

<script setup lang="ts">
import VChart, { type ISpec } from '@visactor/vchart'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { QueryChartOptions, QueryChartPoint } from '@/model/query-chart'

const props = defineProps<{ options: QueryChartOptions; points: QueryChartPoint[]; metric: string }>()
const emit = defineEmits<{ point: [id: number]; error: [] }>()
const { locale } = useI18n()
const host = ref<HTMLElement>()
const themeVersion = ref(0)
let chart: VChart | undefined
let observer: MutationObserver | undefined

const spec = computed(() => {
  const options = props.options
  const element = host.value
  if (!element) return null
  themeVersion.value
  const style = getComputedStyle(element)
  const token = (name: string) => style.getPropertyValue('--mf-color-' + name).trim()
  const palette = { green: token('primary'), blue: token('info'), purple: '#8359d8' }
  const colors = options.colors.split(/[,\s]+/).filter(color => /^#[0-9a-f]{6}$/i.test(color))
  const circular = ['pie', 'donut', 'rose', 'funnel'].includes(options.type)
  const horizontal = options.type === 'horizontal'
  const numericX = options.type === 'scatter' && props.points.every(point => point.category.trim() && Number.isFinite(Number(point.category)))
  const formatCategory = (label: unknown) => {
    const text = String(label)
    if (options.dateFormat === 'full' || !/^\d{4}-\d{2}-\d{2}/.test(text)) return text
    return options.dateFormat === 'time' ? text.slice(5).replace('-', '/') : text.slice(5, 10)
  }
  const formatValue = (value: unknown, label = false) => {
    if (value == null || value === '' || !Number.isFinite(Number(value))) return '—'
    const decimals = label ? Math.max(0, Math.min(6, options.decimals || 0)) : options.numberFormat === 'decimal' ? 2 : 0
    const number = new Intl.NumberFormat(locale.value, {
      style: !label && options.numberFormat === 'percent' ? 'percent' : 'decimal',
      notation: !label && options.numberFormat === 'compact' ? 'compact' : 'standard',
      minimumFractionDigits: decimals, maximumFractionDigits: decimals
    }).format(Number(value))
    return label && options.unit ? number + ' ' + options.unit : number
  }
  const points = props.points.map(point => ({
    ...point,
    x: numericX ? Number(point.category) : point.category,
    label: point.series ? point.category + ' · ' + point.series : point.category,
    seriesLabel: options.seriesName ? (point.series ? options.seriesName + ' · ' + point.series : options.seriesName) : point.series || props.metric
  }))
  const text = token('content-secondary')
  const axisX = {
    orient: horizontal ? 'left' : 'bottom', type: numericX ? 'linear' : 'band', visible: options.xVisible,
    title: { visible: options.xTitleVisible && !!options.xTitle, text: options.xTitle, style: { fill: text } },
    label: { autoHide: options.hideOverlap, formatMethod: formatCategory, style: { fill: text } }
  }
  const axisY = {
    orient: horizontal ? 'bottom' : 'left', type: 'linear',
    min: options.min, max: options.max,
    title: { visible: !!options.yTitle, text: options.yTitle, style: { fill: text } },
    label: { visible: options.yVisible, formatMethod: (value: unknown) => formatValue(value), style: { fill: text } },
    grid: { visible: options.grid, style: { stroke: token('line') } }
  }
  const type = options.type === 'donut' ? 'pie' : horizontal ? 'bar' : options.type
  const tooltipContent = options.tooltipTemplate.trim() ? [{
    key: (datum: typeof points[number]) => datum.seriesLabel,
    value: (datum: typeof points[number]) => options.tooltipTemplate.replace(/\{(x|series|value)\}/g, (_match, field) => {
      if (field === 'x') return formatCategory(datum.category)
      if (field === 'series') return datum.seriesLabel
      return formatValue(datum.value, true)
    })
  }] : undefined
  const color = colors[0] || palette[options.palette]
  const areaStyle = { fill: {
    gradient: 'linear', x0: 0, y0: 0, x1: 0, y1: 1,
    stops: [{ offset: 0, color }, { offset: 1, color: 'transparent' }]
  }, fillOpacity: options.opacity / 100 }
  const definition = {
    type, data: [{ id: 'queryChart', values: points }],
    background: options.transparent ? 'transparent' : token('surface'),
    color: [...new Set([...colors, palette[options.palette], token('info'), '#8359d8', token('warning'), token('danger'), token('primary')])].filter(Boolean),
    padding: { top: 12, right: 18, bottom: 12, left: 12 },
    animation: false,
    hover: options.pointAction === 'highlight',
    select: options.pointAction === 'highlight',
    legends: { visible: options.legend, orient: options.legendPosition, select: options.legendInteractive, item: { label: { style: { fill: text } } } },
    label: { visible: options.labels, position: options.labelPosition === 'auto' ? undefined : options.labelPosition,
      overlap: { hideOnHit: options.labelOverlap }, formatMethod: (_: unknown, datum: QueryChartPoint) => formatValue(datum?.value, true), style: { fill: text } },
    tooltip: { visible: options.tooltip !== 'off', renderMode: 'canvas', confine: true, activeType: circular || options.tooltip === 'mark' ? 'mark' : 'dimension',
      mark: { content: tooltipContent }, dimension: { content: tooltipContent } },
    ...(options.type === 'radar' ? { categoryField: 'category', valueField: 'value', seriesField: 'seriesLabel' } : circular ? { categoryField: 'label', valueField: 'value', outerRadius: 0.82, innerRadius: options.type === 'donut' ? 0.52 : 0 } : {
      xField: horizontal ? 'value' : 'x', yField: horizontal ? 'x' : 'value',
      direction: horizontal ? 'horizontal' : 'vertical',
      seriesField: 'seriesLabel', stack: false,
      axes: [axisX, axisY],
      dataZoom: options.zoom ? [{ orient: horizontal ? 'left' : 'bottom', filterMode: 'axis', zoomLock: false }] : undefined
    }),
    point: { visible: options.points || options.type === 'scatter', style: { size: options.pointSize, stroke: color, lineWidth: 2 } },
    line: { style: { lineWidth: options.lineWidth, curveType: options.smooth ? 'monotone' : 'linear', lineDash: options.dashed ? [5, 4] : [] } },
    area: { visible: options.fill || options.type === 'area', style: areaStyle }
  }
  if (type === 'line' && options.fill) {
    // VChart's area series draws both the stroke and the fill.
    definition.type = 'area'
  }
  return definition as unknown as ISpec
})

watch(spec, definition => {
  if (!host.value || !definition) return
  try {
    if (chart) chart.updateSpecSync(definition, false)
    else {
      chart = new VChart(definition, { dom: host.value })
      chart.renderSync()
      chart.on('click', event => {
        const point = event.datum as QueryChartPoint | undefined
        if (point && Number.isInteger(point.pointId)) emit('point', point.pointId)
      })
    }
  }
  catch {
    chart?.release()
    chart = undefined
    emit('error')
  }
}, { flush: 'post' })

onMounted(() => {
  observer = new MutationObserver(() => { themeVersion.value++ })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['style', 'data-theme'] })
})
onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.release()
  chart = undefined
})
defineExpose({ exportImage: (name: string) => chart?.exportImg(name) })
</script>
