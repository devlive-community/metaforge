<template>
  <MfEmpty v-if="emptyKey" class="h-full" size="sm" :title="$t(emptyKey)"/>
  <div v-else-if="quadrants && quadrantData" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'samples', { count: count(quadrantData.total) }) }}</MfTag>
      <span class="text-content-secondary">{{ $t(key + 'thresholdValues', { x: quadrantData.x, y: quadrantData.y }) }}</span>
    </div>
    <div class="grid min-h-0 flex-1 grid-cols-2 content-start gap-2 overflow-auto">
      <section v-for="(samples, index) in quadrantData.quadrants" :key="index" class="min-w-0 rounded-mf-md border border-line bg-surface-hover px-2.5 py-2">
        <p class="break-words text-xs font-medium text-content-secondary">{{ quadrantLabel(index) }}</p>
        <div class="flex flex-wrap items-center justify-between gap-1">
          <MfButton v-if="interactive && samples.length" size="sm" variant="ghost" class="text-base font-semibold tabular-nums text-primary"
                    :aria-label="linkLabel(quadrantLabel(index), count(samples.length))" @click="emit('filter', samples)">{{ count(samples.length) }}</MfButton>
          <span v-else class="py-1 text-base font-semibold tabular-nums">{{ count(samples.length) }}</span>
          <span class="text-xs tabular-nums text-content-secondary">{{ percent(samples.length / quadrantData.total) }}</span>
        </div>
      </section>
    </div>
  </div>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <div v-if="table.summary || table.note" class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag v-if="table.summary" size="sm" variant="primary">{{ table.summary }}</MfTag>
      <span v-if="table.note" class="text-content-secondary">{{ table.note }}</span>
    </div>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows" row-key="key">
      <template #cell-name="{ record }"><EllipsisCell :text="record.name" class="font-medium"/></template>
      <template v-for="column in table.columns.slice(1)" :key="column.key" #[`cell-${column.key}`]="{ record }">
        <MfButton v-if="interactive && record.cells[column.key]?.samples?.length" size="sm" variant="ghost"
                  class="min-w-8 px-2! tabular-nums text-primary" :style="record.cells[column.key].style"
                  :aria-label="linkLabel(record.name + ' / ' + column.title, record.cells[column.key].text)"
                  @click="emit('filter', record.cells[column.key].samples)">{{ record.cells[column.key].text }}</MfButton>
        <span v-else class="inline-block rounded-mf-sm px-1.5 py-0.5 tabular-nums" :style="record.cells[column.key]?.style"
              :class="record.cells[column.key]?.tone">{{ record.cells[column.key]?.text ?? '—' }}</span>
      </template>
    </MfTable>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MfTableColumn } from 'metaforge-ui'
import { dashboardComparativeSettings, dashboardDataSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardBandBoundaries } from '@/model/dashboard-business'
import { dashboardCorrelation, dashboardDifferenceBands, dashboardGroupComparison, dashboardQuadrants, dashboardRankMovement, dashboardShareMovement } from '@/model/dashboard-comparative'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'comparativeOptions' | 'dataOptions' | 'field' | 'kind'>; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.comparative.'
const settings = computed(() => dashboardComparativeSettings(props.widget.comparativeOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const kind = computed(() => props.widget.kind)
const quadrants = computed(() => kind.value === 'metricQuadrants')
const rows = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const limit = computed(() => Math.min(30, data.value.limit))
const integers = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const numbers = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const percentages = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: data.value.decimals }))
const count = (value: number) => integers.value.format(value)
const percent = (value: number | null) => value === null ? '—' : percentages.value.format(value)
const linkLabel = (name: string, value: string) => t('dashboard.studio.relation.filterCell', { name, value })
const signed = (value: number) => (value > 0 ? '+' : '') + numbers.value.format(value)
const tone = (value: number) => value > 0 ? 'font-semibold text-success' : value < 0 ? 'font-semibold text-danger' : ''

const quadrantData = computed(() => dashboardQuadrants(rows.value, props.widget.field, settings.value.secondField, settings.value.xThreshold, settings.value.yThreshold))
function quadrantLabel(index: number) {
  return `${props.widget.field} ${index % 2 ? '≥' : '<'} ${quadrantData.value?.x} · ${settings.value.secondField} ${index < 2 ? '≥' : '<'} ${quadrantData.value?.y}`
}
const boundaries = computed(() => dashboardBandBoundaries(settings.value.boundaries))
function bandLabel(band: { lower?: number; upper?: number }) {
  if (band.lower === undefined) return band.upper === undefined ? t(key + 'allValues') : '< ' + band.upper
  return band.upper === undefined ? '≥ ' + band.lower : `≥ ${band.lower} · < ${band.upper}`
}
function heat(value: number) {
  const color = value < 0 ? 'danger' : 'primary'
  return { backgroundColor: `color-mix(in srgb, var(--mf-color-${color}) ${8 + Math.abs(value) * 36}%, transparent)` }
}
type Cell = { text: string; samples?: QueryChartRow[]; style?: { backgroundColor: string }; tone?: string }
type Table = { columns: (MfTableColumn & { key: string })[]; rows: { key: string; name: string; cells: Record<string, Cell> }[]; summary?: string; note?: string; empty?: boolean }
function column(id: string, title: string, width?: number) {
  return { key: id, dataIndex: id, title, width, align: id === 'name' ? 'left' as const : 'center' as const }
}
const namedColumn = (label: string) => column('name', t(key + label), 126)
const table = computed<Table>(() => {
  const { fields, secondField, groupField, leftGroup, rightGroup, minSamples } = settings.value
  const field = props.widget.field, source = rows.value, limits = boundaries.value ?? []
  if (kind.value === 'metricCorrelation') {
    const matrix = dashboardCorrelation(source, fields, minSamples)
    return {
      columns: [namedColumn('metric'), ...fields.map((name, index) => column('value' + index, name, 120))],
      rows: matrix.map((cells, row) => ({ key: fields[row], name: fields[row], cells: Object.fromEntries(cells.map((cell, index) => {
        const coefficient = cell.coefficient === null ? '—' : numbers.value.format(cell.coefficient)
        return ['value' + index, { text: coefficient + ' · n=' + count(cell.rows.length), samples: cell.coefficient === null ? undefined : cell.rows,
          style: cell.coefficient === null ? undefined : heat(cell.coefficient) }]
      })) })),
      note: t(key + 'correlationHint', { count: minSamples }),
    }
  }
  if (kind.value === 'rankMovement') {
    const results = dashboardRankMovement(source, groupField, field, secondField)
    return {
      columns: [namedColumn('group'), column('baseline', t(key + 'baselineRank')), column('current', t(key + 'currentRank')), column('change', t(key + 'rankChange'))],
      rows: results.slice(0, limit.value).map(item => ({ key: item.name, name: item.name, cells: {
        baseline: { text: count(item.baselineRank) }, current: { text: count(item.currentRank), samples: item.rows },
        change: { text: signed(item.movement), tone: tone(item.movement) },
      } })),
      note: t(key + 'rankHint'),
    }
  }
  if (kind.value === 'shareMovement') {
    const results = dashboardShareMovement(source, groupField, field, secondField)
    return {
      columns: [namedColumn('group'), column('baseline', t(key + 'baselineShare')), column('current', t(key + 'currentShare')), column('change', t(key + 'shareChange'))],
      rows: results.slice(0, limit.value).map(item => ({ key: item.name, name: item.name, cells: {
        baseline: { text: percent(item.baselineShare) }, current: { text: percent(item.currentShare), samples: item.rows },
        change: { text: item.movement === null ? '—' : signed(item.movement * 100), tone: tone(item.movement ?? 0) },
      } })),
      note: t(key + 'shareHint'),
    }
  }
  if (kind.value === 'comparisonDistribution') {
    const result = dashboardGroupComparison(source, groupField, field, leftGroup, rightGroup, limits)
    return {
      columns: [namedColumn('band'), column('left', leftGroup), column('right', rightGroup)],
      rows: result.bands.map((band, index) => ({ key: String(index), name: bandLabel(band), cells: {
        left: distributionCell(band.rows, result.leftTotal), right: distributionCell(band.otherRows, result.rightTotal),
      } })),
      summary: `${leftGroup} · ${count(result.leftTotal)} / ${rightGroup} · ${count(result.rightTotal)}`,
      note: t(key + 'distributionHint'),
      empty: result.leftTotal + result.rightTotal === 0,
    }
  }
  const result = dashboardDifferenceBands(source, field, secondField, limits)
  return {
    columns: [namedColumn('differenceBand'), column('samples', t(key + 'count')), column('share', t(key + 'share'))],
    rows: result.bands.map((band, index) => ({ key: String(index), name: bandLabel(band), cells: {
      samples: { text: count(band.rows.length), samples: band.rows }, share: { text: percent(result.total ? band.rows.length / result.total : null) },
    } })),
    summary: t(key + 'samples', { count: count(result.total) }),
    note: t(key + 'differenceHint'),
    empty: result.total === 0,
  }
})
function distributionCell(samples: QueryChartRow[], total: number): Cell {
  const share = total ? samples.length / total : null
  return { text: count(samples.length) + ' · ' + percent(share), samples, style: share === null ? undefined : heat(share) }
}

const emptyKey = computed(() => {
  const reportFields = props.options.fields, { fields, secondField, groupField, leftGroup, rightGroup } = settings.value
  const current = kind.value
  if (current === 'metricCorrelation') {
    if (fields.length < 2 || !fields.every(field => reportFields.includes(field))) return key + 'chooseCorrelation'
  } else {
    if (!reportFields.includes(props.widget.field)) return 'queryChart.selectField'
    if (current !== 'comparisonDistribution' && (!reportFields.includes(secondField) || secondField === props.widget.field)) return key + 'chooseMeasures'
    if (['rankMovement', 'shareMovement', 'comparisonDistribution'].includes(current) && !reportFields.includes(groupField)) return key + 'chooseGroupField'
    if (current === 'comparisonDistribution' && (!leftGroup || !rightGroup || leftGroup === rightGroup)) return key + 'chooseGroups'
    if (['pairedDifference', 'comparisonDistribution'].includes(current) && !boundaries.value) return 'dashboard.studio.business.boundaryHint'
  }
  if (!rows.value.length) return 'common.noData'
  if (quadrants.value) return quadrantData.value ? '' : 'common.noData'
  return table.value.empty || !table.value.rows.length ? 'common.noData' : ''
})
</script>
