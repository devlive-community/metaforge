<template>
  <div class="relative overflow-auto" style="height: 500px; max-height: 500px; max-width: 300px;">
    <MfSkeleton v-if="loading" active class="mt-2"/>

    <MfAlert v-else-if="data.length === 0 && errorMessage" class="mt-2" variant="danger" :title="errorMessage"/>

    <MfTree v-else-if="data.length > 0"
            v-model:selected-keys="selectedKeys"
            :data="data as any"
            :load-data="onLoadData"
            @select="onSelect">
      <template #title="{ node }">
        <div class="flex items-center space-x-1">
          <component :size="13" :is="node.level === 2 ? Database :
                          node.level === 3 ? Table :
                          node.level === 4 ? Columns3 :
                          Database
                     " class="text-xs font-semibold text-gray-500" />
          <span class="text-sm font-normal text-gray-500">{{ node.title }}</span>
        </div>
      </template>
    </MfTree>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { StructureEnum, StructureModel } from '@/model/structure'
import MetadataService from '@/services/metadata.ts'
import { ObjectUtils } from '@/utils/object'
import { Columns3, Database, Table } from '@lucide/vue'

defineOptions({ name: 'MetadataTree' })

const props = defineProps<{ code?: string }>()

const loading = ref(false)
const selectedKeys = ref<any[]>([])
const data = ref<StructureModel[]>([])
const errorMessage = ref<string | null>(null)

const updateTreeChildren = (list: any[], key: string, children: any[]): any[] =>
  list.map((node) => {
    if (node.key === key) {
      return { ...node, children }
    }
    if (node.children) {
      return { ...node, children: updateTreeChildren(node.children, key, children) }
    }
    return node
  })

const handleInitialize = () => {
  if (props.code) {
    data.value = []
    errorMessage.value = null
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
                           value: name,
                           isLeaf: false
                         } as any)
                       })
                     }
                     else {
                       errorMessage.value = response.data.message
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

const onLoadData = (treeNode: any): Promise<any[]> => {
  const item = treeNode as any
  return new Promise<any[]>((resolve) => {
    if (item.children) {
      resolve([])
      return
    }

    const children = [] as any[]
    if (item.level === StructureEnum.DATABASE) {
      MetadataService.getTablesByDatabase(props.code, item.code)
                     .then((response) => {
                       if (response.status && response.data && response.data.isSuccessful) {
                         response.data.columns.forEach((value: any) => children.push({
                           key: `${ item.key }.${ value.object_name }`,
                           title: value.object_name,
                           value: value.object_name,
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
                     .finally(() => {
                       data.value = updateTreeChildren(data.value, item.key, children)
                       resolve(children)
                     })
    }
    else if (item.level === StructureEnum.TABLE) {
      MetadataService.getColumnsByTable(props.code, item.database, item.code)
                     .then((response) => {
                       if (response.status && response.data && response.data.isSuccessful) {
                         response.data.columns
                                 .filter((value: any) => value.type_name === 'column')
                                 .forEach((value: any) => children.push({
                                   key: `${ item.key }.${ value.object_name }`,
                                   title: value.object_name,
                                   value: value.object_name,
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
                     .finally(() => {
                       data.value = updateTreeChildren(data.value, item.key, children)
                       resolve(children)
                     })
    }
    else {
      resolve([])
    }
  })
}

const onSelect = (_keys: any[], info: any) => {
  const item = info.node as any as StructureModel
  let text: string = item.title as string
  switch (item.level) {
    case StructureEnum.TABLE:
      text = item.database + '.' + text
      break
    case StructureEnum.COLUMN:
      text = item.database + '.' + item.table + '.' + text
      break
  }
  ObjectUtils.copy(text)
}

watch(() => props.code, () => handleInitialize())

handleInitialize()
</script>
