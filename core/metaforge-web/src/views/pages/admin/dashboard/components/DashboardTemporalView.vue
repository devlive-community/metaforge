<template>
  <MfEmpty v-if="emptyKey" class="h-full" size="sm" :title="$t(emptyKey)"/>
  <div v-else-if="kind === 'intervalTimeline'" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-2 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'intervalCount', { visible: count(timeline.items.length), total: count(timeline.total) }) }}</MfTag>
      <span class="text-content-secondary">{{ formatDateTime(timeline.first) }} – {{ formatDateTime(timeline.last) }}</span>
    </div>
    <ol class="min-h-0 space-y-1.5 overflow-auto pr-1">
      <li v-for="item in timeline.items" :key="item.index" class="min-w-0 rounded-mf-md border border-line px-2.5 py-1.5">
        <div class="mb-1 flex min-w-0 items-center justify-between gap-2 text-xs">
          <EllipsisCell :text="intervalName(item)" class="min-w-0 flex-1 font-medium"/>
          <span class="shrink-0 tabular-nums text-content-secondary">{{ duration(item.end - item.start) }}</span>
        </div>
        <div class="relative h-3 rounded-full bg-surface-hover">
          <MfButton v-if="interactive" variant="primary" size="sm" class="absolute top-0 h-3! min-w-2 rounded-full px-0!"
                    :style="barStyle(item)" :aria-label="linkLabel(intervalName(item), duration(item.end - item.start))"
                    @click="emit('filter', [item.row])"/>
          <span v-else class="absolute top-0 h-3 min-w-2 rounded-full bg-primary/70" :style="barStyle(item)"/>
        </div>
        <div class="mt-1 break-words text-xs tabular-nums text-content-tertiary">{{ formatDateTime(item.start) }} – {{ formatDateTime(item.end) }}</div>
      </li>
    </ol>
  </div>
  <div v-else-if="kind === 'eventIntervals'" class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <p class="shrink-0 text-xs text-content-tertiary">{{ $t(key + 'distinctTimes') }}</p>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="intervalColumns" :data-source="events" row-key="name">
      <template #cell-name="{ record }"><EllipsisCell :text="record.name || $t(key + 'allEvents')"/></template>
      <template #cell-count="{ record }">
        <MfButton v-if="interactive" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                  :aria-label="linkLabel(record.name || $t(key + 'allEvents'), count(record.count))" @click="emit('filter', record.rows)">
          {{ count(record.count) }}
        </MfButton>
        <span v-else class="tabular-nums">{{ count(record.count) }}</span>
      </template>
      <template #cell-median="{ record }"><span class="tabular-nums">{{ duration(record.median) }}</span></template>
      <template #cell-max="{ record }"><span class="tabular-nums">{{ duration(record.max) }}</span></template>
    </MfTable>
  </div>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col">
    <p v-if="plot.note" class="shrink-0 text-xs text-content-tertiary">{{ plot.note }}</p>
    <EChart class="min-h-0 flex-1" :option="plot.option" @point="selectPoint"/>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption, LineSeriesOption } from 'echarts'
import type { MfTableColumn } from 'metaforge-ui'
import { useChartColors } from '@/composables/useChartColors'
import { dashboardDataSettings, dashboardTemporalSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardDailySeries, dashboardEventIntervals, dashboardMonthlyComparison, dashboardTimeIntervals, dashboardWeekHours } from '@/model/dashboard-temporal'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { formatDateTime } from '@/utils/date'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import EChart from '@/views/components/visual/EChart.vue'

const props = defineProps<{ widget: DashboardWidget; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.temporal.'
const settings = computed(() => dashboardTemporalSettings(props.widget.temporalOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const kind = computed(() => props.widget.kind)
const timeField = computed(() => props.widget.timeField || '')
const rows = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const operation = computed(() => props.widget.operation === 'sum' ? 'sum' : 'count')
const colors = useChartColors()
const number = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const integer = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const count = (value: number) => integer.value.format(value)
const linkLabel = (name: string, value: string) => t('dashboard.studio.relation.filterCell', { name, value })
const unitMs = { minutes: 60_000, hours: 3_600_000, days: 86_400_000 }
const duration = (value: number) => number.value.format(value / unitMs[settings.value.unit]) + ' ' + t(key + 'unitLabel.' + settings.value.unit)
const limit = computed(() => Math.min(30, data.value.limit))
const events = computed(() => dashboardEventIntervals(rows.value, timeField.value, settings.value.entityField).slice(0, limit.value))
const timeline = computed(() => dashboardTimeIntervals(rows.value, timeField.value, settings.value.endField, settings.value.labelField, limit.value))
const intervalColumns = computed<MfTableColumn[]>(() => [
  { title: t(key + 'entity'), dataIndex: 'name' },
  ...['count', 'median', 'max'].map(name => ({ title: t(key + 'intervalColumn.' + name), dataIndex: name, align: 'right' as const })),
])
function intervalName(item: { name: string; index: number }) { return item.name || t(key + 'record', { index: item.index + 1 }) }
function barStyle(item: { start: number; end: number }) {
  const first = timeline.value.first ?? item.start, last = timeline.value.last ?? item.end
  const span = Math.max(1, last - first)
  return {
    left: `min(${(item.start - first) / span * 100}%, calc(100% - .5rem))`,
    width: (item.end - item.start) / span * 100 + '%',
  }
}

type Plot = { option: EChartsOption; samples: (id: string) => QueryChartRow[] | undefined; empty: boolean; note?: string }
function datum(id: string, value: number | null) { return { id, value: value !== null && Number.isFinite(value) ? value : null } }
function line(name: string, values: ReturnType<typeof datum>[], dashed = false): LineSeriesOption {
  return { type: 'line', name, data: values, showSymbol: true, symbolSize: 5, lineStyle: { type: dashed ? 'dashed' : 'solid' },
    tooltip: { valueFormatter: value => typeof value === 'number' ? number.value.format(value) : '—' } }
}
function baseOption(): EChartsOption {
  const theme = colors.value
  return {
    animation: false,
    color: [props.widget.color || theme.primary, theme.colors[2], theme.colors[3]],
    legend: { top: 0, type: 'scroll', textStyle: { color: theme.text } },
    grid: { top: 34, left: 5, right: 12, bottom: 8, containLabel: true },
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
  }
}
function linePlot(labels: string[], series: LineSeriesOption[], dateLabels = false, cumulative = false): EChartsOption {
  const theme = colors.value
  const axis = { type: 'value' as const, axisLabel: { color: theme.text }, splitLine: { lineStyle: { color: theme.line } } }
  return { ...baseOption(),
    ...(cumulative ? { grid: { top: 48, left: 5, right: 12, bottom: 8, containLabel: true } } : {}),
    xAxis: { type: 'category', data: labels, axisLabel: { color: theme.text, hideOverlap: true, formatter: dateLabels ? value => value.slice(5) : undefined },
      axisLine: { lineStyle: { color: theme.line } } },
    yAxis: cumulative ? [
      { ...axis, name: t(key + 'daily'), nameTextStyle: { color: theme.text }, nameGap: 10 },
      { ...axis, name: t(key + 'cumulative'), nameTextStyle: { color: theme.text }, nameGap: 10, position: 'right', scale: true, splitLine: { show: false } },
    ] : axis,
    series,
  }
}
const plot = computed<Plot>(() => {
  if (kind.value === 'yearOverYearTrend') {
    const result = dashboardMonthlyComparison(rows.value, timeField.value, props.widget.field, operation.value, settings.value.year)
    const formatMonth = new Intl.DateTimeFormat(locale.value, { month: 'short', timeZone: 'UTC' })
    const labels = result.months.map((_, index) => formatMonth.format(new Date(Date.UTC(2024, index, 1))))
    return {
      option: linePlot(labels, [
        line(String(result.year), result.months.map((item, index) => datum('current:' + index, item.current?.value ?? null))),
        line(String(result.year - 1), result.months.map((item, index) => datum('previous:' + index, item.previous?.value ?? null)), true),
      ]),
      samples: id => {
        const [which, index] = id.split(':')
        const month = result.months[Number(index)]
        return which === 'current' ? month?.current?.rows : month?.previous?.rows
      },
      empty: !result.months.some(item => item.current || item.previous),
      note: t(key + 'monthlyHint'),
    }
  }
  if (kind.value === 'weekdayHourHeatmap') {
    const buckets = dashboardWeekHours(rows.value, timeField.value, props.widget.field, operation.value)
    const dayFormat = new Intl.DateTimeFormat(locale.value, { weekday: 'short', timeZone: 'UTC' })
    const days = Array.from({ length: 7 }, (_, day) => dayFormat.format(new Date(Date.UTC(2024, 0, day + 1))))
    const hours = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, '0'))
    const theme = colors.value
    const minimum = Math.min(0, ...buckets.map(item => item.value)), maximum = Math.max(1, ...buckets.map(item => item.value))
    const color = props.widget.color || theme.primary
    return {
      option: {
        ...baseOption(), legend: { show: false }, grid: { top: 5, bottom: 27, left: 5, right: 8, containLabel: true },
        xAxis: { type: 'category', data: hours, axisLabel: { color: theme.text, interval: 2, hideOverlap: true } },
        yAxis: { type: 'category', data: days, inverse: true, axisLabel: { color: theme.text } },
        visualMap: { orient: 'horizontal', left: 'center', bottom: 0, itemWidth: 8, itemHeight: 86, textStyle: { color: theme.text },
          min: minimum < 0 ? -Math.max(-minimum, maximum) : 0, max: Math.max(-minimum, maximum), dimension: 2,
          inRange: { color: minimum < 0 ? [theme.colors[5], theme.line, color] : [theme.line, color] } },
        tooltip: { trigger: 'item', renderMode: 'richText', confine: true, formatter: params => {
          const item = Array.isArray(params) ? params[0] : params
          const [hour, weekday, value] = item.value as number[]
          return `${days[weekday]} ${hours[hour]}:00\n${number.value.format(value)}`
        } },
        series: [{ type: 'heatmap', data: buckets.map(item => ({ id: item.weekday + ':' + item.hour, value: [item.hour, item.weekday, item.value] })) }],
      },
      samples: id => buckets.find(item => item.weekday + ':' + item.hour === id)?.rows,
      empty: !buckets.length,
      note: t(key + 'recordedClock'),
    }
  }
  const result = dashboardDailySeries(rows.value, timeField.value, props.widget.field, operation.value, settings.value.historyDays, settings.value.windowDays)
  const rolling = kind.value === 'rollingAverageTrend'
  const series = [
    line(t(key + 'daily'), result.points.map(point => datum('daily:' + point.day, point.daily))),
    line(t(key + (rolling ? 'average' : 'cumulative')), result.points.map(point => datum('trend:' + point.day, rolling ? point.average : point.cumulative)), true),
  ]
  if (!rolling) series[1].yAxisIndex = 1
  return {
    option: linePlot(result.points.map(point => point.date), series, true, !rolling),
    samples: id => {
      const [which, day] = id.split(':'), point = result.points.find(point => point.day === Number(day))
      if (!point) return
      if (which === 'daily') return result.buckets.get(point.day)?.rows
      return [...result.buckets].filter(([date]) => date <= point.day && (!rolling || date >= point.windowStart)).flatMap(([, sample]) => sample.rows)
    },
    empty: !result.points.length,
    note: rolling ? t(key + 'windowHint', { days: settings.value.windowDays }) : t(key + 'cumulativeHint'),
  }
})
function selectPoint(value: unknown) {
  if (!interactive.value || !value || typeof value !== 'object' || !('id' in value) || typeof value.id !== 'string') return
  const selected = plot.value.samples(value.id)
  if (selected?.length) emit('filter', selected)
}

const emptyKey = computed(() => {
  const fields = props.options.fields
  if (!fields.includes(timeField.value)) return 'dashboard.studio.chooseDateField'
  if (kind.value === 'intervalTimeline') {
    if (!fields.includes(settings.value.endField) || settings.value.endField === timeField.value) return key + 'chooseEnd'
    return timeline.value.items.length ? '' : key + 'noIntervals'
  }
  if (kind.value === 'eventIntervals') return events.value.length ? '' : key + 'noEventGaps'
  if (operation.value === 'sum' && !fields.includes(props.widget.field)) return 'queryChart.selectField'
  return plot.value.empty ? 'common.noData' : ''
})
</script>
