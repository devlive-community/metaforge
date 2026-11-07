<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import EChart from '@/views/components/visual/EChart.vue'
import { dirtyList, duration, metricNumber, metricText, ratePoints, splitList, type WorkflowMetricsSnapshot } from '@/model/workflow-metrics'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
/**
 * The metrics an executor declared for its runs, drawn as charts: progress, read-versus-written
 * volumes, the rate trend with its average and peak, and the reader/writer pipeline. A chart only
 * appears when the executor declares the metrics it needs; nothing undeclared is shown.
 */
const props = defineProps<{ metrics?: WorkflowMetricsSnapshot; declared?: string[]; compact?: boolean }>()
const { t, te } = useI18n()
const has = (...keys: string[]) => keys.every(key => props.declared?.includes(key))
const label = (key: string) => te('workflow.studio.metric.' + key) ? t('workflow.studio.metric.' + key) : key
const text = (key: string) => {
  const value = metricText(key, props.metrics)
  // The total is counted before the run unless the workflow turned pre-counting off; it is then known once the run ends.
  if (value === undefined) return key === 'totalRows' ? t('workflow.studio.totalPending') : '—'
  return te(`workflow.studio.${ key }.${ value }`) ? t(`workflow.studio.${ key }.${ value }`) : value
}
// Every value goes through metricNumber / ratePoints: older records and other executors may omit fields.
const num = (key: keyof WorkflowMetricsSnapshot, fallback = 0) => metricNumber(props.metrics, key, fallback)

// Theme colors are read from tokens and re-read when the theme switches.
const themeVersion = ref(0)
let observer: MutationObserver | undefined
onMounted(() => {
  observer = new MutationObserver(() => { themeVersion.value++ })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'class', 'style'] })
})
onBeforeUnmount(() => observer?.disconnect())
const colors = computed(() => {
  themeVersion.value
  const style = getComputedStyle(document.documentElement)
  const token = (name: string, fallback: string) => style.getPropertyValue('--mf-color-' + name).trim() || fallback
  const dark = document.documentElement.dataset.theme === 'dark'
  // Read and write are the two series everywhere; the pair passes the palette checks in both modes
  // (the dark theme's info blue is too light for marks on the dark surface, so it is stepped down).
  return {
    read: dark ? '#4F8EEF' : token('info', '#3B82F6'), write: token('primary', '#0A8F67'),
    track: token('line', '#E3EAE7'), axis: token('content-tertiary', '#7D8D87'), text: token('content', '#1F2A26'),
    surface: token('surface', '#ffffff'), warning: token('warning', '#E5A11A'),
  }
})
const axisLabel = computed(() => ({ color: colors.value.axis, fontSize: 10 }))
const splitLine = computed(() => ({ lineStyle: { color: colors.value.track } }))
const tooltip = computed(() => ({ backgroundColor: colors.value.surface, borderColor: colors.value.track, textStyle: { color: colors.value.text, fontSize: 12 } }))

/** A single-value ring: share of a whole, with the headline in the middle. */
function ring(value: number, center: string, caption: string, color: string) {
  return {
    series: [{
      type: 'pie' as const, radius: ['72%', '86%'], silent: true, label: { show: false }, labelLine: { show: false },
      itemStyle: { borderColor: colors.value.surface, borderWidth: 2, borderRadius: 4 },
      data: [{ value: Math.max(0, Math.min(100, value)), itemStyle: { color } }, { value: 100 - Math.max(0, Math.min(100, value)), itemStyle: { color: colors.value.track } }],
    }],
    title: { text: center, subtext: caption, left: 'center', top: '36%', itemGap: 4,
      textStyle: { fontSize: props.compact ? 16 : 22, fontWeight: 600, color: colors.value.text }, subtextStyle: { fontSize: 11, color: colors.value.axis } },
  }
}
/** Horizontal read-versus-write bars on one axis of one unit. */
function pair(readKey: string, writeKey: string, format: (value: number) => string) {
  const values = [num(readKey as keyof WorkflowMetricsSnapshot), num(writeKey as keyof WorkflowMetricsSnapshot)]
  return {
    grid: { left: 8, right: 64, top: 4, bottom: 4, containLabel: true },
    tooltip: { ...tooltip.value, trigger: 'item' as const, formatter: (item: any) => `${item.name}: ${format(item.value)}` },
    xAxis: { type: 'value' as const, show: false },
    yAxis: { type: 'category' as const, inverse: true, data: [label(readKey), label(writeKey)], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { ...axisLabel.value, fontSize: 11 } },
    series: [{ type: 'bar' as const, barWidth: props.compact ? 10 : 14, data: values.map((value, index) => ({ value, itemStyle: { color: index ? colors.value.write : colors.value.read, borderRadius: [0, 4, 4, 0] } })),
      label: { show: true, position: 'right' as const, color: colors.value.text, fontSize: 11, formatter: (item: any) => format(item.value) } }],
  }
}
const count = (value: number) => value.toLocaleString()
const bytes = (value: number) => metricText('bytesRead', { bytesRead: value }) || '0 B'
const ms = (value: number) => duration(value)

const progressChart = computed(() => {
  const progress = num('progress', -1)
  const known = progress >= 0
  return ring(known ? progress : 0, known ? `${Math.round(progress)}%` : count(num('rowsWritten')),
    known ? label('progress') : label('rowsWritten'), colors.value.write)
})
const rowsChart = computed(() => pair('rowsRead', 'rowsWritten', count))
const bytesChart = computed(() => pair('bytesRead', 'bytesWritten', bytes))
const waitChart = computed(() => pair('readerWaitMs', 'writerWaitMs', ms))
const queueChart = computed(() => {
  const capacity = num('queueCapacity'), depth = num('queueDepth')
  return ring(capacity ? depth / capacity * 100 : 0, capacity ? `${depth}/${capacity}` : '—', label('queue'), colors.value.read)
})
const series = computed(() => has('series') ? ratePoints(props.metrics) : [])
const trendChart = computed(() => {
  const reference = (value: number | undefined, name: string, color: string, position: 'insideStartTop' | 'insideEndTop') => value && value > 0
    ? [{ yAxis: Math.round(value), name, lineStyle: { color, type: 'dashed' as const, width: 1 }, label: { formatter: `${name} ${Math.round(value)}`, color: colors.value.axis, fontSize: 10, position } }] : []
  return {
    color: [colors.value.read, colors.value.write],
    // Keep the legend on one row and reserve a separate line for the axis unit.
    grid: { left: 8, right: 16, top: 56, bottom: 8, containLabel: true },
    legend: { type: 'scroll' as const, top: 0, left: 0, right: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 4, textStyle: { color: colors.value.axis, fontSize: 11 }, pageIconColor: colors.value.axis, pageTextStyle: { color: colors.value.axis } },
    tooltip: { ...tooltip.value, trigger: 'axis' as const, axisPointer: { type: 'line' as const, lineStyle: { color: colors.value.axis } } },
    xAxis: { type: 'category' as const, boundaryGap: false, data: series.value.map(point => duration(point.at)), axisLabel: axisLabel.value, axisLine: { lineStyle: { color: colors.value.track } }, axisTick: { show: false } },
    yAxis: { type: 'value' as const, name: 'rows/s', nameTextStyle: { color: colors.value.axis, fontSize: 10, align: 'left' as const }, axisLabel: axisLabel.value, splitLine: splitLine.value },
    series: [
      { name: label('readRowsPerSecond'), type: 'line' as const, smooth: true, showSymbol: false, lineStyle: { width: 2 }, data: series.value.map(point => Math.round(point.read)) },
      { name: label('writeRowsPerSecond'), type: 'line' as const, smooth: true, showSymbol: false, lineStyle: { width: 2 }, data: series.value.map(point => Math.round(point.write)),
        markLine: { symbol: 'none', silent: true, data: [
          ...(has('avgWriteRowsPerSecond') ? reference(num('avgWriteRowsPerSecond'), t('workflow.studio.metricAverage'), colors.value.axis, 'insideStartTop') : []),
          ...(has('peakWriteRowsPerSecond') ? reference(num('peakWriteRowsPerSecond'), t('workflow.studio.metricPeak'), colors.value.write, 'insideEndTop') : []),
        ] } },
    ],
  }
})
/** Small figures beside a chart: only declared metrics, in the given order. */
const chips = (keys: string[]) => keys.filter(key => has(key)).map(key => ({ key, label: label(key), value: text(key) }))
const volumeChips = computed(() => chips(['totalRows', 'totalSource', 'elapsedMs', 'etaSeconds', 'rowsSkipped', 'rowsFailed']))
const rateChips = computed(() => chips(['readRowsPerSecond', 'writeRowsPerSecond', 'avgWriteRowsPerSecond', 'peakWriteRowsPerSecond', 'readBytesPerSecond', 'writeBytesPerSecond']))
const pipelineChips = computed(() => chips(['batchesCommitted', 'batchRetries', 'avgBatchMs', 'channels', 'throttleWaitMs']))
// Splits: only worth a table when the run really split the query; rejected rows only when there are any.
const splits = computed(() => has('splits') ? splitList(props.metrics) : [])
const splitsDone = computed(() => splits.value.filter(split => split.state === 'DONE').length)
const splitColumns = computed(() => [
  { title: t('workflow.studio.split.id'), dataIndex: 'id', key: 'id' },
  { title: t('workflow.studio.split.range'), dataIndex: 'range', key: 'range', width: 260, ellipsis: true },
  { title: t('common.state'), dataIndex: 'state', key: 'state' },
  { title: t('workflow.studio.split.channel'), dataIndex: 'channel', key: 'channel' },
  { title: label('rowsRead'), dataIndex: 'rowsRead', key: 'rowsRead' },
  { title: label('rowsWritten'), dataIndex: 'rowsWritten', key: 'rowsWritten' },
  { title: label('batchRetries'), dataIndex: 'retries', key: 'retries' },
  { title: t('workflow.studio.duration'), dataIndex: 'elapsedMs', key: 'elapsedMs' },
])
const splitVariant = (state: string) => state === 'DONE' ? 'success' : state === 'FAILED' ? 'danger' : state === 'RUNNING' ? 'primary' : 'default'
const stageState = (state: string) => te('workflow.studio.stageState.' + state) ? t('workflow.studio.stageState.' + state) : state
const dirty = computed(() => has('dirtySamples') ? dirtyList(props.metrics) : [])
const dirtyColumns = computed(() => [
  { title: t('workflow.studio.split.id'), dataIndex: 'split', key: 'split' },
  { title: t('workflow.studio.dirty.error'), dataIndex: 'error', key: 'error', width: 320, ellipsis: true },
  { title: t('workflow.studio.dirty.row'), dataIndex: 'row', key: 'row', width: 420, ellipsis: true },
])
const bottleneck = computed(() => has('bottleneck') && typeof props.metrics?.bottleneck === 'string' ? props.metrics.bottleneck : undefined)
const card = computed(() => props.compact ? 'rounded-mf-md border border-line bg-surface p-3' : 'rounded-mf-lg border border-line bg-surface p-5')
const title = computed(() => props.compact ? 'mb-2 text-xs font-semibold text-content-secondary' : 'mb-3 text-sm font-semibold text-content')
const chartHeight = computed(() => props.compact ? 'h-28' : 'h-36')
</script>
<template>
  <MfEmpty v-if="!declared?.length" class="h-full" size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.metricsUnsupported')"/>
  <MfEmpty v-else-if="!metrics" class="h-full" size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.engineMetricsHint')"/>
  <div v-else class="grid min-w-0 gap-3" :class="compact ? 'grid-cols-1 md:grid-cols-2 xl:grid-cols-4' : 'grid-cols-1 lg:grid-cols-2 xl:grid-cols-4 gap-4'">
    <!-- Volume: progress ring and read-versus-written rows and bytes -->
    <section v-if="has('rowsWritten')" :class="card">
      <h3 :class="title">{{ $t('workflow.studio.metricGroup.volume') }}</h3>
      <EChart :class="chartHeight" :option="progressChart"/>
      <dl v-if="volumeChips.length" class="mt-2 grid grid-cols-2 gap-2 text-xs"><div v-for="chip in volumeChips" :key="chip.key" class="min-w-0"><dt class="truncate text-content-tertiary">{{ chip.label }}</dt><dd class="truncate font-medium text-content tabular-nums">{{ chip.value }}</dd></div></dl>
    </section>
    <section v-if="has('rowsRead', 'rowsWritten')" :class="card">
      <h3 :class="title">{{ $t('workflow.studio.metricRows') }}</h3>
      <EChart :class="chartHeight" :option="rowsChart"/>
      <template v-if="has('bytesRead', 'bytesWritten')">
        <h3 :class="[title, 'mt-2']">{{ $t('workflow.studio.metricBytes') }}</h3>
        <EChart :class="compact ? 'h-20' : 'h-24'" :option="bytesChart"/>
      </template>
    </section>
    <!-- Throughput: rate trend with average and peak reference lines -->
    <section v-if="has('series') || rateChips.length" :class="[card, 'md:col-span-2']">
      <h3 :class="title">{{ $t('workflow.studio.rateTrend') }}</h3>
      <EChart v-if="series.length > 1" :class="compact ? 'h-36' : 'h-52'" :option="trendChart"/>
      <MfEmpty v-else-if="has('series')" :class="compact ? 'h-36' : 'h-52'" size="sm" :title="$t('workflow.studio.rateTrendPending')"/>
      <dl v-if="rateChips.length" class="mt-3 grid grid-cols-[repeat(auto-fill,minmax(120px,1fr))] gap-2 text-xs"><div v-for="chip in rateChips" :key="chip.key" class="min-w-0 rounded-mf-md bg-surface-hover px-2.5 py-1.5"><dt class="truncate text-content-tertiary" :title="chip.label">{{ chip.label }}</dt><dd class="truncate font-medium text-content tabular-nums">{{ chip.value }}</dd></div></dl>
    </section>
    <!-- Pipeline: buffer queue, where readers and writers waited, and the current bottleneck -->
    <section v-if="has('queue') || has('readerWaitMs', 'writerWaitMs') || pipelineChips.length" :class="[card, compact ? 'md:col-span-2 xl:col-span-4' : 'lg:col-span-2 xl:col-span-4']">
      <div class="flex items-center justify-between gap-2" :class="compact ? 'mb-2' : 'mb-3'">
        <h3 class="m-0" :class="title.replace(/mb-\d/, '')">{{ $t('workflow.studio.metricGroup.pipeline') }}</h3>
        <MfTag v-if="bottleneck" size="sm" :variant="bottleneck === 'NONE' ? 'default' : 'warning'">{{ label('bottleneck') }} · {{ text('bottleneck') }}</MfTag>
      </div>
      <div class="grid items-center gap-4" :class="compact ? 'grid-cols-1 md:grid-cols-[160px_minmax(0,1fr)_minmax(0,1fr)]' : 'grid-cols-1 md:grid-cols-[200px_minmax(0,1fr)_minmax(0,1fr)]'">
        <EChart v-if="has('queue')" :class="chartHeight" :option="queueChart"/>
        <div v-if="has('readerWaitMs', 'writerWaitMs')" class="min-w-0"><p class="mb-1 text-[11px] text-content-tertiary">{{ $t('workflow.studio.metricWaits') }}</p><EChart :class="compact ? 'h-20' : 'h-24'" :option="waitChart"/></div>
        <dl v-if="pipelineChips.length" class="grid grid-cols-[repeat(auto-fill,minmax(120px,1fr))] gap-2 text-xs"><div v-for="chip in pipelineChips" :key="chip.key" class="min-w-0 rounded-mf-md bg-surface-hover px-2.5 py-1.5"><dt class="truncate text-content-tertiary">{{ chip.label }}</dt><dd class="truncate font-medium text-content tabular-nums">{{ chip.value }}</dd></div></dl>
      </div>
    </section>
    <!-- Splits: how the query was divided and how far each part got -->
    <section v-if="splits.length > 1" :class="[card, compact ? 'md:col-span-2 xl:col-span-4' : 'lg:col-span-2 xl:col-span-4']">
      <div class="flex flex-wrap items-center gap-2" :class="compact ? 'mb-2' : 'mb-3'">
        <h3 class="m-0" :class="title.replace(/mb-\d/, '')">{{ $t('workflow.studio.split.title') }}</h3>
        <MfTag size="sm">{{ splitsDone }} / {{ splits.length }}</MfTag>
        <MfTag v-if="metrics.splitColumn" size="sm">{{ $t('workflow.studio.split.column') }} · {{ metrics.splitColumn }}</MfTag>
        <MfTag size="sm" :variant="metrics.resumable ? 'primary' : 'default'">{{ metrics.resumable ? $t('workflow.studio.split.resumable') : $t('workflow.studio.split.notResumable') }}</MfTag>
      </div>
      <div class="max-h-80 overflow-auto [&_thead_th]:sticky [&_thead_th]:top-0 [&_thead_th]:z-20">
        <MfTable :columns="splitColumns" :data-source="splits" row-key="id" density="compact">
          <template #cell-range="{ record }"><EllipsisCell :text="String(record.range)"/></template>
          <template #cell-state="{ record }"><MfTag size="sm" :variant="splitVariant(String(record.state))">{{ stageState(String(record.state)) }}</MfTag></template>
          <template #cell-channel="{ record }">{{ record.channel || '—' }}</template>
          <template #cell-rowsRead="{ record }"><span class="tabular-nums">{{ count(Number(record.rowsRead)) }}</span></template>
          <template #cell-rowsWritten="{ record }"><span class="tabular-nums">{{ count(Number(record.rowsWritten)) }}</span></template>
          <template #cell-retries="{ record }"><span class="tabular-nums">{{ count(Number(record.retries)) }}</span></template>
          <template #cell-elapsedMs="{ record }"><span class="tabular-nums">{{ ms(Number(record.elapsedMs)) }}</span></template>
        </MfTable>
      </div>
    </section>
    <!-- Rejected rows: the first samples; all of them are in the task's dirty file -->
    <section v-if="dirty.length" :class="[card, compact ? 'md:col-span-2 xl:col-span-4' : 'lg:col-span-2 xl:col-span-4']">
      <div class="flex flex-wrap items-center gap-2" :class="compact ? 'mb-2' : 'mb-3'">
        <h3 class="m-0" :class="title.replace(/mb-\d/, '')">{{ $t('workflow.studio.dirty.title') }}</h3>
        <MfTag size="sm" variant="warning">{{ text('rowsSkipped') }}</MfTag>
      </div>
      <div class="max-h-80 overflow-auto [&_thead_th]:sticky [&_thead_th]:top-0 [&_thead_th]:z-20">
        <MfTable :columns="dirtyColumns" :data-source="dirty" row-key="key" density="compact">
          <template #cell-error="{ record }"><EllipsisCell :text="String(record.error)"/></template>
          <template #cell-row="{ record }"><EllipsisCell :text="String(record.row)"/></template>
        </MfTable>
      </div>
    </section>
  </div>
</template>
