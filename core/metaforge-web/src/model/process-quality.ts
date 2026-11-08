import type { QueryChartRow } from './query-chart'
import { reportNumber, reportTimestamp } from './report-values'

export const processQualityViews = ['qualityRun', 'qualityIndividuals', 'qualityMovingRange', 'qualitySignals', 'qualitySpecification', 'qualityGroupYield'] as const
export type ProcessQualityView = typeof processQualityViews[number]
export type QualityOrder = { field: string; type: 'number' | 'date' }
export interface ProcessQualityOptions {
  measure: string
  order?: QualityOrder
  group: string
  lower?: number
  upper?: number
}

export function isProcessQualityView(kind: unknown): kind is ProcessQualityView {
  return processQualityViews.some(view => view === kind)
}

export function readProcessQualityOptions(value: unknown): ProcessQualityOptions {
  const options = (value && typeof value === 'object' ? value : {}) as Partial<ProcessQualityOptions>
  const field = (name: unknown) => typeof name === 'string' ? name.slice(0, 500) : ''
  const order = options.order
  return {
    measure: field(options.measure),
    order: order && field(order.field) && (order.type === 'date' || order.type === 'number') ? { field: field(order.field), type: order.type } : undefined,
    group: field(options.group),
    lower: reportNumber(options.lower),
    upper: reportNumber(options.upper),
  }
}

type Limits = { center: number; lower?: number; upper?: number }
type QualityPoint = { value: number; rows: QueryChartRow[] }
type QualitySeries = { points: (QualityPoint | undefined)[]; limits?: Limits }
const mean = (values: readonly number[]) => values.reduce((sum, value) => sum + value / values.length, 0)
function median(values: readonly number[]) {
  const sorted = [...values].sort((left, right) => left - right)
  const mid = Math.floor(sorted.length / 2)
  return sorted.length % 2 ? sorted[mid] : sorted[mid - 1] / 2 + sorted[mid] / 2
}

export function qualityControlSeries(rows: readonly QueryChartRow[], measure: string, order?: QualityOrder) {
  const sequence = rows.flatMap((row, index) => {
    const position = order ? (order.type === 'date' ? reportTimestamp(row[order.field]) : reportNumber(row[order.field])) : index
    return position === undefined ? [] : [{ row, position }]
  })
  if (order) sequence.sort((left, right) => left.position - right.position)

  const points = sequence.map(({ row }) => {
    const value = reportNumber(row[measure])
    return value === undefined ? undefined : { value, rows: [row] }
  })
  const ranges = points.map((point, index) => {
    const previous = points[index - 1]
    if (!point || !previous) return
    const value = Math.abs(point.value - previous.value)
    if (Number.isFinite(value)) return { value, rows: [previous.rows[0], point.rows[0]] }
  })
  const values = points.flatMap(point => point ? [point.value] : [])
  const movingRanges = ranges.flatMap(point => point ? [point.value] : [])
  const run: QualitySeries = { points, limits: values.length ? { center: median(values) } : undefined }
  const individuals: QualitySeries = { points }
  const moving: QualitySeries = { points: ranges }
  if (movingRanges.length) {
    // Two successive observations: d2 = 1.128, D3 = 0, D4 = 3.267.
    const range = mean(movingRanges)
    const center = mean(values)
    const distance = 3 * (range / 1.128)
    individuals.limits = { center, lower: center - distance, upper: center + distance }
    moving.limits = { center: range, lower: 0, upper: range * 3.267 }
  }
  return { run, individuals, moving, valid: values.length, invalid: rows.length - values.length }
}

export function qualitySignal(value: number, limits?: Pick<Limits, 'lower' | 'upper'>) {
  if (limits?.lower !== undefined && value < limits.lower) return 'below'
  if (limits?.upper !== undefined && value > limits.upper) return 'above'
}

export const specificationStates = ['pass', 'below', 'above', 'invalid'] as const
type SpecificationState = typeof specificationStates[number]
export function qualitySpecificationsValid(lower?: number, upper?: number) {
  return (lower !== undefined || upper !== undefined) && (lower === undefined || upper === undefined || lower <= upper)
}

export function qualitySpecificationGroups(rows: readonly QueryChartRow[], options: ProcessQualityOptions, grouped: boolean) {
  const groups = new Map<string, Record<SpecificationState, QueryChartRow[]>>()
  for (const row of rows) {
    const group = grouped ? row[options.group] : undefined
    const name = typeof group === 'string' ? group.trim()
      : typeof group === 'number' || typeof group === 'boolean' ? String(group) : ''
    const counts = groups.get(name) ?? { pass: [], below: [], above: [], invalid: [] }
    const value = reportNumber(row[options.measure])
    const state = value === undefined ? 'invalid' : qualitySignal(value, options) ?? 'pass'
    counts[state].push(row)
    groups.set(name, counts)
  }
  return groups
}
