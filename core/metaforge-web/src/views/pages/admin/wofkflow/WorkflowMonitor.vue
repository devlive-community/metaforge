<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ExternalLink, Play, RefreshCw, Square } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import WorkflowService from '@/services/workflow'
import { workflowStateVariant, type WorkflowModel, type WorkflowRun } from '@/model/workflow'
import WorkflowShell from './components/WorkflowShell.vue'
import WorkflowLog from './components/WorkflowLog.vue'
import EChart from '@/views/components/visual/EChart.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
const route = useRoute(), router = useRouter(), { t } = useI18n()
const record = ref<WorkflowModel>(), runs = ref<WorkflowRun[]>([]), loading = ref(true), refreshing = ref(false), failure = ref(''), busy = ref(false)
const logs = ref<string[]>([]), logFailure = ref(''), logsLoading = ref(false)
const code = computed(() => String(route.params.code || ''))
const detail = ref<WorkflowRun>()
const selected = computed(() => { const requested = typeof route.query.run === 'string' ? route.query.run : undefined; if (requested) return detail.value?.id === requested ? detail.value : undefined; const summary = runs.value[0]; return detail.value?.id === summary?.id ? detail.value : summary })
const outputHeaders = computed(() => [...new Set((selected.value?.preview || []).flatMap(row => Object.keys(row)))])
const elapsed = (value?: number) => value == null ? '—' : value < 1000 ? `${value}ms` : `${(value / 1000).toFixed(1)}s`
const date = (value?: number) => value ? new Date(value).toLocaleString() : '—'
const metrics = computed(() => selected.value?.metrics)
const stateText = (value?: string) => t('dataTable.state' + (value || 'CREATED'))
const stages = computed(() => metrics.value?.stages || [])
const stageChart = computed(() => ({ grid: { left: 40, right: 20, top: 24, bottom: 36 }, tooltip: { trigger: 'axis' as const }, xAxis: { type: 'category' as const, data: stages.value.map(stage => stage.name), axisLabel: { fontSize: 10 } }, yAxis: { type: 'value' as const, name: 'ms', splitLine: { lineStyle: { color: '#e5eeeb' } } }, series: [{ type: 'bar' as const, data: stages.value.map(stage => stage.elapsedMs), barMaxWidth: 36, itemStyle: { color: '#059669', borderRadius: [4, 4, 0, 0] as [number, number, number, number] } }] }))
const columns = computed(() => [
  { title: t('workflow.studio.runId'), dataIndex: 'id', key: 'id' }, { title: t('common.state'), dataIndex: 'state', key: 'state' },
  { title: t('workflow.studio.startedAt'), dataIndex: 'startedAt', key: 'startedAt' }, { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' },
  { title: t('workflow.studio.written'), dataIndex: 'count', key: 'count' },
])
let generation = 0, logGeneration = 0, timer: ReturnType<typeof setTimeout> | undefined, alive = true
async function refresh(initial = false) {
  const request = ++generation, requestedCode = code.value
  if (initial) loading.value = true
  refreshing.value = true; failure.value = ''; clearTimeout(timer)
  try {
    const [workflow, records] = await Promise.all([WorkflowService.getByCode(requestedCode), WorkflowService.runs(requestedCode)])
    if (!alive || request !== generation) return
    if (!workflow.status || !records.status) throw Error(String(!workflow.status ? workflow.message : records.message))
    record.value = workflow.data; runs.value = records.data || []
    const id = typeof route.query.run === 'string' ? route.query.run : runs.value[0]?.id
    if (id) {
      const result = await WorkflowService.getRun(requestedCode, id)
      if (!alive || request !== generation) return
      if (!result.status) throw Error(String(result.message))
      detail.value = result.data
    }
  } catch (error) { if (alive && request === generation) failure.value = error instanceof Error ? error.message : t('workflow.studio.loadFailed') }
  finally {
    if (alive && request === generation) {
      loading.value = false; refreshing.value = false
      if (record.value?.state === 'RUNNING') timer = setTimeout(() => { if (!document.hidden) refresh(); else scheduleHidden() }, 3000)
    }
  }
}
function scheduleHidden() { timer = setTimeout(() => { if (!document.hidden) refresh(); else scheduleHidden() }, 3000) }
async function loadLogs() {
  const request = ++logGeneration, run = selected.value, requestedCode = code.value
  if (!run) { logs.value = []; logsLoading.value = false; return }
  logsLoading.value = true; logFailure.value = ''
  try { const response = await WorkflowService.runLog(requestedCode, run.id); if (!alive || request !== logGeneration) return; if (!response.status) throw Error(String(response.message)); logs.value = response.data || [] }
  catch (error) { if (alive && request === logGeneration) logFailure.value = error instanceof Error ? error.message : t('workflow.studio.loadFailed') }
  finally { if (alive && request === logGeneration) logsLoading.value = false }
}
watch(() => [selected.value?.id, selected.value?.state, selected.value?.metrics?.elapsedMs], loadLogs)
watch(() => route.query.run, () => { detail.value = undefined; refresh() })
watch(code, () => { record.value = undefined; runs.value = []; logs.value = []; refresh(true) }, { immediate: true })
async function execute() {
  if (busy.value) return; busy.value = true
  try { const response = await WorkflowService.restart(code.value); if (!response.status) throw Error(String(response.message)); await router.replace({ path: route.path, query: { run: response.data } }); await refresh() }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.runFailed')) }
  finally { busy.value = false }
}
async function stop() {
  if (busy.value) return; busy.value = true
  try { const response = await WorkflowService.stop(code.value); if (!response.status) throw Error(String(response.message)); await refresh() }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.stopFailed')) }
  finally { busy.value = false }
}
onBeforeUnmount(() => { alive = false; generation++; logGeneration++; clearTimeout(timer) })
</script>
<template>
  <WorkflowShell :code="code">
    <div v-if="loading" class="flex flex-1 items-center justify-center"><MfSpinner :label="$t('common.loading')"/></div>
    <div v-else class="min-h-0 flex-1 overflow-y-auto p-4 md:p-6">
      <header class="mb-5 flex flex-wrap items-center justify-between gap-3"><div class="min-w-0"><router-link to="/workflow" class="mb-2 flex items-center gap-1 text-xs text-content-tertiary hover:text-primary"><ArrowLeft :size="12"/>{{ $t('workflow.studio.list') }}</router-link><h1 class="flex items-center gap-3 text-xl font-semibold text-content">{{ record?.name }} · {{ $t('workflow.studio.runDetails') }}<MfTag v-if="selected" :variant="workflowStateVariant(selected.state)">{{ stateText(selected.state) }}</MfTag></h1><p v-if="selected" class="mt-2 text-xs text-content-tertiary">{{ selected.id }} · {{ selected.executor }}</p></div><div class="flex items-center gap-2"><router-link :to="`/workflow/info/${code}`"><MfButton size="sm"><ExternalLink :size="14"/>{{ $t('workflow.studio.viewWorkflow') }}</MfButton></router-link><MfButton size="sm" :loading="refreshing" @click="refresh()"><RefreshCw :size="14"/>{{ $t('common.refresh') }}</MfButton><MfButton v-if="record?.state === 'RUNNING'" variant="danger" size="sm" :loading="busy" @click="stop"><Square :size="14"/>{{ $t('workflow.studio.stop') }}</MfButton><MfButton v-else variant="primary" size="sm" :loading="busy" :disabled="!record" @click="execute"><Play :size="14"/>{{ $t('workflow.studio.runAgain') }}</MfButton></div></header>
      <MfAlert v-if="failure" class="mb-4" variant="danger" :title="failure"/>
      <template v-if="selected">
        <div class="mb-5 grid grid-cols-2 gap-3 xl:grid-cols-5"><div v-for="item in [{ label: 'executionState', value: stateText(selected.state) }, { label: 'duration', value: elapsed(selected.finishedAt ? selected.elapsed : metrics?.elapsedMs ?? selected.elapsed) }, { label: 'read', value: metrics?.rowsRead ?? '—' }, { label: 'written', value: metrics?.rowsWritten ?? selected.count ?? '—' }, { label: 'failedRows', value: metrics?.rowsFailed ?? '—' }]" :key="item.label" class="rounded-mf-lg border border-line bg-surface px-5 py-4"><p class="text-xs text-content-tertiary">{{ $t('workflow.studio.' + item.label) }}</p><strong class="mt-2 block text-xl text-content">{{ item.value }}</strong></div></div>
        <MfAlert v-if="selected.message" class="mb-5" :variant="selected.state === 'FAILURE' ? 'danger' : 'info'" :title="selected.message"/>
        <section class="mb-5 rounded-mf-lg border border-line bg-surface p-5"><div class="mb-5 flex items-center justify-between"><h2 class="text-sm font-semibold text-content">{{ $t('workflow.studio.executionStages') }}</h2><span class="text-xs text-content-tertiary">{{ date(selected.startedAt) }} → {{ date(selected.finishedAt) }}</span></div><div v-if="stages.length" class="flex flex-wrap gap-4"><div v-for="stage in stages" :key="stage.name" class="min-w-32 flex-1 rounded-mf-md border border-line bg-surface-hover p-3"><MfTag :variant="stage.state === 'DONE' ? 'success' : stage.state === 'FAILED' ? 'danger' : 'default'">{{ stage.state }}</MfTag><strong class="mt-2 block text-xs text-content">{{ stage.name }}</strong><span class="mt-1 block text-xs text-content-tertiary">{{ elapsed(stage.elapsedMs) }}</span></div></div><MfEmpty v-else size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.engineMetricsHint')"/></section>
        <div class="mb-5 grid gap-5 xl:grid-cols-[minmax(0,1.7fr)_minmax(320px,1fr)]"><div class="h-[360px] overflow-hidden rounded-mf-lg border border-line bg-surface"><WorkflowLog :lines="logs" :error="logFailure" :loading="logsLoading" @refresh="loadLogs"/></div><section class="flex h-[360px] flex-col rounded-mf-lg border border-line bg-surface p-5"><h2 class="text-sm font-semibold text-content">{{ $t('workflow.studio.stageTiming') }}</h2><EChart v-if="stages.length" class="mt-4 min-h-0 flex-1" :option="stageChart"/><MfEmpty v-else class="flex-1" size="sm" :title="$t('workflow.studio.noMetrics')"/></section></div>
        <section class="mb-5 rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-4 text-sm font-semibold text-content">{{ $t('workflow.studio.executionMetadata') }}</h2><div class="grid grid-cols-1 gap-x-8 md:grid-cols-2"><div v-for="item in [{ label: 'executor', value: selected.executor }, { label: 'runId', value: selected.id }, { label: 'startedAt', value: date(selected.startedAt) }, { label: 'finishedAt', value: date(selected.finishedAt) }, { label: 'skipped', value: metrics?.rowsSkipped ?? '—' }, { label: 'writeRate', value: metrics ? metrics.avgWriteRowsPerSecond.toFixed(1) + ' rows/s' : '—' }]" :key="item.label" class="flex items-center justify-between gap-4 border-b border-line py-3 text-xs"><span class="text-content-tertiary">{{ $t('workflow.studio.' + item.label) }}</span><span class="truncate text-content-secondary">{{ item.value }}</span></div></div></section>
      </template>
      <MfEmpty v-else-if="!failure" class="my-12" :title="$t('workflow.studio.noRuns')" :description="$t('workflow.studio.noRunsHint')"/>
      <section v-if="selected?.preview" class="mb-5 flex h-80 flex-col rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-2 text-sm font-semibold text-content">{{ $t('workflow.studio.output') }}</h2><p class="mb-3 text-xs text-content-tertiary">{{ $t('workflow.studio.outputHint') }}</p><ResultTable class="min-h-0 flex-1" fill :headers="outputHeaders" :rows="selected.preview"/></section>
      <section v-if="runs.length" class="rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-4 text-sm font-semibold text-content">{{ $t('workflow.studio.runs') }} <span class="text-xs font-normal text-content-tertiary">{{ $t('workflow.studio.recentRuns') }}</span></h2><MfTable :columns="columns" :data-source="runs" row-key="id" density="compact" @row-click="(row: any) => router.replace({ path: route.path, query: { run: row.id } })"><template #cell-id="{ record: row }"><button type="button" class="font-mono text-xs text-primary" @click.stop="router.replace({ path: route.path, query: { run: row.id } })">{{ row.id }}</button></template><template #cell-state="{ record: row }"><MfTag :variant="workflowStateVariant(String(row.state))">{{ stateText(String(row.state)) }}</MfTag></template><template #cell-startedAt="{ record: row }">{{ date(Number(row.startedAt)) }}</template><template #cell-elapsed="{ record: row }">{{ elapsed(Number(row.elapsed)) }}</template></MfTable></section>
    </div>
  </WorkflowShell>
</template>
