import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { aggregateQueryChart, createQueryChartSnapshot, defaultQueryChart } = await sourceModule('../../metaforge-web/src/model/query-chart.ts')

test('date groups use the selected timezone for instants and preserve SQL wall time', () => {
  const rows = [
    { at: '2026-10-25T23:15:00Z' }, { at: '2026-10-26T02:00:00Z' },
    { at: '2026-10-25 23:15:00' }, { at: '2026-10-25' }
  ]
  const options = defaultQueryChart(['at'], 'at')
  const result = aggregateQueryChart(rows, options)
  assert.deepEqual(result.points.map(({ category, value }) => [category, value]), [['2026-10-25', 2], ['2026-10-26', 2]])
  assert.deepEqual(result.details.get(result.points[1].pointId), rows.slice(0, 2))
})

test('filter groups combine independently and count only matching source rows', () => {
  const rows = [
    { day: 'A', state: 'SUCCESS', client: 'Chrome', score: 4 },
    { day: 'A', state: 'FAILED', client: 'Chrome', score: 12 },
    { day: 'B', state: 'SUCCESS', client: 'Firefox', score: 3 },
    { day: 'B', state: 'SUCCESS', client: 'Chrome', score: 7 }
  ]
  const options = defaultQueryChart(Object.keys(rows[0]))
  options.aggregation = 'sum'
  options.metric = 'score'
  options.filtersEnabled = true
  options.groups = [
    { join: 'and', rules: [{ field: 'state', operator: 'eq', value: 'SUCCESS', end: '' }, { field: 'client', operator: 'contains', value: 'chrome', end: '' }] },
    { join: 'and', rules: [{ field: 'score', operator: 'between', value: '10', end: '15' }] }
  ]
  const result = aggregateQueryChart(rows, options)
  assert.equal(result.rowCount, 3)
  assert.deepEqual(result.points.map(({ category, value }) => [category, value]), [['A', 16], ['B', 7]])
})

test('metric counts and numeric averages handle nulls without inventing data', () => {
  const rows = [{ category: 'A', value: 0 }, { category: 'A', value: null }, { category: 'A', value: '6' }]
  const options = defaultQueryChart(['category', 'value'])
  options.metric = 'value'
  assert.equal(aggregateQueryChart(rows, options).points[0].value, 2)
  options.aggregation = 'avg'
  assert.equal(aggregateQueryChart(rows, options).points[0].value, 3)
  options.nulls = 'zero'
  assert.equal(aggregateQueryChart(rows, options).points[0].value, 2)
})

test('serialized snapshots retain the preview and omit unselected fields and excess rows', () => {
  const rows = [
    { name: 'A', amount: 2, privateValue: 'excluded' },
    { name: 'A', amount: 4, privateValue: 'excluded' },
    { name: 'B', amount: 10, privateValue: 'excluded' }
  ]
  const options = defaultQueryChart(['name', 'amount'])
  options.metric = 'amount'
  options.aggregation = 'sum'
  options.maxRows = 2
  const snapshot = JSON.parse(JSON.stringify(createQueryChartSnapshot(options, rows)))
  assert.deepEqual(snapshot.columns, [{ name: 'A', amount: 2 }, { name: 'A', amount: 4 }])
  assert.deepEqual(aggregateQueryChart(snapshot.columns, snapshot.queryChart.options).points, aggregateQueryChart(rows, options).points)
})

test('date picker bounds include the entire ending day in the configured timezone', () => {
  const rows = [{ at: '2026-10-25T15:59:59Z' }, { at: '2026-10-25T16:00:00Z' }, { at: '2026-10-26 23:59:59' }, { at: '2026-10-27' }]
  const options = defaultQueryChart(['at'], 'at')
  options.filtersEnabled = true
  options.groups = [{ join: 'and', rules: [{ field: 'at', operator: 'between', value: '2026-10-26', end: '2026-10-26' }] }]
  const result = aggregateQueryChart(rows, options)
  assert.deepEqual(result.filtered, rows.slice(1, 3))
  assert.equal(result.points[0].value, 2)
})
