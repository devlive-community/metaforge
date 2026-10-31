import { computed, inject, onMounted, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { mapSourceColumns, planColumnMoves, sourceColumnForm, sourceColumnPayload, type SourceColumn, type SourceColumnForm } from '@/model/source-column'
import { useRowSelection } from './useRowSelection'
import { usePanelState } from './usePanelState'
import { useExecutedSql } from './useExecutedSql'
import { mapDependencies, type ColumnDependency } from '@/model/column-dependency'
import { DROP_ROW_CLASS, useDragSort } from './useDragSort'

export function useSourceTableStructure() {
  const { t } = useI18n()
  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const sharedRows = inject<Ref<any[]> | null>('metadataColumnRows', null)
  const sharedLoading = inject<Ref<boolean> | null>('metadataLoading', null)
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))
  const stats = inject<{ indexes: Ref<any[]>; foreignKeys: Ref<any[]> }>('metadataTableStats', { indexes: ref([]), foreignKeys: ref([]) })
  const loading = sharedLoading || ref(false)
  const columns = ref<SourceColumn[]>([])
  const primaryNames = ref(new Set<string>())
  const detailName = ref<string | null>(null)
  const detailTab = ref('basic')
  const saving = ref(false)
  const nameSort = ref(false)
  const moreOptionsOpen = ref(false)
  const reorderVisible = ref(false)
  const dropVisible = ref(false)
  const dropChecking = ref(false)
  const dropNames = ref<string[]>([])
  const dropDependencies = ref<ColumnDependency[]>([])
  const dataTypes = ref<string[]>([])
  const typesLoading = ref(false)
  const editor = usePanelState<SourceColumn>()
  const panelVisible = editor.visible
  const panelMode = ref<'add' | 'edit' | 'insert'>('add')
  const panelAnchor = ref<string | null>(null)
  const panelForm = ref<SourceColumnForm>(sourceColumnForm())
  const panelIssues = ref<Record<string, string>>({})
  const indexOnly = ref(false)
  const executedSql = useExecutedSql()
  let revision = 0
  let typeRevision = 0
  let alive = true
  const identity = () => [route.params.source, route.params.database, route.params.table].join('/')
  const selection = useRowSelection(() => columns.value, column => column.name)
  const { selected: selectedNames, allSelected, partialSelected, toggleAll, toggle: toggleSelect, select: selectNames, prune } = selection
  const sortedColumns = computed(() => nameSort.value ? [...columns.value].sort((a, b) => a.name.localeCompare(b.name)) : columns.value)
  const detailColumn = computed(() => columns.value.find(column => column.name === detailName.value) || null)
  const typeOptions = computed(() => {
    const values = [...dataTypes.value]
    if (panelForm.value.type && !values.includes(panelForm.value.type)) values.unshift(panelForm.value.type)
    return values.map(value => ({ label: value, value }))
  })
  const defaultModeOptions = computed(() => [
    { label: t('source.structure.defaultNone'), value: 'none' },
    { label: t('source.structure.defaultConstant'), value: 'constant' },
    { label: 'CURRENT_TIMESTAMP', value: 'timestamp' },
  ])
  const charsetOptions = ['utf8mb4', 'utf8', 'latin1', 'gbk'].map(value => ({ label: value, value }))
  const collateOptions = computed(() => ['utf8mb4_general_ci', 'utf8mb4_unicode_ci', 'utf8mb4_bin', 'utf8_general_ci', 'latin1_swedish_ci', 'gbk_chinese_ci']
      .filter(value => !panelForm.value.charset || value.startsWith(panelForm.value.charset + '_')).map(value => ({ label: value, value })))
  const indexTypeOptions = computed(() => [
    { label: t('source.index.normal'), value: 'normal' }, { label: t('source.index.unique'), value: 'unique' },
  ])
  const compositeColumnOptions = computed(() => columns.value.filter(column => column.name !== panelForm.value.name).map(column => ({ label: column.name, value: column.name })))
  const detailTabs = computed(() => [
    { key: 'basic', label: t('source.common.info') }, { key: 'constraints', label: t('source.detail.constraints') },
    { key: 'indexes', label: t('source.detail.indexes') }, { key: 'foreignkeys', label: t('source.detail.foreignKeys') },
    { key: 'usage', label: t('source.detail.usage') },
  ])
  const groupedIndexes = computed(() => {
    const groups = new Map<string, { name: string; columns: string[]; unique: boolean }>()
    for (const row of stats.indexes.value) {
      const name = String(row.object_name || '')
      const index = groups.get(name) || { name, columns: [], unique: row.object_unique === true || row.object_unique === 'true' || row.object_unique === 1 }
      index.columns.push(String(row.object_column || ''))
      groups.set(name, index)
    }
    return [...groups.values()]
  })
  const detailIndexes = computed(() => groupedIndexes.value.filter(index => index.columns.includes(detailName.value || '')))
  const detailForeignKeys = computed(() => stats.foreignKeys.value.filter(row => row.object_column === detailName.value))
  const detailUnique = computed(() => !!detailName.value && ((primaryNames.value.size === 1 && primaryNames.value.has(detailName.value))
      || detailIndexes.value.some(index => index.unique && index.columns.length === 1)))
  const fieldHeaders = computed(() => [
    { title: '', key: '__check__', dataIndex: 'name', width: 37 },
    { title: '#', key: 'position', dataIndex: 'position', width: 41 },
    { title: t('source.structure.fieldName'), key: 'name', dataIndex: 'name', width: 102, ellipsis: true },
    { title: t('source.structure.type'), key: 'type', dataIndex: 'type', width: 120 },
    { title: t('source.structure.lengthPrecision'), key: 'length', dataIndex: 'length', width: 96 },
    { title: t('source.structure.nullable'), key: 'nullable', dataIndex: 'nullable', width: 61 },
    { title: t('source.common.defaultValue'), key: 'defaultValue', dataIndex: 'default', width: 162, ellipsis: true },
    { title: t('source.structure.comment'), key: 'comment', dataIndex: 'comment', width: 209, ellipsis: true },
    { title: t('common.action'), key: 'action', dataIndex: 'name', width: 98, fixed: 'right' as const },
  ].filter(column => column.key !== 'action' || can('column:modify') || can('column:drop')))
  const typeTone = (type: string): 'info' | 'success' | 'warning' | 'default' => /CHAR|ENUM|SET/.test(type) ? 'success' : /TEXT/.test(type) ? 'warning' : /INT|FLOAT|DOUBLE|DECIMAL|NUMERIC/.test(type) ? 'info' : 'default'
  const typeColor = (type: string) => /JSON|DATE|TIME/.test(type) ? 'var(--mf-color-data-complex)' : undefined
  const displayDefault = (column: SourceColumn) => column.autoincrement === 'YES' ? t('source.common.autoIncrement')
      : column.default === null ? (column.nullable === 'YES' ? 'NULL' : '-') : column.default === '' ? "''" : column.default
  type DetailItem = { label: string; value: string; tone?: string }
  const detailBasics = computed<DetailItem[]>(() => {
    const column = detailColumn.value
    if (!column) return []
    return [
      { label: t('source.structure.fieldName'), value: column.name },
      { label: t('source.common.columnType'), value: column.type },
      { label: t('source.prototype.length'), value: column.length || '-' },
      { label: t('source.structure.nullable'), value: t(column.nullable === 'NO' ? 'common.no' : 'common.yes'), tone: column.nullable === 'NO' ? 'text-danger' : '' },
      { label: t('source.common.defaultValue'), value: displayDefault(column) },
      { label: t('source.structure.charset'), value: column.charset || '-' },
      { label: t('source.common.collation'), value: column.collate || '-' },
    ]
  })
  const detailConstraints = computed(() => {
    const column = detailColumn.value
    if (!column) return []
    return [
      { label: t('source.prototype.isPrimary'), value: t(primaryNames.value.has(column.name) ? 'common.yes' : 'common.no'), tone: primaryNames.value.has(column.name) ? 'text-success' : '' },
      { label: t('source.prototype.isUnique'), value: t(detailUnique.value ? 'common.yes' : 'common.no'), tone: detailUnique.value ? 'text-success' : '' },
      { label: t('source.prototype.isAutoIncrement'), value: t(column.autoincrement === 'YES' ? 'common.yes' : 'common.no'), tone: column.autoincrement === 'YES' ? 'text-success' : '' },
    ]
  })
  const detailAdditional = computed<DetailItem[]>(() => [
    { label: t('source.structure.comment'), value: detailColumn.value?.comment || '-' },
    ...detailConstraints.value,
    { label: t('common.updateTime'), value: '-' },
    { label: t('common.createTime'), value: '-' },
  ])

  function applyRows(rows: any[]) {
    columns.value = mapSourceColumns(rows)
    primaryNames.value = new Set(rows.filter(row => row.type_name === 'primary').map(row => String(row.object_name)))
    if (!columns.value.some(column => column.name === detailName.value)) detailName.value = columns.value[0]?.name || null
    prune()
  }
  async function handleInitialize() {
    if (sharedRows && refreshKey) { refreshKey.value++; return }
    const request = ++revision
    loading.value = true
    try {
      const response = await MetadataService.getColumnsByTable(String(route.params.source), String(route.params.database), String(route.params.table))
      if (request !== revision || !alive) return
      assertSuccess(response)
      applyRows(response.data.columns || [])
    } catch (error) { if (request === revision && alive) MfMessage.error(errorMessage(error)) }
    finally { if (request === revision && alive) loading.value = false }
  }
  function assertSuccess(response: any) {
    if (!response.status || response.data?.isSuccessful !== true) throw new Error(response.data?.message || response.message || t('common.loadingFailed'))
  }
  const errorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)
  function openPanel(mode: 'add' | 'edit' | 'insert', row: SourceColumn | null = null) {
    if (saving.value || !can(mode === 'edit' ? 'column:modify' : 'column:create')) return
    panelMode.value = mode
    panelAnchor.value = mode === 'insert' ? row?.name || detailColumn.value?.name || null : null
    panelForm.value = sourceColumnForm()
    if (mode === 'edit' && row) {
      const timestamp = /^CURRENT_TIMESTAMP(?:\(\))?$/i.test(row.default || '')
      panelForm.value = { ...sourceColumnForm(), name: row.name, type: row.type, length: row.length,
        nullable: row.nullable !== 'NO', autoIncrement: row.autoincrement === 'YES',
        defaultMode: timestamp ? 'timestamp' : row.default !== null ? 'constant' : 'none',
        defaultValue: timestamp ? '' : row.default || '', comment: row.comment, charset: row.charset, collate: row.collate,
        indexName: 'idx_' + row.name }
    }
    indexOnly.value = false
    panelIssues.value = {}
    moreOptionsOpen.value = false
    editor.open(row)
  }
  function closePanel(force = false) {
    if (saving.value && !force) return
    editor.close()
    panelIssues.value = {}
    indexOnly.value = false
  }
  function onInsertField() {
    const anchor = detailColumn.value || columns.value[columns.value.length - 1]
    if (anchor) openPanel('insert', anchor)
    else MfMessage.warning(t('source.structure.selectFirst'))
  }
  async function onSubmitPanel() {
    if (saving.value || !can(indexOnly.value ? 'index:create' : panelMode.value === 'edit' ? 'column:modify' : 'column:create')) return
    panelIssues.value = {}
    const form = { ...panelForm.value, compositeColumns: [...panelForm.value.compositeColumns] }
    const key = identity()
    const source = String(route.params.source), database = String(route.params.database), table = String(route.params.table)
    const column = sourceColumnPayload(form)
    if (panelMode.value === 'insert' && panelAnchor.value) column.after = panelAnchor.value
    let columnSaved = indexOnly.value
    const recorder = executedSql.record(indexOnly.value
        ? { operation: t('source.executed.createIndex'), table, target: { label: t('source.executed.index'), value: form.indexName.trim() } }
        : { operation: t(panelMode.value === 'edit' ? 'source.executed.editField' : panelMode.value === 'insert' ? 'source.executed.insertField' : 'source.executed.addField'),
          table, target: { label: t('source.executed.field'), value: form.name.trim() } })
    saving.value = true
    try {
      if (!indexOnly.value) {
        const response = await recorder.run(panelMode.value === 'edit'
            ? MetadataService.changeColumn(source, database, table, { name: table, columns: [column] })
            : MetadataService.createColumn(source, database, table, { name: table, columns: [column] }))
        assertSuccess(response)
        columnSaved = true
      }
      if (form.createIndex) {
        const response = await recorder.run(MetadataService.createIndex(source, database, table, { name: table, indexes: [{
          name: form.indexName.trim(), columns: [form.name.trim(), ...(form.compositeIndex ? form.compositeColumns : [])], unique: form.indexType === 'unique',
        }] }))
        assertSuccess(response)
      }
      if (!alive || identity() !== key) return
      MfMessage.success(t('source.structure.saveSuccess'))
      closePanel(true)
      await handleInitialize()
    } catch (error) {
      recorder.fail(error)
      if (!alive || identity() !== key) return
      if (columnSaved && form.createIndex) {
        indexOnly.value = true
        panelMode.value = 'edit'
        panelIssues.value = { indexName: t('source.prototype.indexFailed', { message: errorMessage(error) }) }
        moreOptionsOpen.value = true
        await handleInitialize()
      } else panelIssues.value = { name: errorMessage(error) }
    } finally { saving.value = false }
  }
  /**
   * Looks up what depends on the fields before opening the confirmation, so the dialog opens with
   * its final content: a plain confirmation, or the dependencies and a forced-drop option.
   */
  async function confirmDrop(names: string[]) {
    if (names.length === 0 || saving.value || dropChecking.value || !can('column:drop')) return
    const key = identity()
    dropChecking.value = true
    try {
      const response = await MetadataService.getColumnDependencies(String(route.params.source), String(route.params.database), String(route.params.table), {
        name: String(route.params.table), columns: names.map(name => ({ name })),
      })
      assertSuccess(response)
      if (!alive || identity() !== key) return
      dropDependencies.value = mapDependencies(response.data?.columns || [])
      dropNames.value = names
      selectNames(names)
      dropVisible.value = true
    } catch (error) {
      if (alive && identity() === key) MfMessage.error(errorMessage(error))
    } finally {
      dropChecking.value = false
    }
  }
  const onRequestDrop = () => confirmDrop(selectedNames.value.size ? [...selectedNames.value] : detailColumn.value ? [detailColumn.value.name] : [])
  async function onDropFields(force = false) {
    if (!dropNames.value.length || saving.value || !can('column:drop')) return
    const key = identity()
    const names = [...dropNames.value]
    const recorder = executedSql.record({ operation: t('source.executed.dropField'), table: String(route.params.table), target: { label: t('source.executed.fields'), value: names.join(', ') } })
    saving.value = true
    try {
      const response = await recorder.run(MetadataService.dropColumn(String(route.params.source), String(route.params.database), String(route.params.table), {
        name: String(route.params.table), columns: names.map(name => ({ name })), force,
      }))
      assertSuccess(response)
      if (!alive || identity() !== key) return
      MfMessage.success(t('source.structure.dropSuccess'))
      dropVisible.value = false
      await handleInitialize()
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === key) MfMessage.error(errorMessage(error))
    }
    finally { saving.value = false }
  }
  function positionPayload(column: SourceColumn, previous: string | null) {
    const form: SourceColumnForm = { ...sourceColumnForm(), ...column, nullable: column.nullable !== 'NO',
      autoIncrement: column.autoincrement === 'YES', defaultMode: /^CURRENT_TIMESTAMP(?:\(\))?$/i.test(column.default || '') ? 'timestamp'
          : column.default !== null ? 'constant' : 'none', defaultValue: column.default || '' }
    const payload = sourceColumnPayload(form)
    if (previous === null) payload.first = true
    else payload.after = previous
    return payload
  }
  /** Moves columns into the given order with one ALTER per displaced column; resolves true when every move succeeded. */
  function applyOrder(names: string[]): Promise<boolean> {
    const current = columns.value.map(column => column.name)
    if (names.length !== current.length || names.some(name => !current.includes(name))) return Promise.resolve(false)
    return runMoves(planColumnMoves(current, names))
  }
  async function runMoves(planned: { name: string; previous: string | null }[]): Promise<boolean> {
    if (saving.value || !can('column:reorder')) return false
    if (!planned.length) return true
    const moves = planned.map(move => ({ column: columns.value.find(column => column.name === move.name)!, previous: move.previous }))
    if (moves.some(move => !dataTypes.value.includes(move.column.type))) { MfMessage.error(t('source.prototype.unsupportedType')); return false }
    const key = identity()
    const recorder = executedSql.record({ operation: t('source.executed.reorderField'), table: String(route.params.table),
      target: { label: t('source.executed.fields'), value: moves.map(move => move.column.name).join(', ') } })
    saving.value = true
    try {
      for (const move of moves) {
        const response = await recorder.run(MetadataService.changeColumn(String(route.params.source), String(route.params.database), String(route.params.table),
          { name: String(route.params.table), columns: [positionPayload(move.column, move.previous)] }))
        assertSuccess(response)
      }
      return true
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === key) MfMessage.error(errorMessage(error))
      return false
    } finally {
      saving.value = false
      // Reload even after a partial failure so the table shows the order the database actually has.
      if (alive && identity() === key) { nameSort.value = false; await handleInitialize() }
    }
  }
  /** A single drag or arrow move always alters the column the user picked. */
  function moveTo(name: string, targetIndex: number) {
    if (targetIndex < 0 || targetIndex >= columns.value.length) return Promise.resolve(false)
    const names = columns.value.map(column => column.name).filter(item => item !== name)
    names.splice(targetIndex, 0, name)
    return runMoves([{ name, previous: targetIndex ? names[targetIndex - 1] : null }])
  }
  const onMove = (direction: -1 | 1) => {
    const name = detailColumn.value?.name
    return name ? moveTo(name, columns.value.findIndex(column => column.name === name) + direction) : Promise.resolve(false)
  }
  const selectDetail = (column: SourceColumn) => { detailName.value = column.name; detailTab.value = 'basic' }
  const sort = useDragSort(() => columns.value.map(column => column.name))
  const rowClass = (column: SourceColumn) => {
    const side = sort.dropSide(column.name)
    return side ? DROP_ROW_CLASS[side] : column.name === detailName.value ? 'bg-primary/5' : ''
  }
  const toggleNameSort = () => { nameSort.value = !nameSort.value }
  // The whole row is the drop target: resolve it from the MfTable row key under the pointer.
  const rowName = (event: DragEvent) => (event.target as HTMLElement | null)?.closest<HTMLElement>('tr[data-row-key]')?.dataset.rowKey
  const onRowDragOver = (event: DragEvent) => {
    const name = rowName(event)
    if (!name || !sort.dragging.value) return
    event.preventDefault()
    sort.enter(name)
  }
  const onRowDrop = (event: DragEvent) => {
    const target = rowName(event)
    event.preventDefault()
    const name = target ? sort.drop(target) : (sort.end(), null)
    if (name && target) moveTo(name, columns.value.findIndex(column => column.name === target))
  }
  watch(panelVisible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(() => panelForm.value.name, (value, previous) => {
    if (!panelForm.value.indexName || panelForm.value.indexName === 'idx_' + previous) panelForm.value.indexName = value ? 'idx_' + value : ''
  })
  watch(() => route.params.source, async source => {
    const request = ++typeRevision
    typesLoading.value = true
    dataTypes.value = []
    if (!can('datatype:list')) { typesLoading.value = false; return }
    try {
      const response = await MetadataService.getDataTypes(String(source || ''))
      if (request !== typeRevision || !alive) return
      assertSuccess(response)
      dataTypes.value = (response.data.columns || []).map((value: any) => typeof value === 'string' ? value : value.value || value.name).filter(Boolean)
    } catch (error) { if (request === typeRevision && alive) MfMessage.error(errorMessage(error)) }
    finally { if (request === typeRevision && alive) typesLoading.value = false }
  }, { immediate: true })
  watch(() => [route.params.source, route.params.database, route.params.table], () => { closePanel(true); detailTab.value = 'basic'; selectNames([]); if (!sharedRows) handleInitialize() })
  if (sharedRows) watch(sharedRows, applyRows, { immediate: true })
  else { onMounted(handleInitialize); if (refreshKey) watch(refreshKey, handleInitialize) }
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; typeRevision++; fieldPanelOpen.value = false })
  return { can, loading, columns, primaryNames, detailName, selectedNames, allSelected, partialSelected, toggleAll, toggleSelect,
    panelVisible, panelMode, panelAnchor, saving, moreOptionsOpen, dropVisible, dropChecking, dropNames, dropDependencies, panelForm, panelIssues, indexOnly, fieldPanelHost, fieldPanelWidth,
    reorderVisible, nameSort, sortedColumns, toggleNameSort, typeOptions, dataTypes, typesLoading, defaultModeOptions,
    charsetOptions, collateOptions, indexTypeOptions, compositeColumnOptions, detailColumn, detailTab, detailTabs, detailIndexes,
    detailForeignKeys, detailUnique, detailBasics, detailConstraints, detailAdditional, fieldHeaders, typeTone, typeColor, displayDefault, openPanel, closePanel, onInsertField,
    onSubmitPanel, confirmDrop, onRequestDrop, onDropFields, onMove, applyOrder, selectDetail, rowClass, sort, onRowDragOver, onRowDrop,
    executed: executedSql.executed, executedVisible: executedSql.visible }
}
