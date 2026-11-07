<template>
  <ChartLoading v-if="loading"/>
  <template v-else-if="dashboardFilterTool(widget.kind)">
    <DashboardFilterControl v-if="snapshot" :widget="widget" :options="snapshot.queryChart.options" :rows="source" :editable="editable"/>
    <MfEmpty v-else class="h-full" size="sm" :title="$t(emptyMessage)"/>
  </template>
  <DashboardUtilityContent v-else-if="widget.kind === 'statusLegend' || widget.kind === 'copyText'" :widget="widget" :editable="editable"/>
  <DashboardInspectionChecklist v-else-if="widget.kind === 'inspectionChecklist'" :widget="widget" :editable="editable"/>
  <div v-else-if="widget.kind === 'metric'" class="flex h-full min-h-0 flex-col justify-center px-1">
    <strong class="truncate text-3xl font-extrabold tabular-nums" :title="metricValue">{{ metricValue }}</strong>
    <span class="mt-1 truncate text-xs text-content-tertiary">{{ report?.name || $t('dashboard.studio.chooseAsset') }}</span>
  </div>
  <template v-else-if="widget.kind === 'progress' || widget.kind === 'gauge'">
    <MfEmpty v-if="metric === null" class="h-full" size="sm" :title="$t(report ? 'common.noData' : 'dashboard.studio.chooseAsset')"/>
    <EChart v-else-if="widget.kind === 'gauge'" :option="gauge" class="size-full"/>
    <div v-else class="flex h-full min-h-0 flex-col justify-center gap-2 px-1">
      <div class="flex flex-wrap items-baseline justify-between gap-1 tabular-nums">
        <span class="font-semibold">{{ metricValue }} / {{ formatMetric(widget.target) }}</span>
        <span class="text-xs text-content-secondary">{{ formatMetric(progressPercent) }}%</span>
      </div>
      <DashboardProgress :value="metric ?? 0" :target="widget.target" :label="widget.name" :color="widget.color" :aria-valuetext="metricValue"/>
    </div>
  </template>
  <DashboardClock v-else-if="widget.kind === 'clock'" :show-date="widget.clockDate"/>
  <div v-else-if="widget.kind === 'shape'" class="flex size-full items-center justify-center [container-type:size]">
    <div class="dashboard-shape size-full border border-primary/50 bg-primary/15"
         :class="widget.shape === 'circle' ? 'dashboard-shape-circle' : 'rounded-mf-sm'"
         :style="{ backgroundColor: widget.color || undefined }"/>
  </div>
  <div v-else-if="widget.kind === 'link' && resourceUrl" class="flex size-full items-center justify-center">
    <a :href="resourceUrl" target="_blank" rel="noopener noreferrer"
       class="inline-flex min-w-0 items-center gap-1.5 rounded-mf-md border border-line px-3 py-1.5 text-sm font-medium text-primary hover:bg-surface-hover">
      <ExternalLink :size="15" class="shrink-0"/><span class="truncate">{{ widget.text || widget.name }}</span>
    </a>
  </div>
  <video v-else-if="widget.kind === 'video' && resourceUrl" :key="resourceUrl" :src="resourceUrl" controls playsinline preload="none"
         :aria-label="widget.name" class="size-full bg-black object-contain"/>
  <div v-else-if="widget.kind === 'heading'" class="flex h-full flex-col justify-center px-1" :class="textAlignment">
    <strong class="break-words text-2xl font-semibold">{{ widget.text || widget.name }}</strong>
  </div>
  <div v-else-if="widget.kind === 'note'" class="flex h-full flex-col justify-center overflow-auto">
    <MfAlert :variant="widget.tone" :title="widget.text || widget.name"/>
  </div>
  <div v-else-if="widget.kind === 'badge'" class="flex h-full items-center justify-center">
    <MfTag :variant="widget.tone" class="max-w-full truncate">{{ widget.text || widget.name }}</MfTag>
  </div>
  <div v-else-if="widget.kind === 'audio' && resourceUrl" class="flex h-full items-center justify-center">
    <audio :key="resourceUrl" :src="resourceUrl" controls preload="none" :aria-label="widget.name" class="w-full"/>
  </div>
  <div v-else-if="widget.kind === 'verticalDivider'" class="flex h-full justify-center"><div class="h-full w-px bg-line"/></div>
  <div v-else-if="widget.kind === 'sectionDivider'" class="flex h-full items-center">
    <MfDivider :label="widget.text || widget.name"/>
  </div>
  <div v-else-if="widget.kind === 'panel'" class="size-full rounded-mf-md border border-primary/25 bg-primary/8"
       :style="{ backgroundColor: widget.color || undefined }"/>
  <div v-else-if="widget.kind === 'spacer'" class="size-full"/>
  <DashboardExtendedContent v-else-if="dashboardExtendedContentTools.some(kind => kind === widget.kind)" :widget="widget"/>
  <DashboardAdvancedContent v-else-if="dashboardAdvancedContentTools.some(kind => kind === widget.kind)" :widget="widget" :editable="editable"/>
  <DashboardContentView v-else-if="['imageText', 'bulletList', 'linkList', 'countdown', 'icon'].includes(widget.kind)" :widget="widget"/>
  <DashboardAdvancedDecoration v-else-if="dashboardAdvancedDecorationTools.some(kind => kind === widget.kind)" :widget="widget" :editable="editable"/>
  <DashboardDecoration v-else-if="['borderFrame', 'gridPanel', 'gradientPanel', 'horizontalArrow', 'verticalArrow', 'glowLine', 'glowRing', 'dotMatrix', 'cornerAccent', 'ripple', 'ribbon'].includes(widget.kind)" :widget="widget"/>
  <DashboardOperationsView v-else-if="options && dashboardOperationDataTool(widget.kind)" :widget="widget" :options="options" :rows="rows" :editable="editable"/>
  <DashboardMonitoring v-else-if="options && dashboardMonitoringTools.some(kind => kind === widget.kind)" :widget="widget" :options="options" :rows="rows" :editable="editable"/>
  <DashboardExtendedData v-else-if="options && dashboardExtendedDataTools.some(kind => kind === widget.kind)" :widget="widget" :options="options" :rows="rows" :editable="editable"/>
  <DashboardAdvancedData v-else-if="options && dashboardAdvancedDataTools.some(kind => kind === widget.kind)" :widget="widget" :options="options" :rows="rows"/>
  <DashboardDataDisplay v-else-if="options && dashboardDataTool(widget.kind)" :kind="widget.kind" :options="options" :rows="rows" :field="widget.field"
                        :display-fields="widget.displayFields" :record-index="widget.recordIndex" :time-field="widget.timeField"
                        :operation="widget.operation" :target="widget.target" :color="widget.color"/>
  <div v-else-if="widget.kind === 'text'" class="h-full overflow-auto whitespace-pre-wrap break-words px-1 text-sm" :class="textAlignment">{{ widget.text }}</div>
  <img v-else-if="widget.kind === 'image' && imageSource" :src="imageSource" :alt="widget.name" class="size-full object-contain"/>
  <div v-else-if="widget.kind === 'divider'" class="flex h-full items-center"><hr class="w-full border-line"/></div>
  <div v-else-if="widget.kind === 'container'" class="size-full rounded-mf-md border border-dashed border-line"/>
  <template v-else-if="snapshot && options">
    <QueryChartPreview class="size-full" :options="options" :rows="rows" :aggregation="ranking" embedded linked @filter="emit('filter', $event)"/>
  </template>
  <VisualView v-else-if="legacy && widget.kind === 'report'"
              :key="report?.code" :configuration="legacy" :query="legacyQuery" :type="report?.type"
              :code="report?.dataset?.code" :original="report?.source?.code" height="100%"/>
  <MfEmpty v-else class="h-full" size="sm" :title="$t(emptyMessage)"/>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { ExternalLink } from '@lucide/vue'
import DashboardClock from './DashboardClock.vue'
import DashboardDataDisplay from './DashboardDataDisplay.vue'
import DashboardContentView from './DashboardContentView.vue'
import DashboardDecoration from './DashboardDecoration.vue'
import DashboardProgress from './DashboardProgress.vue'
import DashboardAdvancedContent from './DashboardAdvancedContent.vue'
import DashboardAdvancedData from './DashboardAdvancedData.vue'
import DashboardExtendedData from './DashboardExtendedData.vue'
import DashboardExtendedContent from './DashboardExtendedContent.vue'
import DashboardFilterControl from './DashboardFilterControl.vue'
import DashboardMonitoring from './DashboardMonitoring.vue'
import DashboardUtilityContent from './DashboardUtilityContent.vue'
import DashboardOperationsView from './DashboardOperationsView.vue'
import DashboardInspectionChecklist from './DashboardInspectionChecklist.vue'
import DashboardAdvancedDecoration from './DashboardAdvancedDecoration.vue'
import ChartLoading from '@/views/components/visual/ChartLoading.vue'
import ChartError from '@/views/components/visual/ChartError.vue'
import EChart from '@/views/components/visual/EChart.vue'
import type { EChartsOption } from 'echarts'
import { aggregateQueryChart, chartFieldKind, defaultQueryChart, type QueryChartConfiguration, type QueryChartRow } from '@/model/query-chart'
import { dashboardExtendedDataTools, dashboardExtendedContentTools, dashboardAdvancedContentTools, dashboardAdvancedDataTools, dashboardAdvancedDecorationTools, dashboardChartMappings, dashboardDataTool, dashboardFilterTool, dashboardMetric, dashboardMetricTool, dashboardMonitoringTools, dashboardOperationDataTool, dashboardResourceUrl, dashboardRowsInPeriod, parseDashboardJson, type DashboardWidget } from '@/model/dashboard-layout'
import type { ReportModel } from '@/model/report'
import type { Configuration } from '@/views/components/visual/Configuration'

const QueryChartPreview = defineAsyncComponent({ loader: () => import('@/views/components/visual/query/QueryChartPreview.vue'), loadingComponent: ChartLoading, errorComponent: ChartError, delay: 0 })
const VisualView = defineAsyncComponent({ loader: () => import('@/views/components/visual/VisualView.vue'), loadingComponent: ChartLoading, errorComponent: ChartError, delay: 0 })

const props = defineProps<{ widget: DashboardWidget; report?: ReportModel; snapshot?: QueryChartConfiguration; refreshed?: QueryChartRow[]; selection?: ReadonlySet<QueryChartRow>; days: number; wall?: boolean; editable?: boolean; loading?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { locale } = useI18n()
const snapshot = computed(() => props.snapshot)
const source = computed(() => props.refreshed ?? snapshot.value?.columns ?? [])
const dateField = computed(() => {
  const saved = snapshot.value?.queryChart.options
  const fields = [props.widget.chartOptions?.category || saved?.category, ...(saved?.fields ?? [])]
  return fields.find((name): name is string => !!name && chartFieldKind(source.value, name) === 'date') || ''
})
const rows = computed(() => {
  const filtered = dashboardRowsInPeriod(source.value, dateField.value, props.days)
  return props.selection ? filtered.filter(row => props.selection?.has(row)) : filtered
})
const options = computed(() => {
  if (!snapshot.value) return
  const saved = snapshot.value.queryChart.options
  const mappings = dashboardChartMappings(props.widget, snapshot.value)
  const action = props.widget.action === 'saved' ? saved.pointAction : props.widget.action
  const grain = mappings.category === saved.category ? saved.grain : 'none'
  return {
    ...defaultQueryChart(saved.fields), ...saved, ...mappings, grain, name: '', description: '', transparent: true,
    metric: props.widget.chartOptions?.metric ?? saved.metric,
    aggregation: props.widget.chartOptions?.aggregation ?? saved.aggregation,
    pointAction: action, detailsMode: 'dialog' as const,
    colors: props.wall ? '#15e5d1,#10b8e2,#6379ff' : saved.colors
  }
})
const ranking = computed(() => {
  if (props.widget.kind !== 'rank' || !options.value) return
  const result = aggregateQueryChart(rows.value, options.value)
  return { ...result, points: [...result.points].sort((a, b) => (a.value ?? 0) - (b.value ?? 0)) }
})
const metric = computed(() => {
  if (!dashboardMetricTool(props.widget.kind) || !options.value) return null
  const filtered = aggregateQueryChart(rows.value, { ...options.value, type: 'table' }).filtered
  return dashboardMetric(filtered, props.widget.field, props.widget.operation)
})
const numberFormat = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 2 }))
const formatMetric = (value: number) => numberFormat.value.format(value)
const metricValue = computed(() => metric.value === null ? '—' : formatMetric(metric.value))
const progressPercent = computed(() => Math.max(0, Math.min(100, (metric.value ?? 0) / props.widget.target * 100)))
const gauge = computed<EChartsOption>(() => ({
  animation: false,
  series: [{
    type: 'gauge', radius: '95%', min: 0, max: 100,
    progress: { show: true, roundCap: true, width: 10, itemStyle: { color: props.widget.color || (props.wall ? '#15e5d1' : '#079b70') } },
    pointer: { show: false },
    axisLine: { lineStyle: { width: 10, color: [[1, props.wall ? '#19556a' : '#dce7e3']] } },
    axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false },
    detail: { formatter: () => metricValue.value, color: props.wall ? '#e3fbff' : '#24352e', fontSize: 16, offsetCenter: [0, '18%'] },
    data: [{ value: progressPercent.value }],
  }],
}))
const legacy = computed(() => {
  const value = parseDashboardJson(props.report?.configure)
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Configuration : null
})
const legacyQuery = computed(() => props.report?.type === 'DATASET' ? parseDashboardJson(props.report.query) : props.report?.query)
const textAlignment = computed(() => ({ left: 'text-left', center: 'text-center', right: 'text-right' })[props.widget.alignment])
const imageSource = computed(() => dashboardResourceUrl(props.widget.image))
const resourceUrl = computed(() => dashboardResourceUrl(props.widget.url))
const emptyMessage = computed(() => {
  if (props.widget.kind === 'image') return 'dashboard.studio.chooseImage'
  if (['link', 'video', 'audio'].includes(props.widget.kind)) return 'dashboard.studio.chooseUrl'
  return props.report ? 'dashboard.studio.snapshotRequired' : 'dashboard.studio.chooseAsset'
})
</script>

<style scoped>
.dashboard-shape-circle { width: min(100cqw, 100cqh); height: auto; aspect-ratio: 1; max-height: 100%; border-radius: 50%; }
</style>
