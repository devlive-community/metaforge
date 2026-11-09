import { dashboardDateKey, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import { dashboardMetric, type DashboardWidget } from './dashboard-layout'
import type { QueryChartRow } from './query-chart'

const DAY = 86_400_000
const dayKey = (day: number) => new Date(day * DAY).toISOString().slice(0, 10)

export function dashboardFieldQuality(rows: readonly QueryChartRow[], fields: readonly string[]) {
  if (!rows.length) return []
  return fields.map(field => {
    let missing = 0
    const values = new Set<unknown>(), objects = new Set<string>()
    for (const row of rows) {
      const value = row[field]
      if (dashboardValueMissing(value)) missing++
      else if (typeof value === 'object') objects.add(JSON.stringify(value))
      else values.add(value)
    }
    return { field, missing, present: rows.length - missing, distinct: values.size + objects.size, completeness: (1 - missing / rows.length) * 100 }
  })
}

function dashboardDays(rows: readonly QueryChartRow[], field: string) {
  let latest = -Infinity
  const entries = rows.flatMap(row => {
    const date = dashboardDateKey(row[field])
    if (!date) return []
    const day = Date.parse(date + 'T00:00:00Z') / DAY
    latest = Math.max(latest, day)
    return [{ day, row }]
  })
  return { entries, latest }
}

function metric(rows: QueryChartRow[], field: string, operation: DashboardWidget['operation']) {
  if (!rows.length) return null
  const value = dashboardMetric(rows, field, operation)
  return value !== null && Number.isFinite(value) ? value : null
}

export function dashboardPeriodComparison(rows: readonly QueryChartRow[], dateField: string, metricField: string, operation: DashboardWidget['operation'], periodDays: number) {
  const { entries, latest } = dashboardDays(rows, dateField)
  if (!Number.isFinite(latest)) return
  const start = latest - periodDays + 1, previousStart = start - periodDays
  const currentRows: QueryChartRow[] = [], previousRows: QueryChartRow[] = []
  for (const { day, row } of entries) {
    if (day >= start) currentRows.push(row)
    else if (day >= previousStart) previousRows.push(row)
  }
  const current = metric(currentRows, metricField, operation), previous = metric(previousRows, metricField, operation)
  const difference = current !== null && previous !== null ? current - previous : null
  const percent = difference !== null && previous ? difference / Math.abs(previous) * 100 : null
  return {
    current, previous, difference: Number.isFinite(difference) ? difference : null, percent: Number.isFinite(percent) ? percent : null,
    currentRange: [dayKey(start), dayKey(latest)], previousRange: [dayKey(previousStart), dayKey(start - 1)],
  }
}

export function dashboardFreshness(rows: readonly QueryChartRow[], field: string, now: number, staleHours: number) {
  let latest = -Infinity
  for (const row of rows) {
    const time = dashboardTimestamp(row[field])
    if (time !== undefined) latest = Math.max(latest, time)
  }
  if (!Number.isFinite(latest)) return
  const age = (now - latest) / 3_600_000
  return { latest, age, state: age < 0 ? 'future' : age >= staleHours ? 'stale' : 'fresh' } as const
}

export function dashboardSparklines(rows: readonly QueryChartRow[], dateField: string, category: string, field: string, operation: DashboardWidget['operation'], periodDays: number, limit: number) {
  const { entries, latest } = dashboardDays(rows, dateField)
  if (!Number.isFinite(latest) || !category) return { labels: [], items: [], bounds: undefined }
  const start = latest - periodDays + 1
  const groups = new Map<string, { rows: QueryChartRow[]; days: Map<number, QueryChartRow[]> }>()
  for (const { day, row } of entries) {
    const value = row[category]
    if (day < start || dashboardValueMissing(value) || typeof value === 'object') continue
    const name = String(value)
    const group = groups.get(name) ?? { rows: [], days: new Map<number, QueryChartRow[]>() }
    const bucket = group.days.get(day) ?? []
    bucket.push(row)
    group.days.set(day, bucket)
    group.rows.push(row)
    groups.set(name, group)
  }
  const labels = Array.from({ length: periodDays }, (_, i) => dayKey(start + i))
  let min = Infinity, max = -Infinity
  const items = [...groups].sort(([a, first], [b, second]) => second.rows.length - first.rows.length || a.localeCompare(b)).slice(0, limit)
    .map(([name, group]) => {
      const values = labels.map((_, index) => {
        const value = metric(group.days.get(start + index) ?? [], field, operation)
        if (value !== null) { min = Math.min(min, value); max = Math.max(max, value) }
        return value
      })
      return { name, values, total: metric(group.rows, field, operation) }
    }).filter(item => item.total !== null)
  return { labels, items, bounds: Number.isFinite(min) && min !== max ? { min, max } : undefined }
}
