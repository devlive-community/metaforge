<template>
  <MfEmpty v-if="emptyKey" class="h-full" size="sm" :title="$t(emptyKey)"/>
  <div v-else-if="widget.kind === 'incidentFeed'" class="h-full min-h-0 overflow-auto pr-1">
    <section v-for="group in days" :key="group.day" class="mb-2">
      <h4 class="mb-1 text-xs font-medium tabular-nums text-content-tertiary">{{ group.day }}</h4>
      <ol class="ml-1 space-y-2 border-l border-line pl-3">
        <li v-for="event in group.events" :key="event.index" class="relative rounded-mf-md bg-surface-hover p-2">
          <span class="absolute top-3 -left-[17px] size-2 rounded-full bg-primary" aria-hidden="true"/>
          <div class="flex flex-wrap items-center gap-2">
            <time class="text-xs tabular-nums text-content-secondary" :datetime="iso(event.time)">{{ formatClock(event.time) }}</time>
            <strong class="min-w-0 flex-1 truncate text-sm" :title="event.label">{{ event.label || $t('dashboard.studio.ops.event') }}</strong>
            <MfTag v-if="event.status" size="sm" :variant="stateVariant(event.state)">{{ event.status }}</MfTag>
          </div>
          <p v-if="event.details" class="mt-1 whitespace-pre-wrap break-words text-xs leading-5 text-content-secondary">{{ event.details }}</p>
        </li>
      </ol>
    </section>
  </div>
  <div v-else-if="widget.kind === 'outlierQueue'" class="flex h-full min-h-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-2 text-xs">
      <MfTag variant="warning" size="sm">{{ $t('dashboard.studio.ops.matches', { count: outliers.total }) }}</MfTag>
      <span class="text-content-secondary">{{ condition }}</span>
    </div>
    <ol class="min-h-0 space-y-1.5 overflow-auto">
      <li v-for="item in outliers.items" :key="item.index" class="flex flex-wrap items-start gap-2 rounded-mf-md border border-line px-2 py-1.5">
        <div class="min-w-24 flex-1">
          <EllipsisCell :text="cell(item.row[settings.labelField]) || $t('dashboard.studio.ops.recordNumber', { index: item.index + 1 })"/>
          <time v-if="timeOf(item.row)" class="block text-xs tabular-nums text-content-tertiary">{{ timeOf(item.row) }}</time>
        </div>
        <strong class="tabular-nums">{{ measure(item.value) }}</strong>
      </li>
    </ol>
  </div>
  <div v-else-if="widget.kind === 'runHistory'" class="flex h-full min-h-0 flex-col gap-2 overflow-auto">
    <div class="flex flex-wrap items-center gap-1.5">
      <MfTag size="sm" variant="success">{{ $t('dashboard.studio.ops.runPassed', { count: runs.healthy }) }}</MfTag>
      <MfTag size="sm" variant="danger">{{ $t('dashboard.studio.ops.runFailed', { count: runs.failed }) }}</MfTag>
      <MfTag v-if="runs.unknown" size="sm">{{ $t('dashboard.studio.ops.runUnknown', { count: runs.unknown }) }}</MfTag>
    </div>
    <div class="flex flex-wrap gap-1" :aria-label="$t('dashboard.studio.ops.recentRuns')" role="list">
      <span v-for="event in events" :key="event.index" role="listitem" class="inline-grid size-7 place-items-center rounded-mf-sm"
            :class="runClasses[event.state]" :title="runTitle(event)" :aria-label="runTitle(event)">
        <component :is="event.state === 'healthy' ? Check : event.state === 'failed' ? X : Minus" :size="15" aria-hidden="true"/>
      </span>
    </div>
    <p class="text-xs text-content-secondary">{{ $t('dashboard.studio.ops.streak', { count: runs.streak }) }}</p>
  </div>
  <div v-else-if="widget.kind === 'slaScorecard' && sla" class="flex h-full min-h-0 flex-col justify-center gap-1 overflow-auto">
    <div class="flex flex-wrap items-baseline justify-between gap-1">
      <strong class="text-xl leading-tight tabular-nums">{{ number.format(sla.percent) }}%</strong>
      <MfTag size="sm" :variant="sla.percent >= settings.goal ? 'success' : 'danger'">
        {{ $t(sla.percent >= settings.goal ? 'dashboard.studio.ops.targetMet' : 'dashboard.studio.ops.targetMissed') }}
      </MfTag>
    </div>
    <ProgressMeter class="shrink-0" :value="sla.percent" :target="100" :label="$t('dashboard.studio.ops.compliance')"/>
    <div class="flex flex-wrap justify-between gap-1 text-xs text-content-secondary">
      <span>{{ $t('dashboard.studio.ops.goalValue', { goal: number.format(settings.goal) }) }}</span>
      <span>{{ $t('dashboard.studio.ops.sampleRatio', { passed: sla.passed, total: sla.total }) }}</span>
    </div>
    <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.ops.passCondition') }}: {{ condition }}</p>
  </div>
  <div v-else-if="widget.kind === 'latencyPercentiles' && percentiles" class="flex h-full min-h-0 flex-col gap-1.5 overflow-auto">
    <div class="grid grid-cols-[repeat(auto-fit,minmax(min(100%,6rem),1fr))] gap-2">
      <div v-for="item in percentiles.quantiles" :key="item.percentile" class="min-w-0 rounded-mf-md bg-surface-hover px-2 py-1">
        <span class="text-xs text-content-secondary">P{{ item.percentile }}</span>
        <strong class="block truncate tabular-nums" :title="measure(item.value)">{{ measure(item.value) }}</strong>
      </div>
    </div>
    <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.ops.sampleCount', { count: percentiles.count }) }}</p>
  </div>
  <div v-else-if="widget.kind === 'maintenanceWindow'" class="h-full min-h-0 space-y-2 overflow-auto">
    <article v-for="window in maintenance" :key="window.index" class="rounded-mf-md border border-line px-2 py-1.5">
      <div class="flex flex-wrap items-center justify-between gap-2">
        <strong class="min-w-0 flex-1 truncate text-sm" :title="window.label">{{ window.label || $t('dashboard.studio.ops.window') }}</strong>
        <MfTag size="sm" :variant="window.active ? 'warning' : 'info'">{{ $t(window.active ? 'dashboard.studio.ops.activeWindow' : 'dashboard.studio.ops.upcomingWindow') }}</MfTag>
      </div>
      <p class="mt-1 text-xs tabular-nums text-content-secondary">
        <time :datetime="iso(window.start)">{{ formatDateTime(window.start) }}</time> – <time :datetime="iso(window.end)">{{ formatDateTime(window.end) }}</time>
      </p>
      <p v-if="window.details" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-secondary">{{ window.details }}</p>
    </article>
  </div>
  <div v-else-if="widget.kind === 'recordLookup'" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap gap-2">
      <MfInput v-model="search" class="min-w-32 flex-1" size="sm" :disabled="editable" :maxlength="256" clearable
               :aria-label="$t('dashboard.studio.ops.searchRecords')" :placeholder="$t('dashboard.studio.ops.searchRecords')">
        <template #prefix><Search :size="14"/></template>
      </MfInput>
      <MfSelect class="min-w-32 flex-1" size="sm" :model-value="active?.index" :disabled="editable || !choices.length" :options="choices"
                :aria-label="$t('dashboard.studio.ops.chooseRecord')" :placeholder="$t('dashboard.studio.ops.chooseRecord')" clearable
                @update:model-value="selectRecord"/>
    </div>
    <dl v-if="active" class="min-h-0 overflow-auto rounded-mf-md border border-line text-sm">
      <div v-for="field in detailFields" :key="field" class="grid grid-cols-[minmax(0,1fr)_minmax(0,2fr)] gap-2 border-b border-line p-2 last:border-b-0">
        <dt class="break-words text-content-secondary">{{ field }}</dt><dd class="whitespace-pre-wrap break-words">{{ detailValue(active.row, field) }}</dd>
      </div>
    </dl>
    <MfEmpty v-else class="min-h-0 flex-1" size="sm" :title="$t(results.length ? 'dashboard.studio.ops.chooseRecord' : 'common.noData')"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Minus, Search, X } from '@lucide/vue'
import { dashboardDataSettings, dashboardOperationsSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardTimestamp } from '@/model/dashboard-filters'
import { dashboardCellText as cell, dashboardEvents, dashboardMaintenance, dashboardOutliers, dashboardPercentiles, dashboardRecordChoices, dashboardRunSummary, dashboardSla, type DashboardRunState } from '@/model/dashboard-operations'
import { aggregateQueryChart, chartFieldKind, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { formatClock, formatDateTime, formatDay } from '@/utils/date'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'dataOptions' | 'displayFields' | 'field' | 'kind' | 'operationsOptions' | 'timeField'>; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const { t, locale } = useI18n()
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardOperationsSettings(kind.value, props.widget.operationsOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const rows = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const number = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: data.value.decimals }))
const condition = computed(() => t('dashboard.studio.ops.' + settings.value.comparator) + ' ' + measure(settings.value.threshold))
function measure(value: number) { return data.value.prefix + number.value.format(value) + data.value.suffix }
const iso = (time: number) => new Date(time).toISOString()
function timeOf(row: QueryChartRow) {
  const time = dashboardTimestamp(row[props.widget.timeField ?? ''])
  return time === undefined ? '' : formatDateTime(time)
}
const outliers = computed(() => dashboardOutliers(rows.value, props.widget.field, settings.value.threshold, settings.value.comparator, data.value.limit))
const sla = computed(() => dashboardSla(rows.value, props.widget.field, settings.value.threshold, settings.value.comparator))
const percentiles = computed(() => dashboardPercentiles(rows.value, props.widget.field))
const events = computed(() => dashboardEvents(rows.value, props.widget.timeField || '', settings.value, data.value.limit))
const runs = computed(() => dashboardRunSummary(events.value))
const days = computed(() => {
  const groups = new Map<string, typeof events.value>()
  for (const event of events.value) {
    const day = formatDay(event.time)
    const group = groups.get(day) ?? []
    group.push(event)
    groups.set(day, group)
  }
  return [...groups].map(([day, events]) => ({ day, events }))
})
function stateVariant(state: DashboardRunState) { return state === 'healthy' ? 'success' : state === 'failed' ? 'danger' : 'default' }
const runClasses = { healthy: 'bg-success-subtle text-success', failed: 'bg-danger-subtle text-danger', unknown: 'bg-surface-hover text-content-tertiary' }
function runTitle(event: typeof events.value[number]) {
  return [event.label, event.status || t('dashboard.studio.ops.unknownStatus'), formatDateTime(event.time)].filter(Boolean).join(' · ')
}

const now = ref(Date.now())
watch([kind, () => props.editable], ([value, editable], _, cleanup) => {
  if (value !== 'maintenanceWindow' || editable) return
  now.value = Date.now()
  const timer = setInterval(() => { now.value = Date.now() }, 15_000)
  cleanup(() => clearInterval(timer))
}, { immediate: true })
const maintenance = computed(() => dashboardMaintenance(rows.value, props.widget.timeField || '', settings.value, now.value, data.value.limit))
const search = ref('')
const selected = shallowRef<QueryChartRow>()
const detailFields = computed(() => (props.widget.displayFields?.length ? props.widget.displayFields : props.options.fields).slice(0, 12))
const dateDetailFields = computed(() => new Set(detailFields.value.filter(field => chartFieldKind(props.rows, field) === 'date')))
function detailValue(row: QueryChartRow, field: string) {
  const raw = row[field], time = dateDetailFields.value.has(field) ? dashboardTimestamp(raw) : undefined
  return time !== undefined ? formatDateTime(time) : cell(raw) || '—'
}
const results = computed(() => dashboardRecordChoices(rows.value, settings.value.labelField, detailFields.value, search.value, data.value.limit))
const choices = computed(() => results.value.map(item => ({ value: item.index, label: item.label + ' · #' + (item.index + 1) })))
const active = computed(() => results.value.find(item => item.row === selected.value))
function selectRecord(value: string | number | (string | number)[]) {
  selected.value = typeof value === 'number' ? results.value.find(item => item.index === value)?.row : undefined
}
watch(results, () => { if (!active.value) selected.value = undefined })
const emptyKey = computed(() => {
  if (['outlierQueue', 'slaScorecard', 'latencyPercentiles'].includes(kind.value) && !props.widget.field) return 'queryChart.selectField'
  if (['incidentFeed', 'runHistory', 'maintenanceWindow'].includes(kind.value) && !props.widget.timeField) return 'dashboard.studio.chooseDateField'
  if (kind.value === 'maintenanceWindow' && !settings.value.endField) return 'dashboard.studio.ops.chooseEnd'
  if (kind.value === 'runHistory' && !settings.value.statusField) return 'dashboard.studio.ops.chooseStatus'
  if (kind.value === 'recordLookup' && !settings.value.labelField) return 'queryChart.selectField'
  if (kind.value === 'outlierQueue') return outliers.value.samples === 0 ? 'common.noData' : outliers.value.total ? '' : 'dashboard.studio.ops.noOutliers'
  if (kind.value === 'slaScorecard') return sla.value ? '' : 'common.noData'
  if (kind.value === 'latencyPercentiles') return percentiles.value ? '' : 'common.noData'
  if (kind.value === 'maintenanceWindow') return maintenance.value.length ? '' : 'dashboard.studio.ops.noWindows'
  if (kind.value === 'recordLookup') return rows.value.length ? '' : 'common.noData'
  return events.value.length ? '' : 'common.noData'
})
</script>
