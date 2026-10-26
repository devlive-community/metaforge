<template>

    <DataTable :title="$t('schedule.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handlerInitialize"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'active'">
          <MfSwitch v-model="record.active" size="sm" :disabled="record.active"/>
        </template>

        <template v-else-if="column.key === 'system'">
          <MfSwitch v-model="record.system" size="sm" :disabled="record.system"/>
        </template>

        <template v-else-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('schedule.common.history')">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="handlerChangeInfo(true, record)">
                <HistoryOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <ScheduleHistory v-if="dataHistoryVisible"
                   :is-visible="dataHistoryVisible"
                   :info="dataInfo"
                   @close="handlerChangeInfo(false, null)"/>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { FilterModel } from '@/model/filter'
import { useHeaders } from './ScheduleUtils'
import ScheduleService from '@/services/schedule'
import DataTable from '@/views/components/table/DataTable.vue'
import ScheduleHistory from '@/views/pages/system/schedule/ScheduleHistory.vue'
import { ScheduleModel } from '@/model/schedule'
import { HistoryOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'ScheduleHome' })

const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const dataHistoryVisible = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfo = ref<ScheduleModel | null>(null)

const handlerInitialize = () => {
  loading.value = true
  ScheduleService.getAll(filter)
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

const handlerChangeInfo = (isOpen: boolean, info: any) => {
  dataHistoryVisible.value = isOpen
  dataInfo.value = info
}

handlerInitialize()
</script>
