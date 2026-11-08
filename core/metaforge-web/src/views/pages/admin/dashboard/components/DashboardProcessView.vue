<template>
  <MfEmpty v-if="setupMessage" class="h-full" size="sm" :title="$t(setupMessage)"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'traceCount', { count: count(traces.traces.length) }) }}</MfTag>
      <span v-if="traces.invalid" class="text-content-secondary">{{ $t(key + 'invalidEvents', { count: count(traces.invalid) }) }}</span>
      <span v-if="traces.ambiguous" class="text-content-secondary">{{ $t(key + 'ambiguousEntities', { count: count(traces.ambiguous) }) }}</span>
    </div>
    <MfEmpty v-if="!traces.traces.length" class="min-h-0 flex-1" size="sm" :title="$t(key + 'noTraces')"/>
    <ol v-else-if="kind === 'processPaths'" class="min-h-0 flex-1 space-y-2 overflow-auto">
      <li v-for="path in paths" :key="path.key" class="flex min-w-0 flex-wrap items-center justify-between gap-2 rounded-mf-md border border-line px-2.5 py-1">
        <div class="flex min-w-0 flex-1 flex-wrap items-center gap-1">
          <template v-for="(stage, index) in path.stages.slice(0, 8)" :key="index">
            <ArrowRight v-if="index" :size="12" class="shrink-0 text-content-tertiary" aria-hidden="true"/>
            <span class="max-w-full break-words rounded-mf-sm bg-surface-hover px-1.5 py-0.5 text-xs font-medium">{{ stage }}</span>
          </template>
          <span v-if="path.stages.length > 8" class="text-xs text-content-tertiary">{{ $t(key + 'extraStages', { count: count(path.stages.length - 8) }) }}</span>
        </div>
        <MfButton v-if="interactive" size="sm" variant="ghost" class="h-auto! shrink-0 py-1! text-primary"
                  :aria-label="linkLabel(path.stages.slice(0, 8).join(' → '), path.count)" @click="link(path.rows)">{{ count(path.count) }}</MfButton>
        <span v-else class="tabular-nums">{{ count(path.count) }}</span>
        <span class="shrink-0 text-xs tabular-nums text-content-secondary">{{ percent(path.count) }}</span>
      </li>
    </ol>
    <div v-else-if="kind === 'processEndpoints'" class="grid min-h-0 flex-1 grid-cols-[repeat(auto-fit,minmax(min(100%,10rem),1fr))] gap-2 overflow-auto">
      <section v-for="side in sides" :key="side.key" class="min-w-0 rounded-mf-md border border-line px-2.5 py-2">
        <h4 class="mb-2 text-sm font-medium">{{ $t(key + side.key) }}</h4>
        <ol class="space-y-2">
          <li v-for="item in side.rows" :key="item.stage" class="min-w-0">
            <div class="mb-1 flex min-w-0 items-center justify-between gap-1 text-xs">
              <EllipsisCell :text="item.stage" class="min-w-0 flex-1"/>
              <MfButton v-if="interactive" size="sm" variant="ghost" class="h-auto! px-1.5! py-0.5! tabular-nums text-primary"
                        :aria-label="linkLabel($t(key + side.key) + ' / ' + item.stage, item.count)" @click="link(item.rows)">{{ count(item.count) }}</MfButton>
              <span v-else class="tabular-nums">{{ count(item.count) }}</span>
            </div>
            <DashboardProgress :value="item.count" :target="traces.traces.length" :label="item.stage"/>
          </li>
        </ol>
      </section>
    </div>
    <div v-else-if="kind === 'processCompliance'" class="flex min-h-0 flex-1 flex-col gap-2 overflow-auto">
      <div class="grid grid-cols-[repeat(auto-fit,minmax(min(100%,8rem),1fr))] gap-2">
        <section v-for="item in compliance" :key="item.label" class="min-w-0 rounded-mf-md border border-line bg-surface-hover px-2.5 py-1">
          <p class="text-xs font-medium text-content-secondary">{{ $t(key + item.label) }}</p>
          <div class="flex flex-wrap items-center justify-between gap-1">
            <MfButton v-if="interactive && item.count" size="sm" variant="ghost" class="h-auto! py-0.5! text-base font-semibold tabular-nums text-primary"
                      :aria-label="linkLabel($t(key + item.label), item.count)" @click="link(item.rows)">{{ count(item.count) }}</MfButton>
            <span v-else class="py-1 text-base font-semibold tabular-nums">{{ count(item.count) }}</span>
            <span class="text-xs tabular-nums text-content-secondary">{{ percent(item.count) }}</span>
          </div>
        </section>
      </div>
      <div class="flex flex-wrap items-center gap-1 text-xs text-content-secondary">
        <span class="mr-1">{{ $t(key + 'requiredStages') }}</span>
        <template v-for="(stage, index) in settings.requiredStages" :key="stage">
          <ArrowRight v-if="index" :size="12" class="shrink-0 text-content-tertiary" aria-hidden="true"/>
          <MfTag size="sm">{{ stage }}</MfTag>
        </template>
      </div>
    </div>
    <div v-else class="flex min-h-0 flex-1 flex-col gap-1 overflow-auto">
      <p class="shrink-0 text-xs text-content-tertiary">{{ $t(table.note) }}</p>
      <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="table.columns" :data-source="table.rows" row-key="key" :empty-text="$t(table.empty)">
        <template #cell-name="{ record }"><EllipsisCell :text="record.name"/></template>
        <template #cell-count="{ record }">
          <MfButton v-if="interactive" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                    :aria-label="linkLabel(record.name, record.count)" @click="link(record.rows)">{{ count(record.count) }}</MfButton>
          <span v-else class="tabular-nums">{{ count(record.count) }}</span>
        </template>
        <template #cell-first="{ record }"><span class="tabular-nums">{{ record.first }}</span></template>
        <template #cell-second="{ record }"><span class="tabular-nums">{{ record.second }}</span></template>
      </MfTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight } from '@lucide/vue'
import type { MfTableColumn } from 'metaforge-ui'
import { dashboardDataSettings, dashboardProcessSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardProcessCompliance, dashboardProcessCycles, dashboardProcessEndpoints, dashboardProcessPaths, dashboardProcessRework,
  dashboardProcessTraces, dashboardProcessWaits } from '@/model/dashboard-process'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import DashboardProgress from './DashboardProgress.vue'

const props = defineProps<{ widget: DashboardWidget; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.process.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardProcessSettings(props.widget.processOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const limit = computed(() => Math.min(30, data.value.limit))
const source = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const traces = computed(() => dashboardProcessTraces(source.value, settings.value.entityField, settings.value.stageField, props.widget.timeField ?? ''))
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const integer = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const number = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const percentages = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: 1 }))
const count = (value: number) => integer.value.format(value)
const percent = (value: number) => percentages.value.format(value / traces.value.traces.length)
const linkLabel = (name: string, value: number) => t('dashboard.studio.relation.filterCell', { name, value: count(value) })
const unitMs = { minutes: 60_000, hours: 3_600_000, days: 86_400_000 }
const duration = (ms: number) => number.value.format(ms / unitMs[settings.value.unit]) + ' ' + t('dashboard.studio.temporal.unitLabel.' + settings.value.unit)
function link(rows: QueryChartRow[]) { if (rows.length) emit('filter', rows) }

const paths = computed(() => dashboardProcessPaths(traces.value.traces, limit.value))
const sides = computed(() => {
  const endpoints = dashboardProcessEndpoints(traces.value.traces, limit.value)
  return [{ key: 'entry', rows: endpoints.entry }, { key: 'exit', rows: endpoints.exit }]
})
const compliance = computed(() => dashboardProcessCompliance(traces.value.traces, settings.value.requiredStages))

type Table = { columns: MfTableColumn[]; rows: { key: string; name: string; count: number; rows: QueryChartRow[]; first: string; second?: string }[]; note: string; empty: string }
function columns(name: string, sample: string, first: string, second?: string): MfTableColumn[] {
  return [
    { title: t(key + name), dataIndex: 'name' },
    { title: t(key + sample), dataIndex: 'count', align: 'right' },
    { title: t(key + first), dataIndex: 'first', align: 'right' },
    ...(second ? [{ title: t(key + second), dataIndex: 'second', align: 'right' as const }] : []),
  ]
}
const table = computed<Table>(() => {
  if (kind.value === 'processWaits') {
    const results = dashboardProcessWaits(traces.value.traces, limit.value)
    return {
      columns: columns('transition', 'transitionCount', 'median', 'p90'),
      rows: results.map(item => ({ key: item.key, name: item.from + ' → ' + item.to, count: item.count, rows: item.rows, first: duration(item.median), second: duration(item.p90) })),
      note: key + 'waitHint', empty: key + 'noTransitions',
    }
  }
  if (kind.value === 'processRework') {
    const results = dashboardProcessRework(traces.value.traces, limit.value)
    return {
      columns: columns('stage', 'entityCount', 'revisitCount'),
      rows: results.map(item => ({ key: item.stage, name: item.stage, count: item.count, rows: item.rows, first: count(item.revisits) })),
      note: key + 'reworkHint', empty: key + 'noRework',
    }
  }
  const results = dashboardProcessCycles(traces.value.traces, limit.value)
  return {
    columns: columns('exit', 'entityCount', 'median', 'p90'),
    rows: results.map(item => ({ key: item.stage, name: item.stage, count: item.count, rows: item.rows, first: duration(item.median), second: duration(item.p90) })),
    note: key + 'cycleHint', empty: key + 'noCycles',
  }
})
const setupMessage = computed(() => {
  const { entityField, stageField, requiredStages } = settings.value
  const fields = [entityField, stageField, props.widget.timeField ?? '']
  if (new Set(fields).size !== 3 || !fields.every(field => props.options.fields.includes(field))) return key + 'chooseFields'
  if (kind.value === 'processCompliance' && requiredStages.length < 2) return key + 'chooseRequired'
  return source.value.length ? '' : 'common.noData'
})
</script>
