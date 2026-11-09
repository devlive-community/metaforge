import type { EChartsOption } from 'echarts'

type Legend = Exclude<NonNullable<EChartsOption['legend']>, unknown[]>
type Axis = Exclude<NonNullable<EChartsOption['xAxis'] | EChartsOption['yAxis']>, unknown[]>
type Side = 'top' | 'right' | 'bottom' | 'left'

const array = <T>(value?: T | T[]) => value === undefined ? [] : Array.isArray(value) ? value : [value]
const between = (value: number, minimum: number, maximum: number) => Math.max(minimum, Math.min(maximum, value))

/** Keep chart labels in dedicated lanes, including when a card is resized or exported. */
export function fitChartToCard(option: EChartsOption, width: number, height: number, textColor: string): EChartsOption {
  const axesX = array(option.xAxis)
  const axesY = array(option.yAxis)
  const cartesian = axesX.length > 0 && axesY.length > 0 && !Array.isArray(option.grid)
  const compact = width < 400 || height < 240
  const tiny = height < 106 || width < 160
  const micro = height < 76 || width < 112
  const fontSize = tiny ? 8 : compact ? 10 : 12
  const inset = tiny ? 3 : 7
  const margins = { top: inset, right: inset, bottom: inset, left: inset }
  const fitted: EChartsOption = { ...option }

  if (option.legend && !Array.isArray(option.legend) && option.legend.show !== false && micro) {
    fitted.legend = { ...option.legend, show: false }
  } else if (option.legend && !Array.isArray(option.legend) && option.legend.show !== false) {
    const original = option.legend
    const vertical = original.orient === 'vertical' && width >= 460 && !tiny
    const atBottom = original.bottom !== undefined || original.orient === 'vertical' && !vertical
    const lane = fontSize + 13
    const legend: Legend = {
      ...original, type: original.type ?? 'scroll', padding: 0, itemWidth: fontSize + 2, itemHeight: fontSize - 1,
      itemGap: 8, textStyle: { ...original.textStyle, fontSize, width: vertical ? 76 : Math.max(32, Math.floor(width * .35)), overflow: 'truncate' },
    }
    if (vertical) {
      Object.assign(legend, { orient: 'vertical', right: inset, left: undefined, top: 'middle', bottom: undefined, height: Math.max(20, height - 2 * inset) })
      margins.right += 104
    } else {
      Object.assign(legend, { orient: 'horizontal', left: 'center', right: undefined, top: atBottom ? undefined : inset,
        bottom: atBottom ? inset : undefined, width: Math.max(20, width - inset * 2) })
      margins[atBottom ? 'bottom' : 'top'] += lane
    }
    fitted.legend = legend
  }

  if (option.visualMap && !Array.isArray(option.visualMap) && option.visualMap.show !== false && micro) {
    fitted.visualMap = { ...option.visualMap, show: false }
  } else if (option.visualMap && !Array.isArray(option.visualMap) && option.visualMap.show !== false && option.visualMap.orient === 'horizontal') {
    fitted.visualMap = { ...option.visualMap, top: undefined, bottom: margins.bottom, left: 'center', padding: 0,
      itemHeight: Math.min(option.visualMap.itemHeight ?? 90, Math.max(26, width / 3)), itemWidth: Math.min(option.visualMap.itemWidth ?? 10, fontSize),
      textStyle: { ...option.visualMap.textStyle, fontSize } }
    margins.bottom += fontSize + 10
  }

  if (cartesian) {
    if (option.dataZoom) fitted.dataZoom = array(option.dataZoom).map(zoom => {
      if (zoom.type !== 'slider' || 'show' in zoom && zoom.show === false) return zoom
      if (micro) return { ...zoom, show: false }
      if (zoom.yAxisIndex !== undefined && zoom.xAxisIndex === undefined) {
        const right = margins.right
        margins.right += compact ? 15 : 20
        return { ...zoom, right, width: compact ? 11 : 14 }
      }
      const bottom = margins.bottom
      margins.bottom += compact ? 16 : 22
      return { ...zoom, bottom, height: compact ? 11 : 16 }
    })

    const captions: { axis: Axis; side: Side; offset: number }[] = []
    const axisLane = fontSize + 5
    function fitAxis(axis: Axis, side: Side) {
      if (axis.show !== false && axis.name?.trim()) {
        captions.push({ axis, side, offset: margins[side] + axisLane / 2 })
        if (!micro) margins[side] += axisLane
      }
      const vertical = side === 'left' || side === 'right'
      const size = vertical ? height : width
      const count = Math.max(2, Math.floor(size / (vertical ? 38 : 66)))
      const preferred = 'splitNumber' in axis && typeof axis.splitNumber === 'number' ? axis.splitNumber : 5
      return { ...axis, position: side, name: '', splitNumber: Math.min(preferred, count),
        axisLabel: { ...axis.axisLabel, show: axis.axisLabel?.show !== false && !micro, fontSize, margin: compact ? 4 : 6, hideOverlap: true,
          width: vertical ? between(Math.floor(width * .22), 25, 126) : between(Math.floor(width * .42), 26, 180),
          overflow: 'truncate' as const } }
    }
    fitted.xAxis = axesX.map((axis, index) => fitAxis(axis, axis.position === 'top' || axis.position === undefined && index === 1 ? 'top' : 'bottom')) as EChartsOption['xAxis']
    fitted.yAxis = axesY.map((axis, index) => fitAxis(axis, axis.position === 'right' || axis.position === undefined && index === 1 ? 'right' : 'left')) as EChartsOption['yAxis']
    fitted.grid = { ...option.grid, ...margins, containLabel: true }

    if (captions.length) {
      const centerX = (width + margins.left - margins.right) / 2
      const centerY = (height + margins.top - margins.bottom) / 2
      const elements = captions.map(({ axis, side, offset }, index) => {
        const vertical = side === 'left' || side === 'right'
        const x = vertical ? side === 'left' ? offset : width - offset : centerX
        const y = vertical ? centerY : side === 'top' ? offset : height - offset
        return {
          id: 'chart-card-axis-' + index, type: 'text' as const, x, y, rotation: vertical ? (side === 'left' ? Math.PI / 2 : -Math.PI / 2) : 0,
          silent: true, invisible: micro, z: 100,
          style: { text: axis.name, fill: axis.nameTextStyle?.color ?? option.textStyle?.color ?? textColor, fontSize,
            align: 'center' as const, verticalAlign: 'middle' as const, overflow: 'truncate' as const,
            width: Math.max(20, vertical ? height - margins.top - margins.bottom : width - margins.left - margins.right) },
        }
      })
      fitted.graphic = [...array(option.graphic), { elements }]
    }
    return fitted
  }

  if (option.calendar && !Array.isArray(option.calendar)) {
    const calendar = option.calendar
    const interval = width < 280 ? 3 : width < 430 ? 2 : 1
    fitted.calendar = { ...calendar, left: calendar.dayLabel?.show === false ? inset : Math.max(25, fontSize * 3), right: inset,
      top: margins.top + fontSize + 4, bottom: margins.bottom + 2,
      dayLabel: { ...calendar.dayLabel, margin: 3, fontSize },
      monthLabel: { ...calendar.monthLabel, margin: 3, fontSize,
        ...(interval > 1 && !calendar.monthLabel?.formatter ? {
          formatter: ({ M, nameMap }: { M: number; nameMap: string }) => (M - 1) % interval === 0 ? nameMap : '',
        } : {}) } }
    return fitted
  }

  const plotWidth = Math.max(1, width - margins.left - margins.right)
  const plotHeight = Math.max(1, height - margins.top - margins.bottom)
  const center: [number, number] = [margins.left + plotWidth / 2, margins.top + plotHeight / 2]
  const radiusBase = Math.min(plotWidth, plotHeight) / 2
  const radius = (value: number | string, base = radiusBase) => typeof value === 'string' && value.endsWith('%')
    ? between(parseFloat(value) / 100, 0, 1) * base : between(Number(value), 0, base)
  const radii = (value: number | string | (number | string)[], base?: number): number | [number, number] => Array.isArray(value)
    ? [radius(value[0], base), radius(value[1], base)] : radius(value, base)
  if (option.radar && !Array.isArray(option.radar)) fitted.radar = { ...option.radar, center,
    radius: radius(option.radar.radius as string | number ?? '68%', radiusBase * .88),
    axisName: { ...option.radar.axisName, fontSize, width: Math.max(25, plotWidth * .24), overflow: 'truncate' } }
  if (option.polar && !Array.isArray(option.polar)) fitted.polar = { ...option.polar, center,
    radius: radii(option.polar.radius ?? '74%', radiusBase * .9) }

  if (option.series) fitted.series = array(option.series).map(series => {
    if (series.type === 'funnel') return { ...series, ...margins, label: { ...series.label, fontSize, width: Math.floor(plotWidth * .3), overflow: 'truncate' },
      labelLayout: { hideOverlap: true } }
    if (series.type === 'sunburst') return { ...series, center, radius: radii(series.radius ?? '90%') }
    if (series.type !== 'pie') return series
    const outside = series.label?.show !== false && series.label?.position !== 'inside' && series.label?.position !== 'center'
    const base = outside ? radiusBase * .78 : radiusBase
    return { ...series, center, radius: radii(series.radius ?? '75%', base),
      label: { ...series.label, fontSize, ...(outside ? { alignTo: 'edge' as const, edgeDistance: Math.max(margins.left, margins.right) } : {}) },
      labelLayout: { ...series.labelLayout, hideOverlap: true } }
  }) as EChartsOption['series']

  // A doughnut's summary belongs inside its hole, independently of legend placement.
  if (option.title && !Array.isArray(option.title) && option.title.textAlign === 'center' && array(option.series).some(series => series.type === 'pie' && Array.isArray(series.radius))) {
    const ring = array(fitted.series).find(series => series.type === 'pie' && Array.isArray(series.radius))
    if (ring?.type === 'pie' && Array.isArray(ring.radius)) {
      const inner = Number(ring.radius[0])
      const available = Math.max(10, inner * 1.85 - 2)
      const textUnits = Array.from(option.title.text ?? '').reduce((total, character) => total +
        (/[,.:\s]/.test(character) ? .35 : character.charCodeAt(0) > 127 ? 1 : .64), 0)
      const totalFont = between(Math.floor(Math.min(available / Math.max(1, textUnits), inner * .63)), 7, 19)
      fitted.title = { ...option.title, left: center[0], top: center[1], itemGap: 2, subtext: inner < 18 ? '' : option.title.subtext,
        textStyle: { ...option.title.textStyle, fontSize: totalFont, width: available, overflow: 'truncate' },
        subtextStyle: { ...option.title.subtextStyle, fontSize: Math.min(fontSize, 10), width: available, overflow: 'truncate' } }
    }
  }
  return fitted
}
