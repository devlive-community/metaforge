import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardCorrelation, dashboardDifferenceBands, dashboardGroupComparison, dashboardQuadrants, dashboardRankMovement, dashboardShareMovement } = await model('dashboard-comparative')
const { dashboardComparativeTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')

test('correlation uses pairwise complete observations and omits constant or undersampled pairs', () => {
  const rows = [
    { x: 1, y: -2, flat: 4, sparse: 10 },
    { x: 2, y: -4, flat: 4, sparse: 20 },
    { x: 3, y: -6, flat: 4, sparse: null },
    { x: null, y: 10, flat: 4 },
  ]
  const matrix = dashboardCorrelation(rows, ['x', 'y', 'flat', 'sparse'], 3)
  assert.ok(Math.abs(matrix[0][1].coefficient + 1) < 1e-12)
  assert.equal(matrix[0][1], matrix[1][0])
  assert.equal(matrix[0][1].rows.length, 3)
  assert.equal(matrix[0][1].rows[0], rows[0])
  assert.equal(matrix[0][2].coefficient, null)
  assert.equal(matrix[0][3].coefficient, null)
  assert.equal(matrix[0][3].rows.length, 2)
  const large = dashboardCorrelation([{ a: -1e300, b: -1 }, { a: 0, b: 0 }, { a: 1e300, b: 1 }], ['a', 'b'], 3)
  assert.ok(Math.abs(large[0][1].coefficient - 1) < 1e-12)
})

test('quadrants use paired medians, keep zero thresholds and assign equality to the upper group', () => {
  const rows = [{ x: 0, y: 2 }, { x: 2, y: 0 }, { x: 1, y: 1 }, { x: null, y: 100 }]
  const automatic = dashboardQuadrants(rows, 'x', 'y', null, null)
  assert.deepEqual([automatic.x, automatic.y, automatic.total], [1, 1, 3])
  assert.deepEqual(automatic.quadrants, [[rows[0]], [rows[2]], [], [rows[1]]])
  assert.deepEqual(dashboardQuadrants(rows, 'x', 'y', 0, 0).quadrants.map(group => group.length), [0, 3, 0, 0])
})

test('ranking keeps ties and shares use only valid nonnegative pairs with independent totals', () => {
  const rows = [
    { group: 'A', current: 90, baseline: 60 }, { group: 'B', current: 60, baseline: 100 },
    { group: 'C', current: 90, baseline: 100 }, { group: 'A', current: 999, baseline: null },
    { group: 'D', current: -10, baseline: 40 },
  ]
  const ranks = dashboardRankMovement(rows, 'group', 'current', 'baseline')
  assert.deepEqual(ranks.map(item => [item.name, item.currentRank, item.baselineRank, item.movement]),
    [['A', 1, 3, 2], ['C', 1, 1, 0], ['B', 3, 1, -2], ['D', 4, 4, 0]])
  const shares = dashboardShareMovement(rows, 'group', 'current', 'baseline')
  assert.equal(shares.length, 3)
  assert.equal(shares.find(item => item.name === 'A').currentShare, 90 / 240)
  assert.equal(shares.find(item => item.name === 'A').baselineShare, 60 / 260)
  assert.equal(dashboardShareMovement([{ group: 'A', current: 0, baseline: 2 }], 'group', 'current', 'baseline')[0].movement, null)
})

test('difference and two-group distributions preserve source identity and include exact lower bounds', () => {
  const rows = [
    { group: 'A', current: 10, baseline: 20 }, { group: 'A', current: 20, baseline: 20 },
    { group: 'A', current: 30, baseline: 20 }, { group: 'B', current: 30, baseline: null },
    { group: 'B', current: false, baseline: 20 },
  ]
  const differences = dashboardDifferenceBands(rows, 'current', 'baseline', [-10, 0, 10])
  assert.equal(differences.total, 3)
  assert.deepEqual(differences.bands.map(band => band.rows.length), [0, 1, 1, 1])
  assert.equal(differences.bands[1].rows[0], rows[0])
  const groups = dashboardGroupComparison(rows, 'group', 'current', 'A', 'B', [20, 30])
  assert.deepEqual([groups.leftTotal, groups.rightTotal], [3, 1])
  assert.deepEqual(groups.bands.map(band => [band.rows.length, band.otherRows.length]), [[1, 0], [1, 0], [1, 1]])
  assert.equal(groups.bands[2].otherRows[0], rows[3])
})

test('comparison settings and linked actions survive draft and published saves', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Comparison')
  design.boards.dashboard.widgets = dashboardComparativeTools.map(kind => {
    for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
    return { ...dashboardWidget(kind, kind, kind), reportCode: 'sales', field: 'actual',
      comparativeOptions: { fields: ['actual', 'target', 'actual'], secondField: 'target', groupField: 'region', leftGroup: 'A', rightGroup: 'B',
        boundaries: '-10, 0, 10', xThreshold: 0, yThreshold: null, minSamples: 1000 } }
  })
  const saved = readDashboardDocument(JSON.stringify({ version: 3, draft: design, published: design }))
  for (const board of [saved.draft.boards.dashboard, saved.published.boards.dashboard]) {
    assert.deepEqual(board.widgets.map(widget => widget.kind), dashboardComparativeTools)
    const widget = board.widgets[0]
    assert.deepEqual(widget.comparativeOptions.fields, ['actual', 'target'])
    assert.equal(widget.comparativeOptions.minSamples, 100)
    assert.equal(widget.comparativeOptions.xThreshold, 0)
    assert.equal(widget.comparativeOptions.boundaries, '-10, 0, 10')
    assert.equal(widget.comparativeOptions.rightGroup, 'B')
    assert.equal(widget.action, 'filter')
  }
})
