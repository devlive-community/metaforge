<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="$t('schedule.common.history')"
           @close="onCancel">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg"/>
    </div>
    <template v-else>
      <MfTable :columns="headers"
               :data-source="data"
               row-key="id"/>

      <div v-if="data?.length > 0" class="py-2 flex justify-end">
        <MfPagination :current="pageIndex"
                      :page-size="pageSize"
                      :total="dataCount"
                      show-size
                      :page-sizes="[10, 20, 50]"
                      @change="(page: number, size: number) => size !== pageSize ? onSizeChange(page, size) : onPageChange(page)"/>
      </div>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ScheduleModel } from '@/model/schedule'
import { FilterModel } from '@/model/filter'
import { useHeaders } from '@/views/pages/system/schedule/ScheduleUtils'
import ScheduleService from '@/services/schedule'

defineOptions({ name: 'ScheduleHistory' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: ScheduleModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const filter: FilterModel = new FilterModel()
const { historyHeaders: headers } = useHeaders()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)

const handlerInitialize = () => {
  loading.value = true
  ScheduleService.getScheduleHistory(filter, props.info?.id as number)
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

const onCancel = () => {
  visible.value = false
}

handlerInitialize()
</script>
