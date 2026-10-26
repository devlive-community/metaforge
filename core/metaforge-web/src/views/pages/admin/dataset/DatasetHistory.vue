<template>
  <MfModal v-model:open="visible"
           width="60%"
           :title="`[ ${ info?.name } ] ${ $t('dataset.common.history') }`">
    <div class="relative">
      <MfTable :columns="historyHeaders as any"
               :data-source="data"
               :loading="loading"
               row-key="id"
               :empty-text="$t('common.noData')">
        <template #cell-state="{ record }">
          <MfPopover v-if="hasMessage(record)">
            <template #content>
              <div class="p-2 w-full overflow-x-auto">{{ record?.message }}</div>
            </template>
            <MfTag :style="{ color: Common.getColor(record?.state), borderColor: Common.getColor(record?.state) }">{{ getStateText(record?.state) }}</MfTag>
          </MfPopover>
          <MfTag v-else :style="{ color: Common.getColor(record?.state), borderColor: Common.getColor(record?.state) }">{{ getStateText(record?.state) }}</MfTag>
        </template>

        <template #cell-progress="{ record }">
          <div class="h-2 w-full min-w-[120px] rounded-mf-full bg-surface-active">
            <div class="h-2 rounded-mf-full bg-primary transition-[width]"
                 :style="{ width: toPercent(record?.progress) + '%' }"/>
          </div>
        </template>

        <template #cell-action="{ record }">
          <div class="flex gap-1">
            <MfButton size="sm" variant="primary" @click="onViewLog(record)">
              {{ $t('dataset.history.viewLog') }}
            </MfButton>
            <MfButton v-if="record?.executorConfigure" size="sm" @click="onViewConfigure(record)">
              {{ $t('dataset.history.viewConfigure') }}
            </MfButton>
            <MfButton v-if="isStoppable(record)"
                      size="sm"
                      variant="danger"
                      :loading="stoppingId === record.id"
                      @click="onStop(record)">
              {{ $t('dataset.history.stop') }}
            </MfButton>
          </div>
        </template>
      </MfTable>

      <div v-if="data.length > 0" class="py-2 flex justify-end">
        <MfPagination :current="pageIndex"
                      :page-size="pageSize"
                      :total="dataCount"
                      show-size
                      :page-sizes="[10, 20, 50]"
                      @change="onPageChange"
                      @update:page-size="(size: number) => onSizeChange(1, size)"/>
      </div>
    </div>

    <DatasetHistoryLogger v-if="loggerVisible"
                          :is-visible="loggerVisible"
                          :info="loggerInfo"
                          @close="onLoggerClose"/>

    <MfModal v-model:open="configureVisible"
             width="50%"
             :title="$t('dataset.history.configureTitle')">
      <pre class="text-xs whitespace-pre-wrap break-all p-2 bg-gray-50 dark:bg-gray-900 rounded">{{ formatJson(configureInfo) }}</pre>
    </MfModal>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { FilterModel } from '@/model/filter'
import { useDatasetHeaders } from './DatasetUtils'
import { DatasetModel } from '@/model/dataset'
import Common, { useUtil } from '@/utils/common'
import DatasetService from '@/services/dataset'
import DatasetHistoryLogger from './DatasetHistoryLogger.vue'

defineOptions({ name: 'DatasetHistory' })

const props = withDefaults(defineProps<{ isVisible?: boolean; info?: DatasetModel | null }>(), {
  isVisible: false,
  info: null
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()
const filter: FilterModel = new FilterModel()
const { historyHeaders } = useDatasetHeaders()
const { getText } = useUtil()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const loggerVisible = ref(false)
const loggerInfo = ref<any>(null)
const stoppingId = ref<number | null>(null)
const configureVisible = ref(false)
const configureInfo = ref<string | null>(null)

const handleInitialize = () => {
  loading.value = true
  DatasetService.getHistory(props.info?.code as string, filter)
                .then((response: any) => {
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
  handleInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const getStateText = (origin: string): string => getText(origin)

const toPercent = (value: number | string | null | undefined): number => {
  if (value === null || value === undefined || value === '') return 0
  const v = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(v) || v < 0) return 0
  return Math.min(v, 100)
}

const onViewLog = (row: any) => {
  loggerInfo.value = row
  loggerVisible.value = true
}

const onLoggerClose = () => {
  loggerVisible.value = false
  loggerInfo.value = null
}

const isStoppable = (row: any): boolean => row?.state === 'RUNNING' || row?.state === 'CREATED'

const hasMessage = (row: any): boolean => !!row?.message && (row?.state === 'FAILURE' || row?.state === 'INTERRUPTED')

const onViewConfigure = (row: any) => {
  configureInfo.value = row?.executorConfigure || ''
  configureVisible.value = true
}

const formatJson = (raw: string | null | undefined): string => {
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  }
  catch {
    return raw
  }
}

const onStop = (row: any) => {
  if (!row?.id) {
    return
  }
  stoppingId.value = row.id
  DatasetService.stopHistory(row.id)
                .then((response: any) => {
                  if (response.status) {
                    MfMessage.success(t('dataset.history.stopRequested'))
                    handleInitialize()
                  }
                  else {
                    MfMessage.error(response.message)
                  }
                })
                .finally(() => (stoppingId.value = null))
}

handleInitialize()
</script>
