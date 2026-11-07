import type { DashboardWidget } from './dashboard-layout'
import type { QueryChartRow } from './query-chart'

export type DashboardFilterValue =
  | { kind: 'selectFilter' | 'searchFilter'; value: string }
  | { kind: 'dateRangeFilter'; start: string; end: string }
  | { kind: 'multiValueFilter'; values: string[] }
  | { kind: 'numberRangeFilter'; min?: number; max?: number }
  | { kind: 'valueStateFilter'; presence: 'present' | 'missing' | '' }

export type DashboardFilter = DashboardFilterValue & { reportCode: string; field: string }

export function dashboardDateKey(value: unknown): string {
  const key = value instanceof Date && Number.isFinite(value.getTime())
    ? value.toISOString().slice(0, 10)
    : typeof value === 'string' ? /^(\d{4}-\d{2}-\d{2})(?:[T ]|$)/.exec(value)?.[1] : undefined
  if (!key) return ''
  const day = new Date(key + 'T00:00:00Z')
  return Number.isFinite(day.getTime()) && day.toISOString().slice(0, 10) === key ? key : ''
}

export function dashboardDateRangeValid(start: string, end: string) {
  return (!start || dashboardDateKey(start) === start) && (!end || dashboardDateKey(end) === end) && (!start || !end || start <= end)
}

function scalar(value: unknown) {
  return typeof value === 'string' || typeof value === 'number' && Number.isFinite(value) || typeof value === 'boolean' ? String(value) : ''
}

export function dashboardValueMissing(value: unknown) {
  return value == null || typeof value === 'string' && !value.trim()
}

function numericValue(value: unknown): number | undefined {
  if (typeof value !== 'number' && (typeof value !== 'string' || !/^[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?$/.test(value.trim()))) return
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

export function dashboardFilterChoices(rows: readonly QueryChartRow[], field: string, locale: string) {
  if (!field) return []
  return [...new Set(rows.map(row => scalar(row[field])).filter(value => value !== ''))]
    .sort((a, b) => a.localeCompare(b, locale, { numeric: true }))
    .map(value => ({ value, label: value }))
}

export function dashboardFilterHasValue(filter: DashboardFilterValue) {
  switch (filter.kind) {
    case 'dateRangeFilter': return !!(filter.start || filter.end) && dashboardDateRangeValid(filter.start, filter.end)
    case 'numberRangeFilter': {
      const { min, max } = filter
      return (min !== undefined || max !== undefined) && (min === undefined || Number.isFinite(min))
        && (max === undefined || Number.isFinite(max)) && (min === undefined || max === undefined || min <= max)
    }
    case 'multiValueFilter': return filter.values.some(value => value !== '')
    case 'valueStateFilter': return !!filter.presence
    case 'searchFilter': return !!filter.value.trim()
    case 'selectFilter': return filter.value !== ''
  }
}

export function dashboardFilterMatchesWidget(filter: DashboardFilter, widget: Pick<DashboardWidget, 'reportCode' | 'field' | 'kind'>) {
  return filter.reportCode === widget.reportCode && filter.field === widget.field && filter.kind === widget.kind
}

export function dashboardFilteredReports(filters: Iterable<DashboardFilter>, rowsFor: (code: string) => readonly QueryChartRow[]) {
  const byReport = new Map<string, ((row: QueryChartRow) => boolean)[]>()
  for (const filter of filters) {
    if (!filter.reportCode || !filter.field || !dashboardFilterHasValue(filter)) continue
    const { field } = filter
    let match: (row: QueryChartRow) => boolean
    if (filter.kind === 'dateRangeFilter') {
      const { start, end } = filter
      match = row => {
        const date = dashboardDateKey(row[field])
        return !!date && (!start || date >= start) && (!end || date <= end)
      }
    } else if (filter.kind === 'numberRangeFilter') {
      const { min, max } = filter
      match = row => {
        const value = numericValue(row[field])
        return value !== undefined && (min === undefined || value >= min) && (max === undefined || value <= max)
      }
    } else if (filter.kind === 'multiValueFilter') {
      const choices = new Set(filter.values.filter(value => value !== ''))
      match = row => choices.has(scalar(row[field]))
    } else if (filter.kind === 'valueStateFilter') {
      match = row => dashboardValueMissing(row[field]) === (filter.presence === 'missing')
    } else if (filter.kind === 'searchFilter') {
      const search = filter.value.trim().toLowerCase()
      match = row => scalar(row[field]).toLowerCase().includes(search)
    } else match = row => scalar(row[field]) === filter.value
    const report = byReport.get(filter.reportCode) ?? []
    report.push(match)
    byReport.set(filter.reportCode, report)
  }
  return new Map([...byReport].map(([code, clauses]) => [code, new Set(rowsFor(code).filter(row => clauses.every(match => match(row))))]))
}
