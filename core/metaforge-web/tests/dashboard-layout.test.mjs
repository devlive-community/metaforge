import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const {
  constrainDashboardGroup, constrainDashboardRect, dashboardAnimationEffects, dashboardChartMappings, dashboardChartType, dashboardDeadlineTime, dashboardGridTarget, dashboardGroupBounds, dashboardMetric, dashboardOverlap, dashboardPivot, dashboardPlacement, dashboardReportCodes,
  dashboardResourceUrl, dashboardWallResolution,
  dashboardCanContain, dashboardContentSettings, dashboardDataSettings, dashboardDescendantIds, dashboardLayoutSettings,
  dashboardRowsInPeriod, dashboardScreenEffectSettings, dashboardTools, dashboardWidget, dashboardWidgetLabelKey, dashboardWidgetUsesAsset, emptyDashboard, groupDashboardWidgets, readDashboardDocument,
  reorderDashboardLayers, resizeDashboardGroup, switchDashboardLayout
} = await sourceModule('../../metaforge-web/src/model/dashboard-layout.ts')

test('old dashboard layouts keep their linked chart and grid geometry', () => {
  const layout = [{ i: 'old-id', x: 2, y: 3, w: 5, h: 4, title: 'Trend', node: { id: 'CHART' } }]
  const document = readDashboardDocument(JSON.stringify(layout), 'Legacy dashboard')
  const [widget] = document.draft.boards.dashboard.widgets
  assert.deepEqual([widget.reportCode, widget.name, widget.x, widget.y, widget.w, widget.h], ['CHART', 'Trend', 2, 3, 5, 4])
  assert.equal(document.published.name, 'Legacy dashboard')
  assert.deepEqual(dashboardReportCodes(document), ['CHART'])
})

test('draft edits keep the published layout and retain both sets of chart links', () => {
  const draft = emptyDashboard('Edited')
  const published = emptyDashboard('Published')
  draft.boards.dashboard.widgets.push({ ...dashboardWidget('new', 'report', 'New chart'), reportCode: 'NEW' })
  published.boards.wall.widgets.push({ ...dashboardWidget('old', 'report', 'Old chart'), reportCode: 'OLD', w: 500, h: 300 })
  published.mode = 'wall'
  const document = readDashboardDocument(JSON.stringify({ version: 3, draft, published }))
  assert.equal(document.draft.name, 'Edited')
  assert.equal(document.published.mode, 'wall')
  assert.deepEqual(dashboardReportCodes(document), ['NEW', 'OLD'])
})

test('switching layouts transfers missing components in both directions and preserves existing geometry', () => {
  const design = emptyDashboard()
  const { dashboard, wall } = design.boards
  dashboard.widgets.push({ ...dashboardWidget('trend', 'line', 'Trend'), x: 3, y: 0, w: 6, h: 4, reportCode: 'CHART', showCard: false })
  assert.equal(switchDashboardLayout(design), 0)
  assert.equal(design.mode, 'wall')
  assert.deepEqual(wall.widgets.map(({ id, x, y, w, h, reportCode, showCard }) => [id, x, y, w, h, reportCode, showCard]),
    [['trend', 480, 0, 960, 270, 'CHART', false]])
  wall.widgets[0].x = 160
  wall.widgets.push({ ...dashboardWidget('clock', 'clock', 'Clock', 'wall'), x: 1600, y: 64, w: 240, h: 180 })
  assert.equal(switchDashboardLayout(design), 0)
  assert.equal(design.mode, 'dashboard')
  assert.deepEqual(dashboard.widgets.map(widget => widget.id), ['trend', 'clock'])
  assert.deepEqual(dashboard.widgets.map(widget => widget.x), [3, 10])
  assert.equal(dashboardOverlap(...dashboard.widgets), false)
  assert.equal(switchDashboardLayout(design), 0)
  assert.deepEqual(wall.widgets.map(widget => widget.id), ['trend', 'clock'])
  assert.deepEqual(wall.widgets.map(widget => widget.x), [160, 1600])
})

test('existing widgets in a destination layout keep their sizes and positions during transfer', () => {
  const design = emptyDashboard()
  const { dashboard, wall } = design.boards
  dashboard.widgets.push({ ...dashboardWidget('table', 'table', 'Table'), x: 0, y: 0, w: 12, h: 4 })
  wall.widgets.push({ ...dashboardWidget('logo', 'image', 'Logo', 'wall'), x: 0, y: 0, w: 280, h: 140 })
  const original = { ...wall.widgets[0] }
  switchDashboardLayout(design)
  assert.deepEqual(wall.widgets[0], original)
  assert.equal(wall.widgets[1].id, 'table')
  assert.equal(dashboardOverlap(...wall.widgets), false)
})

test('group bounds resize the children proportionally and respect their minimum sizes', () => {
  const draft = emptyDashboard()
  const wall = draft.boards.wall
  const lower = { ...dashboardWidget('chart', 'line', 'Chart', 'wall'), x: 200, y: 100, w: 160, h: 128 }
  const upper = { ...dashboardWidget('panel', 'panel', 'Panel', 'wall'), x: 120, y: 68, w: 320, h: 256 }
  wall.widgets = groupDashboardWidgets([lower, upper], ['chart', 'panel'], 'group')
  const rect = dashboardGroupBounds(wall.widgets)
  assert.deepEqual(rect, { x: 120, y: 68, w: 320, h: 256 })
  const scaled = resizeDashboardGroup(wall.widgets, { x: 300, y: 150, w: 640, h: 512 })
  assert.deepEqual(scaled.get('chart'), { x: 460, y: 214, w: 320, h: 256 })
  assert.deepEqual(scaled.get('panel'), { x: 300, y: 150, w: 640, h: 512 })
  const minimum = constrainDashboardGroup({ ...rect, w: 90, h: 70 }, wall.widgets, wall)
  assert.deepEqual([minimum.w, minimum.h], [160, 128])
  const restored = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.wall
  assert.deepEqual(restored.widgets.map(widget => widget.groupId), ['group', 'group'])
})

test('layer ordering moves whole groups and permits reordering only within a layer level', () => {
  const widgets = ['a', 'b', 'c', 'd'].map(id => dashboardWidget(id, 'text', id, 'wall'))
  const grouped = groupDashboardWidgets(widgets, ['a', 'c'], 'group')
  assert.deepEqual(grouped.map(widget => widget.id), ['b', 'a', 'c', 'd'])
  assert.deepEqual(reorderDashboardLayers(grouped, 'group', 'd', true).map(widget => widget.id), ['b', 'd', 'a', 'c'])
  assert.deepEqual(reorderDashboardLayers(grouped, 'c', 'a', false).map(widget => widget.id), ['b', 'c', 'a', 'd'])
  assert.equal(reorderDashboardLayers(grouped, 'a', 'd', true), grouped)
  const contained = widgets.map(widget => ({ ...widget, parentId: widget.id === 'a' || widget.id === 'b' ? 'container' : undefined }))
  assert.equal(reorderDashboardLayers(contained, 'a', 'd', true), contained)
  assert.deepEqual(reorderDashboardLayers(contained, 'a', 'b', true).map(widget => widget.id), ['b', 'a', 'c', 'd'])
})

test('new grid widgets are placed without collisions and wall coordinates stay inside the board', () => {
  const { boards } = emptyDashboard()
  boards.dashboard.widgets.push({ ...dashboardWidget('metric', 'metric', 'Visits'), x: 0, y: 0, w: 3, h: 2 })
  const next = dashboardPlacement(boards.dashboard, 'dashboard', 'metric')
  assert.equal(dashboardOverlap(boards.dashboard.widgets[0], next), false)
  assert.deepEqual(constrainDashboardRect({ x: 2000, y: -8, w: 400, h: 250 }, 'wall', boards.wall),
    { x: 1520, y: 0, w: 400, h: 250 })
})

test('grid auto-align moves a blocked drop to a nearby slot only when enabled', () => {
  const board = emptyDashboard().boards.dashboard
  board.widgets.push({ ...dashboardWidget('fixed', 'metric', 'Fixed'), x: 0, y: 0, w: 3, h: 2 })
  const drop = { x: 2.4, y: 0, w: 3, h: 2 }
  assert.deepEqual(dashboardGridTarget(board, drop), { x: 3, y: 0, w: 3, h: 2 })
  assert.equal(dashboardGridTarget(board, drop, 'moving', false), undefined)
  assert.deepEqual(dashboardGridTarget(board, drop, 'fixed'), { x: 2, y: 0, w: 3, h: 2 })
  board.snap = false
  assert.equal(dashboardGridTarget(board, drop), undefined)
  assert.deepEqual(dashboardGridTarget(board, { ...drop, x: 3 }), { x: 3, y: 0, w: 3, h: 2 })
})

test('saved canvas appearance and geometry are independent and older layouts retain their defaults', () => {
  const draft = emptyDashboard('Canvas')
  Object.assign(draft.boards.dashboard, { gap: 20, padding: 6, rowHeight: 72, background: '#EEF4F2', snap: false,
    backgroundImage: 'https://example.com/background.png', backgroundFit: 'contain', backgroundOpacity: 35, backgroundBlur: 3 })
  Object.assign(draft.boards.wall, { snapStep: 32, background: 'invalid', backgroundImage: 'javascript:alert(1)' })
  const restored = readDashboardDocument(JSON.stringify({ version: 3, draft, published: { boards: { dashboard: {}, wall: {} } } }))
  const { dashboard, wall } = restored.draft.boards
  assert.deepEqual([dashboard.gap, dashboard.padding, dashboard.rowHeight, dashboard.background, dashboard.snap], [20, 6, 72, '#EEF4F2', false])
  assert.deepEqual([dashboard.backgroundImage, dashboard.backgroundFit, dashboard.backgroundOpacity, dashboard.backgroundBlur], ['https://example.com/background.png', 'contain', 35, 3])
  assert.deepEqual([wall.snapStep, wall.background], [32, ''])
  assert.equal(wall.backgroundImage, '')
  const older = restored.published.boards.dashboard
  assert.deepEqual([older.gap, older.padding, older.rowHeight, older.snapStep], [12, 12, 56, 16])
})

test('calendar periods and metric cards use real values and ignore missing numbers', () => {
  const rows = [{ day: '2026-10-28', value: 10 }, { day: '2026-11-02', value: 3 }, { day: '2026-11-03', value: '7' }, { day: '2026-11-04', value: null }]
  const period = dashboardRowsInPeriod(rows, 'day', 3, new Date(2026, 10, 4, 12).getTime())
  assert.equal(dashboardMetric(period, '', 'count'), 3)
  assert.equal(dashboardMetric(period, 'value', 'avg'), 5)
  assert.equal(dashboardMetric(period, 'value', 'distinct'), 2)
})

test('canvas title visibility and independent card spacing survive saved layout parsing', () => {
  const draft = emptyDashboard('Spacing')
  const widget = dashboardWidget('chart', 'report', 'Chart')
  draft.boards.dashboard.widgets.push({ ...widget, padding: [8, 16, 24, 32], corners: [0, 4, 12, 16] })
  draft.boards.wall.showTitle = true
  const { draft: restored } = readDashboardDocument(JSON.stringify({ version: 3, draft }))
  assert.equal(restored.boards.dashboard.showTitle, false)
  assert.equal(restored.boards.wall.showTitle, true)
  assert.deepEqual(restored.boards.dashboard.widgets[0].padding, [8, 16, 24, 32])
  assert.deepEqual(restored.boards.dashboard.widgets[0].corners, [0, 4, 12, 16])
})

test('card visibility persists independently of the title and stored card appearance', () => {
  const draft = emptyDashboard()
  const legacy = dashboardWidget('legacy', 'text', 'Old widget')
  delete legacy.showCard
  draft.boards.dashboard.widgets.push(legacy, {
    ...dashboardWidget('bare', 'line', 'Bare chart'), showCard: false, showTitle: true, background: '#EEF4F2',
    padding: [6, 12, 8, 16], corners: [2, 4, 6, 8], border: true,
  })
  const [oldWidget, bare] = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard.widgets
  assert.equal(oldWidget.showCard, true)
  assert.deepEqual([bare.showCard, bare.showTitle, bare.background, bare.padding, bare.corners, bare.border],
    [false, true, '#EEF4F2', [6, 12, 8, 16], [2, 4, 6, 8], true])
})

test('additional display, content and decorative settings survive saved layout parsing', () => {
  const draft = emptyDashboard()
  draft.boards.dashboard.widgets.push(
    { ...dashboardWidget('details', 'recordDetail', 'Details'), displayFields: ['name', 'state', 'name'], recordIndex: 2 },
    { ...dashboardWidget('links', 'linkList', 'Links'), links: [{ id: 'one', label: 'Docs', url: '/docs' }] },
    { ...dashboardWidget('deadline', 'countdown', 'Deadline'), deadline: { date: '2026-12-01', hour: 14, minute: 30 } },
    { ...dashboardWidget('gradient', 'gradientPanel', 'Gradient'), secondaryColor: '#15E5D1', strokeWidth: 99, gridSize: 0 },
    { ...dashboardWidget('corner', 'cornerAccent', 'Corner'), cornerSize: 80, cornerPosition: 'bottomRight' },
  )
  const { widgets } = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard
  assert.deepEqual(widgets[0].displayFields, ['name', 'state'])
  assert.equal(widgets[0].recordIndex, 2)
  assert.deepEqual(widgets[1].links, [{ id: 'one', label: 'Docs', url: '/docs' }])
  assert.deepEqual(widgets[2].deadline, { date: '2026-12-01', hour: 14, minute: 30 })
  assert.deepEqual([widgets[3].secondaryColor, widgets[3].strokeWidth, widgets[3].gridSize, widgets[3].showCard], ['#15E5D1', 12, 8, false])
  assert.deepEqual([widgets[4].cornerSize, widgets[4].cornerPosition, widgets[4].showCard], [80, 'bottomRight', false])
  assert.equal(dashboardWidgetUsesAsset('metricGrid'), true)
  assert.equal(dashboardWidgetUsesAsset('countdown'), false)
  assert.equal(dashboardWidgetUsesAsset('gradientPanel'), false)
})

test('countdown targets accept local calendar dates and reject impossible dates and times', () => {
  assert.equal(dashboardDeadlineTime({ date: '2028-02-29', hour: 12, minute: 30 }), new Date(2028, 1, 29, 12, 30).getTime())
  assert.equal(dashboardDeadlineTime({ date: '2026-02-29', hour: 12, minute: 0 }), undefined)
  assert.equal(dashboardDeadlineTime({ date: '2026-12-01', hour: 24, minute: 0 }), undefined)
  assert.equal(dashboardDeadlineTime(), undefined)
})

test('saved animations keep timing and triggers and reject unknown effects', () => {
  const draft = emptyDashboard()
  draft.boards.wall.widgets.push(
    { ...dashboardWidget('ring', 'glowRing', 'Ring', 'wall'), animation: { effect: 'pulse', duration: 2.5, delay: .2, repeat: true, trigger: 'hover', easing: 'linear' } },
    { ...dashboardWidget('invalid', 'ribbon', 'Ribbon', 'wall'), animation: { effect: 'unknown', duration: -1, delay: 100 } },
  )
  const { widgets } = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.wall
  assert.deepEqual(widgets[0].animation, { effect: 'pulse', duration: 2.5, delay: .2, repeat: true, trigger: 'hover', easing: 'linear' })
  assert.deepEqual(widgets[1].animation, { effect: 'none', duration: .1, delay: 60, repeat: false, trigger: 'load', easing: 'ease-out' })
  assert.equal(dashboardWidgetUsesAsset('glowRing'), false)
})

test('screen effects persist their bounded settings without occupying the component grid', () => {
  const draft = emptyDashboard()
  const snow = dashboardWidget('snow', 'snow', 'Snow')
  snow.screenEffect = { quantity: 999, speed: 0, size: 100, opacity: 500, wind: -999, layer: 'background', enabled: false }
  draft.boards.dashboard.widgets.push(snow)
  const next = dashboardPlacement(draft.boards.dashboard, 'dashboard', 'metric')
  assert.deepEqual(next, { x: 0, y: 0, w: 3, h: 2 })
  assert.deepEqual(dashboardGridTarget(draft.boards.dashboard, next), next)
  const [saved] = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard.widgets
  assert.deepEqual(saved.screenEffect, { quantity: 300, speed: 10, size: 32, opacity: 100, wind: -100, layer: 'background', enabled: false })
  assert.equal(dashboardWidgetUsesAsset('snow'), false)
  assert.equal(saved.showCard, false)
  assert.equal(dashboardScreenEffectSettings('aurora', { quantity: 300 }).quantity, 12)
  assert.equal(dashboardScreenEffectSettings('particleNetwork', { quantity: 300 }).quantity, 120)
  switchDashboardLayout(draft)
  assert.deepEqual(draft.boards.wall.widgets.map(widget => widget.id), ['snow'])
})

test('nested containers preserve children, prevent cycles and transfer their component tree between layouts', () => {
  const draft = emptyDashboard()
  const root = { ...dashboardWidget('root', 'gridContainer', 'Root'), x: 0, y: 0, w: 6, h: 4, layoutOptions: dashboardLayoutSettings({ columns: 3, gap: 8 }) }
  const tabs = { ...dashboardWidget('tabs', 'tabsContainer', 'Tabs'), parentId: 'root' }
  const nested = { ...dashboardWidget('nested', 'columnContainer', 'Nested'), parentId: 'tabs' }
  const metric = { ...dashboardWidget('metric', 'metricTicker', 'Metric'), parentId: 'nested', reportCode: 'REPORT' }
  const extra = dashboardWidget('extra', 'rowContainer', 'Extra')
  const widgets = [root, tabs, nested, metric, extra]
  assert.equal(dashboardCanContain(widgets, 'root', 'root'), false)
  assert.equal(dashboardCanContain(widgets, 'nested', 'root'), false)
  assert.equal(dashboardCanContain(widgets, 'nested', 'extra'), false)
  assert.equal(dashboardCanContain(widgets, 'root', 'tabs'), true)
  draft.boards.dashboard.widgets = widgets.slice(0, 4)
  const restored = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft
  assert.deepEqual(restored.boards.dashboard.widgets.map(widget => widget.parentId), [undefined, 'root', 'tabs', 'nested'])
  assert.deepEqual([...dashboardDescendantIds(restored.boards.dashboard.widgets, ['root'])], ['root', 'tabs', 'nested', 'metric'])
  assert.equal(switchDashboardLayout(restored), 0)
  assert.deepEqual(restored.boards.wall.widgets.map(widget => widget.parentId), [undefined, 'root', 'tabs', 'nested'])
  assert.deepEqual(dashboardReportCodes({ version: 3, draft: restored }), ['REPORT'])
})

test('malformed container ownership is repaired and display settings retain safe bounds', () => {
  const draft = emptyDashboard()
  draft.boards.dashboard.widgets = [
    { ...dashboardWidget('a', 'rowContainer', 'A'), parentId: 'b' },
    { ...dashboardWidget('b', 'gridContainer', 'B'), parentId: 'a' },
    { ...dashboardWidget('c', 'heading', 'C'), parentId: 'missing' },
  ]
  const widgets = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard.widgets
  assert.equal(widgets[0].parentId, undefined)
  assert.equal(widgets[1].parentId, 'a')
  assert.equal(widgets[2].parentId, undefined)
  assert.equal(dashboardDataSettings({ limit: 999, decimals: -4 }).limit, 100)
  assert.equal(dashboardDataSettings({ limit: 999, decimals: -4 }).decimals, 0)
  assert.equal(dashboardContentSettings({ interval: 0, lineHeight: 99 }).interval, 2)
  assert.equal(dashboardContentSettings({ interval: 0, lineHeight: 99 }).lineHeight, 3)
  assert.equal(dashboardScreenEffectSettings('fog').size, 110)
  assert.equal(dashboardScreenEffectSettings('fireworks', { quantity: 500 }).quantity, 24)
})

test('previous layouts with a single radius preserve their existing corner styles', () => {
  const draft = emptyDashboard()
  draft.boards.dashboard.widgets = [{ id: 'one', kind: 'report', name: 'Chart', radius: 18 }]
  draft.boards.wall.widgets = [{ id: 'two', kind: 'report', name: 'Wall chart', radius: 12 }]
  const { draft: restored } = readDashboardDocument(JSON.stringify({ version: 3, draft }))
  assert.deepEqual(restored.boards.dashboard.widgets[0].corners, [18, 18, 18, 18])
  assert.deepEqual(restored.boards.wall.widgets[0].corners, [0, 0, 0, 0])
})

test('new walls fit the screen while previously customized resolutions stay fixed', () => {
  const draft = emptyDashboard()
  assert.equal(draft.boards.wall.resolutionMode, 'auto')
  delete draft.boards.wall.resolutionMode
  draft.boards.wall.width = 1280
  draft.boards.wall.height = 720
  const old = readDashboardDocument(JSON.stringify({ version: 3, draft }))
  assert.equal(old.draft.boards.wall.resolutionMode, 'fixed')
  Object.assign(draft.boards.wall, dashboardWallResolution('auto'))
  const automatic = readDashboardDocument(JSON.stringify({ version: 3, draft }))
  assert.equal(automatic.draft.boards.wall.resolutionMode, 'auto')
  assert.equal(automatic.draft.boards.wall.width, 1280)
})

test('new widgets keep their settings after save and reject unsafe links', () => {
  const draft = emptyDashboard()
  draft.boards.dashboard.widgets.push(
    { ...dashboardWidget('link', 'link', 'Docs'), url: 'https://example.com/guide', text: 'Open docs' },
    { ...dashboardWidget('gauge', 'gauge', 'Goal'), target: 240, color: '#079b70' },
    dashboardWidget('radar', 'radar', 'Distribution'),
  )
  const { widgets } = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard
  assert.equal(dashboardResourceUrl(widgets[0].url), 'https://example.com/guide')
  assert.equal(widgets[0].text, 'Open docs')
  assert.equal(widgets[1].target, 240)
  assert.equal(widgets[1].color, '#079b70')
  assert.equal(dashboardChartType(widgets[2].kind), 'radar')
  assert.equal(dashboardResourceUrl('javascript:alert(1)'), '')
  assert.equal(dashboardResourceUrl('//example.com/file'), '')
})

test('every dashboard tool has a translated default name in both locales', async () => {
  for (const locale of ['en', 'zh-cn']) {
    const messages = JSON.parse(await readFile(new URL(`../src/i18n/locales/${locale}.json`, import.meta.url), 'utf8'))
    for (const kind of ['report', ...dashboardTools]) {
      const key = dashboardWidgetLabelKey(kind)
      assert.ok(messages[key]?.trim(), `${locale} is missing a label for ${kind}: ${key}`)
    }
    for (const effect of dashboardAnimationEffects) assert.ok(messages['dashboard.studio.animationEffect.' + effect]?.trim(), `${locale} is missing ${effect}`)
  }
  assert.equal(dashboardWidgetLabelKey('map'), 'queryChart.type.map')
})

test('saved translation keys are repaired in both dashboard versions while custom names are preserved', async () => {
  const draft = emptyDashboard('Chart titles')
  draft.boards.dashboard.widgets.push(
    dashboardWidget('map', 'map', 'dashboard.studio.tool.map'),
    dashboardWidget('sales', 'map', '区域销售'),
  )
  const published = emptyDashboard('Chart titles')
  published.boards.wall.widgets.push(dashboardWidget('heatmap', 'heatmap', 'queryChart.type.heatmap', 'wall'))
  const locale = JSON.parse(await readFile(new URL('../src/i18n/locales/zh-cn.json', import.meta.url), 'utf8'))
  const restored = readDashboardDocument(JSON.stringify({ version: 3, draft, published }), '', '', key => locale[key])
  assert.deepEqual(restored.draft.boards.dashboard.widgets.map(widget => widget.name), ['地图', '区域销售'])
  assert.equal(restored.published.boards.wall.widgets[0].name, '热力图')
})

test('a cross-tab preserves zero and distinguishes missing combinations', () => {
  assert.deepEqual(dashboardPivot([
    { category: 'Phone', series: 'Success', value: 3 },
    { category: 'Phone', series: 'Failure', value: 0 },
    { category: 'Desktop', series: 'Success', value: 8 },
  ], 'Device'), {
    headers: ['Device', 'Success', 'Failure'],
    rows: [['Phone', 3, 0], ['Desktop', 8, null]],
  })
  assert.equal(dashboardPivot(Array.from({ length: 51 }, (_, index) => ({
    category: 'Phone', series: String(index), value: 1,
  })), 'Device'), undefined)
})

test('map and range configuration is available before a chart asset is bound', () => {
  const widget = dashboardWidget('map', 'map', 'Map')
  widget.chartOptions = { mapGeoJson: '{"type":"FeatureCollection","features":[]}' }
  const draft = emptyDashboard()
  draft.boards.dashboard.widgets.push(widget)
  const [saved] = readDashboardDocument(JSON.stringify({ version: 3, draft })).draft.boards.dashboard.widgets
  assert.equal(dashboardChartMappings(saved).type, 'map')
  assert.equal(dashboardChartMappings(saved).mapGeoJson, widget.chartOptions.mapGeoJson)
  assert.equal(dashboardChartMappings(dashboardWidget('range', 'custom', 'Range')).type, 'custom')
})
