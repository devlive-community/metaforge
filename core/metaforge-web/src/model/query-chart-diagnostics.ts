import type { EChartsOption, LineSeriesOption } from 'echarts'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartPoint, type QueryChartRow } from './query-chart'
import type { ChartText } from './query-chart-analysis'
import { reportDateKey, reportNumber } from './report-values'

type Aggregated = ReturnType<typeof aggregateQueryChart>
type AddRows = (rows: QueryChartRow[]) => number
type Datum = { index: number; value: number; rows: QueryChartRow[] }
type Range = { low: number; high: number; actual?: number; rows: QueryChartRow[] }

function axes(options: QueryChartOptions, categories: string[], percent = false) {
  return {
    xAxis: { type: 'category' as const, data: categories, name: options.xTitleVisible ? options.xTitle : '',
      show: options.xVisible, axisLabel: { hideOverlap: options.hideOverlap } },
    yAxis: { type: 'value' as const, name: options.yTitle, min: options.min, max: options.max,
      axisLabel: { show: options.yVisible, ...(percent ? { formatter: '{value}%' } : {}) }, splitLine: { show: options.grid } },
  }
}

function stroke(options: QueryChartOptions) {
  return { width: options.lineWidth, type: options.dashed ? 'dashed' as const : 'solid' as const }
}

function legend(options: QueryChartOptions) {
  return { show: options.legend, selectedMode: options.legendInteractive, type: 'scroll' as const, bottom: 0 }
}

function trendSeries(options: QueryChartOptions, groups: Map<string, Datum[]>, addRows: AddRows): LineSeriesOption[] {
  return [...groups].flatMap(([name, values]) => {
    if (!values.length) return []
    const sorted = [...values].sort((a, b) => a.index - b.index)
    const data: NonNullable<LineSeriesOption['data']> = []
    sorted.forEach((point, index) => {
      const previous = sorted[index - 1]
      if (previous && point.index > previous.index + 1) data.push([previous.index + 1, null])
      const item = { value: [point.index, point.value], pointId: addRows(point.rows) }
      data.push(item)
    })
    return [{ type: 'line', name, data, smooth: options.smooth, lineStyle: stroke(options), showSymbol: options.points, symbolSize: options.pointSize }]
  })
}

function bucketRows(options: QueryChartOptions, rows: QueryChartRow[], grouped: boolean) {
  return aggregateQueryChart(rows, { ...options, filtersEnabled: false, metric: '', aggregation: 'count', series: grouped ? options.series : '' })
}

function indexByCategory(points: QueryChartPoint[]) {
  const categories = [...new Set(points.map(point => point.category))]
  return { categories, indexes: new Map(categories.map((name, index) => [name, index])) }
}

function measurements(options: QueryChartOptions, rows: QueryChartRow[], fields: string[], addRows: AddRows) {
  const buckets = bucketRows(options, rows, true)
  const { categories, indexes } = indexByCategory(buckets.points)
  const groups = new Map<string, Datum[]>()
  const weighted = options.type === 'weightedMeanLine'
  for (const point of buckets.points) {
    const pairs = (buckets.details.get(point.pointId) ?? []).flatMap(row => {
      const value = reportNumber(row[fields[0]]), factor = reportNumber(row[fields[1]])
      if (value === undefined || factor === undefined || weighted && factor <= 0) return []
      return [{ value, factor, row }]
    })
    const scale = pairs.reduce((max, pair) => Math.max(max, Math.abs(pair.factor), weighted ? 0 : Math.abs(pair.value)), 0)
    if (!scale) continue
    const sum = pairs.reduce((total, pair) => total + pair.factor / scale, 0)
    if (!sum) continue
    const value = weighted ? pairs.reduce((total, pair) => total + pair.value * (pair.factor / scale / sum), 0)
      : pairs.reduce((total, pair) => total + pair.value / scale, 0) / sum * 100
    if (!Number.isFinite(value)) continue
    const name = point.series || options.seriesName || fields[0]
    const series = groups.get(name) ?? []
    series.push({ index: indexes.get(point.category)!, value, rows: pairs.map(pair => pair.row) })
    groups.set(name, series)
  }
  const series = trendSeries(options, groups, addRows)
  if (series.length) return { ...axes(options, categories, !weighted), legend: legend(options), series }
}

function ranges(options: QueryChartOptions, categories: string[], values: (Range | undefined)[], addRows: AddRows, text: ChartText): EChartsOption | undefined {
  if (!values.some(Boolean)) return
  const baseline = values.map(value => value?.low ?? null)
  const bounds = values.map(value => value ? { value: [value.high - value.low, value.low, value.high], pointId: addRows(value.rows) } : null)
  const actual = values.map(value => value?.actual === undefined ? null : {
    value: value.actual, pointId: addRows(value.rows),
    itemStyle: value.actual < value.low || value.actual > value.high ? { color: '#d15864' } : undefined,
  })
  return { ...axes(options, categories), series: [
    { type: 'line', stack: 'bounds', stackStrategy: 'all', silent: true, symbol: 'none', lineStyle: { opacity: 0 },
      tooltip: { show: false }, data: baseline },
    { type: 'line', stack: 'bounds', stackStrategy: 'all', name: text('diagnosticRange', 'Range'), symbol: 'none', lineStyle: stroke(options),
      dimensions: ['span', text('mapping.lower', 'Lower'), text('mapping.upper', 'Upper')],
      encode: { y: 0, tooltip: [1, 2] }, areaStyle: { opacity: options.opacity / 100 }, data: bounds },
    ...(actual.some(Boolean) ? [{ type: 'line' as const, name: text('diagnosticObserved', 'Observed'), data: actual, symbolSize: options.pointSize, lineStyle: stroke(options) }] : []),
  ] }
}

function thresholdRanges(options: QueryChartOptions, rows: QueryChartRow[], fields: string[], addRows: AddRows, text: ChartText) {
  const buckets = bucketRows(options, rows, false)
  const values = buckets.points.map(point => {
    const valid = (buckets.details.get(point.pointId) ?? []).flatMap(row => {
      const actual = reportNumber(row[fields[0]]), low = reportNumber(row[fields[1]]), high = reportNumber(row[fields[2]])
      if (actual === undefined || low === undefined || high === undefined || low > high) return []
      return [{ actual, low, high, row }]
    })
    if (!valid.length) return
    const average = (key: 'actual' | 'low' | 'high') => valid.reduce((sum, entry) => sum + entry[key] / valid.length, 0)
    return { actual: average('actual'), low: average('low'), high: average('high'), rows: valid.map(entry => entry.row) }
  })
  return ranges(options, buckets.points.map(point => point.category), values, addRows, text)
}

function rollingBands(options: QueryChartOptions, result: Aggregated, addRows: AddRows, text: ChartText) {
  const window: QueryChartPoint[] = []
  let lower: QueryChartPoint | undefined, upper: QueryChartPoint | undefined
  const running = options.type === 'runningEnvelope'
  const values = result.points.map(point => {
    if (point.value === null) { window.length = 0; return }
    if (running) {
      if (lower?.value == null || point.value < lower.value) lower = point
      if (upper?.value == null || point.value > upper.value) upper = point
      return { low: lower.value!, high: upper.value!, rows: [...new Set([lower, upper, point].flatMap(item => result.details.get(item.pointId) ?? []))] }
    }
    window.push(point)
    if (window.length > 3) window.shift()
    if (window.length === 3) return { low: Math.min(...window.map(item => item.value!)), high: Math.max(...window.map(item => item.value!)),
      rows: window.flatMap(item => result.details.get(item.pointId) ?? []) }
  })
  return ranges(options, result.points.map(point => point.category), values, addRows, text)
}

function changeTrend(options: QueryChartOptions, result: Aggregated, addRows: AddRows) {
  const { categories, indexes } = indexByCategory(result.points)
  const groups = new Map<string, Datum[]>()
  for (const point of result.points) {
    const name = point.series || options.seriesName || options.metric || options.category
    const group = groups.get(name) ?? []
    if (point.value !== null) group.push({ index: indexes.get(point.category)!, value: point.value, rows: result.details.get(point.pointId) ?? [] })
    groups.set(name, group)
  }
  const change = new Map<string, Datum[]>()
  const slope = options.type === 'slopeChange'
  const growth = options.type === 'growthTrend'
  for (const [name, values] of groups) {
    const ordered = [...values].sort((left, right) => left.index - right.index)
    if (slope) {
      if (categories.length > 1 && ordered[0]?.index === 0 && ordered.at(-1)?.index === categories.length - 1) {
        change.set(name, [ordered[0], { ...ordered.at(-1)!, index: 1 }])
      }
      continue
    }
    change.set(name, ordered.flatMap((point, index) => {
      const previous = ordered[index - 1]
      if (previous?.index !== point.index - 1) return []
      const delta = point.value - previous.value
      let value = growth ? delta / Math.abs(previous.value) * 100 : delta
      let rows = [...previous.rows, ...point.rows]
      if (options.type === 'rollingMedian') {
        const earlier = ordered[index - 2]
        if (earlier?.index !== point.index - 2) return []
        value = [earlier.value, previous.value, point.value].sort((left, right) => left - right)[1]
        rows = [...earlier.rows, ...rows]
      }
      return Number.isFinite(value) ? [{ ...point, value, rows }] : []
    }))
  }
  const series = trendSeries(options, change, addRows)
  if (series.length) return { ...axes(options, slope ? [categories[0], categories.at(-1)!] : categories, growth), legend: legend(options), series }
}

function coverage(options: QueryChartOptions, rows: QueryChartRow[], addRows: AddRows, text: ChartText): EChartsOption | undefined {
  const buckets = bucketRows(options, rows, false)
  const fields = options.fields.filter(name => name !== options.category)
  if (!buckets.points.length || !fields.length) return
  const data = buckets.points.flatMap((point, index) => {
    const records = buckets.details.get(point.pointId) ?? []
    const pointId = addRows(records)
    return fields.map((field, position) => {
      const present = records.reduce((count, row) => {
        const value = row[field]
        return count + (value != null && (typeof value !== 'string' || value.trim()) ? 1 : 0)
      }, 0)
      return { value: [index, position, 100 * present / records.length, present, records.length], pointId }
    })
  })
  const axis = axes(options, buckets.points.map(point => point.category))
  return { ...axis, yAxis: { type: 'category', data: fields, name: options.yTitle, inverse: true, show: options.yVisible },
    visualMap: { min: 0, max: 100, dimension: 2, orient: 'horizontal', bottom: 0, left: 'center', text: ['100%', '0%'] },
    series: [{ type: 'heatmap', data, dimensions: [options.category, text('diagnosticField', 'Field'), text('diagnosticCoverage', 'Coverage (%)'),
      text('diagnosticPresent', 'Present'), text('records', 'Records')], encode: { x: 0, y: 1, tooltip: [2, 3, 4] }, label: { show: options.labels } }] }
}

function seasonal(options: QueryChartOptions, rows: QueryChartRow[], addRows: AddRows) {
  const months = aggregateQueryChart(rows.filter(row => reportDateKey(row[options.category])), { ...options, filtersEnabled: false, grain: 'month', series: '' })
  const groups = new Map<string, Datum[]>()
  for (const point of months.points) {
    if (point.value === null || !/^\d{4}-\d{2}$/.test(point.category)) continue
    const [year, month] = point.category.split('-')
    const series = groups.get(year) ?? []
    series.push({ index: Number(month) - 1, value: point.value, rows: months.details.get(point.pointId) ?? [] })
    groups.set(year, series)
  }
  const series = trendSeries(options, groups, addRows)
  if (series.length) return { ...axes(options, Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, '0'))), legend: legend(options), series }
}

function histogram(options: QueryChartOptions, rows: QueryChartRow[], field: string, addRows: AddRows): EChartsOption | undefined {
  const samples = rows.flatMap(row => {
    const value = reportNumber(row[field])
    return value === undefined || row[options.category] == null ? [] : [{ name: String(row[options.category]), value, row }]
  })
  if (!samples.length) return
  const scale = samples.reduce((max, sample) => Math.max(max, Math.abs(sample.value)), 0) || 1
  const lower = samples.reduce((min, sample) => Math.min(min, sample.value / scale), Infinity)
  const upper = samples.reduce((max, sample) => Math.max(max, sample.value / scale), -Infinity)
  const count = lower === upper ? 1 : Math.min(16, Math.max(4, Math.ceil(Math.sqrt(samples.length))))
  const bounds = Array.from({ length: count + 1 }, (_, index) => (lower * (1 - index / count) + upper * index / count) * scale)
  const categories = bounds.slice(0, -1).map((value, index) => count === 1 ? String(value) : `${+value.toPrecision(4)} – ${+bounds[index + 1].toPrecision(4)}`)
  const groups = new Map<string, QueryChartRow[][]>()
  for (const sample of samples) {
    const bins = groups.get(sample.name) ?? Array.from({ length: count }, () => [])
    const bin = count === 1 ? 0 : Math.min(count - 1, Math.max(0, Math.floor((sample.value / scale - lower) / (upper - lower) * count)))
    bins[bin].push(sample.row)
    groups.set(sample.name, bins)
  }
  return { ...axes(options, categories, true), legend: legend(options), series: [...groups].map(([name, bins]) => {
    const total = bins.reduce((sum, bin) => sum + bin.length, 0)
    return { type: 'line', step: 'middle', name, lineStyle: stroke(options), showSymbol: options.points, symbolSize: options.pointSize,
      data: bins.map(bin => ({ value: 100 * bin.length / total, pointId: bin.length ? addRows(bin) : undefined })) }
  }) }
}

export function diagnosticChartOption(options: QueryChartOptions, result: Aggregated, fields: string[], addRows: AddRows, text: ChartText): EChartsOption | undefined {
  switch (options.type) {
    case 'weightedMeanLine':
    case 'ratioTrend': return measurements(options, result.filtered, fields, addRows)
    case 'thresholdBands': return thresholdRanges(options, result.filtered, fields, addRows, text)
    case 'rollingRange':
    case 'runningEnvelope': return rollingBands(options, bucketedMetric(options, result.filtered), addRows, text)
    case 'coverageHeatmap': return coverage(options, result.filtered, addRows, text)
    case 'seasonalProfile': return seasonal(options, result.filtered, addRows)
    case 'histogramOverlay': return histogram(options, result.filtered, fields[0], addRows)
    default: return changeTrend(options, result, addRows)
  }
}

function bucketedMetric(options: QueryChartOptions, rows: QueryChartRow[]) {
  return aggregateQueryChart(rows, { ...options, filtersEnabled: false, series: '', nulls: options.nulls === 'zero' ? 'zero' : 'keep' })
}
