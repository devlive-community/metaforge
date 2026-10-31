import { computed, onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import ImportService from '@/services/import'
import MetadataService from '@/services/metadata'
import SourceService from '@/services/source'
import { sourceManageContext, sourceManageLocation } from '@/model/source-manage'
import { defaultImportSettings, importPlan, mapFields, mappingCounts, matchTables, type ImportAnalysis, type ImportSettings, type TablePlan,
  type TargetColumn } from '@/model/table-import'

export const IMPORT_STEPS = ['file', 'tables', 'mapping', 'settings', 'preview'] as const

/**
 * Five-step import into the database the workbench header shows: analyse a file on the server,
 * choose a target per table, map fields, set the write strategy, then check the server's preview
 * and run the import as a job whose progress is polled.
 */
export function useImportWizard() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const context = computed(() => sourceManageContext(route))

  const step = ref(0)
  const parsers = ref<{ name: string; extension: string }[]>([])
  const file = ref<File | null>(null)
  const analysis = ref<ImportAnalysis | null>(null)
  const analyzing = ref(false)
  const analyzeError = ref('')
  const sourceType = ref('')
  const targetTables = ref<string[]>([])
  const targetColumns = ref<Record<string, TargetColumn[]>>({})
  const plans = ref<TablePlan[]>([])
  const current = ref(0)
  const settings = ref<ImportSettings>(defaultImportSettings())
  const preview = ref<any | null>(null)
  const previewLoading = ref(false)
  const previewError = ref('')
  const confirmed = ref(false)
  const job = ref<any | null>(null)
  const starting = ref(false)
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  let previewRevision = 0
  let pollTimer: ReturnType<typeof setTimeout> | undefined
  let alive = true

  const location = () => [context.value.source, context.value.database] as const
  const sourceTable = (name: string) => analysis.value?.tables.find(table => table.name === name) || null
  const selectedPlans = computed(() => plans.value.filter(plan => plan.selected))
  const currentPlan = computed(() => selectedPlans.value[current.value] || null)
  const totals = computed(() => ({
    tables: analysis.value?.tables.length || 0,
    rows: (analysis.value?.tables || []).reduce((sum, table) => sum + table.rows, 0),
    definitions: (analysis.value?.tables || []).filter(table => table.hasDefinition).length,
    anomalies: analysis.value?.anomalies.length || 0,
  }))
  /** Fields the server flagged in the preview, as "source → target". */
  const flagged = computed(() => {
    const plan = currentPlan.value
    const row = (preview.value?.tables || []).find((item: any) => item.source === plan?.source)
    return new Set<string>((row?.messages || []).filter((message: any) => message.code === 'typeMismatch').map((message: any) => message.subject))
  })
  const currentCounts = computed(() => currentPlan.value ? mappingCounts(currentPlan.value, flagged.value) : { mapped: 0, total: 0, attention: 0 })
  const running = computed(() => job.value?.status === 'running')
  const canNext = computed(() => {
    if (step.value === 0) return !!analysis.value && !analyzing.value
    if (step.value === 1) return selectedPlans.value.length > 0 && selectedPlans.value.every(plan => plan.target.trim())
    return true
  })
  const canStart = computed(() => !!preview.value && !previewLoading.value && preview.value.errors === 0 && confirmed.value && !starting.value && !job.value)

  async function load() {
    const [source, database] = location()
    targetTables.value = []
    if (!source) return
    const [parserResponse, sourceResponse] = await Promise.all([ImportService.parsers(), SourceService.getByCode(source)])
    if (!alive) return
    parsers.value = parserResponse.status ? parserResponse.data || [] : []
    sourceType.value = sourceResponse.status ? String(sourceResponse.data?.type || '') : ''
    if (database) {
      const response = await MetadataService.getTablesByDatabase(source, database)
      if (alive && response.status) targetTables.value = (response.data?.columns || []).filter((item: any) => item.type_name === 'table').map((item: any) => String(item.object_name))
    }
  }

  async function columnsOf(table: string): Promise<TargetColumn[]> {
    if (targetColumns.value[table]) return targetColumns.value[table]
    const [source, database] = location()
    const response = await MetadataService.getColumnsByTable(source, database, table)
    const columns = response.status ? (response.data?.columns || []).filter((row: any) => row.type_name === 'column')
        .map((row: any) => ({ name: String(row.object_name), type: String(row.object_data_type || '') })) : []
    targetColumns.value = { ...targetColumns.value, [table]: columns }
    return columns
  }
  /** Maps a plan's fields onto its target: the existing table's columns, or the source's own for a new table. */
  async function remap(plan: TablePlan) {
    const source = sourceTable(plan.source)
    if (!source) return
    const targets = plan.action === 'create' ? source.columns.map(column => ({ name: column.name, type: column.type })) : await columnsOf(plan.target)
    plan.mappings = mapFields(source, targets)
  }
  const targetOptions = (plan: TablePlan) => plan.action === 'create' ? source(plan) : targetColumns.value[plan.target] || []
  const source = (plan: TablePlan) => (sourceTable(plan.source)?.columns || []).map(column => ({ name: column.name, type: column.type }))

  async function analyze(chosen: File, parser = '') {
    const [code, database] = location()
    if (!code || !database) { MfMessage.warning(t('import.chooseDatabase')); return }
    file.value = chosen
    analyzing.value = true
    analyzeError.value = ''
    analysis.value = null
    preview.value = null
    const response = await ImportService.analyze(code, database, chosen, parser)
    if (!alive) return
    analyzing.value = false
    if (!response.status) { analyzeError.value = response.message || t('common.loadingFailed'); return }
    analysis.value = response.data as ImportAnalysis
    await autoMatch()
  }
  const reparse = (parser: string) => { if (file.value) analyze(file.value, parser) }
  function removeFile() {
    file.value = null
    analysis.value = null
    analyzeError.value = ''
    plans.value = []
    preview.value = null
  }

  async function autoMatch() {
    if (!analysis.value) return
    const selected = new Map(plans.value.map(plan => [plan.source, plan.selected]))
    const matched = matchTables(analysis.value.tables, targetTables.value)
    matched.forEach(plan => { plan.selected = selected.get(plan.source) ?? true })
    await Promise.all(matched.map(remap))
    plans.value = matched
    current.value = 0
  }
  async function resetSelection() {
    plans.value.forEach(plan => { plan.selected = true })
    await autoMatch()
  }
  async function setAction(plan: TablePlan, action: TablePlan['action']) {
    plan.action = action
    if (action === 'create') plan.target = targetTables.value.includes(plan.source) ? `${ plan.source }_import` : plan.source
    else if (action === 'existing' && !targetTables.value.includes(plan.target)) plan.target = ''
    if (action !== 'skip' && plan.target) await remap(plan)
    plan.selected = action !== 'skip'
  }
  async function setTarget(plan: TablePlan, target: string) {
    plan.target = target
    if (plan.action === 'existing' && target) await remap(plan)
  }

  // The server checks the plan as it changes from the mapping step on.
  function schedulePreview() {
    clearTimeout(previewTimer)
    previewTimer = setTimeout(runPreview, 300)
  }
  async function runPreview() {
    if (!analysis.value) return
    const request = ++previewRevision
    const [code, database] = location()
    previewLoading.value = true
    const response = await ImportService.preview(code, database, importPlan(analysis.value.importId, plans.value, settings.value))
    if (request !== previewRevision || !alive) return
    previewLoading.value = false
    previewError.value = response.status ? '' : response.message || t('common.loadingFailed')
    preview.value = response.status ? response.data : null
  }
  watch([plans, settings], () => { if (step.value >= 2 && !job.value) schedulePreview() }, { deep: true })

  function next() {
    if (!canNext.value || step.value >= IMPORT_STEPS.length - 1) return
    step.value++
    if (step.value === 2) current.value = 0
    if (step.value >= 2) runPreview()
  }
  const previous = () => { if (step.value > 0 && !running.value) step.value-- }
  const goTo = (index: number) => { if (index < step.value && !running.value) step.value = index }
  const back = () => router.push(sourceManageLocation({ source: context.value.source, database: context.value.database }))

  async function start() {
    if (!canStart.value || !analysis.value) return
    const [code, database] = location()
    starting.value = true
    const response = await ImportService.execute(code, database, importPlan(analysis.value.importId, plans.value, settings.value))
    starting.value = false
    if (!response.status) { MfMessage.error(response.message || t('common.loadingFailed')); return }
    job.value = { status: 'running', tables: [] }
    poll(String(response.data?.jobId))
  }
  async function poll(id: string) {
    const response = await ImportService.job(id)
    if (!alive) return
    if (response.status) job.value = response.data
    if (job.value?.status === 'running') pollTimer = setTimeout(() => poll(id), 1000)
    else if (job.value?.status === 'done') MfMessage.success(t('import.done'))
    else MfMessage.error(t('import.failed'))
  }

  watch(() => [context.value.source, context.value.database], () => { removeFile(); step.value = 0; job.value = null; targetColumns.value = {}; load() }, { immediate: true })
  onScopeDispose(() => { alive = false; previewRevision++; clearTimeout(previewTimer); clearTimeout(pollTimer) })

  return {
    context, step, parsers, file, analysis, analyzing, analyzeError, sourceType, targetTables, plans, selectedPlans, current, currentPlan, currentCounts,
    flagged, totals, settings, preview, previewLoading, previewError, confirmed, job, starting, running, canNext, canStart, targetOptions,
    analyze, reparse, removeFile, autoMatch, resetSelection, setAction, setTarget, remap, next, previous, goTo, back, start,
  }
}

export type ImportWizard = ReturnType<typeof useImportWizard>
