import { computed, inject, onScopeDispose, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { copyName, emptyTriggerForm, formToTrigger, mapTriggers, triggerToForm, validateTriggerForm,
  type SourceTrigger, type TriggerForm, type TriggerIssue } from '@/model/source-trigger'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { useRowSelection } from './useRowSelection'
import { usePanelState } from './usePanelState'
import { useExecutedSql } from './useExecutedSql'

/**
 * Trigger tab of a table's workbench, built like the index and foreign key tabs: list and
 * details, a docked editor whose SQL is previewed by the server, copy as a new trigger, rebuild
 * and drop. Every change is recorded for the executed SQL dialog.
 */
export function useSourceTableTriggers() {
  const { t } = useI18n()
  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const loading = ref(false)
  const saving = ref(false)
  const triggers = ref<SourceTrigger[]>([])
  const detailName = ref<string | null>(null)
  const detailSql = ref('')
  const createSqlVisible = ref(false)
  const editor = usePanelState<SourceTrigger>()
  const editorMode = ref<'add' | 'edit'>('add')
  const form = ref<TriggerForm>(emptyTriggerForm(''))
  const issues = ref<Partial<Record<'name' | 'body', TriggerIssue>>>({})
  const previewSql = ref('')
  const previewError = ref('')
  const dropTargets = ref<SourceTrigger[]>([])
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
  const editable = computed(() => can('trigger:create') && can('trigger:drop'))
  const selection = useRowSelection(() => triggers.value, trigger => trigger.name)
  const { selected, allSelected, partialSelected, toggleAll, toggle, select } = selection
  const detail = computed(() => triggers.value.find(trigger => trigger.name === detailName.value) || null)
  // Rows select like the data preview: a click selects that row, shift extends, cmd/ctrl toggles.
  let anchor: number | null = null
  function onRowClick(trigger: SourceTrigger, position: number, event: MouseEvent) {
    detailName.value = trigger.name
    if (event.shiftKey && anchor !== null) {
      const [from, to] = anchor < position ? [anchor, position] : [position, anchor]
      select(triggers.value.slice(from, to + 1).map(item => item.name))
      return
    }
    anchor = position
    if (event.metaKey || event.ctrlKey) toggle(trigger.name, !selected.value.has(trigger.name))
    else select([trigger.name])
  }
  function onCheck(trigger: SourceTrigger, position: number, checked: boolean) {
    anchor = position
    toggle(trigger.name, checked)
  }

  async function load() {
    const request = ++revision
    const [source, database, table] = location()
    if (!can('trigger:read') || !source || !database || !table) { triggers.value = []; return }
    loading.value = true
    try {
      const response = await MetadataService.getTriggerDetails(source, database, table)
      if (request !== revision || !alive) return
      assertSuccess(response)
      triggers.value = mapTriggers(response.data?.columns || [])
      if (!triggers.value.some(trigger => trigger.name === detailName.value)) detailName.value = triggers.value[0]?.name || null
      select([...selected.value].filter(name => triggers.value.some(trigger => trigger.name === name)))
    } catch (error) {
      if (request === revision && alive) { triggers.value = []; MfMessage.error(errorMessage(error)) }
    } finally {
      if (request === revision && alive) loading.value = false
    }
  }
  /** Reloads this tab and the workspace, whose trigger rows feed the structure page. */
  async function reload() {
    if (refreshKey) refreshKey.value++
    await load()
  }

  // The selected trigger's full statement, as the server would create it.
  watch(detail, async trigger => {
    detailSql.value = ''
    if (!trigger || !can('trigger:create')) return
    const scope = identity()
    const [source, database, table] = location()
    try {
      const response = await MetadataService.createTrigger(source, database, table, { name: table, preview: true, triggers: [formToTrigger(triggerToForm(trigger))] })
      if (alive && identity() === scope && detailName.value === trigger.name && response.status) detailSql.value = response.data?.content || ''
    } catch { /* The statement is informative only. */ }
  })

  /** Opens the editor for a new trigger, one to edit, or a copy of one under a new name. */
  function openEditor(mode: 'add' | 'edit' | 'copy', trigger: SourceTrigger | null = null) {
    if (saving.value || !can('trigger:create') || (mode === 'edit' && !can('trigger:drop'))) return
    editorMode.value = mode === 'edit' ? 'edit' : 'add'
    form.value = !trigger ? emptyTriggerForm(location()[2])
      : mode === 'copy' ? { ...triggerToForm(trigger), name: copyName(trigger.name, triggers.value.map(item => item.name)) } : triggerToForm(trigger)
    issues.value = {}
    previewSql.value = ''
    previewError.value = ''
    editor.open(mode === 'edit' ? trigger : null)
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  function sendDefinition(preview: boolean) {
    const [source, database, table] = location()
    const definition = formToTrigger(form.value, editorMode.value === 'edit' ? editor.target.value?.name : undefined)
    const request = { name: table, preview, triggers: [definition] }
    return editorMode.value === 'edit'
      ? MetadataService.alterTrigger(source, database, table, request)
      : MetadataService.createTrigger(source, database, table, request)
  }
  // "Generated SQL" comes from the server in preview mode, so it is exactly what will run.
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  watch(form, () => {
    if (!editor.visible.value) return
    clearTimeout(previewTimer)
    previewTimer = setTimeout(async () => {
      const request = ++previewRevision
      const problems = validateTriggerForm(form.value, triggers.value.map(trigger => trigger.name), editor.target.value?.name)
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
  }, { deep: true, immediate: false })
  // The first preview appears as soon as the editor opens.
  watch(editor.visible, visible => { if (visible) form.value = { ...form.value } })

  /** Runs one trigger change, records its SQL and reloads on success. */
  async function mutate(request: () => Promise<any>, operation: string, target: string, onSuccess: () => void) {
    const scope = identity()
    const recorder = executedSql.record({ operation: t(operation), table: String(route.params.table), target: { label: t('source.executed.trigger'), value: target } })
    saving.value = true
    try {
      assertSuccess(await recorder.run(request()))
      if (!alive || identity() !== scope) return
      MfMessage.success(t('source.trg.saved'))
      onSuccess()
      await reload()
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === scope) MfMessage.error(errorMessage(error))
      // A failed edit may have restored the original trigger; show what the database has now.
      if (alive && identity() === scope) await load()
    } finally {
      saving.value = false
    }
  }
  async function submitEditor() {
    if (saving.value) return
    issues.value = validateTriggerForm(form.value, triggers.value.map(trigger => trigger.name), editorMode.value === 'edit' ? editor.target.value?.name : undefined)
    if (Object.keys(issues.value).length) return
    const name = form.value.name.trim()
    await mutate(() => sendDefinition(false), editorMode.value === 'edit' ? 'source.executed.editTrigger' : 'source.executed.createTrigger', name, () => {
      closeEditor(true)
      detailName.value = name
    })
  }
  async function rebuild(trigger: SourceTrigger) {
    if (saving.value || !editable.value) return
    const [source, database, table] = location()
    await mutate(() => MetadataService.alterTrigger(source, database, table, { name: table, triggers: [formToTrigger(triggerToForm(trigger), trigger.name)] }),
      'source.executed.rebuildTrigger', trigger.name, () => {})
  }

  const requestDrop = (targets: SourceTrigger[]) => { if (targets.length && can('trigger:drop') && !saving.value) dropTargets.value = targets }
  const closeDrop = () => { if (!saving.value) dropTargets.value = [] }
  async function confirmDrop() {
    const targets = dropTargets.value
    if (!targets.length || saving.value) return
    const [source, database, table] = location()
    await mutate(() => MetadataService.dropTrigger(source, database, table, { name: table, triggers: targets.map(trigger => ({ name: trigger.name })) }),
      'source.executed.dropTrigger', targets.map(trigger => trigger.name).join(', '), () => {
        dropTargets.value = []
        select([])
      })
  }

  watch(editor.visible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(identity, () => {
    closeEditor(true)
    dropTargets.value = []
    detailName.value = null
    createSqlVisible.value = false
    select([])
    load()
  }, { immediate: true })
  // The header's refresh reloads this tab's own rows too.
  const manualRefresh = inject<Ref<number> | null>('metadataManualRefresh', null)
  if (manualRefresh) watch(manualRefresh, () => { load() })
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; previewRevision++; clearTimeout(previewTimer); fieldPanelOpen.value = false })

  return {
    can, editable, loading, saving, triggers, selected, allSelected, partialSelected, toggleAll, onRowClick, onCheck, detail, detailName, detailSql,
    createSqlVisible, editorVisible: editor.visible, editorMode, form, issues, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
    openEditor, closeEditor, submitEditor, rebuild, dropTargets, requestDrop, closeDrop, confirmDrop, reload,
    executed: executedSql.executed, executedVisible: executedSql.visible,
  }
}
