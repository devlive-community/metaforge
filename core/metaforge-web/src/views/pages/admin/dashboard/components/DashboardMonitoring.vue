<template>
  <MfEmpty v-if="empty" class="h-full" size="sm" :title="$t(empty)"/>
  <MfTable v-else-if="widget.kind === 'dataQuality'" fill density="compact" :columns="qualityColumns" :data-source="quality" row-key="field">
    <template #cell-field="{ record }"><EllipsisCell :text="record.field"/></template>
    <template #cell-completeness="{ record }">
      <MfTag size="sm" :variant="record.missing === 0 ? 'success' : 'warning'">{{ number.format(record.completeness) }}%</MfTag>
    </template>
  </MfTable>
  <div v-else-if="widget.kind === 'periodComparison' && period" class="flex h-full min-h-0 min-w-0 flex-col justify-center gap-2 overflow-auto">
    <div class="grid grid-cols-[repeat(auto-fit,minmax(min(100%,10rem),1fr))] gap-2">
      <div v-for="item in periods" :key="item.key" class="min-w-0 rounded-mf-md bg-surface-hover px-3 py-2">
        <span class="text-xs text-content-secondary">{{ $t('dashboard.studio.monitoring.' + item.key) }}</span>
        <strong class="block truncate text-xl tabular-nums" :title="format(item.value)">{{ format(item.value) }}</strong>
        <div class="break-words text-xs tabular-nums text-content-tertiary">{{ item.range.join(' – ') }}</div>
      </div>
    </div>
    <div class="flex flex-wrap items-center gap-2 text-xs">
      <span class="text-content-secondary">{{ $t('dashboard.studio.monitoring.periodChange') }}</span>
      <MfTag :variant="period.difference === null || period.difference === 0 ? 'default' : period.difference > 0 ? 'success' : 'danger'" size="sm">
        {{ period.difference !== null && period.difference > 0 ? '+' : '' }}{{ format(period.difference) }}
        <template v-if="period.percent !== null"> ({{ period.percent > 0 ? '+' : '' }}{{ number.format(period.percent) }}%)</template>
      </MfTag>
    </div>
  </div>
  <div v-else-if="widget.kind === 'dataFreshness' && freshness" class="flex h-full min-h-0 min-w-0 flex-col justify-center gap-1 overflow-auto">
    <div class="flex flex-wrap items-center gap-2 text-xs text-content-secondary">
      {{ $t('dashboard.studio.monitoring.latestRecord') }}
      <MfTag size="sm" :variant="freshness.state === 'fresh' ? 'success' : 'warning'">{{ $t('dashboard.studio.monitoring.' + freshness.state) }}</MfTag>
    </div>
    <time :datetime="new Date(freshness.latest).toISOString()" class="break-words text-base leading-tight font-semibold tabular-nums">{{ formatDateTime(freshness.latest) }}</time>
    <span v-if="freshness.age >= 0" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.monitoring.ageHours', { hours: ageFormat.format(freshness.age) }) }}</span>
  </div>
  <div v-else class="h-full min-h-0 overflow-auto">
    <p class="mb-2 break-words text-xs tabular-nums text-content-tertiary">{{ sparklines.labels[0] }} – {{ sparklines.labels.at(-1) }}</p>
    <div class="grid grid-cols-[repeat(auto-fit,minmax(min(100%,9rem),1fr))] gap-2">
      <section v-for="item in miniCharts" :key="item.name" class="min-w-0 rounded-mf-md border border-line px-3 py-2">
        <EllipsisCell :text="item.name"/>
        <strong class="block truncate text-base tabular-nums" :title="format(item.total)">{{ format(item.total) }}</strong>
        <EChart class="h-12" :option="item.option"/>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import { aggregateQueryChart, chartFieldKind, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { dashboardDataSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardFieldQuality, dashboardFreshness, dashboardPeriodComparison, dashboardSparklines } from '@/model/dashboard-monitoring'
import { useChartColors } from '@/composables/useChartColors'
import { formatDateTime } from '@/utils/date'
import EChart from '@/views/components/visual/EChart.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

const props = defineProps<{ widget: DashboardWidget; rows: QueryChartRow[]; options: QueryChartOptions; editable?: boolean }>()
const { t, locale } = useI18n()
const colors = useChartColors()
const settings = computed(() => dashboardDataSettings(props.widget.dataOptions))
const filtered = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const dateField = computed(() => props.widget.timeField || props.options.fields.find(field => chartFieldKind(props.rows, field) === 'date') || '')
const category = computed(() => props.widget.chartOptions?.category || props.options.category)
const fields = computed(() => (props.widget.displayFields?.length ? props.widget.displayFields : props.options.fields).slice(0, 12))
const number = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: settings.value.decimals }))
const ageFormat = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 1 }))
function format(value: number | null) { return value === null ? '—' : settings.value.prefix + number.value.format(value) + settings.value.suffix }

const quality = computed(() => dashboardFieldQuality(filtered.value, fields.value))
const qualityColumns = computed(() => [
  { key: 'field', dataIndex: 'field', title: t('dashboard.studio.statistics.field'), width: '26%' },
  ...['present', 'missing', 'distinct', 'completeness'].map(key => ({ key, dataIndex: key, title: t('dashboard.studio.monitoring.' + key), width: '18.5%' })),
])
const period = computed(() => dashboardPeriodComparison(filtered.value, dateField.value, props.widget.field, props.widget.operation, settings.value.periodDays))
const periods = computed(() => !period.value ? [] : [
  { key: 'currentPeriod', range: period.value.currentRange, value: period.value.current },
  { key: 'previousPeriod', range: period.value.previousRange, value: period.value.previous },
])
const now = ref(Date.now())
watch([() => props.widget.kind, () => props.editable], ([kind, editable], _, cleanup) => {
  if (kind !== 'dataFreshness' || editable) return
  now.value = Date.now()
  const timer = setInterval(() => { now.value = Date.now() }, 60_000)
  cleanup(() => clearInterval(timer))
}, { immediate: true })
const freshness = computed(() => dashboardFreshness(filtered.value, dateField.value, now.value, settings.value.staleHours))
const sparklines = computed(() => dashboardSparklines(filtered.value, dateField.value, category.value, props.widget.field, props.widget.operation, settings.value.periodDays, Math.min(8, settings.value.limit)))
const miniCharts = computed(() => sparklines.value.items.map((item, index) => {
  const option: EChartsOption = {
    animation: false, grid: { left: 4, right: 4, top: 5, bottom: 5 },
    xAxis: { type: 'category', show: false, data: sparklines.value.labels }, yAxis: { type: 'value', show: false, ...sparklines.value.bounds },
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    color: [props.widget.color || colors.value.colors[index % colors.value.colors.length]],
    series: [{ type: 'line', name: item.name, data: item.values,
      showSymbol: item.values.length <= 12 || !item.values.some((value, index) => value !== null && index > 0 && item.values[index - 1] !== null), symbolSize: 4 }],
  }
  return { ...item, option }
}))
const empty = computed(() => {
  if (props.widget.kind !== 'dataQuality' && !dateField.value) return 'dashboard.studio.chooseDateField'
  if (['periodComparison', 'sparklineGrid'].includes(props.widget.kind) && props.widget.operation !== 'count' && !props.widget.field) return 'dashboard.studio.summaryNeedsField'
  if (props.widget.kind === 'sparklineGrid' && !category.value) return 'queryChart.selectField'
  const available = props.widget.kind === 'dataQuality' ? quality.value.length : props.widget.kind === 'periodComparison' ? period.value
    : props.widget.kind === 'dataFreshness' ? freshness.value : sparklines.value.items.length
  return available ? '' : 'common.noData'
})
</script>
