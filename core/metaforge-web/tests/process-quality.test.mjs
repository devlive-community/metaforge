import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const load = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { processQualityViews, qualityControlSeries, qualitySignal, qualitySpecificationGroups,
  qualitySpecificationsValid, readProcessQualityOptions } = await load('process-quality')
const { dashboardWidget, emptyDashboard, readDashboardDocument } = await load('dashboard-layout')

test('control charts preserve gaps and link moving ranges to both observations', () => {
  const rows = [
    { time: '2026-11-08T11:00:00Z', value: 200 },
    { time: '2026-11-08T08:00:00Z', value: '10' },
    { time: '2026-11-08T10:00:00Z', value: null },
    { time: '2026-11-08T09:00:00Z', value: 12 },
    { time: 'invalid', value: 10000 },
  ]
  const series = qualityControlSeries(rows, 'value', { field: 'time', type: 'date' })
  assert.deepEqual(series.run.points.map(point => point?.value), [10, 12, undefined, 200])
  assert.deepEqual(series.run.limits, { center: 12 })
  assert.deepEqual(series.moving.points.map(point => point?.value), [undefined, 2, undefined, undefined])
  assert.deepEqual(series.moving.points[1].rows, [rows[1], rows[3]])
  assert.equal(series.moving.points[1].rows[0], rows[1])
  assert.equal(series.moving.limits.upper, 6.534)
  assert.equal(series.invalid, 2)
  assert.equal(qualitySignal(200, series.individuals.limits), 'above')
  assert.equal(qualitySignal(series.individuals.limits.upper, series.individuals.limits), undefined)
  assert.equal(qualityControlSeries([{ value: 0 }, { value: '' }, { value: 3 }], 'value').individuals.limits, undefined)
})

test('specification limits are inclusive, with invalid measurements separated from rejected values', () => {
  const options = readProcessQualityOptions({ measure: 'value', group: 'team', lower: '0', upper: 3 })
  const rows = [{ value: 0, team: false }, { value: '3', team: false }, { value: 4, team: false },
    { value: -1, team: 0 }, { value: true, team: 0 }, { value: ' ', team: '' }]
  const groups = qualitySpecificationGroups(rows, options, true)
  assert.deepEqual(groups.get('false'), { pass: rows.slice(0, 2), below: [], above: [rows[2]], invalid: [] })
  assert.deepEqual(groups.get('0'), { pass: [], below: [rows[3]], above: [], invalid: [rows[4]] })
  assert.deepEqual(groups.get('').invalid, [rows[5]])
  assert.equal(qualitySpecificationsValid(0, 0), true)
  assert.equal(qualitySpecificationsValid(undefined, 0), true)
  assert.equal(qualitySpecificationsValid(4, 3), false)
  assert.equal(qualitySpecificationsValid(), false)
})

test('all process quality components and their mappings survive reopening a saved screen', () => {
  const draft = emptyDashboard()
  const options = readProcessQualityOptions({ measure: 'weight', order: { field: 'time', type: 'date' }, group: 'lot', lower: 0, upper: 5.5 })
  draft.boards.dashboard.widgets = processQualityViews.map(kind => ({
    ...dashboardWidget(kind, kind, kind), reportCode: 'quality-report', processQualityOptions: options,
  }))
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard.widgets
  assert.deepEqual(saved.map(item => item.kind), processQualityViews)
  for (const widget of saved) {
    assert.deepEqual(widget.processQualityOptions, options)
    assert.equal(widget.reportCode, 'quality-report')
    assert.equal(widget.action, 'filter')
  }
})
