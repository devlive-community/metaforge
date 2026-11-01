import { computed, onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import ExportService from '@/services/export'
import MetadataService from '@/services/metadata'
import { sourceManageContext, sourceManageLocation } from '@/model/source-manage'
import { useSourceCapabilities } from './useSourceCapabilities'
import { configured, defaultExportState, EXPORT_STEPS, exportedTables, exportPlan, formatOptions, outputName, setting,
  type ExportColumn, type ExportFilter, type ExportFormat, type ExportObject, type TableSetting } from '@/model/database-export'

export { EXPORT_STEPS }

/**
 * Five-step export of the database the workbench header shows: choose tables, set their content,
 * fields and rows, pick an installed format and its declared settings, set the output, then check
 * the plan on the server and run it as a job whose file is downloaded when it is done.
 */
export function useExportWizard() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const context = computed(() => sourceManageContext(route))
  const { can } = useSourceCapabilities(computed(() => context.value.source))
  // Structure goes into schema.sql only for sources whose plugin can give a table's statement.
  const canStructure = computed(() => can('table:statement'))

  const step = ref(0)
  const state = ref(defaultExportState(''))
  const objects = ref<ExportObject[]>([])
  const objectsLoading = ref(false)
  const columns = ref<Record<string, ExportColumn[]>>({})
  const formats = ref<ExportFormat[]>([])
  const current = ref('')
  const sample = ref<{ file: string; content: string } | null>(null)
  const sampleLoading = ref(false)
  const sampleError = ref('')
  const check = ref<any | null>(null)
  const checkLoading = ref(false)
  const checkError = ref('')
  const rows = ref<{ headers: string[]; rows: any[][] } | null>(null)
  const job = ref<any | null>(null)
  const starting = ref(false)
  let alive = true
  let sampleTimer: ReturnType<typeof setTimeout> | undefined
  let sampleRevision = 0
  let pollTimer: ReturnType<typeof setTimeout> | undefined

  const location = () => [context.value.source, context.value.database] as const
  const tables = computed(() => exportedTables(state.value, objects.value))
  const format = computed(() => formats.value.find(item => item.name === state.value.format))
  const output = computed(() => outputName(state.value, tables.value, format.value?.extension || ''))
  const plan = computed(() => exportPlan(state.value, objects.value, columns.value))
  const running = computed(() => job.value?.status === 'running')
  const canNext = computed(() => {
    if (step.value === 0) return tables.value.length > 0
    if (step.value === 2) return !!format.value
    if (step.value === 3) return !!state.value.fileName.trim() && state.value.batchSize > 0
    return true
  })
  const canStart = computed(() => !!check.value && check.value.errors === 0 && !checkLoading.value && !starting.value && !job.value)

  async function load() {
    const [source, database] = location()
    state.value = defaultExportState(database)
    objects.value = []
    columns.value = {}
    if (!source || !database) return
    objectsLoading.value = true
    const [sizes, list, formatResponse] = await Promise.all([
      MetadataService.getDatabaseTables(source, database), MetadataService.getTablesByDatabase(source, database), ExportService.formats(),
    ])
    if (!alive) return
    objectsLoading.value = false
    const bySize = new Map<string, any>((sizes.status ? sizes.data?.columns || [] : []).map((row: any) => [String(row.object_name), row]))
    objects.value = (list.status ? list.data?.columns || [] : [])
        .filter((row: any) => row.type_name === 'table' || row.type_name === 'view')
        .map((row: any) => {
          const size = bySize.get(String(row.object_name))
          const number = (value: any) => value === null || value === undefined || value === '' ? null : Number(value)
          return { name: String(row.object_name), type: row.type_name, rows: number(size?.object_rows), bytes: number(size?.object_data_length), comment: String(row.object_comment || '') }
        })
    formats.value = formatResponse.status ? formatResponse.data || [] : []
    if (!canStructure.value) state.value.content = 'data'
    chooseFormat(formats.value[0]?.name || '')
  }

  async function columnsOf(table: string): Promise<ExportColumn[]> {
    if (columns.value[table]) return columns.value[table]
    const [source, database] = location()
    const response = await MetadataService.getColumnsByTable(source, database, table)
    const list = response.status ? (response.data?.columns || []).filter((row: any) => row.type_name === 'column')
        .map((row: any) => ({ name: String(row.object_name), type: String(row.object_data_type || '').toUpperCase() })) : []
    columns.value = { ...columns.value, [table]: list }
    return list
  }

  // ---- step 1 ----
  const toggle = (name: string, checked: boolean) => {
    const selected = new Set(state.value.selected)
    if (checked) selected.add(name); else selected.delete(name)
    state.value.selected = objects.value.map(object => object.name).filter(item => selected.has(item))
  }
  const selectAll = (names: string[]) => { state.value.selected = [...new Set([...state.value.selected, ...names])] }
  const clearSelection = () => { state.value.selected = [] }

  // ---- step 2 ----
  async function configure(table: string) {
    current.value = table
    const list = await columnsOf(table)
    if (!state.value.settings[table]) state.value.settings[table] = { fields: list.map(column => column.name), filters: [], scope: 'all' }
  }
  const currentSetting = computed<TableSetting>(() => setting(state.value, current.value))
  const toggleField = (name: string, checked: boolean) => {
    const value = state.value.settings[current.value]
    if (!value) return
    const order = (columns.value[current.value] || []).map(column => column.name)
    const fields = new Set(value.fields)
    if (checked) fields.add(name); else fields.delete(name)
    value.fields = order.filter(item => fields.has(item))
  }
  const addFilter = () => {
    const value = state.value.settings[current.value]
    if (!value) return
    value.scope = 'filtered'
    value.filters.push({ column: columns.value[current.value]?.[0]?.name || '', operator: '=', value: '' })
  }
  const removeFilter = (filter: ExportFilter) => {
    const value = state.value.settings[current.value]
    if (value) value.filters = value.filters.filter(item => item !== filter)
  }
  const isConfigured = (table: string) => configured(state.value.settings[table])

  // ---- step 3 ----
  function chooseFormat(name: string) {
    state.value.format = name
    state.value.options = formatOptions(formats.value.find(item => item.name === name), state.value.options)
  }
  // The format's output for the first table is written by the server as the settings change.
  function scheduleSample() {
    clearTimeout(sampleTimer)
    sampleTimer = setTimeout(runSample, 300)
  }
  async function runSample() {
    const table = tables.value[0]
    const [source, database] = location()
    if (!table || !state.value.format) { sample.value = null; return }
    const revision = ++sampleRevision
    sampleLoading.value = true
    const response = await ExportService.sample(source, database, plan.value, table, 3)
    if (revision !== sampleRevision || !alive) return
    sampleLoading.value = false
    sampleError.value = response.status ? '' : response.message || t('common.requestError')
    sample.value = response.status ? { file: String(response.data?.file || ''), content: String(response.data?.content || '') } : null
  }
  watch(() => [step.value, state.value.format, JSON.stringify(state.value.options), tables.value[0]], () => { if (step.value === 2) scheduleSample() })

  // ---- step 5 ----
  async function runCheck() {
    const [source, database] = location()
    checkLoading.value = true
    checkError.value = ''
    const [checked, sampled] = await Promise.all([
      ExportService.check(source, database, plan.value),
      tables.value[0] ? ExportService.sample(source, database, { ...plan.value, format: '' }, tables.value[0], 3) : Promise.resolve(null),
    ])
    if (!alive) return
    checkLoading.value = false
    checkError.value = checked.status ? '' : checked.message || t('common.requestError')
    check.value = checked.status ? checked.data : null
    rows.value = sampled?.status ? { headers: sampled.data?.headers || [], rows: sampled.data?.rows || [] } : null
  }

  async function next() {
    if (!canNext.value || step.value >= EXPORT_STEPS.length - 1) return
    step.value++
    if (step.value === 1 && !current.value && tables.value[0]) await configure(tables.value[0])
    if (step.value === 4) runCheck()
  }
  const previous = () => { if (step.value > 0 && !running.value && !job.value) step.value-- }
  const goTo = (index: number) => { if (index < step.value && !running.value && !job.value) step.value = index }
  const back = () => router.push(sourceManageLocation({ source: context.value.source, database: context.value.database }))

  async function start() {
    if (!canStart.value) return
    const [source, database] = location()
    starting.value = true
    const response = await ExportService.execute(source, database, plan.value)
    starting.value = false
    if (!response.status) { MfMessage.error(response.message || t('common.requestError')); return }
    job.value = { status: 'running', tables: [] }
    poll(String(response.data?.jobId))
  }
  async function poll(id: string) {
    const response = await ExportService.job(id)
    if (!alive) return
    if (response.status) job.value = { ...response.data, id }
    if (job.value?.status === 'running') { pollTimer = setTimeout(() => poll(id), 1000); return }
    if (job.value?.status === 'done' || job.value?.status === 'partial') {
      MfMessage.success(t('export.done'))
      if (state.value.autoDownload) download()
    }
    else MfMessage.error(job.value?.message || t('export.failed'))
  }
  async function download() {
    if (!job.value?.id) return
    const response = await ExportService.file(job.value.id)
    if (!response.status || !response.blob) { MfMessage.error(response.message || t('common.requestError')); return }
    const url = URL.createObjectURL(response.blob)
    const link = document.createElement('a')
    link.href = url
    link.download = response.fileName || output.value
    link.click()
    URL.revokeObjectURL(url)
  }

  watch(() => [context.value.source, context.value.database], () => { step.value = 0; job.value = null; check.value = null; current.value = ''; sample.value = null; load() }, { immediate: true })
  onScopeDispose(() => { alive = false; sampleRevision++; clearTimeout(sampleTimer); clearTimeout(pollTimer) })

  return {
    context, can, canStructure, step, state, objects, objectsLoading, columns, formats, format, tables, output, current, currentSetting, sample, sampleLoading,
    sampleError, check, checkLoading, checkError, rows, job, starting, running, canNext, canStart,
    toggle, selectAll, clearSelection, configure, toggleField, addFilter, removeFilter, isConfigured, chooseFormat, runSample, runCheck,
    next, previous, goTo, back, start, download,
  }
}

export type ExportWizard = ReturnType<typeof useExportWizard>
