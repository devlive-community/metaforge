<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('dataset.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handleInitialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <MfTooltip :content="record?.source.type">
            <MfAvatar size="sm" :src="'/static/images/plugin/' + record?.source.type.toLowerCase() + '.svg'" :alt="record?.source.type"/>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'syncMode'">
          <MfTag v-if="record?.syncMode === 'MANUAL'">{{ $t('dataset.common.syncModeManual') }}</MfTag>
          <MfTag v-else-if="record?.syncMode === 'TIMING'">{{ $t('dataset.common.syncModeTiming') }}</MfTag>
          <MfTag v-else-if="record?.syncMode === 'OUT_SYNC'">{{ $t('dataset.common.syncModeOutSync') }}</MfTag>
        </template>
        <template v-else-if="column.key === 'state'">
          <MfPopover>
            <template #content>
              <DatasetState :states="record?.state"/>
            </template>
            <span class="cursor-pointer">{{ getState(record?.state) }}</span>
          </MfPopover>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('dataset.common.adhoc')">
              <router-link v-if="isSuccess(record?.state)" :to="`/dataset/adhoc/${ record?.code }`" target="_blank">
                <MfButton variant="ghost" size="sm">
                  <ChartColumn :size="15" />
                </MfButton>
              </router-link>
              <MfButton v-else variant="ghost" size="sm" disabled>
                <ChartColumn :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm">
                <Settings :size="15" />
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem key_="info">
                    <router-link :to="`/dataset/info/${ record?.code }`" target="_blank" class="flex items-center">
                      <Info :size="15" />
                      <span class="ml-2">{{ $t('dataset.common.info') }}</span>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem key_="sync" :disabled="!isSuccess(record?.state)" @click="visibleSyncData(record, true)">
                    <div class="flex items-center space-x-2">
                      <RefreshCw :size="15" />
                      <span>{{ $t('dataset.common.syncData') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="history" @click="visibleHistory(record, true)">
                    <div class="flex items-center space-x-2">
                      <History :size="15" />
                      <span>{{ $t('dataset.common.history') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="error" :disabled="isSuccess(record?.state)" @click="!isSuccess(record?.state) && visibleError(record, true)">
                    <div class="flex items-center space-x-2">
                      <TriangleAlert :size="15" />
                      <span>{{ $t('dataset.common.error') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="rebuild" :disabled="isSuccess(record?.state)" @click="visibleRebuild(record, true)">
                    <div class="flex items-center space-x-2">
                      <component :size="15" :is="record?.state === 'SUCCESS' ? CirclePlay : CirclePause" />
                      <span>{{ $t('dataset.common.rebuild') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="clear" :disabled="!(record?.totalRows > 0)" @click="visibleClearData(record, true)">
                    <div class="flex items-center space-x-2">
                      <SquareX :size="15" />
                      <span>{{ $t('dataset.common.clearData') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="delete" @click="visibleDelete(record, true)">
                    <div class="flex items-center space-x-2">
                      <Trash2 :size="15" />
                      <span>{{ $t('dataset.common.delete') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <DatasetRebuild v-if="rebuildVisible"
                  :is-visible="rebuildVisible"
                  :data="contextData"
                  @close="visibleRebuild(null, false)"/>

  <DatasetHistory v-if="historyVisible"
                  :is-visible="historyVisible"
                  :info="contextData"
                  @close="visibleHistory(null, false)"/>

  <DatasetSync v-if="syncDataVisible"
               :is-visible="syncDataVisible"
               :info="contextData"
               @close="visibleSyncData(null, false)"/>

  <DatasetClear v-if="clearDataVisible"
                :is-visible="clearDataVisible"
                :info="contextData"
                @close="visibleClearData(null, false)"/>

  <DatasetDelete v-if="deleteVisible"
                 :is-visible="deleteVisible"
                 :info="contextData"
                 @close="visibleDelete(null, false)"/>

  <MarkdownPreview v-if="errorVisible && contextData"
                   :is-visible="errorVisible"
                   :content="'```java\n' + contextData.message + '\n```'"
                   @close="visibleError(null, false)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel, type TableFilters } from '@/model/filter'
import { useDatasetHeaders } from './DatasetUtils'
import { DatasetModel } from '@/model/dataset'
import { useUtil } from '@/utils/common'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import DatasetService from '@/services/dataset'
import DatasetState from '@/views/pages/admin/dataset/components/DatasetState.vue'
import DatasetSync from '@/views/pages/admin/dataset/DatasetSync.vue'
import DatasetHistory from '@/views/pages/admin/dataset/DatasetHistory.vue'
import MarkdownPreview from '@/views/components/markdown/MarkdownView.vue'
import DatasetRebuild from '@/views/pages/admin/dataset/DatasetRebuild.vue'
import DatasetClear from '@/views/pages/admin/dataset/DatasetClear.vue'
import DatasetDelete from '@/views/pages/admin/dataset/DatasetDelete.vue'
import { ChartColumn, CirclePause, CirclePlay, History, Info, RefreshCw, Settings, SquareX, Trash2, TriangleAlert } from '@lucide/vue'

defineOptions({ name: 'DatasetHome' })

const tableFilters = useTableFilters('dataset')
const filter: FilterModel = new FilterModel()
const { headers } = useDatasetHeaders()
const { getDatasetStateText } = useUtil()

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const contextData = ref<DatasetModel | null>(null)
const rebuildVisible = ref(false)
const historyVisible = ref(false)
const syncDataVisible = ref(false)
const clearDataVisible = ref(false)
const errorVisible = ref(false)
const deleteVisible = ref(false)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handleInitialize()
}

let latestRequest = 0
const handleInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  DatasetService.getAll({ ...filter })
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
  handleInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const isSuccess = (state: Array<any> | null) => {
  if (state && state.length > 0) {
    return state[state.length - 1].endsWith('SUCCESS')
  }
  return false
}

const getState = (state: Array<any> | null): string | null => {
  if (state && state.length > 0) {
    const last = state[state.length - 1]
    return last ? getDatasetStateText(String(last)) : null
  }
  return null
}

const visibleRebuild = (record: DatasetModel | null, opened: boolean) => {
  if (record && isSuccess(record.state)) {
    return
  }
  rebuildVisible.value = opened
  contextData.value = record
}

const visibleHistory = (record: DatasetModel | null, opened: boolean) => {
  contextData.value = record
  historyVisible.value = opened
}

const visibleSyncData = (record: DatasetModel | null, opened: boolean) => {
  if (record && !isSuccess(record.state)) {
    return
  }
  contextData.value = record
  syncDataVisible.value = opened
}

const visibleClearData = (record: DatasetModel | null, opened: boolean) => {
  if (record && !(record.totalRows > 0)) {
    return
  }
  contextData.value = record
  clearDataVisible.value = opened
  if (!opened) {
    handleInitialize()
  }
}

const visibleError = (record: DatasetModel | null, opened: boolean) => {
  errorVisible.value = opened
  contextData.value = record
}

const visibleDelete = (record: DatasetModel | null, opened: boolean) => {
  deleteVisible.value = opened
  contextData.value = record
  if (!opened) {
    handleInitialize()
  }
}

onMounted(() => handleInitialize())
</script>
