import { computed, inject, onScopeDispose, reactive, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import SourceService from '@/services/source'
import { sourceManageLocation } from '@/model/source-manage'
import { mapDatabaseOverview, mapDatabaseTables, spaceSegments, type DatabaseOverview, type DatabaseTable } from '@/model/source-database'
import { defaultCollation, mapCollations, type Collation } from '@/model/source-table-properties'
import { useWorkspaceLoading } from './useWorkspaceLoading'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'
import { usePanelState } from './usePanelState'

/** Asks the database overview to open its properties editor, from the workspace header. */
export interface DatabasePropertiesEditRequest { request: number; handled: number }

/**
 * Database overview: statistics, space use, object counts and the tables, loaded together, plus a
 * docked editor for the default character set and collation whose statement the server previews.
 */
export function useSourceDatabase() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)
  const refreshKey = inject<Ref<number> | null>('metadataRefreshKey', null)
  const editRequest = inject<DatabasePropertiesEditRequest>('metadataDatabasePropertiesEdit', reactive({ request: 0, handled: 0 }))
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const loading = ref(false)
  const overview = ref<DatabaseOverview | null>(null)
  const tables = ref<DatabaseTable[]>([])
  const sourceType = ref('')
  const showAllTables = ref(false)
  const tableCreateVisible = ref(false)
  const editor = usePanelState()
  const saving = ref(false)
  const collations = ref<Collation[]>([])
  const form = ref({ charset: '', collation: '' })
  const previewSql = ref('')
  const previewError = ref('')
  let revision = 0
  let previewRevision = 0
  let alive = true

  const location = () => [String(route.params.source || ''), String(route.params.database || '')] as const
  const identity = () => location().join('/')
  const failure = (response: any) => !response?.status || response.data?.isSuccessful === false
  const message = (response: any) => response?.data?.message || response?.message || t('common.loadingFailed')
  const segments = computed(() => overview.value ? spaceSegments(overview.value) : [])
  const totalBytes = computed(() => segments.value.reduce((sum, segment) => sum + segment.bytes, 0))
  const visibleTables = computed(() => showAllTables.value ? tables.value : tables.value.slice(0, 5))
  const editable = computed(() => can('database:alter'))
  const charsets = computed(() => [...new Set(collations.value.map(item => item.charset))])
  const charsetCollations = computed(() => collations.value.filter(item => item.charset === form.value.charset).map(item => item.collation))
  const changes = computed(() => {
    const current = overview.value
    if (!current) return {}
    const options: Record<string, string> = {}
    if (form.value.charset && form.value.charset !== current.charset) options.charset = form.value.charset
    if (form.value.collation && form.value.collation !== current.collation) options.collate = form.value.collation
    return options
  })

  async function load() {
    const request = ++revision
    const [source, database] = location()
    if (!source || !database || !can('database:info')) { overview.value = null; tables.value = []; return }
    loading.value = true
    const [detail, list, info] = await Promise.all([
      MetadataService.getDatabase(source, database),
      MetadataService.getDatabaseTables(source, database),
      SourceService.getByCode(source),
    ])
    if (request !== revision || !alive) return
    if (failure(detail)) { overview.value = null; MfMessage.error(message(detail)) }
    else overview.value = mapDatabaseOverview(detail.data?.columns?.[0], database)
    tables.value = failure(list) ? [] : mapDatabaseTables(list.data?.columns || [])
    sourceType.value = info?.status ? String(info.data?.type || '') : ''
    loading.value = false
  }

  const openTable = (table: string) => {
    const [source, database] = location()
    router.push(sourceManageLocation({ source, database, table, tab: 'structure' }))
  }
  function quick(action: 'create' | 'sql' | 'export' | 'import') {
    const [source, database] = location()
    if (action === 'create') tableCreateVisible.value = true
    else if (action === 'sql') router.push(sourceManageLocation({ source, database, tab: 'sql' }))
    else router.push({ path: action === 'export' ? '/export' : '/import', query: { source, database } })
  }
  function onTableCreated() {
    tableCreateVisible.value = false
    if (refreshKey) refreshKey.value++
  }

  async function openEditor() {
    const current = overview.value
    if (!current || !editable.value || saving.value) return
    form.value = { charset: current.charset, collation: current.collation }
    previewSql.value = ''
    previewError.value = ''
    editor.open()
    if (!collations.value.length) {
      const response = await MetadataService.getCharacterSets(location()[0])
      if (alive && !failure(response)) collations.value = mapCollations(response.data?.columns || [])
    }
  }
  const closeEditor = (force = false) => { if (!saving.value || force) editor.close() }
  /** A new character set brings its default collation unless the current one belongs to it. */
  function onCharset(charset: string) {
    form.value.charset = charset
    if (!collations.value.some(item => item.charset === charset && item.collation === form.value.collation)) form.value.collation = defaultCollation(collations.value, charset)
  }
  let previewTimer: ReturnType<typeof setTimeout> | undefined
  watch(form, () => {
    if (!editor.visible.value) return
    clearTimeout(previewTimer)
    previewTimer = setTimeout(async () => {
      const request = ++previewRevision
      if (!Object.keys(changes.value).length) { previewSql.value = ''; previewError.value = ''; return }
      const [source, database] = location()
      const response = await MetadataService.alterDatabaseOptions(source, database, { preview: true, options: changes.value })
      if (request !== previewRevision || !alive) return
      previewSql.value = failure(response) ? '' : response.data?.content || ''
      previewError.value = failure(response) ? message(response) : ''
    }, 300)
  }, { deep: true })
  async function submitEditor() {
    if (saving.value) return
    if (!Object.keys(changes.value).length) { closeEditor(); return }
    const scope = identity()
    const [source, database] = location()
    saving.value = true
    try {
      const response = await MetadataService.alterDatabaseOptions(source, database, { options: changes.value })
      if (!alive || identity() !== scope) return
      if (failure(response)) { MfMessage.error(message(response)); return }
      MfMessage.success(t('source.database.saved'))
      closeEditor(true)
      await load()
    } finally {
      saving.value = false
    }
  }

  // The workspace header's "edit database properties" opens the editor once the database is loaded.
  watch(() => [editRequest.request, overview.value] as const, () => {
    if (editRequest.request > editRequest.handled && overview.value) {
      editRequest.handled = editRequest.request
      openEditor()
    }
  }, { immediate: true })
  watch(editor.visible, value => { fieldPanelOpen.value = value }, { immediate: true })
  watch(identity, () => { closeEditor(true); showAllTables.value = false; load() }, { immediate: true })
  if (refreshKey) watch(refreshKey, load)
  useWorkspaceLoading(loading)
  onScopeDispose(() => { alive = false; revision++; previewRevision++; clearTimeout(previewTimer); fieldPanelOpen.value = false })

  return {
    can, loading, overview, tables, visibleTables, showAllTables, sourceType, segments, totalBytes, tableCreateVisible, openTable, quick, onTableCreated,
    editable, editorVisible: editor.visible, saving, form, charsets, charsetCollations, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
    closeEditor, onCharset, submitEditor,
  }
}
