<template>
  <DataTable filterable :filter-fields="tableFilters" :columns="headers"
             :title="$t('user.common.log')"
             :description="$t('user.tip.log')"
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
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ua'">
          <EllipsisCell :text="record.ua"/>
        </template>
        <template v-else-if="column.key === 'message'">
          <EllipsisCell :text="record.message"/>
        </template>
        <template v-else-if="column.key === 'state'">
          <span class="inline-flex items-center gap-2">
            <MfBadge :variant="record.state === 'SUCCESS' ? 'success' : 'danger'" dot/>
            {{ record.state === 'SUCCESS' ? $t('common.success') : $t('common.failure') }}
          </span>
        </template>
      </template>
    </DataTable>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useHeaders } from './ProfileUtils'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import UserService from '@/services/user'

defineOptions({ name: 'LogHome' })

const tableFilters = useTableFilters('log')
const { headers } = useHeaders()


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
  } = usePagedList<any>(filter => UserService.getLogs(filter))

onMounted(() => initialize())
</script>
