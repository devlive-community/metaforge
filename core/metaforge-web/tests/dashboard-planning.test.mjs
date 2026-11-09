import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const model = name => sourceModule('../../metaforge-web/src/model/' + name + '.ts')
const { dashboardAgenda, dashboardCalendarDays, dashboardDateAfter, dashboardGroupCompletion, dashboardOnTimeCompletion, dashboardOverdueAging,
  dashboardPlanMonth, dashboardPlanTasks, dashboardShiftPlanMonth, dashboardTaskBoard } = await model('dashboard-planning')
const { dashboardPlanningSettings, dashboardPlanningTools, dashboardWidget, dashboardWidgetLabelKey, emptyDashboard, readDashboardDocument } = await model('dashboard-layout')
const { screenPreview } = await model('screen-preview')

const settings = dashboardPlanningSettings({ statusField: 'status', labelField: 'name', ownerField: 'owner', groupField: 'team',
  completedField: 'finished', doneValues: 'done，已验收' })

test('status board groups original records and orders visible cards by valid deadlines', () => {
  const rows = [
    { status: 'open', name: 'A', due: '2026-11-09' },
    { status: 'open', name: 'B', due: '2026-11-03Tbroken' },
    { status: 'open', name: 'C', due: '2026-11-07' },
    { status: ' Done ', name: 'D', owner: 0, due: '2026-11-07' },
    { status: '已验收', name: 'E', due: '2026-02-30' },
    { status: null, name: 'F' },
  ]
  const tasks = dashboardPlanTasks(rows, 'due', settings)
  assert.equal(tasks[3].done, true)
  assert.equal(tasks[3].owner, '0')
  assert.equal(tasks[4].done, true)
  assert.equal(tasks[4].due, '')
  const result = dashboardTaskBoard(tasks, 2, 2)
  assert.equal(result.totalGroups, 4)
  assert.equal(result.columns[0].tasks.length, 3)
  assert.deepEqual(result.columns[0].cards.map(task => task.name), ['C', 'A'])
  assert.equal(result.columns[0].cards[0].row, rows[2])
})

test('agenda includes today and the final day while completed and past tasks follow separate rules', () => {
  const tasks = dashboardPlanTasks([
    { due: '2026-11-06', status: 'open' }, { due: '2026-11-07', status: 'open' },
    { due: '2026-11-08', status: 'done' }, { due: '2026-11-09', status: 'open' }, { due: '2026-11-10', status: 'open' },
  ], 'due', settings)
  const result = dashboardAgenda(tasks, '2026-11-07', 3, false, 1)
  assert.equal(result.last, '2026-11-09')
  assert.equal(result.total, 2)
  assert.deepEqual(result.days.map(day => day.name), ['2026-11-07'])
  assert.equal(dashboardAgenda(tasks, '2026-11-07', 3, true, 10).total, 3)
})

test('overdue age separates calendar day boundaries and excludes completed and due-today records', () => {
  const today = '2026-11-07'
  const rows = [0, 1, 7, 8, 30, 31, 90, 91].map(age => ({ due: dashboardDateAfter(today, -age), status: 'open' }))
  rows.push({ due: '2026-10-01', status: '已验收' }, { due: null, status: 'open' })
  const result = dashboardOverdueAging(dashboardPlanTasks(rows, 'due', settings), today)
  assert.deepEqual(result.buckets.map(bucket => bucket.length), [2, 2, 2, 1])
  assert.equal(result.total, 7)
})

test('completion uses status while on-time completion uses both dates and includes the due day', () => {
  const rows = [
    { team: 'A', status: 'done', due: '2026-11-07', finished: '2026-11-07T23:00:00-05:00' },
    { team: 'A', status: 'done', due: '2026-11-07', finished: '2026-11-08' },
    { team: 'A', status: 'open', due: '2026-11-05' },
    { team: 'A', status: 'done', finished: '2026-11-07' },
  ]
  const tasks = dashboardPlanTasks(rows, 'due', settings)
  const [completion] = dashboardGroupCompletion(tasks)
  const [onTime] = dashboardOnTimeCompletion(tasks)
  assert.equal(completion.tasks.length, 4)
  assert.equal(completion.completed.length, 3)
  assert.equal(onTime.tasks.length, 2)
  assert.equal(onTime.onTime[0].row, rows[0])
  assert.equal(onTime.late[0].row, rows[1])
})

test('calendar aligns Monday to Sunday, shows leap days and starts at the closest populated month', () => {
  const rows = [{ due: '2024-02-29' }, { due: '2024-02-29T18:00:00Z' }, { due: '2024-03-01' }]
  const tasks = dashboardPlanTasks(rows, 'due', settings)
  const grid = dashboardCalendarDays(tasks, '2024-02')
  assert.equal(grid[0].date, '2024-01-29')
  assert.equal(grid.length, 42)
  assert.equal(grid.find(day => day.date === '2024-02-29').tasks.length, 2)
  assert.equal(grid.find(day => day.date === '2024-03-01').inMonth, false)
  assert.equal(dashboardPlanMonth(tasks, '2024-04-07'), '2024-03')
  assert.equal(dashboardShiftPlanMonth('2026-12', 1), '2027-01')
})

test('planning settings and actions survive saves and date-sensitive widgets stay static in thumbnails', async () => {
  const languages = await Promise.all(['en', 'zh-cn'].map(locale => readFile(new URL('../src/i18n/locales/' + locale + '.json', import.meta.url), 'utf8').then(JSON.parse)))
  const design = emptyDashboard('Planning')
  design.boards.dashboard.widgets = dashboardPlanningTools.map(kind => {
    for (const language of languages) assert.ok(language[dashboardWidgetLabelKey(kind)])
    return { ...dashboardWidget(kind, kind, kind), reportCode: 'tasks', timeField: 'due',
      planningOptions: { ...settings, horizonDays: 999, includeCompleted: true } }
  })
  const configure = JSON.stringify({ version: 3, draft: design, published: design })
  const saved = readDashboardDocument(configure)
  for (const board of [saved.draft.boards.dashboard, saved.published.boards.dashboard]) {
    assert.deepEqual(board.widgets.map(widget => widget.kind), dashboardPlanningTools)
    assert.equal(board.widgets[0].planningOptions.doneValues, 'done，已验收')
    assert.equal(board.widgets[2].planningOptions.horizonDays, 90)
    assert.equal(board.widgets[2].planningOptions.includeCompleted, true)
    assert.equal(board.widgets[5].planningOptions.completedField, 'finished')
    assert.equal(board.widgets[0].action, 'filter')
  }
  const preview = screenPreview({ name: design.name, configure }).design.boards.dashboard.widgets
  assert.deepEqual(preview.slice(1, 4).map(widget => widget.kind), ['text', 'text', 'text'])
  assert.equal(preview[0].action, 'none')
})
