<template>

    <DataTable :title="$t('user.common.list')"
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
          {{ $t('user.common.create') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'role'">
          <MfTag v-for="role in record.roles" :key="role.id">{{ role.name }}</MfTag>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('user.common.assignRole')">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerChangeRole(true, record)">
                <ArrowUpOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerChangeInfo(true, record)">
                <EditOutlined :style="{ fontSize: '15px' }"/>
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
import { FilterModel } from '@/model/filter'
import { useHeaders } from './UserUtils'
import { UserModel } from '@/model/user'
import DataTable from '@/views/components/table/DataTable.vue'
import UserInfo from '@/views/pages/system/user/UserInfo.vue'
import UserRole from '@/views/pages/system/user/components/UserRole.vue'
import UserService from '@/services/user'
import { ArrowUpOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'UserHome' })

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

const handlerInitialize = () => {
  loading.value = true
  UserService.getAll(filter)
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
