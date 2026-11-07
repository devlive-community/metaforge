<template>
  <div :style="{ width, height }">
    <MfAlert v-if="failed" variant="danger" :title="$t('queryChart.renderError')"/>
    <EChart v-else class="h-full w-full" :option="option" @error="failed = true"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { CustomSeriesOption, EChartsOption } from 'echarts'
import type { Configuration } from '../Configuration'
import { Type } from '../Type'
import EChart from '../EChart.vue'

const props = withDefaults(defineProps<{ configuration: Configuration; submitted?: boolean; width?: string; height?: string; type?: Type }>(), {
  submitted: true, width: '100%', height: '400px'
})
const emit = defineEmits<{ change: [configuration: Configuration] }>()
const failed = ref(false)

const radius = (value: number | undefined, fallback: number) => (value ?? fallback) * 100 + '%'
const number = (value: unknown) => {
  if (value == null || value === '') return null
  const result = Number(value)
  return Number.isFinite(result) ? result : null
}

const option = computed<EChartsOption>(() => {
  const configuration = props.configuration
  const type = props.type ?? configuration.type
  const settings = configuration.chartConfigure || {}
  const x = settings.xAxis || settings.leftField || ''
  const y = settings.yAxis || ''
  const points = (configuration.columns as Record<string, unknown>[]).map(row => ({
    name: String(row[x] ?? ''),
    value: number(row[y]),
    group: settings.series ? String(row[settings.series] ?? '') : '',
    x2: number(row[settings.x2Axis || settings.rightField || ''])
  }))
  const category = [...new Set(points.map(point => point.name))]
  const dataBySeries = new Map<string, typeof points>()
  points.forEach(point => {
    if (!dataBySeries.has(point.group)) dataBySeries.set(point.group, [])
    dataBySeries.get(point.group)!.push(point)
  })
  const data = points.map(({ name, value }) => ({ name, value }))
  const base: EChartsOption = {
    tooltip: { renderMode: 'richText', confine: true },
    legend: { show: settings.showLegend ?? (!!settings.series || type === Type.PIE || type === Type.ROSE), bottom: 0, type: 'scroll' },
    title: { show: settings.titleVisible !== false && !!settings.titleText,
      text: settings.titleText, subtext: settings.titleSubText,
      left: settings.titleAlign || (settings.titlePosition === 'right' ? 'right' : 'center'),
      top: settings.titlePosition === 'bottom' ? undefined : 0,
      bottom: settings.titlePosition === 'bottom' ? 26 : undefined }
  }
  const label = { show: !!settings.labelVisible }
  if (type === Type.PIE || type === Type.ROSE) {
    return { ...base, series: [{ type: 'pie', roseType: type === Type.ROSE ? 'radius' : undefined,
      radius: [radius(settings.innerRadius?.[0], 0), radius(settings.outerRadius?.[0], 0.8)],
      label, data }] }
  }
  if (type === Type.FUNNEL) return { ...base, series: [{ type: 'funnel', data, label }] }
  if (type === Type.GAUGE) {
    return { ...base, series: [{ type: 'gauge', radius: radius(settings.outerRadius?.[0], 0.8),
      axisLine: { lineStyle: { width: Math.max(1, ((settings.outerRadius?.[0] ?? 0.8) - (settings.innerRadius?.[0] ?? 0.6)) * 75) } },
      startAngle: settings.startAngle?.[0], endAngle: settings.endAngle?.[0], data }] }
  }
  if (type === Type.WORDCLOUD) {
    return { ...base, series: [{ type: 'wordCloud', data: data.filter(item => (item.value ?? 0) > 0), sizeRange: [12, 54] }] } as EChartsOption
  }
  if (type === Type.RADAR) {
    const maximum = Math.max(1, ...points.map(point => point.value ?? 0))
    const minimum = Math.min(0, ...points.map(point => point.value ?? 0))
    return { ...base, radar: { indicator: category.map(name => ({ name, min: minimum, max: maximum })) },
      series: [{ type: 'radar', data: Array.from(dataBySeries, ([name, values]) => ({
        name, value: category.map(item => values.find(value => value.name === item)?.value ?? 0)
      })) }] }
  }
  const grid = { top: settings.titleText ? 58 : 20, bottom: 34, left: 16, right: 16, containLabel: true }
  if (type === Type.HISTOGRAM && (settings.x2Axis || settings.rightField)) {
    const histogram: CustomSeriesOption = {
      type: 'custom',
      renderItem: (_params, api) => {
        const start = api.coord([api.value(0), 0])
        const end = api.coord([api.value(1), api.value(2)])
        return { type: 'rect', shape: { x: start[0], y: Math.min(start[1], end[1]), width: Math.max(1, end[0] - start[0]), height: Math.abs(start[1] - end[1]) },
          style: { fill: String(api.visual('color')) } }
      },
      encode: { x: [0, 1], y: 2 },
      data: points.flatMap(point => {
        const left = number(point.name)
        return left != null && point.x2 != null && point.value != null ? [[left, point.x2, point.value]] : []
      })
    }
    return { ...base, grid, xAxis: { type: 'value' }, yAxis: { type: 'value' }, series: [histogram] }
  }
  const scatter = type === Type.SCATTER
  const numericX = scatter && points.every(point => number(point.name) !== null)
  const seriesType = scatter ? 'scatter' : type === Type.BAR || type === Type.HISTOGRAM ? 'bar' : 'line'
  return { ...base, grid, xAxis: numericX ? { type: 'value' } : { type: 'category', data: category }, yAxis: { type: 'value' },
    series: Array.from(dataBySeries, ([name, values]) => ({
      type: seriesType, name, label, connectNulls: settings.invalidType === 'link',
      areaStyle: type === Type.AREA ? {} : undefined,
      data: values.filter(point => settings.invalidType !== 'ignore' || point.value !== null).map(point => [
        numericX ? Number(point.name) : point.name, point.value ?? (settings.invalidType === 'zero' ? 0 : null)
      ])
    })) } as EChartsOption
})

watch(() => props.configuration, value => {
  failed.value = false
  if (props.submitted) emit('change', { ...value, headers: [], columns: [] })
}, { immediate: true, deep: true })
</script>
