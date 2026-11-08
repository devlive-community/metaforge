<template>
  <MfEmpty v-if="setupMessage" class="h-full" size="sm" :title="$t(setupMessage)"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'validCount', { count: count(result.intervals.length) }) }}</MfTag>
      <MfTag v-if="kind === 'intervalConcurrency' && coverage" size="sm">{{ $t(key + 'peakCount', { count: count(peak) }) }}</MfTag>
      <span v-if="result.invalid" class="text-content-secondary">{{ $t(key + 'invalidCount', { count: count(result.invalid) }) }}</span>
      <span v-if="coverage" class="text-content-secondary">{{ $t(key + 'range', { start: formatDateTime(coverage.first), end: formatDateTime(coverage.last) }) }}</span>
    </div>
    <MfEmpty v-if="!coverage" class="min-h-0 flex-1" size="sm" :title="$t(key + 'noIntervals')"/>
    <EChart v-else-if="kind === 'intervalConcurrency'" class="min-h-0 flex-1" :option="plot" @point="selectPoint"/>
    <div v-else-if="kind === 'intervalCoverage'" class="flex min-h-0 flex-1 flex-col gap-2 overflow-auto">
      <DashboardProgress :value="coverage.covered" :target="coverage.span" :label="$t(key + 'coverageRate')"/>
      <div class="grid grid-cols-[repeat(auto-fit,minmax(min(100%,8rem),1fr))] gap-2">
        <section v-for="item in coverageCards" :key="item.label" class="min-w-0 rounded-mf-md border border-line bg-surface-hover px-2.5 py-1">
          <p class="text-xs font-medium text-content-secondary">{{ $t(key + item.label) }}</p>
          <div class="flex flex-wrap items-center justify-between gap-1">
            <MfButton v-if="interactive && item.label === 'covered'" size="sm" variant="ghost" class="h-auto! py-0.5! font-semibold tabular-nums text-primary"
                      :aria-label="linkLabel($t(key + item.label), duration(item.ms))" @click="link(result.intervals.map(interval => interval.row))">{{ duration(item.ms) }}</MfButton>
            <span v-else class="py-1 text-sm font-semibold tabular-nums">{{ duration(item.ms) }}</span>
            <span class="text-xs tabular-nums text-content-secondary">{{ percent(item.ms / coverage.span) }}</span>
          </div>
        </section>
      </div>
      <p class="text-xs text-content-tertiary">{{ $t(key + 'coverageHint') }}</p>
    </div>
    <div v-else class="flex min-h-0 flex-1 flex-col gap-1 overflow-auto">
      <p class="shrink-0 text-xs text-content-tertiary">{{ table.note }}</p>
      <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows" row-key="key" :empty-text="$t(table.empty)">
        <template #cell-name="{ record }"><EllipsisCell :text="record.name ?? ''"/></template>
        <template #cell-start="{ record }"><span class="whitespace-normal tabular-nums">{{ record.start }}</span></template>
        <template #cell-end="{ record }"><span class="whitespace-normal tabular-nums">{{ record.end }}</span></template>
        <template #cell-value="{ record }">
          <MfButton v-if="interactive && (record.period || record.rows?.length)" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                    :aria-label="linkLabel(record.label, record.value)" @click="selectRow(record)">{{ record.value }}</MfButton>
          <span v-else class="tabular-nums">{{ record.value }}</span>
        </template>
      </MfTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import type { MfTableColumn } from 'metaforge-ui'
import { useChartColors } from '@/composables/useChartColors'
import { dashboardDataSettings, dashboardIntervalSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardIntervalBandBoundaries, dashboardIntervalCoverage, dashboardIntervalDurationBands, dashboardIntervalGroupCoverage,
  dashboardIntervalOverlaps, dashboardIntervalSweep, dashboardIntervalUnitMs, dashboardReadIntervals, type DashboardIntervalRange } from '@/model/dashboard-intervals'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { formatDate, formatDateTime } from '@/utils/date'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import EChart from '@/views/components/visual/EChart.vue'
import DashboardProgress from './DashboardProgress.vue'

const props = defineProps<{ widget: DashboardWidget; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.interval.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardIntervalSettings(props.widget.intervalOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const limit = computed(() => Math.min(30, data.value.limit))
const source = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const result = computed(() => dashboardReadIntervals(source.value, props.widget.timeField ?? '', settings.value.endField, settings.value.groupField))
const coverage = computed(() => dashboardIntervalCoverage(result.value.intervals))
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const integers = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const numbers = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const percentages = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: 1 }))
const count = (value: number) => integers.value.format(value)
const percent = (value: number) => percentages.value.format(value)
const unitMs = computed(() => dashboardIntervalUnitMs[settings.value.unit])
const unitLabel = computed(() => t('dashboard.studio.temporal.unitLabel.' + settings.value.unit))
const duration = (value: number) => numbers.value.format(value / unitMs.value) + ' ' + unitLabel.value
const boundaries = computed(() => dashboardIntervalBandBoundaries(settings.value.durationBands, unitMs.value))
const linkLabel = (name: string, value: string) => t('dashboard.studio.relation.filterCell', { name, value })
function link(rows: QueryChartRow[]) { if (rows.length) emit('filter', rows) }

const coverageCards = computed(() => coverage.value ? [
  { label: 'covered', ms: coverage.value.covered },
  { label: 'idle', ms: coverage.value.span - coverage.value.covered },
] : [])
const colors = useChartColors()
const sweep = computed(() => dashboardIntervalSweep(result.value.intervals))
const peak = computed(() => sweep.value.reduce((maximum, point) => Math.max(maximum, point.active), 0))
const plot = computed<EChartsOption>(() => {
  const theme = colors.value, color = props.widget.color || theme.primary
  const range = coverage.value
  const sameDay = !!range && formatDate(range.first, 'YYYY-MM-DD') === formatDate(range.last, 'YYYY-MM-DD')
  return {
    animation: false,
    color: [color],
    grid: { top: 12, left: sameDay ? 16 : 36, right: sameDay ? 25 : 45, bottom: 12, containLabel: true },
    tooltip: { trigger: 'item', renderMode: 'richText', confine: true, formatter: params => {
      const item = Array.isArray(params) ? params[0] : params
      const [time, active] = item.value as number[]
      return formatDateTime(time) + '\n' + t(key + 'activeCount', { count: count(active) })
    } },
    xAxis: { type: 'time', axisLabel: { color: theme.text, hideOverlap: true, formatter: value => formatDate(value, sameDay ? 'HH:mm' : 'MM-DD HH:mm') } },
    yAxis: { type: 'value', min: 0, minInterval: 1, axisLabel: { color: theme.text }, splitLine: { lineStyle: { color: theme.line } } },
    series: [{ type: 'line', step: 'end', showSymbol: true, symbolSize: 8, areaStyle: { opacity: .13 },
      data: sweep.value.map(point => ({ id: String(point.time), value: [point.time, point.active] })) }],
  }
})
function selectPoint(value: unknown) {
  if (!interactive.value || !value || typeof value !== 'object' || !('id' in value) || typeof value.id !== 'string') return
  const time = Number(value.id)
  link(result.value.intervals.filter(item => item.start <= time && item.end > time).map(item => item.row))
}

type TableRow = { key: string; label: string; value: string; name?: string; start?: string; end?: string; duration?: string; share?: string;
  rows?: QueryChartRow[]; period?: DashboardIntervalRange; group?: string }
type Table = { columns: MfTableColumn[]; rows: TableRow[]; note: string; empty: string }
function column(id: string, label: string): MfTableColumn {
  return { title: t(key + label), dataIndex: id, align: ['value', 'duration', 'share'].includes(id) ? 'right' : 'left' }
}
function bandLabel(band: { lower?: number; upper?: number }) {
  if (band.lower === undefined) return band.upper === undefined ? unitLabel.value : '< ' + band.upper + ' ' + unitLabel.value
  return band.upper === undefined ? '≥ ' + band.lower + ' ' + unitLabel.value : `≥ ${band.lower} · < ${band.upper} ${unitLabel.value}`
}
const table = computed<Table>(() => {
  const intervals = result.value.intervals
  if (kind.value === 'intervalDurationBands') {
    const bands = dashboardIntervalDurationBands(intervals, boundaries.value ?? [], unitMs.value)
    return { columns: [column('name', 'durationBand'), column('value', 'samples'), column('share', 'share')],
      rows: bands.map((band, index) => ({ key: String(index), label: bandLabel(band), name: bandLabel(band), value: count(band.rows.length),
        rows: band.rows, share: percent(band.rows.length / intervals.length) })),
      note: t(key + 'bandNote'), empty: key + 'noIntervals' }
  }
  if (kind.value === 'intervalGroupCoverage') {
    const groups = coverage.value ? dashboardIntervalGroupCoverage(intervals, coverage.value.span) : []
    return { columns: [column('name', 'group'), column('value', 'covered'), column('share', 'coverageRate')],
      rows: groups.slice(0, limit.value).map(group => {
        const name = group.group || t(key + 'ungrouped')
        return { key: group.group, label: name, name, value: duration(group.covered), rows: group.rows, share: percent(group.share) }
      }), note: t(key + 'groupNote'), empty: key + 'noIntervals' }
  }
  if (kind.value === 'intervalOverlaps') {
    const periods = dashboardIntervalOverlaps(intervals, settings.value.minConcurrent)
    return { columns: [...(settings.value.groupField ? [column('name', 'group')] : []), column('start', 'start'), column('end', 'end'),
      column('duration', 'duration'), column('value', 'peak')],
      rows: periods.slice(0, limit.value).map(period => {
        const start = formatDateTime(period.start), end = formatDateTime(period.end)
        return { key: JSON.stringify([period.group, period.start]), label: start + ' – ' + end, name: period.group || t(key + 'ungrouped'),
          start, end, duration: duration(period.end - period.start), value: count(period.peak), period, group: period.group }
      }), note: t(key + 'overlapNote', { minimum: count(settings.value.minConcurrent) }), empty: key + 'noOverlaps' }
  }
  const gaps = [...(coverage.value?.gaps ?? [])].sort((a, b) => (b.end - b.start) - (a.end - a.start) || a.start - b.start)
  return { columns: [column('start', 'start'), column('end', 'end'), column('value', 'duration')],
    rows: gaps.slice(0, limit.value).map(gap => {
      const start = formatDateTime(gap.start), end = formatDateTime(gap.end)
      return { key: String(gap.start), label: start + ' – ' + end, start, end, value: duration(gap.end - gap.start), period: gap }
    }), note: t(key + 'gapNote'), empty: key + 'noGaps' }
})
function selectRow(record: TableRow) {
  if (record.rows) {
    link(record.rows)
    return
  }
  if (!record.period) return
  const { start, end } = record.period
  const rows = kind.value === 'intervalGaps'
    ? result.value.intervals.filter(item => item.end === start || item.start === end)
    : result.value.intervals.filter(item => item.group === record.group && item.start < end && item.end > start)
  link(rows.map(item => item.row))
}

const setupMessage = computed(() => {
  const fields = props.options.fields, startField = props.widget.timeField ?? '', { endField, groupField } = settings.value
  if (startField === endField || !fields.includes(startField) || !fields.includes(endField)) return key + 'chooseTimes'
  if ((kind.value === 'intervalGroupCoverage' || kind.value === 'intervalOverlaps' && groupField) && !fields.includes(groupField)) return key + 'chooseGroup'
  if (kind.value === 'intervalDurationBands' && !boundaries.value) return key + 'bandHint'
  return source.value.length ? '' : 'common.noData'
})
</script>
