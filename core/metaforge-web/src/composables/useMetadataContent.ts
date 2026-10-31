import { computed, defineAsyncComponent, inject, ref, watch, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import { usePanelState } from './usePanelState'
import { sourceManageLocation, sourceTabCapability } from '@/model/source-manage'
import type { useSourceCapabilities } from './useSourceCapabilities'
import type { useSourceWorkspaceLayout } from './useSourceWorkspaceLayout'

export function useMetadataContent() {
  const route = useRoute()
  const capabilityState = inject<ReturnType<typeof useSourceCapabilities>>('metadataCapabilityState')!
  const { can, profile, capabilityLoading, capabilityError } = capabilityState
  const layout = inject<ReturnType<typeof useSourceWorkspaceLayout>>('metadataWorkspaceLayout')!
  const router = useRouter()
  const { t } = useI18n()
  const tableInfo = inject<Ref<any | null>>('metadataTableInfo', ref(null))
  const refreshKey = inject<Ref<number>>('metadataRefreshKey', ref(0))
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const metadataError = inject<Ref<string>>('metadataError', ref(''))
  const originalSource = computed(() => String(route.params.source || ''))
  const originalDatabase = computed(() => String(route.params.database || ''))
  const originalTable = computed(() => String(route.params.table || ''))
  const tableName = computed(() => String(tableInfo.value?.object_name || originalTable.value))
  const selectTab = computed(() => String(route.meta.type || 'structure'))
  const tableCreate = usePanelState()
  const tableExport = usePanelState()
  const tableTruncate = usePanelState()
  const tableDrop = usePanelState()
  const tableCreateVisible = tableCreate.visible
  const tableExportVisible = tableExport.visible
  const tableTruncateVisible = tableTruncate.visible
  const tableDropVisible = tableDrop.visible
  const tabs = computed(() => [
    { key: 'keys', label: t('source.native.keyBrowser') },
    { key: 'objects', label: t('source.native.objectBrowser') },
    { key: 'structure', label: t('source.tab.structure') },
    { key: 'data', label: t('source.tab.dataPreview') },
    { key: 'sql', label: t('source.tab.sqlQuery') },
    { key: 'indexes', label: t('source.tab.indexes') },
    { key: 'foreignkeys', label: t('source.tab.foreignKeys') },
    { key: 'triggers', label: t('source.tab.triggers') },
    { key: 'info', label: t('source.tab.tableOptions') },
    { key: 'statement', label: t('source.tab.ddl') },
    { key: 'erDiagram', label: t('source.tab.erDiagram') },
  ].filter(tab => can(sourceTabCapability(tab.key))))
  const propertiesEdit = inject<{ request: number; handled: number }>('metadataTablePropertiesEdit', { request: 0, handled: 0 })
  /** Opens the properties tab with its editor. */
  const onEditProperties = () => {
    onChange('info')
    propertiesEdit.request++
  }
  const databaseEdit = inject<{ request: number; handled: number }>('metadataDatabasePropertiesEdit', { request: 0, handled: 0 })
  /** The database overview opens its properties editor. */
  const onEditDatabase = () => { databaseEdit.request++ }
  const onChange = (tab: string | number) => {
    if (can(sourceTabCapability(String(tab)))) router.push(sourceManageLocation({ source: originalSource.value, database: originalDatabase.value, table: originalTable.value, tab: String(tab) }, route.query))
  }
  const panes = {
    keys: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceNativeBrowser.vue')),
    objects: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceNativeBrowser.vue')),
    structure: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableStructure.vue')),
    data: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableData.vue')),
    sql: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceSqlPane.vue')),
    indexes: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableIndexes.vue')),
    foreignkeys: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableForeignKeys.vue')),
    triggers: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableTriggers.vue')),
    info: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableInfo.vue')),
    statement: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableStatement.vue')),
    erDiagram: defineAsyncComponent(() => import('@/views/pages/admin/source/SourceTableErDiagram.vue')),
  }
  const databasePane = defineAsyncComponent(() => import('@/views/pages/admin/source/SourceDatabase.vue'))
  const paneComponent = computed(() => !originalTable.value && !['sql','keys','objects'].includes(selectTab.value) ? (can('database:info') ? databasePane : null) : panes[selectTab.value as keyof typeof panes])
  const paneKey = computed(() => [originalSource.value, originalDatabase.value, originalTable.value, selectTab.value].join('/'))
  const databaseLocation = computed(() => sourceManageLocation({ source: originalSource.value, database: originalDatabase.value }))
  watch([profile, selectTab], () => {
    if (profile.value && !can(sourceTabCapability(selectTab.value)) && tabs.value[0]) {
      router.replace(sourceManageLocation({ source: originalSource.value, database: originalDatabase.value, table: originalTable.value, tab: tabs.value[0].key }))
    }
  }, { immediate: true })
  const manualRefresh = inject<Ref<number>>('metadataManualRefresh', ref(0))
  const refreshing = inject<Ref<boolean>>('metadataRefreshing', ref(false))
  const onRefresh = () => {
    if (refreshing.value) return
    refreshKey.value++
    manualRefresh.value++
  }
  const onStructureCompare = () => router.push({ path: '/compare', query: { source: originalSource.value, database: originalDatabase.value, table: originalTable.value } })
  const onCopyName = () => {
    if (tableName.value) {
      ObjectUtils.copy(tableName.value)
      MfMessage.success(t('query.tip.copySuccess'))
    }
  }
  return { can, profile, capabilityLoading, capabilityError, paneComponent, paneKey, databaseLocation, tableInfo, tableName, selectTab, tabs, fieldPanelOpen, fieldPanelHost, metadataError, ...layout,
    originalSource, originalDatabase, originalTable, onChange, onRefresh, refreshing, onStructureCompare, onCopyName, onEditProperties, onEditDatabase,
    tableCreateVisible, tableExportVisible, tableTruncateVisible, tableDropVisible }
}
