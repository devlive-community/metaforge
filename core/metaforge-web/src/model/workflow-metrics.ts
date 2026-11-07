/**
 * Metrics an executor may report for a workflow run. Each executor declares the ones it reports in
 * its workflow.yaml (`metrics:`), named after ExecutorMetrics fields; pages show exactly those.
 * When an executor starts or stops reporting a metric, update its declaration and this registry
 * together (tests/workflow-metrics.test.mjs checks every declared metric is registered here).
 */
export interface WorkflowMetricsSnapshot {
  rowsRead?: number; rowsWritten?: number; rowsSkipped?: number; rowsFailed?: number
  bytesRead?: number; bytesWritten?: number; totalRows?: number; totalSource?: string
  progress?: number; elapsedMs?: number; etaSeconds?: number
  readRowsPerSecond?: number; writeRowsPerSecond?: number; readBytesPerSecond?: number; writeBytesPerSecond?: number
  avgReadRowsPerSecond?: number; avgWriteRowsPerSecond?: number; peakWriteRowsPerSecond?: number
  batchesCommitted?: number; avgBatchMs?: number; channels?: number; queueDepth?: number; queueCapacity?: number
  batchRetries?: number; readerWaitMs?: number; writerWaitMs?: number; throttleWaitMs?: number; bottleneck?: string
  resumable?: boolean; splitStrategy?: string; splitColumn?: string | null
  splits?: { id: number; range: string; state: string; channel: number; rowsRead: number; rowsWritten: number; elapsedMs: number; retries?: number; watermark?: string | null }[]
  dirtySamples?: { split: number; error: string; row: Record<string, unknown> }[]
  stages?: { name: string; state: string; startedAt?: number; elapsedMs: number }[]
  series?: { at: number; readRowsPerSecond: number; writeRowsPerSecond: number }[]
}

export type WorkflowMetricGroup = 'volume' | 'throughput' | 'pipeline'
type Format = 'count' | 'bytes' | 'rowsRate' | 'bytesRate' | 'ms' | 'eta' | 'percent' | 'queue' | 'bottleneck'

/** Card metrics by group, in display order. `stages` and `series` are charts, not cards. */
export const WORKFLOW_METRICS: Record<string, { group: WorkflowMetricGroup; format: Format }> = {
  rowsRead: { group: 'volume', format: 'count' },
  rowsWritten: { group: 'volume', format: 'count' },
  rowsFailed: { group: 'volume', format: 'count' },
  rowsSkipped: { group: 'volume', format: 'count' },
  totalRows: { group: 'volume', format: 'count' },
  progress: { group: 'volume', format: 'percent' },
  etaSeconds: { group: 'volume', format: 'eta' },
  bytesRead: { group: 'volume', format: 'bytes' },
  bytesWritten: { group: 'volume', format: 'bytes' },
  readRowsPerSecond: { group: 'throughput', format: 'rowsRate' },
  writeRowsPerSecond: { group: 'throughput', format: 'rowsRate' },
  avgReadRowsPerSecond: { group: 'throughput', format: 'rowsRate' },
  avgWriteRowsPerSecond: { group: 'throughput', format: 'rowsRate' },
  peakWriteRowsPerSecond: { group: 'throughput', format: 'rowsRate' },
  readBytesPerSecond: { group: 'throughput', format: 'bytesRate' },
  writeBytesPerSecond: { group: 'throughput', format: 'bytesRate' },
  batchesCommitted: { group: 'pipeline', format: 'count' },
  batchRetries: { group: 'pipeline', format: 'count' },
  avgBatchMs: { group: 'pipeline', format: 'ms' },
  channels: { group: 'pipeline', format: 'count' },
  queue: { group: 'pipeline', format: 'queue' },
  readerWaitMs: { group: 'pipeline', format: 'ms' },
  writerWaitMs: { group: 'pipeline', format: 'ms' },
  throttleWaitMs: { group: 'pipeline', format: 'ms' },
  bottleneck: { group: 'pipeline', format: 'bottleneck' },
}
/** Declared metrics drawn as charts rather than cards. */
export const WORKFLOW_METRIC_CHARTS = ['stages', 'series', 'splits', 'dirtySamples'] as const
export const WORKFLOW_METRIC_GROUPS: WorkflowMetricGroup[] = ['volume', 'throughput', 'pipeline']

const units = ['B', 'KB', 'MB', 'GB', 'TB']
function bytes(value: number): string {
  let size = value, unit = 0
  while (size >= 1024 && unit < units.length - 1) { size /= 1024; unit++ }
  return `${unit ? size.toFixed(1) : Math.round(size)} ${units[unit]}`
}
const number = (value: number, digits = 0) => value.toLocaleString(undefined, { maximumFractionDigits: digits })
export function duration(ms: number): string {
  if (ms < 1000) return `${Math.round(ms)}ms`
  const seconds = ms / 1000
  if (seconds < 60) return `${seconds.toFixed(1)}s`
  const minutes = Math.floor(seconds / 60)
  return minutes < 60 ? `${minutes}m ${Math.round(seconds % 60)}s` : `${Math.floor(minutes / 60)}h ${minutes % 60}m`
}

/**
 * The display text of one metric, or undefined when the run has no value for it (the executor
 * reports -1 for unknown totals, progress and ETA). Enumerations return their key for translation.
 */
export function metricText(key: string, metrics: WorkflowMetricsSnapshot | undefined): string | undefined {
  const definition = WORKFLOW_METRICS[key]
  if (!definition || !metrics) return undefined
  if (definition.format === 'queue') {
    return typeof metrics.queueDepth === 'number' && metrics.queueCapacity ? `${metrics.queueDepth} / ${metrics.queueCapacity}` : undefined
  }
  const value = (metrics as Record<string, unknown>)[key]
  if (definition.format === 'bottleneck') return typeof value === 'string' ? value : undefined
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) return undefined
  switch (definition.format) {
    case 'bytes': return bytes(value)
    case 'bytesRate': return `${bytes(value)}/s`
    case 'rowsRate': return `${number(value, 1)} rows/s`
    case 'ms': return duration(value)
    case 'eta': return duration(value * 1000)
    case 'percent': return `${number(value, 1)}%`
    default: return number(value)
  }
}

/** Declared card metrics grouped for display, skipping unknown keys and charts. */
export function metricGroups(declared: string[] | undefined): { group: WorkflowMetricGroup; keys: string[] }[] {
  const keys = (declared || []).filter(key => key in WORKFLOW_METRICS)
  return WORKFLOW_METRIC_GROUPS.map(group => ({ group, keys: keys.filter(key => WORKFLOW_METRICS[key].group === group) }))
    .filter(entry => entry.keys.length)
}

const finite = (value: unknown): number | undefined => typeof value === 'number' && Number.isFinite(value) ? value : undefined
/** A numeric metric, or `fallback` when the run did not report it (older records, other executors). */
export const metricNumber = (metrics: WorkflowMetricsSnapshot | undefined | null, key: keyof WorkflowMetricsSnapshot, fallback = 0): number =>
  finite(metrics?.[key]) ?? fallback
/** Rate samples with every field present, in time order; malformed samples are dropped. */
export function ratePoints(metrics: WorkflowMetricsSnapshot | undefined | null): { at: number; read: number; write: number }[] {
  const series = Array.isArray(metrics?.series) ? metrics!.series : []
  return series.flatMap(point => {
    const at = finite(point?.at)
    return at === undefined ? [] : [{ at, read: Math.max(0, finite(point.readRowsPerSecond) ?? 0), write: Math.max(0, finite(point.writeRowsPerSecond) ?? 0) }]
  }).sort((left, right) => left.at - right.at)
}
/** Splits with an id and every count present; malformed entries are dropped. */
export function splitList(metrics: WorkflowMetricsSnapshot | undefined | null) {
  const splits = Array.isArray(metrics?.splits) ? metrics!.splits : []
  return splits.flatMap(split => finite(split?.id) === undefined ? [] : [{
    id: split.id, range: typeof split.range === 'string' ? split.range : '', state: typeof split.state === 'string' ? split.state : 'PENDING',
    channel: Math.max(0, finite(split.channel) ?? 0), rowsRead: Math.max(0, finite(split.rowsRead) ?? 0), rowsWritten: Math.max(0, finite(split.rowsWritten) ?? 0),
    elapsedMs: Math.max(0, finite(split.elapsedMs) ?? 0), retries: Math.max(0, finite(split.retries) ?? 0),
  }])
}
/** Rejected-row samples with the row shown as one line of JSON; malformed entries are dropped. */
export function dirtyList(metrics: WorkflowMetricsSnapshot | undefined | null) {
  const samples = Array.isArray(metrics?.dirtySamples) ? metrics!.dirtySamples : []
  return samples.flatMap((sample, index) => !sample || typeof sample !== 'object' ? [] : [{
    key: index, split: finite(sample.split) ?? 0, error: typeof sample.error === 'string' ? sample.error : '', row: JSON.stringify(sample.row ?? {}),
  }])
}
/** Stages with a name, a state and a non-negative duration; malformed entries are dropped. */
export function stageList(metrics: WorkflowMetricsSnapshot | undefined | null): { name: string; state: string; elapsedMs: number }[] {
  const stages = Array.isArray(metrics?.stages) ? metrics!.stages : []
  return stages.flatMap(stage => typeof stage?.name === 'string' && stage.name
    ? [{ name: stage.name, state: typeof stage.state === 'string' ? stage.state : 'PENDING', elapsedMs: Math.max(0, finite(stage.elapsedMs) ?? 0) }] : [])
}
