import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardIntervalBandBoundaries, dashboardIntervalCoverage, dashboardIntervalDurationBands, dashboardIntervalGroupCoverage,
  dashboardIntervalOverlaps, dashboardIntervalSweep, dashboardIntervalUnitMs, dashboardReadIntervals } = await model('dashboard-intervals')
const { dashboardIntervalTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

const HOUR = dashboardIntervalUnitMs.hours
const at = hour => new Date(Date.UTC(2026, 10, 8) + hour * HOUR).toISOString()
const row = (group, start, end) => ({ group, start: at(start), end: at(end) })
const hours = timestamp => (timestamp - Date.UTC(2026, 10, 8)) / HOUR

test('reading intervals rejects invalid, reversed and zero-length endpoints while preserving source rows', () => {
  const rows = [row('A', 0, 1), row('A', 2, 1), row('A', 2, 2), { group: 'B', start: 'bad', end: at(3) },
    { group: 'B', start: '2026-11-01T01:30:00-04:00', end: '2026-11-01T01:30:00-05:00' }]
  const result = dashboardReadIntervals(rows, 'start', 'end', 'group')
  assert.equal(result.invalid, 3)
  assert.equal(result.intervals[0].row, rows[0])
  assert.equal(result.intervals[1].end - result.intervals[1].start, HOUR)
  assert.equal(result.intervals[1].group, 'B')
})

test('sweep combines equal endpoints without introducing transient concurrency or dropping a boundary', () => {
  const { intervals } = dashboardReadIntervals([row('A', 9, 10), row('A', 10, 11), row('A', 9.5, 10.5)], 'start', 'end')
  assert.deepEqual(dashboardIntervalSweep(intervals).map(point => [hours(point.time), point.active]), [[9, 1], [9.5, 2], [10, 2], [10.5, 1], [11, 0]])
  assert.deepEqual(dashboardIntervalOverlaps(intervals, 2).map(item => [hours(item.start), hours(item.end), item.peak]), [[9.5, 10.5, 2]])
  assert.equal(dashboardIntervalOverlaps(intervals, 3).length, 0)
})

test('coverage merges nested and touching intervals and identifies each internal gap', () => {
  const { intervals } = dashboardReadIntervals([row('A', 9, 11), row('A', 10, 10.5), row('B', 11, 12), row('B', 13, 14), row('A', 15, 17)], 'start', 'end', 'group')
  const result = dashboardIntervalCoverage(intervals)
  assert.deepEqual([hours(result.first), hours(result.last), result.span / HOUR, result.covered / HOUR], [9, 17, 8, 6])
  assert.deepEqual(result.gaps.map(gap => [hours(gap.start), hours(gap.end)]), [[12, 13], [14, 15]])
  const groups = dashboardIntervalGroupCoverage(intervals, result.span)
  assert.deepEqual(groups.map(group => [group.group, group.covered / HOUR, group.share]), [['A', 4, .5], ['B', 2, .25]])
})

test('overlap thresholds apply within each group and split after concurrency drops', () => {
  const { intervals } = dashboardReadIntervals([
    row('A', 9, 11), row('A', 10, 12), row('A', 10.5, 11.5), row('A', 13, 15), row('A', 14, 16), row('B', 10, 11),
  ], 'start', 'end', 'group')
  assert.deepEqual(dashboardIntervalOverlaps(intervals, 2).map(item => [item.group, hours(item.start), hours(item.end), item.peak]),
    [['A', 10, 11.5, 3], ['A', 14, 15, 2]])
})

test('duration bands are positive and strictly increasing and include exact boundaries on the lower side', () => {
  const rows = [row('A', 0, .25), row('A', 0, .5), row('B', 0, 1), row('B', 0, 2)]
  const { intervals } = dashboardReadIntervals(rows, 'start', 'end')
  const boundaries = dashboardIntervalBandBoundaries('0.5,1,2', HOUR)
  assert.deepEqual(boundaries, [.5, 1, 2])
  assert.equal(dashboardIntervalBandBoundaries('0,1', HOUR), undefined)
  assert.equal(dashboardIntervalBandBoundaries('2,1', HOUR), undefined)
  const bands = dashboardIntervalDurationBands(intervals, boundaries, HOUR)
  assert.deepEqual(bands.map(band => band.rows.length), [1, 1, 1, 1])
  assert.equal(bands[3].rows[0], rows[3])
})

test('interval options and actions survive saves and localized names exist in both languages', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Intervals')
  for (const mode of ['dashboard', 'wall']) {
    design.boards[mode].widgets = dashboardIntervalTools.map(kind => {
      for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
      return { ...dashboardWidget(kind, kind, kind, mode), reportCode: 'events', timeField: 'start',
        intervalOptions: { endField: 'end', groupField: 'owner', unit: 'minutes', durationBands: '30, 60', minConcurrent: 999 } }
    })
  }
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [saved.draft, saved.published]) for (const mode of ['dashboard', 'wall']) {
    const widgets = version.boards[mode].widgets
    assert.deepEqual(widgets.map(widget => widget.kind), dashboardIntervalTools)
    assert.equal(widgets[2].intervalOptions.minConcurrent, 20)
    assert.equal(widgets[3].intervalOptions.unit, 'minutes')
    assert.equal(widgets[4].intervalOptions.groupField, 'owner')
    assert.equal(widgets[0].intervalOptions.endField, 'end')
    assert.equal(widgets[0].action, 'filter')
  }
})
