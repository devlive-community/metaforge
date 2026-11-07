import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardBusinessTools, dashboardBusinessSettings, dashboardTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')
const { dashboardBandBoundaries, dashboardCohortRetention, dashboardPareto, dashboardRecencyDistribution, dashboardSegmentSummary, dashboardStageConversion,
  dashboardTargetGap, dashboardValueBands, dashboardVarianceDrivers } = await model('dashboard-business')

test('business widgets and their nested layout round-trip in the draft and published modes', async () => {
  const kinds = [...dashboardBusinessTools, 'accordionContainer', 'definitionList']
  const locales = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Business')
  for (const mode of ['wall', 'dashboard']) {
    design.boards[mode].widgets = kinds.map(kind => {
      assert.ok(dashboardTools.includes(kind))
      for (const locale of locales) assert.ok(locale[dashboardWidgetLabelKey(kind)])
      const widget = dashboardWidget(kind, kind, kind, mode)
      if (kind === 'stageConversion') Object.assign(widget, { reportCode: 'orders', businessOptions: { groupField: 'stage', entityField: 'customer', stageOrder: ['View', 'Order', 'Order', 'Pay'] } })
      if (kind === 'definitionList') Object.assign(widget, { parentId: 'accordionContainer', links: [{ id: 'rule', label: 'Source', text: 'Orders', url: '' }] })
      return widget
    })
  }
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [saved.draft, saved.published]) for (const mode of ['wall', 'dashboard']) {
    const widgets = version.boards[mode].widgets
    assert.deepEqual(widgets.map(widget => widget.kind), kinds)
    assert.deepEqual(widgets[0].businessOptions.stageOrder, ['View', 'Order', 'Pay'])
    assert.equal(widgets[0].businessOptions.entityField, 'customer')
    assert.equal(widgets[0].reportCode, 'orders')
    assert.equal(widgets.at(-1).parentId, 'accordionContainer')
    assert.equal(widgets.at(-1).links[0].text, 'Orders')
  }
  assert.equal(dashboardBusinessSettings({ periods: 99 }).periods, 12)
})

test('conversion cohorts exclude repeat and skipped-stage entities while retaining configured order', () => {
  const rows = [{ s: 'C', id: 'late' }, { s: 'C', id: 'returning' }, { s: 'A', id: 'returning' }, { s: 'A', id: 'returning' },
    { s: 'B', id: 'returning' }, { s: 'A', id: 'skipped' }, { s: 'C', id: 'skipped' }, { s: 'B', id: null }]
  assert.deepEqual(dashboardStageConversion(rows, 's', ['A', 'B', 'C'], '', 'id'), [
    { name: 'A', value: 2, observed: true, stepRate: null, overallRate: 100 },
    { name: 'B', value: 1, observed: true, stepRate: 50, overallRate: 50 },
    { name: 'C', value: 1, observed: true, stepRate: 100, overallRate: 50 },
  ])
  assert.equal(dashboardStageConversion(rows, 's', ['missing', 'A'])[1].stepRate, null)
  const [zero, missing] = dashboardStageConversion([{ s: 'A', v: 0 }, { s: 'B', v: null }], 's', ['A', 'B'], 'v')
  assert.deepEqual([zero.value, zero.observed, missing.value, missing.observed], [0, true, null, false])
})

test('target aggregation handles repeated targets and missing or zero baselines', () => {
  const rows = [{ group: 'A', actual: 60, target: 100 }, { group: 'A', actual: 50, target: 100 }, { group: 'B', actual: 0, target: 0 },
    { group: 'C', actual: null, target: 10 }]
  assert.deepEqual(dashboardTargetGap(rows, 'group', 'actual', 'target', 'max').map(({ name, gap, attainment }) => [name, gap, attainment === null ? null : Math.round(attainment)]),
    [['A', 10, 110], ['B', 0, null], ['C', null, null]])
  assert.equal(dashboardTargetGap(rows, 'group', 'actual', 'target', 'sum')[0].gap, -90)
})

test('subtotals use weighted raw records and the full grand total even when rows are limited', () => {
  const rows = [{ a: 'X', b: 'one', v: 2 }, { a: 'X', b: 'two', v: 10 }, { a: 'Y', b: 'one', v: 0 }, { a: 'X', b: 'one', v: 12 }]
  const { items, total } = dashboardSegmentSummary(rows, 'a', 'b', 'v', 'avg', 1)
  assert.equal(total.count, 4)
  assert.equal(total.value, 6)
  assert.deepEqual(items.map(({ name, type, value }) => [name, type, value]), [['X', 'group', 8], ['one', 'child', 7]])
})

test('Pareto marks the first threshold crossing and variance reconciles only paired measures', () => {
  const rows = [{ g: 'A', val: 70, base: 50 }, { g: 'B', val: 20, base: 30 }, { g: 'C', val: 10, base: 5 },
    { g: 'A', val: -25, base: null }, { g: 'D', val: null, base: 50 }]
  const pareto = dashboardPareto(rows, 'g', 'val', 80)
  assert.equal(pareto.excluded, 1)
  assert.deepEqual(pareto.items.map(item => [item.name, item.cumulative, item.reachesCutoff]), [['A', 70, false], ['B', 90, true], ['C', 100, false]])
  const drivers = dashboardVarianceDrivers(rows, 'g', 'val', 'base')
  assert.equal(drivers.samples, 3)
  assert.equal(drivers.net, 15)
  assert.equal(drivers.items.reduce((sum, item) => sum + item.delta, 0), drivers.net)
})

test('retention deduplicates activity, marks unobserved periods, and starts weeks on Mondays', () => {
  const rows = [{ u: 'a', day: '2026-11-01' }, { u: 'a', day: '2026-11-02' }, { u: 'a', day: '2026-11-02T01:00:00Z' },
    { u: 'b', day: '2026-11-01' }, { u: 'c', day: '2026-11-04' }]
  const daily = dashboardCohortRetention(rows, 'u', 'day', 'day', 5, 10)
  assert.deepEqual(daily[0].periods, [{ count: 1, percent: 100 }, null, null, null, null])
  assert.deepEqual(daily.at(-1).periods.map(value => value?.percent ?? null), [100, 50, 0, 0, null])
  const weekly = dashboardCohortRetention(rows, 'u', 'day', 'week', 3, 10)
  assert.deepEqual(weekly.map(row => row.name), ['2026-11-02', '2026-10-26'])
  assert.equal(weekly[1].periods[1].percent, 50)
})

test('recency uses each entity’s latest activity, and numeric bands include their lower boundary', () => {
  const recency = dashboardRecencyDistribution([
    { id: 'fresh', at: '2026-11-07' }, { id: 'fresh', at: '2026-04-01' }, { id: 'week', at: '2026-10-31' },
    { id: 'month', at: '2026-10-30' }, { id: 'quarter', at: '2026-10-02' }, { id: 'older', at: '2026-07-01' },
  ], 'id', 'at')
  assert.equal(recency.latest, '2026-11-07')
  assert.deepEqual(recency.buckets.map(item => item.count), [2, 1, 1, 1])
  const values = dashboardBandBoundaries('-2，0; 10')
  assert.deepEqual(values, [-2, 0, 10])
  assert.equal(dashboardBandBoundaries('0, 0, 10'), undefined)
  assert.equal(dashboardBandBoundaries('1, x'), undefined)
  const bands = dashboardValueBands([-3, -2, 0, 9, 10, null, false, ' '].map(val => ({ val })), 'val', values)
  assert.deepEqual(bands.items.map(item => item.count), [1, 1, 2, 1])
  assert.equal(bands.total, 5)
})
