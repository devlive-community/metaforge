import { dashboardTimestamp, dashboardValueMissing } from './dashboard-filters'
import type { QueryChartRow } from './query-chart'

interface ProcessVisit {
  stage: string
  entered: number
  rows: QueryChartRow[]
}

export interface DashboardProcessTrace {
  visits: ProcessVisit[]
  rows: QueryChartRow[]
  first: number
  last: number
}

function scalar(value: unknown) {
  if (dashboardValueMissing(value)) return ''
  if (typeof value === 'string' || typeof value === 'boolean' || typeof value === 'number' && Number.isFinite(value)) return String(value)
  return ''
}

export function dashboardProcessTraces(rows: readonly QueryChartRow[], entityField: string, stageField: string, timeField: string) {
  const events = new Map<string, { stage: string; time: number; row: QueryChartRow; index: number }[]>()
  let invalid = 0
  rows.forEach((row, index) => {
    const id = row[entityField], entity = scalar(id), stage = scalar(row[stageField]), time = dashboardTimestamp(row[timeField])
    if (!entity || !stage || time === undefined) {
      invalid++
      return
    }
    const key = JSON.stringify([typeof id, entity])
    const items = events.get(key) ?? []
    items.push({ stage, time, row, index })
    events.set(key, items)
  })

  const traces: DashboardProcessTrace[] = []
  let ambiguous = 0
  for (const items of events.values()) {
    items.sort((a, b) => a.time - b.time || a.index - b.index)
    if (items.some((item, index) => index > 0 && item.time === items[index - 1].time && item.stage !== items[index - 1].stage)) {
      ambiguous++
      continue
    }
    const visits: ProcessVisit[] = []
    for (const item of items) {
      const previous = visits.at(-1)
      if (previous?.stage === item.stage) previous.rows.push(item.row)
      else visits.push({ stage: item.stage, entered: item.time, rows: [item.row] })
    }
    traces.push({ visits, rows: items.map(item => item.row), first: items[0].time, last: items.at(-1)!.time })
  }
  return { traces, invalid, ambiguous }
}

export function dashboardProcessPaths(traces: readonly DashboardProcessTrace[], limit: number) {
  const groups = new Map<string, { key: string; stages: string[]; count: number; rows: QueryChartRow[] }>()
  for (const trace of traces) {
    const stages = trace.visits.map(visit => visit.stage), key = JSON.stringify(stages)
    const group = groups.get(key) ?? { key, stages, count: 0, rows: [] }
    group.count++
    for (const row of trace.rows) group.rows.push(row)
    groups.set(key, group)
  }
  return [...groups.values()].sort((a, b) => b.count - a.count || a.key.localeCompare(b.key)).slice(0, limit)
}

export function dashboardProcessEndpoints(traces: readonly DashboardProcessTrace[], limit: number) {
  function countSide(side: 'entry' | 'exit') {
    const groups = new Map<string, { stage: string; count: number; rows: QueryChartRow[] }>()
    for (const trace of traces) {
      const stage = (side === 'entry' ? trace.visits[0] : trace.visits.at(-1)!).stage
      const group = groups.get(stage) ?? { stage, count: 0, rows: [] }
      group.count++
      for (const row of trace.rows) group.rows.push(row)
      groups.set(stage, group)
    }
    return [...groups.values()].sort((a, b) => b.count - a.count || a.stage.localeCompare(b.stage)).slice(0, limit)
  }
  return { entry: countSide('entry'), exit: countSide('exit') }
}

function durationStats(values: number[]) {
  values.sort((a, b) => a - b)
  function percentile(ratio: number) {
    const index = (values.length - 1) * ratio, low = Math.floor(index)
    return values[low] + (values[Math.ceil(index)] - values[low]) * (index - low)
  }
  return { count: values.length, median: percentile(.5), p90: percentile(.9) }
}

export function dashboardProcessCycles(traces: readonly DashboardProcessTrace[], limit: number) {
  const groups = new Map<string, { stage: string; durations: number[]; rows: QueryChartRow[] }>()
  for (const trace of traces) {
    if (trace.last === trace.first) continue
    const stage = trace.visits.at(-1)!.stage
    const group = groups.get(stage) ?? { stage, durations: [], rows: [] }
    group.durations.push(trace.last - trace.first)
    for (const row of trace.rows) group.rows.push(row)
    groups.set(stage, group)
  }
  return [...groups.values()].map(group => ({ stage: group.stage, rows: group.rows, ...durationStats(group.durations) }))
    .sort((a, b) => b.count - a.count || a.stage.localeCompare(b.stage)).slice(0, limit)
}

export function dashboardProcessWaits(traces: readonly DashboardProcessTrace[], limit: number) {
  const groups = new Map<string, { key: string; from: string; to: string; durations: number[]; rows: Set<QueryChartRow> }>()
  for (const trace of traces) {
    for (let index = 1; index < trace.visits.length; index++) {
      const previous = trace.visits[index - 1], next = trace.visits[index]
      const key = JSON.stringify([previous.stage, next.stage])
      const group = groups.get(key) ?? { key, from: previous.stage, to: next.stage, durations: [], rows: new Set<QueryChartRow>() }
      group.durations.push(next.entered - previous.entered)
      for (const row of previous.rows) group.rows.add(row)
      for (const row of next.rows) group.rows.add(row)
      groups.set(key, group)
    }
  }
  return [...groups.values()].map(group => ({ key: group.key, from: group.from, to: group.to, rows: [...group.rows], ...durationStats(group.durations) }))
    .sort((a, b) => b.median - a.median || b.count - a.count || a.key.localeCompare(b.key)).slice(0, limit)
}

export function dashboardProcessRework(traces: readonly DashboardProcessTrace[], limit: number) {
  const groups = new Map<string, { stage: string; count: number; revisits: number; rows: QueryChartRow[] }>()
  for (const trace of traces) {
    const seen = new Set<string>(), revisits = new Map<string, number>()
    for (const { stage } of trace.visits) {
      if (seen.has(stage)) revisits.set(stage, (revisits.get(stage) ?? 0) + 1)
      seen.add(stage)
    }
    for (const [stage, count] of revisits) {
      const group = groups.get(stage) ?? { stage, count: 0, revisits: 0, rows: [] }
      group.count++
      group.revisits += count
      for (const row of trace.rows) group.rows.push(row)
      groups.set(stage, group)
    }
  }
  return [...groups.values()].sort((a, b) => b.count - a.count || b.revisits - a.revisits || a.stage.localeCompare(b.stage)).slice(0, limit)
}

export function dashboardProcessCompliance(traces: readonly DashboardProcessTrace[], required: readonly string[]) {
  const results = ['matched', 'outOfOrder', 'missing'].map(label => ({ label, count: 0, rows: [] as QueryChartRow[] }))
  for (const trace of traces) {
    const stages = new Set(trace.visits.map(visit => visit.stage))
    let index = 0
    for (const visit of trace.visits) {
      if (visit.stage === required[index]) index++
    }
    const group = index === required.length ? results[0] : required.every(stage => stages.has(stage)) ? results[1] : results[2]
    group.count++
    for (const row of trace.rows) group.rows.push(row)
  }
  return results
}
