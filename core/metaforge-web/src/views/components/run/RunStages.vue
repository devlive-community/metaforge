<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EChart from '@/views/components/visual/EChart.vue'
import { duration, stageList, type WorkflowMetricsSnapshot } from '@/model/workflow-metrics'
/** The stages an executor reports for a run, as cards and a timing chart; shared by workflow runs and dataset syncs. */
const props = defineProps<{ metrics?: WorkflowMetricsSnapshot | null }>()
const { t, te } = useI18n()
const stages = computed(() => stageList(props.metrics))
const elapsed = (value?: number) => value == null || !Number.isFinite(value) ? '—' : duration(value)
/** Stage names and states come from the executor; known ones are translated, others shown as reported. */
const stageName = (name: string) => te('workflow.studio.stage.' + name) ? t('workflow.studio.stage.' + name) : name
const stageState = (state: string) => te('workflow.studio.stageState.' + state) ? t('workflow.studio.stageState.' + state) : state
const stageChart = computed(() => {
  const style = getComputedStyle(document.documentElement)
  const token = (name: string, fallback: string) => style.getPropertyValue('--mf-color-' + name).trim() || fallback
  const axis = { color: token('content-tertiary', '#7D8D87'), fontSize: 10 }
  return { grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true }, tooltip: { trigger: 'axis' as const, valueFormatter: (value: unknown) => elapsed(Number(value)) },
    xAxis: { type: 'category' as const, data: stages.value.map(stage => stageName(stage.name)), axisLabel: axis, axisTick: { show: false }, axisLine: { lineStyle: { color: token('line', '#E3EAE7') } } },
    yAxis: { type: 'value' as const, name: 'ms', nameTextStyle: axis, axisLabel: axis, splitLine: { lineStyle: { color: token('line', '#E3EAE7') } } },
    series: [{ type: 'bar' as const, data: stages.value.map(stage => stage.elapsedMs), barMaxWidth: 24, itemStyle: { color: token('primary', '#0A8F67'), borderRadius: [4, 4, 0, 0] as [number, number, number, number] } }] }
})
</script>
<template>
  <div class="grid gap-4 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,1fr)]">
      <section class="rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-4 text-sm font-semibold text-content">{{ $t('workflow.studio.executionStages') }}</h2><div v-if="stages.length" class="grid grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-3"><div v-for="stage in stages" :key="stage.name" class="rounded-mf-md border border-line bg-surface-hover p-3"><MfTag :variant="stage.state === 'DONE' ? 'success' : stage.state === 'FAILED' ? 'danger' : stage.state === 'RUNNING' ? 'primary' : 'default'">{{ stageState(stage.state) }}</MfTag><strong class="mt-2 block text-xs text-content">{{ stageName(stage.name) }}</strong><span class="mt-1 block text-xs text-content-tertiary">{{ elapsed(stage.elapsedMs) }}</span></div></div><MfEmpty v-else size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.engineMetricsHint')"/></section>
      <section class="flex min-h-64 flex-col rounded-mf-lg border border-line bg-surface p-5"><h2 class="text-sm font-semibold text-content">{{ $t('workflow.studio.stageTiming') }}</h2><EChart v-if="stages.length" class="mt-3 min-h-48 flex-1" :option="stageChart"/><MfEmpty v-else class="flex-1" size="sm" :title="$t('workflow.studio.noMetrics')"/></section>
    </div>
</template>
