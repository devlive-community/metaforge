import { computed, inject, onScopeDispose, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { useI18n } from 'vue-i18n'
import { MfMessage, useDismissableLayer } from 'metaforge-ui'
import MetadataService from '@/services/metadata.ts'
import { sourceManageLocation } from '@/model/source-manage'
import { StructureEnum, StructureModel } from '@/model/structure.ts'

export function useMetadataSidebar() {
  interface MenuItem
  {
    type: string
    title: string
    comment?: string
    isLeaf?: boolean
    level?: StructureEnum
    code?: string
    children?: MenuItem[]
    value?: string
    key?: string
    dataType?: string
    nullable?: string
    defaultValue?: string
    position?: number
    definition?: string
    typeName?: string
    disabled?: boolean
  }

  interface SourceData
  {
    type_name: string
    object_name: string
    object_comment: string
    object_data_type: string
    object_nullable: string
    object_default_value: string
    object_position: number
    object_definition: string
  }

  const route = useRoute()
  const can = inject<(capability: string) => boolean>('metadataCan', () => false)

  const router = useRouter()

  const { t } = useI18n()

  const loading = ref(false)

  const selectDatabase = ref<any>(undefined)

  const selectedKeys = ref<any[]>([])

  const originalSource = ref<string | null>(null)



  const dataTreeArray = ref<any[]>([])

  const dataInfo = ref<StructureModel | null>(null)

  const tableCreateVisible = ref(false)

  const tableExportVisible = ref(false)

  const tableTruncateVisible = ref(false)

  const tableDropVisible = ref(false)

  const columnCreateVisible = ref(false)

  const columnChangeVisible = ref(false)

  const columnDropVisible = ref(false)

  const contextMenuRoot = ref<HTMLElement>()

  const contextmenu = reactive({
    visible: false,
    position: { x: 0, y: 0 }
  })



  const tableInfo = inject<import('vue').Ref<any | null>>('metadataTableInfo', ref(null))

  const tableEngine = computed(() => String(tableInfo.value?.object_engine || route.params.database || ''))

  const searchKeyword = ref('')

  const tableCount = computed(() => {
    const group = dataTreeArray.value.find((group: any) => group.type === 'table')
    return group?.children?.length || 0
  })

  const filteredTreeArray = computed(() => {
    const keyword = searchKeyword.value.trim().toLowerCase()
    if (!keyword) return dataTreeArray.value
    return dataTreeArray.value
        .map((group: any) => ({
          ...group,
          children: (group.children || []).filter((child: any) => String(child.title || '').toLowerCase().includes(keyword))
        }))
        .filter((group: any) => (group.children || []).length > 0)
  })

  const convertToTreeData = (flatData: SourceData[], level: StructureEnum = StructureEnum.DATABASE): MenuItem[] => {
    const groupedData = flatData.reduce((acc, curr) => {
      if (!acc[curr.type_name]) {
        acc[curr.type_name] = []
      }
      acc[curr.type_name].push(curr)
      return acc
    }, {} as Record<string, SourceData[]>)

    return Object.entries(groupedData).map(([type, items]) => ({
      type,
      title: `${ t('common.' + type) } (${ items.length })`,
      level: StructureEnum.TYPE,
      value: type,
      key: type,
      children: items.map(item => ({
        type: item.object_data_type || item.type_name || '',
        title: item.object_name,
        level: level,
        isLeaf: level === StructureEnum.TABLE,
        code: item.object_name,
        value: `${ item.object_name }_${ item.type_name }`,
        key: `${ item.object_name }_${ item.type_name }`,
        comment: item.object_comment,
        dataType: item.object_data_type,
        nullable: item.object_nullable,
        defaultValue: item.object_default_value,
        position: item.object_position,
        definition: item.object_definition,
        typeName: item.type_name
      }))
    }))
  }

  const updateTreeChildren = (list: any[], key: string, children: any[]): any[] =>
    list.map((node) => {
      if (node.value === key) {
        return { ...node, children }
      }
      if (node.children) {
        return { ...node, children: updateTreeChildren(node.children, key, children) }
      }
      return node
    })

  let tableRevision = 0
  const onChangeDatabase = async () => {
    const request = ++tableRevision
    const source = String(route.params.source || '')
    const database = String(route.params.database || selectDatabase.value || '')
    originalSource.value = source
    selectDatabase.value = database
    dataTreeArray.value = []
    if (!(source && database) || !can('table:list')) { loading.value = false; return }
    loading.value = true
    try {
      const response = await MetadataService.getTablesByDatabase(source, database)
      if (request !== tableRevision) return
      if (response.status && response.data?.isSuccessful) {
        dataTreeArray.value = convertToTreeData(response.data.columns || [], StructureEnum.TABLE)
      } else {
        MfMessage.error(response.data?.message || response.message)
      }
    } finally {
      if (request === tableRevision) loading.value = false
    }
  }

  const onSelect = (_keys: any[], info: any) => {
    const node = info.node
    if (node.level !== StructureEnum.TABLE) return
    const tab = String(route.meta.type || 'structure')
    router.push(sourceManageLocation({ source: String(route.params.source), database: String(route.params.database), table: String(node.code), tab }))
  }

  const onLoadData = (treeNode: any): Promise<void> => {
    const item = treeNode as any
    return new Promise<void>((resolve) => {
      if (item.children && item.children.length > 0 && item.level === StructureEnum.COLUMN) {
        resolve()
        return
      }
      if (item.level === StructureEnum.COLUMN) {
        resolve()
        return
      }

      MetadataService.getColumnsByTable(originalSource.value, selectDatabase.value as string, item.code as string)
                     .then((response) => {
                       if (response.status) {
                         const children = [...convertToTreeData(response.data.columns, StructureEnum.COLUMN)]
                         dataTreeArray.value = updateTreeChildren(dataTreeArray.value, item.value, children)
                       }
                       else {
                         MfMessage.error(response.message)
                       }
                     })
                     .finally(() => resolve())
    })
  }

  const visibleContextMenu = async (event: MouseEvent, node: MenuItem) => {
    if (node.level === StructureEnum.TABLE && node.code !== route.params.table) {
      await router.push(sourceManageLocation({ source: String(route.params.source), database: String(route.params.database), table: node.code, tab: 'structure' }))
    }
    contextmenu.position = { x: event.clientX, y: event.clientY }
    dataInfo.value = node
    contextmenu.visible = true
  }

  const closeContextMenu = () => {
    contextmenu.visible = false
  }

  useDismissableLayer(contextMenuRoot, computed(() => contextmenu.visible && !!dataInfo.value), closeContextMenu)

  const visibleCreateTable = (opened: boolean) => {
    tableCreateVisible.value = opened
    closeContextMenu()
    if (!opened) {
      onChangeDatabase()
    }
  }

  const visibleCreateColumn = (opened: boolean) => {
    columnCreateVisible.value = opened
    closeContextMenu()
  }

  const visibleExportData = (opened: boolean) => {
    tableExportVisible.value = opened
    closeContextMenu()
  }

  const visibleTruncateTable = (opened: boolean) => {
    tableTruncateVisible.value = opened
    closeContextMenu()
  }

  const visibleDropTable = (opened: boolean) => {
    tableDropVisible.value = opened
    closeContextMenu()
    if (!opened) {
      onChangeDatabase()
    }
  }

  const visibleChangeColumn = (opened: boolean) => {
    columnChangeVisible.value = opened
    closeContextMenu()
  }

  const visibleDropColumn = (opened: boolean) => {
    columnDropVisible.value = opened
    closeContextMenu()
    if (!opened) {
      onChangeDatabase()
    }
  }

  watch(() => [route.params.source, route.params.database, can('table:list')], onChangeDatabase, { immediate: true })
  watch(() => route.params.table, table => { selectedKeys.value = table ? [String(table) + '_table'] : [] }, { immediate: true })
  onScopeDispose(() => { tableRevision++ })

  return { can, contextMenuRoot, loading, selectDatabase, selectedKeys, originalSource, dataInfo, tableCreateVisible, tableExportVisible, tableTruncateVisible, tableDropVisible, columnCreateVisible, columnChangeVisible, columnDropVisible, contextmenu, tableEngine, searchKeyword, tableCount, filteredTreeArray, onChangeDatabase, onSelect, onLoadData, visibleContextMenu, visibleCreateTable, visibleCreateColumn, visibleExportData, visibleTruncateTable, visibleDropTable, visibleChangeColumn, visibleDropColumn }
}
