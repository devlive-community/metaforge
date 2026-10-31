import { computed, inject, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { mapSourceColumns } from '@/model/source-column'
import { emptyIndexForm, formToDefinition, groupIndexDetails, indexToForm, validateIndexForm, type IndexForm, type IndexIssue, type SourceIndex } from '@/model/source-index'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { useRowSelection } from './useRowSelection'
import { usePanelState } from './usePanelState'
import { useExecutedSql } from './useExecutedSql'

/**
 * Index tab of a table's workbench: the index list and details, a docked editor whose SQL is
 * previewed by the server, rebuild and drop. Every change is recorded for the executed SQL dialog.
 */
export function useSourceTableIndexes() {
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
  const indexes = ref<SourceIndex[]>([])
  const detailName = ref<string | null>(null)
  const detailSql = ref('')
  const editor = usePanelState<SourceIndex>()
  const editorMode = ref<'add' | 'edit'>('add')
  const form = ref<IndexForm>(emptyIndexForm())
  const issues = ref<Partial<Record<'name' | 'columns', IndexIssue>>>({})
  const previewSql = ref('')
  const previewError = ref('')
  const dropTargets = ref<SourceIndex[]>([])
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
  const tableColumns = computed(() => mapSourceColumns(columnRows.value || []).map(column => column.name))
  const selection = useRowSelection(() => indexes.value.filter(index => !index.primary), index => index.name)
  const { selected, allSelected, partialSelected, toggleAll, toggle, select } = selection
  const detail = computed(() => indexes.value.find(index => index.name === detailName.value) || null)
  // Rows select like the data preview: a click selects that row, shift extends from the last one,
  // cmd/ctrl toggles. PRIMARY cannot be dropped, so it only opens its details.
  let anchor: number | null = null
  function onRowClick(index: SourceIndex, position: number, event: MouseEvent) {
    detailName.value = index.name
    const selectable = (item: SourceIndex) => !item.primary
    if (event.shiftKey && anchor !== null) {
      const [from, to] = anchor < position ? [anchor, position] : [position, anchor]
      select(indexes.value.slice(from, to + 1).filter(selectable).map(item => item.name))
      return
    }
    anchor = position
    if (event.metaKey || event.ctrlKey) { if (selectable(index)) toggle(index.name, !selected.value.has(index.name)) }
    else select(selectable(index) ? [index.name] : [])
  }
  function onCheck(index: SourceIndex, position: number, checked: boolean) {
    anchor = position
    toggle(index.name, checked)
  }

  async function load() {
    const request = ++revision
    const [source, database, table] = location()
    if (!can('index:read') || !source || !database || !table) { indexes.value = []; return }
    loading.value = true
    try {
      const response = await MetadataService.getIndexDetails(source, database, table)
      if (request !== revision || !alive) return
      assertSuccess(response)
      indexes.value = groupIndexDetails(response.data?.columns || [])
      if (!indexes.value.some(index => index.name === detailName.value)) detailName.value = indexes.value[0]?.name || null
      select([...selected.value].filter(name => indexes.value.some(index => index.name === name)))
    } catch (error) {
      if (request === revision && alive) { indexes.value = []; MfMessage.error(errorMessage(error)) }
    } finally {
      if (request === revision && alive) loading.value = false
    }
  }
  /** Reloads this tab and the workspace, whose index rows feed the tab count and the structure page. */
  async function reload() {
    if (refreshKey) refreshKey.value++
    await load()
  }

  // The selected index's statement, as the server would create it.
  watch(detail, async index => {
    detailSql.value = ''
    if (!index || !can('index:create')) return
    const key = identity()
    const [source, database, table] = location()
    try {
      const response = await MetadataService.createIndex(source, database, table, { name: table, preview: true, indexes: [formToDefinition(indexToForm(index))] })
      if (alive && identity() === key && detailName.value === index.name && response.status) detailSql.value = response.data?.content || ''
    } catch { /* The statement is informative only. */ }
  })

  function openEditor(mode: 'add' | 'edit', index: SourceIndex | null = null) {
    if (saving.value || !can('index:create') || (mode === 'edit' && !can('index:drop'))) return
    editorMode.value = mode
    form.value = index ? indexToForm(index) : emptyIndexForm()
    issues.value = {}
    previewSql.value = ''
    previewError.value = ''
    editor.open(index)
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  function definitionRequest(preview: boolean) {
    const original = editor.target.value
    const [, , table] = location()
    const definition = formToDefinition(form.value, editorMode.value === 'edit' ? original?.name : undefined)
    return { name: table, preview, indexes: [definition] }
  }
  function sendDefinition(preview: boolean) {
    const [source, database, table] = location()
    return editorMode.value === 'edit'
      ? MetadataService.alterIndex(source, database, table, definitionRequest(preview))
      : MetadataService.createIndex(source, database, table, definitionRequest(preview))
  }
  // "Generated SQL" comes from the server in preview mode, so it is exactly what will run.
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  watch(form, () => {
    if (!editor.visible.value) return
    clearTimeout(previewTimer)
    previewTimer = setTimeout(async () => {
      const request = ++previewRevision
      const problems = validateIndexForm(form.value, indexes.value.map(index => index.name), editor.target.value?.name)
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

  /** Runs one index change, records its SQL and reloads on success. */
  async function mutate(request: () => Promise<any>, operation: string, target: string, onSuccess: () => void) {
    const key = identity()
    const recorder = executedSql.record({ operation: t(operation), table: String(route.params.table), target: { label: t('source.executed.index'), value: target } })
    saving.value = true
    try {
      assertSuccess(await recorder.run(request()))
      if (!alive || identity() !== key) return
      MfMessage.success(t('source.idx.saved'))
      onSuccess()
      await reload()
      return true
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === key) MfMessage.error(errorMessage(error))
      return false
    } finally {
      saving.value = false
    }
  }
  async function submitEditor() {
    if (saving.value) return
    issues.value = validateIndexForm(form.value, indexes.value.map(index => index.name), editorMode.value === 'edit' ? editor.target.value?.name : undefined)
    if (Object.keys(issues.value).length) return
    const name = form.value.name.trim()
    await mutate(() => sendDefinition(false), editorMode.value === 'edit' ? 'source.executed.editIndex' : 'source.executed.createIndex', name, () => {
      closeEditor(true)
      detailName.value = name
    })
  }
  async function rebuild(index: SourceIndex) {
    if (saving.value || !can('index:create') || !can('index:drop')) return
    const [source, database, table] = location()
    await mutate(() => MetadataService.alterIndex(source, database, table, { name: table, indexes: [formToDefinition(indexToForm(index), index.name)] }),
      'source.executed.rebuildIndex', index.name, () => {})
  }

  const requestDrop = (targets: SourceIndex[]) => {
    const droppable = targets.filter(index => !index.primary)
    if (droppable.length && can('index:drop') && !saving.value) dropTargets.value = droppable
  }
  const closeDrop = () => { if (!saving.value) dropTargets.value = [] }
  async function confirmDrop() {
    const targets = dropTargets.value
    if (!targets.length || saving.value) return
    const [source, database, table] = location()
    await mutate(() => MetadataService.dropIndex(source, database, table, { name: table, indexes: targets.map(index => ({ name: index.name })) }),
      'source.executed.dropIndex', targets.map(index => index.name).join(', '), () => {
        dropTargets.value = []
        select([])
      })
  }

  watch(editor.visible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(identity, () => {
    closeEditor(true)
    dropTargets.value = []
    detailName.value = null
    select([])
    load()
  }, { immediate: true })
  // The header's refresh reloads this tab's own rows too.
  const manualRefresh = inject<Ref<number> | null>('metadataManualRefresh', null)
  if (manualRefresh) watch(manualRefresh, () => { load() })
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; previewRevision++; clearTimeout(previewTimer); fieldPanelOpen.value = false })

  return {
    can, loading, saving, indexes, tableColumns, selected, allSelected, partialSelected, toggleAll, onRowClick, onCheck, detail, detailName, detailSql,
    editorVisible: editor.visible, editorMode, form, issues, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
    openEditor, closeEditor, submitEditor, rebuild, dropTargets, requestDrop, closeDrop, confirmDrop, reload,
    executed: executedSql.executed, executedVisible: executedSql.visible,
  }
}
