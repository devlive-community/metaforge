import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardProcessCompliance, dashboardProcessCycles, dashboardProcessEndpoints, dashboardProcessPaths, dashboardProcessRework,
  dashboardProcessTraces, dashboardProcessWaits } = await model('dashboard-process')
const { dashboardProcessTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

const event = (id, stage, hour) => ({ id, stage, time: `2026-11-08T${String(hour).padStart(2, '0')}:00:00Z` })
const trace = rows => dashboardProcessTraces(rows, 'id', 'stage', 'time')
const stages = item => item.visits.map(visit => visit.stage)

test('traces retain source rows, collapse repeats and exclude entities with ambiguous timestamps', () => {
  const rows = [event(0, 'B', 3), event(0, 'A', 1), event(0, 'A', 2), event('0', 'B', 4),
    event('tie', 'A', 1), event('tie', 'B', 1), event('tie', 'C', 2), { id: 'invalid', stage: 'A', time: 'broken' }]
  const result = trace(rows)
  assert.equal(result.invalid, 1)
  assert.equal(result.ambiguous, 1)
  assert.deepEqual(result.traces.map(stages), [['A', 'B'], ['B']])
  assert.deepEqual(result.traces[0].visits[0].rows, [rows[1], rows[2]])
  assert.equal(result.traces[0].rows[2], rows[0])
})

test('path and endpoint counts use entities rather than event counts', () => {
  const rows = [event('a', 'A', 1), event('a', 'B', 2), event('a', 'B', 3),
    event('b', 'A', 1), event('b', 'B', 5), event('c', 'C', 4)]
  const { traces } = trace(rows)
  const [common] = dashboardProcessPaths(traces, 5)
  assert.deepEqual([common.stages, common.count], [['A', 'B'], 2])
  assert.deepEqual(common.rows, rows.slice(0, 5))
  const { entry, exit } = dashboardProcessEndpoints(traces, 5)
  assert.deepEqual(entry.map(item => [item.stage, item.count]), [['A', 2], ['C', 1]])
  assert.deepEqual(exit.map(item => [item.stage, item.count]), [['B', 2], ['C', 1]])
})

test('waiting durations use stage entry while cycles measure first to last event', () => {
  const rows = [
    event('a', 'A', 1), event('a', 'A', 2), event('a', 'B', 3), event('a', 'B', 5),
    event('b', 'A', 1), event('b', 'B', 5), event('c', 'B', 7),
  ]
  const { traces } = trace(rows)
  const hour = 3_600_000
  const [wait] = dashboardProcessWaits(traces, 5)
  const [cycle] = dashboardProcessCycles(traces, 5)
  assert.deepEqual([wait.from, wait.to, wait.count, wait.median / hour, wait.p90 / hour], ['A', 'B', 2, 3, 3.8])
  assert.deepEqual(wait.rows, rows.slice(0, 6))
  assert.deepEqual([cycle.stage, cycle.count, cycle.median / hour, cycle.p90 / hour], ['B', 2, 4, 4])
})

test('rework distinguishes affected entities from visits and compliance separates missing and unordered stages', () => {
  const rows = [
    event('good', 'A', 1), event('good', 'B', 2), event('good', 'A', 3), event('good', 'B', 4),
    event('good', 'A', 5), event('good', 'C', 6), event('unordered', 'B', 1), event('unordered', 'A', 2),
    event('unordered', 'C', 3), event('missing', 'A', 1), event('missing', 'C', 2),
  ]
  const { traces } = trace(rows)
  const revisits = dashboardProcessRework(traces, 5)
  assert.deepEqual(revisits.map(item => [item.stage, item.count, item.revisits]), [['A', 1, 2], ['B', 1, 1]])
  const results = dashboardProcessCompliance(traces, ['A', 'B', 'C'])
  assert.deepEqual(results.map(item => [item.label, item.count]), [['matched', 1], ['outOfOrder', 1], ['missing', 1]])
  assert.equal(results[1].rows[0], rows[6])
})

test('process configuration and translated widget names survive saves in both dashboard modes', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Process')
  for (const mode of ['dashboard', 'wall']) {
    design.boards[mode].widgets = dashboardProcessTools.map(kind => {
      for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
      return { ...dashboardWidget(kind, kind, kind, mode), reportCode: 'events', timeField: 'time',
        processOptions: { entityField: 'id', stageField: 'stage', requiredStages: ['B', 'A', 'B', 'C'], unit: 'minutes' } }
    })
  }
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [saved.draft, saved.published]) for (const mode of ['dashboard', 'wall']) {
    const widgets = version.boards[mode].widgets
    assert.deepEqual(widgets.map(widget => widget.kind), dashboardProcessTools)
    assert.deepEqual(widgets[5].processOptions.requiredStages, ['B', 'A', 'C'])
    assert.equal(widgets[3].processOptions.unit, 'minutes')
    assert.equal(widgets[3].timeField, 'time')
    assert.equal(widgets[0].action, 'filter')
  }
})
