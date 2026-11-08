import type { EChartsOption, LineSeriesOption } from 'echarts'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartPoint, type QueryChartRow } from './query-chart'
import { reportDateKey, reportNumber } from './report-values'

type Aggregated = ReturnType<typeof aggregateQueryChart>
type NumericPoint = QueryChartPoint & { value: number }
type AddRows = (rows: QueryChartRow[]) => number
export type ChartText = (key: string, fallback: string) => string
const percentAxis = { type: 'value' as const, min: 0, max: 100, axisLabel: { formatter: '{value}%' } }
const categoryAxis = (data: string[]) => ({ type: 'category' as const, data, axisLabel: { hideOverlap: true } })
const valueAxis = { type: 'value' as const }

function seriesOverCategories(points: NumericPoint[], options: QueryChartOptions, add: (point: NumericPoint) => number) {
  const categories = [...new Set(points.map(point => point.category))]
  const index = new Map(categories.map((name, position) => [name, position]))
  const groups = new Map<string, { position: number; value: number; pointId: number }[]>()
  const running = new Map<string, number>()
  const rank = new Map<number, number>()
  if (options.type === 'rankTrend') {
    const byCategory = new Map<string, NumericPoint[]>()
    for (const point of points) {
      const group = byCategory.get(point.category)
      if (group) group.push(point)
      else byCategory.set(point.category, [point])
    }
    for (const group of byCategory.values()) {
      group.sort((left, right) => right.value - left.value)
      let current = 1
      group.forEach((point, position) => {
        if (position > 0 && point.value !== group[position - 1].value) current = position + 1
        rank.set(point.pointId, current)
      })
    }
  }
  const ordered = [...points].sort((left, right) => index.get(left.category)! - index.get(right.category)!)
  for (const point of ordered) {
    const name = point.series || options.seriesName || options.metric || options.category
    const value = rank.get(point.pointId) ?? ((running.get(name) ?? 0) + point.value)
    if (!Number.isFinite(value)) continue
    running.set(name, value)
    const items = groups.get(name) ?? []
    items.push({ position: index.get(point.category)!, value, pointId: add(point) })
    groups.set(name, items)
  }
  const series: LineSeriesOption[] = [...groups].map(([name, items]) => {
    const data: LineSeriesOption['data'] = []
    items.forEach((item, position) => {
      const previous = items[position - 1]
      if (previous && item.position > previous.position + 1) data.push([previous.position + 1, null])
      const datum = { value: [item.position, item.value], pointId: item.pointId }
      data.push(datum)
    })
    return { type: 'line', name, data, showSymbol: options.points, symbolSize: options.pointSize, smooth: options.smooth,
      lineStyle: { width: options.lineWidth, type: options.dashed ? 'dashed' : 'solid' } }
  })
  return { categories, series }
}

function axisTitles(options: QueryChartOptions, name: string) {
  return {
    xAxis: { ...valueAxis, scale: true, show: options.xVisible, name: options.xTitleVisible ? options.xTitle || name : '' },
    yAxis: { ...valueAxis, name: options.yTitle, min: options.min, max: options.max,
      axisLabel: { show: options.yVisible }, splitLine: { show: options.grid } },
  }
}

function comparisonSeries(options: QueryChartOptions, rows: QueryChartRow[], fields: string[], addRows: AddRows): EChartsOption | 'nonnegative' | undefined {
  const [first, second] = fields.map(metric => aggregateQueryChart(rows, {
    ...options, metric, aggregation: 'sum', series: '', filtersEnabled: false, nulls: 'keep',
  }))
  const points = [...first.points, ...second.points]
  if (!points.some(point => point.value !== null)) return
  const categories = [...new Set(points.map(point => point.category))]
  const byName = new Map(categories.map((name, index) => [name, index]))
  const mirror = options.type === 'mirrorBar'
  if (mirror && points.some(point => (point.value ?? 0) < 0)) return 'nonnegative'
  const data = (aggregate: Aggregated, direction: number) => aggregate.points.flatMap(point => {
    if (point.value === null) return []
    const index = byName.get(point.category)!
    return [{ value: mirror ? [point.value * direction, index, point.value] : [index, point.value],
      pointId: addRows(aggregate.details.get(point.pointId) ?? []) }]
  })
  const legend = { show: options.legend, selectedMode: options.legendInteractive, type: 'scroll' as const }
  if (mirror) {
    const maximum = points.reduce((max, point) => Math.max(max, point.value ?? 0), 1)
    return {
      legend, xAxis: { ...valueAxis, min: -maximum, max: maximum, axisLabel: { formatter: value => String(Math.abs(value)) } },
      yAxis: { ...categoryAxis(categories), inverse: true, axisLine: { onZero: false } },
      series: [first, second].map((group, index) => ({
        type: 'bar', name: fields[index], barGap: '-100%', dimensions: ['signed', options.category, fields[index]],
        encode: { x: 0, y: 1, tooltip: 2 }, data: data(group, index === 0 ? -1 : 1),
      })),
    }
  }
  return {
    legend, xAxis: categoryAxis(categories), yAxis: axisTitles(options, '').yAxis,
    series: [
      { type: 'bar', name: fields[0], data: data(first, 1), barMaxWidth: 36 },
      { type: 'scatter', name: fields[1], data: data(second, 1), symbol: 'rect', symbolSize: [25, 4], z: 4 },
    ],
  }
}

export function analyticalChartOption(options: QueryChartOptions, result: Aggregated, fields: string[], addRows: AddRows, text: ChartText): EChartsOption | 'nonnegative' | undefined {
  const rows = result.filtered
  const points = result.points.filter((point): point is NumericPoint => point.value !== null)
  const add = (point: QueryChartPoint) => addRows(result.details.get(point.pointId) ?? [])
  const name = options.metric || text('records', 'Records')
  const legend = { show: options.legend, selectedMode: options.legendInteractive, type: 'scroll' as const }
  switch (options.type) {
    case 'mirrorBar':
    case 'targetComparison':
      return comparisonSeries(options, rows, fields, addRows)
    case 'deviationBar':
      if (!points.length) return
      return {
        xAxis: categoryAxis(points.map(point => point.category)), yAxis: axisTitles(options, '').yAxis,
        series: [{ type: 'bar', name, data: points.map(point => ({
          value: point.value, pointId: add(point), itemStyle: point.value < 0 ? { color: '#d15864' } : undefined,
        })) }],
      }
    case 'pareto': {
      if (points.some(point => point.value < 0)) return 'nonnegative'
      const ordered = points.filter(point => point.value > 0).sort((left, right) => right.value - left.value)
      if (!ordered.length) return
      const max = ordered[0].value
      const scaledTotal = ordered.reduce((sum, point) => sum + point.value / max, 0)
      let running = 0
      const marks = ordered.map(point => {
        running += point.value / max
        return { pointId: add(point), value: point.value, cumulative: Math.min(100, 100 * running / scaledTotal) }
      })
      return {
        legend, xAxis: categoryAxis(ordered.map(point => point.category)), yAxis: [valueAxis, percentAxis],
        series: [
          { type: 'bar', name, data: marks.map(({ value, pointId }) => ({ value, pointId })) },
          { type: 'line', name: text('cumulativeShare', 'Cumulative share'), yAxisIndex: 1, data: marks.map(({ cumulative, pointId }) => ({ value: cumulative, pointId })) },
        ],
      }
    }
    case 'cumulativeLine':
    case 'rankTrend': {
      const { categories, series } = seriesOverCategories(points, options, add)
      if (!series.length) return
      const rank = options.type === 'rankTrend'
      return { legend, xAxis: categoryAxis(categories),
        yAxis: rank ? { ...valueAxis, inverse: true, min: 1, minInterval: 1, name: text('position', 'Rank') } : axisTitles(options, '').yAxis,
        series }
    }
    case 'radialBar': {
      if (points.some(point => point.value < 0)) return 'nonnegative'
      if (!points.length) return
      const maximum = points.reduce((max, point) => Math.max(max, point.value), 0) || 1
      return {
        polar: { radius: ['16%', '74%'] },
        angleAxis: { type: 'value', min: 0, max: Number.isFinite(maximum * 1.12) ? maximum * 1.12 : maximum, startAngle: 90 },
        radiusAxis: { ...categoryAxis(points.map(point => point.category)), axisLine: { show: false } },
        series: [{ type: 'bar', name, coordinateSystem: 'polar', roundCap: true, colorBy: 'data',
          data: points.map(point => ({ value: point.value, pointId: add(point) })) }],
      }
    }
    case 'calendarMatrix': {
      const daily = aggregateQueryChart(rows.filter(row => reportDateKey(row[options.category])), {
        ...options, grain: 'day', series: '', filtersEnabled: false,
      })
      const samples = daily.points.filter((point): point is NumericPoint => point.value !== null && /^\d{4}-\d{2}-\d{2}$/.test(point.category))
      const year = samples.reduce((latest, point) => {
        const year = point.category.slice(0, 4)
        return year > latest ? year : latest
      }, '')
      if (!year) return
      const selected = samples.filter(point => point.category.startsWith(year + '-'))
      const minimum = selected.reduce((min, point) => Math.min(min, point.value), 0)
      const maximum = selected.reduce((max, point) => Math.max(max, point.value), 0)
      const language = text('calendarLanguage', 'EN')
      return {
        calendar: { range: year, left: 45, right: 20, top: 24, bottom: 38, cellSize: ['auto', 'auto'],
          yearLabel: { show: false }, dayLabel: { firstDay: 1, nameMap: language }, monthLabel: { nameMap: language } },
        visualMap: { min: minimum, max: maximum === minimum ? maximum + 1 : maximum, dimension: 1, orient: 'horizontal', left: 'center', bottom: 2 },
        series: [{ type: 'heatmap', name, coordinateSystem: 'calendar', data: selected.map(point => ({
          value: [point.category, point.value], pointId: addRows(daily.details.get(point.pointId) ?? []),
        })) }],
      }
    }
    case 'stripPlot': {
      const buckets = new Map<string, { index: number; repetitions: Map<number, number> }>()
      const data = rows.flatMap(row => {
        const value = reportNumber(row[fields[0]])
        if (value === undefined || row[options.category] == null) return []
        const group = String(row[options.category])
        const bucket = buckets.get(group) ?? { index: buckets.size, repetitions: new Map() }
        buckets.set(group, bucket)
        const position = bucket.repetitions.get(value) ?? 0
        bucket.repetitions.set(value, position + 1)
        const offset = position === 0 ? 0 : ((position * .61803398875) % 1 - .5) * 16
        return [{ name: group, value: [value, bucket.index], symbolOffset: [0, offset], pointId: addRows([row]) }]
      })
      if (!data.length) return
      const groups = [...buckets.keys()]
      return {
        xAxis: axisTitles(options, fields[0]).xAxis,
        yAxis: { ...categoryAxis(groups), inverse: true, splitLine: { show: false } },
        series: [{ type: 'scatter', name: fields[0], data, symbolSize: options.pointSize, dimensions: [fields[0], 'position'], encode: { x: 0, y: 1, tooltip: 0 } }],
      }
    }
    case 'ecdf': {
      const groups = new Map<number, QueryChartRow[]>()
      let count = 0
      for (const row of rows) {
        const value = reportNumber(row[fields[0]])
        if (value === undefined) continue
        const group = groups.get(value) ?? []
        group.push(row)
        groups.set(value, group)
        count++
      }
      const ordered = [...groups].sort(([left], [right]) => left - right)
      if (!count) return
      let seen = 0
      const data = ordered.map(([value, records]) => {
        seen += records.length
        return { value: [value, 100 * seen / count], pointId: addRows(records) }
      })
      return {
        xAxis: axisTitles(options, fields[0]).xAxis, yAxis: percentAxis,
        series: [{ type: 'line', step: 'end', name: text('cumulativeShare', 'Cumulative share'), data: [{ value: [ordered[0][0], 0] }, ...data],
          dimensions: [fields[0], text('percent', 'Percent')] }],
      }
    }
    case 'scatterFit': {
      const data = rows.flatMap(row => {
        const x = reportNumber(row[fields[0]]), y = reportNumber(row[fields[1]])
        return x === undefined || y === undefined ? [] : [{ value: [x, y] as [number, number], pointId: addRows([row]) }]
      })
      if (!data.length) return
      const axis = axisTitles(options, fields[0])
      const xValues = data.map(point => point.value[0])
      const [min, max] = [Math.min(...xValues), Math.max(...xValues)]
      const scaleX = Math.max(Math.abs(min), Math.abs(max)) || 1
      const scaleY = data.reduce((scale, point) => Math.max(scale, Math.abs(point.value[1])), 0) || 1
      const meanX = data.reduce((sum, { value: [x] }) => sum + x / scaleX, 0) / data.length
      const meanY = data.reduce((sum, { value: [, y] }) => sum + y / scaleY, 0) / data.length
      const variance = data.reduce((sum, { value: [x] }) => sum + (x / scaleX - meanX) ** 2, 0)
      const covariance = data.reduce((sum, { value: [x, y] }) => sum + (x / scaleX - meanX) * (y / scaleY - meanY), 0)
      const fit = variance > 0 ? [min, max].map(x => [x, (meanY + (x / scaleX - meanX) * covariance / variance) * scaleY]) : []
      const trend: LineSeriesOption[] = fit.length && fit.every(([, y]) => Number.isFinite(y)) ? [
        { type: 'line', name: text('linearFit', 'Linear fit'), data: fit, symbol: 'none', silent: true, lineStyle: { type: 'dashed', width: options.lineWidth } },
      ] : []
      return { ...axis, yAxis: { ...axis.yAxis, scale: true, name: options.yTitle || fields[1] },
        series: [{ type: 'scatter', name: fields[1], data, symbolSize: options.pointSize, dimensions: fields }, ...trend] }
    }
  }
}
