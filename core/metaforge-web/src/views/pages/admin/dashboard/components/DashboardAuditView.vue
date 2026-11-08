<template>
  <MfEmpty v-if="setupMessage" class="h-full" size="sm" :title="$t(setupMessage)"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <div v-if="table.summary" class="flex shrink-0 flex-wrap items-center gap-2 text-xs">
      <MfTag size="sm" variant="primary">{{ table.summary }}</MfTag>
      <span v-if="table.note" class="text-content-secondary">{{ table.note }}</span>
    </div>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows" row-key="key" :empty-text="table.empty">
      <template #cell-first="{ record }"><EllipsisCell :text="record.first" class="font-medium"/></template>
      <template #cell-second="{ record }"><EllipsisCell :text="record.second ?? ''"/></template>
      <template #cell-count="{ record }">
        <MfButton v-if="interactive && record.samples?.length" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                  :aria-label="$t('dashboard.studio.relation.filterCell', { name: record.second ? record.first + ' / ' + record.second : record.first, value: record.count })"
                  @click="emit('filter', record.samples)">{{ record.count }}</MfButton>
        <span v-else class="tabular-nums">{{ record.count }}</span>
      </template>
      <template #cell-share="{ record }"><span class="tabular-nums">{{ record.share }}</span></template>
    </MfTable>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MfTableColumn } from 'metaforge-ui'
import { dashboardAuditSettings, dashboardDataSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardDateCoverage, dashboardDuplicateKeys, dashboardFieldComparison, dashboardInvalidValues, dashboardMissingPatterns, dashboardValueFrequency } from '@/model/dashboard-audit'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

const props = defineProps<{ widget: DashboardWidget; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.audit.'
const settings = computed(() => dashboardAuditSettings(props.widget.auditOptions))
const limit = computed(() => Math.min(30, dashboardDataSettings(props.widget.dataOptions).limit))
const rows = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const kind = computed(() => props.widget.kind)
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const integer = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const percent = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: 1 }))
const count = (value: number) => integer.value.format(value)
const label = (value: string | null) => value ?? t(key + 'blank')

type AuditRow = { key: string; first: string; second?: string; count: string; share?: string; samples?: QueryChartRow[] }
type AuditTable = { columns: MfTableColumn[]; rows: AuditRow[]; empty: string; summary?: string; note?: string }
function column(id: string, title: string, width?: number): MfTableColumn {
  return { key: id, dataIndex: id, title, width, align: id === 'count' || id === 'share' ? 'right' : 'left' }
}
const fieldColumn = (id: 'first' | 'second', field: string) => column(id, field)
const namedColumn = (id: 'first' | 'second', title: string) => fieldColumn(id, t(key + title))
const sampleColumn = (title = 'sampleCount') => column('count', t(key + title), 90)

const table = computed<AuditTable>(() => {
  const data = rows.value
  const { fields, rule, ignoreCase, minGapDays } = settings.value
  const field = props.widget.field
  switch (kind.value) {
    case 'duplicateKeys': {
      const result = dashboardDuplicateKeys(data, fields)
      return {
        columns: [namedColumn('first', 'keyValues'), sampleColumn()],
        rows: result.groups.slice(0, limit.value).map(group => ({
          key: group.key, first: group.values.map((value, index) => fields[index] + ' = ' + label(value)).join(' · '),
          count: count(group.rows.length), samples: group.rows,
        })),
        summary: t(key + 'duplicateSummary', { groups: count(result.groups.length), rows: count(result.affected) }),
        note: t(key + 'checkedRows', { count: count(result.evaluated) }),
        empty: t(key + (result.evaluated ? 'noDuplicates' : 'noCompleteKeys')),
      }
    }
    case 'missingPatterns': {
      const result = dashboardMissingPatterns(data, fields)
      return {
        columns: [namedColumn('first', 'missingFields'), sampleColumn()],
        rows: result.groups.slice(0, limit.value).map(group => ({
          key: group.key, first: group.values.join(' · '), count: count(group.rows.length), samples: group.rows,
        })),
        summary: t(key + 'affectedRows', { count: count(result.affected), total: count(data.length) }),
        empty: t(key + 'noMissing'),
      }
    }
    case 'fieldComparison': {
      const result = dashboardFieldComparison(data, fields[0], fields[1], ignoreCase)
      return {
        columns: [fieldColumn('first', fields[0]), fieldColumn('second', fields[1]), sampleColumn()],
        rows: result.groups.slice(0, limit.value).map(group => ({
          key: group.key, first: label(group.values[0]), second: label(group.values[1]), count: count(group.rows.length), samples: group.rows,
        })),
        summary: t(key + 'mismatchSummary', { count: count(result.mismatched), total: count(result.compared) }),
        empty: t(key + (result.compared ? 'noMismatches' : 'noComparableValues')),
      }
    }
    case 'invalidValues': {
      const result = dashboardInvalidValues(data, field, rule)
      return {
        columns: [fieldColumn('first', field), sampleColumn()],
        rows: result.groups.slice(0, limit.value).map(group => ({
          key: group.key, first: label(group.values[0]), count: count(group.rows.length), samples: group.rows,
        })),
        summary: t(key + 'invalidSummary', { count: count(result.invalid), total: count(result.checked) }),
        note: t(key + 'rule.' + rule),
        empty: t(key + (result.checked ? 'noInvalid' : 'noCheckedValues')),
      }
    }
    case 'dateCoverage': {
      const result = dashboardDateCoverage(data, props.widget.timeField ?? '', minGapDays)
      return {
        columns: [namedColumn('first', 'gapStart'), namedColumn('second', 'gapEnd'), sampleColumn('gapDays')],
        rows: result?.gaps.slice(0, limit.value).map(gap => ({ key: gap.start, first: gap.start, second: gap.end, count: count(gap.days) })) ?? [],
        summary: result ? t(key + 'coverageSummary', { observed: count(result.observed), expected: count(result.expected), percent: percent.value.format(result.observed / result.expected) }) : undefined,
        note: result ? `${result.first} – ${result.last} · ${t(key + 'minGapNote', { days: count(minGapDays) })}` : undefined,
        empty: t(key + (result ? 'noGaps' : 'noValidDates')),
      }
    }
    default: {
      const result = dashboardValueFrequency(data, field)
      return {
        columns: [fieldColumn('first', field), sampleColumn(), column('share', t(key + 'share'), 86)],
        rows: result.groups.slice(0, limit.value).map(group => ({
          key: group.key, first: label(group.values[0]), count: count(group.rows.length), share: percent.value.format(group.rows.length / data.length), samples: group.rows,
        })),
        summary: t(key + 'frequencySummary', { values: count(result.distinct), rows: count(data.length) }),
        empty: t('common.noData'),
      }
    }
  }
})
const setupMessage = computed(() => {
  const fields = props.options.fields
  if (kind.value === 'dateCoverage' && !fields.includes(props.widget.timeField ?? '')) return 'dashboard.studio.chooseDateField'
  if ((kind.value === 'valueFrequency' || kind.value === 'invalidValues') && !fields.includes(props.widget.field)) return 'queryChart.selectField'
  if (['duplicateKeys', 'missingPatterns', 'fieldComparison'].includes(kind.value)) {
    const selected = settings.value.fields
    if (selected.length < (kind.value === 'fieldComparison' ? 2 : 1) || !selected.every(field => fields.includes(field))) return key + 'chooseFields'
  }
  return rows.value.length ? '' : 'common.noData'
})
</script>
