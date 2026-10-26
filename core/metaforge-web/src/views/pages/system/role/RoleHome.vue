<template>

    <DataTable :title="$t('role.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handlerInitialize"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #actions>
        <MfButton variant="primary" @click="handlerChangeInfo(true, null)">
          <PlusOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('role.common.create') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerChangeInfo(true, record)">
                <EditOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('role.common.assignMenu').replace('$NAME', record?.name)">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerAssignMenu(true, record)">
                <MenuOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <RoleInfo v-if="dataInfoVisible"
            :is-visible="dataInfoVisible"
            :info="dataInfo"
            @close="handlerChangeInfo(false, null)"/>
  <RoleMenu v-if="dataAllocationVisible"
            :is-visible="dataAllocationVisible"
            :info="dataInfo"
            @close="handlerAssignMenu(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel } from '@/model/filter'
import { useHeaders } from '@/views/pages/system/role/RoleUtils'
import { RoleModel } from '@/model/role'
import DataTable from '@/views/components/table/DataTable.vue'
import RoleInfo from '@/views/pages/system/role/RoleInfo.vue'
import RoleMenu from '@/views/pages/system/role/RoleMenu.vue'
import RoleService from '@/services/role'
import { EditOutlined, MenuOutlined, PlusOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'RoleHome' })

const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const dataInfoVisible = ref(false)
const dataAllocationVisible = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfo = ref<RoleModel | null>(null)

const handlerInitialize = () => {
  loading.value = true
  RoleService.getAll(filter)
             .then((response) => {
               if (response.status) {
                 data.value = response.data.content
                 dataCount.value = response.data.total
                 pageSize.value = response.data.size
                 pageIndex.value = response.data.page
               }
             })
             .finally(() => (loading.value = false))
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

const handlerChangeInfo = (isOpen: boolean, info: RoleModel | null) => {
  dataInfoVisible.value = isOpen
  dataInfo.value = info
  if (!isOpen) {
    handlerInitialize()
  }
}

const handlerAssignMenu = (opened: boolean, info: RoleModel | null) => {
  dataAllocationVisible.value = opened
  dataInfo.value = info
}

onMounted(() => handlerInitialize())
</script>
