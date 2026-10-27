<template>
  <DataTable filterable :filter-fields="tableFilters" :columns="headers"
             :title="$t('user.common.log')"
             :description="$t('user.tip.log')"
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
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'state'">
          <span class="inline-flex items-center gap-2">
            <MfBadge :variant="record.state === 'SUCCESS' ? 'success' : 'danger'" dot/>
            {{ record.state }}
          </span>
        </template>
      </template>
    </DataTable>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel, type TableFilters } from '@/model/filter'
import { useHeaders } from './ProfileUtils'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import UserService from '@/services/user'

defineOptions({ name: 'LogHome' })

const tableFilters = useTableFilters('log')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const data = ref<any[]>([])

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handlerInitialize()
}

let latestRequest = 0
const handlerInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  UserService.getLogs({ ...filter })
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

onMounted(() => handlerInitialize())
</script>
