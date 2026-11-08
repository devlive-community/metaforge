import { dashboardNumericValue, dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

type Aggregation = 'count' | 'sum' | 'avg'

interface Observation {
  row: string
  column: string
  value: number
  sources: QueryChartRow[]
}

function category(value: unknown) {
  if (dashboardValueMissing(value)) return ''
  if (typeof value === 'string' || typeof value === 'boolean' || typeof value === 'number' && Number.isFinite(value)) return String(value)
  return ''
}

function mostFrequent(counts: Map<string, number>, limit: number) {
  return [...counts.keys()].sort((a, b) => counts.get(b)! - counts.get(a)! || a.localeCompare(b)).slice(0, limit)
}

function matrix(observations: Observation[], aggregation: Aggregation, limit: number) {
  const rowCounts = new Map<string, number>()
  for (const { row } of observations) rowCounts.set(row, (rowCounts.get(row) ?? 0) + 1)
  const rows = mostFrequent(rowCounts, limit)
  const visibleRows = new Set(rows)
  const columnCounts = new Map<string, number>()
  for (const { row, column } of observations) {
    if (visibleRows.has(row)) columnCounts.set(column, (columnCounts.get(column) ?? 0) + 1)
  }
  const columns = mostFrequent(columnCounts, limit)
  const visibleColumns = new Set(columns)
  const buckets = new Map(rows.map(row => [row, new Map<string, { count: number; total: number; sources: Set<QueryChartRow> }>()]))
  let visible = 0
  for (const point of observations) {
    const row = buckets.get(point.row)
    if (!row || !visibleColumns.has(point.column)) continue
    const cell = row.get(point.column) ?? { count: 0, total: 0, sources: new Set<QueryChartRow>() }
    cell.count++
    cell.total += point.value
    for (const source of point.sources) cell.sources.add(source)
    row.set(point.column, cell)
    visible++
  }
  return {
    columns,
    total: observations.length,
    visible,
    rows: rows.map(label => ({
      label,
      cells: columns.map(column => {
        const cell = buckets.get(label)?.get(column)
        if (!cell) return null
        const value = aggregation === 'count' ? cell.count : aggregation === 'avg' ? cell.total / cell.count : cell.total
        return { value: Number.isFinite(value) ? value : null, count: cell.count, sources: [...cell.sources] }
      }),
    })),
  }
}

export function dashboardCrossTab(rows: readonly QueryChartRow[], rowField: string, columnField: string, valueField: string, aggregation: Aggregation, limit: number) {
  const points = rows.flatMap(row => {
    const vertical = category(row[rowField]), horizontal = category(row[columnField])
    const value = aggregation === 'count' ? 1 : dashboardNumericValue(row[valueField])
    if (!vertical || !horizontal || value === undefined) return []
    return [{ row: vertical, column: horizontal, value, sources: [row] }]
  })
  return matrix(points, aggregation, limit)
}

interface StateAtTime {
  state: string
  sources: QueryChartRow[]
}

export function dashboardTransitions(rows: readonly QueryChartRow[], entityField: string, stateField: string, dateField: string, limit: number) {
  const entities = new Map<string, Map<number, Map<string, QueryChartRow[]>>>()
  for (const row of rows) {
    const entity = category(row[entityField]), state = category(row[stateField]), time = dashboardTimestamp(row[dateField])
    if (!entity || !state || time === undefined) continue
    const dates = entities.get(entity) ?? new Map<number, Map<string, QueryChartRow[]>>()
    const states = dates.get(time) ?? new Map<string, QueryChartRow[]>()
    const sources = states.get(state) ?? []
    sources.push(row)
    states.set(state, sources)
    dates.set(time, states)
    entities.set(entity, dates)
  }
  const points: Observation[] = []
  for (const dates of entities.values()) {
    let previous: StateAtTime | undefined
    for (const [, states] of [...dates].sort(([a], [b]) => a - b)) {
      if (states.size !== 1) {
        previous = undefined
        continue
      }
      const [state, sources] = states.entries().next().value!
      if (previous && previous.state !== state) {
        points.push({ row: previous.state, column: state, value: 1, sources: [...previous.sources, ...sources] })
      }
      previous = { state, sources }
    }
  }
  return matrix(points, 'count', limit)
}

export function dashboardCooccurrences(rows: readonly QueryChartRow[], entityField: string, itemField: string, dimensionLimit: number, limit: number) {
  const entities = new Map<string, Map<string, QueryChartRow[]>>()
  for (const row of rows) {
    const entity = category(row[entityField]), item = category(row[itemField])
    if (!entity || !item) continue
    const items = entities.get(entity) ?? new Map<string, QueryChartRow[]>()
    const sources = items.get(item) ?? []
    sources.push(row)
    items.set(item, sources)
    entities.set(entity, items)
  }
  const frequencies = new Map<string, number>()
  for (const items of entities.values()) for (const item of items.keys()) frequencies.set(item, (frequencies.get(item) ?? 0) + 1)
  const candidates = mostFrequent(frequencies, dimensionLimit)
  const combinations = new Map<string, { key: string; left: string; right: string; count: number; sources: QueryChartRow[] }>()
  for (const items of entities.values()) {
    const present = candidates.filter(item => items.has(item))
    for (let first = 0; first < present.length; first++) {
      for (let second = first + 1; second < present.length; second++) {
        const left = present[first], right = present[second]
        const key = JSON.stringify([left, right])
        const pair = combinations.get(key) ?? { key, left, right, count: 0, sources: [] }
        pair.count++
        pair.sources.push(...items.get(left)!, ...items.get(right)!)
        combinations.set(key, pair)
      }
    }
  }
  return {
    entities: entities.size,
    limited: candidates.length < frequencies.size,
    items: [...combinations.values()].sort((a, b) => b.count - a.count || a.left.localeCompare(b.left) || a.right.localeCompare(b.right))
      .slice(0, limit).map(item => ({ ...item, share: item.count / entities.size * 100 })),
  }
}

export function dashboardGroupSpread(rows: readonly QueryChartRow[], groupField: string, valueField: string, limit: number) {
  const groups = new Map<string, { value: number; source: QueryChartRow }[]>()
  for (const row of rows) {
    const name = category(row[groupField]), value = dashboardNumericValue(row[valueField])
    if (!name || value === undefined) continue
    const group = groups.get(name) ?? []
    group.push({ value, source: row })
    groups.set(name, group)
  }
  return [...groups].sort((a, b) => b[1].length - a[1].length || a[0].localeCompare(b[0])).slice(0, limit).map(([name, group]) => {
    const values = group.map(item => item.value).sort((a, b) => a - b)
    function quantile(ratio: number) {
      const position = (values.length - 1) * ratio
      const weight = position % 1
      return values[Math.floor(position)] * (1 - weight) + values[Math.ceil(position)] * weight
    }
    return { name, count: values.length, min: values[0], q1: quantile(.25), median: quantile(.5), q3: quantile(.75), max: values.at(-1)!,
      sources: group.map(item => item.source) }
  })
}
