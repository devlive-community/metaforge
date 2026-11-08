<template>
  <MfEmpty v-if="emptyKey" class="h-full" size="sm" :title="$t(emptyKey)"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <div v-if="table.summary" class="flex shrink-0 flex-wrap items-center gap-2 text-xs">
      <MfTag size="sm" variant="primary">{{ table.summary }}</MfTag>
      <span v-if="table.note" class="text-content-secondary">{{ table.note }}</span>
    </div>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows" row-key="key">
      <template #cell-name="{ record }"><EllipsisCell :text="record.name" class="font-medium"/></template>
      <template v-for="column in table.columns.slice(1)" :key="column.key" #[`cell-${column.key}`]="{ record }">
        <MfButton v-if="interactive && record.cells[column.key]?.sources?.length" size="sm" variant="ghost"
                  class="w-full px-2! tabular-nums" :style="record.cells[column.key].heat" :aria-label="record.cells[column.key].label"
                  @click="emit('filter', record.cells[column.key].sources)">
          {{ record.cells[column.key].text }}
        </MfButton>
        <span v-else class="inline-block rounded-mf-sm px-1.5 py-0.5 tabular-nums" :style="record.cells[column.key]?.heat">
          {{ record.cells[column.key]?.text ?? '—' }}
        </span>
      </template>
    </MfTable>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MfTableColumn } from 'metaforge-ui'
import { dashboardDataSettings, dashboardRelationSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardCooccurrences, dashboardCrossTab, dashboardGroupSpread, dashboardTransitions } from '@/model/dashboard-relations'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'dataOptions' | 'field' | 'kind' | 'operation' | 'relationOptions' | 'timeField'>; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.relation.'
const settings = computed(() => dashboardRelationSettings(props.widget.relationOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const rows = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const kind = computed(() => props.widget.kind)
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const operation = computed(() => props.widget.operation === 'sum' || props.widget.operation === 'avg' ? props.widget.operation : 'count')
const integer = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const numbers = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const percent = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 1 }))
const count = (value: number) => integer.value.format(value)
const metric = (value: number | null) => value === null ? '—' : data.value.prefix + numbers.value.format(value) + data.value.suffix

type Cell = { text: string; sources?: QueryChartRow[]; label?: string; heat?: { backgroundColor: string } }
type Table = {
  columns: (MfTableColumn & { key: string })[]
  rows: { key: string; name: string; cells: Record<string, Cell> }[]
  summary?: string
  note?: string
}
function column(id: string, title: string, width: number, align: MfTableColumn['align'] = 'center') {
  return { key: id, dataIndex: id, title, width, align }
}
const groupColumn = (label: string) => column('name', t(key + label), 112, 'left')
function linkLabel(name: string, value: string) { return t(key + 'filterCell', { name, value }) }
function heat(value: number | null, maximum: number) {
  if (value === null || !maximum) return
  const amount = 8 + Math.abs(value) / maximum * 40
  const color = value < 0 ? 'danger' : 'primary'
  return { backgroundColor: `color-mix(in srgb, var(--mf-color-${color}) ${amount}%, transparent)` }
}
const matrix = computed(() => kind.value === 'transitionMatrix'
  ? dashboardTransitions(rows.value, settings.value.entityField, settings.value.rowField, props.widget.timeField || '', settings.value.dimensionLimit)
  : dashboardCrossTab(rows.value, settings.value.rowField, settings.value.columnField, props.widget.field, operation.value, settings.value.dimensionLimit))
const table = computed<Table>(() => {
  const limit = Math.min(data.value.limit, 30)
  if (kind.value === 'cooccurrencePairs') {
    const result = dashboardCooccurrences(rows.value, settings.value.entityField, settings.value.rowField, settings.value.dimensionLimit, limit)
    return {
      columns: [groupColumn('itemA'), column('right', t(key + 'itemB'), 112, 'left'),
        column('count', t(key + 'entities'), 88), column('share', t(key + 'support'), 80)],
      rows: result.items.map(item => ({ key: item.key, name: item.left, cells: {
        right: { text: item.right },
        count: { text: count(item.count), sources: item.sources, label: linkLabel(item.left + ' / ' + item.right, count(item.count)) },
        share: { text: percent.value.format(item.share) + '%' },
      } })),
      summary: t(key + 'entityTotal', { count: count(result.entities) }),
      note: result.limited ? t(key + 'itemLimitNote', { count: settings.value.dimensionLimit }) : undefined,
    }
  }
  if (kind.value === 'groupSpread') {
    const result = dashboardGroupSpread(rows.value, settings.value.rowField, props.widget.field, limit)
    const metrics = ['min', 'q1', 'median', 'q3', 'max'] as const
    return {
      columns: [groupColumn('group'), column('count', t(key + 'sampleCount'), 72),
        ...metrics.map(id => column(id, t(key + id), 74))],
      rows: result.map(item => ({ key: item.name, name: item.name, cells: {
        count: { text: count(item.count), sources: item.sources, label: linkLabel(item.name, count(item.count)) },
        ...Object.fromEntries(metrics.map(id => [id, { text: metric(item[id]) }])),
      } })),
    }
  }
  const result = matrix.value
  const maximum = Math.max(0, ...result.rows.flatMap(row => row.cells.map(cell => Math.abs(cell?.value ?? 0))))
  const display = kind.value === 'transitionMatrix' || operation.value === 'count'
    ? (value: number | null) => value === null ? '—' : count(value)
    : metric
  return {
    columns: [groupColumn(kind.value === 'transitionMatrix' ? 'fromTo' : 'group'),
      ...result.columns.map((label, index) => column('value' + index, label, 98))],
    rows: result.rows.map(row => ({ key: row.label, name: row.label, cells: Object.fromEntries(row.cells.flatMap((cell, index) => {
      if (!cell) return []
      const value = display(cell.value)
      return [['value' + index, { text: value, heat: heat(cell.value, maximum), sources: cell.sources,
        label: linkLabel(row.label + ' → ' + result.columns[index], value) }]]
    })) })),
    summary: t(key + (kind.value === 'transitionMatrix' ? 'transitionTotal' : 'recordTotal'), { count: count(result.total) }),
    note: result.visible < result.total ? t(key + 'matrixLimited', { visible: count(result.visible), total: count(result.total) }) : undefined,
  }
})
const emptyKey = computed(() => {
  const { rowField, columnField, entityField } = settings.value
  const current = kind.value
  if (!rowField || current === 'crossTabHeatmap' && (!columnField || rowField === columnField) ||
      ['cooccurrencePairs', 'transitionMatrix'].includes(current) && (!entityField || rowField === entityField) ||
      current === 'transitionMatrix' && !props.widget.timeField ||
      (current === 'groupSpread' || current === 'crossTabHeatmap' && operation.value !== 'count') && !props.widget.field) return key + 'chooseFields'
  return table.value.rows.length ? '' : 'common.noData'
})
</script>
