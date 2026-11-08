<template>
  <MfEmpty v-if="message" class="h-full" size="sm" :title="$t(message)"/>
  <ResultTable v-else-if="kind === 'pivot' && pivot" fill :headers="pivot.headers" :rows="pivot.rows" :page-size="50"/>
  <dl v-else-if="kind === 'dataList'" class="h-full overflow-auto divide-y divide-line">
    <div v-for="point in result.points" :key="point.pointId" class="flex items-center justify-between gap-3 px-2 py-1.5 text-sm">
      <dt class="min-w-0 flex-1 truncate" :title="point.category">{{ point.category }}</dt>
      <dd class="shrink-0 font-medium tabular-nums">{{ formatValue(point.value) }}</dd>
    </div>
  </dl>
  <dl v-else-if="kind === 'metricGrid'" class="grid h-full content-start grid-cols-2 gap-2 overflow-auto">
    <div v-for="field in fields" :key="field" class="min-w-0 rounded-mf-md border border-line px-3 py-2">
      <dt class="truncate text-xs text-content-secondary" :title="field">{{ field }}</dt>
      <dd class="mt-1 truncate text-xl font-semibold tabular-nums">{{ formatValue(dashboardMetric(result.filtered, field, operation || 'sum')) }}</dd>
    </div>
  </dl>
  <dl v-else-if="kind === 'recordDetail'" class="h-full overflow-auto divide-y divide-line text-sm">
    <div v-for="field in fields" :key="field" class="grid grid-cols-[minmax(0,1fr)_minmax(0,2fr)] gap-3 px-1 py-2">
      <dt class="truncate text-content-secondary" :title="field">{{ field }}</dt>
      <dd class="min-w-0 break-words"><span v-if="record?.[field] == null" class="text-content-tertiary">NULL</span><span v-else>{{ cellText(record?.[field]) }}</span></dd>
    </div>
  </dl>
  <div v-else-if="kind === 'progressList'" class="h-full space-y-3 overflow-auto px-1 py-1">
    <div v-for="point in limitedPoints" :key="point.pointId" class="min-w-0">
      <div class="mb-1 flex items-center gap-2 text-xs"><span class="min-w-0 flex-1 truncate" :title="point.category">{{ point.category }}</span><span class="tabular-nums">{{ formatValue(point.value) }} / {{ formatValue(target ?? 100) }}</span></div>
      <ProgressMeter :value="point.value ?? 0" :target="target ?? 100" :label="point.category" :color="color"/>
    </div>
  </div>
  <div v-else-if="kind === 'dataTags'" class="flex h-full content-start flex-wrap gap-2 overflow-auto p-1">
    <MfTag v-for="point in limitedPoints" :key="point.pointId" class="max-w-full" :title="point.category">
      <span class="truncate">{{ point.category }}</span><span class="ml-1.5 tabular-nums opacity-70">{{ formatValue(point.value) }}</span>
    </MfTag>
  </div>
  <ol v-else-if="kind === 'dataTimeline'" class="h-full overflow-auto px-2 py-1">
    <li v-for="(item, index) in timeline" :key="index" class="relative border-l border-line pb-4 pl-4 last:pb-0">
      <span class="absolute top-1 -left-1 size-2 rounded-full bg-primary"/>
      <time class="text-xs text-content-tertiary">{{ formatDate(item.timestamp, 'YYYY-MM-DD HH:mm', { timeZone: options.timezone }) }}</time>
      <p class="mt-1 break-words text-sm">{{ cellText(item.row[field || timelineLabel]) }}</p>
    </li>
  </ol>
  <dl v-else class="grid h-full content-start grid-cols-2 gap-2 overflow-auto">
    <div v-for="item in summary" :key="item.key" class="min-w-0 rounded-mf-md border border-line px-2 py-1.5">
      <dt class="truncate text-xs text-content-secondary">{{ $t('dashboard.studio.summary.' + item.key) }}</dt>
      <dd class="truncate font-semibold tabular-nums" :title="formatValue(item.value)">{{ formatValue(item.value) }}</dd>
    </div>
  </dl>
</template>

<script setup lang="ts">
import { formatDate } from '@/utils/date'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ResultTable from '@/views/components/table/ResultTable.vue'
import { aggregateQueryChart, chartFieldKind, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { dashboardDataAggregationTool, dashboardMetric, dashboardPivot, type DashboardDataTool, type DashboardWidget } from '@/model/dashboard-layout'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'

const props = defineProps<{ kind: DashboardDataTool; options: QueryChartOptions; rows: QueryChartRow[]; field: string;
  displayFields?: string[]; recordIndex?: number; timeField?: string; operation?: DashboardWidget['operation']; target?: number; color?: string }>()
const { locale } = useI18n()
const numberFormat = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 2 }))
const formatValue = (value: number | null) => value === null ? '—' : numberFormat.value.format(value)
const cellText = (value: unknown) => value == null ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value)
const fields = computed(() => (props.displayFields?.length ? props.displayFields : props.options.fields)
  .filter(field => props.options.fields.includes(field) && (props.kind !== 'metricGrid' || chartFieldKind(props.rows, field) === 'number')).slice(0, 12))
const result = computed(() => aggregateQueryChart(props.rows, {
  ...props.options, type: 'table', series: props.kind === 'pivot' ? props.options.series : '',
}))
const pivot = computed(() => props.kind === 'pivot' ? dashboardPivot(result.value.points, props.options.category) : undefined)
const record = computed(() => result.value.filtered[(props.recordIndex ?? 1) - 1])
const limitedPoints = computed(() => result.value.points.slice(0, 50))
const timelineField = computed(() => props.timeField || props.options.fields.find(field => chartFieldKind(props.rows, field) === 'date') || '')
const timelineLabel = computed(() => props.options.fields.find(field => field !== timelineField.value) || timelineField.value)
const timeline = computed(() => result.value.filtered.map(row => ({ row, timestamp: new Date(String(row[timelineField.value] ?? '')).getTime() }))
  .filter(item => Number.isFinite(item.timestamp)).sort((left, right) => right.timestamp - left.timestamp).slice(0, 50))
const summary = computed(() => [
  { key: 'rows', value: result.value.filtered.length },
  ...(['min', 'max', 'avg'] as const).map(key => ({ key, value: dashboardMetric(result.value.filtered, props.field, key) })),
])
const message = computed(() => {
  if (props.kind === 'pivot' && (!props.options.series || props.options.category === props.options.series)) {
    return 'dashboard.studio.pivotNeedsFields'
  }
  const aggregated = dashboardDataAggregationTool(props.kind)
  if (aggregated && !props.options.category) return 'queryChart.issue.missingFields'
  if (aggregated && props.options.aggregation !== 'count' && !props.options.metric) return 'queryChart.issue.missingFields'
  if (props.kind === 'dataSummary' && !props.field) return 'dashboard.studio.summaryNeedsField'
  if (!result.value.filtered.length || (aggregated && !result.value.points.length)) return 'common.noData'
  if ((props.kind === 'metricGrid' || props.kind === 'recordDetail') && !fields.value.length) return 'dashboard.studio.summaryNeedsField'
  if (props.kind === 'recordDetail' && !record.value) return 'dashboard.studio.recordUnavailable'
  if (props.kind === 'dataTimeline' && (!timelineField.value || !timeline.value.length)) return 'dashboard.studio.timelineNeedsDate'
  if (props.kind === 'pivot' && !pivot.value) return 'dashboard.studio.pivotTooWide'
})
</script>
