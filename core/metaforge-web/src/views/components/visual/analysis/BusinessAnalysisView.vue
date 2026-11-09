<template>
  <MfEmpty v-if="emptyKey" class="h-full" size="sm" :title="$t(emptyKey)"/>
  <div v-else-if="kind === 'stageConversion'" class="h-full min-h-0 space-y-2 overflow-auto">
    <ol class="space-y-1.5">
      <li v-for="(item, index) in stages" :key="item.name" class="rounded-mf-md border border-line px-2.5 py-1.5">
        <div class="mb-1 flex items-center gap-2 text-sm">
          <span class="shrink-0 rounded-mf-sm bg-primary/10 px-1.5 py-0.5 text-xs tabular-nums text-primary">{{ index + 1 }}</span>
          <EllipsisCell :text="item.name" class="min-w-0 flex-1 font-medium"/>
          <strong class="shrink-0 tabular-nums">{{ settings.entityField || !widget.field ? integer(item.value) : metric(item.value) }}</strong>
        </div>
        <ProgressMeter v-if="item.overallRate !== null" :value="item.overallRate" :target="100" :label="item.name"/>
        <div class="mt-1 flex flex-wrap gap-x-4 gap-y-0.5 text-xs tabular-nums text-content-secondary">
          <span v-if="index">{{ $t('dashboard.studio.business.stepRate', { rate: percentage(item.stepRate) }) }}</span>
          <span>{{ $t('dashboard.studio.business.overallRate', { rate: percentage(item.overallRate) }) }}</span>
        </div>
      </li>
    </ol>
    <p v-if="widget.field && !settings.entityField" class="text-xs text-content-tertiary">{{ $t('dashboard.studio.business.nonNegativeHint') }}</p>
  </div>
  <div v-else-if="kind === 'recencyDistribution' || kind === 'valueBands'" class="h-full min-h-0 overflow-auto">
    <div class="mb-2 flex flex-wrap items-center gap-2 text-xs">
      <MfTag size="sm" variant="primary">{{ $t('dashboard.studio.business.' + (kind === 'recencyDistribution' ? 'entityCount' : 'validCount'), { count: integer(distribution.total) }) }}</MfTag>
      <span v-if="kind === 'recencyDistribution'" class="text-content-secondary">{{ $t('dashboard.studio.business.asOf', { date: recency.latest }) }}</span>
    </div>
    <ol class="space-y-1.5">
      <li v-for="item in distribution.items" :key="item.name" class="rounded-mf-md border border-line px-2.5 py-1">
        <div class="mb-1 flex flex-wrap items-baseline justify-between gap-x-3 gap-y-0.5 text-xs">
          <span class="break-words font-medium">{{ item.name }}</span>
          <span class="tabular-nums text-content-secondary">{{ integer(item.count) }} · {{ percentage(item.percent) }}</span>
        </div>
        <ProgressMeter :value="item.percent" :target="100" :label="item.name"/>
      </li>
    </ol>
  </div>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <div v-if="table.badges?.length" class="flex shrink-0 flex-wrap items-center gap-1.5">
      <MfTag v-for="item in table.badges" :key="item" size="sm" variant="primary">{{ item }}</MfTag>
    </div>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.items" row-key="key" :row-class="rowClass">
      <template #cell-name="{ record }">
        <EllipsisCell :text="record.name" :class="record.depth ? 'pl-3 text-content-secondary' : 'font-medium'"/>
      </template>
      <template v-for="column in table.columns.slice(1)" :key="column.key" #[`cell-${column.key}`]="{ record }">
        <span class="tabular-nums" :class="cellClass(record.cells[column.key])" :title="record.cells[column.key]?.hint" :style="heatStyle(record.cells[column.key])">
          {{ record.cells[column.key]?.text ?? '—' }}
        </span>
      </template>
    </MfTable>
    <p v-if="table.note" class="shrink-0 text-xs text-content-tertiary">{{ table.note }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MfTableColumn } from 'metaforge-ui'
import { dashboardBusinessSettings, dashboardDataSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardBandBoundaries, dashboardCohortRetention, dashboardPareto, dashboardRecencyDistribution, dashboardSegmentSummary, dashboardStageConversion,
  dashboardTargetGap, dashboardValueBands, dashboardVarianceDrivers } from '@/model/dashboard-business'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'businessOptions' | 'dataOptions' | 'field' | 'kind' | 'operation' | 'timeField'>; options: QueryChartOptions; rows: QueryChartRow[] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.business.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardBusinessSettings(props.widget.businessOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const rows = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const limit = computed(() => Math.min(30, data.value.limit))
const number = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const counts = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const pct = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: Math.min(2, data.value.decimals) }))
const integer = (value: number | null) => value === null ? '—' : counts.value.format(value)
const metric = (value: number | null) => value === null ? '—' : data.value.prefix + number.value.format(value) + data.value.suffix
const percentage = (value: number | null) => value === null ? '—' : pct.value.format(value) + '%'
const nameOf = (name: string) => name || t(key + 'missingGroup')
const stages = computed(() => dashboardStageConversion(rows.value, settings.value.groupField, settings.value.stageOrder, props.widget.field, settings.value.entityField))
const targets = computed(() => dashboardTargetGap(rows.value, settings.value.groupField, props.widget.field, settings.value.referenceField, settings.value.targetAggregation))
const segments = computed(() => dashboardSegmentSummary(rows.value, settings.value.groupField, settings.value.subgroupField, props.widget.field,
  props.widget.operation === 'sum' || props.widget.operation === 'avg' ? props.widget.operation : 'count', limit.value))
const pareto = computed(() => dashboardPareto(rows.value, settings.value.groupField, props.widget.field, settings.value.paretoCutoff))
const drivers = computed(() => dashboardVarianceDrivers(rows.value, settings.value.groupField, props.widget.field, settings.value.referenceField))
const cohorts = computed(() => dashboardCohortRetention(rows.value, settings.value.entityField, props.widget.timeField || '', settings.value.period, settings.value.periods, limit.value))
const recency = computed(() => dashboardRecencyDistribution(rows.value, settings.value.entityField, props.widget.timeField || ''))
const boundaries = computed(() => dashboardBandBoundaries(settings.value.boundaries))
const bands = computed(() => dashboardValueBands(rows.value, props.widget.field, boundaries.value ?? []))
const exactBoundary = (value: number) => data.value.prefix + String(value) + data.value.suffix
const distribution = computed(() => kind.value === 'recencyDistribution'
  ? { total: recency.value.total, items: recency.value.buckets.map(item => ({ ...item, name: t(key + 'recency.' + item.index) })) }
  : { total: bands.value.total, items: bands.value.items.map(item => ({ ...item, name: item.lower === undefined ? '< ' + exactBoundary(item.upper!)
    : item.upper === undefined ? '≥ ' + exactBoundary(item.lower) : '≥ ' + exactBoundary(item.lower) + ' · < ' + exactBoundary(item.upper) })) })

type Cell = { text: string; delta?: number | null; heat?: number; hint?: string }
type Item = { key: string; name: string; cells: Record<string, Cell>; depth?: number; emphasis?: boolean }
type Table = { columns: (MfTableColumn & { key: string })[]; items: Item[]; badges?: string[]; note?: string }
function column(name: string, options: Partial<MfTableColumn> = {}, translation = key + name) {
  return { title: t(translation), dataIndex: name, key: name, align: name === 'name' ? 'left' as const : 'right' as const, ...options }
}
const namedColumn = (label = 'group') => column('name', { width: 125 }, key + label)
const cell = (text: string, extra?: Omit<Cell, 'text'>) => ({ text, ...extra })
const delta = (value: number | null) => cell((value !== null && value > 0 ? '+' : '') + metric(value), { delta: value })
const table = computed<Table>(() => {
  if (kind.value === 'targetGap') return {
    columns: [namedColumn(), column('actual'), column('target'), column('gap'), column('attainment')],
    items: targets.value.slice(0, limit.value).map(item => ({ key: item.name, name: nameOf(item.name), cells: { actual: cell(metric(item.actual)),
      target: cell(metric(item.target)), gap: delta(item.gap), attainment: cell(percentage(item.attainment)) } })),
  }
  if (kind.value === 'segmentSummary') return {
    columns: [namedColumn(), column('rowCount'), ...(props.widget.operation === 'sum' || props.widget.operation === 'avg' ? [column('value')] : [])],
    items: [segments.value.total, ...segments.value.items].map(item => ({ key: item.key, name: item.type === 'total' ? t(key + 'allGroups') : nameOf(item.name),
      depth: item.type === 'child' ? 1 : 0, emphasis: item.type === 'total', cells: { rowCount: cell(integer(item.count)), value: cell(metric(item.value)) } })),
  }
  if (kind.value === 'paretoTable') return {
    columns: [namedColumn(), column('value'), column('share'), column('cumulative')],
    items: pareto.value.items.slice(0, limit.value).map(item => ({ key: item.name, name: nameOf(item.name), emphasis: item.reachesCutoff,
      cells: { value: cell(props.widget.field ? metric(item.value) : integer(item.value)), share: cell(percentage(item.share)), cumulative: cell(percentage(item.cumulative)) } })),
    badges: [t(key + 'cutoffValue', { value: settings.value.paretoCutoff })],
    note: pareto.value.excluded ? t(key + 'negativeExcluded', { count: pareto.value.excluded }) : '',
  }
  if (kind.value === 'varianceDrivers') return {
    columns: [namedColumn(), column('gap')],
    items: drivers.value.items.slice(0, limit.value).map(item => ({ key: item.name, name: nameOf(item.name), cells: { gap: delta(item.delta) } })),
    badges: [t(key + 'baselineValue', { value: metric(drivers.value.baseline) }), t(key + 'actualValue', { value: metric(drivers.value.actual) }),
      t(key + 'netValue', { value: delta(drivers.value.net).text })],
    note: t(key + 'pairedSamples', { count: drivers.value.samples }),
  }
  const periods = Array.from({ length: settings.value.periods }, (_, index) => column('period' + index,
    { width: 74, title: t(key + (settings.value.period === 'day' ? 'dayIndex' : 'weekIndex'), { index }) }))
  return {
    columns: [namedColumn('cohort'), column('size', { width: 82 }), ...periods],
    items: cohorts.value.map(item => ({ key: item.name, name: item.name, cells: { size: cell(integer(item.size)),
      ...Object.fromEntries(item.periods.map((value, index) => ['period' + index, value === null ? cell('—', { hint: t(key + 'periodPending') })
        : cell(percentage(value.percent), { heat: value.percent, hint: t(key + 'returnCount', { count: value.count, total: item.size }) })])) } })),
    note: t(key + 'cohortHint'),
  }
})
function rowClass(item: Item) { return item.emphasis ? 'bg-primary/10' : '' }
function cellClass(value?: Cell) {
  return value?.delta != null && value.delta !== 0 ? (value.delta > 0 ? 'font-semibold text-success' : 'font-semibold text-danger')
    : value?.heat !== undefined ? 'inline-block rounded-mf-sm px-1 py-0.5' : ''
}
function heatStyle(value?: Cell) {
  return value?.heat === undefined ? undefined : { backgroundColor: `color-mix(in srgb, var(--mf-color-primary) ${4 + .25 * value.heat}%, transparent)` }
}
const emptyKey = computed(() => {
  const current = kind.value
  if (['stageConversion', 'targetGap', 'segmentSummary', 'paretoTable', 'varianceDrivers'].includes(current) && !settings.value.groupField)
    return key + 'chooseGroup'
  if (current === 'stageConversion' && settings.value.stageOrder.length < 2) return key + 'chooseStages'
  if (['cohortRetention', 'recencyDistribution'].includes(current) && (!settings.value.entityField || !props.widget.timeField)) return key + 'chooseEntityDate'
  if (['targetGap', 'varianceDrivers'].includes(current) && (!props.widget.field || !settings.value.referenceField)) return key + 'chooseMeasures'
  if ((current === 'valueBands' || current === 'segmentSummary' && ['sum', 'avg'].includes(props.widget.operation)) && !props.widget.field)
    return 'queryChart.selectField'
  if (current === 'valueBands' && !boundaries.value) return key + 'boundaryHint'
  if (!rows.value.length) return 'common.noData'
  if (current === 'stageConversion') return stages.value.some(item => item.observed) ? '' : 'common.noData'
  if (current === 'recencyDistribution' || current === 'valueBands') return distribution.value.total ? '' : 'common.noData'
  return table.value.items.length ? '' : 'common.noData'
})
</script>
