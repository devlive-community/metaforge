<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EChart from '@/views/components/visual/EChart.vue'
import { metricGroups, metricText, type WorkflowMetricsSnapshot } from '@/model/workflow-metrics'
/**
 * The metrics an executor declared for its runs, grouped into volume, throughput and pipeline
 * cards, plus the rate trend when it reports a series. Nothing undeclared is shown.
 */
const props = defineProps<{ metrics?: WorkflowMetricsSnapshot; declared?: string[]; compact?: boolean }>()
const { t, te } = useI18n()
const groups = computed(() => metricGroups(props.declared))
const label = (key: string) => te('workflow.studio.metric.' + key) ? t('workflow.studio.metric.' + key) : key
function value(key: string) {
  const text = metricText(key, props.metrics)
  if (text === undefined) return '—'
  return key === 'bottleneck' && te('workflow.studio.bottleneck.' + text) ? t('workflow.studio.bottleneck.' + text) : text
}
const series = computed(() => props.declared?.includes('series') ? props.metrics?.series || [] : [])
const token = (name: string) => getComputedStyle(document.documentElement).getPropertyValue('--mf-color-' + name).trim()
const trend = computed(() => {
  const read = token('info') || '#2f80ed', write = token('primary') || '#0a8f67', line = token('line') || '#e5eeeb'
  return {
    color: [read, write],
    grid: { left: 48, right: 16, top: 32, bottom: 28 },
    legend: { top: 0, right: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 11 } },
    tooltip: { trigger: 'axis' as const },
    xAxis: { type: 'category' as const, boundaryGap: false, data: series.value.map(point => (point.at / 1000).toFixed(1) + 's'), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value' as const, name: 'rows/s', nameTextStyle: { fontSize: 10 }, axisLabel: { fontSize: 10 }, splitLine: { lineStyle: { color: line } } },
    series: [
      { name: t('workflow.studio.metric.readRowsPerSecond'), type: 'line' as const, smooth: true, showSymbol: false, data: series.value.map(point => Math.round(point.readRowsPerSecond)) },
      { name: t('workflow.studio.metric.writeRowsPerSecond'), type: 'line' as const, smooth: true, showSymbol: false, data: series.value.map(point => Math.round(point.writeRowsPerSecond)) },
    ],
  }
})
</script>
<template>
  <MfEmpty v-if="!declared?.length" class="h-full" size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.metricsUnsupported')"/>
  <MfEmpty v-else-if="!metrics" class="h-full" size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.engineMetricsHint')"/>
  <div v-else :class="compact ? 'space-y-3' : 'space-y-5'">
    <section v-for="entry in groups" :key="entry.group" :class="!compact && 'rounded-mf-lg border border-line bg-surface p-5'">
      <h3 class="text-xs font-semibold text-content-tertiary" :class="compact ? 'mb-1.5' : 'mb-3 text-sm text-content'">{{ $t('workflow.studio.metricGroup.' + entry.group) }}</h3>
      <div class="grid gap-2" :class="compact ? 'grid-cols-[repeat(auto-fill,minmax(132px,1fr))]' : 'grid-cols-[repeat(auto-fill,minmax(168px,1fr))] gap-3'">
        <div v-for="key in entry.keys" :key="key" class="min-w-0 rounded-mf-md bg-surface-hover" :class="compact ? 'px-3 py-2' : 'px-4 py-3'">
          <p class="truncate text-[11px] text-content-tertiary" :title="label(key)">{{ label(key) }}</p>
          <strong class="mt-1 block truncate text-content tabular-nums" :class="compact ? 'text-sm' : 'text-base'">{{ value(key) }}</strong>
        </div>
      </div>
    </section>
    <section v-if="series.length > 1" :class="compact ? '' : 'rounded-mf-lg border border-line bg-surface p-5'">
      <h3 class="text-xs font-semibold text-content-tertiary" :class="compact ? 'mb-1.5' : 'mb-2 text-sm text-content'">{{ $t('workflow.studio.rateTrend') }}</h3>
      <EChart :class="compact ? 'h-40' : 'h-64'" :option="trend"/>
    </section>
  </div>
</template>
