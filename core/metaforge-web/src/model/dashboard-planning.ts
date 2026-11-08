import { dashboardDateKey, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { DashboardPlanningOptions } from './dashboard-layout'
import type { QueryChartRow } from './query-chart'

const DAY = 86_400_000

function label(value: unknown) {
  return !dashboardValueMissing(value) && (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') ? String(value) : ''
}

function reportDay(value: unknown) {
  return dashboardTimestamp(value) === undefined ? '' : dashboardDateKey(value)
}

export function dashboardDateAfter(date: string, days: number) {
  const time = Date.parse(date + 'T00:00:00Z') + days * DAY
  const shifted = new Date(time)
  return Number.isFinite(shifted.getTime()) ? shifted.toISOString().slice(0, 10) : ''
}

export interface DashboardPlanTask {
  row: QueryChartRow
  index: number
  name: string
  owner: string
  group: string
  status: string
  due: string
  completed: string
  done: boolean
}

export function dashboardPlanTasks(rows: readonly QueryChartRow[], dueField: string, settings: DashboardPlanningOptions): DashboardPlanTask[] {
  const done = new Set(settings.doneValues.split(/[,，;；\n]+/).map(value => value.trim().toLowerCase()).filter(Boolean))
  return rows.map((row, index) => {
    const status = settings.statusField ? label(row[settings.statusField]) : ''
    return {
      row, index, status,
      name: settings.labelField ? label(row[settings.labelField]) : '',
      owner: settings.ownerField ? label(row[settings.ownerField]) : '',
      group: settings.groupField ? label(row[settings.groupField]) : '',
      due: dueField ? reportDay(row[dueField]) : '',
      completed: settings.completedField ? reportDay(row[settings.completedField]) : '',
      done: done.has(status.trim().toLowerCase()),
    }
  })
}

function partition(tasks: readonly DashboardPlanTask[], field: 'group' | 'due' | 'status') {
  const groups = new Map<string, DashboardPlanTask[]>()
  for (const task of tasks) {
    const value = task[field]
    const group = groups.get(value) ?? []
    group.push(task)
    groups.set(value, group)
  }
  return [...groups].map(([name, tasks]) => ({ name, tasks }))
}

function byDueDate(a: DashboardPlanTask, b: DashboardPlanTask) {
  if (a.due === b.due) return a.index - b.index
  if (!a.due || !b.due) return a.due ? -1 : 1
  return a.due.localeCompare(b.due)
}

export function dashboardTaskBoard(tasks: readonly DashboardPlanTask[], columnLimit: number, cardLimit: number) {
  const groups = partition(tasks, 'status').sort((a, b) => b.tasks.length - a.tasks.length || a.name.localeCompare(b.name))
  return {
    totalGroups: groups.length,
    columns: groups.slice(0, columnLimit).map(group => ({ ...group, cards: [...group.tasks].sort(byDueDate).slice(0, cardLimit) })),
  }
}

export function dashboardAgenda(tasks: readonly DashboardPlanTask[], today: string, horizonDays: number, includeCompleted: boolean, limit: number) {
  const last = dashboardDateAfter(today, horizonDays - 1)
  const selected = tasks.filter(task => task.due && task.due >= today && task.due <= last && (includeCompleted || !task.done)).sort(byDueDate)
  return { total: selected.length, last, days: partition(selected.slice(0, limit), 'due') }
}

export const dashboardAgingBoundaries = [7, 30, 90] as const

export function dashboardOverdueAging(tasks: readonly DashboardPlanTask[], today: string) {
  const buckets = dashboardAgingBoundaries.map(() => [] as DashboardPlanTask[])
  buckets.push([])
  const reference = Date.parse(today + 'T00:00:00Z')
  let total = 0
  for (const task of tasks) {
    if (task.done || !task.due || task.due >= today) continue
    const age = (reference - Date.parse(task.due + 'T00:00:00Z')) / DAY
    const index = dashboardAgingBoundaries.findIndex(days => age <= days)
    buckets[index === -1 ? buckets.length - 1 : index].push(task)
    total++
  }
  return { buckets, total }
}

export function dashboardGroupCompletion(tasks: readonly DashboardPlanTask[]) {
  return partition(tasks, 'group').map(group => ({
    ...group, completed: group.tasks.filter(task => task.done),
  })).sort((a, b) => b.tasks.length - a.tasks.length || a.name.localeCompare(b.name))
}

export function dashboardOnTimeCompletion(tasks: readonly DashboardPlanTask[]) {
  return partition(tasks.filter(task => task.due && task.completed), 'group').map(group => ({
    ...group, onTime: group.tasks.filter(task => task.completed <= task.due),
    late: group.tasks.filter(task => task.completed > task.due),
  })).sort((a, b) => b.tasks.length - a.tasks.length || a.name.localeCompare(b.name))
}

export function dashboardPlanMonth(tasks: readonly DashboardPlanTask[], today: string) {
  const due = tasks.map(task => task.due).filter(Boolean)
  const month = today.slice(0, 7)
  if (!due.length || due.some(date => date.startsWith(month))) return month
  const reference = Date.parse(today + 'T00:00:00Z')
  return due.reduce((nearest, date) => {
    const distance = Math.abs(Date.parse(date + 'T00:00:00Z') - reference)
    const previous = Math.abs(Date.parse(nearest + 'T00:00:00Z') - reference)
    return distance < previous || distance === previous && date < nearest ? date : nearest
  }).slice(0, 7)
}

export function dashboardShiftPlanMonth(month: string, step: number) {
  const date = new Date(month + '-01T00:00:00Z')
  date.setUTCMonth(date.getUTCMonth() + step)
  const year = date.getUTCFullYear()
  return year >= 0 && year <= 9999 ? date.toISOString().slice(0, 7) : month
}

export function dashboardCalendarDays(tasks: readonly DashboardPlanTask[], month: string) {
  const first = month + '-01'
  const weekday = (new Date(first + 'T00:00:00Z').getUTCDay() + 6) % 7
  const byDate = new Map(partition(tasks.filter(task => task.due), 'due').map(group => [group.name, group.tasks]))
  return Array.from({ length: 42 }, (_, index) => {
    const date = dashboardDateAfter(first, index - weekday)
    return { date, inMonth: date.startsWith(month), tasks: byDate.get(date) ?? [] }
  })
}
