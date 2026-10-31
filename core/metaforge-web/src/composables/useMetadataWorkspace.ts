import { computed, onScopeDispose, provide, reactive, ref, shallowReactive, watch } from 'vue'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import MetadataService from '@/services/metadata'
import { useSourceWorkspaceLayout } from './useSourceWorkspaceLayout'
import { useNativeNamespaces } from './useNativeNamespaces'
import { useSourceCapabilities } from './useSourceCapabilities'
import type { WorkspaceLoaders } from './useWorkspaceLoading'

/** Data-source workspace context; route changes invalidate in-flight metadata requests. */
export function useMetadataWorkspace() {
  const route = useRoute()
  const tableInfo = ref<any | null>(null)
  const indexes = ref<any[]>([])
  const foreignKeys = ref<any[]>([])
  const columnRows = ref<any[]>([])
  const refreshKey = ref(0)
  const loading = ref(false)
  const error = ref('')
  const fieldPanelOpen = ref(false)
  const fieldPanelHost = ref<HTMLElement | null>(null)
  const layout = useSourceWorkspaceLayout(fieldPanelOpen)
  const sourceId = computed(() => String(route.params.source || ''))
  const capabilityState = useSourceCapabilities(sourceId)
  const { profile, can, capabilityLoading, capabilityError, ensureCapabilities } = capabilityState
  const nativeNamespaces = useNativeNamespaces(sourceId, profile, can, refreshKey)
  provide('nativeNamespaceState', nativeNamespaces)
  provide('metadataCan', can)
  provide('metadataCapabilityState', capabilityState)
  let revision = 0
  const identity = computed(() => [route.params.source, route.params.database, route.params.table].join('/'))

  provide('metadataTableInfo', tableInfo)
  provide('metadataTableStats', { indexes, foreignKeys })
  provide('metadataColumnRows', columnRows)
  provide('metadataRefreshKey', refreshKey)
  // Bumped only by the header's refresh: panes holding their own data reload on it.
  provide('metadataManualRefresh', ref(0))
  provide('metadataLoading', loading)
  // Panes register their own loading flags; the header's refresh shows loading until all are done.
  const paneLoaders: WorkspaceLoaders = shallowReactive(new Set())
  provide('metadataPaneLoaders', paneLoaders)
  provide('metadataRefreshing', computed(() => loading.value || [...paneLoaders].some(flag => flag.value)))
  provide('metadataError', error)
  provide('metadataFieldPanelOpen', fieldPanelOpen)
  provide('metadataFieldPanelHost', fieldPanelHost)
  provide('metadataFieldPanelWidth', layout.rightWidth)
  provide('metadataWorkspaceLayout', layout)
  // The header's "edit table properties" asks the properties tab to open its editor.
  provide('metadataTablePropertiesEdit', reactive({ request: 0, handled: 0 }))
  // The header's "edit database properties" asks the database overview to open its editor.
  provide('metadataDatabasePropertiesEdit', reactive({ request: 0, handled: 0 }))

  async function load() {
    const request = ++revision
    const source = String(route.params.source || '')
    const database = String(route.params.database || '')
    const table = String(route.params.table || '')
    error.value = ''
    await ensureCapabilities()
    if (request !== revision) return
    if (capabilityError.value) { error.value = capabilityError.value; loading.value = false; return }
    if (!(source && database && table)) {
      tableInfo.value = null
      indexes.value = []
      foreignKeys.value = []
      columnRows.value = []
      loading.value = false
      return
    }
    loading.value = true
    const results = await Promise.allSettled([
      can('table:info') ? MetadataService.getTable(source, database, table) : Promise.resolve(null),
      can('index:read') ? MetadataService.getIndexes(source, database, table) : Promise.resolve(null),
      can('foreign-key:read') ? MetadataService.getForeignKeys(source, database, table) : Promise.resolve(null),
      can('column:read') ? MetadataService.getColumnsByTable(source, database, table) : Promise.resolve(null),
    ])
    if (request !== revision) return
    const [tableResult, indexResult, foreignResult, columnResult] = results
    if (tableResult.status === 'fulfilled' && tableResult.value?.status && tableResult.value?.data?.isSuccessful) {
      tableInfo.value = tableResult.value?.data.columns?.[0] || null
    } else if (can('table:info')) {
      error.value = tableResult.status === 'fulfilled' ? tableResult.value?.data?.message || tableResult.value?.message : String(tableResult.reason)
    }
    indexes.value = indexResult.status === 'fulfilled' && indexResult.value?.status && indexResult.value?.data?.isSuccessful
        ? indexResult.value?.data.columns || [] : []
    foreignKeys.value = foreignResult.status === 'fulfilled' && foreignResult.value?.status && foreignResult.value?.data?.isSuccessful
        ? foreignResult.value?.data.columns || [] : []
    if (columnResult.status === 'fulfilled' && columnResult.value?.status && columnResult.value?.data?.isSuccessful) {
      columnRows.value = columnResult.value?.data.columns || []
    } else {
      columnRows.value = []
      if (can('column:read')) error.value ||= columnResult.status === 'fulfilled' ? columnResult.value?.data?.message || columnResult.value?.message : String(columnResult.reason)
    }
    loading.value = false
  }

  watch(identity, () => {
    tableInfo.value = null
    indexes.value = []
    foreignKeys.value = []
    columnRows.value = []
    fieldPanelOpen.value = false
    load()
  }, { immediate: true })
  watch(refreshKey, load)
  onScopeDispose(() => { revision++ })
  return { fieldPanelHost, fieldPanelOpen, profile, can, capabilityLoading, capabilityError, ...layout }
}
