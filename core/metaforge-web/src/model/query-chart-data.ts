import type { QueryChartOptions, QueryChartType } from './query-chart'

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
  gauge: {},
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
  if ((type === 'boxplot' || type === 'histogram') && !chartFields.value && numericFields[0]) chartFields.value = numericFields[0]
  return { chartFields }
}
