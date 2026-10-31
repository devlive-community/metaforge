<template>

    <MfAlert v-if="notice" :variant="notice.variant" :title="notice.message" closable @close="notice = null" class="mb-3"/>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('schedule.common.list')"
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
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <EllipsisCell :text="record.name"/>
        </template>

        <template v-else-if="column.key === 'description'">
          <EllipsisCell :text="record.description"/>
        </template>

        <template v-else-if="column.key === 'type'">
          {{ $t(`schedule.type.${ record.type }`) }}
        </template>

        <template v-else-if="column.key === 'active'">
          <MfSwitch :model-value="record.active" size="sm"
                    @update:model-value="(value: boolean) => onChangeActive(record, value)"/>
        </template>

        <template v-else-if="column.key === 'system'">
          <MfSwitch :model-value="record.system" size="sm"
                    @update:model-value="(value: boolean) => onChangeSystem(record, value)"/>
        </template>

        <template v-else-if="column.key === 'expression'">
          <code class="rounded bg-surface-hover px-1.5 py-0.5 text-xs">{{ record.expression }}</code>
        </template>

        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('schedule.common.trigger')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerTrigger(record)">
                <MfSpinner v-if="triggeringId === record.id" size="sm"/>
                <CirclePlay :size="15" v-else />
              </MfButton>
            </MfTooltip>
            <MfTooltip :content="$t('schedule.common.upcoming')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerUpcoming(record)">
                <Clock :size="15" />
              </MfButton>
            </MfTooltip>
            <MfTooltip :content="$t('schedule.common.history')">
              <MfButton variant="ghost" size="sm" shape="circle" @click="handlerChangeInfo(true, record)">
                <History :size="15" />
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

  <MfModal v-model:open="upcomingVisible"
           :title="upcomingName"
           width="420px">
    <div v-if="upcomingLoading" class="flex items-center justify-center py-10">
      <MfSpinner size="lg"/>
    </div>
    <div v-else class="flex flex-col">
      <div v-for="(time, index) in upcomingTimes"
           :key="index"
           class="flex items-center gap-2 border-b border-line py-2 text-sm text-content last:border-b-0">
        <Clock :size="14" class="text-content-tertiary"/>
        <span>{{ time }}</span>
      </div>
      <div v-if="upcomingTimes.length === 0" class="py-6 text-center text-sm text-content-tertiary">
        {{ $t('common.noData') }}
      </div>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useHeaders } from './ScheduleUtils'
import ScheduleService from '@/services/schedule'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import ScheduleHistory from '@/views/pages/system/schedule/ScheduleHistory.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { ScheduleModel } from '@/model/schedule'
import { CirclePlay, Clock, History } from '@lucide/vue'

defineOptions({ name: 'ScheduleHome' })

const { t } = useI18n()
const tableFilters = useTableFilters('schedule')
const { headers } = useHeaders()

const dataHistoryVisible = ref(false)
const dataInfo = ref<ScheduleModel | null>(null)

const triggeringId = ref<number | null>(null)
const noticeSeq = ref(0)
const notice = ref<{ variant: 'success' | 'danger', message: string, id: number } | null>(null)
const upcomingVisible = ref(false)
const upcomingLoading = ref(false)
const upcomingName = ref('')
const upcomingTimes = ref<string[]>([])

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
  } = usePagedList<any>(filter => ScheduleService.getAll(filter))

const onChangeActive = (record: any, value: boolean) => {
  ScheduleService.changeActive(record.code, value)
                 .then((response) => {
                   if (response.status) {
                     record.active = value
                   }
                   else {
                     showNotice('danger', response.message)
                   }
                 })
}

const onChangeSystem = (record: any, value: boolean) => {
  ScheduleService.changeSystem(record.code, value)
                 .then((response) => {
                   if (response.status) {
                     record.system = value
                   }
                   else {
                     showNotice('danger', response.message)
                   }
                 })
}

const handlerChangeInfo = (isOpen: boolean, info: any) => {
  dataHistoryVisible.value = isOpen
  dataInfo.value = info
}

const showNotice = (variant: 'success' | 'danger', message: string) => {
  const id = ++noticeSeq.value
  notice.value = { variant, message, id }
  // Success notices fade automatically; error notices stay until closed by the user
  if (variant === 'success') {
    window.setTimeout(() => {
      if (notice.value?.id === id) {
        notice.value = null
      }
    }, 4000)
  }
}

const handlerTrigger = (record: any) => {
  if (triggeringId.value !== null) return
  triggeringId.value = record.id
  ScheduleService.trigger(record.id)
                 .then((response) => {
                   if (response.status) {
                     showNotice('success', t('schedule.common.triggerSuccess'))
                     initialize()
                   }
                   else {
                     showNotice('danger', response.message || 'Trigger failed')
                   }
                 })
                 .finally(() => { triggeringId.value = null })
}

const handlerUpcoming = (record: any) => {
  upcomingName.value = record.name
  upcomingVisible.value = true
  upcomingLoading.value = true
  upcomingTimes.value = []
  ScheduleService.getUpcomingTimes(record.id)
                 .then((response) => {
                   if (response.status) {
                     upcomingTimes.value = response.data || []
                   }
                   else {
                     upcomingVisible.value = false
                     showNotice('danger', response.message || 'Failed to load upcoming times')
                   }
                 })
                 .finally(() => { upcomingLoading.value = false })
}

initialize()
</script>
