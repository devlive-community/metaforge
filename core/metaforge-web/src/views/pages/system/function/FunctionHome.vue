<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('function.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="initialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #actions>
        <MfButton variant="primary" @click="handlerInfo(true, null)">
          <Plus :size="16" />
          {{ $t('function.common.create') }}
        </MfButton>
        <MfButton @click="handlerImport(true)">
          <Import :size="16" />
          {{ $t('function.common.import') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <EllipsisCell :text="record.name"/>
        </template>
        <template v-else-if="column.key === 'description'">
          <EllipsisCell :text="record.description"/>
        </template>
        <template v-else-if="column.key === 'type'">
          <MfTag>{{ $t('function.common.' + String(record.type).toLowerCase()) }}</MfTag>
        </template>
        <template v-else-if="column.key === 'syntax'">
          <EllipsisCell :text="record.syntax" class="font-mono text-xs"/>
        </template>
        <template v-else-if="column.key === 'plugin'">
          <div class="flex items-center -space-x-2">
            <MfAvatar v-for="item in extractItem(record?.plugin).slice(0, 3)" :key="item.name" :src="item.src" size="sm"/>
            <MfAvatar v-if="extractItem(record?.plugin).length > 3" size="sm">
              +{{ extractItem(record?.plugin).length - 3 }}
            </MfAvatar>
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerInfo(true, record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('function.common.delete')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerDelete(true, record)">
                <Trash2 :size="15" />
              </MfButton>
            </MfTooltip>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <FunctionDelete v-if="dataDeleteVisible"
                  :is-visible="dataDeleteVisible"
                  :info="dataInfo"
                  @close="handlerDelete(false, null)"/>

  <FunctionInfo v-if="dataInfoVisible"
                :is-visible="dataInfoVisible"
                :info="dataInfo"
                @close="handlerInfo(false, null)"/>

  <FunctionImport v-if="dataImportVisible" :is-visible="dataImportVisible" @close="handlerImport(false)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useHeaders } from '@/views/pages/system/function/FunctionUtils'
import { FunctionModel } from '@/model/function'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import FunctionService from '@/services/function'
import FunctionInfo from '@/views/pages/system/function/FunctionInfo.vue'
import FunctionImport from '@/views/pages/system/function/FunctionImport.vue'
import FunctionDelete from '@/views/pages/system/function/FunctionDelete.vue'
import { Import, Pencil, Plus, Trash2 } from '@lucide/vue'

defineOptions({ name: 'FunctionHome' })

const tableFilters = useTableFilters('function')
const { headers } = useHeaders()

const dataInfoVisible = ref(false)
const dataImportVisible = ref(false)
const dataDeleteVisible = ref(false)
const dataInfo = ref<FunctionModel | null>(null)

const {
    loading,
    data,
    pageIndex,
    pageSize,
    dataCount,
    initialize,
    onSearch,
    onPageChange,
    onSizeChange
  } = usePagedList<any>(filter => FunctionService.getAll(filter))

const extractItem = (plugins: string[]) => {
  return (plugins || []).map((item: string) => ({
    name: item,
    src: `/static/images/plugin/${ item.toLowerCase() }.svg`
  }))
}

const handlerInfo = (opened: boolean, value: FunctionModel | null) => {
  dataInfoVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    initialize()
  }
}

const handlerDelete = (opened: boolean, value: FunctionModel | null) => {
  dataDeleteVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    initialize()
  }
}

const handlerImport = (value: boolean) => {
  dataImportVisible.value = value
  initialize()
}

onMounted(() => initialize())
</script>
