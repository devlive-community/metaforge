import { isCompleteFilter, type FilterFieldKind, type FilterGroup, type FilterJoin, type FilterRule } from './typed-filter'

export const queryChartTypes = [
  'line', 'bar', 'area', 'pie', 'donut', 'scatter', 'horizontal',
  'radar', 'funnel', 'rose', 'table', 'effectScatter', 'pictorialBar', 'heatmap', 'gauge',
  'candlestick', 'boxplot', 'histogram', 'parallel', 'themeRiver', 'tree', 'treemap', 'sunburst',
  'graph', 'sankey', 'map', 'lines', 'custom',
  'stepLine', 'stackedBar', 'stackedHorizontal', 'stackedArea', 'percentStackedBar', 'bubble',
  'stackedLine', 'percentStackedHorizontal', 'waterfall', 'lollipop', 'dumbbell', 'rangeBand',
  'dualAxis', 'polarBar', 'polarStackedBar', 'polarLine',
  'pareto', 'cumulativeLine', 'deviationBar', 'mirrorBar', 'stripPlot', 'ecdf',
  'scatterFit', 'calendarMatrix', 'radialBar', 'frequencyPolygon', 'targetComparison', 'rankTrend',
  'tukeyBox', 'violinPlot', 'ridgePlot', 'quantileComparison', 'percentileProfile', 'lorenzCurve',
  'ganttTimeline', 'signedShareBar', 'quadrantScatter', 'lagScatter', 'indexTrend', 'movingAverage',
] as const
export type QueryChartType = typeof queryChartTypes[number]
export type QueryChartRow = Record<string, unknown>
export interface QueryChartOptions {
  type: QueryChartType
  name: string
  description: string
  fields: string[]
  category: string
  metric: string
  aggregation: 'count' | 'distinct' | 'sum' | 'avg' | 'min' | 'max'
  series: string
  grain: 'none' | 'hour' | 'day' | 'week' | 'month'
  chartFields?: Record<string, string>
  parallelFields?: string[]
  mapGeoJson?: string
  filtersEnabled: boolean
  filterJoin: FilterJoin
  groups: FilterGroup[]
  xTitle: string
  yTitle: string
  dateFormat: 'short' | 'full' | 'time'
  sort: 'asc' | 'desc' | 'original'
  xVisible: boolean
  xTitleVisible: boolean
  hideOverlap: boolean
  yVisible: boolean
  grid: boolean
  numberFormat: 'integer' | 'decimal' | 'percent' | 'compact'
  min?: number
  max?: number
  legend: boolean
  legendPosition: 'top' | 'bottom' | 'right'
  seriesName: string
  legendInteractive: boolean
  labels: boolean
  labelPosition: 'auto' | 'top' | 'inside'
  unit: string
  decimals: number
  labelOverlap: boolean
  palette: 'green' | 'blue' | 'purple'
  colors: string
  smooth: boolean
  dashed: boolean
  lineWidth: number
  pointSize: number
  points: boolean
  fill: boolean
  opacity: number
  barMaxWidth?: number
  barCornerRadius?: number
  pieRadius?: number
  donutHole?: number
  donutTotal?: boolean
  transparent: boolean
  tooltip: 'dimension' | 'mark' | 'off'
  tooltipTemplate: string
  zoom: boolean
  download: boolean
  pointAction: 'details' | 'filter' | 'highlight' | 'none'
  refreshSeconds?: number
  detailsMode: 'panel' | 'dialog'
  nulls: 'ignore' | 'zero' | 'keep'
  timezone: string
  maxRows: number
}

export interface QueryChartConfiguration {
  queryChart: { version: 1; options: QueryChartOptions }
  headers: string[]
  columns: QueryChartRow[]
}

export const snapshotRowLimit = 10000

export function chartFieldKind(rows: QueryChartRow[], name: string, type?: string): FilterFieldKind {
  if (type && /date|time/i.test(type)) return 'date'
  if (type && /int|decimal|double|float|numeric|number|real/i.test(type) && !/interval|point/i.test(type)) return 'number'
  if (type && /bool/i.test(type)) return 'boolean'
  const sample = rows.slice(0, 200).map(row => row[name]).filter(value => value !== null && value !== undefined && value !== '')
  if (sample.length && sample.every(value => typeof value === 'boolean')) return 'boolean'
  if (sample.length && sample.every(value => typeof value === 'number')) return 'number'
  if (sample.length && sample.every(value => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}(?:$|[T ])/.test(value) && Number.isFinite(Date.parse(value)))) return 'date'
  return 'text'
}

export function defaultQueryChart(headers: string[], dateField?: string): QueryChartOptions {
  return {
    type: 'line', name: '', description: '', fields: [...headers], category: dateField || headers[0] || '',
    metric: '', aggregation: 'count', series: '', grain: dateField ? 'day' : 'none',
    filtersEnabled: false, filterJoin: 'or', groups: [],
    xTitle: '', yTitle: '', dateFormat: 'short', sort: 'asc', xVisible: true, xTitleVisible: true, hideOverlap: true,
    yVisible: true, grid: true, numberFormat: 'integer',
    legend: true, legendPosition: 'bottom', seriesName: '', legendInteractive: true,
    labels: false, labelPosition: 'auto', unit: '', decimals: 0, labelOverlap: true,
    palette: 'green', colors: '', smooth: false, dashed: false, lineWidth: 2, pointSize: 5, points: true,
    fill: true, opacity: 24, transparent: false,
    tooltip: 'dimension', tooltipTemplate: '', zoom: false, download: true, pointAction: 'details', detailsMode: 'panel',
    refreshSeconds: 0, nulls: 'ignore', timezone: 'Asia/Shanghai', maxRows: snapshotRowLimit
  }
}

function asText(value: unknown): string {
  return typeof value === 'object' && value !== null ? JSON.stringify(value) : String(value ?? '')
}

function asNumber(value: unknown) {
  if (typeof value !== 'number' && (typeof value !== 'string' || !value.trim())) return undefined
  const result = Number(value)
  return Number.isFinite(result) ? result : undefined
}

function dateValue(value: unknown) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}/.test(value)) return undefined
  // Parse offset-less values consistently for comparison; grouping preserves their wall time.
  const normalized = /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?$/.test(value)
      ? value.replace(' ', 'T') + 'Z' : value
  const millis = Date.parse(normalized)
  return Number.isFinite(millis) ? millis : undefined
}

function timeGroup(value: unknown, grain: QueryChartOptions['grain'], dateFormat: Intl.DateTimeFormat) {
  const millis = typeof value === 'number' && Number.isFinite(new Date(value).getTime()) ? value : dateValue(value)
  if (grain === 'none' || millis === undefined) return asText(value)
  // Offset-less SQL dates and timestamps carry wall time, so preserve their calendar fields.
  const wallTime = typeof value === 'string' ? /^(\d{4}-\d{2}-\d{2})(?:[T ](\d{2}):\d{2}(?::\d{2}(?:\.\d+)?)?)?$/.exec(value) : null
  const parts = new Map(dateFormat.formatToParts(millis).map(part => [part.type, part.value]))
  const date = wallTime ? wallTime[1] : `${parts.get('year')}-${parts.get('month')}-${parts.get('day')}`
  if (grain === 'hour') return `${date} ${wallTime ? wallTime[2] ?? '00' : parts.get('hour')}:00`
  if (grain === 'month') return date.slice(0, 7)
  if (grain === 'week') {
    const monday = new Date(date + 'T00:00:00Z')
    monday.setUTCDate(monday.getUTCDate() - (monday.getUTCDay() + 6) % 7)
    return monday.toISOString().slice(0, 10)
  }
  return date
}

function compare(value: unknown, target: string) {
  const left = asNumber(value) ?? dateValue(value)
  const right = asNumber(target) ?? dateValue(target)
  return left !== undefined && right !== undefined ? left - right : asText(value).localeCompare(target)
}

function matches(row: QueryChartRow, rule: FilterRule, formatter: Intl.DateTimeFormat) {
  const raw = row[rule.field]
  const dayComparison = rule.operator !== 'contains' && rule.operator !== 'in' && /^\d{4}-\d{2}-\d{2}$/.test(rule.value)
  const value = dayComparison && (dateValue(raw) !== undefined || typeof raw === 'number') ? timeGroup(raw, 'day', formatter) : raw
  if (rule.operator === 'null') return value == null
  if (rule.operator === 'notNull') return value != null
  if (value == null || (rule.kind === 'number' && asNumber(value) === undefined)) return false
  const text = rule.kind === 'boolean' && (value === 0 || value === 1) ? String(value === 1) : asText(value)
  switch (rule.operator) {
    case 'eq': return rule.kind === 'number' ? compare(value, rule.value) === 0 : text === rule.value
    case 'ne': return rule.kind === 'number' ? compare(value, rule.value) !== 0 : text !== rule.value
    case 'contains': return text.toLocaleLowerCase().includes(rule.value.toLocaleLowerCase())
    case 'in': return rule.value.split(',').map(item => item.trim()).includes(text)
    case 'gt': return compare(value, rule.value) > 0
    case 'lt': return compare(value, rule.value) < 0
    case 'gte': return compare(value, rule.value) >= 0
    case 'lte': return compare(value, rule.value) <= 0
    case 'between': return compare(value, rule.value) >= 0 && compare(value, rule.end) <= 0
  }
}

export interface QueryChartPoint extends QueryChartRow {
  pointId: number
  category: string
  series: string
  value: number | null
}

export function aggregateQueryChart(rows: QueryChartRow[], options: QueryChartOptions) {
  const groups = options.groups.map(group => ({ ...group, rules: group.rules.filter(isCompleteFilter) })).filter(group => group.rules.length)
  const limited = rows.slice(0, Math.max(1, Math.min(snapshotRowLimit, Math.trunc(options.maxRows) || snapshotRowLimit)))
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: options.timezone, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', hourCycle: 'h23'
  })
  const filtered = options.filtersEnabled && groups.length ? limited.filter(row => {
    const evaluate = (group: FilterGroup) => group.join === 'and'
        ? group.rules.every(rule => matches(row, rule, formatter)) : group.rules.some(rule => matches(row, rule, formatter))
    return options.filterJoin === 'and' ? groups.every(evaluate) : groups.some(evaluate)
  }) : limited
  const buckets = new Map<string, { category: string; series: string; rows: QueryChartRow[] }>()
  if (options.category) filtered.forEach(row => {
    if (row[options.category] == null && options.nulls === 'ignore') return
    const category = timeGroup(row[options.category], options.grain, formatter)
    const series = options.series && !['map', 'waterfall', 'lollipop', 'pareto', 'deviationBar', 'calendarMatrix', 'radialBar'].includes(options.type)
      ? asText(row[options.series]) : ''
    const key = JSON.stringify([category, series])
    if (!buckets.has(key)) buckets.set(key, { category, series, rows: [] })
    buckets.get(key)!.rows.push(row)
  })
  const details = new Map<number, QueryChartRow[]>()
  const points = Array.from(buckets.values(), (bucket, pointId): QueryChartPoint => {
    const cells = bucket.rows.map(row => row[options.metric])
    const numbers = cells.map(cell => cell == null && options.nulls === 'zero' ? 0 : asNumber(cell))
        .filter((value): value is number => value !== undefined)
    let value: number | null = null
    if (options.aggregation === 'count') value = options.metric ? cells.filter(cell => cell != null).length : bucket.rows.length
    else if (options.aggregation === 'distinct') value = new Set(cells.filter(cell => cell != null).map(asText)).size
    else if (numbers.length) {
      const sum = numbers.reduce((total, number) => total + number, 0)
      if (options.aggregation === 'sum') value = sum
      if (options.aggregation === 'avg') value = sum / numbers.length
      if (options.aggregation === 'min') value = Math.min(...numbers)
      if (options.aggregation === 'max') value = Math.max(...numbers)
    }
    details.set(pointId, bucket.rows)
    return { pointId, category: bucket.category, series: bucket.series, value: value !== null && Number.isFinite(value) ? value : null }
  }).filter(point => point.value !== null || options.nulls === 'keep')
  if (options.sort !== 'original') points.sort((left, right) => (options.sort === 'asc' ? 1 : -1) * left.category.localeCompare(right.category, undefined, { numeric: true }))
  return { points, details, filtered, rowCount: filtered.length, limitedCount: limited.length }
}

export function createQueryChartSnapshot(options: QueryChartOptions, rows: QueryChartRow[]): QueryChartConfiguration {
  return {
    queryChart: { version: 1, options },
    headers: [...options.fields],
    columns: rows.slice(0, Math.max(1, Math.min(snapshotRowLimit, Math.trunc(options.maxRows) || snapshotRowLimit)))
        .map(row => Object.fromEntries(options.fields.map(name => [name, row[name] ?? null])))
  }
}
