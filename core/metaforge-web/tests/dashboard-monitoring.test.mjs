import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = path => sourceModule('../../metaforge-web/src/model/' + path + '.ts')
const { dashboardFieldQuality, dashboardFreshness, dashboardPeriodComparison, dashboardSparklines } = await model('dashboard-monitoring')
const { dashboardFilteredReports } = await model('dashboard-filters')
const { dashboardWidget, dashboardTools, dashboardWidgetLabelKey, dashboardDataSettings, dashboardScreenEffectSettings, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

const kinds = ['dataQuality', 'periodComparison', 'dataFreshness', 'sparklineGrid', 'multiValueFilter', 'numberRangeFilter', 'valueStateFilter',
  'statusLegend', 'copyText', 'spotlightContainer', 'waterCaustics', 'lightTrails']

test('all new library entries localize and survive draft and published round trips', async () => {
  const locales = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Operations')
  for (const mode of ['wall', 'dashboard']) {
    design.boards[mode].widgets = kinds.map(kind => {
      assert.ok(dashboardTools.includes(kind))
      for (const locale of locales) assert.ok(locale[dashboardWidgetLabelKey(kind)], kind)
      const widget = dashboardWidget(kind, kind, kind, mode)
      if (kind === 'statusLegend') widget.links = [{ id: 'active', label: 'Active', url: '', tone: 'success' }, { id: 'invalid', label: 'Other', url: '', tone: 'custom' }]
      if (kind === 'numberRangeFilter') Object.assign(widget, { reportCode: 'report-2', field: 'latency' })
      if (kind === 'periodComparison' || kind === 'dataFreshness') widget.dataOptions = { ...dashboardDataSettings(), periodDays: 30, staleHours: 72 }
      return widget
    })
  }
  const doc = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const version of [doc.draft, doc.published]) for (const mode of ['wall', 'dashboard']) {
    const saved = new Map(version.boards[mode].widgets.map(widget => [widget.kind, widget]))
    assert.deepEqual([...saved.keys()], kinds)
    assert.deepEqual(saved.get('statusLegend').links.map(item => item.tone), ['success', 'default'])
    assert.equal(saved.get('periodComparison').dataOptions.periodDays, 30)
    assert.equal(saved.get('dataFreshness').dataOptions.staleHours, 72)
    assert.equal(saved.get('spotlightContainer').layoutOptions.columns, 3)
    assert.equal(saved.get('numberRangeFilter').field, 'latency')
    assert.equal(saved.get('numberRangeFilter').reportCode, 'report-2')
    for (const kind of ['waterCaustics', 'lightTrails']) {
      assert.equal(saved.get(kind).showCard, false)
      assert.ok(dashboardScreenEffectSettings(kind, { quantity: 9999 }).quantity <= 80)
    }
  }
})

test('field completeness counts zero and false, while null and whitespace are missing', () => {
  const rows = [{ a: 0, b: 1 }, { a: false, b: 1 }, { a: null }, { a: '   ' }]
  assert.deepEqual(dashboardFieldQuality(rows, ['a', 'b']), [
    { field: 'a', present: 2, missing: 2, distinct: 2, completeness: 50 },
    { field: 'b', present: 2, missing: 2, distinct: 1, completeness: 50 },
  ])
  assert.deepEqual(dashboardFieldQuality([], ['a']), [])
})

test('period comparisons use adjacent calendar windows and handle a zero or missing baseline', () => {
  const rows = [{ day: '2024-02-27', value: 0 }, { day: '2024-02-29 12:00:00', value: 5 }, { day: '2024-03-01', value: 7 }, { day: '2024-02-30', value: 50 }]
  assert.deepEqual(dashboardPeriodComparison(rows, 'day', 'value', 'sum', 2), {
    current: 12, previous: 0, difference: 12, percent: null,
    currentRange: ['2024-02-29', '2024-03-01'], previousRange: ['2024-02-27', '2024-02-28'],
  })
  const missing = dashboardPeriodComparison(rows.slice(1), 'day', 'value', 'sum', 2)
  assert.equal(missing.previous, null)
  assert.equal(missing.difference, null)
})

test('freshness uses the newest valid timestamp and distinguishes future or stale records', () => {
  const now = Date.parse('2026-11-07T20:00:00Z')
  const rows = [{ at: '2026-11-07T14:00:00Z' }, { at: '2026-02-30T14:00:00Z' }, { at: '2026-11-07T16:00:00Z' }, { at: null }]
  assert.deepEqual(dashboardFreshness(rows, 'at', now, 4), { latest: now - 4 * 3600000, age: 4, state: 'stale' })
  assert.equal(dashboardFreshness(rows, 'at', now, 5).state, 'fresh')
  assert.equal(dashboardFreshness([...rows, { at: '2026-11-08T16:00:00Z' }], 'at', now, 5).state, 'future')
  assert.equal(dashboardFreshness([{ at: '2026-02-30' }], 'at', now, 1), undefined)
})

test('trend cards align dates, keep gaps, and use one value scale across groups', () => {
  const rows = [{ day: '2026-11-04', group: 'A', val: 2 }, { day: '2026-11-06', group: 'A', val: 4 },
    { day: '2026-11-05', group: 'B', val: 0 }, { day: '2026-11-06', group: 'B', val: 3 }]
  assert.deepEqual(dashboardSparklines(rows, 'day', 'group', 'val', 'sum', 3, 2), {
    labels: ['2026-11-04', '2026-11-05', '2026-11-06'], bounds: { min: 0, max: 4 },
    items: [{ name: 'A', values: [2, null, 4], total: 6 }, { name: 'B', values: [null, 0, 3], total: 3 }],
  })
})

test('multi-select, numeric range and blank filtering compose without coercing false or blanks to zero', () => {
  const rows = [{ group: 'A', latency: 0, status: '' }, { group: 'B', latency: '-.25', status: null },
    { group: 'B', latency: 0.5, status: false }, { group: 'C', latency: 0, status: '' },
    { group: 'B', latency: false, status: '' }, { group: 'A', latency: ' ', status: '' }]
  const reportCode = 'ops'
  const filters = [
    { kind: 'multiValueFilter', reportCode, field: 'group', values: ['A', 'B'] },
    { kind: 'numberRangeFilter', reportCode, field: 'latency', min: -0.25, max: 0.5 },
    { kind: 'valueStateFilter', reportCode, field: 'status', presence: 'missing' },
  ]
  assert.deepEqual([...dashboardFilteredReports(filters, () => rows).get(reportCode)], rows.slice(0, 2))
  assert.deepEqual([...dashboardFilteredReports([...filters.slice(0, 2), { ...filters[2], presence: 'present' }], () => rows).get(reportCode)], [rows[2]])
  assert.equal(dashboardFilteredReports([{ ...filters[1], min: 2 }], () => rows).size, 0)
})

test('thumbnails retain the static legend but do not run clocks, filters, clipboard or canvas effects', async () => {
  const { screenPreview } = await model('screen-preview')
  const design = emptyDashboard('Snapshot')
  design.boards.dashboard.widgets = ['statusLegend', 'dataFreshness', 'sparklineGrid', 'numberRangeFilter', 'copyText', 'waterCaustics', 'lightTrails']
    .map(kind => dashboardWidget(kind, kind, kind))
  const preview = screenPreview({ name: design.name, configure: JSON.stringify({ version: 3, draft: design }) })
  assert.deepEqual(preview.design.boards.dashboard.widgets.map(widget => widget.kind), ['statusLegend', 'text', 'text', 'text', 'text'])
})
