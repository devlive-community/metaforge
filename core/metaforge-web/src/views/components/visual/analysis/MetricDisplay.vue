<template>
  <MfEmpty v-if="emptyReason" class="h-full" size="sm" :title="$t(emptyReason)"/>
  <div v-else-if="['metricTicker', 'metricComparison', 'bulletMetric', 'metricTrend'].includes(widget.kind)" class="flex h-full min-h-0 flex-col justify-center gap-2 px-1">
    <strong class="truncate text-3xl font-bold" :title="format(metric)">
      <AnimatedNumber v-if="widget.kind === 'metricTicker'" :value="metric" :decimals="settings.decimals" :prefix="settings.prefix" :suffix="settings.suffix"/>
      <span v-else>{{ format(metric) }}</span>
    </strong>
    <template v-if="widget.kind === 'metricComparison'">
      <div class="flex flex-wrap items-center gap-2 text-sm"><MfTag :variant="difference >= 0 ? 'success' : 'danger'">{{ difference >= 0 ? '+' : '' }}{{ number.format(difference) }}</MfTag><span v-if="relative !== undefined" class="tabular-nums">{{ number.format(relative) }}%</span></div>
      <span class="text-xs text-content-tertiary">{{ $t('dashboard.studio.baselineValue') }} {{ format(baseline) }}</span>
    </template>
    <template v-if="widget.kind === 'bulletMetric'">
      <BulletMeter :value="metric ?? 0" :target="widget.target" :label="widget.name" :color="widget.color"/>
      <span class="text-xs text-content-secondary">{{ $t('dashboard.studio.targetValue') }} {{ number.format(widget.target) }}</span>
    </template>
    <EChart v-if="widget.kind === 'metricTrend'" :option="sparkline" class="min-h-12 flex-1"/>
  </div>
  <ol v-else-if="widget.kind === 'rankList' || widget.kind === 'distributionList'" class="h-full space-y-3 overflow-auto px-1 py-1">
    <li v-for="(point, index) in points" :key="point.pointId">
      <div class="mb-1 flex min-w-0 items-center gap-2 text-sm"><MfTag v-if="widget.kind === 'rankList'" size="sm">{{ index + 1 }}</MfTag><span class="min-w-0 flex-1 truncate" :title="point.category">{{ point.category }}</span><span class="shrink-0 tabular-nums">{{ widget.kind === 'distributionList' ? number.format(total > 0 ? Math.max(0, point.value ?? 0) / total * 100 : 0) + '%' : format(point.value) }}</span></div>
      <ProgressMeter :value="Math.max(0, point.value ?? 0)" :target="widget.kind === 'distributionList' ? total : maximum" :label="point.category" :color="widget.color"/>
    </li>
  </ol>
  <div v-else-if="widget.kind === 'statusGrid'" class="grid h-full content-start grid-cols-2 gap-2 overflow-auto">
    <div v-for="(row, index) in records" :key="index" class="grid min-w-0 gap-1 rounded-mf-md border border-line p-2">
      <span class="truncate text-xs text-content-secondary" :title="cellText(row[labelField])">{{ cellText(row[labelField]) }}</span>
      <MfTag :variant="positive.has(cellText(row[statusField]).toLowerCase()) ? 'success' : 'danger'" class="max-w-full truncate">{{ cellText(row[statusField]) }}</MfTag>
    </div>
  </div>
  <div v-else class="grid h-full content-start gap-2 overflow-auto">
    <dl v-for="(row, index) in records" :key="index" class="rounded-mf-md border border-line p-2 text-sm">
      <div v-for="field in fields" :key="field" class="grid grid-cols-2 gap-3 py-1"><dt class="truncate text-content-tertiary" :title="field">{{ field }}</dt><dd class="min-w-0 truncate" :title="cellText(row[field])">{{ cellText(row[field]) }}</dd></div>
    </dl>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { dashboardDataSettings, dashboardMetric, type DashboardWidget } from '@/model/dashboard-layout'
import AnimatedNumber from '@/views/components/visual/metrics/AnimatedNumber.vue'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'
import BulletMeter from '@/views/components/visual/metrics/BulletMeter.vue'
import EChart from '@/views/components/visual/EChart.vue'
const props = defineProps<{ widget: Pick<DashboardWidget, 'color' | 'dataOptions' | 'displayFields' | 'field' | 'kind' | 'name' | 'operation' | 'target'>; options: QueryChartOptions; rows: QueryChartRow[] }>()
const { locale } = useI18n()
const settings = computed(() => dashboardDataSettings(props.widget.dataOptions))
const number = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: settings.value.decimals }))
const format = (value: number | null) => value === null ? '—' : settings.value.prefix + number.value.format(value) + settings.value.suffix
const cellText = (value: unknown) => value == null ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value)
const result = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table', series: '' }))
const trend = computed(() => props.widget.kind === 'metricTrend')
const metric = computed(() => dashboardMetric(result.value.filtered, trend.value ? props.options.metric : props.widget.field, trend.value ? props.options.aggregation : props.widget.operation))
const baseline = computed(() => dashboardMetric(result.value.filtered, settings.value.baselineField, props.widget.operation))
const difference = computed(() => (metric.value ?? 0) - (baseline.value ?? 0))
const relative = computed(() => baseline.value ? difference.value / Math.abs(baseline.value) * 100 : undefined)
const points = computed(() => [...result.value.points].sort((left, right) => (right.value ?? 0) - (left.value ?? 0)).slice(0, settings.value.limit))
const total = computed(() => result.value.points.reduce((sum, point) => sum + Math.max(0, point.value ?? 0), 0))
const maximum = computed(() => Math.max(1, ...points.value.map(point => point.value ?? 0)))
const records = computed(() => result.value.filtered.slice(0, settings.value.limit))
const fields = computed(() => (props.widget.displayFields?.length ? props.widget.displayFields : props.options.fields).filter(field => props.options.fields.includes(field)).slice(0, 12))
const statusField = computed(() => settings.value.statusField || props.options.fields.find(field => /^(state|status)$/i.test(field)) || '')
const labelField = computed(() => props.widget.field || props.options.category || props.options.fields[0])
const positive = computed(() => new Set(settings.value.positiveValues.split(/[,，\n]/).map(value => value.trim().toLowerCase())))
const emptyReason = computed(() => {
  if (!result.value.filtered.length) return 'common.noData'
  if (props.widget.kind === 'metricComparison' && (!settings.value.baselineField || baseline.value === null)) return 'dashboard.studio.chooseBaseline'
  if (props.widget.kind === 'statusGrid' && !statusField.value) return 'dashboard.studio.chooseStatusField'
  if (['metricTicker', 'metricComparison', 'bulletMetric', 'metricTrend'].includes(props.widget.kind) && metric.value === null) return 'dashboard.studio.summaryNeedsField'
  if (['rankList', 'distributionList', 'metricTrend'].includes(props.widget.kind) && !result.value.points.length) return 'queryChart.issue.missingFields'
})
const sparkline = computed<EChartsOption>(() => ({ backgroundColor: 'transparent', animation: false, grid: { left: 2, right: 2, top: 8, bottom: 2 },
  tooltip: { trigger: 'axis', confine: true }, xAxis: { type: 'category', show: false, data: result.value.points.map(point => point.category) },
  yAxis: { type: 'value', show: false }, series: [{ type: 'line', data: result.value.points.map(point => point.value), smooth: true, symbol: 'none',
    lineStyle: { color: props.widget.color || '#079B70', width: 2 }, areaStyle: { color: props.widget.color || '#079B70', opacity: .12 } }] }))
</script>
