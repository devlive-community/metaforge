import type { CustomSeriesRenderItem, EChartsOption } from 'echarts'
import { chartDataSpec, geoJsonLimit, requiresChartSeries } from './query-chart-data'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartPoint, type QueryChartRow } from './query-chart'

type Aggregated = ReturnType<typeof aggregateQueryChart>
type MapInput = Parameters<typeof import('echarts').registerMap>[1]
export interface PreparedQueryChart {
  option?: EChartsOption
  issue?: 'missingFields' | 'invalidMap' | 'mapMismatch' | 'cycle' | 'noPoints'
  details: Map<number, QueryChartRow[]>
  names?: Map<string, number>
  map?: { name: string; source: MapInput }
}

function numberValue(value: unknown): number | undefined {
  if (typeof value !== 'number' && (typeof value !== 'string' || !value.trim())) return
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

function timeValue(value: string): number | undefined {
  if (/^-?\d+(\.\d+)?$/.test(value)) {
    const number = Number(value)
    if (Math.abs(number) >= 100_000_000_000) return number
    return
  }
  const date = Date.parse(value.replace(' ', 'T'))
  return Number.isFinite(date) ? date : undefined
}

function hasCycle(edges: { source: string; target: string }[]): boolean {
  const outgoing = new Map<string, string[]>()
  const incoming = new Map<string, number>()
  for (const { source, target } of edges) {
    if (!outgoing.has(source)) outgoing.set(source, [])
    outgoing.get(source)!.push(target)
    if (!incoming.has(source)) incoming.set(source, 0)
    incoming.set(target, (incoming.get(target) ?? 0) + 1)
  }
  const ready = [...incoming].filter(([, count]) => count === 0).map(([name]) => name)
  for (let index = 0; index < ready.length; index++) {
    for (const target of outgoing.get(ready[index]) ?? []) {
      const remaining = incoming.get(target)! - 1
      incoming.set(target, remaining)
      if (!remaining) ready.push(target)
    }
  }
  return ready.length < incoming.size
}

function mapSource(raw?: string) {
  if (!raw?.trim() || raw.length > geoJsonLimit) return
  const coordinate = (item: unknown) => Array.isArray(item) && item.length >= 2
      && item.slice(0, 2).every(value => typeof value === 'number' && Number.isFinite(value))
  const polygon = (item: unknown) => Array.isArray(item) && item.length > 0
      && item.every(ring => Array.isArray(ring) && ring.length >= 4 && ring.every(coordinate))
  try {
    const value = JSON.parse(raw)
    if (value?.type !== 'FeatureCollection' || !Array.isArray(value.features) || !value.features.length) return
    const features = []
    const names = new Set<string>()
    for (const feature of value.features) {
      const name = feature?.properties?.name
      const geometry = feature?.geometry
      if (feature?.type !== 'Feature' || typeof name !== 'string' || !name.trim() || !geometry) return
      const coordinates = geometry.coordinates
      if (geometry.type === 'Polygon' ? !polygon(coordinates)
          : geometry.type !== 'MultiPolygon' || !Array.isArray(coordinates) || !coordinates.length || !coordinates.every(polygon)) return
      names.add(name)
      features.push({ type: 'Feature', properties: { name }, geometry: { type: geometry.type, coordinates } })
    }
    let hash = 2166136261
    for (let index = 0; index < raw.length; index++) hash = Math.imul(hash ^ raw.charCodeAt(index), 16777619)
    return { name: `mf-map-${hash >>> 0}-${raw.length}`, names, source: { type: 'FeatureCollection', features } as MapInput }
  }
  catch { return }
}

const rangeBar: CustomSeriesRenderItem = (_params, api) => {
  const low = api.coord([api.value(0), api.value(1)])
  const high = api.coord([api.value(0), api.value(2)])
  const width = Math.max(3, Math.abs(api.size?.([1, 0])?.[0] ?? 18) * 0.55)
  return {
    type: 'rect',
    shape: { x: low[0] - width / 2, y: Math.min(low[1], high[1]), width, height: Math.max(1, Math.abs(low[1] - high[1])) },
    style: { fill: api.visual('color') as string },
  }
}

function horizontalMarker(paired: boolean, radius: number): CustomSeriesRenderItem {
  return (_params, api) => {
    const row = api.value(0)
    const [left, y] = api.coord([api.value(1), row])
    const [right] = api.coord([api.value(2), row])
    const color = api.visual('color') as string
    const marker = (x: number, open = false) => ({
      type: 'circle' as const, shape: { cx: x, cy: y, r: radius },
      style: { fill: open ? 'transparent' : color, stroke: color, lineWidth: 2 },
    })
    return {
      type: 'group', children: [
        { type: 'line', shape: { x1: left, y1: y, x2: right, y2: y }, style: { stroke: color, lineWidth: 2, opacity: .65 } },
        ...(paired ? [marker(left, left !== right)] : []), marker(right),
      ],
    }
  }
}

export function prepareQueryChart(options: QueryChartOptions, result: Aggregated): PreparedQueryChart | undefined {
  const spec = chartDataSpec(options.type)
  if (!spec) return
  const details = new Map<number, QueryChartRow[]>()
  const issue = (key: NonNullable<PreparedQueryChart['issue']>): PreparedQueryChart => ({ issue: key, details })
  const fieldSet = new Set(options.fields)
  const category = options.category
  if ((spec.category && !fieldSet.has(category)) || (requiresChartSeries(options.type)
      && (!fieldSet.has(options.series) || options.series === category))) return issue('missingFields')
  const fields = spec.fields?.map(key => options.chartFields?.[key] ?? '') ?? []
  const parallel = [...new Set(options.parallelFields ?? [])].slice(0, 12)
  if (fields.some(field => !fieldSet.has(field)) || (spec.parallel && (parallel.length < 2 || parallel.some(field => !fieldSet.has(field))))) {
    return issue('missingFields')
  }
  const id = (rows: QueryChartRow[]) => {
    const index = details.size
    details.set(index, rows)
    return index
  }
  const raw = result.filtered
  const points = result.points.filter((point): point is QueryChartPoint & { value: number } => point.value !== null)
  const pointRows = (point: QueryChartPoint) => result.details.get(point.pointId) ?? []
  const done = (option: EChartsOption): PreparedQueryChart => ({ option, details })
  const axes = (categories: string[]) => ({
    xAxis: { type: 'category' as const, data: categories, name: options.xTitle, show: options.xVisible, axisLabel: { hideOverlap: options.hideOverlap } },
    yAxis: { type: 'value' as const, name: options.yTitle, min: options.min, max: options.max, axisLabel: { show: options.yVisible },
      splitLine: { show: options.grid } },
  })
  const horizontalAxes = (categories: string[]) => {
    const axis = axes(categories)
    return { xAxis: axis.yAxis, yAxis: { ...axis.xAxis, inverse: true } }
  }
  const bound = Math.max(1, ...points.map(point => point.value))

  switch (options.type) {
    case 'waterfall': {
      let running = 0
      const data = points.map((point, index) => {
        const start = running
        running += point.value
        return { value: [index, start, running, point.value], pointId: id(pointRows(point)),
          itemStyle: point.value < 0 ? { color: '#d15864' } : undefined }
      })
      return data.length ? done({ ...axes(points.map(point => point.category)),
        series: [{ type: 'custom', renderItem: rangeBar, clip: true, dimensions: [category, 'start', 'end', options.metric || 'value'],
          encode: { x: 0, y: [1, 2], tooltip: [3] }, data }] }) : issue('noPoints')
    }
    case 'lollipop':
    case 'dumbbell': {
      const paired = options.type === 'dumbbell'
      const items = paired ? raw.flatMap(row => {
        const before = numberValue(row[fields[0]]), after = numberValue(row[fields[1]])
        if (row[category] == null || before === undefined || after === undefined) return []
        return [{ label: String(row[category]), before, after, rows: [row] }]
      }) : points.map(point => ({ label: point.category, before: 0, after: point.value, rows: pointRows(point) }))
      return items.length ? done({ ...horizontalAxes(items.map(item => item.label)),
        series: [{ type: 'custom', renderItem: horizontalMarker(paired, Math.max(3, options.pointSize / 2)), clip: true,
          dimensions: [category, paired ? fields[0] : 'baseline', paired ? fields[1] : options.metric || 'value'],
          encode: { x: [1, 2], y: 0, tooltip: paired ? [1, 2] : [2] },
          data: items.map((item, index) => ({ value: [index, item.before, item.after], pointId: id(item.rows) })) }] }) : issue('noPoints')
    }
    case 'dualAxis':
    case 'rangeBand': {
      const band = options.type === 'rangeBand'
      const source = band ? raw.filter(row => {
        const lower = numberValue(row[fields[0]]), upper = numberValue(row[fields[1]])
        return lower !== undefined && upper !== undefined && lower <= upper
      }) : raw
      const [first, second] = fields.map((metric, index) => aggregateQueryChart(source, {
        ...options, metric, aggregation: band ? index === 0 ? 'min' : 'max' : 'sum',
        filtersEnabled: false, series: '', nulls: 'keep',
      }))
      const categories = [...new Set([...first.points, ...second.points].map(point => point.category))]
      const byCategory = (data: Aggregated) => new Map(data.points.map(point => [point.category, point]))
      const left = byCategory(first), right = byCategory(second)
      const axis = axes(categories)
      if (![...first.points, ...second.points].some(point => point.value !== null)) return issue('noPoints')
      if (band) {
        const ranges = categories.map(label => {
          const lower = left.get(label), upper = right.get(label)
          if (lower?.value == null || upper?.value == null) return null
          return { value: [upper.value - lower.value, lower.value, upper.value], pointId: id(first.details.get(lower.pointId) ?? []) }
        })
        return done({ ...axis, series: [
          { type: 'line', stack: 'range', stackStrategy: 'all', silent: true, symbol: 'none', name: fields[0],
            tooltip: { show: false }, lineStyle: { opacity: 0 }, data: categories.map(label => left.get(label)?.value ?? null) },
          { type: 'line', stack: 'range', stackStrategy: 'all', name: fields[1], symbolSize: options.pointSize,
            dimensions: ['span', fields[0], fields[1]], encode: { y: 0, tooltip: [1, 2] },
            areaStyle: { opacity: Math.max(.15, options.opacity / 100) }, data: ranges },
        ] })
      }
      const seriesValues = (map: typeof left, data: Aggregated) => categories.map(label => {
        const point = map.get(label)
        if (point?.value == null) return null
        return { value: point.value, pointId: id(data.details.get(point.pointId) ?? []) }
      })
      return done({ ...axis, grid: { top: 36, bottom: options.zoom ? 38 : 16, left: 24, right: 24, containLabel: true },
        legend: { show: options.legend, selectedMode: options.legendInteractive, type: 'scroll', top: 0 },
        yAxis: fields.map((name, index) => ({ ...axis.yAxis, min: undefined, max: undefined, name,
          splitLine: { show: index === 0 && options.grid } })),
        series: [
          { type: 'bar', name: fields[0], yAxisIndex: 0, data: seriesValues(left, first) },
          { type: 'line', name: fields[1], yAxisIndex: 1, data: seriesValues(right, second), symbolSize: options.pointSize },
        ] })
    }
    case 'polarBar':
    case 'polarStackedBar':
    case 'polarLine': {
      const categories = [...new Set(points.map(point => point.category))]
      if (!categories.length) return issue('noPoints')
      const groups = new Map<string, Map<string, QueryChartPoint>>()
      for (const point of points) {
        const name = point.series || options.seriesName || options.metric || category
        const group = groups.get(name) ?? new Map()
        group.set(point.category, point)
        groups.set(name, group)
      }
      return done({
        polar: { radius: ['8%', '67%'] },
        angleAxis: { type: 'category', data: categories, show: options.xVisible, axisLabel: { hideOverlap: options.hideOverlap } },
        radiusAxis: { min: options.min, max: options.max, axisLabel: { show: options.yVisible }, splitLine: { show: options.grid } },
        legend: { show: options.legend && groups.size > 1, selectedMode: options.legendInteractive, bottom: 0, type: 'scroll' },
        series: [...groups].map(([name, group]) => ({
          type: options.type === 'polarLine' ? 'line' : 'bar', name, coordinateSystem: 'polar',
          stack: options.type === 'polarStackedBar' ? 'radius' : undefined,
          data: categories.map(label => {
            const point = group.get(label)
            return point ? { value: point.value, pointId: id(pointRows(point)) } : null
          }),
        })),
      })
    }
    case 'candlestick':
    case 'custom':
    case 'lines':
    case 'bubble':
    case 'parallel': {
      const input = spec.parallel ? parallel : fields
      const rows = raw.flatMap(row => {
        if (spec.category && row[category] == null) return []
        const values = input.map(field => numberValue(row[field]))
        if (!values.every((value): value is number => value !== undefined)) return []
        if (options.type === 'candlestick') {
          const [open, close, low, high] = values
          if (low > Math.min(open, close) || high < Math.max(open, close)) return []
        }
        if (options.type === 'custom' && values[0] > values[1]) return []
        if (options.type === 'bubble' && values[2] < 0) return []
        return [{ category: String(row[category] ?? ''), values, row }]
      })
      if (!rows.length) return issue('noPoints')
      if (options.type === 'bubble') {
        const largest = rows.reduce((max, row) => Math.max(max, row.values[2]), 0)
        const maxDiameter = Math.max(20, Math.min(56, options.pointSize * 8))
        return done({
          xAxis: { type: 'value', name: options.xTitleVisible ? options.xTitle || fields[0] : '', show: options.xVisible, scale: true,
            axisLabel: { hideOverlap: options.hideOverlap } },
          yAxis: { type: 'value', name: options.yTitle || fields[1], scale: true, min: options.min, max: options.max,
            axisLabel: { show: options.yVisible }, splitLine: { show: options.grid } },
          series: [{ type: 'scatter', dimensions: fields, encode: { x: 0, y: 1, tooltip: [0, 1, 2] },
            symbolSize: value => largest ? Math.max(5, maxDiameter * Math.sqrt(value[2] / largest)) : 5,
            itemStyle: { opacity: .75 },
            data: rows.map(row => ({ value: row.values, pointId: id([row.row]) })) }],
        })
      }
      if (options.type === 'parallel') return done({
        parallel: { top: 26, bottom: 26, left: '9%', right: '9%' },
        parallelAxis: input.map((name, dim) => ({ dim, name, type: 'value' })),
        series: [{ type: 'parallel', lineStyle: { opacity: 0.55 },
          data: rows.map(row => ({ value: row.values, pointId: id([row.row]) })) }],
      })
      if (options.type === 'lines') return done({
        xAxis: { type: 'value', name: options.xTitle, scale: true },
        yAxis: { type: 'value', name: options.yTitle, scale: true },
        series: [{ type: 'lines', coordinateSystem: 'cartesian2d', symbol: ['circle', 'arrow'],
          lineStyle: { width: options.lineWidth, opacity: 0.65 },
          data: rows.map(({ values: [x1, y1, x2, y2], row }) => ({
            coords: [[x1, y1], [x2, y2]], pointId: id([row]),
          })) }],
      })
      if (options.type === 'candlestick') return done({
        ...axes(rows.map(row => row.category)),
        series: [{ type: 'candlestick', data: rows.map(row => ({ value: row.values, pointId: id([row.row]) })) }],
      })
      return done({
        ...axes(rows.map(row => row.category)),
        series: [{ type: 'custom', renderItem: rangeBar, dimensions: [category, ...input], encode: { x: 0, y: [1, 2], tooltip: [1, 2] },
          data: rows.map((row, index) => ({ value: [index, ...row.values], pointId: id([row.row]) })) }],
      })
    }
    case 'boxplot': {
      const grouped = new Map<string, QueryChartRow[]>()
      for (const row of raw) {
        if (row[category] == null || numberValue(row[fields[0]]) === undefined) continue
        const name = String(row[category])
        if (!grouped.has(name)) grouped.set(name, [])
        grouped.get(name)!.push(row)
      }
      const data = [...grouped].map(([name, rows]) => {
        const values = rows.map(row => numberValue(row[fields[0]])!).sort((a, b) => a - b)
        const quantile = (ratio: number) => {
          const index = (values.length - 1) * ratio
          const start = Math.floor(index)
          return values[start] + ((values[start + 1] ?? values[start]) - values[start]) * (index - start)
        }
        return { name, value: [0, .25, .5, .75, 1].map(quantile), pointId: id(rows) }
      })
      return data.length ? done({ ...axes(data.map(row => row.name)), series: [{ type: 'boxplot', data }] }) : issue('noPoints')
    }
    case 'histogram': {
      const samples = raw.flatMap(row => {
        const value = numberValue(row[fields[0]])
        return value === undefined ? [] : [{ row, value }]
      })
      if (!samples.length) return issue('noPoints')
      const values = samples.map(sample => sample.value)
      const min = Math.min(...values)
      const max = Math.max(...values)
      const count = min === max ? 1 : Math.min(20, Math.ceil(Math.sqrt(samples.length)))
      const width = (max - min) / count || 1
      const bins = Array.from({ length: count }, () => [] as QueryChartRow[])
      for (const { row, value } of samples) bins[Math.min(count - 1, Math.floor((value - min) / width))].push(row)
      const label = (value: number) => Number(value.toPrecision(5)).toString()
      return done({
        ...axes(bins.map((_, index) => count === 1 ? label(min) : label(min + index * width) + ' – ' + label(min + (index + 1) * width))),
        series: [{ type: 'bar', barCategoryGap: 0, data: bins.map(rows => ({ value: rows.length, pointId: id(rows) })) }],
      })
    }
    case 'heatmap': {
      const x = [...new Set(points.map(point => point.category))]
      const y = [...new Set(points.map(point => point.series))]
      const xIndex = new Map(x.map((name, index) => [name, index]))
      const yIndex = new Map(y.map((name, index) => [name, index]))
      return points.length ? done({
        ...axes(x), yAxis: { type: 'category', data: y, show: options.yVisible },
        visualMap: { min: Math.min(0, ...points.map(point => point.value)), max: bound, show: false },
        series: [{ type: 'heatmap', label: { show: options.labels },
          data: points.map(point => ({ value: [xIndex.get(point.category)!, yIndex.get(point.series)!, point.value], pointId: id(pointRows(point)) })) }],
      }) : issue('noPoints')
    }
    case 'gauge': {
      const cells = raw.map(row => row[options.metric]).filter(cell => cell != null)
      const numbers = cells.flatMap(cell => {
        const value = numberValue(cell)
        return value === undefined ? [] : [value]
      })
      const sum = numbers.reduce((total, value) => total + value, 0)
      let value: number | undefined
      if (options.aggregation === 'count') value = options.metric ? cells.length : raw.length
      else if (options.aggregation === 'distinct') value = new Set(cells.map(String)).size
      else if (numbers.length) {
        if (options.aggregation === 'min') value = Math.min(...numbers)
        if (options.aggregation === 'max') value = Math.max(...numbers)
        if (options.aggregation === 'sum') value = sum
        if (options.aggregation === 'avg') value = sum / numbers.length
      }
      if (value === undefined || !Number.isFinite(value)) return issue('noPoints')
      const datum = { value, pointId: id(raw) }
      return done({
        series: [{ type: 'gauge', min: options.min ?? Math.min(0, value), max: options.max ?? Math.max(100, value),
          progress: { show: true }, detail: { valueAnimation: false, fontSize: 18 },
          data: [datum] }],
      })
    }
    case 'graph':
    case 'sankey': {
      const nodeRows = new Map<string, QueryChartRow[]>()
      const links = points.flatMap(point => {
        const source = point.category
        const target = point.series
        if (!source || !target || source === target || (options.type === 'sankey' && point.value <= 0)) return []
        const rows = pointRows(point)
        for (const name of [source, target]) {
          if (!nodeRows.has(name)) nodeRows.set(name, [])
          nodeRows.get(name)!.push(...rows)
        }
        return [{ source, target, value: point.value, pointId: id(rows) }]
      })
      if (!links.length) return issue('noPoints')
      if (options.type === 'sankey' && hasCycle(links)) return issue('cycle')
      const data = [...nodeRows].map(([name, rows]) => ({ name, pointId: id([...new Set(rows)]) }))
      return done({ series: options.type === 'sankey'
        ? [{ type: 'sankey', data, links, emphasis: { focus: 'adjacency' }, lineStyle: { color: 'source', opacity: 0.35 } }]
        : [{ type: 'graph', layout: 'circular', roam: true, symbolSize: 16, data, links,
          label: { show: options.labels }, emphasis: { focus: 'adjacency' }, lineStyle: { curveness: 0.15 } }] })
    }
    case 'tree':
    case 'treemap':
    case 'sunburst': {
      type Node = { name: string; value: number; pointId: number; children?: Node[] }
      const groups = new Map<string, { leaves: Node[]; rows: QueryChartRow[] }>()
      const leaves: Node[] = []
      for (const point of points) {
        if (point.value < 0 || (point.value === 0 && options.type !== 'tree')) continue
        const rows = pointRows(point)
        const node = { name: point.category, value: point.value, pointId: id(rows) }
        if (!options.series || !point.series) leaves.push(node)
        else {
          if (!groups.has(point.series)) groups.set(point.series, { leaves: [], rows: [] })
          const group = groups.get(point.series)!
          group.leaves.push(node)
          group.rows.push(...rows)
        }
      }
      const data = [...leaves, ...[...groups].map(([name, group]) => ({
        name, value: group.leaves.reduce((total, leaf) => total + leaf.value, 0),
        pointId: id(group.rows), children: group.leaves,
      }))]
      if (!data.length) return issue('noPoints')
      if (options.type === 'tree') return done({ series: [{ type: 'tree', data: [{ name: options.name || category, children: data }],
        roam: true, expandAndCollapse: true, initialTreeDepth: 2, label: { show: true } }] })
      if (options.type === 'treemap') return done({ series: [{ type: 'treemap', data, breadcrumb: { show: false } }] })
      return done({ series: [{ type: 'sunburst', data, radius: [0, '90%'], label: { rotate: 'radial' } }] })
    }
    case 'map': {
      const map = mapSource(options.mapGeoJson)
      if (!map) return issue('invalidMap')
      const grouped = new Map<string, { value: number; rows: QueryChartRow[] }>()
      for (const point of points) {
        if (!map.names.has(point.category)) continue
        const entry = grouped.get(point.category) ?? { value: 0, rows: [] }
        entry.value += point.value
        entry.rows.push(...pointRows(point))
        grouped.set(point.category, entry)
      }
      if (!grouped.size) return issue('mapMismatch')
      const data = [...grouped].map(([name, entry]) => ({ name, value: entry.value, pointId: id(entry.rows) }))
      return {
        ...done({ visualMap: { min: Math.min(0, ...data.map(point => point.value)), max: Math.max(1, ...data.map(point => point.value)), show: false },
          series: [{ type: 'map', map: map.name, roam: true, data, label: { show: options.labels } }] }),
        map: { name: map.name, source: map.source },
      }
    }
    case 'themeRiver': {
      const groups = new Map<string, QueryChartRow[]>()
      const data: [number, number, string][] = []
      for (const point of points) {
        const time = timeValue(point.category)
        if (time === undefined || !point.series || point.value < 0) continue
        data.push([time, point.value, point.series])
        if (!groups.has(point.series)) groups.set(point.series, [])
        groups.get(point.series)!.push(...pointRows(point))
      }
      if (!data.length) return issue('noPoints')
      data.sort((a, b) => a[0] - b[0])
      const names = new Map([...groups].map(([name, rows]) => [name, id(rows)]))
      return { ...done({ singleAxis: { type: 'time', bottom: 32, top: 32 },
        series: [{ type: 'themeRiver', data, label: { show: options.labels } }] }), names }
    }
    default: return
  }
}
