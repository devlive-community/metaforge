import type { DashboardModel } from './dashboard'
import { copyDashboardDesign, dashboardAnimationDefaults, dashboardContentSettings, dashboardContainerTool, dashboardDataTool, dashboardScreenEffectTool, dashboardSnapshot, readDashboardDocument } from './dashboard-layout'
import { queryChartTypes } from './query-chart'
import type { ReportModel } from './report'

/** A static saved-state preview. No SQL, refresh policies, media players or remote frames. */
export function screenPreview(screen: DashboardModel) {
  const document = readDashboardDocument(screen.configure, screen.name, screen.description)
  const design = copyDashboardDesign(document.published ?? document.draft)
  const reports: ReportModel[] = (screen.reports || []).flatMap(report => {
    const snapshot = dashboardSnapshot(report)
    if (!snapshot) return []
    return [{ code: report.code, name: report.name, realtime: false, configure: JSON.stringify({ ...snapshot,
      queryChart: { ...snapshot.queryChart, options: { ...snapshot.queryChart.options, name: '', description: '',
        tooltip: 'off', pointAction: 'none', download: false, zoom: false, refreshSeconds: 0 } } }) }]
  })
  for (const board of Object.values(design.boards)) {
    let plots = 0
    board.refresh = 'off'
    board.grid = false
    board.widgets = board.widgets.filter(widget => !dashboardScreenEffectTool(widget.kind)).map(widget => {
      const chart = queryChartTypes.some(kind => kind === widget.kind) || widget.kind === 'report' || dashboardDataTool(widget.kind) || ['metric', 'progress'].includes(widget.kind)
      const simple = ['text', 'heading', 'note', 'badge', 'shape', 'panel', 'divider', 'verticalDivider', 'sectionDivider', 'spacer', 'image', 'markdown', 'codeBlock', 'stepGuide', 'iconLinks', 'imageCompare', 'statusLegend'].includes(widget.kind)
      const safe = dashboardContainerTool(widget.kind) || simple || (chart && !['dataFreshness', 'sparklineGrid'].includes(widget.kind) && ++plots <= 12)
      return { ...widget, ...(safe ? {} : { kind: 'text' as const, text: widget.name, showTitle: false }),
        ...(['rollingTable','carouselContainer'].includes(widget.kind) ? { contentOptions: { ...dashboardContentSettings(widget.contentOptions), autoplay: false } } : {}),
        animation: { ...dashboardAnimationDefaults, ...widget.animation, effect: 'none' as const }, expandable: false, action: 'none' as const }
    })
  }
  const board = design.boards[design.mode]
  const width = 960
  const height = design.mode === 'wall' ? width * board.height / board.width
    : Math.max(480, board.padding * 2 + Math.max(0, ...board.widgets.filter(widget => !widget.parentId).map(widget => widget.y + widget.h)) * (board.rowHeight + board.gap))
  return { design, reports, width, height }
}
