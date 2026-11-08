import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardCooccurrences, dashboardCrossTab, dashboardGroupSpread, dashboardTransitions } = await model('dashboard-relations')
const { dashboardFilteredReports } = await model('dashboard-filters')
const { dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')
const { screenPreview } = await model('screen-preview')

test('cross-tab distinguishes missing cells from zero and preserves rows for linked filters', () => {
  const rows = [
    { group: 'A', channel: 'web', value: 0 }, { group: 'A', channel: 'web', value: 4 },
    { group: 'B', channel: 'web', value: 0 }, { group: 'B', channel: 'app', value: null },
    { group: 'C', channel: 'app', value: -3 }, { group: 'D', channel: 'app', value: false },
  ]
  const result = dashboardCrossTab(rows, 'group', 'channel', 'value', 'avg', 3)
  assert.deepEqual(result.columns, ['web', 'app'])
  assert.deepEqual(result.rows.map(row => row.cells.map(cell => cell?.value ?? null)), [[2, null], [0, null], [null, -3]])
  assert.equal(result.total, 4)
  assert.equal(result.rows[1].cells[0].sources[0], rows[2])
  assert.equal(dashboardCrossTab(rows, 'group', 'channel', 'value', 'count', 2).total, 6)
})

test('state changes sort by time, collapse repeats and never bridge an ambiguous timestamp', () => {
  const rows = [
    { id: 'u', state: 'Paid', time: '2026-11-03T09:00:00Z' },
    { id: 'u', state: 'New', time: '2026-11-03T07:00:00Z' },
    { id: 'u', state: 'New', time: '2026-11-03T08:00:00Z' },
    { id: 'u', state: 'Sent', time: '2026-11-03T10:00:00Z' },
    { id: 'u', state: 'Cancelled', time: '2026-11-03T10:00:00Z' },
    { id: 'u', state: 'Closed', time: '2026-11-03T11:00:00Z' },
  ]
  const result = dashboardTransitions(rows, 'id', 'state', 'time', 8)
  assert.equal(result.total, 1)
  assert.deepEqual(result.columns, ['Paid'])
  assert.equal(result.rows[0].label, 'New')
  assert.equal(result.rows[0].cells[0].value, 1)
  assert.equal(result.rows[0].cells[0].sources[0], rows[2])
  assert.equal(result.rows[0].cells[0].sources[1], rows[0])
})

test('co-occurrence counts each entity once and restricts candidates before pairing', () => {
  const rows = [
    { id: 1, item: 'A' }, { id: 1, item: 'A' }, { id: 1, item: 'B' },
    { id: 2, item: 'A' }, { id: 2, item: 'B' }, { id: 3, item: 'C' },
  ]
  const result = dashboardCooccurrences(rows, 'id', 'item', 2, 5)
  assert.equal(result.entities, 3)
  assert.equal(result.limited, true)
  assert.equal(result.items.length, 1)
  assert.equal(result.items[0].count, 2)
  assert.equal(Math.round(result.items[0].share), 67)
  assert.deepEqual(result.items[0].sources, rows.slice(0, 5))
  const spread = dashboardGroupSpread([0, 2, 4, 6, false, null].map(value => ({ group: 'A', value })), 'group', 'value', 10)[0]
  assert.deepEqual([spread.count, spread.min, spread.q1, spread.median, spread.q3, spread.max], [4, 0, 1.5, 3, 4.5, 6])
})

test('quick dates include the latest seven days before other filters and distinguish false from blanks', () => {
  const rows = [
    { date: '2026-10-19', active: false }, { date: '2026-10-20', active: '0' },
    { date: '2026-10-24', active: 0 }, { date: '2026-10-25', active: '' },
    { date: '2026-10-26T23:00:00Z', active: true }, { date: '2026-10-29', active: null },
    { date: '2026-02-30', active: false },
  ]
  const filters = [
    { kind: 'relativeDateFilter', reportCode: 'orders', field: 'date', days: 7 },
    { kind: 'booleanFilter', reportCode: 'orders', field: 'active', checked: false },
  ]
  let reads = 0
  const result = dashboardFilteredReports(filters, () => { reads++; return rows })
  assert.equal(reads, 1)
  assert.deepEqual([...result.get('orders')], [rows[2]])
  assert.deepEqual([...dashboardFilteredReports(filters, () => rows.slice(0, 5)).get('orders')], [rows[1], rows[2]])
  assert.deepEqual([...dashboardFilteredReports(filters.slice(1), () => rows).get('orders')], [rows[0], rows[1], rows[2], rows[6]])
})

test('relation options, new filters and grid children survive save and static previews disable linked clicks', async () => {
  const kinds = ['crossTabHeatmap', 'transitionMatrix', 'cooccurrencePairs', 'groupSpread', 'relativeDateFilter', 'booleanFilter', 'pagedGridContainer']
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Relations')
  for (const mode of ['dashboard', 'wall']) {
    design.boards[mode].widgets = kinds.map(kind => {
      for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
      const widget = dashboardWidget(kind, kind, kind, mode)
      if (kind === 'crossTabHeatmap') {
        widget.parentId = 'pagedGridContainer'
        widget.relationOptions = { rowField: 'region', columnField: 'channel', dimensionLimit: 999 }
      }
      if (kind === 'booleanFilter') widget.field = 'active'
      if (kind === 'pagedGridContainer') widget.layoutOptions.pageSize = 4
      return widget
    })
  }
  const configure = JSON.stringify({ version: 3, draft: design, published: design })
  const document = readDashboardDocument(configure)
  for (const mode of ['dashboard', 'wall']) {
    const widgets = document.draft.boards[mode].widgets
    assert.deepEqual(widgets.map(widget => widget.kind), kinds)
    assert.equal(widgets[0].parentId, 'pagedGridContainer')
    assert.equal(widgets[0].action, 'filter')
    assert.equal(widgets[0].relationOptions.dimensionLimit, 16)
    assert.equal(widgets[5].field, 'active')
    assert.equal(widgets[6].layoutOptions.pageSize, 4)
  }
  assert.equal(screenPreview({ name: design.name, configure }).design.boards.dashboard.widgets[0].action, 'none')
})
