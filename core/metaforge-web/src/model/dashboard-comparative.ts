import { dashboardNumericValue, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

type Pair = { row: QueryChartRow; current: number; baseline: number }
type Sample = { row: QueryChartRow; value: number }

function pair(row: QueryChartRow, current: unknown, baseline: unknown): Pair | undefined {
  const left = dashboardNumericValue(current), right = dashboardNumericValue(baseline)
  if (left !== undefined && right !== undefined) return { row, current: left, baseline: right }
}

function pairedRows(rows: readonly QueryChartRow[], currentField: string, baselineField: string) {
  return rows.flatMap(row => {
    const item = pair(row, row[currentField], row[baselineField])
    return item ? [item] : []
  })
}

function category(value: unknown) {
  if (dashboardValueMissing(value)) return ''
  if (typeof value === 'string' || typeof value === 'boolean' || typeof value === 'number' && Number.isFinite(value)) return String(value)
  return ''
}

function pearson(pairs: readonly Pair[], minSamples: number) {
  if (pairs.length < minSamples) return null
  let scaleX = 0, scaleY = 0
  for (const { current, baseline } of pairs) {
    scaleX = Math.max(scaleX, Math.abs(current))
    scaleY = Math.max(scaleY, Math.abs(baseline))
  }
  if (!scaleX || !scaleY) return null

  let meanX = 0, meanY = 0, varianceX = 0, varianceY = 0, covariance = 0
  for (let index = 0; index < pairs.length; index++) {
    const x = pairs[index].current / scaleX, y = pairs[index].baseline / scaleY
    const dx = x - meanX, dy = y - meanY
    meanX += dx / (index + 1)
    meanY += dy / (index + 1)
    varianceX += dx * (x - meanX)
    varianceY += dy * (y - meanY)
    covariance += dx * (y - meanY)
  }
  if (varianceX <= 0 || varianceY <= 0) return null
  return Math.max(-1, Math.min(1, covariance / Math.sqrt(varianceX) / Math.sqrt(varianceY)))
}

export function dashboardCorrelation(rows: readonly QueryChartRow[], fields: readonly string[], minSamples: number) {
  const values = rows.map(row => fields.map(field => dashboardNumericValue(row[field])))
  const cells = fields.map(() => fields.map(() => ({ coefficient: null as number | null, rows: [] as QueryChartRow[] })))
  for (let i = 0; i < fields.length; i++) {
    for (let j = i; j < fields.length; j++) {
      const pairs = values.flatMap((row, index) => {
        const current = row[i], baseline = row[j]
        return current === undefined || baseline === undefined ? [] : [{ row: rows[index], current, baseline }]
      })
      const result = { coefficient: pearson(pairs, minSamples), rows: pairs.map(item => item.row) }
      cells[i][j] = result
      cells[j][i] = result
    }
  }
  return cells
}

function median(values: number[]) {
  values.sort((a, b) => a - b)
  const middle = Math.floor(values.length / 2)
  return values.length % 2 ? values[middle] : values[middle - 1] / 2 + values[middle] / 2
}

export function dashboardQuadrants(rows: readonly QueryChartRow[], xField: string, yField: string, xThreshold: number | null, yThreshold: number | null) {
  const pairs = pairedRows(rows, xField, yField)
  if (!pairs.length) return
  const x = xThreshold ?? median(pairs.map(item => item.current))
  const y = yThreshold ?? median(pairs.map(item => item.baseline))
  const quadrants: QueryChartRow[][] = [[], [], [], []]
  for (const item of pairs) {
    const column = item.current < x ? 0 : 1
    const row = item.baseline < y ? 1 : 0
    quadrants[row * 2 + column].push(item.row)
  }
  return { x, y, total: pairs.length, quadrants }
}

function pairedGroups(rows: readonly QueryChartRow[], groupField: string, currentField: string, baselineField: string, nonnegative = false) {
  const groups = new Map<string, { name: string; current: number; baseline: number; rows: QueryChartRow[] }>()
  for (const item of pairedRows(rows, currentField, baselineField)) {
    const name = category(item.row[groupField])
    if (!name || nonnegative && (item.current < 0 || item.baseline < 0)) continue
    const group = groups.get(name) ?? { name, current: 0, baseline: 0, rows: [] }
    group.current += item.current
    group.baseline += item.baseline
    group.rows.push(item.row)
    groups.set(name, group)
  }
  return [...groups.values()].filter(group => Number.isFinite(group.current) && Number.isFinite(group.baseline))
}

export function dashboardRankMovement(rows: readonly QueryChartRow[], groupField: string, currentField: string, baselineField: string) {
  const groups = pairedGroups(rows, groupField, currentField, baselineField)
  function ranks(field: 'current' | 'baseline') {
    const ranked = [...groups].sort((a, b) => b[field] - a[field] || a.name.localeCompare(b.name))
    let rank = 0
    return new Map(ranked.map((group, index) => {
      if (!index || group[field] !== ranked[index - 1][field]) rank = index + 1
      return [group.name, rank]
    }))
  }
  const current = ranks('current'), baseline = ranks('baseline')
  return groups.map(group => {
    const currentRank = current.get(group.name)!, baselineRank = baseline.get(group.name)!
    return { ...group, currentRank, baselineRank, movement: baselineRank - currentRank }
  }).sort((a, b) => a.currentRank - b.currentRank || a.name.localeCompare(b.name))
}

export function dashboardShareMovement(rows: readonly QueryChartRow[], groupField: string, currentField: string, baselineField: string) {
  const groups = pairedGroups(rows, groupField, currentField, baselineField, true)
  const currentTotal = groups.reduce((sum, group) => sum + group.current, 0)
  const baselineTotal = groups.reduce((sum, group) => sum + group.baseline, 0)
  return groups.map(group => {
    const currentShare = currentTotal > 0 && Number.isFinite(currentTotal) ? group.current / currentTotal : null
    const baselineShare = baselineTotal > 0 && Number.isFinite(baselineTotal) ? group.baseline / baselineTotal : null
    return { ...group, currentShare, baselineShare, movement: currentShare === null || baselineShare === null ? null : currentShare - baselineShare }
  }).sort((a, b) => Math.abs(b.movement ?? 0) - Math.abs(a.movement ?? 0) || a.name.localeCompare(b.name))
}

function groupedBands(samples: readonly Sample[], boundaries: readonly number[]) {
  const bands = Array.from({ length: boundaries.length + 1 }, (_, index) => ({
    lower: boundaries[index - 1],
    upper: boundaries[index],
    rows: [] as QueryChartRow[],
  }))
  for (const { value, row } of samples) {
    const index = boundaries.findIndex(upper => value < upper)
    bands[index < 0 ? boundaries.length : index].rows.push(row)
  }
  return bands
}

export function dashboardDifferenceBands(rows: readonly QueryChartRow[], currentField: string, baselineField: string, boundaries: readonly number[]) {
  const samples = pairedRows(rows, currentField, baselineField).flatMap(item => {
    const value = item.current - item.baseline
    return Number.isFinite(value) ? [{ value, row: item.row }] : []
  })
  return { total: samples.length, bands: groupedBands(samples, boundaries) }
}

export function dashboardGroupComparison(rows: readonly QueryChartRow[], groupField: string, metricField: string, leftGroup: string, rightGroup: string, boundaries: readonly number[]) {
  const first: Sample[] = [], second: Sample[] = []
  for (const row of rows) {
    const group = category(row[groupField]), value = dashboardNumericValue(row[metricField])
    if (!group || value === undefined) continue
    if (group === leftGroup) first.push({ value, row })
    else if (group === rightGroup) second.push({ value, row })
  }
  const other = groupedBands(second, boundaries)
  return {
    leftTotal: first.length,
    rightTotal: second.length,
    bands: groupedBands(first, boundaries).map((band, index) => ({ ...band, otherRows: other[index].rows })),
  }
}
