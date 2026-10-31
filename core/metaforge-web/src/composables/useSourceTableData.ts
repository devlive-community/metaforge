import { computed, inject, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { mapSourceColumns } from '@/model/source-column'
import { buildDataFilters, dataFields, dataKind, defaultOperator, emptyValue, formToJson, formToRow, jsonToRow, rowToForm, toCsv, toJson, validateForm,
  type DataCondition, type DataField, type DataOperator, type FieldIssue, type FormValues } from '@/model/source-data'
import { DEFAULT_TABLE_SETTINGS, defaultColumnView, EXPORT_LIMIT, INDEX_COLUMN, MAX_SORT_RULES, quickSorts, readColumnView, readDefaultSort, readTableSettings,
  type ColumnView, type ExportFormat, type SortRule, type TableSettings } from '@/model/source-data-view'
import { useExecutedSql } from './useExecutedSql'
import { useRowSelection } from './useRowSelection'
import { usePanelState } from './usePanelState'

export type DataRow = Record<string, unknown> & { __key: string }
export type RowEditorMode = 'add' | 'edit' | 'copy'
export type SidePanel = 'columns' | 'sort' | 'settings'

const SETTINGS_KEY = 'MetaForgeDataSettings'

/** Data preview of one table: server-side filters, sort and paging, plus row insert/update/delete. */
export function useSourceTableData() {
  const { t } = useI18n()
  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const sharedRows = inject<Ref<any[]>>('metadataColumnRows', ref([]))
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const loading = ref(false)
  const saving = ref(false)
  const headers = ref<string[]>([])
  const rows = ref<DataRow[]>([])
  const total = ref(0)
  const page = ref(1)
  // Viewer-wide table preferences; the page size only persists when the viewer asks for it.
  let storedSettings: string | null = null
  try { storedSettings = localStorage.getItem(SETTINGS_KEY) } catch { /* Preferences are optional. */ }
  const settings = ref<TableSettings>(readTableSettings(storedSettings))
  const size = ref(settings.value.rememberPageSize ? settings.value.pageSize : DEFAULT_TABLE_SETTINGS.pageSize)
  const exporting = ref(false)
  const defaultSort = ref<SortRule | null>(null)
  const orders = ref<SortRule[]>([])
  let nextConditionId = 1
  const conditions = ref<DataCondition[]>([])
  let applied: DataCondition[] = []
  const deleteTargets = ref<DataRow[]>([])
  let revision = 0
  let alive = true

  const columnMeta = computed(() => mapSourceColumns(sharedRows.value || []))
  const typeOf = (name: string) => columnMeta.value.find(column => column.name === name)?.type || ''
  /** Columns known from metadata keep their table order; the query headers fill in when metadata is unavailable. */
  const columnNames = computed(() => columnMeta.value.length ? columnMeta.value.map(column => column.name) : headers.value)
  // Column layout (order, visibility, width, alignment, pinning) is a per-viewer preference per table.
  const columnView = ref<ColumnView>(defaultColumnView([]))
  const viewKey = () => `MetaForgeDataView:${identity()}`
  const sortKey = () => `MetaForgeDataSort:${identity()}`
  /** Saves the settings panel; vertical borders are the same switch as in the column settings. */
  function applySettings(next: TableSettings, bordered: boolean, sortRule: SortRule | null) {
    const sizeChanged = next.pageSize !== size.value
    settings.value = next
    defaultSort.value = sortRule
    try {
      localStorage.setItem(SETTINGS_KEY, JSON.stringify(next))
      if (sortRule) localStorage.setItem(sortKey(), JSON.stringify(sortRule)); else localStorage.removeItem(sortKey())
    } catch { /* Preferences are optional. */ }
    if (bordered !== columnView.value.bordered) applyColumnView({ ...columnView.value, bordered })
    sidePanel.value = null
    if (sizeChanged) { size.value = next.pageSize; page.value = 1; load() }
  }
  const visibleColumns = computed(() => columnView.value.columns.filter(column => column.visible && column.name !== INDEX_COLUMN).map(column => column.name))
  /** Header drags update the width live and save it once the drag ends. */
  function resizeColumn(name: string, width: number, persist: boolean) {
    const key = name === '__index__' ? INDEX_COLUMN : name
    const view = { ...columnView.value, columns: columnView.value.columns.map(column => column.name === key ? { ...column, width } : column) }
    if (persist) applyColumnView(view)
    else columnView.value = view
  }
  function applyColumnView(view: ColumnView) {
    columnView.value = view
    try { localStorage.setItem(viewKey(), JSON.stringify(view)) } catch { /* Preferences are optional. */ }
    sidePanel.value = null
  }
  const selection = useRowSelection(() => rows.value, row => row.__key)
  const { selected, allSelected, partialSelected, toggleAll, toggle, select } = selection
  const selectedRows = computed(() => rows.value.filter(row => selected.value.has(row.__key)))
  // Shift-click selects from the last row clicked or checked; plain clicks replace the selection.
  let anchor: number | null = null
  function onRowClick(row: DataRow, index: number, event: MouseEvent) {
    if (event.shiftKey && anchor !== null) {
      const [from, to] = anchor < index ? [anchor, index] : [index, anchor]
      select(rows.value.slice(from, to + 1).map(item => item.__key))
      return
    }
    anchor = index
    if (event.metaKey || event.ctrlKey) toggle(row.__key, !selected.value.has(row.__key))
    else select([row.__key])
  }
  function onCheck(row: DataRow, index: number, checked: boolean) {
    anchor = index
    toggle(row.__key, checked)
  }
  const rowClass = (row: DataRow) => selected.value.has(row.__key) ? 'bg-surface-hover [&>td]:bg-surface-hover' : ''
  const editor = usePanelState<DataRow>()
  const editorMode = ref<RowEditorMode>('add')
  const editorValues = ref<FormValues>({})
  const editorOriginal = ref<FormValues>({})
  const editorTab = ref<'form' | 'json'>('form')
  const editorJson = ref('')
  const editorIssues = ref<Record<string, FieldIssue>>({})
  const jsonError = ref('')
  const continueAdding = ref(false)
  const sidePanel = ref<SidePanel | null>(null)
  const executedSql = useExecutedSql()
  const editorMultiple = ref(false)

  const identity = () => [route.params.source, route.params.database, route.params.table].join('/')
  const errorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)
  function assertSuccess(response: any) {
    if (!response.status || response.data?.isSuccessful === false) throw new Error(response.data?.message || response.message || t('common.loadingFailed'))
  }
  /** Rows sent back to the server for WHERE clauses must not carry the client-side row key. */
  const plain = (row: DataRow) => Object.fromEntries(Object.entries(row).filter(([key]) => key !== '__key'))

  async function load() {
    const request = ++revision
    const [source, database, table] = [route.params.source, route.params.database, route.params.table].map(String)
    if (!can('table:read') || !source || !database || !table) { rows.value = []; total.value = 0; return }
    loading.value = true
    try {
      const response = await MetadataService.queryTable(source, database, table, {
        pagination: { page: page.value, size: size.value },
        filters: buildDataFilters(applied),
        orders: orders.value,
      })
      if (request !== revision || !alive) return
      assertSuccess(response)
      const data = response.data || {}
      headers.value = data.headers || []
      rows.value = (data.columns || []).map((row: Record<string, unknown>, index: number) => ({ ...row, __key: String(index) }))
      total.value = Number(data.pagination?.total ?? rows.value.length)
      select([])
      anchor = null
    } catch (error) {
      if (request === revision && alive) { rows.value = []; total.value = 0; MfMessage.error(errorMessage(error)) }
    } finally {
      if (request === revision && alive) loading.value = false
    }
  }

  // Filter rows
  function addCondition(column = '') {
    const operator = defaultOperator(dataKind(typeOf(column)))
    conditions.value = [...conditions.value, { id: nextConditionId++, column, operator, value: emptyValue(operator) }]
  }
  function changeColumn(condition: DataCondition, column: string) {
    condition.column = column
    condition.operator = defaultOperator(dataKind(typeOf(column)))
    condition.value = emptyValue(condition.operator)
  }
  function changeOperator(condition: DataCondition, operator: DataOperator) {
    const previous = condition.operator
    condition.operator = operator
    if (Array.isArray(emptyValue(previous)) !== Array.isArray(emptyValue(operator)) || previous === 'BETWEEN' || operator === 'BETWEEN') condition.value = emptyValue(operator)
  }
  const removeCondition = (id: number) => { conditions.value = conditions.value.filter(condition => condition.id !== id) }
  const clearConditions = () => { conditions.value = [] }
  function onSearch() {
    applied = conditions.value.map(condition => ({ ...condition, value: Array.isArray(condition.value) ? [...condition.value] : condition.value }))
    page.value = 1
    load()
  }
  function onReset() {
    conditions.value = []
    addCondition()
    onSearch()
  }

  // Paging, sort and columns
  function onPageChange(next: number, nextSize: number) {
    if (nextSize !== size.value) { size.value = nextSize; page.value = 1 } else page.value = next
    load()
  }
  function applyOrders(rules: SortRule[]) {
    orders.value = rules.slice(0, MAX_SORT_RULES)
    sidePanel.value = null
    page.value = 1
    load()
  }
  const primaryKeys = computed(() => (sharedRows.value || []).filter(row => row.type_name === 'primary').map(row => String(row.object_name)))

  // Row editor: a typed form or raw JSON, kept in sync when the user switches between them.
  const fields = computed<DataField[]>(() => {
    const list = dataFields(sharedRows.value || [])
    return list.length ? list : headers.value.map(name => ({ name, type: '', kind: 'text' as const, options: [], nullable: true, hasDefault: false, autoIncrement: false, required: false }))
  })
  /**
   * Opens the editor for a new row, one row to edit, or rows to copy. Copies drop generated keys;
   * several copied rows can only be edited as JSON.
   */
  function openEditor(mode: RowEditorMode, source: DataRow[] = []) {
    if (saving.value || !can(mode === 'edit' ? 'data:modify' : 'data:insert')) return
    if (mode !== 'add' && !source.length) return
    const generated = (values: FormValues) => mode === 'copy'
        ? { ...values, ...Object.fromEntries(fields.value.filter(field => field.autoIncrement).map(field => [field.name, null])) } : values
    const forms = mode === 'add' ? [rowToForm(fields.value, null)] : source.map(row => generated(rowToForm(fields.value, row)))
    editorMode.value = mode
    editorValues.value = forms[0]
    editorOriginal.value = { ...forms[0] }
    editorMultiple.value = forms.length > 1
    editorTab.value = editorMultiple.value ? 'json' : 'form'
    editorJson.value = JSON.stringify(editorMultiple.value ? forms.map(form => formToJson(fields.value, form)) : formToJson(fields.value, forms[0]), null, 2)
    editorIssues.value = {}
    jsonError.value = ''
    sidePanel.value = null
    editor.open(mode === 'edit' ? source[0] : null)
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  /** Parses the JSON editor into row objects; a single object or an array of objects. */
  function parseJsonRows(): Record<string, unknown>[] | null {
    let parsed: unknown
    try { parsed = JSON.parse(editorJson.value) } catch (error) {
      jsonError.value = t('source.data.jsonInvalid', { message: errorMessage(error) })
      return null
    }
    const list = Array.isArray(parsed) ? parsed : [parsed]
    if (!list.length || list.some(item => item === null || typeof item !== 'object' || Array.isArray(item))) {
      jsonError.value = t('source.data.jsonObjectRequired')
      return null
    }
    jsonError.value = ''
    return list as Record<string, unknown>[]
  }
  function setEditorTab(tab: 'form' | 'json') {
    if (tab === editorTab.value) return
    if (tab === 'json') {
      editorJson.value = JSON.stringify(formToJson(fields.value, editorValues.value), null, 2)
      jsonError.value = ''
      editorTab.value = 'json'
      return
    }
    const parsed = parseJsonRows()
    if (!parsed) return
    if (parsed.length !== 1) { jsonError.value = t('source.data.formSingleRow'); return }
    editorValues.value = rowToForm(fields.value, parsed[0])
    editorIssues.value = {}
    editorTab.value = 'form'
  }
  /** Rows to send: typed form values, or the parsed JSON. Edits keep only the fields that changed. */
  function editorRows(): Record<string, unknown>[] | null {
    const editing = editorMode.value === 'edit'
    if (editorTab.value === 'form') {
      editorIssues.value = validateForm(fields.value, editorValues.value)
      if (Object.keys(editorIssues.value).length) return null
      if (!editing) return [formToRow(fields.value, editorValues.value)]
      const changed = fields.value.filter(field => editorValues.value[field.name] !== editorOriginal.value[field.name])
      return [formToRow(changed, editorValues.value, false)]
    }
    const parsed = parseJsonRows()
    if (!parsed) return null
    if (!editing) return parsed.map(row => jsonToRow(fields.value, row))
    if (parsed.length !== 1) { jsonError.value = t('source.data.formSingleRow'); return null }
    const original = formToJson(fields.value, editorOriginal.value)
    const changed = Object.fromEntries(Object.entries(parsed[0]).filter(([name, value]) => JSON.stringify(value) !== JSON.stringify(original[name])))
    return [jsonToRow(fields.value, changed)]
  }
  async function submitEditor() {
    if (saving.value) return
    const rowsToSave = editorRows()
    if (!rowsToSave) return
    const [source, database, table] = [route.params.source, route.params.database, route.params.table].map(String)
    const target = editor.target.value
    if (editorMode.value === 'edit') {
      if (!target || !Object.keys(rowsToSave[0]).length) { closeEditor(); return }
      await mutate(MetadataService.updateData(source, database, table, { preview: false, rows: [{ row: rowsToSave[0], original: plain(target) }] }), () => closeEditor(true),
        { operation: 'source.executed.editRow', rows: 1 })
      return
    }
    const operation = editorMode.value === 'copy' ? 'source.executed.copyRow' : 'source.executed.addRow'
    await mutate(MetadataService.insertData(source, database, table, { preview: false, rows: rowsToSave }), () => {
      // "Continue adding" keeps the panel open with a blank form for the next row.
      if (continueAdding.value && editorMode.value === 'add') {
        editorValues.value = rowToForm(fields.value, null)
        editorJson.value = JSON.stringify(formToJson(fields.value, editorValues.value), null, 2)
        editorIssues.value = {}
      } else closeEditor(true)
    }, { operation, rows: rowsToSave.length })
  }
  /** Runs one data change, records the SQL it executed, then reloads the page on success. */
  async function mutate(request: Promise<any>, onSuccess: () => void, info: { operation: string; rows: number }) {
    const key = identity()
    const recorder = executedSql.record({ operation: t(info.operation), table: String(route.params.table), target: { label: t('source.executed.rows'), value: String(info.rows) } })
    saving.value = true
    try {
      assertSuccess(await recorder.run(request))
      if (!alive || identity() !== key) return
      MfMessage.success(t('source.tip.updateSuccess'))
      onSuccess()
      await load()
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === key) MfMessage.error(errorMessage(error))
    } finally {
      saving.value = false
    }
  }


  // Single rows and batches are confirmed in a dialog; the delete is recorded like any other change.
  const requestDelete = (targets: DataRow[]) => { if (targets.length && can('data:delete') && !saving.value) deleteTargets.value = targets }
  const deleteRows = computed(() => deleteTargets.value.map(plain))
  const closeDelete = () => { if (!saving.value) deleteTargets.value = [] }
  async function confirmDelete() {
    const targets = deleteRows.value
    if (!targets.length || saving.value) return
    const [source, database, table] = [route.params.source, route.params.database, route.params.table].map(String)
    await mutate(MetadataService.deleteData(source, database, table, { preview: false, columns: targets.map(original => ({ original })) }),
      () => { deleteTargets.value = [] }, { operation: 'source.executed.deleteRow', rows: targets.length })
  }

  /**
   * Exports the selected rows, or every row the query matches (the current filters only when the
   * settings say so), up to EXPORT_LIMIT rows, with the visible columns.
   */
  async function exportRows(format: ExportFormat) {
    if (exporting.value) return
    let data = selectedRows.value.map(plain)
    if (!data.length) {
      const [source, database, table] = [route.params.source, route.params.database, route.params.table].map(String)
      exporting.value = true
      try {
        const response = await MetadataService.queryTable(source, database, table, {
          pagination: { page: 1, size: EXPORT_LIMIT },
          filters: settings.value.exportWithFilters ? buildDataFilters(applied) : [],
          orders: orders.value,
        })
        assertSuccess(response)
        data = response.data?.columns || []
        if (data.length >= EXPORT_LIMIT) MfMessage.warning(t('source.data.exportTruncated', { count: EXPORT_LIMIT.toLocaleString() }))
      } catch (error) {
        MfMessage.error(errorMessage(error))
        return
      } finally {
        exporting.value = false
      }
    }
    const content = format === 'csv' ? '\uFEFF' + toCsv(visibleColumns.value, data) : toJson(visibleColumns.value, data)
    const url = URL.createObjectURL(new Blob([content], { type: format === 'csv' ? 'text/csv;charset=utf-8' : 'application/json' }))
    const link = document.createElement('a')
    link.href = url
    link.download = `${route.params.table}.${format}`
    link.click()
    URL.revokeObjectURL(url)
  }

  /** The editor and the settings panels share the docked right pane; opening one closes the other. */
  function openPanel(panel: SidePanel) {
    if (saving.value) return
    closeEditor(true)
    sidePanel.value = panel
  }
  const closePanel = () => { sidePanel.value = null }
  watch(() => editor.visible.value || sidePanel.value !== null, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(() => [identity(), fields.value.map(field => field.name).join('\u0000')], () => {
    let saved: string | null = null
    try { saved = localStorage.getItem(viewKey()) } catch { /* Preferences are optional. */ }
    columnView.value = readColumnView(saved, fields.value)
  }, { immediate: true })
  watch(identity, () => {
    closeEditor(true)
    sidePanel.value = null
    let savedSort: string | null = null
    try { savedSort = localStorage.getItem(sortKey()) } catch { /* Preferences are optional. */ }
    defaultSort.value = readDefaultSort(savedSort)
    orders.value = defaultSort.value ? [defaultSort.value] : []
    conditions.value = []
    addCondition()
    applied = []
    page.value = 1
    load()
  }, { immediate: true })
  // The header's refresh reloads this tab's own rows too.
  const manualRefresh = inject<Ref<number> | null>('metadataManualRefresh', null)
  if (manualRefresh) watch(manualRefresh, () => { load() })
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; fieldPanelOpen.value = false })

  return {
    can, loading, saving, rows, total, page, size, columnNames, visibleColumns, orders, typeOf,
    sidePanel, openPanel, closePanel, columnView, applyColumnView, resizeColumn, settings, defaultSort, applySettings, exporting, defaultView: computed(() => defaultColumnView(fields.value)),
    conditions, addCondition, changeColumn, changeOperator, removeCondition, clearConditions, onSearch, onReset,
    onPageChange, applyOrders, quickSortRules: computed(() => quickSorts(fields.value, primaryKeys.value)), selected, selectedRows, allSelected, partialSelected, toggleAll, onRowClick, onCheck, rowClass,
    editorVisible: editor.visible, editorMode, editorValues, editorTab, editorJson, editorIssues, jsonError, continueAdding, editorMultiple, fields,
    openEditor, closeEditor, setEditorTab, submitEditor, fieldPanelHost, fieldPanelWidth,
    deleteTargets, deleteRows, requestDelete, closeDelete, confirmDelete, primaryKeys, executed: executedSql.executed, executedVisible: executedSql.visible, exportRows, refresh: load,
  }
}
