<template>
  <MfEmpty v-if="setupMessage" class="h-full" size="sm" :title="$t(setupMessage)"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-2 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'validCount', { count: number(sampleCounts.valid) }) }}</MfTag>
      <span v-if="sampleCounts.invalid" class="text-content-secondary">{{ $t(key + 'invalidCount', { count: number(sampleCounts.invalid) }) }}</span>
      <span v-if="!usesSpecification && kind !== 'qualityRun'" class="text-content-secondary">{{ $t(key + 'controlHint') }}</span>
    </div>
    <MfEmpty v-if="!usesSpecification && kind !== 'qualityRun' && !control.moving.limits" class="min-h-0 flex-1" size="sm" :title="$t(key + 'noPairs')"/>
    <MfEmpty v-else-if="kind === 'qualityRun' && !control.valid" class="min-h-0 flex-1" size="sm" :title="$t('common.noData')"/>
    <EChart v-else-if="chartSeries" class="min-h-0 flex-1" :option="plot" @point="selectPoint"/>
    <MfTable v-else class="min-h-0 flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows"
             row-key="id" :empty-text="$t(key + 'noSignals')">
      <template #cell-label="{ record }"><EllipsisCell :text="record.label"/></template>
      <template #cell-value="{ record }">
        <MfButton v-if="interactive && record.rows.length" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                  :aria-label="$t(key + 'selectRows', { name: record.label, value: record.value })"
                  @click="emit('select', record.rows)">{{ record.value }}</MfButton>
        <span v-else class="tabular-nums">{{ record.value }}</span>
      </template>
    </MfTable>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption, LineSeriesOption } from 'echarts'
import type { MfTableColumn } from 'metaforge-ui'
import { useChartColors } from '@/composables/useChartColors'
import { qualityControlSeries, qualitySignal, qualitySpecificationGroups, qualitySpecificationsValid, specificationStates,
  type ProcessQualityOptions, type ProcessQualityView } from '@/model/process-quality'
import type { QueryChartRow } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import EChart from '@/views/components/visual/EChart.vue'

const props = defineProps<{ kind: ProcessQualityView; options: ProcessQualityOptions; fields: readonly string[];
  rows: readonly QueryChartRow[]; interactive?: boolean; color?: string }>()
const emit = defineEmits<{ select: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'processQuality.'
const colors = useChartColors()
const formatNumber = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 6 }))
const formatRate = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: 1 }))
const number = (value: number) => formatNumber.value.format(value)
const usesSpecification = computed(() => props.kind === 'qualitySpecification' || props.kind === 'qualityGroupYield')
const setupMessage = computed(() => {
  const { measure, order, lower, upper, group } = props.options
  if (!props.fields.includes(measure)) return key + 'chooseMeasure'
  if (usesSpecification.value && !qualitySpecificationsValid(lower, upper)) return key + 'chooseLimits'
  if (props.kind === 'qualityGroupYield' && !props.fields.includes(group)) return key + 'chooseGroup'
  if (!usesSpecification.value && order && !props.fields.includes(order.field)) return key + 'chooseOrder'
  return props.rows.length ? '' : 'common.noData'
})

const control = computed(() => qualityControlSeries(props.rows, props.options.measure, props.options.order))
const specifications = computed(() => qualitySpecificationGroups(props.rows, props.options, props.kind === 'qualityGroupYield'))
const sampleCounts = computed(() => {
  if (!usesSpecification.value) return control.value
  let invalid = 0
  for (const group of specifications.value.values()) invalid += group.invalid.length
  return { valid: props.rows.length - invalid, invalid }
})
const chartSeries = computed(() => {
  if (props.kind === 'qualityRun') return control.value.run
  if (props.kind === 'qualityIndividuals') return control.value.individuals
  if (props.kind === 'qualityMovingRange') return control.value.moving
})

const plot = computed<EChartsOption>(() => {
  const series = chartSeries.value
  if (!series) return {}
  const theme = colors.value
  const { points, limits } = series
  const guides = limits ? [
    { name: props.kind === 'qualityRun' ? 'median' : 'center', value: limits.center, color: theme.text },
    { name: 'controlUpper', value: limits.upper, color: theme.colors[5] },
    { name: 'controlLower', value: limits.lower, color: theme.colors[5] },
  ] : []
  const lines: LineSeriesOption[] = guides.flatMap(guide => {
    const value = guide.value
    const controlLimit = guide.name === 'controlUpper' || guide.name === 'controlLower'
    if (value === undefined || !Number.isFinite(value) || controlLimit && value === limits?.center) return []
    return [{ type: 'line', name: t(key + guide.name), data: points.map(() => value), symbol: 'none', silent: true,
      lineStyle: { color: guide.color, type: 'dashed', width: 1 }, itemStyle: { color: guide.color } }]
  })
  return {
    animation: false,
    grid: { top: 32, right: 16, bottom: 12, left: 16, containLabel: true },
    legend: { top: 0, left: 0, type: 'scroll', selectedMode: false, textStyle: { color: theme.text } },
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    xAxis: { type: 'category', data: points.map((_, index) => String(index + 1)), axisLabel: { color: theme.text, hideOverlap: true } },
    yAxis: { type: 'value', scale: true, axisLabel: { color: theme.text }, splitLine: { lineStyle: { color: theme.line } } },
    series: [{ type: 'line', name: t(key + (props.kind === 'qualityMovingRange' ? 'rangeValue' : 'value')), symbolSize: 6,
      lineStyle: { color: props.color || theme.primary }, itemStyle: { color: props.color || theme.primary },
      data: points.map((point, index) => point ? { id: String(index), value: point.value,
        itemStyle: qualitySignal(point.value, limits) ? { color: theme.colors[5] } : undefined } : '-') }, ...lines],
  }
})
function selectPoint(data: unknown) {
  if (!props.interactive || !data || typeof data !== 'object' || !('id' in data) || typeof data.id !== 'string') return
  const point = chartSeries.value?.points[Number(data.id)]
  if (point) emit('select', point.rows)
}

type TableRow = { id: string; label: string; value: string; rows: QueryChartRow[]; index?: number; limit?: string; valid?: string; passed?: string; invalid?: string; rate?: string }
type Table = { columns: MfTableColumn[]; rows: TableRow[] }
function column(id: string, title: string): MfTableColumn {
  return { dataIndex: id, title: t(key + title), align: id === 'label' ? 'left' : 'right' }
}
const table = computed<Table>(() => {
  if (props.kind === 'qualitySignals') {
    const { individuals, moving } = control.value
    const rows = ([['qualityIndividuals', individuals], ['qualityMovingRange', moving]] as const).flatMap(([name, series]) =>
      series.points.flatMap((point, index) => {
        if (!point) return []
        const signal = qualitySignal(point.value, series.limits)
        if (!signal) return []
        const limit = signal === 'above' ? series.limits?.upper : series.limits?.lower
        return [{ id: name + index, label: t(key + name), index: index + 1, value: number(point.value), limit: limit === undefined ? '' : number(limit), rows: point.rows }]
      }))
    rows.sort((left, right) => left.index - right.index)
    return { columns: [column('label', 'signal'), column('index', 'sequence'), column('value', 'value'), column('limit', 'limit')], rows }
  }
  if (props.kind === 'qualityGroupYield') {
    const rows = [...specifications.value].map(([name, group]) => {
      const rejected = [...group.below, ...group.above]
      const valid = group.pass.length + rejected.length
      return { id: name, label: name || t(key + 'ungrouped'), valid: number(valid), passed: number(group.pass.length),
        value: number(rejected.length), invalid: number(group.invalid.length),
        rate: valid ? formatRate.value.format(group.pass.length / valid) : '—', rows: rejected }
    })
    return { columns: [column('label', 'group'), column('valid', 'valid'), column('passed', 'pass'),
      column('value', 'rejected'), column('invalid', 'invalid'), column('rate', 'yield')], rows }
  }
  const group = specifications.value.get('')
  const rows = specificationStates.map(state => {
    const records = group?.[state] ?? []
    return { id: state, label: t(key + state), value: number(records.length), rows: records }
  })
  return { columns: [column('label', 'result'), column('value', 'samples')], rows }
})
</script>
