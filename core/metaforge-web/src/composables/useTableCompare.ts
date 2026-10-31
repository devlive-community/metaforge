import { computed, onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { compareLocation, sourceManageContext, sourceManageLocation, type CompareMode } from '@/model/source-manage'
import { dataBody, emptyCounts, emptyDataForm, structureSides, type CompareTables, type DataForm, type DataResult, type DataRow, type StructureCounts,
  type StructureRow } from '@/model/table-compare'

type Option = { label: string; value: string }

/**
 * Compare page of the source and database the workbench header shows: the source table comes
 * from the current database, the target from any database of the same source. Structure compares
 * as soon as both tables are chosen; data compares on request with the chosen key and range.
 */
export function useTableCompare() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()

  const context = computed(() => sourceManageContext(route))
  const source = computed(() => context.value.source)
  const database = computed(() => context.value.database)
  const mode = computed<CompareMode>(() => route.query.mode === 'data' ? 'data' : 'structure')
  const sourceTable = computed(() => context.value.table)

  const databases = ref<Option[]>([])
  const sourceTables = ref<Option[]>([])
  const targetTables = ref<Option[]>([])
  const targetDatabase = ref('')
  const targetTable = ref('')

  const structureLoading = ref(false)
  const structureRows = ref<StructureRow[]>([])
  const structureCounts = ref<StructureCounts>(emptyCounts())
  const structureError = ref('')
  const onlyDifferent = ref(false)

  const dataForm = ref<DataForm>(emptyDataForm())
  const dataLoading = ref(false)
  const dataResult = ref<DataResult | null>(null)
  const dataError = ref('')
  const columnNames = ref<string[]>([])
  const primaryKeys = ref<string[]>([])
  const detail = ref<DataRow | null>(null)
  const exporting = ref(false)
  let structureRevision = 0
  let dataRevision = 0
  let alive = true

  const tables = computed<CompareTables>(() => ({ sourceDatabase: database.value, sourceTable: sourceTable.value, targetDatabase: targetDatabase.value, targetTable: targetTable.value }))
  const ready = computed(() => !!(source.value && database.value && sourceTable.value && targetDatabase.value && targetTable.value))
  const sides = computed(() => structureSides(structureRows.value, onlyDifferent.value))
  const tableNames = (response: any) => (response.data?.columns || []).filter((item: any) => item.type_name === 'table').map((item: any) => ({ label: item.object_name, value: item.object_name }))

  async function loadDatabases() {
    databases.value = []
    if (!source.value) return
    const response = await MetadataService.getDatabaseBySource(source.value)
    if (alive && response.status) {
      databases.value = (response.data?.columns || []).map((item: any) => item.object_name || item.schema_name).filter(Boolean).map((name: string) => ({ label: name, value: name }))
    }
  }
  async function loadSourceTables() {
    sourceTables.value = []
    if (!source.value || !database.value) return
    const response = await MetadataService.getTablesByDatabase(source.value, database.value)
    if (alive && response.status) sourceTables.value = tableNames(response)
  }
  /** Target tables of the chosen database; the source table's namesake is picked when there is one. */
  async function loadTargetTables(keep = '') {
    targetTables.value = []
    targetTable.value = ''
    if (!source.value || !targetDatabase.value) return
    const requested = targetDatabase.value
    const response = await MetadataService.getTablesByDatabase(source.value, requested)
    if (!alive || requested !== targetDatabase.value || !response.status) return
    targetTables.value = tableNames(response)
    const wanted = keep || sourceTable.value
    if (targetTables.value.some(item => item.value === wanted)) targetTable.value = wanted
  }
  async function loadColumns() {
    columnNames.value = []
    primaryKeys.value = []
    if (!source.value || !database.value || !sourceTable.value) return
    const response = await MetadataService.getColumnsByTable(source.value, database.value, sourceTable.value)
    if (!alive || !response.status) return
    const rows = response.data?.columns || []
    columnNames.value = rows.filter((row: any) => row.type_name === 'column').map((row: any) => String(row.object_name))
    primaryKeys.value = rows.filter((row: any) => row.type_name === 'primary').map((row: any) => String(row.object_name))
  }

  async function compareStructure() {
    const request = ++structureRevision
    structureError.value = ''
    if (!ready.value) { structureRows.value = []; structureCounts.value = emptyCounts(); return }
    structureLoading.value = true
    const response = await MetadataService.compareStructure(source.value, tables.value)
    if (request !== structureRevision || !alive) return
    structureLoading.value = false
    if (!response.status) { structureRows.value = []; structureCounts.value = emptyCounts(); structureError.value = response.message || t('common.loadingFailed'); return }
    structureRows.value = response.data?.rows || []
    structureCounts.value = { ...emptyCounts(), ...(response.data?.counts || {}) }
  }
  async function compareData() {
    if (!ready.value || dataLoading.value) return
    const request = ++dataRevision
    dataError.value = ''
    dataLoading.value = true
    const response = await MetadataService.compareData(source.value, dataBody(tables.value, dataForm.value))
    if (request !== dataRevision || !alive) return
    dataLoading.value = false
    if (!response.status) { dataResult.value = null; dataError.value = response.message || t('common.loadingFailed'); return }
    dataResult.value = response.data as DataResult
  }
  function resetData() {
    dataForm.value = emptyDataForm()
    dataResult.value = null
    dataError.value = ''
  }

  const setMode = (value: string | number) => router.replace({ ...compareLocation({ source: source.value, database: database.value, table: sourceTable.value, mode: value === 'data' ? 'data' : 'structure' }) })
  const setSourceTable = (value: string) => router.replace(compareLocation({ source: source.value, database: database.value, table: value, mode: mode.value }))
  /** The target becomes the source (moving the header to its database) and the source the target. */
  let swapping: { database: string; table: string } | null = null
  function swap() {
    if (!ready.value) return
    swapping = { database: database.value, table: sourceTable.value }
    router.replace(compareLocation({ source: source.value, database: targetDatabase.value, table: targetTable.value, mode: mode.value }))
  }
  function back() {
    router.push(sourceManageLocation({ source: source.value, database: database.value, table: sourceTable.value || undefined, tab: sourceTable.value ? 'structure' : undefined }))
  }

  async function exportResult(kind: 'structure' | 'data', format: 'sql' | 'csv') {
    if (!ready.value || exporting.value) return
    exporting.value = true
    try {
      const body = { ...(kind === 'data' ? dataBody(tables.value, dataForm.value) : tables.value), kind, format }
      const response = await MetadataService.compareExport(source.value, body)
      if (!response.status || !response.blob) { MfMessage.error(response.message || t('common.loadingFailed')); return }
      const url = URL.createObjectURL(response.blob)
      const link = document.createElement('a')
      link.href = url
      link.download = response.fileName || `${ sourceTable.value }-${ kind }.${ format }`
      link.click()
      URL.revokeObjectURL(url)
      MfMessage.success(t('source.export.downloaded', { file: link.download }))
    } finally {
      exporting.value = false
    }
  }

  /** The user picks another target database. */
  function setTargetDatabase(value: string) {
    targetDatabase.value = value
    loadTargetTables()
  }

  watch(source, loadDatabases, { immediate: true })
  // A new database (or a swap) reloads both sides; the target starts in the same database.
  watch(database, value => {
    loadSourceTables()
    const next = swapping || { database: value, table: '' }
    swapping = null
    targetDatabase.value = next.database
    loadTargetTables(next.table)
  }, { immediate: true })
  watch(sourceTable, (value, previous) => {
    loadColumns()
    dataResult.value = null
    dataForm.value.keys = []
    // Follow the source with its namesake unless the user chose another target.
    if (previous !== undefined && (!targetTable.value || targetTable.value === previous)) {
      targetTable.value = targetTables.value.some(item => item.value === value) ? value : ''
    }
  }, { immediate: true })
  watch([tables, mode], () => { if (mode.value === 'structure') compareStructure() }, { deep: true, immediate: true })
  watch(() => [targetDatabase.value, targetTable.value], () => { dataResult.value = null })
  onScopeDispose(() => { alive = false; structureRevision++; dataRevision++ })

  return {
    source, database, mode, sourceTable, databases, sourceTables, targetTables, targetDatabase, targetTable, ready,
    structureLoading, structureRows, structureCounts, structureError, onlyDifferent, sides,
    dataForm, dataLoading, dataResult, dataError, columnNames, primaryKeys, detail, exporting,
    setMode, setSourceTable, setTargetDatabase, swap, back, compareStructure, compareData, resetData, exportResult,
  }
}
