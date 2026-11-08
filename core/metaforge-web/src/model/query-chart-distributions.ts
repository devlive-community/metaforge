import type { CustomSeriesRenderItem, EChartsOption } from 'echarts'
import type { QueryChartOptions, QueryChartRow } from './query-chart'
import type { ChartText } from './query-chart-analysis'
import { reportNumber, reportTimestamp } from './report-values'

type Sample = { value: number; row: QueryChartRow }
type AddRows = (rows: QueryChartRow[]) => number
const categoryAxis = (data: string[]) => ({ type: 'category' as const, data, axisLabel: { hideOverlap: true } })
const percentAxis = (name: string) => ({ type: 'value' as const, min: 0, max: 100, name, axisLabel: { formatter: '{value}%' } })
const probabilityLevels = [0, .01, .05, .1, .2, .25, .3, .4, .5, .6, .7, .75, .8, .9, .95, .99, 1]

function sample(rows: QueryChartRow[], field: string): Sample[] {
  return rows.flatMap(row => {
    const value = reportNumber(row[field])
    return value === undefined ? [] : [{ value, row }]
  }).sort((left, right) => left.value - right.value)
}

function quantile(ordered: Sample[], p: number) {
  const index = (ordered.length - 1) * p
  const left = ordered[Math.floor(index)], right = ordered[Math.ceil(index)]
  const weight = index % 1
  return { value: left.value * (1 - weight) + right.value * weight, rows: left === right ? [left.row] : [left.row, right.row] }
}

function byGroup(rows: QueryChartRow[], category: string, field: string) {
  const groups = new Map<string, QueryChartRow[]>()
  for (const row of rows) {
    if (row[category] == null) continue
    const name = String(row[category])
    const group = groups.get(name) ?? []
    group.push(row)
    groups.set(name, group)
  }
  return [...groups].flatMap(([name, records]) => {
    const values = sample(records, field)
    return values.length ? [{ name, values }] : []
  })
}

function tukeyBox(rows: QueryChartRow[], options: QueryChartOptions, field: string, addRows: AddRows, text: ChartText): EChartsOption | undefined {
  const groups = byGroup(rows, options.category, field)
  if (!groups.length) return
  const outliers: { value: [number, number]; pointId: number }[] = []
  const boxes = groups.map(({ values }, index) => {
    const [lower, median, upper] = [.25, .5, .75].map(p => quantile(values, p).value)
    const span = upper - lower
    const inliers = values.filter(item => item.value >= lower - 1.5 * span && item.value <= upper + 1.5 * span)
    for (const item of values) {
      if (item.value < lower - 1.5 * span || item.value > upper + 1.5 * span) {
        outliers.push({ value: [index, item.value], pointId: addRows([item.row]) })
      }
    }
    return { value: [inliers[0]?.value ?? lower, lower, median, upper, inliers.at(-1)?.value ?? upper],
      pointId: addRows(values.map(item => item.row)) }
  })
  return {
    xAxis: { ...categoryAxis(groups.map(group => group.name)), show: options.xVisible, name: options.xTitle },
    yAxis: numericAxis(options),
    series: [{ type: 'boxplot', name: field, data: boxes },
      { type: 'scatter', name: text('outliers', 'Outliers'), symbolSize: options.pointSize, data: outliers }],
  }
}

function densityShapes(rows: QueryChartRow[], options: QueryChartOptions, field: string, addRows: AddRows, text: ChartText): EChartsOption | undefined {
  const groups = byGroup(rows, options.category, field)
  if (!groups.length) return
  const minimum = groups.reduce((min, group) => Math.min(min, group.values[0].value), Infinity)
  const maximum = groups.reduce((max, group) => Math.max(max, group.values.at(-1)!.value), -Infinity)
  // Work in scaled units so variance and range calculations cannot overflow.
  const scale = Math.max(Math.abs(minimum), Math.abs(maximum)) || 1
  const min = minimum / scale, max = maximum / scale
  const spread = max - min || .1
  const lo = Math.max(-Number.MAX_VALUE / scale, min - spread * .12)
  const hi = Math.min(Number.MAX_VALUE / scale, max + spread * .12)
  const samples = groups.map(({ values }) => {
    const numbers = values.map(({ value }) => value / scale)
    const mean = numbers.reduce((sum, value) => sum + value, 0) / numbers.length
    const variance = numbers.reduce((sum, value) => sum + (value - mean) ** 2, 0) / numbers.length
    const bandwidth = Math.max(spread / 120, 1.06 * Math.sqrt(variance) * numbers.length ** (-.2), Number.EPSILON)
    const curve = Array.from({ length: 49 }, (_, index) => {
      const value = lo + (hi - lo) * index / 48
      const density = numbers.reduce((sum, number) => sum + Math.exp(-.5 * ((value - number) / bandwidth) ** 2), 0)
      return { x: value * scale, density }
    })
    const peak = Math.max(...curve.map(point => point.density)) || 1
    return curve.map(point => ({ x: point.x, ratio: point.density / peak }))
  })
  const ridge = options.type === 'ridgePlot'
  const renderItem: CustomSeriesRenderItem = (params, api) => {
    const lane = api.value(0)
    const size = Math.abs(api.size?.([0, 1])?.[1] ?? 18)
    const points = samples[params.dataIndex].map(({ x, ratio }) => {
      const [left, center] = api.coord([x, lane])
      const base = center + (ridge ? size * .34 : 0)
      return { left, base, offset: size * (ridge ? .64 : .40) * ratio }
    })
    const outline = points.map(({ left, base, offset }) => [left, base - offset])
    const bottom = points.slice().reverse().map(({ left, base, offset }) => [left, ridge ? base : base + offset])
    const color = api.visual('color') as string
    return { type: 'polygon', shape: { points: [...outline, ...bottom] },
      style: { fill: color, stroke: color, lineWidth: options.lineWidth, opacity: options.opacity / 100 } }
  }
  return {
    xAxis: { type: 'value', scale: true, show: options.xVisible, name: options.xTitle || field },
    yAxis: { ...categoryAxis(groups.map(group => group.name)), inverse: true, show: options.yVisible },
    series: [{ type: 'custom', renderItem, clip: true, colorBy: 'data',
      dimensions: ['group', 'min', 'max', text('records', 'Records')], encode: { x: [1, 2], y: 0, tooltip: 3 },
      data: groups.map(({ name, values }, index) => ({
        name, value: [index, lo * scale, hi * scale, values.length], pointId: addRows(values.map(item => item.row)),
      })) }],
  }
}

function numericAxis(options: QueryChartOptions) {
  return { type: 'value' as const, scale: true, show: options.yVisible, name: options.yTitle,
    min: options.min, max: options.max, splitLine: { show: options.grid } }
}

function referenceLine(name: string, data: number[][], width: number) {
  return { type: 'line' as const, name, data, symbol: 'none', silent: true,
    tooltip: { show: false }, lineStyle: { width, type: 'dashed' as const } }
}

function timeline(rows: QueryChartRow[], options: QueryChartOptions, fields: string[], addRows: AddRows): EChartsOption | undefined {
  const time = (value: unknown) => typeof value === 'number' && Number.isFinite(new Date(value).getTime()) ? value : reportTimestamp(value)
  const lanes = new Map<string, number>()
  const data = rows.flatMap(row => {
    const start = time(row[fields[0]]), end = time(row[fields[1]])
    if (row[options.category] == null || start === undefined || end === undefined || end < start) return []
    const name = String(row[options.category])
    if (!lanes.has(name)) lanes.set(name, lanes.size)
    return [{ name, value: [lanes.get(name)!, start, end], pointId: addRows([row]) }]
  })
  if (!data.length) return
  const renderItem: CustomSeriesRenderItem = (_params, api) => {
    const lane = api.value(0)
    const [start, y] = api.coord([api.value(1), lane])
    const [end] = api.coord([api.value(2), lane])
    const height = Math.min(20, Math.abs(api.size?.([0, 1])?.[1] ?? 20) * .55)
    return { type: 'rect', shape: { x: start, y: y - height / 2, width: Math.max(2, end - start), height, r: 2 },
      style: { fill: api.visual('color') as string } }
  }
  return {
    xAxis: { type: 'time', show: options.xVisible, name: options.xTitle },
    yAxis: { ...categoryAxis([...lanes.keys()]), inverse: true, show: options.yVisible },
    series: [{ type: 'custom', clip: true, renderItem, colorBy: 'data',
      dimensions: [options.category, { name: fields[0], type: 'time' }, { name: fields[1], type: 'time' }],
      encode: { x: [1, 2], y: 0, tooltip: [1, 2] }, data }],
  }
}

export function distributionChartOption(options: QueryChartOptions, rows: QueryChartRow[], fields: string[], addRows: AddRows, text: ChartText): EChartsOption | 'nonnegative' | undefined {
  const field = fields[0]
  const xAxis = { type: 'value' as const, scale: true, show: options.xVisible, name: options.xTitle || field }
  const yAxis = numericAxis(options)
  const pointSize = options.pointSize
  const lineStyle = { width: options.lineWidth, type: options.dashed ? 'dashed' as const : 'solid' as const }
  switch (options.type) {
    case 'tukeyBox': return tukeyBox(rows, options, field, addRows, text)
    case 'violinPlot':
    case 'ridgePlot': return densityShapes(rows, options, field, addRows, text)
    case 'ganttTimeline': return timeline(rows, options, fields, addRows)
    case 'quantileComparison': {
      const left = sample(rows, field), right = sample(rows, fields[1])
      const count = Math.min(left.length, right.length, 101)
      if (!count) return
      const data = Array.from({ length: count }, (_, index) => {
        const p = count === 1 ? .5 : index / (count - 1)
        const x = quantile(left, p), y = quantile(right, p)
        return { value: [x.value, y.value], pointId: addRows([...new Set([...x.rows, ...y.rows])]) }
      })
      const low = Math.min(left[0].value, right[0].value)
      const high = Math.max(left.at(-1)!.value, right.at(-1)!.value)
      return { xAxis, yAxis: { ...yAxis, name: options.yTitle || fields[1] }, series: [
        { type: 'scatter', name: fields[1], dimensions: fields, data, symbolSize: pointSize },
        referenceLine(text('equalityLine', 'Equal quantiles'), [[low, low], [high, high]], options.lineWidth),
      ] }
    }
    case 'percentileProfile': {
      const values = sample(rows, field)
      if (!values.length) return
      return { xAxis: percentAxis(text('percentile', 'Percentile')), yAxis: { ...yAxis, name: options.yTitle || field },
        series: [{ type: 'line', name: field, symbolSize: pointSize, lineStyle,
          data: probabilityLevels.map(p => {
            const point = quantile(values, p)
            return { value: [p * 100, point.value], pointId: addRows(point.rows) }
          }) }] }
    }
    case 'lorenzCurve': {
      const values = sample(rows, field)
      if (values[0]?.value < 0) return 'nonnegative'
      const max = values.at(-1)?.value
      if (!max) return
      const total = values.reduce((sum, item) => sum + item.value / max, 0)
      let running = 0
      const data = values.map((item, index) => {
        running += item.value / max
        return { value: [100 * (index + 1) / values.length, Math.min(100, 100 * running / total)], pointId: addRows([item.row]) }
      })
      return { xAxis: percentAxis(text('populationShare', 'Sample share')), yAxis: percentAxis(text('cumulativeShare', 'Cumulative share')),
        series: [{ type: 'line', name: field, showSymbol: options.points, symbolSize: pointSize, lineStyle, data: [{ value: [0, 0] }, ...data] },
          referenceLine(text('equalityLine', 'Equality line'), [[0, 0], [100, 100]], options.lineWidth)] }
    }
    case 'quadrantScatter': {
      const data = rows.flatMap(row => {
        const x = reportNumber(row[field]), y = reportNumber(row[fields[1]])
        return x === undefined || y === undefined ? [] : [{ value: [x, y], pointId: addRows([row]) }]
      })
      if (!data.length) return
      const midpoint = (axis: number) => {
        const ordered = data.map(point => point.value[axis]).sort((left, right) => left - right)
        const middle = Math.floor(ordered.length / 2)
        return ordered.length % 2 ? ordered[middle] : ordered[middle - 1] / 2 + ordered[middle] / 2
      }
      return { xAxis, yAxis: { ...yAxis, name: options.yTitle || fields[1] },
        series: [{ type: 'scatter', name: fields[1], dimensions: fields, data, symbolSize: pointSize,
          markLine: { silent: true, symbol: 'none', label: { show: false }, lineStyle: { type: 'dashed', width: options.lineWidth },
            data: [{ xAxis: midpoint(0) }, { yAxis: midpoint(1) }] } }] }
    }
    case 'lagScatter': {
      const values = rows.map(row => reportNumber(row[field]))
      const data = values.flatMap((current, index) => {
        const previous = values[index - 1]
        return current === undefined || previous === undefined ? [] : [{ value: [previous, current], pointId: addRows([rows[index - 1], rows[index]]) }]
      })
      if (!data.length) return
      return { xAxis: { ...xAxis, name: options.xTitle || text('previousValue', 'Previous value') },
        yAxis: { ...yAxis, name: options.yTitle || text('currentValue', 'Current value') },
        series: [{ type: 'scatter', name: field, data, symbolSize: pointSize }] }
    }
  }
}
