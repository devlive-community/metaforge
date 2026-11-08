import { chartFieldKind, queryChartTypes, type QueryChartConfiguration, type QueryChartOptions, type QueryChartPoint, type QueryChartRow, type QueryChartType } from './query-chart'
import { geoJsonLimit, suggestChartFields, type ChartMappings } from './query-chart-data'
import type { ReportModel } from './report'
import { informationBlockKinds, isInformationBlock, readInformationItems, type InformationItem } from './information-block'
import { isProcessQualityView, processQualityViews, readProcessQualityOptions, type ProcessQualityOptions } from './process-quality'

export const dashboardMode = ['dashboard', 'wall'] as const
export type DashboardMode = typeof dashboardMode[number]
export const dashboardExtendedDataTools = ['rollingTable', 'comparisonTable', 'statisticList', 'calendarHeatmap', 'radialProgress', 'thresholdCards'] as const
export const dashboardMonitoringTools = ['dataQuality', 'periodComparison', 'dataFreshness', 'sparklineGrid'] as const
export const dashboardOperationDataTools = ['incidentFeed', 'outlierQueue', 'runHistory', 'slaScorecard', 'latencyPercentiles', 'maintenanceWindow', 'recordLookup'] as const
export function dashboardOperationDataTool(kind?: DashboardWidgetKind) { return dashboardOperationDataTools.some(tool => tool === kind) }
export const dashboardBusinessTools = ['stageConversion', 'targetGap', 'segmentSummary', 'paretoTable', 'varianceDrivers', 'cohortRetention', 'recencyDistribution', 'valueBands'] as const
export function dashboardBusinessTool(kind?: DashboardWidgetKind) { return dashboardBusinessTools.some(tool => tool === kind) }
export const dashboardRelationTools = ['crossTabHeatmap', 'transitionMatrix', 'cooccurrencePairs', 'groupSpread'] as const
export function dashboardRelationTool(kind?: DashboardWidgetKind) { return dashboardRelationTools.some(tool => tool === kind) }
export const dashboardAuditTools = ['duplicateKeys', 'valueFrequency', 'missingPatterns', 'fieldComparison', 'invalidValues', 'dateCoverage'] as const
export function dashboardAuditTool(kind?: DashboardWidgetKind) { return dashboardAuditTools.some(tool => tool === kind) }
export const dashboardTemporalTools = ['cumulativeTrend', 'rollingAverageTrend', 'yearOverYearTrend', 'weekdayHourHeatmap', 'eventIntervals', 'intervalTimeline'] as const
export function dashboardTemporalTool(kind?: DashboardWidgetKind) { return dashboardTemporalTools.some(tool => tool === kind) }
export const dashboardComparativeTools = ['metricCorrelation', 'metricQuadrants', 'rankMovement', 'shareMovement', 'pairedDifference', 'comparisonDistribution'] as const
export function dashboardComparativeTool(kind?: DashboardWidgetKind) { return dashboardComparativeTools.some(tool => tool === kind) }
export const dashboardPlanningTools = ['taskBoard', 'planCalendar', 'dueAgenda', 'overdueAging', 'groupCompletion', 'onTimeCompletion'] as const
export function dashboardPlanningTool(kind?: DashboardWidgetKind) { return dashboardPlanningTools.some(tool => tool === kind) }
export const dashboardSpatialTools = ['geoPoints', 'geoGrid', 'geoTrack', 'geoBounds', 'geoHotspots', 'geoDistanceBands'] as const
export function dashboardSpatialTool(kind?: DashboardWidgetKind) { return dashboardSpatialTools.some(tool => tool === kind) }
export const dashboardProcessTools = ['processPaths', 'processEndpoints', 'processCycle', 'processWaits', 'processRework', 'processCompliance'] as const
export function dashboardProcessTool(kind?: DashboardWidgetKind) { return dashboardProcessTools.some(tool => tool === kind) }
export const dashboardIntervalTools = ['intervalConcurrency', 'intervalCoverage', 'intervalOverlaps', 'intervalDurationBands', 'intervalGroupCoverage', 'intervalGaps'] as const
export function dashboardIntervalTool(kind?: DashboardWidgetKind) { return dashboardIntervalTools.some(tool => tool === kind) }
export const dashboardAdvancedDataTools = ['metricTicker', 'metricComparison', 'metricTrend', 'bulletMetric', 'rankList', 'distributionList', 'statusGrid', 'recordCards', ...dashboardExtendedDataTools, ...dashboardMonitoringTools] as const
const dataDisplays = ['pivot', 'dataList', 'dataSummary', 'metricGrid', 'recordDetail', 'progressList', 'dataTags', 'dataTimeline', ...dashboardAdvancedDataTools, ...dashboardOperationDataTools, ...dashboardBusinessTools, ...dashboardRelationTools, ...dashboardAuditTools, ...dashboardTemporalTools, ...dashboardComparativeTools, ...dashboardPlanningTools, ...dashboardSpatialTools, ...dashboardProcessTools, ...dashboardIntervalTools, ...processQualityViews] as const
export type DashboardDataTool = typeof dataDisplays[number]
export const dashboardExtendedContentTools = ['markdown', 'codeBlock', 'imageCompare', 'iconLinks', 'stepGuide', 'worldClock'] as const
export const dashboardAdvancedContentTools = ['imageCarousel', 'imageGallery', 'announcement', 'textTicker', 'tabsContent', 'accordion', 'quote', 'embed', 'actionButton', 'tagCloud', ...dashboardExtendedContentTools] as const
export const dashboardContentTools = ['text', 'heading', 'note', 'badge', 'image', 'video', 'audio', 'clock', 'link',
  'imageText', 'bulletList', 'linkList', 'countdown', 'icon', 'statusLegend', 'copyText', 'inspectionChecklist', 'definitionList',
  ...dashboardAdvancedContentTools, ...informationBlockKinds] as const
export const dashboardFilterTools = ['selectFilter', 'searchFilter', 'dateRangeFilter', 'clearFilters', 'multiValueFilter', 'numberRangeFilter', 'valueStateFilter', 'relativeDateFilter', 'booleanFilter'] as const
export type DashboardFilterTool = typeof dashboardFilterTools[number]
export function dashboardFilterTool(kind: DashboardWidgetKind): kind is DashboardFilterTool {
  return dashboardFilterTools.some(tool => tool === kind)
}
export const dashboardContainerTools = ['rowContainer', 'columnContainer', 'gridContainer', 'tabsContainer', 'stackContainer', 'splitContainer', 'headerContainer', 'responsiveContainer', 'footerContainer', 'sidebarContainer', 'scrollContainer', 'carouselContainer', 'spotlightContainer', 'accordionContainer', 'pagedGridContainer'] as const
export const dashboardLayoutTools = ['shape', 'container', 'panel', 'divider', 'verticalDivider', 'sectionDivider', 'spacer',
  'borderFrame', 'gridPanel', 'gradientPanel', 'horizontalArrow', 'verticalArrow', ...dashboardContainerTools] as const
export const dashboardAdvancedDecorationTools = ['doubleLine', 'dashedLine', 'chevrons', 'zigzag', 'waveLine', 'bracketPair', 'titlePlate', 'badgeSeal', 'hexagon', 'honeycomb', 'radarSweep', 'scanFrame', 'circuitPattern', 'orbitalRings', 'cornerFrame', 'sectionBanner', 'techRuler', 'bezierConnector'] as const
export const dashboardDecorationTools = ['glowLine', 'glowRing', 'dotMatrix', 'cornerAccent', 'ripple', 'ribbon', ...dashboardAdvancedDecorationTools] as const
export const dashboardScreenEffectTools = ['snow', 'rain', 'stars', 'meteors', 'fireflies', 'bubbles', 'confetti', 'petals', 'particleNetwork', 'aurora',
  'fog', 'smoke', 'clouds', 'sandstorm', 'embers', 'leaves', 'hearts', 'sparkles', 'lightning', 'fireworks', 'risingDigits', 'warpStars', 'oceanWaves', 'sunRays', 'matrixGrid', 'searchBeams', 'floatingHexagons', 'digitalScan', 'waterCaustics', 'lightTrails', 'pulseGrid', 'contourLines'] as const
export type DashboardScreenEffectKind = typeof dashboardScreenEffectTools[number]
export interface DashboardScreenEffectOptions {
  quantity: number
  speed: number
  size: number
  opacity: number
  wind: number
  layer: 'background' | 'foreground'
  enabled: boolean
}
export const dashboardScreenEffectPresets: Record<DashboardScreenEffectKind, { quantity: number; size: number; color: string; maxQuantity?: number; maxSize?: number }> = {
  snow: { quantity: 90, size: 3, color: '#B4CEDF' }, rain: { quantity: 130, size: 2, color: '#72AEE5' },
  stars: { quantity: 100, size: 2, color: '#E5B85C' }, meteors: { quantity: 12, size: 3, color: '#68CFE5' },
  fireflies: { quantity: 40, size: 3, color: '#8BCD58' }, bubbles: { quantity: 30, size: 10, color: '#5AB8D8' },
  confetti: { quantity: 80, size: 6, color: '#D98CB3' }, petals: { quantity: 50, size: 7, color: '#E998BA' },
  particleNetwork: { quantity: 60, size: 2, color: '#61BDB9' }, aurora: { quantity: 5, size: 80, color: '#3BBEAE' },
  fog: { quantity: 12, size: 110, color: '#A8BEC8', maxQuantity: 30, maxSize: 240 },
  smoke: { quantity: 25, size: 45, color: '#91A7B4', maxQuantity: 80, maxSize: 160 },
  clouds: { quantity: 8, size: 90, color: '#D1E1E8', maxQuantity: 20, maxSize: 200 },
  sandstorm: { quantity: 160, size: 2, color: '#D6B479' }, embers: { quantity: 60, size: 3, color: '#F4A35C' },
  leaves: { quantity: 40, size: 10, color: '#7CB479' }, hearts: { quantity: 30, size: 10, color: '#E89DB9' },
  sparkles: { quantity: 70, size: 5, color: '#E4C777' }, lightning: { quantity: 3, size: 2, color: '#A7CFEB', maxQuantity: 8 },
  fireworks: { quantity: 8, size: 5, color: '#F2B66E', maxQuantity: 24 }, risingDigits: { quantity: 60, size: 14, color: '#68B89F' },
  warpStars: { quantity: 100, size: 2, color: '#A5CBEC' },
  oceanWaves: { quantity: 5, size: 70, color: '#68B9D2', maxQuantity: 12, maxSize: 160 },
  matrixGrid: { quantity: 24, size: 1, color: '#68B9D2', maxQuantity: 60, maxSize: 4 },
  searchBeams: { quantity: 3, size: 60, color: '#68CFE5', maxQuantity: 8, maxSize: 120 },
  floatingHexagons: { quantity: 20, size: 18, color: '#61BDB9', maxQuantity: 80, maxSize: 60 },
  digitalScan: { quantity: 100, size: 12, color: '#68B89F', maxQuantity: 200, maxSize: 32 },
  sunRays: { quantity: 10, size: 40, color: '#E2C68E', maxQuantity: 24, maxSize: 160 },
  waterCaustics: { quantity: 18, size: 80, color: '#65D7DD', maxQuantity: 36, maxSize: 160 },
  lightTrails: { quantity: 24, size: 40, color: '#8AA9FF', maxQuantity: 80, maxSize: 120 },
  pulseGrid: { quantity: 14, size: 26, color: '#56C8AB', maxQuantity: 26, maxSize: 60 },
  contourLines: { quantity: 10, size: 35, color: '#9BAFF6', maxQuantity: 24, maxSize: 120 },
}
export const dashboardAnimationEffects = [
  'none', 'fade', 'slideUp', 'slideDown', 'slideLeft', 'slideRight', 'zoom', 'zoomOut', 'dropIn', 'bounceIn', 'blurIn',
  'wipeLeft', 'wipeRight', 'wipeUp', 'wipeDown', 'circleReveal', 'stretchIn', 'rollIn', 'rotate', 'flip', 'flipX',
  'pulse', 'heartbeat', 'bounce', 'float', 'shake', 'swing', 'rubberBand', 'wobble', 'jello', 'tada', 'glow', 'highlight',
] as const
export interface DashboardAnimation {
  effect: typeof dashboardAnimationEffects[number]
  duration: number
  delay: number
  repeat: boolean
  trigger: 'load' | 'hover'
  easing: 'linear' | 'ease-in' | 'ease-out' | 'ease-in-out'
}
export const dashboardAnimationDefaults: DashboardAnimation = { effect: 'none', duration: 1, delay: 0, repeat: false, trigger: 'load', easing: 'ease-out' }
export type DashboardChartOverrides = Partial<ChartMappings & Pick<QueryChartOptions, 'metric' | 'aggregation'>>
export const dashboardTools = [
  ...queryChartTypes, 'rank', 'metric', 'progress', ...dataDisplays,
  ...dashboardContentTools, ...dashboardFilterTools, ...dashboardLayoutTools, ...dashboardDecorationTools, ...dashboardScreenEffectTools,
] as const
export type DashboardTool = typeof dashboardTools[number]
export type DashboardWidgetKind = DashboardTool | 'report'
export type DashboardRefresh = 'saved' | '5m' | 'custom' | 'off'
export type DashboardAction = 'saved' | 'details' | 'filter' | 'none'

const widgetKinds = ['report', ...dashboardTools] as const
const toolLabelPrefix = 'dashboard.studio.tool.'
const chartLabelPrefix = 'queryChart.type.'
type WidgetTranslator = (key: string) => string

export function dashboardWidgetLabelKey(kind: DashboardWidgetKind): string {
  return (queryChartTypes.some(chart => chart === kind) ? chartLabelPrefix : toolLabelPrefix) + kind
}

function restoreWidgetName(name: string, translate?: WidgetTranslator): string {
  if (!translate) return name
  // Earlier editor versions could save an untranslated default title.
  const legacy = name.startsWith(toolLabelPrefix)
  if (!legacy && !name.startsWith(chartLabelPrefix)) return name
  const suffix = name.slice((legacy ? toolLabelPrefix : chartLabelPrefix).length)
  const kind = widgetKinds.find(value => value === suffix)
  if (!kind) return name
  const key = dashboardWidgetLabelKey(kind)
  return legacy || key === name ? translate(key) : name
}

export type DashboardInsets = [number, number, number, number]
export const dashboardPaddingEdges = ['top', 'right', 'bottom', 'left'] as const
export const dashboardRadiusCorners = ['topLeft', 'topRight', 'bottomRight', 'bottomLeft'] as const

export interface DashboardRect { x: number; y: number; w: number; h: number }
export const dashboardLegendTones = ['default', 'primary', 'info', 'success', 'warning', 'danger'] as const
export interface DashboardLinkItem { id: string; label: string; url: string; text?: string; image?: string; tone?: typeof dashboardLegendTones[number] }
export interface DashboardDeadline { date: string; hour: number; minute: number }
export interface DashboardDataOptions { year: number; periodDays: number; staleHours: number; baselineField: string; statusField: string; limit: number; decimals: number; prefix: string; suffix: string; positiveValues: string }
export interface DashboardContentOptions { comparePosition: number; language: 'text' | 'sql' | 'json'; timezone: string; activeIndex: number; direction: 'horizontal' | 'vertical'; fontSize: number; fontWeight: 'normal' | 'medium' | 'bold'; lineHeight: number; fit: 'cover' | 'contain'; interval: number; autoplay: boolean; attribution: string; pauseOnHover: boolean }
export interface DashboardLayoutOptions { itemHeight: number; columns: number; gap: number; padding: number; ratio: number; header: number; minWidth: number; pageSize: number }
export interface DashboardRelationOptions {
  rowField: string
  columnField: string
  entityField: string
  dimensionLimit: number
}
export const dashboardAuditRules = ['number', 'date', 'boolean'] as const
export interface DashboardAuditOptions {
  fields: string[]
  rule: typeof dashboardAuditRules[number]
  ignoreCase: boolean
  minGapDays: number
}
export interface DashboardTemporalOptions {
  windowDays: number
  historyDays: number
  year: number
  entityField: string
  endField: string
  labelField: string
  unit: 'minutes' | 'hours' | 'days'
}
export interface DashboardComparativeOptions {
  fields: string[]
  secondField: string
  groupField: string
  leftGroup: string
  rightGroup: string
  boundaries: string
  xThreshold: number | null
  yThreshold: number | null
  minSamples: number
}
export interface DashboardPlanningOptions {
  statusField: string
  labelField: string
  ownerField: string
  groupField: string
  completedField: string
  doneValues: string
  horizonDays: number
  includeCompleted: boolean
}
export interface DashboardSpatialOptions {
  latitudeField: string
  longitudeField: string
  groupField: string
  gridDegrees: number
  pointLimit: number
  precision: number
  west: number
  east: number
  south: number
  north: number
  referenceLat: number | null
  referenceLon: number | null
  distanceBands: string
}
export interface DashboardProcessOptions {
  entityField: string
  stageField: string
  requiredStages: string[]
  unit: 'minutes' | 'hours' | 'days'
}
export interface DashboardIntervalOptions {
  endField: string
  groupField: string
  unit: 'minutes' | 'hours' | 'days'
  durationBands: string
  minConcurrent: number
}
export interface DashboardDecorationOptions { count: number; rotation: number; filled: boolean; opacity: number; animate: boolean }
export const dashboardThresholdComparators = ['above', 'atLeast', 'below', 'atMost'] as const
export interface DashboardOperationsOptions {
  labelField: string; statusField: string; detailsField: string; endField: string;
  healthyValues: string; failureValues: string; comparator: typeof dashboardThresholdComparators[number];
  threshold: number; goal: number
}
export interface DashboardBusinessOptions {
  groupField: string
  subgroupField: string
  referenceField: string
  entityField: string
  stageOrder: string[]
  targetAggregation: 'sum' | 'max'
  period: 'day' | 'week'
  periods: number
  paretoCutoff: number
  boundaries: string
}
export interface DashboardWidget extends DashboardRect {
  id: string
  groupId?: string
  parentId?: string
  kind: DashboardWidgetKind
  name: string
  description: string
  reportCode?: string
  chartOptions?: DashboardChartOverrides
  text: string
  image: string
  url: string
  target: number
  shape: 'rectangle' | 'circle'
  color: string
  alignment: 'left' | 'center' | 'right'
  tone: 'info' | 'success' | 'warning' | 'danger'
  clockDate: boolean
  displayFields?: string[]
  recordIndex?: number
  timeField?: string
  links?: DashboardLinkItem[]
  deadline?: DashboardDeadline
  iconName?: string
  iconSize?: number
  secondaryColor?: string
  strokeWidth?: number
  gridSize?: number
  cornerSize?: number
  cornerPosition?: typeof dashboardRadiusCorners[number]
  animation?: DashboardAnimation
  screenEffect?: DashboardScreenEffectOptions
  dataOptions?: DashboardDataOptions
  operationsOptions?: DashboardOperationsOptions
  businessOptions?: DashboardBusinessOptions
  relationOptions?: DashboardRelationOptions
  auditOptions?: DashboardAuditOptions
  temporalOptions?: DashboardTemporalOptions
  comparativeOptions?: DashboardComparativeOptions
  planningOptions?: DashboardPlanningOptions
  spatialOptions?: DashboardSpatialOptions
  processOptions?: DashboardProcessOptions
  intervalOptions?: DashboardIntervalOptions
  processQualityOptions?: ProcessQualityOptions
  contentOptions?: DashboardContentOptions
  informationItems?: InformationItem[]
  layoutOptions?: DashboardLayoutOptions
  decorationOptions?: DashboardDecorationOptions
  refresh: DashboardRefresh
  refreshSeconds?: number
  action: DashboardAction
  field: string
  operation: 'count' | 'distinct' | 'sum' | 'avg' | 'min' | 'max'
  padding: DashboardInsets
  corners: DashboardInsets
  titleSize: number
  background: string
  border: boolean
  showCard: boolean
  showTitle: boolean
  movable: boolean
  resizable: boolean
  expandable: boolean
}

export interface DashboardBoard {
  widgets: DashboardWidget[]
  showTitle: boolean
  grid: boolean
  snap: boolean
  snapStep: number
  padding: number
  gap: number
  rowHeight: number
  background: string
  backgroundImage: string
  backgroundFit: 'cover' | 'contain' | 'stretch' | 'tile'
  backgroundPosition: 'center' | 'top' | 'bottom'
  backgroundOpacity: number
  backgroundBlur: number
  resolutionMode: 'auto' | 'fixed'
  width: number
  height: number
  fit: 'contain' | 'cover'
  theme: 'ocean' | 'jade'
  refresh: DashboardRefresh
  refreshSeconds?: number
}

export interface DashboardDesign {
  mode: DashboardMode
  name: string
  description: string
  boards: Record<DashboardMode, DashboardBoard>
}

export interface DashboardDocument {
  version: 3
  draft: DashboardDesign
  published?: DashboardDesign
}

export const dashboardColumns = 12
// Two older layouts can each contain 100 distinct widgets before they are combined.
const dashboardLayoutLimit = 200
export const dashboardCanvasNumbers = {
  padding: { min: 0, max: 64, initial: 12 },
  gap: { min: 0, max: 48, initial: 12 },
  rowHeight: { min: 24, max: 128, initial: 56 },
  snapStep: { min: 8, max: 96, initial: 16 },
} as const
export const dashboardWallPresets = ['1280x720', '1920x1080', '2560x1440', '3840x2160'].map(value => ({
  value, label: value.replace('x', ' × '),
}))

export function emptyDashboard(name = '', description = ''): DashboardDesign {
  const board = (): DashboardBoard => ({
    widgets: [], showTitle: false, grid: true, snap: true, resolutionMode: 'auto',
    snapStep: dashboardCanvasNumbers.snapStep.initial, padding: dashboardCanvasNumbers.padding.initial,
    gap: dashboardCanvasNumbers.gap.initial, rowHeight: dashboardCanvasNumbers.rowHeight.initial, background: '',
    backgroundImage: '', backgroundFit: 'cover', backgroundPosition: 'center', backgroundOpacity: 100, backgroundBlur: 0,
    width: 1920, height: 1080, fit: 'contain', theme: 'ocean', refresh: 'saved'
  })
  return { mode: 'dashboard', name, description, boards: { dashboard: board(), wall: board() } }
}

export function parseDashboardJson(value?: string): unknown {
  try { return JSON.parse(value || '') }
  catch { return undefined }
}

function record(value: unknown): Record<string, unknown> | undefined {
  return value !== null && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : undefined
}
function text(value: unknown, fallback = '') {
  return typeof value === 'string' ? value : fallback
}
function integer(value: unknown, fallback: number, min: number, max: number) {
  return typeof value === 'number' && Number.isFinite(value) ? Math.max(min, Math.min(max, Math.round(value))) : fallback
}

export function dashboardCanvasColor(value: unknown) {
  return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value : ''
}

export function dashboardCanvasNumber(key: keyof typeof dashboardCanvasNumbers, value: unknown) {
  const { initial, min, max } = dashboardCanvasNumbers[key]
  return integer(value, initial, min, max)
}

function readInsets(value: unknown, fallback: DashboardInsets, max: number): DashboardInsets {
  if (!Array.isArray(value)) return fallback
  return [
    integer(value[0], fallback[0], 0, max), integer(value[1], fallback[1], 0, max),
    integer(value[2], fallback[2], 0, max), integer(value[3], fallback[3], 0, max),
  ]
}

export function dashboardWidget(id: string, kind: DashboardWidgetKind, name: string, mode: DashboardMode = 'dashboard'): DashboardWidget {
  const decoration = ['borderFrame', 'gridPanel', 'gradientPanel', 'horizontalArrow', 'verticalArrow', 'icon', ...dashboardDecorationTools].some(tool => tool === kind)
  const screenEffect = dashboardScreenEffectTool(kind)
  return {
    id, kind, name, description: '', x: 0, y: 0, w: 6, h: 4,
    text: '', image: '', url: '', target: 100, shape: 'rectangle', color: '', alignment: 'left', tone: 'info', clockDate: true,
    refresh: 'saved', action: isProcessQualityView(kind) || dashboardIntervalTool(kind) || dashboardProcessTool(kind) || dashboardSpatialTool(kind) || dashboardPlanningTool(kind) || dashboardComparativeTool(kind) || dashboardTemporalTool(kind) || dashboardRelationTool(kind) || dashboardAuditTool(kind) && kind !== 'dateCoverage' ? 'filter' : 'saved',
    field: '', operation: ['metricGrid', 'metricComparison', 'comparisonTable'].includes(kind) ? 'sum' : 'count',
    padding: [10, 12, 10, 12], corners: mode === 'wall' ? [0, 0, 0, 0] : [12, 12, 12, 12], titleSize: 14, background: '',
    border: !['spacer', 'divider', 'verticalDivider', 'sectionDivider'].includes(kind),
    showCard: kind !== 'spacer' && !decoration && !screenEffect,
    showTitle: !screenEffect && !decoration && !['divider', 'verticalDivider', 'sectionDivider', 'spacer', 'container', 'panel', 'shape', 'link', 'heading', 'note', 'badge', 'copyText'].includes(kind),
    movable: !screenEffect, resizable: !screenEffect, expandable: false,
    screenEffect: screenEffect ? dashboardScreenEffectSettings(kind) : undefined,
    dataOptions: ['radialProgress', 'rollingTable', 'sparklineGrid'].includes(kind) ? dashboardDataSettings({ limit: kind === 'rollingTable' ? 3 : kind === 'sparklineGrid' ? 4 : 5 }) : undefined,
    layoutOptions: kind === 'spotlightContainer' ? dashboardLayoutSettings({ columns: 3, itemHeight: 112 })
      : kind === 'accordionContainer' ? dashboardLayoutSettings({ itemHeight: 200 })
      : kind === 'pagedGridContainer' ? dashboardLayoutSettings({ itemHeight: 112, columns: 3 }) : undefined,
    ...(kind === 'cornerFrame' ? { cornerSize: 32 } : {}),
  }
}

export function constrainDashboardRect(rect: DashboardRect, mode: DashboardMode, board: DashboardBoard): DashboardRect {
  const wall = mode === 'wall'
  const width = wall ? board.width : dashboardColumns
  const height = wall ? board.height : 1000
  const w = integer(rect.w, wall ? 320 : 4, wall ? 80 : 1, width)
  const h = integer(rect.h, wall ? 180 : 3, wall ? 64 : 1, height)
  return {
    x: integer(rect.x, 0, 0, width - w), y: integer(rect.y, 0, 0, height - h),
    w, h
  }
}

export function dashboardOverlap(a: DashboardRect, b: DashboardRect) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y
}

export function dashboardGridTarget(board: DashboardBoard, proposed: DashboardRect, widgetId?: string, relocate = true) {
  const target = constrainDashboardRect(proposed, 'dashboard', board)
  const fits = (rect: DashboardRect) => !board.widgets.some(widget => !widget.parentId && !dashboardScreenEffectTool(widget.kind) && widget.id !== widgetId && dashboardOverlap(widget, rect))
  if (fits(target)) return target
  if (!board.snap || !relocate) return

  // Try adjacent slots only, so a blocked drop never sends a card across the board.
  let nearest: DashboardRect | undefined
  let distance = Infinity
  for (let dy = -1; dy <= 1; dy++) {
    for (let dx = -1; dx <= 1; dx++) {
      const candidate = { ...target, x: target.x + dx, y: target.y + dy }
      if (candidate.x < 0 || candidate.x + candidate.w > dashboardColumns || candidate.y < 0 || candidate.y + candidate.h > 1000) continue
      const score = (candidate.x - proposed.x) ** 2 + (candidate.y - proposed.y) ** 2
      if (score >= distance || !fits(candidate)) continue
      nearest = candidate
      distance = score
    }
  }
  return nearest
}

export function dashboardPlacement(board: DashboardBoard, mode: DashboardMode, kind: DashboardWidgetKind): DashboardRect {
  if (dashboardScreenEffectTool(kind)) return { x: 0, y: 0, w: 3, h: 2 }
  const widgets = board.widgets.filter(widget => !widget.parentId && !dashboardScreenEffectTool(widget.kind))
  const small = dashboardFilterTool(kind) || ['metric', 'progress', 'clock', 'link', 'shape', 'text', 'image', 'badge', 'audio', 'spacer', 'countdown', 'icon', 'copyText', 'dataFreshness'].includes(kind)
  const wide = ['table', 'pivot', 'divider', 'sectionDivider', 'container'].includes(kind)
  const short = ['divider', 'sectionDivider', 'spacer', 'horizontalArrow'].includes(kind)
  if (mode === 'wall') {
    const w = kind === 'cornerAccent' ? 144 : Math.round(board.width * (kind === 'verticalDivider' ? .05 : small ? .22 : wide ? .54 : .45))
    const h = kind === 'cornerAccent' ? 144 : Math.round(board.height * (short ? .06 : small ? .15 : .36))
    const stepX = Math.max(16, Math.floor(board.width / 12))
    const stepY = Math.max(16, Math.floor(board.height / 12))
    for (let y = 0; y + h <= board.height; y += stepY) {
      for (let x = 0; x + w <= board.width; x += stepX) {
        const rect = { x, y, w, h }
        if (!widgets.some(widget => dashboardOverlap(widget, rect))) return rect
      }
    }
    const offset = widgets.length * 16
    return constrainDashboardRect({ x: offset, y: offset, w, h }, mode, board)
  }
  const w = kind === 'cornerAccent' ? 2 : kind === 'verticalDivider' ? 1 : small ? 3 : wide ? 12 : 6
  const h = kind === 'cornerAccent' ? 2 : short ? 1 : small ? 2 : 4
  const bottom = Math.max(0, ...widgets.map(widget => widget.y + widget.h))
  for (let y = 0; y <= bottom; y++) {
    for (let x = 0; x + w <= dashboardColumns; x++) {
      const rect = { x, y, w, h }
      if (!widgets.some(widget => dashboardOverlap(widget, rect))) return rect
    }
  }
  return { x: 0, y: bottom, w, h }
}

export function switchDashboardLayout(design: DashboardDesign): number {
  const mode = design.mode === 'wall' ? 'dashboard' : 'wall'
  const source = design.boards[design.mode]
  const target = design.boards[mode]
  const ids = new Set(target.widgets.map(widget => widget.id))
  const toWall = mode === 'wall'
  const grid = toWall ? source : target
  const wall = toWall ? target : source
  const visibleRows = Math.max(1, Math.round(wall.height / (grid.rowHeight + grid.gap)))
  const gridRows = toWall ? Math.max(visibleRows, ...source.widgets.filter(widget => !widget.parentId && !dashboardScreenEffectTool(widget.kind)).map(widget => widget.y + widget.h)) : visibleRows
  const scaleX = toWall ? target.width / dashboardColumns : dashboardColumns / source.width
  const scaleY = toWall ? target.height / gridRows : gridRows / source.height
  let skipped = 0

  for (const widget of source.widgets) {
    if (ids.has(widget.id)) continue
    if (target.widgets.length >= dashboardLayoutLimit) { skipped++; continue }
    if (dashboardScreenEffectTool(widget.kind) || widget.parentId) { target.widgets.push({ ...widget, groupId: undefined }); continue }
    const x = Math.round(widget.x * scaleX)
    const y = Math.round(widget.y * scaleY)
    let rect = constrainDashboardRect({ x, y, w: Math.round((widget.x + widget.w) * scaleX) - x,
      h: Math.round((widget.y + widget.h) * scaleY) - y }, mode, target)
    const overlaps = (candidate: DashboardRect) => target.widgets.some(other => !other.parentId && !dashboardScreenEffectTool(other.kind) && dashboardOverlap(other, candidate))
    if (overlaps(rect)) {
      rect = constrainDashboardRect(dashboardPlacement(target, mode, widget.kind), mode, target)
      if (!toWall && overlaps(rect)) { skipped++; continue }
    }
    target.widgets.push({ ...widget, ...rect, groupId: undefined })
  }
  design.mode = mode
  return skipped
}

export function dashboardGroupBounds(widgets: DashboardWidget[]): DashboardRect {
  const x = Math.min(...widgets.map(widget => widget.x))
  const y = Math.min(...widgets.map(widget => widget.y))
  return { x, y, w: Math.max(...widgets.map(widget => widget.x + widget.w)) - x,
    h: Math.max(...widgets.map(widget => widget.y + widget.h)) - y }
}

export function constrainDashboardGroup(rect: DashboardRect, members: DashboardWidget[], board: DashboardBoard): DashboardRect {
  const bounds = dashboardGroupBounds(members)
  const w = Math.max(rect.w, Math.ceil(bounds.w * Math.max(...members.map(widget => 80 / widget.w))))
  const h = Math.max(rect.h, Math.ceil(bounds.h * Math.max(...members.map(widget => 64 / widget.h))))
  return constrainDashboardRect({ ...rect, w, h }, 'wall', board)
}

export function resizeDashboardGroup(members: DashboardWidget[], rect: DashboardRect) {
  const bounds = dashboardGroupBounds(members)
  return new Map(members.map(widget => {
    const x = Math.round((widget.x - bounds.x) * rect.w / bounds.w)
    const y = Math.round((widget.y - bounds.y) * rect.h / bounds.h)
    const right = Math.round((widget.x + widget.w - bounds.x) * rect.w / bounds.w)
    const bottom = Math.round((widget.y + widget.h - bounds.y) * rect.h / bounds.h)
    return [widget.id, { x: rect.x + x, y: rect.y + y, w: right - x, h: bottom - y }]
  }))
}

export function groupDashboardWidgets(widgets: DashboardWidget[], ids: string[], groupId: string): DashboardWidget[] {
  const members = widgets.filter(widget => ids.includes(widget.id) && !widget.groupId && !widget.parentId && !dashboardScreenEffectTool(widget.kind))
  if (members.length < 2) return widgets
  return widgets.flatMap(widget => !members.includes(widget) ? [widget]
    : widget === members.at(-1) ? members.map(member => ({ ...member, groupId })) : [])
}

export function dashboardLayerMembers(widgets: DashboardWidget[], id: string) {
  return widgets.filter(widget => widget.id === id || widget.groupId === id)
}

export function reorderDashboardLayers(widgets: DashboardWidget[], from: string, to: string, above: boolean) {
  const scope = (id: string) => { const widget = widgets.find(item => item.id === id); return widget?.groupId ?? widget?.parentId }
  if (from === to || scope(from) !== scope(to)) return widgets
  const moving = dashboardLayerMembers(widgets, from)
  const destination = dashboardLayerMembers(widgets, to)
  if (!moving.length || !destination.length) return widgets
  const order = widgets.filter(widget => !moving.includes(widget))
  const insertAt = order.indexOf(above ? destination[destination.length - 1] : destination[0]) + (above ? 1 : 0)
  order.splice(insertAt, 0, ...moving)
  return order.every((widget, index) => widget === widgets[index]) ? widgets : order
}

export function dashboardContainerTool(kind: DashboardWidgetKind) { return dashboardContainerTools.some(tool => tool === kind) }
export function dashboardDescendantIds(widgets: DashboardWidget[], ids: Iterable<string>): Set<string> {
  const result = new Set(ids)
  let previous = -1
  while (previous !== result.size) {
    previous = result.size
    for (const widget of widgets) if (widget.parentId && result.has(widget.parentId)) result.add(widget.id)
  }
  return result
}
export function dashboardCanContain(widgets: DashboardWidget[], parentId: string, childId: string) {
  const parent = widgets.find(widget => widget.id === parentId)
  const child = widgets.find(widget => widget.id === childId)
  if (!parent || !child || !dashboardContainerTool(parent.kind) || child.groupId || dashboardScreenEffectTool(child.kind)
      || dashboardDescendantIds(widgets, [childId]).has(parentId)) return false
  let depth = 1
  let ancestor = parent
  const visited = new Set<string>()
  while (ancestor.parentId && !visited.has(ancestor.id)) {
    visited.add(ancestor.id)
    depth++
    const next = widgets.find(widget => widget.id === ancestor.parentId)
    if (!next) break
    ancestor = next
  }
  const descendants = dashboardDescendantIds(widgets, [childId])
  let childDepth = dashboardContainerTool(child.kind) ? 2 : 1
  for (const id of descendants) {
    let current = widgets.find(widget => widget.id === id)
    let distance = 1
    while (current?.parentId && descendants.has(current.parentId) && distance <= 4) {
      distance++
      current = widgets.find(widget => widget.id === current?.parentId)
    }
    childDepth = Math.max(childDepth, distance)
  }
  return depth + childDepth <= 4
}
export function dashboardCardStyle(widget: DashboardWidget) {
  return widget.showCard ? { borderRadius: widget.corners.map(value => value + 'px').join(' '),
    padding: widget.padding.map(value => value + 'px').join(' '), backgroundColor: widget.background || undefined } : {}
}
export function dashboardDataSettings(value?: unknown): DashboardDataOptions {
  const raw = record(value) ?? {}
  return { year: integer(raw.year, 0, 0, 9999), periodDays: integer(raw.periodDays, 7, 1, 90), staleHours: integer(raw.staleHours, 24, 1, 8760),
    baselineField: text(raw.baselineField), statusField: text(raw.statusField), limit: integer(raw.limit, 10, 1, 100),
    decimals: integer(raw.decimals, 2, 0, 8), prefix: text(raw.prefix).slice(0, 30), suffix: text(raw.suffix).slice(0, 30),
    positiveValues: text(raw.positiveValues, 'SUCCESS,ONLINE,ACTIVE,正常,成功,true,1').slice(0, 500) }
}
export function dashboardOperationsSettings(kind?: DashboardWidgetKind, value?: unknown): DashboardOperationsOptions {
  const raw = record(value) ?? {}
  const bounded = (input: unknown, fallback: number, min: number, max: number) =>
    typeof input === 'number' && Number.isFinite(input) ? Math.max(min, Math.min(max, input)) : fallback
  return {
    labelField: text(raw.labelField).slice(0, 500), statusField: text(raw.statusField).slice(0, 500),
    detailsField: text(raw.detailsField).slice(0, 500), endField: text(raw.endField).slice(0, 500),
    healthyValues: text(raw.healthyValues, 'success,succeeded,ok,passed,healthy,成功,正常').slice(0, 500),
    failureValues: text(raw.failureValues, 'error,failed,critical,breach,失败,异常,严重').slice(0, 500),
    comparator: dashboardThresholdComparators.find(item => item === raw.comparator) ?? (kind === 'slaScorecard' ? 'atMost' : 'above'),
    threshold: bounded(raw.threshold, 100, -1e12, 1e12), goal: bounded(raw.goal, 99, 0, 100),
  }
}
export function dashboardBusinessSettings(value?: unknown): DashboardBusinessOptions {
  const raw = record(value) ?? {}
  return {
    groupField: text(raw.groupField).slice(0, 500), subgroupField: text(raw.subgroupField).slice(0, 500),
    referenceField: text(raw.referenceField).slice(0, 500), entityField: text(raw.entityField).slice(0, 500),
    stageOrder: Array.isArray(raw.stageOrder) ? [...new Set(raw.stageOrder.filter((item): item is string => typeof item === 'string' && !!item.trim()).map(item => item.slice(0, 500)))].slice(0, 12) : [],
    targetAggregation: raw.targetAggregation === 'max' ? 'max' : 'sum',
    period: raw.period === 'day' ? 'day' : 'week', periods: integer(raw.periods, 6, 2, 12),
    paretoCutoff: integer(raw.paretoCutoff, 80, 1, 100), boundaries: text(raw.boundaries, '0, 25, 50, 100').slice(0, 400),
  }
}
export function dashboardContentSettings(value?: unknown): DashboardContentOptions {
  const raw = record(value) ?? {}
  let timezone = text(raw.timezone, 'UTC').slice(0,100)
  try { new Intl.DateTimeFormat('en', { timeZone: timezone }) } catch { timezone = 'UTC' }
  return { comparePosition: integer(raw.comparePosition, 50, 0, 100), language: raw.language === 'sql' || raw.language === 'json' ? raw.language : 'text', timezone,
    activeIndex: integer(raw.activeIndex, 0, 0, 99), direction: raw.direction === 'horizontal' ? 'horizontal' : 'vertical',
    fontSize: integer(raw.fontSize, 16, 10, 128), fontWeight: raw.fontWeight === 'bold' || raw.fontWeight === 'medium' ? raw.fontWeight : 'normal',
    lineHeight: typeof raw.lineHeight === 'number' && Number.isFinite(raw.lineHeight) ? Math.max(1, Math.min(3, raw.lineHeight)) : 1.6,
    fit: raw.fit === 'contain' ? 'contain' : 'cover', interval: integer(raw.interval, 5, 2, 60), autoplay: raw.autoplay !== false,
    attribution: text(raw.attribution).slice(0, 200), pauseOnHover: raw.pauseOnHover !== false }
}
export function dashboardLayoutSettings(value?: unknown): DashboardLayoutOptions {
  const raw = record(value) ?? {}
  return { itemHeight: integer(raw.itemHeight, 180, 48, 800), columns: integer(raw.columns, 2, 1, 6), gap: integer(raw.gap, 12, 0, 48), padding: integer(raw.padding, 8, 0, 64),
    ratio: integer(raw.ratio, 40, 10, 90), header: integer(raw.header, 25, 10, 70), minWidth: integer(raw.minWidth, 220, 80, 600),
    pageSize: integer(raw.pageSize, 6, 1, 24) }
}
export function dashboardRelationSettings(value?: unknown): DashboardRelationOptions {
  const raw = record(value) ?? {}
  return {
    rowField: text(raw.rowField).slice(0, 500),
    columnField: text(raw.columnField).slice(0, 500),
    entityField: text(raw.entityField).slice(0, 500),
    dimensionLimit: integer(raw.dimensionLimit, 8, 2, 16),
  }
}
export function dashboardAuditSettings(value?: unknown): DashboardAuditOptions {
  const raw = record(value) ?? {}
  const fields = Array.isArray(raw.fields) ? raw.fields.filter((field): field is string => typeof field === 'string' && !!field.trim()) : []
  return {
    fields: [...new Set(fields.map(field => field.slice(0, 500)))].slice(0, 8),
    rule: dashboardAuditRules.find(rule => rule === raw.rule) ?? 'number',
    ignoreCase: raw.ignoreCase === true,
    minGapDays: integer(raw.minGapDays, 1, 1, 365),
  }
}
export function dashboardTemporalSettings(value?: unknown): DashboardTemporalOptions {
  const raw = record(value) ?? {}
  return {
    windowDays: integer(raw.windowDays, 7, 2, 90),
    historyDays: integer(raw.historyDays, 90, 7, 366),
    year: integer(raw.year, 0, 0, 9999),
    entityField: text(raw.entityField).slice(0, 500),
    endField: text(raw.endField).slice(0, 500),
    labelField: text(raw.labelField).slice(0, 500),
    unit: raw.unit === 'minutes' || raw.unit === 'days' ? raw.unit : 'hours',
  }
}
export function dashboardComparativeSettings(value?: unknown): DashboardComparativeOptions {
  const raw = record(value) ?? {}
  const fields = Array.isArray(raw.fields) ? raw.fields.filter((field): field is string => typeof field === 'string' && !!field.trim()) : []
  const threshold = (input: unknown) => typeof input === 'number' && Number.isFinite(input) ? input : null
  return {
    fields: [...new Set(fields.map(field => field.slice(0, 500)))].slice(0, 8),
    secondField: text(raw.secondField).slice(0, 500),
    groupField: text(raw.groupField).slice(0, 500),
    leftGroup: text(raw.leftGroup).slice(0, 500),
    rightGroup: text(raw.rightGroup).slice(0, 500),
    boundaries: text(raw.boundaries, '0, 25, 50, 100').slice(0, 400),
    xThreshold: threshold(raw.xThreshold),
    yThreshold: threshold(raw.yThreshold),
    minSamples: integer(raw.minSamples, 3, 2, 100),
  }
}
export function dashboardPlanningSettings(value?: unknown): DashboardPlanningOptions {
  const raw = record(value) ?? {}
  return {
    statusField: text(raw.statusField).slice(0, 500),
    labelField: text(raw.labelField).slice(0, 500),
    ownerField: text(raw.ownerField).slice(0, 500),
    groupField: text(raw.groupField).slice(0, 500),
    completedField: text(raw.completedField).slice(0, 500),
    doneValues: text(raw.doneValues, 'done, completed, closed, 已完成, 完成').slice(0, 400),
    horizonDays: integer(raw.horizonDays, 14, 1, 90),
    includeCompleted: raw.includeCompleted === true,
  }
}
export function dashboardSpatialSettings(value?: unknown): DashboardSpatialOptions {
  const raw = record(value) ?? {}
  function bounded(input: unknown, min: number, max: number) {
    return typeof input === 'number' && Number.isFinite(input) ? Math.max(min, Math.min(max, input)) : null
  }
  return {
    latitudeField: text(raw.latitudeField).slice(0, 500),
    longitudeField: text(raw.longitudeField).slice(0, 500),
    groupField: text(raw.groupField).slice(0, 500),
    gridDegrees: bounded(raw.gridDegrees, .01, 60) ?? 1,
    pointLimit: integer(raw.pointLimit, 500, 20, 2000),
    precision: integer(raw.precision, 3, 0, 6),
    west: bounded(raw.west, -180, 180) ?? -180,
    east: bounded(raw.east, -180, 180) ?? 180,
    south: bounded(raw.south, -90, 90) ?? -90,
    north: bounded(raw.north, -90, 90) ?? 90,
    referenceLat: bounded(raw.referenceLat, -90, 90),
    referenceLon: bounded(raw.referenceLon, -180, 180),
    distanceBands: text(raw.distanceBands, '1, 5, 10, 50, 100').slice(0, 400),
  }
}
export function dashboardProcessSettings(value?: unknown): DashboardProcessOptions {
  const raw = record(value) ?? {}
  const stages = Array.isArray(raw.requiredStages) ? raw.requiredStages.filter((stage): stage is string => typeof stage === 'string' && !!stage.trim()) : []
  return {
    entityField: text(raw.entityField).slice(0, 500),
    stageField: text(raw.stageField).slice(0, 500),
    requiredStages: [...new Set(stages.map(stage => stage.slice(0, 500)))].slice(0, 12),
    unit: raw.unit === 'minutes' || raw.unit === 'days' ? raw.unit : 'hours',
  }
}
export function dashboardIntervalSettings(value?: unknown): DashboardIntervalOptions {
  const raw = record(value) ?? {}
  return {
    endField: text(raw.endField).slice(0, 500),
    groupField: text(raw.groupField).slice(0, 500),
    unit: raw.unit === 'minutes' || raw.unit === 'days' ? raw.unit : 'hours',
    durationBands: text(raw.durationBands, '0.5, 1, 2, 4, 8').slice(0, 400),
    minConcurrent: integer(raw.minConcurrent, 2, 2, 20),
  }
}
export function dashboardDecorationSettings(value?: unknown): DashboardDecorationOptions {
  const raw = record(value) ?? {}
  return { count: integer(raw.count, 6, 2, 24), rotation: integer(raw.rotation, 0, -180, 180), filled: raw.filled === true, animate: raw.animate !== false,
    opacity: integer(raw.opacity, 100, 0, 100) }
}

function readWidgetChartOptions(value: unknown): DashboardChartOverrides | undefined {
  const raw = record(value)
  if (!raw) return
  const chartFields = record(raw.chartFields)
  return {
    category: typeof raw.category === 'string' ? raw.category : undefined,
    series: typeof raw.series === 'string' ? raw.series : undefined,
    metric: typeof raw.metric === 'string' ? raw.metric : undefined,
    aggregation: (['count', 'distinct', 'sum', 'avg', 'min', 'max'] as const).find(value => value === raw.aggregation),
    chartFields: chartFields && Object.fromEntries(Object.entries(chartFields).filter(([, field]) => typeof field === 'string')) as Record<string, string> | undefined,
    parallelFields: Array.isArray(raw.parallelFields) ? raw.parallelFields.filter(field => typeof field === 'string').slice(0, 12) : undefined,
    mapGeoJson: typeof raw.mapGeoJson === 'string' && raw.mapGeoJson.length <= geoJsonLimit ? raw.mapGeoJson : undefined,
  }
}

function readWidgetAnimation(value: unknown): DashboardAnimation | undefined {
  const raw = record(value)
  if (!raw) return
  const duration = typeof raw.duration === 'number' && Number.isFinite(raw.duration) ? Math.max(.1, Math.min(60, raw.duration)) : 1
  const delay = typeof raw.delay === 'number' && Number.isFinite(raw.delay) ? Math.max(0, Math.min(60, raw.delay)) : 0
  return {
    effect: dashboardAnimationEffects.find(effect => effect === raw.effect) ?? 'none', duration, delay, repeat: raw.repeat === true,
    trigger: raw.trigger === 'hover' ? 'hover' : 'load',
    easing: raw.easing === 'linear' || raw.easing === 'ease-in' || raw.easing === 'ease-in-out' ? raw.easing : 'ease-out',
  }
}

function parseBoard(value: unknown, mode: DashboardMode, translate?: WidgetTranslator): DashboardBoard {
  const defaults = emptyDashboard().boards[mode]
  const raw = record(value)
  if (!raw) return defaults
  const width = integer(raw.width, defaults.width, 640, 3840)
  const height = integer(raw.height, defaults.height, 360, 2160)
  const hasCustomSize = width !== defaults.width || height !== defaults.height
  const board: DashboardBoard = {
    widgets: [], showTitle: raw.showTitle === true, grid: raw.grid !== false, snap: raw.snap !== false,
    snapStep: dashboardCanvasNumber('snapStep', raw.snapStep), padding: dashboardCanvasNumber('padding', raw.padding),
    gap: dashboardCanvasNumber('gap', raw.gap), rowHeight: dashboardCanvasNumber('rowHeight', raw.rowHeight),
    background: dashboardCanvasColor(raw.background),
    backgroundImage: dashboardResourceUrl(text(raw.backgroundImage)),
    backgroundFit: raw.backgroundFit === 'contain' || raw.backgroundFit === 'stretch' || raw.backgroundFit === 'tile' ? raw.backgroundFit : 'cover',
    backgroundPosition: raw.backgroundPosition === 'top' || raw.backgroundPosition === 'bottom' ? raw.backgroundPosition : 'center',
    backgroundOpacity: integer(raw.backgroundOpacity, 100, 0, 100), backgroundBlur: integer(raw.backgroundBlur, 0, 0, 20),
    resolutionMode: raw.resolutionMode === 'auto' || raw.resolutionMode === 'fixed' ? raw.resolutionMode : hasCustomSize ? 'fixed' : 'auto',
    width, height,
    fit: raw.fit === 'cover' ? 'cover' : 'contain', theme: raw.theme === 'jade' ? 'jade' : 'ocean',
    refresh: readRefreshPolicy(raw.refresh),
    refreshSeconds: integer(raw.refreshSeconds, 300, 60, 86400),
  }
  if (!Array.isArray(raw.widgets)) return board
  const ids = new Set<string>()
  for (const [index, entry] of raw.widgets.slice(0, dashboardLayoutLimit).entries()) {
    const item = record(entry)
    if (!item) continue
    const kind = widgetKinds.find(value => value === item.kind)
    if (!kind) continue
    const id = text(item.id, 'widget-' + index)
    if (ids.has(id)) continue
    ids.add(id)
    const widget = dashboardWidget(id, kind, restoreWidgetName(text(item.name), translate), mode)
    const oldRadius = integer(item.radius, 12, 0, 28)
    const corner = mode === 'wall' && oldRadius === 12 ? 0 : oldRadius
    const deadline = record(item.deadline)
    board.widgets.push({
      ...widget,
      parentId: typeof item.parentId === 'string' ? item.parentId : undefined,
      groupId: mode === 'wall' && !dashboardScreenEffectTool(kind) && typeof item.groupId === 'string' ? item.groupId.slice(0, 100) || undefined : undefined,
      ...constrainDashboardRect({ x: Number(item.x), y: Number(item.y), w: Number(item.w), h: Number(item.h) }, mode, board),
      description: text(item.description).slice(0, 200),
      reportCode: text(item.reportCode) || undefined, chartOptions: readWidgetChartOptions(item.chartOptions), text: text(item.text), image: text(item.image), field: text(item.field),
      url: text(item.url), target: integer(item.target, 100, 1, 1000000000),
      shape: item.shape === 'circle' ? 'circle' : 'rectangle', clockDate: item.clockDate !== false,
      displayFields: Array.isArray(item.displayFields) ? [...new Set(item.displayFields.filter((field): field is string => typeof field === 'string'))].slice(0, 12) : undefined,
      recordIndex: typeof item.recordIndex === 'number' ? integer(item.recordIndex, 1, 1, 10000) : undefined,
      timeField: typeof item.timeField === 'string' ? item.timeField : undefined,
      links: Array.isArray(item.links) ? item.links.slice(0, 20).flatMap((value, index) => {
        const link = record(value)
        return link ? [{ id: text(link.id, 'link-' + index), label: text(link.label).slice(0, 100), url: text(link.url).slice(0, 2000),
          ...(typeof link.text === 'string' ? { text: link.text.slice(0, 5000) } : {}), ...(typeof link.image === 'string' ? { image: link.image.slice(0, 2000) } : {}),
          ...(kind === 'statusLegend' ? { tone: dashboardLegendTones.find(tone => tone === link.tone) || 'default' } : {}) }] : []
      }) : undefined,
      deadline: deadline ? { date: text(deadline.date).slice(0, 10),
        hour: integer(deadline.hour, 0, 0, 23), minute: integer(deadline.minute, 0, 0, 59) } : undefined,
      iconName: typeof item.iconName === 'string' ? item.iconName.slice(0, 60) : undefined,
      iconSize: typeof item.iconSize === 'number' ? integer(item.iconSize, 64, 16, 256) : undefined,
      secondaryColor: dashboardCanvasColor(item.secondaryColor) || undefined,
      strokeWidth: typeof item.strokeWidth === 'number' ? integer(item.strokeWidth, 2, 1, 12) : undefined,
      gridSize: typeof item.gridSize === 'number' ? integer(item.gridSize, 24, 8, 96) : undefined,
      cornerSize: typeof item.cornerSize === 'number' ? integer(item.cornerSize, 64, 16, 192) : undefined,
      cornerPosition: dashboardRadiusCorners.find(position => position === item.cornerPosition),
      animation: readWidgetAnimation(item.animation),
      screenEffect: dashboardScreenEffectTool(kind) ? dashboardScreenEffectSettings(kind, item.screenEffect) : undefined,
      dataOptions: record(item.dataOptions) ? dashboardDataSettings(item.dataOptions) : widget.dataOptions,
      operationsOptions: dashboardOperationDataTool(kind) ? dashboardOperationsSettings(kind, item.operationsOptions) : undefined,
      businessOptions: dashboardBusinessTool(kind) ? dashboardBusinessSettings(item.businessOptions) : undefined,
      relationOptions: dashboardRelationTool(kind) ? dashboardRelationSettings(item.relationOptions) : undefined,
      auditOptions: dashboardAuditTool(kind) ? dashboardAuditSettings(item.auditOptions) : undefined,
      temporalOptions: dashboardTemporalTool(kind) ? dashboardTemporalSettings(item.temporalOptions) : undefined,
      comparativeOptions: dashboardComparativeTool(kind) ? dashboardComparativeSettings(item.comparativeOptions) : undefined,
      planningOptions: dashboardPlanningTool(kind) ? dashboardPlanningSettings(item.planningOptions) : undefined,
      spatialOptions: dashboardSpatialTool(kind) ? dashboardSpatialSettings(item.spatialOptions) : undefined,
      processOptions: dashboardProcessTool(kind) ? dashboardProcessSettings(item.processOptions) : undefined,
      intervalOptions: dashboardIntervalTool(kind) ? dashboardIntervalSettings(item.intervalOptions) : undefined,
      processQualityOptions: isProcessQualityView(kind) ? readProcessQualityOptions(item.processQualityOptions) : undefined,
      contentOptions: record(item.contentOptions) ? dashboardContentSettings(item.contentOptions) : undefined,
      informationItems: isInformationBlock(kind) ? readInformationItems(item.informationItems) : undefined,
      layoutOptions: record(item.layoutOptions) ? dashboardLayoutSettings(item.layoutOptions) : widget.layoutOptions,
      decorationOptions: record(item.decorationOptions) ? dashboardDecorationSettings(item.decorationOptions) : undefined,
      color: /^#[0-9a-f]{6}$/i.test(text(item.color)) ? text(item.color) : '',
      alignment: item.alignment === 'center' || item.alignment === 'right' ? item.alignment : 'left',
      tone: item.tone === 'success' || item.tone === 'warning' || item.tone === 'danger' ? item.tone : 'info',
      refresh: readRefreshPolicy(item.refresh),
      refreshSeconds: integer(item.refreshSeconds, 300, 60, 86400),
      action: item.action === 'details' || item.action === 'filter' || item.action === 'none' || item.action === 'saved' ? item.action : widget.action,
      operation: ['distinct', 'sum', 'avg', 'min', 'max'].includes(String(item.operation)) ? item.operation as DashboardWidget['operation'] : 'count',
      padding: readInsets(item.padding, widget.padding, 96),
      corners: readInsets(item.corners, [corner, corner, corner, corner], 96),
      titleSize: integer(item.titleSize, 14, 10, 30),
      background: /^#[0-9a-f]{6}$/i.test(text(item.background)) ? text(item.background) : '',
      border: typeof item.border === 'boolean' ? item.border : widget.border,
      showCard: typeof item.showCard === 'boolean' ? item.showCard : widget.showCard,
      showTitle: typeof item.showTitle === 'boolean' ? item.showTitle : widget.showTitle, movable: item.movable !== false,
      resizable: item.resizable !== false, expandable: item.expandable === true
    })
  }
  const groupCounts = new Map<string, number>()
  for (const widget of board.widgets) if (widget.groupId) groupCounts.set(widget.groupId, (groupCounts.get(widget.groupId) ?? 0) + 1)
  for (const widget of board.widgets) {
    if (widget.groupId && (ids.has(widget.groupId) || groupCounts.get(widget.groupId) === 1)) widget.groupId = undefined
  }
  for (const widget of board.widgets) {
    if (!widget.parentId) continue
    const parentId = widget.parentId
    widget.parentId = undefined
    if (dashboardCanContain(board.widgets, parentId, widget.id)) { widget.parentId = parentId; widget.groupId = undefined }
  }
  return board
}

function readRefreshPolicy(value: unknown): DashboardRefresh {
  return value === '5m' || value === 'custom' || value === 'off' ? value : 'saved'
}

function parseDesign(value: unknown, name: string, description: string, translate?: WidgetTranslator): DashboardDesign {
  const raw = record(value)
  const boards = record(raw?.boards)
  return {
    mode: raw?.mode === 'wall' ? 'wall' : 'dashboard', name: text(raw?.name, name),
    description: text(raw?.description, description),
    boards: { dashboard: parseBoard(boards?.dashboard, 'dashboard', translate), wall: parseBoard(boards?.wall, 'wall', translate) }
  }
}

export function readDashboardDocument(configure?: string, name = '', description = '', translate?: WidgetTranslator): DashboardDocument {
  const value = parseDashboardJson(configure)
  const raw = record(value)
  if (raw?.version === 3) {
    return {
      version: 3, draft: parseDesign(raw.draft, name, description, translate),
      published: record(raw.published) ? parseDesign(raw.published, name, description, translate) : undefined
    }
  }
  const draft = emptyDashboard(name, description)
  if (Array.isArray(value)) {
    const widgets = value.flatMap((entry, index) => {
      const item = record(entry)
      const original = record(item?.original)
      const node = record(item?.node)
      const code = text(original?.code) || text(node?.id)
      if (!item || !code) return []
      return [{ ...dashboardWidget('legacy-' + index, 'report', text(item.title, text(original?.name))), ...item, id: 'legacy-' + index, kind: 'report', reportCode: code }]
    })
    draft.boards.dashboard = parseBoard({ widgets }, 'dashboard', translate)
  }
  return { version: 3, draft, published: configure ? copyDashboardDesign(draft) : undefined }
}

export function copyDashboardDesign(design: DashboardDesign): DashboardDesign {
  return JSON.parse(JSON.stringify(design)) as DashboardDesign
}

export function dashboardReportCodes(document: DashboardDocument): string[] {
  const codes = new Set<string>()
  for (const design of [document.draft, document.published]) {
    if (!design) continue
    for (const board of Object.values(design.boards)) {
      for (const widget of board.widgets) if (widget.reportCode) codes.add(widget.reportCode)
    }
  }
  return [...codes]
}

export function dashboardSnapshot(report?: ReportModel): QueryChartConfiguration | undefined {
  const config = record(parseDashboardJson(report?.configure))
  const saved = record(config?.queryChart)
  const options = record(saved?.options)
  if (saved?.version !== 1 || !options || !Array.isArray(options.fields) || !Array.isArray(options.groups)
      || !Array.isArray(config?.columns) || !Array.isArray(config?.headers)) return
  return config as unknown as QueryChartConfiguration
}

export function dashboardChartType(kind: DashboardWidgetKind): QueryChartType | undefined {
  if (kind === 'rank') return 'horizontal'
  return queryChartTypes.find(chart => chart === kind)
}

export function dashboardChartMappings(widget: DashboardWidget, snapshot?: QueryChartConfiguration): ChartMappings & { type: QueryChartType } {
  const saved = snapshot?.queryChart.options
  const type = dashboardChartType(widget.kind) ?? saved?.type ?? 'table'
  const suggestions = suggestChartFields(type, saved?.fields.filter(name => chartFieldKind(snapshot?.columns ?? [], name) === 'number') ?? [])
  return {
    type,
    category: widget.chartOptions?.category ?? saved?.category ?? '',
    series: widget.chartOptions?.series ?? (widget.kind === 'graph' ? widget.field || saved?.series : saved?.series) ?? '',
    chartFields: { ...suggestions.chartFields, ...saved?.chartFields, ...widget.chartOptions?.chartFields },
    parallelFields: widget.chartOptions?.parallelFields ?? saved?.parallelFields ?? suggestions.parallelFields,
    mapGeoJson: widget.chartOptions?.mapGeoJson ?? saved?.mapGeoJson,
  }
}

export function dashboardMetricTool(kind: DashboardWidgetKind) {
  return kind === 'metric' || kind === 'progress' || kind === 'gauge' || kind === 'metricTicker' || kind === 'metricComparison' || kind === 'bulletMetric' ||
    ['comparisonTable', 'calendarHeatmap', 'radialProgress', 'thresholdCards', 'periodComparison', 'sparklineGrid'].includes(kind)
}

export function dashboardDataTool(kind: DashboardWidgetKind): kind is DashboardDataTool {
  return dataDisplays.some(tool => tool === kind)
}

export function dashboardDataAggregationTool(kind: DashboardWidgetKind) {
  return kind === 'pivot' || kind === 'dataList' || kind === 'progressList' || kind === 'dataTags' || kind === 'metricTrend' || kind === 'rankList' || kind === 'distributionList'
}

export function dashboardDeadlineTime(deadline?: DashboardDeadline) {
  if (!deadline || !/^\d{4}-\d{2}-\d{2}$/.test(deadline.date)) return
  const [year, month, day] = deadline.date.split('-').map(Number)
  const date = new Date(year, month - 1, day, deadline.hour, deadline.minute)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day
      || date.getHours() !== deadline.hour || date.getMinutes() !== deadline.minute) return
  return date.getTime()
}

export function dashboardPivot(points: QueryChartPoint[], rowLabel: string) {
  const groups = [...new Set(points.map(point => point.series))]
  if (groups.length > 50) return
  const categories = new Map<string, Map<string, number | null>>()
  for (const point of points) {
    if (!categories.has(point.category)) categories.set(point.category, new Map())
    categories.get(point.category)?.set(point.series, point.value)
  }
  return {
    headers: [rowLabel, ...groups],
    rows: [...categories].map(([name, cells]) => [name, ...groups.map(group => cells.get(group) ?? null)]),
  }
}

export function dashboardWidgetUsesAsset(kind: DashboardWidgetKind) {
  return ![...dashboardContentTools, ...dashboardLayoutTools, ...dashboardDecorationTools, ...dashboardScreenEffectTools].some(tool => tool === kind)
}

export function dashboardScreenEffectTool(kind: DashboardWidgetKind): kind is DashboardScreenEffectKind {
  return dashboardScreenEffectTools.some(tool => tool === kind)
}

export function dashboardScreenEffectQuantityLimit(kind: DashboardScreenEffectKind) {
  return dashboardScreenEffectPresets[kind].maxQuantity ?? (kind === 'aurora' ? 12 : kind === 'particleNetwork' ? 120 : 300)
}
export function dashboardScreenEffectSizeLimit(kind: DashboardScreenEffectKind) { return dashboardScreenEffectPresets[kind].maxSize ?? (kind === 'aurora' ? 160 : 32) }

export function dashboardScreenEffectSettings(kind: DashboardScreenEffectKind, value?: unknown): DashboardScreenEffectOptions {
  const raw = record(value) ?? {}
  const defaults = dashboardScreenEffectPresets[kind]
  return {
    quantity: integer(raw.quantity, defaults.quantity, 1, dashboardScreenEffectQuantityLimit(kind)),
    speed: integer(raw.speed, 100, 10, 300), size: integer(raw.size, defaults.size, 1, dashboardScreenEffectSizeLimit(kind)),
    opacity: integer(raw.opacity, 65, 0, 100), wind: integer(raw.wind, 15, -100, 100),
    layer: raw.layer === 'background' ? 'background' : 'foreground', enabled: raw.enabled !== false,
  }
}

export function dashboardMotionStyle(widget: Pick<DashboardWidget, 'animation'>) {
  const animation = widget.animation
  return animation ? {
    '--widget-motion-name': 'mf-dashboard-' + animation.effect, '--widget-motion-duration': animation.duration + 's',
    '--widget-motion-delay': animation.delay + 's', '--widget-motion-repeat': animation.repeat ? 'infinite' : '1',
    '--widget-motion-easing': animation.easing,
  } : {}
}

export function dashboardWallResolution(value: string | number | undefined): Partial<DashboardBoard> | undefined {
  if (value === 'auto') return { resolutionMode: 'auto' }
  if (!dashboardWallPresets.some(preset => preset.value === value)) return
  const [width, height] = String(value).split('x').map(Number)
  return { resolutionMode: 'fixed', width, height }
}

export function dashboardResourceUrl(value: string): string {
  const url = value.trim()
  if (/^\/(?![\/\\])/.test(url) && !/[\r\n\t]/.test(url)) return url
  try {
    const parsed = new URL(url)
    if (parsed.protocol === 'https:' || parsed.protocol === 'http:') return parsed.href
  }
  catch { /* Leave the component unconfigured until the address is valid. */ }
  return ''
}

export function dashboardRowsInPeriod(rows: QueryChartRow[], field: string, days: number, now = Date.now()) {
  if (!field || !days) return rows
  const today = new Date(now)
  today.setHours(0, 0, 0, 0)
  const start = new Date(today)
  start.setDate(start.getDate() - days + 1)
  const end = new Date(today)
  end.setDate(end.getDate() + 1)
  return rows.filter(row => {
    const date = row[field]
    const time = typeof date === 'string' ? new Date(date.length === 10 ? date + 'T00:00:00' : date.replace(' ', 'T')).getTime() : NaN
    return time >= start.getTime() && time < end.getTime()
  })
}

export function dashboardMetric(rows: QueryChartRow[], field: string, operation: DashboardWidget['operation']): number | null {
  if (operation === 'count') return rows.length
  if (!field) return null
  const values = rows.map(row => row[field]).filter(value => value != null && value !== '')
  if (operation === 'distinct') return new Set(values.map(value => typeof value === 'object' ? JSON.stringify(value) : value)).size
  const numbers = values.flatMap(value => {
    if (typeof value !== 'number' && typeof value !== 'string') return []
    const number = typeof value === 'string' && !value.trim() ? NaN : Number(value)
    return Number.isFinite(number) ? [number] : []
  })
  if (!numbers.length) return null
  if (operation === 'min') return numbers.reduce((a, b) => Math.min(a, b))
  if (operation === 'max') return numbers.reduce((a, b) => Math.max(a, b))
  const sum = numbers.reduce((a, b) => a + b, 0)
  return operation === 'avg' ? sum / numbers.length : sum
}
