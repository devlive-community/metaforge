import { dashboardDateKey, dashboardNumericValue, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

export type DashboardBusinessAggregation = 'count' | 'sum' | 'avg'

const DAY = 86_400_000
const dateAt = (day: number) => new Date(day * DAY).toISOString().slice(0, 10)
function dayIndex(value: unknown) {
  const key = dashboardDateKey(value)
  return key ? Date.parse(key + 'T00:00:00Z') / DAY : undefined
}

function label(value: unknown) {
  return !dashboardValueMissing(value) && (typeof value === 'string' || typeof value === 'boolean' || typeof value === 'number' && Number.isFinite(value)) ? String(value) : ''
}

function groups(rows: readonly QueryChartRow[], field: string) {
  const result = new Map<string, QueryChartRow[]>()
  for (const row of rows) {
    const key = label(row[field])
    const bucket = result.get(key)
    if (bucket) bucket.push(row)
    else result.set(key, [row])
  }
  return result
}

function numbers(rows: readonly QueryChartRow[], field: string) {
  return rows.flatMap(row => {
    const value = dashboardNumericValue(row[field])
    return value === undefined ? [] : [value]
  })
}

function finite(value: number) { return Number.isFinite(value) ? value : null }
function sum(values: number[]) { return finite(values.reduce((total, value) => total + value, 0)) }
function ratio(numerator: number | null, denominator: number | null) {
  return numerator === null || denominator === null || denominator <= 0 ? null : finite(numerator / denominator * 100)
}

function aggregate(rows: readonly QueryChartRow[], field: string, operation: DashboardBusinessAggregation) {
  if (operation === 'count') return rows.length
  const values = numbers(rows, field)
  if (!values.length) return null
  return operation === 'avg' ? finite(values.reduce((value, item) => value + item / values.length, 0)) : sum(values)
}

export function dashboardStageConversion(rows: readonly QueryChartRow[], stageField: string, order: readonly string[], metricField = '', entityField = '') {
  const stages = groups(rows, stageField)
  let cohort: Set<string> | undefined, previous: number | null = null, first: number | null = null
  return order.map((name, index) => {
    const group = stages.get(name) ?? []
    let value: number | null, observed: boolean
    if (entityField) {
      const entities = new Set(group.map(row => label(row[entityField])).filter(Boolean))
      cohort = cohort ? new Set([...cohort].filter(entity => entities.has(entity))) : entities
      value = cohort.size
      observed = entities.size > 0
    } else {
      const values = metricField ? numbers(group, metricField).filter(value => value >= 0) : []
      value = metricField ? group.length && !values.length ? null : sum(values) : group.length
      observed = metricField ? values.length > 0 : group.length > 0
    }
    if (!index) first = value
    const result = { name, value, observed, stepRate: index ? ratio(value, previous) : null, overallRate: ratio(value, first) }
    previous = value
    return result
  })
}

export function dashboardTargetGap(rows: readonly QueryChartRow[], groupField: string, actualField: string, goalField: string, targetAggregation: 'sum' | 'max') {
  return [...groups(rows, groupField)].map(([name, group]) => {
    const goals = numbers(group, goalField)
    const actual = aggregate(group, actualField, 'sum')
    const target = !goals.length ? null : targetAggregation === 'max' ? goals.reduce((maximum, goal) => Math.max(maximum, goal)) : sum(goals)
    return { name, actual, target, gap: actual === null || target === null ? null : finite(actual - target), attainment: ratio(actual, target) }
  }).filter(item => item.actual !== null || item.target !== null).sort((a, b) => (b.actual ?? -Infinity) - (a.actual ?? -Infinity))
}

function sortedGroups(rows: readonly QueryChartRow[], field: string) {
  return [...groups(rows, field)].sort((a, b) => b[1].length - a[1].length)
}

export function dashboardSegmentSummary(rows: readonly QueryChartRow[], groupField: string, subgroupField: string, valueField: string, operation: DashboardBusinessAggregation, limit: number) {
  const items = sortedGroups(rows, groupField).slice(0, limit).flatMap(([name, group], index) => [
    { key: 'group-' + index, type: 'group', name, count: group.length, value: aggregate(group, valueField, operation) },
    ...(subgroupField ? sortedGroups(group, subgroupField).slice(0, limit).map(([child, bucket], childIndex) =>
      ({ key: 'child-' + index + '-' + childIndex, type: 'child', name: child, count: bucket.length, value: aggregate(bucket, valueField, operation) })) : []),
  ])
  return { items, total: { key: 'total', type: 'total', name: '', count: rows.length, value: aggregate(rows, valueField, operation) } }
}

export function dashboardPareto(rows: readonly QueryChartRow[], groupField: string, metricField: string, cutoff: number) {
  let excluded = 0
  const ranked = [...groups(rows, groupField)].map(([name, group]) => {
    if (!metricField) return { name, value: group.length }
    const values = numbers(group, metricField)
    const positive = values.filter(value => value >= 0)
    excluded += values.length - positive.length
    return { name, value: positive.length ? sum(positive) : null }
  }).filter(item => item.value !== null).sort((a, b) => b.value! - a.value!)
  const total = sum(ranked.map(item => item.value!))
  let running = 0, crossed = false
  return { total, excluded, items: ranked.map(item => {
    running += item.value!
    const cumulative = ratio(running, total)
    const reachesCutoff = !crossed && cumulative !== null && cumulative + 1e-10 >= cutoff
    crossed ||= reachesCutoff
    return { ...item, share: ratio(item.value, total), cumulative, reachesCutoff }
  }) }
}

export function dashboardVarianceDrivers(rows: readonly QueryChartRow[], groupField: string, actualField: string, baselineField: string) {
  let samples = 0, actualTotal = 0, baselineTotal = 0
  const items = [...groups(rows, groupField)].flatMap(([name, group]) => {
    let actual = 0, baseline = 0, count = 0
    for (const row of group) {
      const a = dashboardNumericValue(row[actualField]), b = dashboardNumericValue(row[baselineField])
      if (a === undefined || b === undefined) continue
      actual += a; baseline += b; count++
    }
    if (!count) return []
    samples += count; actualTotal += actual; baselineTotal += baseline
    return [{ name, delta: finite(actual - baseline) }]
  }).sort((a, b) => Math.abs(b.delta ?? 0) - Math.abs(a.delta ?? 0))
  return { samples, actual: finite(actualTotal), baseline: finite(baselineTotal), net: finite(actualTotal - baselineTotal), items }
}

function entityDays(rows: readonly QueryChartRow[], entityField: string, dateField: string) {
  let latest = -Infinity
  const entities = new Map<string, Set<number>>()
  for (const row of rows) {
    const entity = label(row[entityField]), day = dayIndex(row[dateField])
    if (!entity || day === undefined) continue
    latest = Math.max(latest, day)
    const visits = entities.get(entity) ?? new Set<number>()
    visits.add(day)
    entities.set(entity, visits)
  }
  return { entities, latest }
}

export function dashboardCohortRetention(rows: readonly QueryChartRow[], entityField: string, dateField: string, period: 'day' | 'week', periods: number, limit: number) {
  const { entities, latest } = entityDays(rows, entityField, dateField)
  if (!entities.size) return []
  const width = period === 'day' ? 1 : 7
  const bucket = (day: number) => Math.floor((day + (width === 7 ? 3 : 0)) / width)
  const cohorts = new Map<number, { size: number; counts: number[] }>()
  for (const visits of entities.values()) {
    const buckets = new Set([...visits].map(bucket)), first = Math.min(...buckets)
    const cohort = cohorts.get(first) ?? { size: 0, counts: Array<number>(periods).fill(0) }
    cohort.size++
    for (const visit of buckets) {
      const offset = visit - first
      if (offset < periods) cohort.counts[offset]++
    }
    cohorts.set(first, cohort)
  }
  return [...cohorts].sort(([a], [b]) => b - a).slice(0, limit).map(([start, cohort]) => ({
    name: dateAt(start * width - (width === 7 ? 3 : 0)), size: cohort.size,
    periods: cohort.counts.map((count, index) => start + index > bucket(latest) ? null : { count, percent: count / cohort.size * 100 }),
  }))
}

export function dashboardRecencyDistribution(rows: readonly QueryChartRow[], entityField: string, dateField: string) {
  const { entities, latest } = entityDays(rows, entityField, dateField)
  const counts = [0, 0, 0, 0]
  for (const visits of entities.values()) {
    const days = latest - Math.max(...visits)
    counts[days <= 7 ? 0 : days <= 30 ? 1 : days <= 90 ? 2 : 3]++
  }
  return { latest: entities.size ? dateAt(latest) : '', total: entities.size,
    buckets: counts.map((count, index) => ({ index, count, percent: entities.size ? count / entities.size * 100 : 0 })) }
}

export function dashboardBandBoundaries(input: string): number[] | undefined {
  const parsed = input.split(/[,，;；\n]+/).map(dashboardNumericValue)
  if (!parsed.length || parsed.length > 10 || parsed.some(value => value === undefined)) return
  const values = parsed as number[]
  return values.every((value, index) => index === 0 || value > values[index - 1]) ? values : undefined
}

export function dashboardValueBands(rows: readonly QueryChartRow[], field: string, boundaries: readonly number[]) {
  const values = numbers(rows, field)
  const counts = Array<number>(boundaries.length + 1).fill(0)
  for (const value of values) {
    const index = boundaries.findIndex(upper => value < upper)
    counts[index < 0 ? boundaries.length : index]++
  }
  return { total: values.length, items: counts.map((count, index) => ({
    index, lower: boundaries[index - 1], upper: boundaries[index], count, percent: values.length ? count / values.length * 100 : 0,
  })) }
}
