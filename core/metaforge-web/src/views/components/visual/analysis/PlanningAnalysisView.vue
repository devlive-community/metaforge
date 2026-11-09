<template>
  <MfEmpty v-if="setupMessage" class="h-full" size="sm" :title="$t(setupMessage)"/>
  <div v-else-if="kind === 'taskBoard'" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <p v-if="board.totalGroups > board.columns.length" class="shrink-0 text-xs text-content-tertiary">
      {{ $t(key + 'visibleColumns', { shown: count(board.columns.length), total: count(board.totalGroups) }) }}
    </p>
    <div class="grid min-h-0 flex-1 auto-cols-[minmax(10rem,1fr)] grid-flow-col gap-2 overflow-auto">
      <section v-for="column in board.columns" :key="column.name" class="flex min-h-0 min-w-0 flex-col gap-1.5 rounded-mf-md border border-line bg-surface-hover p-2">
        <div class="flex shrink-0 items-center justify-between gap-1">
          <EllipsisCell :text="column.name || $t(key + 'noStatus')" class="min-w-0 flex-1 font-medium"/>
          <MfButton v-if="interactive" size="sm" variant="ghost" class="shrink-0 text-primary"
                    :aria-label="linkLabel(column.name || $t(key + 'noStatus'), column.tasks.length)" @click="link(column.tasks)">{{ count(column.tasks.length) }}</MfButton>
          <MfTag v-else size="sm">{{ count(column.tasks.length) }}</MfTag>
        </div>
        <ol class="min-h-0 space-y-1 overflow-auto">
          <li v-for="task in column.cards" :key="task.index" class="min-w-0 rounded-mf-sm border border-line bg-surface p-2">
            <MfButton v-if="interactive" size="sm" variant="ghost" class="max-w-full px-0! text-left"
                      :aria-label="linkLabel(taskName(task), 1)" @click="link([task])"><span class="truncate">{{ taskName(task) }}</span></MfButton>
            <EllipsisCell v-else :text="taskName(task)" class="font-medium"/>
            <p v-if="task.owner || task.due" class="break-words text-xs text-content-secondary">{{ [task.owner, task.due].filter(Boolean).join(' · ') }}</p>
          </li>
        </ol>
      </section>
    </div>
  </div>
  <div v-else-if="kind === 'planCalendar'" class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <div class="flex shrink-0 flex-wrap items-center justify-between gap-1.5">
      <strong class="tabular-nums">{{ month }}</strong>
      <div class="flex gap-1">
        <MfButton size="sm" variant="ghost" :disabled="editable" @click="selectedMonth = today.slice(0, 7)">{{ $t(key + 'today') }}</MfButton>
        <MfButton size="sm" variant="ghost" :disabled="editable" :aria-label="$t(key + 'previousMonth')" @click="moveMonth(-1)"><ChevronLeft :size="16"/></MfButton>
        <MfButton size="sm" variant="ghost" :disabled="editable" :aria-label="$t(key + 'nextMonth')" @click="moveMonth(1)"><ChevronRight :size="16"/></MfButton>
      </div>
    </div>
    <div class="grid shrink-0 grid-cols-7 gap-1 text-center text-xs text-content-tertiary">
      <span v-for="weekday in weekdays" :key="weekday">{{ weekday }}</span>
    </div>
    <div class="grid shrink-0 grid-cols-7 gap-1">
      <div v-for="(day, index) in calendar" :key="index" class="min-w-0 rounded-mf-sm border border-line"
           :class="[day.date === today && 'border-primary/45 bg-primary/10', !day.inMonth && 'text-content-tertiary']">
        <MfButton v-if="interactive && day.tasks.length" size="sm" variant="ghost" class="h-auto! min-h-7 w-full gap-1 px-0! py-0.5!"
                  :aria-label="linkLabel(day.date, day.tasks.length)" @click="link(day.tasks)">
          <span class="text-xs tabular-nums">{{ day.date.slice(-2) }}</span>
          <span class="rounded-mf-sm bg-primary/10 px-0.5 text-[10px] font-semibold tabular-nums text-primary">{{ calendarCount(day.tasks.length) }}</span>
        </MfButton>
        <div v-else class="flex min-h-7 items-center justify-center gap-1 px-0 py-0.5 text-xs tabular-nums">
          <span>{{ day.date.slice(-2) }}</span>
          <span v-if="day.tasks.length" class="rounded-mf-sm bg-primary/10 px-0.5 text-[10px] font-semibold text-primary">{{ calendarCount(day.tasks.length) }}</span>
        </div>
      </div>
    </div>
  </div>
  <div v-else-if="kind === 'dueAgenda'" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'taskCount', { count: count(agenda.total) }) }}</MfTag>
      <span class="text-content-secondary">{{ today }} – {{ agenda.last }}</span>
    </div>
    <MfEmpty v-if="!agenda.total" class="min-h-0 flex-1" size="sm" :title="$t(key + 'noUpcoming')"/>
    <ol v-else class="min-h-0 space-y-2 overflow-auto">
      <li v-for="day in agenda.days" :key="day.name" class="rounded-mf-md border border-line">
        <div class="flex items-center justify-between gap-2 bg-surface-hover px-2.5 py-1.5 text-sm font-medium tabular-nums">{{ day.name }}</div>
        <ul class="divide-y divide-line">
          <li v-for="task in day.tasks" :key="task.index" class="flex min-w-0 flex-wrap items-center gap-x-2 gap-y-0.5 px-2.5 py-1">
            <MfButton v-if="interactive" variant="ghost" size="sm" class="min-w-0 max-w-full px-0!"
                      :aria-label="linkLabel(taskName(task), 1)" @click="link([task])"><span class="truncate">{{ taskName(task) }}</span></MfButton>
            <EllipsisCell v-else :text="taskName(task)"/>
            <span v-if="task.owner" class="break-words text-xs text-content-secondary">{{ task.owner }}</span>
            <MfTag v-if="task.done" size="sm" variant="success">{{ $t(key + 'done') }}</MfTag>
          </li>
        </ul>
      </li>
    </ol>
  </div>
  <div v-else-if="kind === 'overdueAging'" class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center gap-1.5 text-xs">
      <MfTag size="sm" variant="primary">{{ $t(key + 'overdueCount', { count: count(aging.total) }) }}</MfTag>
      <span class="text-content-secondary">{{ $t(key + 'referenceDate', { date: today }) }}</span>
    </div>
    <MfEmpty v-if="!aging.total" class="min-h-0 flex-1" size="sm" :title="$t(key + 'noOverdue')"/>
    <ol v-else class="min-h-0 space-y-1.5 overflow-auto">
      <li v-for="(bucket, index) in aging.buckets" :key="index" class="rounded-mf-md border border-line px-2.5 py-1">
        <div class="mb-1 flex min-w-0 flex-wrap items-center justify-between gap-1 text-xs">
          <span>{{ $t(key + 'aging.' + index) }}</span>
          <MfButton v-if="interactive && bucket.length" size="sm" variant="ghost" class="h-auto! px-2! py-0.5! tabular-nums text-primary"
                    :aria-label="linkLabel($t(key + 'aging.' + index), bucket.length)" @click="link(bucket)">{{ count(bucket.length) }}</MfButton>
          <span v-else class="tabular-nums">{{ count(bucket.length) }}</span>
        </div>
        <ProgressMeter :value="bucket.length" :target="aging.total" :label="$t(key + 'aging.' + index)"/>
      </li>
    </ol>
  </div>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2 overflow-auto">
    <p v-if="kind === 'onTimeCompletion'" class="shrink-0 text-xs text-content-tertiary">{{ $t(key + 'onTimeHint') }}</p>
    <MfTable class="min-h-[4.25rem]! flex-1" fill density="compact" :columns="statColumns" :data-source="stats" row-key="key" :empty-text="$t(key + 'noCompletedDates')">
      <template #cell-name="{ record }"><EllipsisCell :text="record.name"/></template>
      <template #cell-passed="{ record }">
        <MfButton v-if="interactive && record.passed.length" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                  :aria-label="linkLabel(record.name + ' / ' + passedLabel, record.passed.length)" @click="link(record.passed)">{{ count(record.passed.length) }}</MfButton>
        <span v-else class="tabular-nums">{{ count(record.passed.length) }}</span>
      </template>
      <template #cell-rest="{ record }">
        <MfButton v-if="interactive && record.rest.length" size="sm" variant="ghost" class="min-w-8 tabular-nums text-primary"
                  :aria-label="linkLabel(record.name + ' / ' + restLabel, record.rest.length)" @click="link(record.rest)">{{ count(record.rest.length) }}</MfButton>
        <span v-else class="tabular-nums">{{ count(record.rest.length) }}</span>
      </template>
      <template #cell-rate="{ record }"><span class="tabular-nums">{{ percentage.format(record.passed.length / record.total) }}</span></template>
    </MfTable>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronLeft, ChevronRight } from '@lucide/vue'
import type { MfTableColumn } from 'metaforge-ui'
import { dashboardDataSettings, dashboardPlanningSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardAgenda, dashboardCalendarDays, dashboardGroupCompletion, dashboardOnTimeCompletion, dashboardOverdueAging,
  dashboardPlanMonth, dashboardPlanTasks, dashboardShiftPlanMonth, dashboardTaskBoard, type DashboardPlanTask } from '@/model/dashboard-planning'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { formatDay } from '@/utils/date'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'dataOptions' | 'kind' | 'planningOptions' | 'timeField'>; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.planning.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardPlanningSettings(props.widget.planningOptions))
const limit = computed(() => Math.min(30, dashboardDataSettings(props.widget.dataOptions).limit))
const source = computed(() => aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered)
const tasks = computed(() => dashboardPlanTasks(source.value, props.widget.timeField ?? '', settings.value))
const interactive = computed(() => !props.editable && props.widget.action === 'filter')
const integer = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 0 }))
const percentage = computed(() => new Intl.NumberFormat(locale.value, { style: 'percent', maximumFractionDigits: 1 }))
const count = (value: number) => integer.value.format(value)
const calendarCount = (value: number) => value > 99 ? '99+' : count(value)
const linkLabel = (name: string, value: number) => t('dashboard.studio.relation.filterCell', { name, value: count(value) })
const taskName = (task: DashboardPlanTask) => task.name || t(key + 'taskName', { index: count(task.index + 1) })
function link(tasks: DashboardPlanTask[]) { if (tasks.length) emit('filter', tasks.map(task => task.row)) }

const today = ref(formatDay(new Date()))
const usesToday = computed(() => !props.editable && ['planCalendar', 'dueAgenda', 'overdueAging'].includes(kind.value))
watch(usesToday, (enabled, _, cleanup) => {
  if (!enabled) return
  const tick = () => { today.value = formatDay(new Date()) }
  tick()
  const timer = window.setInterval(tick, 60_000)
  cleanup(() => window.clearInterval(timer))
}, { immediate: true })

const selectedMonth = ref('')
const month = computed(() => selectedMonth.value || dashboardPlanMonth(tasks.value, today.value))
function moveMonth(step: number) { selectedMonth.value = dashboardShiftPlanMonth(month.value, step) }
const weekdays = computed(() => {
  const format = new Intl.DateTimeFormat(locale.value, { weekday: 'short', timeZone: 'UTC' })
  return Array.from({ length: 7 }, (_, index) => format.format(new Date(Date.UTC(2024, 0, 1 + index))))
})
const calendar = computed(() => dashboardCalendarDays(tasks.value, month.value))
const board = computed(() => dashboardTaskBoard(tasks.value, 6, limit.value))
const agenda = computed(() => dashboardAgenda(tasks.value, today.value, settings.value.horizonDays, settings.value.includeCompleted, limit.value))
const aging = computed(() => dashboardOverdueAging(tasks.value, today.value))

const onTime = computed(() => kind.value === 'onTimeCompletion')
const passedLabel = computed(() => t(key + (onTime.value ? 'onTime' : 'done')))
const restLabel = computed(() => t(key + (onTime.value ? 'late' : 'open')))
const statColumns = computed<MfTableColumn[]>(() => [
  { title: t(key + 'group'), dataIndex: 'name' },
  { title: passedLabel.value, dataIndex: 'passed', align: 'right' },
  { title: restLabel.value, dataIndex: 'rest', align: 'right' },
  { title: t(key + (onTime.value ? 'onTimeRate' : 'completionRate')), dataIndex: 'rate', align: 'right' },
])
function groupName(value: string) { return value || t(key + (settings.value.groupField ? 'noGroup' : 'allGroups')) }
const stats = computed(() => {
  if (onTime.value) {
    return dashboardOnTimeCompletion(tasks.value).slice(0, limit.value).map(group => ({
      key: group.name, name: groupName(group.name), passed: group.onTime, rest: group.late, total: group.tasks.length,
    }))
  }
  return dashboardGroupCompletion(tasks.value).slice(0, limit.value).map(group => ({
    key: group.name, name: groupName(group.name), passed: group.completed, rest: group.tasks.filter(task => !task.done), total: group.tasks.length,
  }))
})
const setupMessage = computed(() => {
  const fields = props.options.fields, current = kind.value
  if (['taskBoard', 'groupCompletion', 'overdueAging'].includes(current) && !fields.includes(settings.value.statusField)) return key + 'chooseStatus'
  if (['planCalendar', 'dueAgenda', 'overdueAging', 'onTimeCompletion'].includes(current) && !fields.includes(props.widget.timeField ?? '')) return key + 'chooseDue'
  if (onTime.value && (!fields.includes(settings.value.completedField) || settings.value.completedField === props.widget.timeField)) return key + 'chooseCompleted'
  if (!tasks.value.length) return 'common.noData'
  if (current === 'planCalendar' && !tasks.value.some(task => task.due)) return key + 'noDueDates'
  return ''
})
</script>
