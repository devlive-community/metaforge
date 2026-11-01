<template>
  <div class="min-h-0 flex-1 overflow-auto px-1.5 pb-2">
    <div v-if="loading" class="flex justify-center py-6">
      <MfSpinner size="md"/>
    </div>

    <MfAlert v-else-if="data.length === 0 && errorMessage" class="mt-2" variant="danger" :title="errorMessage"/>

    <MfTree v-else-if="filteredData.length > 0"
            v-model:selected-keys="selectedKeys"
            :data="filteredData"
            :expanded-keys="treeExpandedKeys"
            :load-data="onLoadData"
            @select="onSelect"
            @update:expanded-keys="expandedKeys = $event">
      <template #title="{ node }">
        <div class="flex min-w-0 items-center gap-1.5">
          <component :is="iconOf(node)" :size="13" class="shrink-0" :class="node.level === StructureEnum.DATABASE ? 'text-primary' : 'text-content-tertiary'"/>
          <span class="truncate text-sm text-content-secondary">{{ node.title }}</span>
        </div>
      </template>
    </MfTree>

    <div v-else class="flex items-center justify-center px-3 py-8 text-sm text-content-tertiary">
      {{ $t('query.tip.noMetadata') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { ChevronsDown, Columns3, Database, KeyRound, Table } from '@lucide/vue'
import { StructureEnum } from '@/model/structure'
import MetadataService from '@/services/metadata.ts'
import { nativePage } from '@/model/source-native'
import { useSourceCapabilities } from '@/composables/useSourceCapabilities'
import { ObjectUtils } from '@/utils/object'

/** Tree node shape used by this component (MfTreeNode + metadata level info). */
interface TreeNode
{
  key: string
  title: string
  level: StructureEnum
  code?: string
  catalog?: string
  database?: string
  table?: string
  isLeaf: boolean
  children?: TreeNode[]
  /** Key-value sources: a key, or the entry that loads the next page of keys from `cursor`. */
  kind?: 'key' | 'more'
  cursor?: string
}

/** Keys read per page from a key-value source. */
const KEY_PAGE = 200

defineOptions({ name: 'MetadataTree' })

const props = defineProps<{ code?: string; keyword?: string }>()

const emit = defineEmits<{ (e: 'select-table', value: { database: string; table: string } | null): void }>()

const { t } = useI18n()

const loading = ref(false)
const selectedKeys = ref<(string | number)[]>([])
const expandedKeys = ref<(string | number)[]>([])
const data = ref<TreeNode[]>([])
const errorMessage = ref<string | null>(null)

// Sources without databases and tables but with namespaces (Redis) list their keys instead.
const { can, capabilityLoading } = useSourceCapabilities(computed(() => props.code || ''))
const keyValue = computed(() => can('namespace:list') && !can('database:list'))

const iconOf = (node: TreeNode) => node.kind === 'key' ? KeyRound
  : node.kind === 'more' ? ChevronsDown
  : node.level === StructureEnum.DATABASE ? Database
  : node.level === StructureEnum.COLUMN ? Columns3 : Table

const keyword = computed(() => (props.keyword ?? '').trim().toLowerCase())

const filterTree = (nodes: TreeNode[], kw: string): TreeNode[] =>
  nodes
    .map(node => {
      if (node.title.toLowerCase().includes(kw)) {
        return node
      }
      if (node.children) {
        const children = filterTree(node.children, kw)
        if (children.length > 0) {
          return { ...node, children }
        }
      }
      return null
    })
    .filter(node => node !== null) as TreeNode[]

const collectKeys = (nodes: TreeNode[]): (string | number)[] =>
  nodes.flatMap(node => [node.key, ...(node.children ? collectKeys(node.children) : [])])

const filteredData = computed(() => keyword.value ? filterTree(data.value, keyword.value) : data.value)
const treeExpandedKeys = computed(() => keyword.value ? collectKeys(filteredData.value) : expandedKeys.value)

const updateTreeChildren = (list: TreeNode[], key: string, children: TreeNode[]): TreeNode[] =>
  list.map(node => {
    if (node.key === key) {
      return { ...node, children }
    }
    if (node.children) {
      return { ...node, children: updateTreeChildren(node.children, key, children) }
    }
    return node
  })

const buildTablesGroup = (parent: TreeNode, tables: TreeNode[]): TreeNode => ({
  key: `${ parent.key }::tables`,
  title: `${ t('common.table') } (${ tables.length })`,
  level: StructureEnum.GROUP,
  catalog: parent.code,
  isLeaf: false,
  children: tables
})

const loadTables = (item: TreeNode): Promise<TreeNode[]> => {
  return new Promise<TreeNode[]>((resolve) => {
    const children: TreeNode[] = []
    MetadataService.getTablesByDatabase(props.code!, item.code!)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       response.data.columns.forEach((value: any) => children.push({
                         key: `${ item.key }.${ value.object_name }`,
                         title: value.object_name,
                         code: value.object_name,
                         database: item.code,
                         catalog: value.object_name,
                         level: StructureEnum.TABLE,
                         isLeaf: false
                       }))
                     }
                     else {
                       MfMessage.error(response.data.message)
                     }
                   })
                   .finally(() => resolve(children))
  })
}

const loadColumns = (item: TreeNode): Promise<TreeNode[]> => {
  return new Promise<TreeNode[]>((resolve) => {
    const children: TreeNode[] = []
    MetadataService.getColumnsByTable(props.code!, item.database!, item.code!)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       response.data.columns
                               .filter((value: any) => value.type_name === 'column')
                               .forEach((value: any) => children.push({
                                 key: `${ item.key }.${ value.object_name }`,
                                 title: value.object_name,
                                 database: item.database,
                                 table: item.code,
                                 catalog: value.object_name,
                                 code: value.object_name,
                                 level: StructureEnum.COLUMN,
                                 isLeaf: true
                               }))
                     }
                     else {
                       MfMessage.error(response.data.message)
                     }
                   })
                   .finally(() => resolve(children))
  })
}

/** One page of a namespace's keys; a last "more" entry continues from the returned cursor. */
const loadKeys = async (namespace: TreeNode, cursor = ''): Promise<TreeNode[]> => {
  try {
    const page = nativePage(await MetadataService.nativeBrowse(props.code!, { namespace: namespace.code, prefix: '', cursor, limit: KEY_PAGE }))
    const keys: TreeNode[] = page.entries.map(entry => ({
      key: `${ namespace.key }::${ entry.id }`, title: entry.name, code: entry.name, database: namespace.code, level: StructureEnum.TABLE, isLeaf: true, kind: 'key'
    }))
    if (page.hasMore && page.nextCursor) {
      keys.push({ key: `${ namespace.key }::more::${ page.nextCursor }`, title: t('query.tree.more'), database: namespace.code, level: StructureEnum.GROUP, isLeaf: true,
        kind: 'more', cursor: page.nextCursor, catalog: namespace.key })
    }
    return keys
  }
  catch (error) {
    MfMessage.error(error instanceof Error ? error.message : String(error))
    return []
  }
}

const findNode = (list: TreeNode[], key: string): TreeNode | undefined => {
  for (const node of list) {
    if (node.key === key) return node
    const found = node.children ? findNode(node.children, key) : undefined
    if (found) return found
  }
  return undefined
}

const loadMoreKeys = async (more: TreeNode) => {
  const namespace = findNode(data.value, more.catalog!)
  if (!namespace) return
  const next = await loadKeys(namespace, more.cursor)
  const kept = (namespace.children || []).filter(child => child.kind !== 'more')
  data.value = updateTreeChildren(data.value, namespace.key, [...kept, ...next])
}

const onLoadData = (treeNode: any): Promise<TreeNode[]> => {
  const item = treeNode as TreeNode
  if (item.children) {
    return Promise.resolve([])
  }
  if (keyValue.value && item.level === StructureEnum.DATABASE) {
    return loadKeys(item).then(children => {
      data.value = updateTreeChildren(data.value, item.key, children)
      return children
    })
  }
  if (item.level === StructureEnum.DATABASE) {
    return loadTables(item)
             .then(children => {
               const group = buildTablesGroup(item, children)
               data.value = updateTreeChildren(data.value, item.key, [group])
               return [group]
             })
  }
  if (item.level === StructureEnum.TABLE) {
    return loadColumns(item)
             .then(children => {
               data.value = updateTreeChildren(data.value, item.key, children)
               return children
             })
  }
  return Promise.resolve([])
}

const onSelect = (_keys: (string | number)[], info: any) => {
  const item = info.node as TreeNode
  if (item.kind === 'more') {
    selectedKeys.value = []
    loadMoreKeys(item)
    return
  }
  if (item.kind === 'key') {
    // A key reads as a table of key, type and value in the Redis driver's SQL.
    ObjectUtils.copy(`SELECT * FROM \`${ item.title }\``)
    MfMessage.success(t('query.tree.keyCopied'))
    emit('select-table', null)
    return
  }
  switch (item.level) {
    case StructureEnum.TABLE:
      ObjectUtils.copy(item.database + '.' + item.title)
      emit('select-table', { database: item.database as string, table: item.code as string })
      break
    case StructureEnum.COLUMN:
      ObjectUtils.copy(item.database + '.' + item.table + '.' + item.title)
      emit('select-table', { database: item.database as string, table: item.table as string })
      break
    default:
      if (!info.selected) {
        emit('select-table', null)
      }
  }
}

const initializeKeyValue = async () => {
  data.value = []
  errorMessage.value = null
  selectedKeys.value = []
  expandedKeys.value = []
  loading.value = true
  try {
    const page = nativePage(await MetadataService.nativeNamespaces(props.code!, {}))
    data.value = page.entries.map(entry => ({ key: `ns:${ entry.id }`, title: entry.name, code: entry.id, catalog: entry.id, level: StructureEnum.DATABASE, isLeaf: false }))
  }
  catch (error) {
    errorMessage.value = error instanceof Error ? error.message : String(error)
  }
  finally {
    loading.value = false
  }
}

const handleInitialize = () => {
  if (props.code && capabilityLoading.value) {
    loading.value = true
    return
  }
  if (props.code && keyValue.value) {
    initializeKeyValue()
    return
  }
  if (props.code) {
    data.value = []
    errorMessage.value = null
    selectedKeys.value = []
    expandedKeys.value = []
    loading.value = true
    MetadataService.getDatabaseBySource(props.code)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       response.data.columns.forEach((item: any) => {
                         const name = item.object_name || item.schema_name || item.SCHEMA_NAME
                         data.value.push({
                           key: name,
                           title: name,
                           catalog: name,
                           code: name,
                           level: StructureEnum.DATABASE,
                           isLeaf: false
                         })
                       })
                     }
                     else {
                       errorMessage.value = response.data.message
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

// The tree's shape follows the source's capabilities, so it starts once they are known.
watch(() => [props.code, capabilityLoading.value, keyValue.value], () => handleInitialize(), { immediate: true })
</script>
