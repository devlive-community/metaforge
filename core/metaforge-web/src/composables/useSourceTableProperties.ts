import { computed, inject, onScopeDispose, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import { allOptions, changedOptions, defaultCollation, mapCollations, mapTableProperties, propertiesToForm, validatePropertiesForm,
  type Collation, type PropertiesForm, type PropertiesIssue, type TableProperties } from '@/model/source-table-properties'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { usePanelState } from './usePanelState'
import { useExecutedSql } from './useExecutedSql'

/** Asks the properties tab to open its editor, from the workspace header for example. */
export interface PropertiesEditRequest {
  request: number
  handled: number
}

/**
 * Table properties tab: options and statistics, the statement describing them, and a docked
 * editor that changes only the options the user touched, previewed by the server.
 */
export function useSourceTableProperties() {
  const { t } = useI18n()
  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)
  const editRequest = inject<PropertiesEditRequest>('metadataTablePropertiesEdit', reactive({ request: 0, handled: 0 }))
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const loading = ref(false)
  const saving = ref(false)
  const properties = ref<TableProperties | null>(null)
  const currentSql = ref('')
  const engines = ref<string[]>([])
  const collations = ref<Collation[]>([])
  const editor = usePanelState()
  const form = ref<PropertiesForm>({ engine: '', rowFormat: '', charset: '', collation: '', autoIncrement: '', comment: '' })
  const issues = ref<Partial<Record<keyof PropertiesForm, PropertiesIssue>>>({})
  const previewSql = ref('')
  const previewError = ref('')
  const focusComment = ref(false)
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
  const editable = computed(() => can('table:alter'))
  const charsets = computed(() => [...new Set(collations.value.map(item => item.charset))])
  const charsetCollations = computed(() => collations.value.filter(item => item.charset === form.value.charset).map(item => item.collation))

  async function load() {
    const request = ++revision
    const [source, database, table] = location()
    if (!can('table:info') || !source || !database || !table) { properties.value = null; return }
    loading.value = true
    try {
      const response = await MetadataService.getTableProperties(source, database, table)
      if (request !== revision || !alive) return
      assertSuccess(response)
      properties.value = mapTableProperties(response.data?.columns?.[0])
      loadCurrentSql()
    } catch (error) {
      if (request === revision && alive) { properties.value = null; MfMessage.error(errorMessage(error)) }
    } finally {
      if (request === revision && alive) loading.value = false
    }
  }
  /** The table's options as one statement, built by the server in preview mode. */
  async function loadCurrentSql() {
    currentSql.value = ''
    const current = properties.value
    if (!current || !editable.value) return
    const scope = identity()
    const [source, database, table] = location()
    try {
      const response = await MetadataService.alterTableOptions(source, database, table, { name: table, preview: true, options: allOptions(current) })
      if (alive && identity() === scope && response.status && response.data?.isSuccessful !== false) currentSql.value = response.data?.content || ''
    } catch { /* The statement is informative only. */ }
  }
  /** Engines and character sets are only needed once the editor opens. */
  async function loadChoices() {
    const [source] = location()
    if (!engines.value.length) {
      try {
        const response = await MetadataService.getEngines(source)
        assertSuccess(response)
        engines.value = (response.data?.columns || []).map((value: any) => typeof value === 'string' ? value : String(value?.object_name ?? value?.name ?? '')).filter(Boolean)
      } catch { engines.value = [] }
    }
    if (!collations.value.length) {
      try {
        const response = await MetadataService.getCharacterSets(source)
        assertSuccess(response)
        collations.value = mapCollations(response.data?.columns || [])
      } catch { collations.value = [] }
    }
  }

  function openEditor(comment = false) {
    const current = properties.value
    if (saving.value || !editable.value || !current) return
    form.value = propertiesToForm(current)
    issues.value = {}
    previewSql.value = ''
    previewError.value = ''
    focusComment.value = comment
    loadChoices()
    editor.open()
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  /** A new character set brings its default collation unless the current one belongs to it. */
  function onCharset(charset: string) {
    form.value.charset = charset
    if (!collations.value.some(item => item.charset === charset && item.collation === form.value.collation)) form.value.collation = defaultCollation(collations.value, charset)
  }
  const pendingOptions = computed(() => properties.value ? changedOptions(form.value, properties.value) : {})
  // "Generated SQL" covers only the changed options and comes from the server in preview mode.
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  watch(form, () => {
    if (!editor.visible.value) return
    clearTimeout(previewTimer)
    previewTimer = setTimeout(async () => {
      const request = ++previewRevision
      if (Object.keys(validatePropertiesForm(form.value, collations.value)).length || !Object.keys(pendingOptions.value).length) {
        previewSql.value = ''
        previewError.value = ''
        return
      }
      const [source, database, table] = location()
      try {
        const response = await MetadataService.alterTableOptions(source, database, table, { name: table, preview: true, options: pendingOptions.value })
        if (request !== previewRevision || !alive) return
        assertSuccess(response)
        previewSql.value = response.data?.content || ''
        previewError.value = ''
      } catch (error) {
        if (request === previewRevision && alive) { previewSql.value = ''; previewError.value = errorMessage(error) }
      }
    }, 300)
  }, { deep: true })

  async function submitEditor() {
    if (saving.value) return
    issues.value = validatePropertiesForm(form.value, collations.value)
    if (Object.keys(issues.value).length) return
    const options = pendingOptions.value
    if (!Object.keys(options).length) { closeEditor(); return }
    const scope = identity()
    const [source, database, table] = location()
    const recorder = executedSql.record({ operation: t('source.executed.alterTable'), table, target: { label: t('source.executed.options'), value: Object.keys(options).join(', ') } })
    saving.value = true
    try {
      assertSuccess(await recorder.run(MetadataService.alterTableOptions(source, database, table, { name: table, options })))
      if (!alive || identity() !== scope) return
      MfMessage.success(t('source.props.saved'))
      closeEditor(true)
      // The workspace header shows the engine, charset and comment too.
      if (refreshKey) refreshKey.value++
      await load()
    } catch (error) {
      recorder.fail(error)
      if (alive && identity() === scope) MfMessage.error(errorMessage(error))
    } finally {
      saving.value = false
    }
  }

  // Requests from outside the tab (the workspace header) open the editor once the table is loaded.
  watch(() => [editRequest.request, properties.value] as const, () => {
    if (editRequest.request > editRequest.handled && properties.value) {
      editRequest.handled = editRequest.request
      openEditor()
    }
  }, { immediate: true })
  watch(editor.visible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(identity, () => { closeEditor(true); load() }, { immediate: true })
  // The header's refresh reloads this tab's own rows too.
  const manualRefresh = inject<Ref<number> | null>('metadataManualRefresh', null)
  if (manualRefresh) watch(manualRefresh, () => { load() })
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; previewRevision++; clearTimeout(previewTimer); fieldPanelOpen.value = false })

  return {
    can, editable, loading, saving, properties, currentSql, engines, charsets, charsetCollations, editorVisible: editor.visible, form, issues,
    previewSql, previewError, focusComment, fieldPanelHost, fieldPanelWidth, openEditor, closeEditor, onCharset, submitEditor, reload: load,
    executed: executedSql.executed, executedVisible: executedSql.visible,
  }
}
