import { dashboardNumericValue, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { DashboardOperationsOptions } from './dashboard-layout'
import type { QueryChartRow } from './query-chart'

type Comparator = DashboardOperationsOptions['comparator']
export type DashboardRunState = 'healthy' | 'failed' | 'unknown'

export function dashboardCellText(value: unknown): string {
  if (dashboardValueMissing(value)) return ''
  if (typeof value === 'object') return (JSON.stringify(value) ?? '').slice(0, 2000)
  return String(value)
}

export function dashboardThresholdMatches(value: number, threshold: number, comparator: Comparator) {
  if (comparator === 'above') return value > threshold
  if (comparator === 'atLeast') return value >= threshold
  if (comparator === 'below') return value < threshold
  return value <= threshold
}

function numericRows(rows: readonly QueryChartRow[], field: string) {
  return rows.flatMap((row, index) => {
    const value = dashboardNumericValue(row[field])
    return value === undefined ? [] : [{ row, index, value }]
  })
}

export function dashboardOutliers(rows: readonly QueryChartRow[], field: string, threshold: number, comparator: Comparator, limit: number) {
  const samples = numericRows(rows, field)
  const matched = samples.filter(item => dashboardThresholdMatches(item.value, threshold, comparator))
    .map(item => ({ ...item, distance: Math.abs(item.value - threshold) }))
    .sort((a, b) => b.distance - a.distance || a.index - b.index)
  return { samples: samples.length, total: matched.length, items: matched.slice(0, limit) }
}

export function dashboardSla(rows: readonly QueryChartRow[], field: string, threshold: number, comparator: Comparator) {
  let passed = 0, total = 0
  for (const row of rows) {
    const value = dashboardNumericValue(row[field])
    if (value === undefined) continue
    total++
    if (dashboardThresholdMatches(value, threshold, comparator)) passed++
  }
  if (!total) return
  return { total, passed, breached: total - passed, percent: passed / total * 100 }
}

export function dashboardPercentiles(rows: readonly QueryChartRow[], field: string) {
  const values = numericRows(rows, field).map(item => item.value).sort((a, b) => a - b)
  if (!values.length) return
  const quantiles = [50, 90, 95, 99].map(percentile => {
    const index = (values.length - 1) * percentile / 100
    const offset = index % 1
    const low = values[Math.floor(index)], high = values[Math.ceil(index)]
    return { percentile, value: low * (1 - offset) + high * offset }
  })
  return { count: values.length, quantiles }
}

function normalize(value: string) { return value.trim().toLowerCase() }
function statusSet(text: string) { return new Set(text.split(/[,，\n]+/).map(normalize).filter(Boolean)) }

export function dashboardEvents(rows: readonly QueryChartRow[], field: string, settings: DashboardOperationsOptions, limit: number) {
  const successes = statusSet(settings.healthyValues), failures = statusSet(settings.failureValues)
  return rows.flatMap((row, index) => {
    const time = dashboardTimestamp(row[field])
    if (time === undefined) return []
    const label = dashboardCellText(row[settings.labelField]), status = dashboardCellText(row[settings.statusField])
    const key = normalize(status)
    const state: DashboardRunState = failures.has(key) ? 'failed' : successes.has(key) ? 'healthy' : 'unknown'
    return [{ index, time, label, status, state, details: dashboardCellText(row[settings.detailsField]) }]
  }).sort((a, b) => b.time - a.time || a.index - b.index).slice(0, limit)
}

export function dashboardRunSummary(events: readonly { state: DashboardRunState }[]) {
  let streak = 0, healthy = 0, failed = 0
  for (const [index, event] of events.entries()) {
    if (event.state === 'healthy') {
      healthy++
      if (streak === index) streak++
    }
    if (event.state === 'failed') failed++
  }
  return { healthy, failed, unknown: events.length - healthy - failed, streak }
}

export function dashboardMaintenance(rows: readonly QueryChartRow[], startField: string, settings: DashboardOperationsOptions, now: number, limit: number) {
  return rows.flatMap((row, index) => {
    const start = dashboardTimestamp(row[startField]), end = dashboardTimestamp(row[settings.endField])
    if (start === undefined || end === undefined || end <= start || end <= now) return []
    return [{ index, start, end, active: start <= now, label: dashboardCellText(row[settings.labelField]), details: dashboardCellText(row[settings.detailsField]) }]
  }).sort((a, b) => Number(b.active) - Number(a.active) || a.start - b.start || a.index - b.index).slice(0, limit)
}

export function dashboardRecordChoices(rows: readonly QueryChartRow[], labelField: string, fields: readonly string[], search: string, limit: number) {
  const query = normalize(search)
  const results = []
  for (const [index, row] of rows.entries()) {
    const label = dashboardCellText(row[labelField])
    if (!label) continue
    if (query && !normalize(label).includes(query) && !fields.some(field => normalize(dashboardCellText(row[field])).includes(query))) continue
    results.push({ index, label, row })
    if (results.length === limit) break
  }
  return results
}
