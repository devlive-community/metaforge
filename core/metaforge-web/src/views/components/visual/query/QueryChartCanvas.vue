<template>
  <EChart ref="canvas" class="h-full w-full" :option="spec" :map="prepared?.map" fit @point="selectPoint" @error="emit('error')"/>
</template>

<script setup lang="ts">
import type { EChartsOption } from 'echarts'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { QueryChartOptions, QueryChartPoint } from '@/model/query-chart'
import { queryChartCategoryShares, queryChartGeometry } from '@/model/query-chart-data'
import type { PreparedQueryChart } from '@/model/query-chart-series'
import EChart from '../EChart.vue'

const props = defineProps<{ options: QueryChartOptions; points: QueryChartPoint[]; prepared?: PreparedQueryChart; metric: string; embedded?: boolean }>()
const emit = defineEmits<{ point: [ids: number[]]; error: [] }>()
const { locale, t } = useI18n()
const canvas = ref<InstanceType<typeof EChart>>()
const themeVersion = ref(0)
let observer: MutationObserver | undefined

const spec = computed<EChartsOption>(() => {
  const options = props.options
  themeVersion.value
  const style = getComputedStyle(canvas.value?.$el ?? document.documentElement)
  const token = (name: string) => style.getPropertyValue('--mf-color-' + name).trim()
  const palette = { green: token('primary'), blue: token('info'), purple: '#8359d8' }
  const colors = options.colors.split(/[,\s]+/).filter(color => /^#[0-9a-f]{6}$/i.test(color))
  const chartColors = [...new Set([...colors, palette[options.palette], token('info'), '#8359d8', token('warning'), token('danger'), token('primary')])].filter(Boolean)
  const special = props.prepared?.option
  if (special) {
    const showAxis = !!special.xAxis && !!special.yAxis
    return {
      animation: false,
      color: chartColors,
      backgroundColor: options.transparent ? 'transparent' : token('surface'),
      textStyle: { color: token('content-secondary') },
      tooltip: { show: options.tooltip !== 'off', trigger: 'item', renderMode: 'richText', confine: true },
      ...(showAxis ? { grid: { top: 22, bottom: options.zoom ? 38 : 16, left: 24, right: 18, containLabel: true },
        dataZoom: options.zoom ? [{ type: 'inside' }, { type: 'slider', height: 14, bottom: 4 }] : undefined } : {}),
      ...special,
      ...(special.visualMap ? { visualMap: { ...special.visualMap, inRange: { color: ['#e7f4ef', chartColors[0] || '#0a8f67'] } } } : {}),
    } as EChartsOption
  }
  const circular = ['pie', 'donut', 'rose', 'funnel'].includes(options.type)
  const radar = options.type === 'radar'
  const geometry = queryChartGeometry(options.type)
  const horizontal = geometry?.horizontal
  const shares = geometry?.percentage ? queryChartCategoryShares(props.points) : undefined
  const percentageFormat = new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: Math.max(0, Math.min(6, options.decimals || 0)) })
  const numericX = (options.type === 'scatter' || options.type === 'effectScatter') && props.points.every(point => point.category.trim() && Number.isFinite(Number(point.category)))
  const formatCategory = (label: string) => {
    if (options.dateFormat === 'full' || !/^\d{4}-\d{2}-\d{2}/.test(label)) return label
    return options.dateFormat === 'time' ? label.slice(5).replace('-', '/') : label.slice(5, 10)
  }
  const formatValue = (value: number | null, label = false) => {
    if (value == null || !Number.isFinite(value)) return '—'
    const decimals = label ? Math.max(0, Math.min(6, options.decimals || 0)) : options.numberFormat === 'decimal' ? 2 : 0
    const number = new Intl.NumberFormat(locale.value, {
      style: !label && options.numberFormat === 'percent' ? 'percent' : 'decimal',
      notation: !label && options.numberFormat === 'compact' ? 'compact' : 'standard',
      minimumFractionDigits: decimals, maximumFractionDigits: decimals
    }).format(value)
    return label && options.unit ? number + ' ' + options.unit : number
  }
  const seriesName = (point: QueryChartPoint) => [options.seriesName, point.series].filter(Boolean).join(' · ') || props.metric
  const categories = [...new Set(props.points.map(point => point.category))]
  const bySeries = new Map<string, Map<string, QueryChartPoint>>()
  for (const point of props.points) {
    const name = seriesName(point)
    if (!bySeries.has(name)) bySeries.set(name, new Map())
    bySeries.get(name)!.set(point.category, point)
  }
  const text = token('content-secondary')
  const categoryAxis = {
    type: numericX ? 'value' : 'category',
    data: numericX ? undefined : categories, show: options.xVisible,
    name: options.xTitleVisible ? options.xTitle : '',
    axisLabel: { hideOverlap: options.hideOverlap, formatter: formatCategory, color: text }
  }
  const valueAxis = {
    type: 'value', min: shares ? 0 : options.min, max: shares ? 1 : options.max, name: options.yTitle,
    axisLabel: { show: options.yVisible, formatter: (value: number) => shares ? percentageFormat.format(value) : formatValue(value), color: text },
    splitLine: { show: options.grid, lineStyle: { color: token('line') } }
  }
  const position = options.labelPosition === 'auto' ? undefined : options.labelPosition === 'top' && circular ? 'outside' : options.labelPosition
  const formatShare = (point?: QueryChartPoint) => {
    const share = point ? shares?.get(point.pointId) : undefined
    return share === undefined ? '—' : percentageFormat.format(share)
  }
  const label = { show: options.labels, position, color: text,
    formatter: (param: { data: { point?: QueryChartPoint } }) => shares ? formatShare(param.data?.point) : formatValue(param.data?.point?.value ?? null, true) }
  const marks = {
    label, labelLayout: { hideOverlap: options.labelOverlap },
    emphasis: { focus: options.pointAction === 'highlight' ? 'self' : 'none' },
    selectedMode: options.pointAction === 'highlight' ? 'single' : false
  }
  const topLegend = options.legend && options.legendPosition === 'top'
  const bottomLegend = options.legend && options.legendPosition === 'bottom'
  const rightLegend = options.legend && options.legendPosition === 'right'
  const donut = props.embedded && options.type === 'donut'
  const donutX = rightLegend ? '36%' : '50%'
  const outerRadius = options.pieRadius ?? (donut && rightLegend ? 58 : 75)
  const innerRadius = Math.min(outerRadius - 5, options.donutHole ?? (donut && rightLegend ? 36 : 45))
  const donutTotal = donut && options.donutTotal !== false && (options.aggregation === 'sum' || options.aggregation === 'count')
      ? formatValue(props.points.reduce((sum, point) => sum + (point.value ?? 0), 0), true) : undefined
  const circularData = props.points.map(point => ({
    name: [formatCategory(point.category), point.series].filter(Boolean).join(' · '), value: point.value, point, pointId: point.pointId
  }))
  const series = circular ? [{
    ...marks, type: options.type === 'funnel' ? 'funnel' : 'pie',
    radius: options.type === 'donut' ? [innerRadius + '%', outerRadius + '%'] : outerRadius + '%',
    center: donut ? [donutX, '51%'] : undefined,
    roseType: options.type === 'rose' ? 'radius' : undefined,
    data: circularData
  }] : Array.from(bySeries, ([name, points], index) => ({
    ...marks, name,
    type: geometry?.series ?? options.type,
    ...(geometry?.series === 'bar' ? { barMaxWidth: options.barMaxWidth,
      itemStyle: options.barCornerRadius === undefined ? undefined : { borderRadius: options.barCornerRadius } } : {}),
    ...(geometry?.stacked ? { stack: 'category' } : {}),
    ...(geometry?.step ? { step: 'end' } : {}),
    data: (numericX ? [...points.keys()] : categories).map(category => {
      const point = points.get(category)
      if (!point) return null
      const value = shares ? shares.get(point.pointId) ?? null : point.value
      return { value: numericX ? [Number(category), value] : value, point, pointId: point.pointId }
    }),
    ...(options.type === 'pictorialBar' ? { symbol: 'roundRect' } : {}),
    smooth: !geometry?.step && options.smooth, showSymbol: options.points, symbolSize: options.pointSize,
    lineStyle: { width: options.lineWidth, type: options.dashed ? 'dashed' : 'solid' },
    areaStyle: geometry?.area || (geometry?.series === 'line' && !geometry.stacked && options.fill) ? {
      opacity: options.opacity / 100,
      color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [{ offset: 0, color: chartColors[index % chartColors.length] }, { offset: 1, color: 'transparent' }] }
    } : undefined
  }))
  const radarBounds = radar ? { min: Math.min(0, ...props.points.map(point => point.value ?? 0)), max: Math.max(1, ...props.points.map(point => point.value ?? 0)) } : {}
  const tooltipText = (point: QueryChartPoint) => options.tooltipTemplate.trim()
      ? options.tooltipTemplate.replace(/\{(x|series|value)\}/g, (_match, field) => field === 'x' ? formatCategory(point.category)
          : field === 'series' ? seriesName(point) : formatValue(point.value, true))
      : [formatCategory(point.category), seriesName(point) + ': ' + formatValue(point.value, true),
        ...(shares ? [formatShare(point)] : [])].join(' · ')
  const tooltipFormatter = (params: unknown) => {
    const items = (Array.isArray(params) ? params : [params]) as { data?: { point?: QueryChartPoint } }[]
    return items.flatMap(item => item.data?.point ? [tooltipText(item.data.point)] : []).join('\n')
  }
  return {
    animation: false, color: chartColors,
    backgroundColor: options.transparent ? 'transparent' : token('surface'), textStyle: { color: text },
    legend: { show: options.legend, selectedMode: options.legendInteractive, type: 'scroll',
      orient: rightLegend ? 'vertical' : 'horizontal', right: rightLegend ? 0 : undefined,
      top: donut && rightLegend ? 'middle' : topLegend || rightLegend ? 0 : undefined, bottom: bottomLegend ? 0 : undefined, textStyle: { color: text } },
    tooltip: { show: options.tooltip !== 'off', renderMode: 'richText', confine: true,
      trigger: circular || radar || options.tooltip === 'mark' ? 'item' : 'axis',
      formatter: radar ? undefined : tooltipFormatter },
    ...(donutTotal ? {
      title: { text: donutTotal, subtext: t(options.aggregation === 'count' ? 'dashboard.studio.totalRecords' : 'dashboard.studio.totalValue'),
        left: donutX, top: '50%', textAlign: 'center', textVerticalAlign: 'middle', itemGap: 4, padding: 0,
        textStyle: { fontSize: 19, color: token('content') }, subtextStyle: { fontSize: 10, color: token('content-tertiary') } }
    } : {}),
    ...(radar ? {
      radar: { indicator: categories.map(name => ({ name: formatCategory(name),
        ...radarBounds })), radius: '60%' },
      series: [{ type: 'radar', data: Array.from(bySeries, ([name, points]) => ({
        name, value: categories.map(category => points.get(category)?.value ?? 0), pointIds: [...points.values()].map(point => point.pointId)
      })) }]
    } : { series }),
    ...(!circular && !radar ? {
      grid: { top: topLegend ? 36 : 20, bottom: (bottomLegend ? 36 : 16) + (options.zoom && !horizontal ? 30 : 0),
        right: rightLegend ? 100 : 26, left: 22, containLabel: true },
      xAxis: horizontal ? valueAxis : categoryAxis, yAxis: horizontal ? categoryAxis : valueAxis,
      dataZoom: options.zoom ? [{ type: 'inside', ...(horizontal ? { yAxisIndex: 0 } : { xAxisIndex: 0 }) },
        { type: 'slider', ...(horizontal ? { yAxisIndex: 0, right: rightLegend ? 100 : 0, width: 14 } : { xAxisIndex: 0, bottom: bottomLegend ? 30 : 0, height: 18 }) }] : undefined
    } : {})
  } as EChartsOption
})

function selectPoint(data: unknown, name?: string) {
  const datum = data as { pointId?: number; pointIds?: number[] } | undefined
  const ids = datum?.pointIds ?? (typeof datum?.pointId === 'number' ? [datum.pointId] : [])
  const series = Array.isArray(data) && typeof data[2] === 'string' ? data[2] : name
  const namedId = series === undefined ? undefined : props.prepared?.names?.get(series)
  if (ids.length) emit('point', ids)
  else if (namedId !== undefined) emit('point', [namedId])
}
onMounted(() => {
  themeVersion.value++
  observer = new MutationObserver(() => { themeVersion.value++ })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['style', 'data-theme'] })
})
onBeforeUnmount(() => observer?.disconnect())
defineExpose({ exportImage: (name: string, type: 'png' | 'svg') => canvas.value?.exportImage(name, type),
  loading: computed(() => canvas.value?.loading !== false) })
</script>
