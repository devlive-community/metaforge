import { dashboardDateKey, dashboardNumericValue, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

const DAY = 86_400_000
type Sample = { value: number; rows: QueryChartRow[] }
type Aggregation = 'count' | 'sum'
const dateAt = (day: number) => new Date(day * DAY).toISOString().slice(0, 10)
const finite = (value: number) => Number.isFinite(value) ? value : null
const measure = (row: QueryChartRow, field: string, operation: Aggregation) => operation === 'count' ? 1 : dashboardNumericValue(row[field])

function reportDate(value: unknown) {
  if (dashboardTimestamp(value) === undefined) return ''
  return dashboardDateKey(value)
}

function addSample<T>(buckets: Map<T, Sample>, bucket: T, row: QueryChartRow, value: number) {
  const sample = buckets.get(bucket) ?? { value: 0, rows: [] }
  sample.value += value
  sample.rows.push(row)
  buckets.set(bucket, sample)
}

export function dashboardDailySeries(rows: readonly QueryChartRow[], dateField: string, valueField: string, operation: Aggregation, historyDays: number, windowDays: number) {
  const buckets = new Map<number, Sample>()
  for (const row of rows) {
    const date = reportDate(row[dateField]), value = measure(row, valueField, operation)
    if (!date || value === undefined) continue
    addSample(buckets, Date.parse(date + 'T00:00:00Z') / DAY, row, value)
  }
  const days = [...buckets.keys()].sort((a, b) => a - b)
  const earliest = days[0], latest = days.at(-1)
  if (latest === undefined) return { buckets, points: [] }
  const start = Math.max(earliest, latest - historyDays + 1)
  let cumulative = 0
  for (const day of days) {
    if (day >= start) break
    cumulative += buckets.get(day)!.value
  }
  const points = []
  for (let day = start; day <= latest; day++) {
    const daily = buckets.get(day)?.value
    if (daily !== undefined) cumulative += daily
    const windowStart = Math.max(earliest, day - windowDays + 1)
    let observed = 0, sum = 0
    for (let date = windowStart; date <= day; date++) {
      const value = buckets.get(date)?.value
      if (value !== undefined && Number.isFinite(value)) {
        sum += value
        observed++
      }
    }
    points.push({ day, date: dateAt(day), daily: daily === undefined ? null : finite(daily), cumulative: finite(cumulative),
      average: observed ? finite(sum / observed) : null, windowStart })
  }
  return { buckets, points }
}

export function dashboardMonthlyComparison(rows: readonly QueryChartRow[], dateField: string, valueField: string, operation: Aggregation, year: number) {
  const buckets = new Map<number, Sample>()
  let latest = 0
  for (const row of rows) {
    const date = reportDate(row[dateField]), value = measure(row, valueField, operation)
    if (!date || value === undefined) continue
    const rowYear = Number(date.slice(0, 4)), month = Number(date.slice(5, 7))
    latest = Math.max(latest, rowYear)
    addSample(buckets, rowYear * 12 + month - 1, row, value)
  }
  const selected = year || latest
  const months = Array.from({ length: 12 }, (_, month) => ({
    current: buckets.get(selected * 12 + month),
    previous: buckets.get((selected - 1) * 12 + month),
  }))
  return { year: selected, months }
}

export function dashboardWeekHours(rows: readonly QueryChartRow[], dateField: string, valueField: string, operation: Aggregation) {
  const buckets = new Map<number, Sample>()
  for (const row of rows) {
    const raw = row[dateField], date = reportDate(raw), value = measure(row, valueField, operation)
    if (!date || value === undefined) continue
    const timestamp = raw instanceof Date ? raw.toISOString() : String(raw)
    const hour = /^(?:\d{4}-\d{2}-\d{2})[T ]([01]\d|2[0-3]):\d{2}/.exec(timestamp)
    if (!hour) continue
    const weekday = (new Date(date + 'T00:00:00Z').getUTCDay() + 6) % 7
    addSample(buckets, weekday * 24 + Number(hour[1]), row, value)
  }
  return [...buckets].map(([key, sample]) => ({ ...sample, hour: key % 24, weekday: Math.floor(key / 24) })).filter(item => Number.isFinite(item.value))
}

function label(value: unknown) {
  return dashboardValueMissing(value) || typeof value === 'object' ? '' : String(value)
}

function median(sorted: number[]) {
  const middle = Math.floor(sorted.length / 2)
  return sorted.length % 2 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2
}

export function dashboardEventIntervals(rows: readonly QueryChartRow[], timeField: string, entityField: string) {
  const groups = new Map<string, Map<number, QueryChartRow[]>>()
  for (const row of rows) {
    const time = dashboardTimestamp(row[timeField]), entity = entityField ? label(row[entityField]) : ''
    if (time === undefined || entityField && !entity) continue
    const instants = groups.get(entity) ?? new Map<number, QueryChartRow[]>()
    const samples = instants.get(time) ?? []
    samples.push(row)
    instants.set(time, samples)
    groups.set(entity, instants)
  }
  const results = []
  for (const [name, instants] of groups) {
    const times = [...instants.keys()].sort((a, b) => a - b)
    if (times.length < 2) continue
    const durations = times.slice(1).map((time, index) => time - times[index]).sort((a, b) => a - b)
    results.push({ name, count: durations.length, median: median(durations), max: durations.at(-1)!, rows: [...instants.values()].flat() })
  }
  return results.sort((a, b) => b.count - a.count || a.name.localeCompare(b.name))
}

export function dashboardTimeIntervals(rows: readonly QueryChartRow[], startField: string, endField: string, labelField: string, limit: number) {
  const intervals = rows.flatMap((row, index) => {
    const start = dashboardTimestamp(row[startField]), end = dashboardTimestamp(row[endField])
    if (start === undefined || end === undefined || end < start) return []
    return [{ row, index, start, end, name: labelField ? label(row[labelField]) : '' }]
  }).sort((a, b) => a.start - b.start || a.index - b.index)
  const items = intervals.slice(0, limit)
  return { total: intervals.length, items, first: items[0]?.start,
    last: items.length ? items.reduce((last, item) => Math.max(last, item.end), items[0].end) : undefined }
}
