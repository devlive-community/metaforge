import assert from 'node:assert/strict'
import test from 'node:test'
import * as echarts from 'echarts'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { aggregateQueryChart, defaultQueryChart, queryChartTypes } = await sourceModule('../../metaforge-web/src/model/query-chart.ts')
const { prepareQueryChart } = await sourceModule('../../metaforge-web/src/model/query-chart-series.ts')
const { dashboardTools, dashboardChartMappings, dashboardWidget } = await sourceModule('../../metaforge-web/src/model/dashboard-layout.ts')
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
    startX: 'startX', startY: 'startY', endX: 'endX', endY: 'endY' },
}

test('the dashboard includes every type shown in the visualizer', () => {
  assert.equal(new Set(dashboardTools).size, dashboardTools.length)
  for (const type of queryChartTypes) assert.ok(dashboardTools.includes(type), type)
})

test('specialized field mappings produce real ECharts series that can render SVG', () => {
  const types = ['heatmap', 'gauge', 'candlestick', 'boxplot', 'histogram', 'parallel', 'themeRiver', 'tree', 'treemap', 'sunburst', 'graph', 'sankey', 'map', 'lines', 'custom']
  for (const type of types) {
    const chartOptions = { ...options, type, category: type === 'themeRiver' ? 'date' : options.category }
    const prepared = prepareQueryChart(chartOptions, aggregateQueryChart(rows, chartOptions))
    assert.equal(prepared.issue, undefined, type)
    assert.equal(prepared.option.series[0].type, type === 'histogram' ? 'bar' : type)
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
