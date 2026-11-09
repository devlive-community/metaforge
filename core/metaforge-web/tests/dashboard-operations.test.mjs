import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardOperationsSettings, dashboardOperationDataTools, dashboardWidget, dashboardWidgetLabelKey, dashboardTools, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')
const { dashboardEvents, dashboardMaintenance, dashboardOutliers, dashboardPercentiles, dashboardRecordChoices, dashboardRunSummary, dashboardSla } = await model('dashboard-operations')

test('operations and effect widgets can be saved and translated in both board modes', async () => {
  const kinds = [...dashboardOperationDataTools, 'inspectionChecklist', 'pulseGrid', 'contourLines']
  const locales = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Ops')
  for (const mode of ['dashboard', 'wall']) {
    design.boards[mode].widgets = kinds.map(kind => {
      assert.ok(dashboardTools.includes(kind))
      for (const locale of locales) assert.ok(locale[dashboardWidgetLabelKey(kind)], kind)
      const widget = dashboardWidget(kind, kind, kind, mode)
      if (kind === 'slaScorecard') Object.assign(widget, { reportCode: 'r', field: 'latency', operationsOptions: { comparator: 'atMost', threshold: 25.75, goal: 99.5 } })
      if (kind === 'maintenanceWindow') Object.assign(widget, { timeField: 'starts', operationsOptions: { endField: 'ends' } })
      if (kind === 'inspectionChecklist') widget.links = [{ id: 'a', label: 'Check', text: 'Verify totals', url: '' }]
      return widget
    })
  }
  const document = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [document.draft, document.published]) for (const mode of ['dashboard', 'wall']) {
    const widgets = new Map(version.boards[mode].widgets.map(widget => [widget.kind, widget]))
    assert.deepEqual([...widgets.keys()], kinds)
    assert.equal(widgets.get('slaScorecard').operationsOptions.goal, 99.5)
    assert.equal(widgets.get('slaScorecard').operationsOptions.threshold, 25.75)
    assert.equal(widgets.get('slaScorecard').reportCode, 'r')
    assert.equal(widgets.get('maintenanceWindow').timeField, 'starts')
    assert.equal(widgets.get('maintenanceWindow').operationsOptions.endField, 'ends')
    assert.equal(widgets.get('inspectionChecklist').links[0].text, 'Verify totals')
    assert.equal(widgets.get('pulseGrid').movable, false)
    assert.equal(widgets.get('contourLines').showCard, false)
  }
  assert.equal(dashboardOperationsSettings('slaScorecard', { goal: 200, comparator: 'not-supported' }).comparator, 'atMost')
  assert.equal(dashboardOperationsSettings('slaScorecard', { goal: 200 }).goal, 100)
})

test('exception severity uses the selected inclusive comparison and limits visible rows, not the total', () => {
  const rows = [{ val: 10 }, { val: 12 }, { val: -1 }, { val: false }, { val: ' ' }, { val: '10' }]
  const high = dashboardOutliers(rows, 'val', 10, 'atLeast', 2)
  assert.equal(high.samples, 4)
  assert.equal(high.total, 3)
  assert.deepEqual(high.items.map(item => [item.index, item.distance]), [[1, 2], [0, 0]])
  assert.deepEqual(dashboardOutliers(rows, 'val', 0, 'below', 5).items.map(item => item.value), [-1])
})

test('SLA and percentiles use only valid numeric samples, including zero', () => {
  const rows = [{ n: null }, { n: 0 }, { n: 10 }, { n: '20' }, { n: false }]
  assert.deepEqual(dashboardSla(rows, 'n', 10, 'atMost'), { total: 3, passed: 2, breached: 1, percent: 2 / 3 * 100 })
  assert.equal(dashboardSla([{ n: false }], 'n', 10, 'above'), undefined)
  const percentiles = dashboardPercentiles(rows, 'n')
  assert.equal(percentiles.count, 3)
  assert.deepEqual(percentiles.quantiles, [{ percentile: 50, value: 10 }, { percentile: 90, value: 18 }, { percentile: 95, value: 19 }, { percentile: 99, value: 19.8 }])
})

test('running events sort newest first and unknown statuses end a success streak', () => {
  const rows = [
    { at: '2026-11-01T02:00:00Z', status: ' success ' }, { at: '2026-11-01T04:00:00Z', status: '成功' },
    { at: '2026-11-01T03:00:00Z', status: 'running' }, { at: '2026-11-01T00:00:00Z', status: 'ERROR' },
    { at: '2026-02-30T23:00:00Z', status: 'error' },
  ]
  const events = dashboardEvents(rows, 'at', dashboardOperationsSettings('runHistory', { statusField: 'status' }), 10)
  assert.deepEqual(events.map(event => event.index), [1, 2, 0, 3])
  assert.deepEqual(dashboardRunSummary(events), { healthy: 2, failed: 1, unknown: 1, streak: 1 })
})

test('maintenance windows use inclusive start and exclusive end with active windows first', () => {
  const now = Date.parse('2026-11-03T04:00:00Z')
  const rows = [
    { start: '2026-11-03T05:00:00Z', end: '2026-11-03T06:00:00Z' },
    { start: '2026-11-02T01:00:00Z', end: '2026-11-03T04:00:00Z' },
    { start: '2026-11-03T00:00:00-04:00', end: '2026-11-03T05:00:00Z' },
    { start: '2026-11-03T05:00:00Z', end: '2026-11-03T01:00:00Z' },
  ]
  const windows = dashboardMaintenance(rows, 'start', dashboardOperationsSettings('maintenanceWindow', { endField: 'end' }), now, 10)
  assert.deepEqual(windows.map(window => [window.index, window.active]), [[2, true], [0, false]])
})

test('record lookup searches configured fields across the snapshot before limiting visible results', () => {
  const rows = [{ name: 'First', extra: 'pending', private: 'needle' }, { name: 'Second', extra: ' The Needle ' }, { name: 'Third', extra: 'needle' }]
  assert.deepEqual(dashboardRecordChoices(rows, 'name', ['extra'], 'NEEDLE', 1).map(record => record.index), [1])
  assert.equal(dashboardRecordChoices(rows, 'name', ['extra'], 'second', 10)[0].row, rows[1])
})
