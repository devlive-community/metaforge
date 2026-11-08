import type { QueryChartOptions, QueryChartPoint, QueryChartType } from './query-chart'

export const geoJsonLimit = 1_000_000

export type ChartMappings = Pick<QueryChartOptions, 'category' | 'series' | 'chartFields' | 'parallelFields' | 'mapGeoJson'>

interface ChartDataSpec {
  category?: string
  series?: string
  raw?: boolean
  fields?: readonly string[]
  parallel?: boolean
  map?: boolean
}

const specs: Partial<Record<QueryChartType, ChartDataSpec>> = {
  heatmap: { category: 'xCategory', series: 'yCategory' },
  graph: { category: 'sourceNode', series: 'targetNode' },
  sankey: { category: 'sourceNode', series: 'targetNode' },
  tree: { category: 'child', series: 'parent' },
  treemap: { category: 'child', series: 'parent' },
  sunburst: { category: 'child', series: 'parent' },
  themeRiver: { category: 'time', series: 'group' },
  map: { category: 'region', map: true },
  candlestick: { category: 'category', raw: true, fields: ['open', 'close', 'low', 'high'] },
  boxplot: { category: 'category', raw: true, fields: ['value'] },
  histogram: { raw: true, fields: ['value'] },
  parallel: { raw: true, parallel: true },
  lines: { raw: true, fields: ['startX', 'startY', 'endX', 'endY'] },
  custom: { category: 'category', raw: true, fields: ['lower', 'upper'] },
  bubble: { raw: true, fields: ['bubbleX', 'bubbleY', 'bubbleSize'] },
  waterfall: { category: 'category' },
  lollipop: { category: 'category' },
  dumbbell: { category: 'category', raw: true, fields: ['before', 'after'] },
  rangeBand: { category: 'category', raw: true, fields: ['lower', 'upper'] },
  dualAxis: { category: 'category', raw: true, fields: ['columnMeasure', 'lineMeasure'] },
  polarBar: { category: 'category', series: 'group' },
  polarStackedBar: { category: 'category', series: 'group' },
  polarLine: { category: 'category', series: 'group' },
  gauge: {},
}

type ChartGeometry = { series: 'line' | 'bar'; area?: boolean; stacked?: boolean; horizontal?: boolean; step?: boolean; percentage?: boolean }
const geometries: Partial<Record<QueryChartType, ChartGeometry>> = {
  line: { series: 'line' },
  area: { series: 'line', area: true },
  bar: { series: 'bar' },
  horizontal: { series: 'bar', horizontal: true },
  stepLine: { series: 'line', step: true },
  stackedBar: { series: 'bar', stacked: true },
  stackedHorizontal: { series: 'bar', stacked: true, horizontal: true },
  stackedArea: { series: 'line', stacked: true, area: true },
  percentStackedBar: { series: 'bar', stacked: true, percentage: true },
  stackedLine: { series: 'line', stacked: true },
  percentStackedHorizontal: { series: 'bar', stacked: true, horizontal: true, percentage: true },
}

export function queryChartGeometry(type: QueryChartType) {
  return geometries[type]
}

/** Calculate shares from the full category, even if a legend item is hidden. */
export function queryChartCategoryShares(points: readonly QueryChartPoint[]) {
  const totals = new Map<string, number>()
  for (const { category, value } of points) {
    if (value !== null) totals.set(category, (totals.get(category) ?? 0) + value)
  }
  const shares = new Map<number, number>()
  for (const { pointId, category, value } of points) {
    if (value === null) continue
    const total = totals.get(category) ?? 0
    shares.set(pointId, total > 0 ? value / total : 0)
  }
  return shares
}

export function chartDataSpec(type: QueryChartType): ChartDataSpec | undefined {
  return specs[type]
}

export function requiresChartSeries(type: QueryChartType): boolean {
  return ['heatmap', 'graph', 'sankey', 'themeRiver'].includes(type)
}

const fieldAliases: Record<string, string[]> = {
  open: ['open', 'openingprice'], close: ['close', 'closingprice'],
  low: ['low', 'lowestprice'], high: ['high', 'highestprice'],
  lower: ['lower', 'min', 'low', 'start'], upper: ['upper', 'max', 'high', 'end'],
  value: ['value', 'amount', 'metric'],
  startX: ['startx', 'sourcex', 'fromx'], startY: ['starty', 'sourcey', 'fromy'],
  endX: ['endx', 'targetx', 'tox'], endY: ['endy', 'targety', 'toy'],
  bubbleX: ['x', 'xvalue'], bubbleY: ['y', 'yvalue'], bubbleSize: ['size', 'bubblesize', 'weight', 'radius'],
  before: ['before', 'previous', 'baseline'], after: ['after', 'current', 'actual'],
  columnMeasure: ['volume', 'count', 'quantity'], lineMeasure: ['rate', 'price', 'average'],
}

export function suggestChartFields(type: QueryChartType, numericFields: string[]): Pick<ChartMappings, 'chartFields' | 'parallelFields'> {
  const spec = chartDataSpec(type)
  if (spec?.parallel) return { parallelFields: numericFields.slice(0, 6) }
  if (!spec?.fields) return {}
  const normalized = (field: string) => field.toLowerCase().replace(/[^a-z0-9]/g, '')
  const chartFields: Record<string, string> = {}
  for (const key of spec.fields) {
    const field = numericFields.find(name => fieldAliases[key]?.includes(normalized(name)))
    if (field) chartFields[key] = field
  }
  if (['bubble', 'dumbbell', 'rangeBand', 'dualAxis'].includes(type)) {
    const available = numericFields.filter(field => !Object.values(chartFields).includes(field))
    for (const key of spec.fields) if (!chartFields[key] && available.length) chartFields[key] = available.shift()!
  }
  if ((type === 'boxplot' || type === 'histogram') && !chartFields.value && numericFields[0]) chartFields.value = numericFields[0]
  return { chartFields }
}
