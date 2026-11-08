import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardDateCoverage, dashboardDuplicateKeys, dashboardFieldComparison, dashboardInvalidValues, dashboardMissingPatterns, dashboardValueFrequency } = await model('dashboard-audit')
const { dashboardAuditTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

test('duplicate keys require every key field and preserve the original rows for drill-down', () => {
  const rows = [
    { id: 0, region: 'East' }, { id: 0, region: 'East' }, { id: '0', region: 'East' },
    { id: 0, region: 'West' }, { id: 0, region: ' ' }, { region: 'East' },
  ]
  const result = dashboardDuplicateKeys(rows, ['id', 'region'])
  assert.equal(result.evaluated, 4)
  assert.equal(result.affected, 2)
  assert.equal(result.groups.length, 1)
  assert.deepEqual(result.groups[0].values, ['0', 'East'])
  assert.equal(result.groups[0].rows[0], rows[0])
  assert.equal(result.groups[0].rows[1], rows[1])
})

test('frequency and missing patterns group blanks without classifying zero or false as absent', () => {
  const rows = [{ a: false, b: 0 }, { a: 0, b: null }, { a: '', b: ' ' }, { a: null, b: null }, { b: 0 }]
  const frequency = dashboardValueFrequency(rows, 'a')
  assert.equal(frequency.distinct, 2)
  assert.deepEqual(frequency.groups.map(group => [group.values[0], group.rows.length]), [[null, 3], ['false', 1], ['0', 1]])
  const missing = dashboardMissingPatterns(rows, ['a', 'b'])
  assert.equal(missing.affected, 4)
  assert.deepEqual(missing.groups.map(group => [group.values, group.rows.length]), [[['a', 'b'], 2], [['b'], 1], [['a'], 1]])
})

test('field comparisons and validity checks distinguish case, types and invalid dates', () => {
  const compared = [{ a: 'ABC', b: 'abc' }, { a: false, b: 0 }, { a: 'x', b: null }, { a: '', b: ' ' }]
  assert.equal(dashboardFieldComparison(compared, 'a', 'b', false).mismatched, 3)
  const folded = dashboardFieldComparison(compared, 'a', 'b', true)
  assert.equal(folded.compared, 3)
  assert.deepEqual(folded.groups.map(group => group.values), [['false', '0'], ['x', null]])
  const numbers = [{ val: 0 }, { val: false }, { val: '2.5e-2' }, { val: '' }]
  assert.deepEqual(dashboardInvalidValues(numbers, 'val', 'number').groups.map(group => group.rows[0]), [numbers[1]])
  assert.equal(dashboardInvalidValues(numbers, 'val', 'boolean').invalid, 1)
  const dates = ['2026-10-31T08:30:00-04:00', '2026-11-03', '2026-11-06', '2026-11-06T12:00:00Z',
    '2026-11-08Tbroken', '2026-02-30'].map(date => ({ date }))
  assert.equal(dashboardInvalidValues(dates, 'date', 'date').invalid, 2)
  assert.deepEqual(dashboardDateCoverage(dates, 'date', 2), {
    gaps: [{ start: '2026-11-01', end: '2026-11-02', days: 2 }, { start: '2026-11-04', end: '2026-11-05', days: 2 }],
    observed: 3, expected: 7, first: '2026-10-31', last: '2026-11-06',
  })
  assert.equal(dashboardDateCoverage(dates, 'date', 3).gaps.length, 0)
})

test('audit widgets preserve rule and linked actions across draft and published saves', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Audit')
  design.boards.dashboard.widgets = dashboardAuditTools.map(kind => {
    for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
    return {
      ...dashboardWidget(kind, kind, kind),
      reportCode: 'orders',
      auditOptions: { fields: ['left', 'right', 'left'], rule: 'date', ignoreCase: true, minGapDays: 999 },
    }
  })
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const board of [saved.draft.boards.dashboard, saved.published.boards.dashboard]) {
    assert.deepEqual(board.widgets.map(widget => widget.kind), dashboardAuditTools)
    assert.deepEqual(board.widgets[0].auditOptions.fields, ['left', 'right'])
    assert.equal(board.widgets[3].auditOptions.ignoreCase, true)
    assert.equal(board.widgets[4].auditOptions.rule, 'date')
    assert.equal(board.widgets[5].auditOptions.minGapDays, 365)
    assert.equal(board.widgets[0].action, 'filter')
    assert.equal(board.widgets[5].action, 'saved')
  }
})
