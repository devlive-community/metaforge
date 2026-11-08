import type { CustomSeriesRenderItem, EChartsOption } from 'echarts'
import { chartDataSpec, geoJsonLimit, requiresChartSeries } from './query-chart-data'
import type { aggregateQueryChart, QueryChartOptions, QueryChartPoint, QueryChartRow } from './query-chart'

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
  const bound = Math.max(1, ...points.map(point => point.value))

  switch (options.type) {
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
