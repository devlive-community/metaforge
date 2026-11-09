import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import * as echarts from 'echarts'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { aggregateQueryChart, defaultQueryChart, diagnosticChartTypes, queryChartTypes } = await sourceModule('../../metaforge-web/src/model/query-chart.ts')
const { prepareQueryChart } = await sourceModule('../../metaforge-web/src/model/query-chart-series.ts')
const { queryChartCategoryShares, suggestChartFields } = await sourceModule('../../metaforge-web/src/model/query-chart-data.ts')
const { dashboardTools, dashboardChartMappings, dashboardEmbeddedChartOptions, dashboardWidget } = await sourceModule('../../metaforge-web/src/model/dashboard-layout.ts')
const { screenComponentExample } = await sourceModule('../../metaforge-web/src/model/screen-component-example.ts')
const { fitChartToCard } = await sourceModule('../../metaforge-web/src/model/chart-card-layout.ts')
const rows = [
  { region: 'A', group: 'C', date: '2026-10-01', open: 12, close: 15, low: 10, high: 17, startX: 0, startY: 1, endX: 2, endY: 4 },
  { region: 'B', group: 'D', date: '2026-10-01', open: 16, close: 14, low: 11, high: 18, startX: 3, startY: 2, endX: 4, endY: 5 },
  { region: 'A', group: 'C', date: '2026-10-02', open: 14, close: 18, low: 12, high: 20, startX: 1, startY: 2, endX: 3, endY: 5 },
  { region: 'B', group: 'D', date: '2026-10-02', open: 17, close: 19, low: 15, high: 21, startX: 4, startY: 3, endX: 6, endY: 6 },
]
const headers = Object.keys(rows[0])
const mapGeoJson = JSON.stringify({ type: 'FeatureCollection', features: ['A', 'B'].map((name, index) => ({
  type: 'Feature', properties: { name }, geometry: { type: 'Polygon', coordinates: [[[index, 0], [index + 1, 0], [index + 1, 1], [index, 0]]] },
})) })
const options = {
  ...defaultQueryChart(headers), category: 'region', series: 'group', mapGeoJson,
  parallelFields: ['open', 'close', 'high'],
  chartFields: { open: 'open', close: 'close', low: 'low', high: 'high', value: 'open', lower: 'low', upper: 'high',
    startX: 'startX', startY: 'startY', endX: 'endX', endY: 'endY',
    before: 'open', after: 'close', columnMeasure: 'open', lineMeasure: 'close',
    leftMeasure: 'open', rightMeasure: 'close', actualMeasure: 'close', targetMeasure: 'high',
    bubbleX: 'open', bubbleY: 'close', intervalStart: 'date', intervalEnd: 'date' },
}

test('the dashboard includes every type shown in the visualizer', () => {
  assert.equal(new Set(dashboardTools).size, dashboardTools.length)
  for (const type of queryChartTypes) assert.ok(dashboardTools.includes(type), type)
})

test('diagnostic examples export from a compact card with their contributing rows', () => {
  for (const type of diagnosticChartTypes) {
    const { widget, snapshot } = screenComponentExample(type, type, key => key, ['/a.svg', '/b.svg'], Date.UTC(2026, 10, 8))
    const options = dashboardEmbeddedChartOptions(widget, snapshot, false)
    const prepared = prepareQueryChart(options, aggregateQueryChart(snapshot.columns, options))
    assert.ok(prepared.option && prepared.details.size, type)
    const chart = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width: 320, height: 210 })
    try {
      chart.setOption(fitChartToCard({ ...prepared.option, animation: false }, 320, 210, '#345'))
      assert.match(chart.renderToSVGString(), /<(path|rect|polygon)\b/, type)
    } finally {
      chart.dispose()
    }
  }
})

test('weighted means ignore unusable weights and ratios skip a zero denominator', () => {
  const records = [
    { period: 'A', value: 10, factor: 1 }, { period: 'A', value: 20, factor: 3 }, { period: 'A', value: 500, factor: -1 },
    { period: 'B', value: 5, factor: 0 },
  ]
  const options = { ...defaultQueryChart(['period', 'value', 'factor']), category: 'period',
    chartFields: { value: 'value', weight: 'factor', numerator: 'value', denominator: 'factor' } }
  const prepare = type => prepareQueryChart({ ...options, type }, aggregateQueryChart(records, { ...options, type }))
  const weighted = prepare('weightedMeanLine')
  assert.deepEqual(weighted.option.series[0].data[0].value, [0, 17.5])
  assert.deepEqual(weighted.details.get(0), records.slice(0, 2))
  const ratio = prepare('ratioTrend')
  assert.equal(ratio.option.series[0].data.length, 1)
  assert.deepEqual(ratio.details.get(0), records.slice(0, 3))
})

test('specialized field mappings produce real ECharts series that can render SVG', () => {
  const types = ['heatmap', 'gauge', 'candlestick', 'boxplot', 'histogram', 'parallel', 'themeRiver', 'tree', 'treemap', 'sunburst', 'graph', 'sankey', 'map', 'lines', 'custom']
  const compositions = { waterfall: 'custom', lollipop: 'custom', dumbbell: 'custom', rangeBand: 'line', dualAxis: 'bar',
    polarBar: 'bar', polarStackedBar: 'bar', polarLine: 'line', pareto: 'bar', cumulativeLine: 'line',
    deviationBar: 'bar', mirrorBar: 'bar', stripPlot: 'scatter', ecdf: 'line', scatterFit: 'scatter',
    calendarMatrix: 'heatmap', radialBar: 'bar', frequencyPolygon: 'line', targetComparison: 'bar', rankTrend: 'line',
    tukeyBox: 'boxplot', violinPlot: 'custom', ridgePlot: 'custom', quantileComparison: 'scatter', percentileProfile: 'line',
    lorenzCurve: 'line', ganttTimeline: 'custom', signedShareBar: 'bar', quadrantScatter: 'scatter', lagScatter: 'scatter',
    indexTrend: 'line', movingAverage: 'line' }
  types.push(...Object.keys(compositions))
  for (const type of types) {
    const chartOptions = { ...options, type, category: ['themeRiver', 'calendarMatrix', 'movingAverage'].includes(type) ? 'date' : options.category }
    const records = type === 'movingAverage' ? rows.map((row, index) => ({ ...row, date: '2026-10-0' + (index + 1), group: 'C' })) : rows
    const prepared = prepareQueryChart(chartOptions, aggregateQueryChart(records, chartOptions))
    assert.equal(prepared.issue, undefined, type)
    assert.equal(prepared.option.series[0].type, compositions[type] ?? (type === 'histogram' ? 'bar' : type))
    assert.ok(prepared.details.size, type)
    if (prepared.map) echarts.registerMap(prepared.map.name, prepared.map.source)
    const instance = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width: 540, height: 380 })
    try {
      instance.setOption({ animation: false, ...prepared.option })
      assert.match(instance.renderToSVGString(), /<(path|rect|polygon)\b/, type)
    }
    finally { instance.dispose() }
  }
})

test('distribution charts retain the raw rows behind outliers and lagged pairs', () => {
  const records = [1, 2, 3, 4, 100, null, 5].map(value => ({ group: 'A', value }))
  const options = { ...defaultQueryChart(['group', 'value']), category: 'group', chartFields: { value: 'value' } }
  const render = type => prepareQueryChart({ ...options, type }, aggregateQueryChart(records, { ...options, type }))
  const box = render('tukeyBox')
  assert.deepEqual(box.option.series[0].data[0].value, [1, 2.25, 3.5, 4.75, 5])
  const [outlier] = box.option.series[1].data
  assert.deepEqual(outlier.value, [0, 100])
  assert.equal(box.details.get(outlier.pointId)[0], records[4])
  const lag = render('lagScatter')
  assert.deepEqual(lag.option.series[0].data.map(point => point.value), [[1, 2], [2, 3], [3, 4], [4, 100]])
  assert.deepEqual(lag.details.get(3), records.slice(3, 5))
})

test('rank trend handles ties and cumulative lines follow the shared category order', () => {
  const records = [
    { period: 'A', name: 'first', amount: 5 },
    { period: 'B', name: 'first', amount: 1 },
    { period: 'B', name: 'second', amount: 4 },
    { period: 'A', name: 'second', amount: 5 },
    { period: 'A', name: 'third', amount: 2 },
  ]
  const chart = { ...defaultQueryChart(['period', 'name', 'amount']), category: 'period', series: 'name', metric: 'amount', aggregation: 'sum', sort: 'original' }
  const render = type => prepareQueryChart({ ...chart, type }, aggregateQueryChart(records, { ...chart, type }))
  const rank = render('rankTrend')
  const trends = new Map(rank.option.series.map(series => [series.name, series.data.map(point => point.value)]))
  assert.deepEqual(trends.get('second'), [[0, 1], [1, 1]])
  assert.deepEqual(trends.get('third'), [[0, 3]])
  const cumulative = render('cumulativeLine').option.series.find(series => series.name === 'second')
  assert.deepEqual(cumulative.data.map(point => point.value), [[0, 5], [1, 9]])
})

test('the calendar renders localized months and selects the latest year with data', async () => {
  const zh = JSON.parse(await readFile(new URL('../src/i18n/locales/zh-cn.json', import.meta.url), 'utf8'))
  const samples = [{ date: '2025-10-01' }, { date: '2026-08-12' }]
  const chart = { ...defaultQueryChart(['date'], 'date'), type: 'calendarMatrix' }
  const result = prepareQueryChart(chart, aggregateQueryChart(samples, chart), (key, fallback) => zh['queryChart.' + key] ?? fallback)
  const instance = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width: 840, height: 320 })
  try {
    instance.setOption({ animation: false, ...result.option })
    assert.match(instance.renderToSVGString(), /8月/)
    assert.equal(result.option.calendar.range, '2026')
    assert.equal(result.details.get(0)[0], samples[1])
  }
  finally { instance.dispose() }
})

test('waterfall accumulates signed changes across zero and drills down to the category', () => {
  const input = [{ category: 'A', value: 5, group: 'x' }, { category: 'B', value: -8, group: 'x' }, { category: 'B', value: -1, group: 'y' }]
  const chart = { ...defaultQueryChart(['category', 'value', 'group']), type: 'waterfall', metric: 'value', series: 'group', aggregation: 'sum' }
  const prepared = prepareQueryChart(chart, aggregateQueryChart(input, chart))
  assert.deepEqual(prepared.option.series[0].data.map(point => point.value), [[0, 0, 5, 5], [1, 5, -4, -9]])
  assert.deepEqual(prepared.details.get(1), input.slice(1))
})

test('specialized data reports incomplete mappings, invalid maps, and cyclic Sankey edges', () => {
  const prepare = (type, change, input = rows) => {
    const chart = { ...options, ...change, type }
    return prepareQueryChart(chart, aggregateQueryChart(input, chart))
  }
  assert.equal(prepare('parallel', { parallelFields: ['open'] }).issue, 'missingFields')
  assert.equal(prepare('map', { mapGeoJson: '{}' }).issue, 'invalidMap')
  assert.equal(prepare('map', { category: 'group' }).issue, 'mapMismatch')
  const cycle = [{ ...rows[0], region: 'A', group: 'B' }, { ...rows[1], region: 'B', group: 'A' }]
  assert.equal(prepare('sankey', {}, cycle).issue, 'cycle')
  const snapshot = { queryChart: { version: 1, options }, headers, columns: rows }
  const widget = { ...dashboardWidget('graph', 'graph', 'Graph'), field: 'group' }
  assert.equal(dashboardChartMappings(widget, snapshot).series, 'group')
})

test('percent stacks use per-category totals, including explicit zero values', () => {
  const input = [
    { region: 'A', group: 'C', value: 3 }, { region: 'A', group: 'D', value: 7 },
    { region: 'B', group: 'C', value: 0 }, { region: 'B', group: 'D', value: null },
  ]
  const chart = { ...defaultQueryChart(['region', 'group', 'value']), type: 'percentStackedBar',
    category: 'region', series: 'group', metric: 'value', aggregation: 'sum', nulls: 'keep' }
  const { points, details } = aggregateQueryChart(input, chart)
  const shares = queryChartCategoryShares(points)
  assert.deepEqual(points.map(point => shares.get(point.pointId)), [.3, .7, 0, undefined])
  assert.equal(details.get(points[1].pointId)[0], input[1])
})

test('bubble charts map three numeric fields and retain the underlying records', () => {
  const fields = ['revenue', 'cost', 'weight']
  const input = [{ revenue: 10, cost: 2, weight: 1 }, { revenue: 15, cost: 6, weight: 4 }, { revenue: 9, cost: 1, weight: -1 }]
  const chart = { ...defaultQueryChart(fields), type: 'bubble', ...suggestChartFields('bubble', fields) }
  const prepared = prepareQueryChart(chart, aggregateQueryChart(input, chart))
  const series = prepared.option.series[0]
  assert.equal(series.type, 'scatter')
  assert.deepEqual(series.data.map(point => point.value), [[10, 2, 1], [15, 6, 4]])
  assert.equal(series.symbolSize(series.data[1].value), 2 * series.symbolSize(series.data[0].value))
  assert.equal(prepared.details.get(series.data[1].pointId)[0], input[1])
  const instance = echarts.init(null, undefined, { renderer: 'svg', ssr: true, width: 540, height: 380 })
  try {
    instance.setOption({ animation: false, ...prepared.option })
    assert.match(instance.renderToSVGString(), /<path\b/)
  }
  finally { instance.dispose() }
})
