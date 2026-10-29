<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('user.common.list')"
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
        <MfButton variant="primary" @click="handlerChangeInfo(true, null)">
          <Plus :size="16" />
          {{ $t('user.common.create') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <MfTag v-for="role in record.roles" :key="role.id">{{ role.name }}</MfTag>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace size="xs">
            <MfTooltip :content="$t('user.common.assignRole')">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerChangeRole(true, record)">
                <ArrowUp :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerChangeInfo(true, record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <UserRole v-if="dataRoleVisible"
            :is-visible="dataRoleVisible"
            :info="dataInfo"
            @close="handlerChangeRole(false, null)"/>

  <UserInfo v-if="dataInfoVisible"
            :is-visible="dataInfoVisible"
            :info="dataInfo"
            @close="handlerChangeInfo(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel, type TableFilters } from '@/model/filter'
import { useHeaders } from './UserUtils'
import { UserModel } from '@/model/user'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import UserInfo from '@/views/pages/system/user/UserInfo.vue'
import UserRole from '@/views/pages/system/user/components/UserRole.vue'
import UserService from '@/services/user'
import { ArrowUp, Pencil, Plus } from '@lucide/vue'

defineOptions({ name: 'UserHome' })

const tableFilters = useTableFilters('user')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const dataRoleVisible = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfo = ref<UserModel | null>(null)
const dataInfoVisible = ref(false)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handlerInitialize()
}

let latestRequest = 0
const handlerInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  UserService.getAll({ ...filter })
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

const handlerChangeRole = (isOpen: boolean, info: UserModel | null) => {
  dataRoleVisible.value = isOpen
  dataInfo.value = info
  if (!isOpen) {
    handlerInitialize()
  }
}

const handlerChangeInfo = (opened: boolean, info: UserModel | null) => {
  dataInfoVisible.value = opened
  dataInfo.value = info
  if (!opened) {
    handlerInitialize()
  }
}

onMounted(() => handlerInitialize())
</script>
