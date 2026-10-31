import { computed, inject, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { mapSourceColumns, type SourceColumn } from '@/model/source-column'
import { emptyForeignKeyForm, foreignKeyToForm, formToForeignKey, groupForeignKeys, validateForeignKeyForm,
  type ForeignKeyForm, type ForeignKeyIssue, type SourceForeignKey } from '@/model/source-foreign-key'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { useRowSelection } from './useRowSelection'
import { usePanelState } from './usePanelState'
import { useExecutedSql } from './useExecutedSql'

export interface TableShape {
  columns: SourceColumn[]
  primaryKeys: string[]
}

/**
 * Foreign key tab of a table's workbench, built like the index tab: list and details with a
 * relation diagram, a docked editor whose SQL is previewed by the server, rebuild and drop.
 */
export function useSourceTableForeignKeys() {
  const { t } = useI18n()
  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const columnRows = inject<Ref<any[]>>('metadataColumnRows', ref([]))
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const loading = ref(false)
  const saving = ref(false)
  const keys = ref<SourceForeignKey[]>([])
  const tables = ref<string[]>([])
  const shapes = ref<Record<string, TableShape>>({})
  const detailName = ref<string | null>(null)
  const detailSql = ref('')
  const editor = usePanelState<SourceForeignKey>()
  const editorMode = ref<'add' | 'edit'>('add')
  const form = ref<ForeignKeyForm>(emptyForeignKeyForm(''))
  const issues = ref<Partial<Record<'name' | 'table' | 'columns', ForeignKeyIssue>>>({})
  const previewSql = ref('')
  const previewError = ref('')
  const dropTargets = ref<SourceForeignKey[]>([])
  const executedSql = useExecutedSql()
  let revision = 0
  let previewRevision = 0
  let alive = true

  const identity = () => [route.params.source, route.params.database, route.params.table].join('/')
  const location = () => [route.params.source, route.params.database, route.params.table].map(String) as [string, string, string]
  const errorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)
  function assertSuccess(response: any) {
    if (!response.status || response.data?.isSuccessful === false) throw new Error(response.data?.message || response.message || t('common.loadingFailed'))
  }
  const shapeOf = (rows: any[]): TableShape => ({
    columns: mapSourceColumns(rows || []),
    primaryKeys: (rows || []).filter(row => row.type_name === 'primary').map(row => String(row.object_name)),
  })
  const localShape = computed(() => shapeOf(columnRows.value))
  const shapeKey = (database: string, table: string) => `${database}\u0000${table}`
  /** Columns of a referenced table, fetched once per table. */
  async function loadShape(database: string, table: string) {
    const key = shapeKey(database, table)
    if (!table || shapes.value[key]) return
    try {
      const response = await MetadataService.getColumnsByTable(location()[0], database, table)
      assertSuccess(response)
      if (alive) shapes.value = { ...shapes.value, [key]: shapeOf(response.data?.columns || []) }
    } catch { /* The diagram and column choices stay empty for an unreadable table. */ }
  }
  const shapeFor = (database: string, table: string) => shapes.value[shapeKey(database, table)] || null

  const selection = useRowSelection(() => keys.value, key => key.name)
  const { selected, allSelected, partialSelected, toggleAll, toggle, select } = selection
  const detail = computed(() => keys.value.find(key => key.name === detailName.value) || null)
  // Rows select like the data preview: a click selects that row, shift extends, cmd/ctrl toggles.
  let anchor: number | null = null
  function onRowClick(key: SourceForeignKey, position: number, event: MouseEvent) {
    detailName.value = key.name
    if (event.shiftKey && anchor !== null) {
      const [from, to] = anchor < position ? [anchor, position] : [position, anchor]
      select(keys.value.slice(from, to + 1).map(item => item.name))
      return
    }
    anchor = position
    if (event.metaKey || event.ctrlKey) toggle(key.name, !selected.value.has(key.name))
    else select([key.name])
  }
  function onCheck(key: SourceForeignKey, position: number, checked: boolean) {
    anchor = position
    toggle(key.name, checked)
  }

  async function load() {
    const request = ++revision
    const [source, database, table] = location()
    if (!can('foreign-key:read') || !source || !database || !table) { keys.value = []; return }
    loading.value = true
    try {
      const response = await MetadataService.getForeignKeyDetails(source, database, table)
      if (request !== revision || !alive) return
      assertSuccess(response)
      keys.value = groupForeignKeys(response.data?.columns || [])
      if (!keys.value.some(key => key.name === detailName.value)) detailName.value = keys.value[0]?.name || null
      select([...selected.value].filter(name => keys.value.some(key => key.name === name)))
    } catch (error) {
      if (request === revision && alive) { keys.value = []; MfMessage.error(errorMessage(error)) }
    } finally {
      if (request === revision && alive) loading.value = false
    }
  }
  async function loadTables() {
    const [source, database] = location()
    try {
      const response = await MetadataService.getTablesByDatabase(source, database)
      assertSuccess(response)
      if (alive) tables.value = [...new Set<string>((response.data?.columns || []).map((row: any) => String(row.object_name ?? '')).filter(Boolean))].sort()
    } catch { tables.value = [] }
  }
  /** Reloads this tab and the workspace, whose foreign key rows feed the structure page. */
  async function reload() {
    if (refreshKey) refreshKey.value++
    await load()
  }

  watch(detail, async key => {
    detailSql.value = ''
    if (!key) return
    loadShape(key.referencedDatabase, key.referencedTable)
    if (!can('foreign-key:create')) return
    const scope = identity()
    const [source, database, table] = location()
    try {
      const response = await MetadataService.createForeignKey(source, database, table, { name: table, preview: true, foreignKeys: [formToForeignKey(foreignKeyToForm(key))] })
      if (alive && identity() === scope && detailName.value === key.name && response.status) detailSql.value = response.data?.content || ''
    } catch { /* The statement is informative only. */ }
  })

  function openEditor(mode: 'add' | 'edit', key: SourceForeignKey | null = null) {
    if (saving.value || !can('foreign-key:create') || (mode === 'edit' && !can('foreign-key:drop'))) return
    editorMode.value = mode
    form.value = key ? foreignKeyToForm(key) : emptyForeignKeyForm(location()[1])
    issues.value = {}
    previewSql.value = ''
    previewError.value = ''
    if (!tables.value.length) loadTables()
    editor.open(key)
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  const editorShape = computed(() => shapeFor(form.value.referencedDatabase, form.value.referencedTable))
  watch(() => [form.value.referencedDatabase, form.value.referencedTable], () => loadShape(form.value.referencedDatabase, form.value.referencedTable))
  function sendDefinition(preview: boolean) {
    const [source, database, table] = location()
    const definition = formToForeignKey(form.value, editorMode.value === 'edit' ? editor.target.value?.name : undefined)
    const request = { name: table, preview, foreignKeys: [definition] }
    return editorMode.value === 'edit'
      ? MetadataService.alterForeignKey(source, database, table, request)
      : MetadataService.createForeignKey(source, database, table, request)
  }
  // "Generated SQL" comes from the server in preview mode, so it is exactly what will run.
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  watch(form, () => {
    if (!editor.visible.value) return
    clearTimeout(previewTimer)
    previewTimer = setTimeout(async () => {
      const request = ++previewRevision
      const problems = validateForeignKeyForm(form.value, keys.value.map(key => key.name), editor.target.value?.name)
      if (Object.keys(problems).length) { previewSql.value = ''; previewError.value = ''; return }
      try {
        const response = await sendDefinition(true)
        if (request !== previewRevision || !alive) return
        assertSuccess(response)
        previewSql.value = response.data?.content || ''
        previewError.value = ''
      } catch (error) {
        if (request === previewRevision && alive) { previewSql.value = ''; previewError.value = errorMessage(error) }
      }
    }, 300)
  }, { deep: true })

  /** Runs one foreign key change, records its SQL and reloads on success. */
  async function mutate(request: () => Promise<any>, operation: string, target: string, onSuccess: () => void) {
    const scope = identity()
    const recorder = executedSql.record({ operation: t(operation), table: String(route.params.table), target: { label: t('source.executed.foreignKey'), value: target } })
    saving.value = true
    try {
      assertSuccess(await recorder.run(request()))
      if (!alive || identity() !== scope) return
      MfMessage.success(t('source.fk.saved'))
      onSuccess()
      await reload()
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === scope) MfMessage.error(errorMessage(error))
    } finally {
      saving.value = false
    }
  }
  async function submitEditor() {
    if (saving.value) return
    issues.value = validateForeignKeyForm(form.value, keys.value.map(key => key.name), editorMode.value === 'edit' ? editor.target.value?.name : undefined)
    if (Object.keys(issues.value).length) return
    const name = form.value.name.trim()
    await mutate(() => sendDefinition(false), editorMode.value === 'edit' ? 'source.executed.editForeignKey' : 'source.executed.createForeignKey', name, () => {
      closeEditor(true)
      detailName.value = name
    })
  }
  async function rebuild(key: SourceForeignKey) {
    if (saving.value || !can('foreign-key:create') || !can('foreign-key:drop')) return
    const [source, database, table] = location()
    await mutate(() => MetadataService.alterForeignKey(source, database, table, { name: table, foreignKeys: [formToForeignKey(foreignKeyToForm(key), key.name)] }),
      'source.executed.rebuildForeignKey', key.name, () => {})
  }

  const requestDrop = (targets: SourceForeignKey[]) => { if (targets.length && can('foreign-key:drop') && !saving.value) dropTargets.value = targets }
  const closeDrop = () => { if (!saving.value) dropTargets.value = [] }
  async function confirmDrop() {
    const targets = dropTargets.value
    if (!targets.length || saving.value) return
    const [source, database, table] = location()
    await mutate(() => MetadataService.dropForeignKey(source, database, table, { name: table, foreignKeys: targets.map(key => ({ name: key.name })) }),
      'source.executed.dropForeignKey', targets.map(key => key.name).join(', '), () => {
        dropTargets.value = []
        select([])
      })
  }

  watch(editor.visible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(identity, () => {
    closeEditor(true)
    dropTargets.value = []
    detailName.value = null
    tables.value = []
    select([])
    load()
  }, { immediate: true })
  // The header's refresh reloads this tab's own rows too.
  const manualRefresh = inject<Ref<number> | null>('metadataManualRefresh', null)
  if (manualRefresh) watch(manualRefresh, () => { load() })
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; previewRevision++; clearTimeout(previewTimer); fieldPanelOpen.value = false })

  return {
    can, loading, saving, keys, tables, localShape, shapeFor, editorShape, selected, allSelected, partialSelected, toggleAll, onRowClick, onCheck,
    detail, detailName, detailSql, editorVisible: editor.visible, editorMode, form, issues, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
    openEditor, closeEditor, submitEditor, rebuild, dropTargets, requestDrop, closeDrop, confirmDrop, reload,
    executed: executedSql.executed, executedVisible: executedSql.visible,
  }
}
