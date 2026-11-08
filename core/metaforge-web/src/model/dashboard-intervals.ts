import { dashboardBandBoundaries } from './dashboard-business'
import { dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

export interface DashboardIntervalRange {
  start: number
  end: number
}

export interface DashboardInterval extends DashboardIntervalRange {
  group: string
  row: QueryChartRow
}

export const dashboardIntervalUnitMs = { minutes: 60_000, hours: 3_600_000, days: 86_400_000 } as const

function groupLabel(value: unknown) {
  if (dashboardValueMissing(value)) return ''
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' ? String(value) : ''
}

export function dashboardReadIntervals(rows: readonly QueryChartRow[], startField: string, endField: string, groupField = '') {
  const intervals: DashboardInterval[] = []
  let invalid = 0
  for (const row of rows) {
    const start = dashboardTimestamp(row[startField]), end = dashboardTimestamp(row[endField])
    if (start === undefined || end === undefined || end <= start) invalid++
    else intervals.push({ start, end, row, group: groupField ? groupLabel(row[groupField]) : '' })
  }
  return { intervals, invalid }
}

export function dashboardIntervalSweep(intervals: readonly DashboardInterval[]) {
  const deltas = new Map<number, number>()
  for (const { start, end } of intervals) {
    deltas.set(start, (deltas.get(start) ?? 0) + 1)
    deltas.set(end, (deltas.get(end) ?? 0) - 1)
  }
  let active = 0
  return [...deltas].sort(([left], [right]) => left - right).map(([time, delta]) => {
    active += delta
    return { time, active }
  })
}

export function dashboardIntervalCoverage(intervals: readonly DashboardInterval[]) {
  const ordered = [...intervals].sort((a, b) => a.start - b.start || a.end - b.end)
  if (!ordered.length) return
  const first = ordered[0].start
  let last = ordered[0].end, covered = last - first
  const gaps: DashboardIntervalRange[] = []
  for (const item of ordered.slice(1)) {
    if (item.start > last) gaps.push({ start: last, end: item.start })
    if (item.end > last) {
      covered += item.end - Math.max(last, item.start)
      last = item.end
    }
  }
  return { first, last, covered, span: last - first, gaps }
}

function intervalGroups(intervals: readonly DashboardInterval[]) {
  const groups = new Map<string, DashboardInterval[]>()
  for (const interval of intervals) {
    const group = groups.get(interval.group) ?? []
    group.push(interval)
    groups.set(interval.group, group)
  }
  return groups
}

export function dashboardIntervalOverlaps(intervals: readonly DashboardInterval[], minimum: number) {
  const periods: (DashboardIntervalRange & { group: string; peak: number })[] = []
  for (const [group, entries] of intervalGroups(intervals)) {
    const sweep = dashboardIntervalSweep(entries)
    let period: typeof periods[number] | undefined
    for (let index = 0; index < sweep.length - 1; index++) {
      const point = sweep[index], end = sweep[index + 1].time
      if (point.active < minimum) {
        period = undefined
        continue
      }
      if (period) {
        period.end = end
        period.peak = Math.max(period.peak, point.active)
      } else {
        period = { group, start: point.time, end, peak: point.active }
        periods.push(period)
      }
    }
  }
  return periods.sort((a, b) => b.peak - a.peak || (b.end - b.start) - (a.end - a.start) || a.start - b.start)
}

export function dashboardIntervalGroupCoverage(intervals: readonly DashboardInterval[], observedSpan: number) {
  return [...intervalGroups(intervals)].map(([group, entries]) => {
    const covered = dashboardIntervalCoverage(entries)!.covered
    return { group, covered, share: covered / observedSpan, rows: entries.map(item => item.row) }
  }).sort((a, b) => b.covered - a.covered || a.group.localeCompare(b.group))
}

export function dashboardIntervalBandBoundaries(input: string, unitMs: number) {
  const values = dashboardBandBoundaries(input)
  return values?.every(value => value > 0 && Number.isFinite(value * unitMs)) ? values : undefined
}

export function dashboardIntervalDurationBands(intervals: readonly DashboardInterval[], boundaries: readonly number[], unitMs: number) {
  const bands = Array.from({ length: boundaries.length + 1 }, (_, index) => ({
    lower: boundaries[index - 1],
    upper: boundaries[index],
    rows: [] as QueryChartRow[],
  }))
  for (const item of intervals) {
    const duration = item.end - item.start
    const index = boundaries.findIndex(upper => duration < upper * unitMs)
    bands[index < 0 ? boundaries.length : index].rows.push(item.row)
  }
  return bands
}
