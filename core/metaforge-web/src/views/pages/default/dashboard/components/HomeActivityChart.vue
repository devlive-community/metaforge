<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import type { HomeActivity } from '@/composables/useHomeAnalytics'
import { useChartColors } from '@/composables/useChartColors'
import EChart from '@/views/components/visual/EChart.vue'
const props = defineProps<{ activity: HomeActivity }>()
const { t } = useI18n(), colors = useChartColors()
const option = computed<EChartsOption>(() => ({
  animation: false,
  tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
  grid: { left: 12, right: 20, top: 24, bottom: 8, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: props.activity.trend.map(point => point.date),
    axisLine: { lineStyle: { color: colors.value.line } }, axisTick: { show: false },
    axisLabel: { color: colors.value.text, hideOverlap: true, formatter: (date: string) => date.slice(5) } },
  yAxis: { type: 'value', minInterval: 1, axisLabel: { color: colors.value.text },
    splitLine: { lineStyle: { color: colors.value.line, type: 'dashed' } } },
  series: [{ type: 'line', name: t('homeWorkspace.queryCount'), data: props.activity.trend.map(point => point.count),
    smooth: true, symbol: 'circle', symbolSize: 6, showSymbol: props.activity.days <= 7,
    lineStyle: { width: 2.5, color: colors.value.primary }, itemStyle: { color: colors.value.primary },
    areaStyle: { opacity: .22, color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
      colorStops: [{ offset: 0, color: colors.value.primary }, { offset: 1, color: 'transparent' }] } } }],
}))
</script>
<template><EChart :option="option" class="h-full min-h-0 w-full"/></template>
