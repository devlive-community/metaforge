<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Play, RefreshCw, Square } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import WorkflowService from '@/services/workflow'
import { workflowStateVariant, type WorkflowExecutor, type WorkflowModel, type WorkflowRun } from '@/model/workflow'
import WorkflowShell from './components/WorkflowShell.vue'
import WorkflowLog from './components/WorkflowLog.vue'
import WorkflowMetrics from './components/WorkflowMetrics.vue'
import EChart from '@/views/components/visual/EChart.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
const route = useRoute(), router = useRouter(), { t, te } = useI18n()
const record = ref<WorkflowModel>(), runs = ref<WorkflowRun[]>([]), loading = ref(true), refreshing = ref(false), failure = ref(''), busy = ref(false)
const logs = ref<string[]>([]), logFailure = ref(''), logsLoading = ref(false)
const code = computed(() => String(route.params.code || ''))
const detail = ref<WorkflowRun>()
const selected = computed(() => { const requested = typeof route.query.run === 'string' ? route.query.run : undefined; if (requested) return detail.value?.id === requested ? detail.value : undefined; const summary = runs.value[0]; return detail.value?.id === summary?.id ? detail.value : summary })
const outputHeaders = computed(() => [...new Set((selected.value?.preview || []).flatMap(row => Object.keys(row)))])
const elapsed = (value?: number) => value == null ? '—' : value < 1000 ? `${value}ms` : `${(value / 1000).toFixed(1)}s`
const metrics = computed(() => selected.value?.metrics)
/** Metrics the run's executor declares; pages show only those. */
const executors = ref<WorkflowExecutor[]>([])
const executor = computed(() => executors.value.find(item => item.name === (selected.value?.executor || record.value?.executor)))
const declared = computed(() => executor.value?.metrics || [])
const capabilities = computed(() => executor.value?.capabilities)
/** Overview, metrics (when the executor reports any), log, output and history, remembered in the address. */
const tabs = computed(() => ['overview', ...(declared.value.length ? ['metrics'] : []), 'logs', 'output', 'history'])
const tab = computed(() => { const value = String(route.query.tab || 'overview'); return tabs.value.includes(value) ? value : 'overview' })
function selectTab(value: string) { router.replace({ path: route.path, query: { ...route.query, tab: value === 'overview' ? undefined : value } }) }
function openRun(id: string) { router.replace({ path: route.path, query: { run: id } }) }
const overview = computed(() => selected.value ? [
  { label: 'executionState', value: stateText(selected.value.state) },
  { label: 'duration', value: elapsed(selected.value.finishedAt ? selected.value.elapsed : metrics.value?.elapsedMs ?? selected.value.elapsed) },
  { label: 'startedAt', value: selected.value.startTime || '—' },
  { label: 'finishedAt', value: selected.value.finishTime || '—' },
] : [])
WorkflowService.catalog().then(response => { if (response.status) executors.value = response.data?.executors || [] })
const stateText = (value?: string) => t('dataTable.state' + (value || 'CREATED'))
const stages = computed(() => metrics.value?.stages || [])
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
const columns = computed(() => [
  { title: t('workflow.studio.runId'), dataIndex: 'id', key: 'id' }, { title: t('common.state'), dataIndex: 'state', key: 'state' },
  { title: t('workflow.studio.startedAt'), dataIndex: 'startTime', key: 'startedAt' }, { title: t('workflow.studio.finishedAt'), dataIndex: 'finishTime', key: 'finishedAt' },
  { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' }, { title: t('workflow.studio.written'), dataIndex: 'count', key: 'count' },
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
  <WorkflowShell :code="code" :name="record?.name" :state="record?.state">
    <template v-if="!loading" #header>
      <header class="flex flex-wrap items-center justify-between gap-3">
        <div class="min-w-0">
          <h1 class="flex items-center gap-3 text-xl leading-7 font-semibold text-content">{{ $t('workflow.studio.runs') }}<MfTag v-if="selected" :variant="workflowStateVariant(selected.state)">{{ stateText(selected.state) }}</MfTag></h1>
          <p v-if="selected" class="mt-1 truncate text-xs text-content-tertiary">{{ selected.id }} · {{ selected.executor }}</p>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <MfButton size="sm" :loading="refreshing" @click="refresh()"><RefreshCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
          <MfButton v-if="record?.state === 'RUNNING'" variant="danger" size="sm" :loading="busy" @click="stop"><Square :size="14"/>{{ $t('workflow.studio.stop') }}</MfButton>
          <MfButton v-else variant="primary" size="sm" :loading="busy" :disabled="!record" @click="execute"><Play :size="14"/>{{ $t('workflow.studio.runAgain') }}</MfButton>
        </div>
      </header>
      <MfTabs class="mt-3" :active-key="tab" @update:active-key="selectTab(String($event))">
        <MfTabPane v-for="item in tabs" :key="item" :item-key="item" :tab="$t('workflow.studio.runTab.' + item)"/>
      </MfTabs>
    </template>
    <div v-if="loading" class="flex h-full min-h-96 items-center justify-center"><MfSpinner :label="$t('common.loading')"/></div>
    <div v-else class="min-w-0">
      <MfAlert v-if="failure" class="mb-4" variant="danger" :title="failure"/>
      <!-- History lists every run; the other tabs describe the selected run. -->
      <section v-if="tab === 'history'" class="rounded-mf-lg border border-line bg-surface p-5">
        <p class="mb-3 text-xs text-content-tertiary">{{ $t('workflow.studio.recentRuns') }}</p>
        <MfTable :columns="columns" :data-source="runs" row-key="id" density="compact" :row-class="(row: any) => row.id === selected?.id ? 'bg-primary-light/40' : ''" :empty-text="$t('workflow.studio.noRuns')" @row-click="(row: any) => openRun(row.id)">
          <template #cell-id="{ record: row }"><button type="button" class="font-mono text-xs text-primary" @click.stop="openRun(row.id)">{{ row.id }}</button></template>
          <template #cell-state="{ record: row }"><MfTag :variant="workflowStateVariant(String(row.state))">{{ stateText(String(row.state)) }}</MfTag></template>
          <template #cell-startedAt="{ record: row }"><span class="tabular-nums">{{ row.startTime || '—' }}</span></template>
          <template #cell-finishedAt="{ record: row }"><span class="tabular-nums">{{ row.finishTime || '—' }}</span></template>
          <template #cell-count="{ record: row }"><span class="tabular-nums">{{ row.count ?? '—' }}</span></template>
          <template #cell-elapsed="{ record: row }">{{ elapsed(Number(row.elapsed)) }}</template>
        </MfTable>
      </section>
      <MfEmpty v-else-if="!selected" class="my-12" :title="$t('workflow.studio.noRuns')" :description="$t('workflow.studio.noRunsHint')"/>
      <template v-else-if="tab === 'overview'">
        <div class="mb-4 grid grid-cols-2 gap-3 xl:grid-cols-4">
          <div v-for="item in overview" :key="item.label" class="min-w-0 rounded-mf-lg border border-line bg-surface px-5 py-4"><p class="text-xs text-content-tertiary">{{ $t('workflow.studio.' + item.label) }}</p><strong class="mt-2 block truncate text-lg text-content tabular-nums">{{ item.value }}</strong></div>
        </div>
        <MfAlert v-if="selected.message" class="mb-4" :variant="selected.state === 'FAILURE' ? 'danger' : 'info'" :title="selected.message"/>
        <div v-if="declared.includes('stages')" class="mb-4 grid gap-4 xl:grid-cols-[minmax(0,1.4fr)_minmax(320px,1fr)]">
          <section class="rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-4 text-sm font-semibold text-content">{{ $t('workflow.studio.executionStages') }}</h2><div v-if="stages.length" class="grid grid-cols-[repeat(auto-fill,minmax(140px,1fr))] gap-3"><div v-for="stage in stages" :key="stage.name" class="rounded-mf-md border border-line bg-surface-hover p-3"><MfTag :variant="stage.state === 'DONE' ? 'success' : stage.state === 'FAILED' ? 'danger' : stage.state === 'RUNNING' ? 'primary' : 'default'">{{ stageState(stage.state) }}</MfTag><strong class="mt-2 block text-xs text-content">{{ stageName(stage.name) }}</strong><span class="mt-1 block text-xs text-content-tertiary">{{ elapsed(stage.elapsedMs) }}</span></div></div><MfEmpty v-else size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t('workflow.studio.engineMetricsHint')"/></section>
          <section class="flex min-h-64 flex-col rounded-mf-lg border border-line bg-surface p-5"><h2 class="text-sm font-semibold text-content">{{ $t('workflow.studio.stageTiming') }}</h2><EChart v-if="stages.length" class="mt-3 min-h-48 flex-1" :option="stageChart"/><MfEmpty v-else class="flex-1" size="sm" :title="$t('workflow.studio.noMetrics')"/></section>
        </div>
        <section class="rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-2 text-sm font-semibold text-content">{{ $t('workflow.studio.executionMetadata') }}</h2><div class="grid grid-cols-1 gap-x-8 md:grid-cols-2"><div v-for="item in [{ label: 'executor', value: selected.executor }, { label: 'runId', value: selected.id }, { label: 'startedAt', value: selected.startTime || '—' }, { label: 'finishedAt', value: selected.finishTime || '—' }]" :key="item.label" class="flex items-center justify-between gap-4 border-b border-line py-3 text-xs"><span class="text-content-tertiary">{{ $t('workflow.studio.' + item.label) }}</span><span class="truncate text-content-secondary tabular-nums">{{ item.value }}</span></div></div></section>
      </template>
      <WorkflowMetrics v-else-if="tab === 'metrics'" :metrics="metrics" :declared="declared"/>
      <div v-else-if="tab === 'logs'" class="h-[calc(100dvh-260px)] min-h-96 overflow-hidden rounded-mf-lg border border-line bg-surface"><WorkflowLog :lines="logs" :error="logFailure" :loading="logsLoading" @refresh="loadLogs"/></div>
      <section v-else-if="tab === 'output'" class="flex h-[calc(100dvh-260px)] min-h-96 flex-col rounded-mf-lg border border-line bg-surface p-5">
        <template v-if="selected.preview?.length"><p class="mb-3 text-xs text-content-tertiary">{{ $t('workflow.studio.outputHint') }}</p><ResultTable class="min-h-0 flex-1" fill :headers="outputHeaders" :rows="selected.preview"/></template>
        <div v-else class="flex flex-1 items-center justify-center"><MfEmpty size="sm" :title="$t('workflow.studio.noOutput')" :description="$t(capabilities && !capabilities.preview ? 'workflow.studio.outputUnsupported' : selected.state === 'RUNNING' ? 'workflow.studio.outputPending' : 'workflow.studio.outputUnavailable')"/></div>
      </section>
    </div>
  </WorkflowShell>
</template>
