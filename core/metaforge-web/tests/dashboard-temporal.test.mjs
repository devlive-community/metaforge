import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardDailySeries, dashboardEventIntervals, dashboardMonthlyComparison, dashboardTimeIntervals, dashboardWeekHours } = await model('dashboard-temporal')
const { dashboardTemporalTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

test('daily totals carry in earlier rows and rolling windows keep zero while skipping missing days', () => {
  const rows = [
    { at: '2026-10-01', n: 10 }, { at: '2026-10-03', n: 0 }, { at: '2026-10-05', n: 30 },
    { at: '2026-10-08', n: 'invalid' }, { at: '2026-10-10', n: -2 },
  ]
  const result = dashboardDailySeries(rows, 'at', 'n', 'sum', 7, 3)
  assert.deepEqual(result.points.map(point => [point.date, point.daily, point.cumulative, point.average]), [
    ['2026-10-04', null, 10, 0], ['2026-10-05', 30, 40, 15], ['2026-10-06', null, 40, 30],
    ['2026-10-07', null, 40, 30], ['2026-10-08', null, 40, null], ['2026-10-09', null, 40, null], ['2026-10-10', -2, 38, -2],
  ])
  assert.equal(result.buckets.get(result.points[1].day).rows[0], rows[2])
})

test('monthly comparison aligns the same calendar month and keeps an empty month distinct from zero', () => {
  const rows = [
    { at: '2025-02-01', n: 10 }, { at: '2026-02-02', n: 0 },
    { at: '2026-02-03', n: 4 }, { at: '2025-03-01', n: 0 }, { at: '2026-02-30', n: 99 },
  ]
  const result = dashboardMonthlyComparison(rows, 'at', 'n', 'sum', 0)
  assert.equal(result.year, 2026)
  assert.equal(result.months[1].current.value, 4)
  assert.equal(result.months[1].previous.value, 10)
  assert.equal(result.months[2].previous.value, 0)
  assert.equal(result.months[2].current, undefined)
  assert.equal(result.months[1].current.rows[0], rows[1])
})

test('weekday-hour buckets use the recorded clock and omit date-only values', () => {
  const rows = [
    { at: '2026-11-01T23:55:00-05:00', n: 3 }, { at: '2026-11-02T00:30:00+09:00', n: 0 },
    { at: '2026-11-02', n: 4 }, { at: '2026-11-02T00:45:00+09:00', n: 2 }, { at: '2026-11-02T00:50:bad', n: 7 },
  ]
  const result = dashboardWeekHours(rows, 'at', 'n', 'sum')
  assert.deepEqual(result.map(item => [item.weekday, item.hour, item.value, item.rows.length]), [[6, 23, 3, 1], [0, 0, 2, 2]])
  assert.equal(result[1].rows[1], rows[3])
})

test('intervals use actual elapsed time, deduplicate equal instants and reject reversed ranges', () => {
  const rows = [
    { entity: 'A', start: '2026-10-31T10:00:00Z', end: '2026-10-31T12:00:00Z' },
    { entity: 'A', start: '2026-10-31T06:00:00-04:00', end: '2026-10-31T06:00:00-04:00' },
    { entity: 'A', start: '2026-10-31T11:00:00Z', end: '2026-10-31T10:00:00Z' },
    { entity: 'A', start: '2026-10-31T14:00:00Z', end: null },
    { entity: 'B', start: '2026-10-31T15:00:00Z', end: '2026-10-31T16:00:00Z' },
  ]
  const [group] = dashboardEventIntervals(rows, 'start', 'entity')
  assert.equal(group.name, 'A')
  assert.equal(group.count, 2)
  assert.equal(group.median, 2 * 3_600_000)
  assert.equal(group.max, 3 * 3_600_000)
  assert.equal(group.rows.length, 4)
  const timeline = dashboardTimeIntervals(rows, 'start', 'end', 'entity', 2)
  assert.equal(timeline.total, 3)
  assert.equal(timeline.items[1].end - timeline.items[1].start, 0)
  assert.equal(timeline.last - timeline.first, 2 * 3_600_000)
  assert.equal(timeline.items[0].row, rows[0])
})

test('time-series fields, window options and linking survive save in both dashboard modes', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Temporal')
  for (const mode of ['dashboard', 'wall']) {
    design.boards[mode].widgets = dashboardTemporalTools.map(kind => {
      for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
      return { ...dashboardWidget(kind, kind, kind, mode), reportCode: 'events', timeField: 'started',
        temporalOptions: { windowDays: 999, historyDays: 60, year: 2026, endField: 'finished', labelField: 'service', unit: 'minutes' } }
    })
  }
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [saved.draft, saved.published]) for (const mode of ['dashboard', 'wall']) {
    const widgets = version.boards[mode].widgets
    assert.deepEqual(widgets.map(widget => widget.kind), dashboardTemporalTools)
    assert.equal(widgets[1].temporalOptions.windowDays, 90)
    assert.equal(widgets[2].temporalOptions.year, 2026)
    assert.equal(widgets[5].temporalOptions.endField, 'finished')
    assert.equal(widgets[5].temporalOptions.unit, 'minutes')
    assert.equal(widgets[5].timeField, 'started')
    assert.equal(widgets[5].action, 'filter')
  }
})
