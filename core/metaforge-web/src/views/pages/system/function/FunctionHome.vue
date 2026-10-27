<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('function.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handlerInitialize"
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
        <template v-if="column.key === 'type'">
          <MfTag>{{ $t('function.common.' + record.type.toLowerCase()) }}</MfTag>
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
          <MfTooltip :content="$t('common.editData')">
            <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerInfo(true, record)">
              <Pencil :size="15" />
            </MfButton>
          </MfTooltip>
        </template>
      </template>
    </DataTable>

  <FunctionInfo v-if="dataInfoVisible"
                :is-visible="dataInfoVisible"
                :info="dataInfo"
                @close="handlerInfo(false, null)"/>

  <FunctionImport v-if="dataImportVisible" :is-visible="dataImportVisible" @close="handlerImport(false)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel, type TableFilters } from '@/model/filter'
import { useHeaders } from '@/views/pages/system/function/FunctionUtils'
import { FunctionModel } from '@/model/function'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import FunctionService from '@/services/function'
import FunctionInfo from '@/views/pages/system/function/FunctionInfo.vue'
import FunctionImport from '@/views/pages/system/function/FunctionImport.vue'
import { Import, Pencil, Plus } from '@lucide/vue'

defineOptions({ name: 'FunctionHome' })

const tableFilters = useTableFilters('function')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const dataInfoVisible = ref(false)
const dataImportVisible = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfo = ref<FunctionModel | null>(null)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handlerInitialize()
}

let latestRequest = 0
const handlerInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  FunctionService.getAll({ ...filter })
                 .then((response) => {
                 if (request !== latestRequest) return
                   if (response.status) {
                     data.value = response.data.content
                     dataCount.value = response.data.total
                     pageSize.value = response.data.size
                     pageIndex.value = response.data.page
                   }
                 })
                 .finally(() => { if (request === latestRequest) loading.value = false })
}

const fetchData = (value: number) => {
  filter.page = value
  filter.size = pageSize.value
  handlerInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

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
    handlerInitialize()
  }
}

const handlerImport = (value: boolean) => {
  dataImportVisible.value = value
  handlerInitialize()
}

onMounted(() => handlerInitialize())
</script>
