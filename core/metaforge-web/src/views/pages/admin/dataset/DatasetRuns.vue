<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Database, History, LayoutDashboard, Pause, Play, RefreshCw, RotateCcw, Square } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import DatasetService from '@/services/dataset'
import WorkflowService from '@/services/workflow'
import { FilterModel } from '@/model/filter'
import { workflowStateVariant, type WorkflowExecutor } from '@/model/workflow'
import { duration, metricNumber, metricText, type WorkflowMetricsSnapshot } from '@/model/workflow-metrics'
import RunShell from '@/views/components/run/RunShell.vue'
import RunLog from '@/views/components/run/RunLog.vue'
import RunMetrics from '@/views/components/run/RunMetrics.vue'
import RunStages from '@/views/components/run/RunStages.vue'
import { useUtil } from '@/utils/common'

/**
 * Sync records of a dataset, laid out like workflow run records: the dataset's executor reports
 * the same metrics, stages and log, so the same run components show them. The data comes from the
 * existing dataset history, metrics, log and stop endpoints.
 */
interface SyncRecord {
  id: number; state: string; mode?: string; elapsed?: number; count?: number; message?: string
  rowsRead?: number; rowsWritten?: number; rowsSkipped?: number; progress?: number; totalCount?: number; attempt?: number; createTime?: string; updateTime?: string
}
interface SyncView {
  state?: string; attempt?: number; live?: boolean; retryable?: boolean; message?: string; metrics?: WorkflowMetricsSnapshot | null
  transitions?: { from?: string; to?: string; time?: string; message?: string }[]
}
const route = useRoute(), router = useRouter(), { t } = useI18n()
const code = computed(() => String(route.params.code || ''))
const dataset = ref<{ name?: string; executor?: string; state?: string[] }>()
const records = ref<SyncRecord[]>([]), view = ref<SyncView>(), logs = ref<string[]>([])
const loading = ref(true), refreshing = ref(false), failure = ref(''), busy = ref(false)
const logsLoading = ref(false), logFailure = ref('')
const executors = ref<WorkflowExecutor[]>([])
WorkflowService.catalog().then(response => { if (response.status) executors.value = response.data?.executors || [] })

const TERMINAL = ['SUCCESS', 'FAILURE', 'STOPPED', 'TIMEOUT', 'INTERRUPTED', 'PAUSED']
const selected = computed(() => {
  const requested = Number(route.query.run)
  return records.value.find(record => record.id === requested) || (route.query.run ? undefined : records.value[0])
})
const state = computed(() => view.value?.state || selected.value?.state)
const active = computed(() => !!state.value && !TERMINAL.includes(state.value))
const metrics = computed(() => view.value?.metrics || undefined)
const canPause = computed(() => state.value === 'RUNNING' && metrics.value?.resumable === true)
const paused = computed(() => state.value === 'PAUSED')
const declared = computed(() => executors.value.find(item => item.name === dataset.value?.executor)?.metrics || [])
const stateText = (value?: string) => t('dataTable.state' + (value || 'CREATED'))
// The dataset's own build state is the last of its recorded steps, labelled as on the detail page.
const { getDatasetStateText } = useUtil()
const datasetState = computed(() => Array.isArray(dataset.value?.state) ? dataset.value!.state.at(-1) || '' : '')
const datasetVariant = computed(() => datasetState.value === 'COMPLETE_SUCCESS' ? 'primary' : datasetState.value.endsWith('FAILED') ? 'danger' : 'default')
const elapsedText = (ms?: number) => ms == null || !Number.isFinite(ms) ? '—' : duration(ms)
const durationMs = computed(() => metricNumber(metrics.value, 'elapsedMs', (selected.value?.elapsed ?? NaN) * 1000))
const finishTime = (record?: SyncRecord) => record && TERMINAL.includes(record.state) ? record.updateTime || '—' : '—'

const tabs = computed(() => ['overview', ...(declared.value.length ? ['metrics'] : []), 'logs', 'history'])
const tab = computed(() => { const value = String(route.query.tab || 'overview'); return tabs.value.includes(value) ? value : 'overview' })
function selectTab(value: string) { router.replace({ path: route.path, query: { ...route.query, tab: value === 'overview' ? undefined : value } }) }
function openRun(id: number) { router.replace({ path: route.path, query: { run: String(id) } }) }
const tabLabel = (key: string) => key === 'history' ? t('dataset.runs.history') : t('workflow.studio.runTab.' + key)
const menu = computed(() => [
  { key: 'detail', label: t('dataset.runs.detail'), href: `/dataset/detail/${code.value}`, icon: LayoutDashboard },
  { key: 'runs', label: t('dataset.runs.title'), href: `/dataset/runs/${code.value}`, icon: History },
])
const overview = computed(() => [
  { label: t('workflow.studio.executionState'), value: stateText(state.value) },
  { label: t('workflow.studio.duration'), value: elapsedText(durationMs.value) },
  { label: t('workflow.studio.startedAt'), value: selected.value?.createTime || '—' },
  { label: t('workflow.studio.finishedAt'), value: finishTime(selected.value) },
])
const details = computed(() => [
  { label: t('workflow.studio.executor'), value: dataset.value?.executor || '—' },
  { label: t('dataset.runs.id'), value: selected.value ? String(selected.value.id) : '—' },
  { label: t('dataset.runs.mode'), value: selected.value?.mode || '—' },
  { label: t('dataset.runs.attempt'), value: String(view.value?.attempt ?? selected.value?.attempt ?? '—') },
  { label: t('workflow.studio.metric.rowsRead'), value: rows('rowsRead', selected.value?.rowsRead) },
  { label: t('workflow.studio.metric.rowsWritten'), value: rows('rowsWritten', selected.value?.rowsWritten ?? selected.value?.count) },
])
/** A row count from the metrics, else from the history record, else unknown. */
function rows(key: 'rowsRead' | 'rowsWritten', recorded?: number) {
  const value = metricNumber(metrics.value, key, recorded ?? NaN)
  return Number.isFinite(value) ? value.toLocaleString() : '—'
}
const columns = computed(() => [
  { title: t('dataset.runs.id'), dataIndex: 'id', key: 'id' }, { title: t('common.state'), dataIndex: 'state', key: 'state' },
  { title: t('workflow.studio.startedAt'), dataIndex: 'createTime', key: 'createTime' }, { title: t('workflow.studio.finishedAt'), dataIndex: 'updateTime', key: 'finishTime' },
  { title: t('workflow.studio.metric.progress'), dataIndex: 'progress', key: 'progress' },
  { title: t('workflow.studio.metric.rowsRead'), dataIndex: 'rowsRead', key: 'rowsRead' }, { title: t('workflow.studio.written'), dataIndex: 'count', key: 'count' },
  { title: t('workflow.studio.metric.rowsSkipped'), dataIndex: 'rowsSkipped', key: 'rowsSkipped' }, { title: t('workflow.studio.metric.avgWriteRowsPerSecond'), key: 'rate' },
  { title: t('common.elapsed'), dataIndex: 'elapsed', key: 'elapsed' }, { title: t('dataset.runs.attempt'), dataIndex: 'attempt', key: 'attempt' },
])
const recordWritten = (row: any) => row.rowsWritten ?? row.count
const recordProgress = (row: any) => { const value = Number(row.progress); return Number.isFinite(value) && row.progress != null ? Math.min(100, Math.max(0, value)) : undefined }
const recordRate = (row: any) => { const seconds = Number(row.elapsed), written = Number(recordWritten(row)); return seconds > 0 && written > 0 ? metricText('writeRowsPerSecond', { writeRowsPerSecond: written / seconds }) : '—' }

let generation = 0, logGeneration = 0, timer: ReturnType<typeof setTimeout> | undefined, alive = true
async function refresh(initial = false) {
  const request = ++generation, requested = code.value
  if (initial) loading.value = true
  refreshing.value = true; failure.value = ''; clearTimeout(timer)
  try {
    const [info, history] = await Promise.all([DatasetService.getByCode(requested), DatasetService.getHistory(requested, Object.assign(new FilterModel(), { page: 1, size: 100 }))])
    if (!alive || request !== generation) return
    if (!info.status || !history.status) throw Error(String(!info.status ? info.message : history.message))
    dataset.value = info.data; records.value = history.data?.content || []
    if (selected.value) {
      const detail = await DatasetService.getHistoryMetrics(selected.value.id)
      if (!alive || request !== generation) return
      view.value = detail.status ? detail.data : undefined
    }
    else view.value = undefined
  } catch (error) { if (alive && request === generation) failure.value = error instanceof Error ? error.message : t('dataset.runs.loadFailed') }
  finally {
    if (alive && request === generation) {
      loading.value = false; refreshing.value = false
      if (active.value) timer = setTimeout(poll, 3000)
    }
  }
}
function poll() { if (document.hidden) timer = setTimeout(poll, 3000); else { refresh(); loadLogs() } }
async function loadLogs() {
  const request = ++logGeneration, record = selected.value
  if (!record) { logs.value = []; return }
  logsLoading.value = true; logFailure.value = ''
  try {
    const response = await DatasetService.getHistoryLog(record.id)
    if (!alive || request !== logGeneration) return
    if (!response.status) throw Error(String(response.message))
    logs.value = response.data || []
  } catch (error) { if (alive && request === logGeneration) { logs.value = []; logFailure.value = error instanceof Error ? error.message : t('dataset.runs.loadFailed') } }
  finally { if (alive && request === logGeneration) logsLoading.value = false }
}
watch(() => selected.value?.id, () => { view.value = undefined; loadLogs() })
watch(() => route.query.run, () => refresh())
watch(code, () => { dataset.value = undefined; records.value = []; refresh(true) }, { immediate: true })
async function sync() {
  if (busy.value) return
  busy.value = true
  try {
    const response = await DatasetService.syncData(code.value)
    if (!response.status) throw Error(String(response.message))
    MfMessage.success(t('dataset.runs.syncStarted'))
    if (response.data?.id) await router.replace({ path: route.path, query: { run: String(response.data.id) } })
    await refresh()
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dataset.runs.syncFailed')) }
  finally { busy.value = false }
}
async function stop() {
  if (busy.value || !selected.value) return
  busy.value = true
  try {
    const response = await DatasetService.stopHistory(selected.value.id)
    if (!response.status) throw Error(String(response.message))
    await refresh()
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dataset.runs.stopFailed')) }
  finally { busy.value = false }
}
/** Pause, resume and run again share one shape: call, refresh, and show the server's reason on failure. */
async function act(call: (id: number) => Promise<any>, failed: string) {
  if (busy.value || !selected.value) return
  busy.value = true
  try {
    const response = await call(selected.value.id)
    if (!response.status) throw Error(String(response.message))
    await refresh()
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : failed) }
  finally { busy.value = false }
}
const pause = () => act(id => DatasetService.pauseHistory(id), t('dataset.runs.pauseFailed'))
const resume = () => act(id => DatasetService.resumeHistory(id), t('dataset.runs.resumeFailed'))
const retry = () => act(id => DatasetService.retryHistory(id), t('dataset.runs.retryFailed'))
onBeforeUnmount(() => { alive = false; generation++; logGeneration++; clearTimeout(timer) })
</script>
<template>
  <RunShell :name="dataset?.name" :state-label="getDatasetStateText(datasetState)" :state-variant="datasetVariant" :icon="Database" :items="menu" :back="{ to: '/dataset', label: $t('dataset.runs.list') }">
    <template v-if="!loading" #header>
      <header class="flex flex-wrap items-center justify-between gap-3">
        <div class="min-w-0">
          <h1 class="flex items-center gap-3 text-xl leading-7 font-semibold text-content">{{ $t('dataset.runs.title') }}<MfTag v-if="state" :variant="workflowStateVariant(state)">{{ stateText(state) }}</MfTag></h1>
          <p v-if="selected" class="mt-1 truncate text-xs text-content-tertiary">#{{ selected.id }} · {{ dataset?.executor }}</p>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <MfButton size="sm" :loading="refreshing" @click="refresh(); loadLogs()"><RefreshCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
          <MfButton v-if="canPause" size="sm" :loading="busy" @click="pause"><Pause :size="14"/>{{ $t('dataset.runs.pause') }}</MfButton>
          <MfButton v-if="paused" variant="primary" size="sm" :loading="busy" @click="resume"><Play :size="14"/>{{ $t('dataset.runs.resume') }}</MfButton>
          <MfButton v-if="paused" variant="danger" size="sm" :loading="busy" @click="stop"><Square :size="14"/>{{ $t('dataset.runs.cancel') }}</MfButton>
          <MfButton v-if="active" variant="danger" size="sm" :loading="busy" @click="stop"><Square :size="14"/>{{ $t('workflow.studio.stop') }}</MfButton>
          <MfButton v-if="view?.retryable" size="sm" :loading="busy" @click="retry"><RotateCcw :size="14"/>{{ $t('dataset.runs.retry') }}</MfButton>
          <MfButton v-if="!active && !paused" variant="primary" size="sm" :loading="busy" :disabled="!dataset" @click="sync"><Play :size="14"/>{{ $t('dataset.runs.syncNow') }}</MfButton>
        </div>
      </header>
      <MfTabs class="mt-3" :active-key="tab" @update:active-key="selectTab(String($event))">
        <MfTabPane v-for="item in tabs" :key="item" :item-key="item" :tab="tabLabel(item)"/>
      </MfTabs>
    </template>
    <div v-if="loading" class="flex h-full min-h-96 items-center justify-center"><MfSpinner :label="$t('common.loading')"/></div>
    <div v-else class="min-w-0">
      <MfAlert v-if="failure" class="mb-4" variant="danger" :title="failure"/>
      <section v-if="tab === 'history'" class="rounded-mf-lg border border-line bg-surface p-5">
        <MfTable :columns="columns" :data-source="records" row-key="id" density="compact" :row-class="(row: any) => row.id === selected?.id ? 'bg-primary-light/40' : ''" :empty-text="$t('dataset.runs.noRuns')" @row-click="(row: any) => openRun(row.id)">
          <template #cell-id="{ record: row }"><button type="button" class="font-mono text-xs text-primary" @click.stop="openRun(row.id)">#{{ row.id }}</button></template>
          <template #cell-state="{ record: row }"><MfTag :variant="workflowStateVariant(String(row.state))">{{ stateText(String(row.state)) }}</MfTag></template>
          <template #cell-createTime="{ record: row }"><span class="tabular-nums">{{ row.createTime || '—' }}</span></template>
          <template #cell-finishTime="{ record: row }"><span class="tabular-nums">{{ finishTime(row) }}</span></template>
          <template #cell-elapsed="{ record: row }">{{ elapsedText(row.elapsed == null ? undefined : Number(row.elapsed) * 1000) }}</template>
          <template #cell-progress="{ record: row }">
            <div class="flex w-32 items-center gap-2">
              <span class="h-1.5 min-w-0 flex-1 overflow-hidden rounded-mf-full bg-surface-active"><span class="block h-full rounded-mf-full bg-primary" :style="{ width: (recordProgress(row) ?? 0) + '%' }"/></span>
              <span class="shrink-0 text-xs tabular-nums text-content-secondary">{{ recordProgress(row) === undefined ? '—' : Math.round(recordProgress(row) as number) + '%' }}</span>
            </div>
          </template>
          <template #cell-rowsRead="{ record: row }"><span class="tabular-nums">{{ row.rowsRead?.toLocaleString?.() ?? '—' }}</span></template>
          <template #cell-count="{ record: row }"><span class="tabular-nums">{{ recordWritten(row)?.toLocaleString?.() ?? '—' }}</span></template>
          <template #cell-rowsSkipped="{ record: row }"><span class="tabular-nums">{{ row.rowsSkipped?.toLocaleString?.() ?? '—' }}</span></template>
          <template #cell-rate="{ record: row }"><span class="tabular-nums">{{ recordRate(row) }}</span></template>
          <template #cell-attempt="{ record: row }"><span class="tabular-nums">{{ row.attempt ?? 1 }}</span></template>
        </MfTable>
      </section>
      <MfEmpty v-else-if="!selected" class="my-12" :title="$t('dataset.runs.noRuns')" :description="$t('dataset.runs.noRunsHint')"/>
      <template v-else-if="tab === 'overview'">
        <div class="mb-4 grid grid-cols-2 gap-3 xl:grid-cols-4">
          <div v-for="item in overview" :key="item.label" class="min-w-0 rounded-mf-lg border border-line bg-surface px-5 py-4"><p class="text-xs text-content-tertiary">{{ item.label }}</p><strong class="mt-2 block truncate text-lg text-content tabular-nums">{{ item.value }}</strong></div>
        </div>
        <MfAlert v-if="view?.message || selected.message" class="mb-4" :variant="state === 'FAILURE' ? 'danger' : 'info'" :title="view?.message || selected.message"/>
        <RunStages v-if="declared.includes('stages')" class="mb-4" :metrics="metrics"/>
        <div class="grid gap-4 xl:grid-cols-2">
          <section class="rounded-mf-lg border border-line bg-surface p-5">
            <h2 class="mb-3 text-sm font-semibold text-content">{{ $t('dataset.runs.transitions') }}</h2>
            <ol v-if="view?.transitions?.length" class="space-y-3">
              <li v-for="(item, index) in view.transitions" :key="index" class="flex gap-3 text-xs">
                <span class="mt-1 size-2 shrink-0 rounded-full" :class="item.to === 'FAILURE' ? 'bg-danger' : item.to === 'SUCCESS' ? 'bg-success' : 'bg-primary'"/>
                <div class="min-w-0"><p class="text-content"><span class="tabular-nums text-content-tertiary">{{ item.time || '—' }}</span> · {{ item.from ? stateText(item.from) + ' → ' : '' }}{{ stateText(item.to) }}</p><p v-if="item.message" class="mt-0.5 break-all text-content-tertiary">{{ item.message }}</p></div>
              </li>
            </ol>
            <MfEmpty v-else size="sm" :title="$t('dataset.runs.noTransitions')"/>
          </section>
          <section class="rounded-mf-lg border border-line bg-surface p-5"><h2 class="mb-2 text-sm font-semibold text-content">{{ $t('workflow.studio.executionMetadata') }}</h2><div class="grid grid-cols-1 gap-x-8 md:grid-cols-2"><div v-for="item in details" :key="item.label" class="flex items-center justify-between gap-4 border-b border-line py-3 text-xs"><span class="text-content-tertiary">{{ item.label }}</span><span class="truncate text-content-secondary tabular-nums">{{ item.value }}</span></div></div></section>
        </div>
      </template>
      <RunMetrics v-else-if="tab === 'metrics'" :metrics="metrics" :declared="declared"/>
      <div v-else-if="tab === 'logs'" class="h-[calc(100dvh-260px)] min-h-96 overflow-hidden rounded-mf-lg border border-line bg-surface"><RunLog :lines="logs" :error="logFailure" :loading="logsLoading" filename="dataset-sync.log" @refresh="loadLogs"/></div>
    </div>
  </RunShell>
</template>
