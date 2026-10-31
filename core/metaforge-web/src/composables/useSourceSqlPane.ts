import { computed, inject, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import axios from 'axios'
import ExecuteService from '@/services/execute'
import type { ResponseModel } from '@/model/response'
import type { DatasetCreationEntry } from '@/model/dataset-editor'
import { checkTableScope } from '@/model/table-scoped-sql'
import { useSourceManageRoute } from './useSourceManageRoute'

/**
 * SQL pane of a table's workbench: statements may only use the selected table (checked here for a
 * translated message, enforced by the server), and SELECT results can become datasets.
 */
export function useSourceSqlPane() {
  const { t } = useI18n()
  const route = useSourceManageRoute()
  const router = useRouter()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const content = ref('')
  const response = ref<ResponseModel | null>(null)
  const running = ref(false)
  const executedQuery = ref('')
  // The executed statement with the table qualified by its database, so a dataset runs it anywhere.
  const qualifiedQuery = ref('')
  const scopeError = ref('')
  const editorRoot = ref<HTMLElement | null>(null)
  const database = computed(() => String(route.params.database || ''))
  const table = computed(() => String(route.params.table || ''))
  let revision = 0
  let cancel: ReturnType<typeof axios.CancelToken.source> | undefined
  let observer: ResizeObserver | undefined
  function onEditorCreated(editor: { resize: () => void }) {
    observer?.disconnect()
    if (typeof ResizeObserver !== 'undefined' && editorRoot.value) {
      observer = new ResizeObserver(() => editor.resize())
      observer.observe(editorRoot.value)
    }
  }
  // Each table starts from a query on itself.
  watch(() => [database.value, table.value], () => {
    revision++
    cancel?.cancel()
    running.value = false
    content.value = table.value ? 'SELECT * FROM `' + table.value.replace(/`/g, '``') + '` LIMIT 100' : ''
    response.value = null
    scopeError.value = ''
  }, { immediate: true })
  watch(content, () => { scopeError.value = '' })
  async function run() {
    if (running.value || !content.value.trim() || !can('query:execute') || !table.value) return
    const scope = checkTableScope(content.value, database.value, table.value)
    if ('issue' in scope) {
      scopeError.value = t(`source.sqlScope.${scope.issue}`, { table: table.value, name: scope.detail || '' })
      return
    }
    const request = ++revision
    running.value = true
    response.value = null
    executedQuery.value = content.value
    qualifiedQuery.value = scope.qualified
    cancel = axios.CancelToken.source()
    try {
      const result = await ExecuteService.execute({ name: String(route.params.source), content: content.value, mode: 'ADHOC', database: database.value, table: table.value }, cancel.token)
      if (request !== revision) return
      response.value = result.data?.isSuccessful === false ? { ...result, status: false, message: result.data.message || result.message } : result
    } finally { if (request === revision) running.value = false }
  }

  // Dataset creation from the result or the editor, as on the query page.
  const creationModeVisible = ref(false)
  const datasetWizardVisible = ref(false)
  const existingMappingVisible = ref(false)
  const datasetEntry = ref<DatasetCreationEntry>({ origin: 'manual' })
  const isSelect = (sql: string) => /^\s*select\b/i.test(sql)
  const resultEntry = computed<DatasetCreationEntry | null>(() => {
    const result = response.value
    const headers: string[] = result?.data?.headers || []
    if (running.value || !result?.status || result.data?.isSuccessful === false || !headers.length || new Set(headers).size !== headers.length || !isSelect(qualifiedQuery.value)) return null
    return { origin: 'result', sourceCode: String(route.params.source), query: qualifiedQuery.value, headers: [...headers],
      rows: (result.data.columns || []).slice(0, 100).map((row: Record<string, unknown>) => ({ ...row })), rowCount: result.data.columns?.length || 0,
      elapsed: result.data.processor?.elapsed }
  })
  const sqlEntry = computed<DatasetCreationEntry | null>(() => {
    const scope = checkTableScope(content.value, database.value, table.value)
    return 'qualified' in scope && isSelect(scope.qualified) ? { origin: 'sql', sourceCode: String(route.params.source), query: scope.qualified } : null
  })
  const datasetAvailable = computed(() => !!resultEntry.value)
  function onCreateFromResult() { if (resultEntry.value) creationModeVisible.value = true }
  function openCreation(entry: DatasetCreationEntry) {
    datasetEntry.value = entry
    creationModeVisible.value = false
    datasetWizardVisible.value = true
  }
  function onExistingDataset() {
    if (!resultEntry.value) return
    datasetEntry.value = resultEntry.value
    existingMappingVisible.value = true
  }
  function onDatasetSubmitted(result: { code: string }) {
    datasetWizardVisible.value = false
    existingMappingVisible.value = false
    router.push(result.code ? `/dataset/detail/${encodeURIComponent(result.code)}?submitted=1` : '/dataset')
  }

  onScopeDispose(() => { revision++; cancel?.cancel(); observer?.disconnect() })
  return {
    content, response, running, executedQuery, scopeError, database, table, editorRoot, onEditorCreated, run, sourceCode: String(route.params.source),
    datasetAvailable, creationModeVisible, datasetWizardVisible, existingMappingVisible, datasetEntry, resultEntry, sqlEntry,
    onCreateFromResult, openCreation, onExistingDataset, onDatasetSubmitted,
  }
}
