import { chartDataSpec, queryChartGeometry } from './query-chart-data'
import type { QueryChartOptions, QueryChartType } from './query-chart'

const booleanFields = ['smooth', 'dashed', 'points', 'fill', 'transparent', 'legend', 'legendInteractive', 'labels',
  'labelOverlap', 'xVisible', 'xTitleVisible', 'yVisible', 'grid', 'hideOverlap', 'donutTotal'] as const
const textFields = ['colors', 'xTitle', 'yTitle', 'seriesName', 'unit'] as const
const choices = {
  palette: ['green', 'blue', 'purple'],
  legendPosition: ['top', 'bottom', 'right'],
  labelPosition: ['auto', 'top', 'inside'],
  numberFormat: ['integer', 'decimal', 'percent', 'compact'],
  dateFormat: ['short', 'full', 'time'],
} as const
const numericFields = {
  lineWidth: [1, 6], pointSize: [1, 18], opacity: [0, 100], decimals: [0, 6],
  barMaxWidth: [1, 160], barCornerRadius: [0, 24], pieRadius: [30, 90], donutHole: [15, 75],
} as const
export type QueryChartStyleKey = typeof booleanFields[number] | typeof textFields[number] | keyof typeof choices | keyof typeof numericFields | 'min' | 'max'
export type QueryChartStyle = Partial<Pick<QueryChartOptions, QueryChartStyleKey>>

export function readQueryChartStyle(value: unknown): QueryChartStyle | undefined {
  if (!value || typeof value !== 'object') return
  const raw = value as Record<string, unknown>
  const style: Record<string, unknown> = {}
  for (const field of booleanFields) if (typeof raw[field] === 'boolean') style[field] = raw[field]
  for (const field of textFields) {
    const value = raw[field]
    if (typeof value === 'string') style[field] = value.slice(0, field === 'colors' ? 400 : 120)
  }
  for (const [field, values] of Object.entries(choices)) {
    if (values.some(value => value === raw[field])) style[field] = raw[field]
  }
  for (const [field, [min, max]] of Object.entries(numericFields)) {
    const value = raw[field]
    if (typeof value === 'number' && Number.isFinite(value)) style[field] = Math.max(min, Math.min(max, Math.round(value)))
  }
  for (const field of ['min', 'max'] as const) {
    const value = raw[field]
    if (typeof value === 'number' && Number.isFinite(value)) style[field] = value
  }
  return Object.keys(style).length ? style as QueryChartStyle : undefined
}

/** Options exposed in the screen inspector must be consumed by this chart's renderer. */
export function queryChartStyleFields(type: QueryChartType): QueryChartStyleKey[] {
  if (type === 'table') return []
  const fields: QueryChartStyleKey[] = ['palette', 'colors', 'transparent']
  const geometry = queryChartGeometry(type)
  const direct = !chartDataSpec(type)
  const circular = ['pie', 'donut', 'rose', 'funnel'].includes(type)
  const seriesLegend = ['dualAxis', 'polarBar', 'polarStackedBar', 'polarLine', 'pareto', 'cumulativeLine', 'rankTrend', 'mirrorBar', 'targetComparison'].includes(type)
  if (direct) {
    fields.push('legend', 'legendPosition', 'legendInteractive')
    if (!circular) fields.push('seriesName')
    if (type !== 'radar') fields.push('labels', 'labelPosition', 'unit', 'decimals', 'labelOverlap')
    if (geometry || ['scatter', 'effectScatter', 'pictorialBar'].includes(type)) {
      fields.push('xTitle', 'xVisible', 'xTitleVisible', 'hideOverlap', 'dateFormat', 'yTitle', 'yVisible', 'grid')
      if (!geometry?.percentage) fields.push('numberFormat', 'min', 'max')
    }
  } else {
    if (seriesLegend) fields.push('legend', 'legendInteractive')
    if (['heatmap', 'graph', 'map', 'themeRiver'].includes(type)) fields.push('labels')
    if (['waterfall', 'lollipop', 'dumbbell', 'rangeBand', 'bubble', 'scatterFit', 'cumulativeLine', 'targetComparison', 'deviationBar'].includes(type)) {
      fields.push('yTitle', 'min', 'max', 'yVisible', 'grid')
    }
    if (['waterfall', 'lollipop', 'dumbbell', 'rangeBand', 'bubble', 'scatterFit'].includes(type)) fields.push('xTitle', 'xVisible', 'hideOverlap')
    if (type === 'bubble' || type === 'scatterFit') fields.push('xTitleVisible')
  }
  if (geometry?.series === 'line' || ['cumulativeLine', 'rankTrend'].includes(type)) {
    if (!geometry?.step) fields.push('smooth')
    fields.push('dashed', 'lineWidth', 'points', 'pointSize')
  }
  if (['scatter', 'effectScatter', 'bubble', 'stripPlot', 'scatterFit', 'lollipop', 'dumbbell', 'dualAxis', 'frequencyPolygon', 'rangeBand'].includes(type)) fields.push('pointSize')
  if (type === 'lines' || type === 'scatterFit') fields.push('lineWidth')
  if (geometry?.series === 'line' && !geometry.area && !geometry.stacked) fields.push('fill')
  if (geometry?.area || fields.includes('fill') || type === 'rangeBand') fields.push('opacity')
  if (geometry?.series === 'bar') fields.push('barMaxWidth', 'barCornerRadius')
  if (['pie', 'donut', 'rose'].includes(type)) fields.push('pieRadius')
  if (type === 'donut') fields.push('donutHole', 'donutTotal')
  return fields
}
