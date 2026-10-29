<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Check, Code2, Database, Info, Play, Plus, RefreshCw, Table2 } from '@lucide/vue'
import DatasetService from '@/services/dataset'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'
import PluginService from '@/services/plugin'
import ExecuteService from '@/services/execute'
import type { SourceModel } from '@/model/source'
import { datasetColumnIssue, parseDatasetQueryDraft, reconcileDatasetColumns, type DatasetColumn, type DatasetCreationEntry } from '@/model/dataset-editor'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import QuerySourcePicker from '@/views/pages/admin/query/QuerySourcePicker.vue'
import DatasetColumns from './components/DatasetColumns.vue'
import DatasetWizardShell from './components/DatasetWizardShell.vue'
import DatasetSourcePreview from './components/DatasetSourcePreview.vue'
import DatasetPreviewTable from './components/DatasetPreviewTable.vue'

const props = withDefaults(defineProps<{ embedded?: boolean, datasetCode?: string, entry?: DatasetCreationEntry }>(), { embedded: false })
const emit = defineEmits<{ close: [], submitted: [result: { code: string, name: string }] }>()
const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const step = ref(0)
const code = computed(() => props.datasetCode || (!props.embedded && typeof route.params.code === 'string' ? route.params.code : ''))
const loading = ref(true)
const saving = ref(false)
const validatingColumns = ref(false)
const running = ref(false)
const metadataLoading = ref(false)
const error = ref('')
const loadError = ref('')
const sourceInfo = ref<SourceModel | null>(null)
const pickerValue = ref('')
const sourceMode = ref<'table' | 'sql'>(props.entry?.sourceCode || props.datasetCode || (!props.embedded && (route.params.code || route.params.sourceCode)) ? 'sql' : 'table')
const database = ref('')
const table = ref('')
const databases = ref<string[]>([])
const tables = ref<string[]>([])
const inherited = ref(false)
const previewRows = ref<Record<string, unknown>[]>([])
const previewHeaders = ref<string[]>([])
const previewCount = ref(0)
const elapsed = ref<number | null>(null)
const sqlBackup = ref('')
const verifiedSignature = ref('')
const executors = ref<{ name: string }[]>([])
const schedulers = ref<{ name: string }[]>([])
const pluginsLoading = ref(false)
const form = reactive({
  id: null as number | null, code: '', name: '', description: '', query: '', source: { code: '' },
  syncMode: 'MANUAL', expression: '', scheduler: '', executor: '',
  lifeCycle: undefined as number | undefined, lifeCycleColumn: '', lifeCycleType: '',
  columns: [] as DatasetColumn[]
})
const steps = computed(() => ['basic', 'source', 'columns', 'preview'].map(key => t(`dataset.editor.${key}`)))
const originLabel = computed(() => t(code.value ? 'dataset.common.modify' : inherited.value ? (props.entry?.origin === 'sql' ? 'dataset.prototype.fromSql' : 'dataset.editor.fromQuery') : 'dataset.prototype.manualCreation'))
const syncOptions = computed(() => [{value:'MANUAL',label:t('dataset.common.syncModeManual')},{value:'TIMING',label:t('dataset.common.syncModeTiming')},{value:'OUT_SYNC',label:t('dataset.common.syncModeOutSync')}])
const sourceModes = computed(() => [{value:'table' as const,label:t('dataset.editor.selectTable'),hint:t('dataset.prototype.visualSelection')},{value:'sql' as const,label:t('dataset.editor.customSql'),hint:t('dataset.prototype.writeSql')}])
const summary = computed(() => [
  {label:t('common.source'),value:sourceInfo.value?.name || '—',truncate:true},
  {label:t('common.database'),value:database.value || sourceInfo.value?.database || '—'},
  {label:t('dataset.prototype.sourceMethod'),value:t(sourceMode.value === 'table' ? 'dataset.editor.selectTable' : 'dataset.editor.customSql')},
  {label:t('dataset.editor.columns'),value:String(form.columns.length)},
  {label:t('common.executor'),value:form.executor},
  {label:t('dataset.common.syncMode'),value:syncOptions.value.find(item=>item.value===form.syncMode)?.label || form.syncMode}
])
const signature = computed(() => JSON.stringify([form.source.code, form.query.trim()]))
const previewCurrent = computed(() => verifiedSignature.value === signature.value && !!verifiedSignature.value)
const dateColumns = computed(() => form.columns.filter(column => column.type === 'DATETIME' && !column.virtualColumn))
const pluginOptions = (plugins: { name: string }[], selected: string) => {
  const options: { label: string, value: string, disabled?: boolean }[] = plugins.map(item => ({ label: item.name, value: item.name }))
  if (selected && !plugins.some(item => item.name === selected)) options.push({ label: `${selected} (${t('dataset.editor.pluginUnavailable')})`, value: selected, disabled: true })
  return options
}
const executorOptions = computed(() => pluginOptions(executors.value, form.executor))
const schedulerOptions = computed(() => pluginOptions(schedulers.value, form.scheduler))
const busy = computed(() => loading.value || saving.value || running.value || metadataLoading.value || pluginsLoading.value || validatingColumns.value)
let active = true
let selectionRequest = 0
let sourceRequest = 0
let tableRequest = 0
let previewRequest = 0
const baseline = ref('')
const discardOpen = ref(false)
let pendingLeave: (() => void) | null = null
let saved = false
const snapshot = () => JSON.stringify(form)
const columnsExternalIssue = ref('')
const dirty = computed(() => !!baseline.value && snapshot() !== baseline.value)

function message(cause: unknown): string { return cause instanceof Error ? cause.message : t('dataset.editor.requestFailed') }
function assertResponse(response: any) {
  if (!response.status || response.data?.isSuccessful === false) throw new Error(response.data?.message || response.message || t('dataset.editor.requestFailed'))
  return response.data
}
function clearPreview() {
  previewRequest++
  running.value = false
  verifiedSignature.value = ''
  previewRows.value = []
  previewHeaders.value = []
}
watch(() => [form.source.code, form.query], clearPreview)

async function loadDatabases(sourceCode: string) {
  const request = ++sourceRequest
  metadataLoading.value = true
  databases.value = []
  tables.value = []
  try {
    const data = assertResponse(await MetadataService.getDatabaseBySource(sourceCode))
    if (!active || request !== sourceRequest || form.source.code !== sourceCode) return
    databases.value = (data.columns || []).map((row: any) => row.object_name || row.schema_name || row.SCHEMA_NAME).filter(Boolean)
  }
  catch (cause) { if (active && request === sourceRequest) error.value = message(cause) }
  finally { if (active && request === sourceRequest) metadataLoading.value = false }
}
async function changeSource(value: string) {
  const sourceCode = value.split(':').slice(2).join(':')
  if (sourceCode === form.source.code) return
  const selection = ++selectionRequest
  sourceRequest++
  tableRequest++
  metadataLoading.value = true
  database.value = ''
  table.value = ''
  form.source.code = sourceCode
  sourceInfo.value = null
  inherited.value = false
  error.value = ''
  try {
    const data = assertResponse(await SourceService.getByCode(sourceCode))
    if (!active || selection !== selectionRequest) return
    sourceInfo.value = data
    if (sourceMode.value === 'table') form.query = ''
    if (sourceMode.value === 'table') await loadDatabases(sourceCode)
  }
  catch (cause) { if (active && selection === selectionRequest) error.value = message(cause) }
  finally { if (active && selection === selectionRequest) metadataLoading.value = false }
}
async function changeDatabase() {
  const request = ++tableRequest
  const sourceCode = form.source.code
  const selected = database.value
  table.value = ''
  tables.value = []
  if (sourceMode.value === 'table') form.query = ''
  if (!selected || !sourceCode) return
  metadataLoading.value = true
  error.value = ''
  try {
    const data = assertResponse(await MetadataService.getTablesByDatabase(sourceCode, selected))
    if (!active || request !== tableRequest || form.source.code !== sourceCode) return
    tables.value = (data.columns || []).map((row: any) => row.object_name).filter(Boolean)
  }
  catch (cause) { if (active && request === tableRequest) error.value = message(cause) }
  finally { if (active && request === tableRequest) metadataLoading.value = false }
}
async function selectTable() {
  const request = ++tableRequest
  const sourceCode = form.source.code
  const selectedDatabase = database.value
  const selectedTable = table.value
  form.query = ''
  if (!selectedDatabase || !selectedTable) return
  metadataLoading.value = true
  error.value = ''
  try {
    const data = assertResponse(await MetadataService.queryTable(sourceCode, selectedDatabase, selectedTable, { preview: true }))
    if (!active || request !== tableRequest || form.source.code !== sourceCode) return
    form.query = data.content || ''
  }
  catch (cause) { if (active && request === tableRequest) error.value = message(cause) }
  finally { if (active && request === tableRequest) metadataLoading.value = false }
}
async function setSourceMode(mode: 'table' | 'sql') {
  if (mode === sourceMode.value) return
  error.value = ''
  if (mode === 'table') {
    sqlBackup.value = form.query
    inherited.value = false
    sourceMode.value = mode
    form.query = ''
    if (form.source.code && !databases.value.length) await loadDatabases(form.source.code)
    if (table.value) await selectTable()
  }
  else {
    sourceMode.value = mode
    if (sqlBackup.value) form.query = sqlBackup.value
  }
}
async function runPreview(): Promise<boolean> {
  if (running.value || !sourceInfo.value?.code || !form.source.code || !form.query.trim()) { error.value = t('dataset.editor.sourceRequired'); return false }
  const request = ++previewRequest
  const currentSignature = signature.value
  const sql = form.query
  running.value = true
  error.value = ''
  verifiedSignature.value = ''
  try {
    const data = assertResponse(await ExecuteService.execute({ name: form.source.code, content: sql, mode: 'DATASET' }, null))
    if (!active || request !== previewRequest || currentSignature !== signature.value) return false
    const headers = data?.headers
    if (!Array.isArray(headers) || headers.length === 0 || !headers.every(header => typeof header === 'string')) throw new Error(t('dataset.editor.noColumns'))
    if (new Set(headers).size !== headers.length) throw new Error(t('dataset.editor.ambiguousHeaders'))
    elapsed.value = typeof data.processor?.elapsed === 'number' ? data.processor.elapsed : null
    previewHeaders.value = headers
    previewRows.value = (data.columns || []).slice(0, 100).map((row: Record<string, unknown>) => Object.fromEntries(headers.map((header, index) => [`field_${index}`, row[header]])))
    previewCount.value = data.columns?.length || 0
    form.columns = reconcileDatasetColumns(headers, form.columns)
    verifiedSignature.value = currentSignature
    return true
  }
  catch (cause) { if (active && request === previewRequest) error.value = message(cause); return false }
  finally { if (active && request === previewRequest) running.value = false }
}
const basicFormRef = ref()
const pendingSave = ref(false)

// Step 0 validates through MfForm rules, so errors render inline under
// each input instead of a page-level banner.
const executorRules = [{ validator: (value: unknown) => executors.value.some(item => item.name === value) || t('dataset.editor.executorRequired') }]
const schedulerRules = [{ validator: (value: unknown) => schedulers.value.some(item => item.name === value) || t('dataset.editor.scheduleRequired') }]
const expressionRules = [{ validator: (value: unknown) => String(value ?? '').trim().length > 0 || t('dataset.editor.scheduleRequired') }]

function validateColumns() {
  const issue = datasetColumnIssue(form.columns)
  if (issue) return issue
  if (form.lifeCycleColumn && (!dateColumns.value.some(column => column.name === form.lifeCycleColumn) || !form.lifeCycleType || !form.lifeCycle || form.lifeCycle < 1)) return 'dataset.editor.lifeCycleRequired'
  return null
}
async function next() {
  if (busy.value) return
  error.value = ''
  if (step.value === 0) {
    try { await basicFormRef.value?.validate() }
    catch { return }
    columnsExternalIssue.value = ''
    step.value = 1
    return
  }
  if (step.value === 1 && !previewCurrent.value && !(await runPreview())) return
  if (step.value === 2) {
    const issue = validateColumns()
    if (issue) { columnsExternalIssue.value = t(issue); return }
    if (form.columns.some(column => column.virtualColumn)) {
      validatingColumns.value = true
      try { assertResponse(await DatasetService.validateColumns(form.columns)) }
      catch (cause) { columnsExternalIssue.value = message(cause); return }
      finally { validatingColumns.value = false }
    }
  }
  columnsExternalIssue.value = ''
  step.value = Math.min(3, step.value + 1)
}
async function save() {
  if (busy.value) return
  error.value = ''
  if (step.value === 0) {
    pendingSave.value = true
    try { await basicFormRef.value?.validate() }
    catch { pendingSave.value = false }
    return
  }
  const columnIssue = validateColumns()
  if (columnIssue) { step.value = 2; columnsExternalIssue.value = t(columnIssue); return }
  if (!previewCurrent.value) { step.value = 1; error.value = t('dataset.editor.previewRequired'); return }
  saving.value = true
  try {
    const payload = {
      ...form, id: code.value ? form.id : null, code: code.value || undefined,
      name: form.name.trim(), query: form.query, source: { code: form.source.code },
      lifeCycle: form.lifeCycleColumn ? String(form.lifeCycle) : null,
      lifeCycleColumn: form.lifeCycleColumn || null, lifeCycleType: form.lifeCycleColumn ? form.lifeCycleType : null,
      columns: form.columns.map((column, position) => ({ ...column, name: column.name.trim(), aliasName: column.aliasName.trim(), position }))
    }
    const receipt = assertResponse(await DatasetService.saveOrUpdate(payload))
    saved = true
    MfMessage.success(t('dataset.editor.submitted'))
    const result = { code: receipt?.code || code.value || '', name: form.name.trim() }
    if (props.embedded) emit('submitted', result)
    else await router.push(result.code ? `/dataset/detail/${encodeURIComponent(result.code)}?submitted=1` : '/dataset')
  }
  catch (cause) { error.value = message(cause) }
  finally { saving.value = false }
}

async function onBasicFinished() {
  error.value = ''
  columnsExternalIssue.value = ''
  step.value = 1
  if (pendingSave.value) {
    pendingSave.value = false
    await save()
  }
}
async function loadPluginOptions(failOnError = false) {
  pluginsLoading.value = true
  try {
    const plugins = assertResponse(await PluginService.getPlugins())
    executors.value = plugins.filter((item: any) => item.type === 'EXECUTOR' && typeof item.name === 'string' && item.name.trim())
    schedulers.value = plugins.filter((item: any) => item.type === 'SCHEDULER' && typeof item.name === 'string' && item.name.trim())
    if (!code.value) {
      if (!form.executor) form.executor = executors.value.find(item => item.name === 'Local')?.name || (executors.value.length === 1 ? executors.value[0].name : '')
      if (!form.scheduler) form.scheduler = schedulers.value.length === 1 ? schedulers.value[0].name : ''
    }
  }
  catch (cause) {
    if (failOnError) throw cause
    error.value = message(cause)
  }
  finally { pluginsLoading.value = false }
}
async function initialize() {
  loading.value = true
  loadError.value = ''
  error.value = ''
  try {
    await loadPluginOptions(true)
    if (code.value) {
      const [info, columns] = await Promise.all([DatasetService.getByCode(code.value), DatasetService.getColumnsByCode(code.value)])
      const data = assertResponse(info)
      const fields = assertResponse(columns)
      Object.assign(form, data, { name: data.name || '', description: data.description || '', query: data.query || '', expression: data.expression || '', executor: data.executor || '', scheduler: data.scheduler || '', source: { code: data.source.code }, lifeCycle: data.lifeCycle ? Number(data.lifeCycle) : undefined, lifeCycleColumn: data.lifeCycleColumn || '', lifeCycleType: data.lifeCycleType || '', columns: fields })
      sourceInfo.value = data.source
    }
    else if (props.entry?.sourceCode || (!props.embedded && typeof route.params.sourceCode === 'string')) {
      const sourceCode = props.entry?.sourceCode || route.params.sourceCode as string
      sourceInfo.value = assertResponse(await SourceService.getByCode(sourceCode))
      form.source.code = sourceCode
      const tempId = typeof route.query.tempId === 'string' ? route.query.tempId : ''
      if (tempId) {
        const key = `DatasetQuery_${tempId}`
        const draft = parseDatasetQueryDraft(sessionStorage.getItem(key), sourceCode)
        sessionStorage.removeItem(key)
        if (draft) {
          form.query = draft.query
          form.columns = reconcileDatasetColumns(draft.headers, [])
          inherited.value = true
        }
        else {
          // Preserve old query-page deep links during rollout.
          form.query = localStorage.getItem(`QueryContent_${tempId}`) || ''
          localStorage.removeItem(`QueryContent_${tempId}`)
        }
      }
    }
    if (props.entry && !code.value) {
      form.query = props.entry.query || ''
      form.columns = reconcileDatasetColumns(props.entry.headers || [], [])
      inherited.value = props.entry.origin !== 'manual'
    }
    if (sourceInfo.value) {
      pickerValue.value = `${sourceInfo.value.id}:${sourceInfo.value.type}:${sourceInfo.value.code}`
      if (sourceMode.value === 'table') await loadDatabases(form.source.code)
    }
    await nextTick()
    if (props.entry?.origin === 'result' && props.entry.headers?.length && props.entry.rows) {
      previewHeaders.value = [...props.entry.headers]
      previewRows.value = props.entry.rows.slice(0, 100).map(row => Object.fromEntries(props.entry!.headers!.map((header,index)=>[`field_${index}`,row[header]])))
      previewCount.value = props.entry.rowCount ?? props.entry.rows.length
      elapsed.value = props.entry.elapsed ?? null
      verifiedSignature.value = signature.value
    }
    baseline.value = snapshot()
  }
  catch (cause) { loadError.value = message(cause) }
  finally { if (active) loading.value = false }
}
function closeNow() {
  if (props.embedded) emit('close')
  else router.push('/dataset')
}
function requestClose() {
  if (saving.value) return
  if (!dirty.value) { closeNow(); return }
  pendingLeave = closeNow
  discardOpen.value = true
}
onBeforeRouteLeave(() => {
  if (saved || !dirty.value) return true
  discardOpen.value = true
  return new Promise<boolean>(resolve => { pendingLeave = () => resolve(true); cancelLeave = () => resolve(false) })
})
let cancelLeave: (() => void) | null = null
function leave() { discardOpen.value = false; saved = true; pendingLeave?.(); pendingLeave = null }
function stay() { discardOpen.value = false; cancelLeave?.(); pendingLeave = null; cancelLeave = null }
function beforeUnload(event: BeforeUnloadEvent) { if (!saved && dirty.value) { event.preventDefault(); event.returnValue = '' } }
onMounted(() => { initialize(); window.addEventListener('beforeunload', beforeUnload) })
onBeforeUnmount(() => { active = false; selectionRequest++; sourceRequest++; tableRequest++; previewRequest++; window.removeEventListener('beforeunload', beforeUnload) })
</script>


<template>
  <DatasetWizardShell :error="error" :title="t(code ? 'dataset.common.modify' : 'dataset.common.create')" :step="step" :steps="steps" :busy="busy" :saving="saving" :validating="validatingColumns" :origin="originLabel" @close="requestClose" @previous="step--; error = ''" @next="next" @save="save" @back="step = $event; error = ''">
    <MfSpinner v-if="loading" class="py-16" :label="t('dataset.editor.loading')"/>
    <MfAlert v-else-if="loadError" variant="danger" :title="loadError"><MfButton @click="initialize">{{ t('common.refresh') }}</MfButton></MfAlert>
    <template v-else>
      <MfAlert v-if="error" role="alert" variant="danger" :title="error" class="mb-5"/>
      <div v-if="step === 0" class="grid items-stretch gap-6 lg:grid-cols-[minmax(0,1.85fr)_minmax(260px,1fr)]">
        <MfForm id="dataset-basic-form" ref="basicFormRef" :model="form" @finish="onBasicFinished">
          <h2 class="text-base font-semibold">{{ t('dataset.prototype.fillBasic') }}</h2>
          <MfFormItem name="name" :label="t('common.name')" :rules="[{required: true, message: t('dataset.validator.specifiedName')}]"><MfInput v-model="form.name" :placeholder="t('dataset.editor.namePlaceholder')"/></MfFormItem><p class="-mt-2 text-xs text-content-tertiary">{{ t('dataset.prototype.nameHelp') }}</p>
          <MfFormItem name="description" :label="t('common.description')"><MfTextarea v-model="form.description" :rows="3"/></MfFormItem>
          <div class="grid gap-4 sm:grid-cols-2">
            <MfFormItem name="executor" :label="t('common.executor')" :rules="executorRules"><MfSelect v-model="form.executor" :options="executorOptions" :placeholder="t('dataset.editor.chooseExecutor')" :empty-text="t('dataset.editor.noExecutors')" :loading="pluginsLoading"/><div v-if="!executors.length" class="mt-2 space-y-2"><p class="text-xs text-warning">{{ t('dataset.editor.noExecutorsHint') }}</p><MfButton size="sm" :loading="pluginsLoading" @click="loadPluginOptions()">{{ t('common.refresh') }}</MfButton></div></MfFormItem>
            <MfFormItem name="syncMode" :label="t('dataset.common.syncMode')"><MfSelect v-model="form.syncMode" :options="syncOptions"/></MfFormItem>
            <template v-if="form.syncMode === 'TIMING'">
              <MfFormItem name="scheduler" :label="t('common.scheduler')" :rules="schedulerRules"><MfSelect v-model="form.scheduler" :options="schedulerOptions" :placeholder="t('dataset.editor.chooseScheduler')" :empty-text="t('dataset.editor.noSchedulers')"/></MfFormItem>
              <MfFormItem name="expression" :label="t('dataset.common.columnExpression')" :rules="expressionRules"><MfInput v-model="form.expression" placeholder="0 0 * * * ?"/></MfFormItem>
            </template>
          </div>
          <div class="flex items-start gap-2 rounded-mf-md border border-primary/15 bg-primary/5 px-4 py-3 text-xs leading-6 text-primary"><Info :size="16" class="mt-1 shrink-0"/><span>{{ t('dataset.prototype.sourceSafe') }}</span></div>
        </MfForm>
        <aside class="rounded-mf-lg border border-line bg-surface-hover/40 p-5">
          <h3 class="mb-3 text-sm font-semibold">{{ t('dataset.prototype.creationMode') }}</h3>
          <div class="flex gap-3 rounded-mf-md border border-primary/30 bg-primary/5 p-4"><span class="flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><component :is="inherited ? Table2 : Plus" :size="20"/></span><div><p class="text-sm font-semibold">{{ originLabel }}</p><p class="mt-1 text-xs leading-5 text-content-secondary">{{ t(inherited ? 'dataset.prototype.queryModeHint' : 'dataset.prototype.manualModeHint') }}</p></div></div>
          <h3 class="mt-5 border-t border-line pt-4 text-xs font-semibold text-content-secondary">{{ t('dataset.prototype.creationFlow') }}</h3>
          <ol class="mt-4 space-y-5"><li v-for="(label, index) in steps" :key="label" class="flex gap-3"><span class="flex size-6 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-xs font-semibold text-primary">{{ index + 1 }}</span><div><p class="text-sm font-semibold">{{ label }}</p><p class="mt-1 text-xs text-content-tertiary">{{ t(`dataset.prototype.flow${index + 1}`) }}</p></div></li></ol>
        </aside>
      </div>
      <div v-else-if="step === 1" class="grid items-start gap-6 lg:grid-cols-[minmax(0,1.15fr)_minmax(0,1fr)]">
        <div class="min-w-0 space-y-4">
          <h2 class="text-base font-semibold">{{ t(inherited ? 'dataset.prototype.inheritQuery' : 'dataset.prototype.chooseSource') }}</h2>
          <div v-if="inherited" class="rounded-mf-md border border-primary/15 bg-primary/5 px-4 py-3 text-xs leading-6 text-primary">{{ t('dataset.editor.inheritedHint') }}</div>
          <div v-else class="grid grid-cols-2 gap-3" role="group" :aria-label="steps[1]">
            <button v-for="mode in sourceModes" :key="mode.value" type="button" class="flex items-center gap-3 rounded-mf-md border p-3 text-left" :class="sourceMode === mode.value ? 'border-primary/40 bg-primary/5 text-primary' : 'border-line text-content-secondary hover:bg-surface-hover'" :disabled="busy" :aria-pressed="sourceMode === mode.value" @click="setSourceMode(mode.value)"><component :is="mode.value === 'table' ? Database : Code2" :size="18"/><span><span class="block text-sm font-semibold">{{ mode.label }}</span><span class="mt-0.5 block text-xs text-content-tertiary">{{ mode.hint }}</span></span></button>
          </div>
          <div class="grid gap-4" :class="!inherited && sourceMode === 'table' ? 'sm:grid-cols-2' : ''">
            <MfFormItem name="source" :label="t('common.source')"><div class="w-full -mx-3"><QuerySourcePicker :model-value="pickerValue" :selected-source="sourceInfo" @update:model-value="pickerValue = $event" @change="changeSource"/></div></MfFormItem>
            <MfFormItem v-if="!inherited && sourceMode === 'table'" name="sourceType" :label="t('dataset.prototype.databaseType')"><MfInput :model-value="[sourceInfo?.type, sourceInfo?.version].filter(Boolean).join(' ')" disabled/></MfFormItem>
          </div>
          <MfEmpty v-if="!form.source.code" variant="initial" :title="t('source.tip.selectSource')"/>
          <template v-else>
            <div v-if="sourceMode === 'table'" class="grid gap-4 sm:grid-cols-2">
              <MfFormItem name="database" :label="t('common.database')"><MfSelect v-model="database" searchable :options="databases.map(value => ({label:value,value}))" :loading="metadataLoading" :disabled="running" @change="changeDatabase"/></MfFormItem>
              <MfFormItem name="table" :label="t('common.table')"><MfSelect v-model="table" searchable :options="tables.map(value => ({label:value,value}))" :disabled="!database || running" :loading="metadataLoading" @change="selectTable"/></MfFormItem>
            </div>
            <div><p class="mb-2 text-sm font-medium">{{ t(sourceMode === 'table' ? 'dataset.prototype.generatedSql' : 'dataset.prototype.querySql') }}</p><div class="overflow-hidden rounded-mf-md border border-line"><AceEditor :key="sourceMode" :value="form.query" :read-only="sourceMode === 'table' || running" height="210px" @update:value="form.query = $event"/></div></div>
            <div class="flex flex-wrap items-center gap-3"><MfButton :loading="running" :disabled="metadataLoading || !form.query.trim()" @click="runPreview"><Play :size="14"/>{{ t('dataset.prototype.identifyFields') }}</MfButton><span v-if="previewCurrent" class="text-xs text-success">{{ t('dataset.editor.verified', [previewHeaders.length]) }}<span v-if="elapsed !== null"> · {{ elapsed }} ms</span></span></div>
          </template>
        </div>
        <div class="min-w-0 space-y-5 lg:sticky lg:top-0 lg:max-h-[min(636px,calc(100dvh_-_360px))] lg:overflow-y-auto">
          <DatasetSourcePreview :headers="previewHeaders" :rows="previewRows" :current="previewCurrent" :running="running" :count="previewCount"/>
          <section class="space-y-4 rounded-mf-lg border border-line bg-surface-hover/40 p-5"><h3 class="text-sm font-semibold">{{ t('dataset.prototype.sourceSummary') }}</h3><dl class="space-y-3 text-xs"><div class="flex justify-between gap-3"><dt class="shrink-0 text-content-tertiary">{{ t('common.source') }}</dt><dd class="min-w-0 text-right font-medium"><MfTooltip v-if="sourceInfo?.name" :content="sourceInfo.name"><span class="block max-w-40 truncate">{{ sourceInfo.name }}</span></MfTooltip><span v-else>—</span></dd></div><div class="flex justify-between gap-3"><dt class="text-content-tertiary">{{ t('dataset.prototype.sourceMethod') }}</dt><dd>{{ t(sourceMode === 'table' ? 'dataset.editor.selectTable' : 'dataset.editor.customSql') }}</dd></div><div v-if="sourceMode === 'table'" class="flex justify-between gap-3"><dt class="text-content-tertiary">{{ t('dataset.prototype.targetObject') }}</dt><dd class="break-all">{{ [database, table].filter(Boolean).join('.') || '—' }}</dd></div><div class="flex justify-between gap-3"><dt class="text-content-tertiary">{{ t('dataset.prototype.identifiedFields') }}</dt><dd><MfTag v-if="previewCurrent" variant="primary" size="sm">{{ previewHeaders.length }}</MfTag><span v-else>—</span></dd></div></dl></section>
        </div>
      </div>
      <div v-else-if="step === 2" class="space-y-5">
        <DatasetColumns v-model="form.columns" :refreshing="running" :external-issue="columnsExternalIssue" @refresh-columns="runPreview"/>
        <details class="rounded-mf-lg border border-line px-4 py-3"><summary class="cursor-pointer text-sm font-medium">{{ t('dataset.common.dataLifeCycle') }}</summary><div class="mt-4 grid gap-4 sm:grid-cols-3"><MfFormItem name="lifeCycleColumn" :label="t('dataset.common.lifeCycleColumn')"><MfSelect v-model="form.lifeCycleColumn" clearable :options="dateColumns.map(column => ({label:column.name,value:column.name}))" :disabled="!dateColumns.length"/></MfFormItem><MfFormItem name="lifeCycleType" :label="t('dataset.common.lifeCycleType')"><MfSelect v-model="form.lifeCycleType" :disabled="!form.lifeCycleColumn" :options="['MONTH','WEEK','DAY','HOUR'].map(value => ({value,label:t(`dataset.common.lifeCycle${value[0] + value.slice(1).toLowerCase()}`)}))"/></MfFormItem><MfFormItem name="lifeCycle" :label="t('dataset.common.lifeCycleNumber')"><MfInputNumber v-model="form.lifeCycle" :min="1" :disabled="!form.lifeCycleColumn"/></MfFormItem></div></details>
      </div>
      <div v-else class="grid items-start gap-6 lg:grid-cols-[minmax(240px,0.7fr)_minmax(0,1.6fr)]">
        <div class="space-y-5">
          <h2 class="text-base font-semibold">{{ t('dataset.prototype.confirmInfo') }}</h2>
          <section class="rounded-mf-lg border border-line bg-surface-hover/40 p-5"><div class="mb-5 flex items-center gap-3"><span class="flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><Database :size="21"/></span><div class="min-w-0"><h3 class="break-all text-sm font-semibold">{{ form.name }}</h3><p class="mt-1 text-xs text-content-tertiary">{{ originLabel }}</p></div></div><dl class="divide-y divide-line text-xs"><div v-for="item in summary" :key="item.label" class="flex justify-between gap-3 py-3"><dt class="shrink-0 text-content-tertiary">{{ item.label }}</dt><dd class="min-w-0 text-right font-medium"><MfTooltip v-if="item.truncate && item.value && item.value !== '—'" :content="item.value"><span class="block max-w-40 truncate">{{ item.value }}</span></MfTooltip><span v-else class="break-all">{{ item.value }}</span></dd></div></dl></section>
          <div class="rounded-mf-md border border-primary/15 bg-primary/5 p-4 text-xs leading-6 text-primary">{{ t('dataset.editor.asyncHint') }}</div>
        </div>
        <div class="min-w-0 space-y-4">
          <div class="flex flex-wrap items-start justify-between gap-3"><div><h2 class="text-base font-semibold">{{ t('dataset.prototype.previewValidation') }}</h2><p class="mt-1 text-xs leading-5 text-content-tertiary">{{ t('dataset.editor.previewHint') }}</p></div><MfButton size="sm" :loading="running" @click="runPreview"><RefreshCw :size="14"/>{{ t('dataset.prototype.refreshPreview') }}</MfButton></div>
          <div v-if="previewCurrent" class="flex items-center gap-2 rounded-mf-md border border-primary/15 bg-primary/5 px-4 py-3 text-xs text-primary"><Check :size="15"/>{{ t('dataset.editor.verified', [previewHeaders.length]) }} · {{ t('dataset.editor.previewCount', [Math.min(previewCount, 100), previewCount]) }}</div>
          <DatasetPreviewTable :headers="previewHeaders" :rows="previewRows" :loading="running" :empty-text="t('dataset.editor.emptyPreview')"/>
          <section class="rounded-mf-lg border border-line bg-surface-hover/40 p-4"><h3 class="text-sm font-semibold">{{ t('dataset.prototype.sqlDefinition') }}</h3><pre class="mt-3 overflow-x-auto whitespace-pre-wrap break-all font-mono text-xs leading-6 text-content-secondary">{{ form.query }}</pre></section>
        </div>
      </div>
    </template>
  </DatasetWizardShell>
  <MfModal :open="discardOpen" trap-focus @update:open="stay" :title="t('dataset.editor.discardTitle')"><p>{{ t('dataset.editor.discardHint') }}</p><template #footer><MfButton @click="stay">{{ t('common.cancel') }}</MfButton><MfButton variant="danger" @click="leave">{{ t('dataset.editor.discard') }}</MfButton></template></MfModal>
</template>
