import {
  dashboardAuditSettings, dashboardBusinessSettings, dashboardChartType, dashboardComparativeSettings, dashboardContainerTool,
  dashboardContentSettings, dashboardDataSettings, dashboardIntervalSettings, dashboardLayoutSettings, dashboardOperationsSettings,
  dashboardPlanningSettings, dashboardProcessSettings, dashboardRelationSettings, dashboardSpatialSettings, dashboardTemporalSettings,
  dashboardWidget, dashboardWidgetUsesAsset, type DashboardWidgetKind,
} from './dashboard-layout'
import { informationFields, isInformationBlock, type InformationItem } from './information-block'
import { defaultQueryChart, type QueryChartConfiguration, type QueryChartRow } from './query-chart'
import type { ReportModel } from './report'

type Translate = (key: string, values?: Record<string, number | string>) => string
const sampleCode = 'component-example'
const DAY = 86_400_000
const HOUR = 3_600_000
const palette = ['#16a38a', '#6f8ce8', '#ebac60']
const groups = ['North', 'South', 'West']
const stages = ['Intake', 'Review', 'Delivery']
const fields = ['Period', 'Region', 'Category', 'Stage', 'Value', 'Target', 'Balance', 'Date', 'End', 'Done', 'Entity',
  'Task', 'Owner', 'Status', 'Expected', 'Active', 'Input', 'Latitude', 'Longitude', 'Open', 'Close', 'Low', 'High', 'StartX', 'StartY', 'EndX', 'EndY']
const mapExample = JSON.stringify({ type: 'FeatureCollection', features: groups.map((name, index) => {
  const x = index * 2, y = index % 2
  return { type: 'Feature', properties: { name }, geometry: { type: 'Polygon',
    coordinates: [[[x, y], [x + 1.8, y + .25], [x + 1.9, y + 1.5], [x + .65, y + 1.8], [x, y]]] } }
}) })

function sampleRows(now: number): QueryChartRow[] {
  return Array.from({ length: 18 }, (_, index) => {
    const period = Math.floor(index / 3)
    const group = index % 3
    const start = now + [-14, -9, -3, -1, 1, 3][period] * DAY + group * HOUR
    const value = [42, 62, 54, 81, 75, 94][period] + [8, -9, 0][group] + (index === 15 ? 42 : 0)
    const status = ['success', 'pending', 'failed'][index % 3]
    const longitude = 116.38 + (group * 4 + period) * .014
    const latitude = 39.89 + (group * 5 - period) * .012
    return {
      Period: 'P' + (period + 1), Region: groups[group], Category: ['Online', 'Store', 'Partner'][(group + period) % 3],
      Stage: stages[(period + Math.floor(group / 2)) % 3], Value: value, Target: 62 + period * 4 + group * 5, Balance: (group === 1 ? -1 : 1) * (period + 1) * 8,
      Date: new Date(start).toISOString(), End: new Date(start + (6 + group) * HOUR).toISOString(), Done: index % 3 === 0 ? new Date(start - HOUR).toISOString() : null,
      Entity: 'ID-' + (group + 1), Task: 'TASK-' + (index + 1).toString().padStart(2, '0'), Owner: ['Ada', 'Kai', 'Mina'][group],
      Status: status, Expected: index % 4 === 0 ? 'success' : status, Active: index % 3 !== 2, Input: index % 5 === 0 ? 'n/a' : index === 2 ? null : String(value),
      Latitude: latitude, Longitude: longitude, Open: value - 3, Close: value + (period % 2 ? -7 : 4), Low: value - 11, High: value + 9,
      StartX: longitude, StartY: latitude, EndX: longitude + .03, EndY: latitude + .02,
    }
  })
}

function chartExample(kind: DashboardWidgetKind, now: number): QueryChartConfiguration {
  const type = dashboardChartType(kind) ?? 'line'
  const rows = sampleRows(now)
  const numeric = ['Value', 'Target', 'Balance', 'Longitude', 'Latitude']
  const dateCategory = ['calendarMatrix', 'themeRiver'].includes(type)
  const regionCategory = ['map', 'graph', 'sankey', 'tree', 'treemap', 'sunburst', 'violinPlot', 'ridgePlot', 'tukeyBox',
    'stripPlot', 'ganttTimeline', 'radialBar', 'mirrorBar', 'targetComparison', 'polarBar', 'polarStackedBar', 'polarLine'].includes(type)
  const category = dateCategory ? 'Date' : regionCategory ? 'Region' : 'Period'
  const noSeries = ['pie', 'donut', 'funnel', 'rose', 'radar', 'candlestick', 'boxplot', 'tukeyBox', 'radialBar', 'ganttTimeline',
    'dualAxis', 'dumbbell', 'rangeBand', 'mirrorBar', 'targetComparison'].includes(type)
  const series = noSeries ? '' : ['graph', 'sankey', 'tree', 'treemap', 'sunburst', 'heatmap', 'table'].includes(type) ? 'Category' : 'Region'
  const headers = type === 'table' ? ['Period', 'Region', 'Value', 'Status'] : fields
  let raw = type === 'table' ? rows.slice(0, 5) : ['candlestick', 'dumbbell'].includes(type) ? rows.filter((_, index) => index % 3 === 0) : rows
  if (kind === 'yearOverYearTrend') raw = [...raw, ...rows.map(row => {
    const date = new Date(String(row.Date))
    date.setUTCFullYear(date.getUTCFullYear() - 1)
    return { ...row, Date: date.toISOString(), Value: Number(row.Value) * .8 }
  })]
  return { headers, columns: raw, queryChart: { version: 1, options: {
    ...defaultQueryChart(headers), type, category, series, metric: ['deviationBar', 'signedShareBar', 'waterfall'].includes(type) ? 'Balance' : 'Value',
    aggregation: 'sum', grain: type === 'calendarMatrix' ? 'day' : 'none', colors: palette.join(','), labels: false,
    legend: !!series && !['table', 'scatter', 'effectScatter', 'themeRiver'].includes(type), transparent: true, pointAction: 'none',
    tooltip: 'off', zoom: false, download: false, refreshSeconds: 0, sort: 'original', mapGeoJson: mapExample,
    parallelFields: numeric,
    chartFields: { value: 'Value', lower: 'Low', upper: 'High', open: 'Open', close: 'Close', low: 'Low', high: 'High',
      bubbleX: 'Value', bubbleY: 'Target', bubbleSize: 'Open', before: 'Target', after: 'Value', columnMeasure: 'Value', lineMeasure: 'Target',
      leftMeasure: 'Target', rightMeasure: 'Value', actualMeasure: 'Value', targetMeasure: 'Target', intervalStart: 'Date', intervalEnd: 'End',
      startX: 'StartX', startY: 'StartY', endX: 'EndX', endY: 'EndY' },
  } } }
}

function informationExamples(kind: DashboardWidgetKind, t: Translate): InformationItem[] | undefined {
  if (!isInformationBlock(kind)) return
  const key = 'dashboard.example.'
  const schema = informationFields[kind]
  const values: Record<string, string[]> = {
    url: ['https://example.com', 'https://example.org'],
    email: ['ada@example.com', 'kai@example.com'],
    ratingValue: ['4.6', '3.8'], capacityUsed: ['64', '85'], capacityLimit: ['100', '120'],
    windowStart: ['09:00', '14:00'], windowEnd: ['10:30', '15:00'], deployedVersion: ['v2.4.0', 'v2.5.0'],
    assetId: ['AS-021', 'AS-036'], owner: ['Ada', 'Kai'], reviewer: ['Mina', 'Kai'], recipient: ['Kai', 'Ada'],
    shift: ['09:00–13:00', '13:00–17:00'], snippetLanguage: ['SQL', 'JSON'],
  }
  const cell = (field: string | undefined, index: number, fallback: string) => field ? values[field]?.[index] ?? fallback : ''
  return [0, 1].map(index => ({
    id: 'entry-' + index,
    title: kind === 'faqList' ? t(key + 'question', { number: index + 1 }) : t(key + 'entry', { number: index + 1 }),
    value: cell('value' in schema ? schema.value : undefined, index, index === 0 ? '64' : '85'),
    secondary: cell('secondary' in schema ? schema.secondary : undefined, index, index === 0 ? '80' : '100'),
    detail: kind === 'copySnippets' ? index === 0 ? 'SELECT region, SUM(value) FROM sample GROUP BY region;' : '{ "status": "ready" }'
      : t(key + 'description'),
    tone: index === 0 ? 'success' : 'warning',
  }))
}

export function screenComponentExample(kind: DashboardWidgetKind, title: string, t: Translate, art: readonly [string, string], now = Date.now()) {
  const key = 'dashboard.example.'
  const widget = dashboardWidget('example', kind, title)
  const snapshot = dashboardWidgetUsesAsset(kind) ? chartExample(kind, now) : undefined
  const report: ReportModel | undefined = snapshot ? { code: sampleCode, name: t(key + 'sampleData'), realtime: false } : undefined
  if (report) widget.reportCode = report.code
  const data = dashboardDataSettings({ baselineField: 'Target', statusField: 'Status', limit: 4, decimals: 1, periodDays: 7 })
  Object.assign(widget, {
    text: t(key + 'description'), image: art[0], url: 'https://example.com', field: 'Value', operation: 'sum',
    action: 'none', expandable: false, timeField: 'Date', target: 1600, displayFields: ['Region', 'Value', 'Target', 'Status'],
    links: [0, 1, 2].map(index => ({ id: 'link-' + index, label: t(key + 'entry', { number: index + 1 }),
      url: 'https://example.com', text: t(key + 'description'), image: art[index % 2], color: palette[index] })),
    deadline: { date: new Date(now + DAY * 2).toISOString().slice(0, 10), hour: 12, minute: 0 },
    contentOptions: dashboardContentSettings({ autoplay: kind === 'textTicker', attribution: t(key + 'sampleData') }),
    dataOptions: data, layoutOptions: dashboardLayoutSettings({ columns: 2, gap: 5, padding: 5, itemHeight: 56, pageSize: 4, minWidth: 90 }),
    informationItems: informationExamples(kind, t),
    operationsOptions: dashboardOperationsSettings(kind, { labelField: 'Task', statusField: 'Status', detailsField: 'Region', endField: 'End', threshold: 60 }),
    businessOptions: dashboardBusinessSettings({ groupField: kind === 'stageConversion' ? 'Stage' : 'Region', subgroupField: 'Category',
      referenceField: 'Target', entityField: 'Entity', stageOrder: stages }),
    relationOptions: dashboardRelationSettings({ rowField: kind === 'transitionMatrix' || kind === 'cooccurrencePairs' ? 'Stage' : 'Region',
      columnField: 'Category', entityField: 'Entity' }),
    auditOptions: dashboardAuditSettings({ fields: kind === 'fieldComparison' ? ['Status', 'Expected'] : ['Entity', 'Input'] }),
    temporalOptions: dashboardTemporalSettings({ entityField: 'Entity', endField: 'End', labelField: 'Task', historyDays: 24 }),
    comparativeOptions: dashboardComparativeSettings({ fields: ['Value', 'Target', 'Balance'], secondField: 'Target',
      groupField: 'Region', leftGroup: groups[0], rightGroup: groups[1] }),
    planningOptions: dashboardPlanningSettings({ labelField: 'Task', statusField: 'Status', ownerField: 'Owner', groupField: 'Region',
      completedField: 'Done', doneValues: 'success', includeCompleted: true }),
    spatialOptions: dashboardSpatialSettings({ longitudeField: 'Longitude', latitudeField: 'Latitude', groupField: 'Region',
      gridDegrees: .05, referenceLat: 39.9, referenceLon: 116.4 }),
    processOptions: dashboardProcessSettings({ entityField: 'Entity', stageField: 'Stage', requiredStages: stages }),
    intervalOptions: dashboardIntervalSettings({ endField: 'End', groupField: kind === 'intervalOverlaps' ? '' : 'Region', durationBands: '5,10,20', minConcurrent: 2 }),
    processQualityOptions: { measure: 'Value', group: 'Region', lower: 50, upper: 96, order: { field: 'Date', type: 'date' } },
  } satisfies Partial<typeof widget>)
  if (['dueAgenda', 'overdueAging', 'planCalendar', 'onTimeCompletion'].includes(kind)) widget.timeField = 'Date'
  if (['selectFilter', 'multiValueFilter', 'searchFilter', 'dataTags', 'valueFrequency'].includes(kind)) widget.field = 'Region'
  if (kind === 'dateRangeFilter' || kind === 'relativeDateFilter') widget.field = 'Date'
  if (kind === 'booleanFilter') widget.field = 'Active'
  if (kind === 'invalidValues' || kind === 'valueStateFilter') widget.field = 'Input'
  if (kind === 'stageConversion') widget.field = ''
  if (kind === 'metric' || kind === 'gauge' || kind === 'progress') widget.target = 1900
  if (kind === 'tagCloud') widget.text = ['North', 'South', 'West', 'Online', 'Partner'].join('\n')
  if (kind === 'codeBlock') widget.text = 'SELECT region, SUM(value)\nFROM sample\nGROUP BY region;'
  if (kind === 'markdown') widget.text = '## ' + t(key + 'sampleData') + '\n\n- ' + t(key + 'description') + '\n- ' + t(key + 'entry', { number: 2 })
  if (kind === 'worldClock') widget.text = 'UTC'
  const children = dashboardContainerTool(kind) ? [0, 1, 2].map(index => ({
    ...dashboardWidget('child-' + index, 'badge', t(key + 'entry', { number: index + 1 })),
    parentId: widget.id, text: t(key + 'entry', { number: index + 1 }), tone: (['info', 'success', 'warning'] as const)[index],
    padding: [4, 4, 4, 4] as typeof widget.padding, background: ['#208ed51a', '#19b0821a', '#d99b3e1a'][index],
  })) : []
  return { widget, snapshot, report, children }
}
