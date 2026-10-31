<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('role.common.list')"
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
        <MfButton variant="primary" @click="handlerChangeInfo(true, null)">
          <Plus :size="16" />
          {{ $t('role.common.create') }}
        </MfButton>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <EllipsisCell :text="record.name"/>
        </template>
        <template v-else-if="column.key === 'description'">
          <EllipsisCell :text="record.description"/>
        </template>
        <template v-else-if="column.key === 'active'">
          <MfSwitch :model-value="record.active"
                    :disabled="record.code === 'ADMIN'"
                    @update:model-value="(value: boolean) => onChangeActive(record, value)"/>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('common.editData')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerChangeInfo(true, record)">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('role.common.assignMenu').replace('$NAME', record?.name)">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerAssignMenu(true, record)">
                <Menu :size="15" />
              </MfButton>
            </MfTooltip>

            <MfTooltip :content="$t('role.common.delete')">
              <MfButton variant="ghost" size="sm" shape="circle" :disabled="record.code === 'ADMIN'" @click="handlerChangeDelete(true, record)">
                <Trash2 :size="15" />
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
  <RoleDelete v-if="dataDeleteVisible"
              :is-visible="dataDeleteVisible"
              :info="dataInfo"
              @close="handlerChangeDelete(false, null)"/>

  <RoleMenu v-if="dataAllocationVisible"
            :is-visible="dataAllocationVisible"
            :info="dataInfo"
            @close="handlerAssignMenu(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useHeaders } from '@/views/pages/system/role/RoleUtils'
import { RoleModel } from '@/model/role'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import RoleInfo from '@/views/pages/system/role/RoleInfo.vue'
import RoleMenu from '@/views/pages/system/role/RoleMenu.vue'
import RoleDelete from '@/views/pages/system/role/RoleDelete.vue'
import RoleService from '@/services/role'
import { MfMessage } from 'metaforge-ui'
import { Menu, Pencil, Plus, Trash2 } from '@lucide/vue'

defineOptions({ name: 'RoleHome' })

const tableFilters = useTableFilters('role')
const { headers } = useHeaders()

const dataInfoVisible = ref(false)
const dataAllocationVisible = ref(false)
const dataDeleteVisible = ref(false)
const dataInfo = ref<RoleModel | null>(null)

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
  } = usePagedList<any>(filter => RoleService.getAll(filter))

const onChangeActive = (record: any, value: boolean) => {
  RoleService.changeActive(record.code, value)
             .then((response) => {
               if (response.status) {
                 record.active = value
               }
               else {
                 MfMessage.error(response.message)
               }
             })
}

const handlerChangeDelete = (isOpen: boolean, info: RoleModel | null) => {
  dataDeleteVisible.value = isOpen
  dataInfo.value = info
  if (!isOpen) {
    initialize()
  }
}

const handlerChangeInfo = (isOpen: boolean, info: RoleModel | null) => {
  dataInfoVisible.value = isOpen
  dataInfo.value = info
  if (!isOpen) {
    initialize()
  }
}

const handlerAssignMenu = (opened: boolean, info: RoleModel | null) => {
  dataAllocationVisible.value = opened
  dataInfo.value = info
}

onMounted(() => initialize())
</script>
