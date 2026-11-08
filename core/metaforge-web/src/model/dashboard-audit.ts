import { dashboardBooleanValue, dashboardDateKey, dashboardNumericValue, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { DashboardAuditOptions } from './dashboard-layout'
import type { QueryChartRow } from './query-chart'

interface AuditGroup {
  key: string
  values: (string | null)[]
  rows: QueryChartRow[]
}

function display(value: unknown): string | null {
  if (dashboardValueMissing(value)) return null
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

function identity(value: unknown, ignoreCase = false): string | null {
  if (dashboardValueMissing(value)) return null
  const normalized = ignoreCase && typeof value === 'string' ? value.toLowerCase() : value
  return JSON.stringify([typeof normalized, display(normalized)])
}

function grouped(rows: readonly QueryChartRow[], describe: (row: QueryChartRow) => Omit<AuditGroup, 'rows'> | undefined) {
  const groups = new Map<string, AuditGroup>()
  let evaluated = 0
  for (const row of rows) {
    const item = describe(row)
    if (!item) continue
    const group = groups.get(item.key) ?? { ...item, rows: [] }
    group.rows.push(row)
    groups.set(item.key, group)
    evaluated++
  }
  return { evaluated, groups: [...groups.values()].sort((a, b) => b.rows.length - a.rows.length) }
}

export function dashboardDuplicateKeys(rows: readonly QueryChartRow[], fields: readonly string[]) {
  const { groups, evaluated } = grouped(rows, row => {
    if (!fields.length || fields.some(field => dashboardValueMissing(row[field]))) return
    return { key: JSON.stringify(fields.map(field => identity(row[field]))), values: fields.map(field => display(row[field])) }
  })
  const duplicates = groups.filter(group => group.rows.length > 1)
  return { evaluated, groups: duplicates, affected: duplicates.reduce((count, group) => count + group.rows.length, 0) }
}

export function dashboardValueFrequency(rows: readonly QueryChartRow[], field: string) {
  const { groups } = grouped(rows, row => {
    const value = row[field]
    return { key: identity(value) ?? 'missing', values: [display(value)] }
  })
  return { groups, distinct: groups.filter(group => group.values[0] !== null).length }
}

export function dashboardMissingPatterns(rows: readonly QueryChartRow[], fields: readonly string[]) {
  const { groups, evaluated } = grouped(rows, row => {
    const values = fields.filter(field => dashboardValueMissing(row[field]))
    return values.length ? { key: JSON.stringify(values), values } : undefined
  })
  return { groups, affected: evaluated }
}

export function dashboardFieldComparison(rows: readonly QueryChartRow[], left: string, right: string, ignoreCase: boolean) {
  let compared = 0
  const { groups, evaluated } = grouped(rows, row => {
    const a = row[left], b = row[right]
    const first = identity(a, ignoreCase), second = identity(b, ignoreCase)
    if (first === null && second === null) return
    compared++
    if (first === second) return
    return { key: JSON.stringify([identity(a), identity(b)]), values: [display(a), display(b)] }
  })
  return { groups, compared, mismatched: evaluated }
}

export function dashboardInvalidValues(rows: readonly QueryChartRow[], field: string, rule: DashboardAuditOptions['rule']) {
  const validate = rule === 'number' ? dashboardNumericValue : rule === 'date' ? dashboardTimestamp : dashboardBooleanValue
  let checked = 0
  const { groups, evaluated } = grouped(rows, row => {
    const value = row[field], key = identity(value)
    if (key === null) return
    checked++
    if (validate(value) !== undefined) return
    return { key, values: [display(value)] }
  })
  return { groups, checked, invalid: evaluated }
}

const DAY = 86_400_000
const dateAt = (day: number) => new Date(day * DAY).toISOString().slice(0, 10)

export function dashboardDateCoverage(rows: readonly QueryChartRow[], field: string, minGapDays: number) {
  const observed = new Set<number>()
  for (const row of rows) {
    const value = row[field]
    if (dashboardTimestamp(value) === undefined) continue
    const day = dashboardDateKey(value)
    observed.add(Date.parse(day + 'T00:00:00Z') / DAY)
  }
  const days = [...observed].sort((a, b) => a - b)
  if (!days.length) return
  const gaps = days.slice(1).flatMap((day, index) => {
    const previous = days[index], length = day - previous - 1
    if (length < minGapDays) return []
    return [{ start: dateAt(previous + 1), end: dateAt(day - 1), days: length }]
  }).sort((a, b) => b.days - a.days || a.start.localeCompare(b.start))
  const first = days[0], last = days[days.length - 1]
  return { gaps, observed: days.length, expected: last - first + 1, first: dateAt(first), last: dateAt(last) }
}
