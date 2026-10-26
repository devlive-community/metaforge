<template>

    <DataTable :title="$t('dataset.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handleInitialize"
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
              <router-link v-if="isSuccess(record?.state)" :to="`/admin/dataset/adhoc/${ record?.code }`" target="_blank">
                <MfButton variant="ghost" size="sm">
                  <BarChartOutlined :style="{ fontSize: '15px' }"/>
                </MfButton>
              </router-link>
              <MfButton v-else variant="ghost" size="sm" disabled>
                <BarChartOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm">
                <SettingOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem key_="info">
                    <router-link :to="`/admin/dataset/info/${ record?.code }`" target="_blank" class="flex items-center">
                      <InfoCircleOutlined :style="{ fontSize: '15px' }"/>
                      <span class="ml-2">{{ $t('dataset.common.info') }}</span>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem key_="sync" :disabled="!isSuccess(record?.state)" @click="visibleSyncData(record, true)">
                    <div class="flex items-center space-x-2">
                      <SyncOutlined :style="{ fontSize: '15px' }"/>
                      <span>{{ $t('dataset.common.syncData') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="history" @click="visibleHistory(record, true)">
                    <div class="flex items-center space-x-2">
                      <HistoryOutlined :style="{ fontSize: '15px' }"/>
                      <span>{{ $t('dataset.common.history') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="error" :disabled="isSuccess(record?.state)" @click="!isSuccess(record?.state) && visibleError(record, true)">
                    <div class="flex items-center space-x-2">
                      <WarningOutlined :style="{ fontSize: '15px' }"/>
                      <span>{{ $t('dataset.common.error') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="rebuild" :disabled="isSuccess(record?.state)" @click="visibleRebuild(record, true)">
                    <div class="flex items-center space-x-2">
                      <component :is="record?.state === 'SUCCESS' ? PlayCircleOutlined : PauseCircleOutlined" :style="{ fontSize: '15px' }"/>
                      <span>{{ $t('dataset.common.rebuild') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="clear" :disabled="!(record?.totalRows > 0)" @click="visibleClearData(record, true)">
                    <div class="flex items-center space-x-2">
                      <CloseSquareOutlined :style="{ fontSize: '15px' }"/>
                      <span>{{ $t('dataset.common.clearData') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="delete" @click="visibleDelete(record, true)">
                    <div class="flex items-center space-x-2">
                      <DeleteOutlined :style="{ fontSize: '15px' }"/>
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
import { FilterModel } from '@/model/filter'
import { useDatasetHeaders } from './DatasetUtils'
import { DatasetModel } from '@/model/dataset'
import { useUtil } from '@/utils/common'
import DataTable from '@/views/components/table/DataTable.vue'
import DatasetService from '@/services/dataset'
import DatasetState from '@/views/pages/admin/dataset/components/DatasetState.vue'
import DatasetSync from '@/views/pages/admin/dataset/DatasetSync.vue'
import DatasetHistory from '@/views/pages/admin/dataset/DatasetHistory.vue'
import MarkdownPreview from '@/views/components/markdown/MarkdownView.vue'
import DatasetRebuild from '@/views/pages/admin/dataset/DatasetRebuild.vue'
import DatasetClear from '@/views/pages/admin/dataset/DatasetClear.vue'
import DatasetDelete from '@/views/pages/admin/dataset/DatasetDelete.vue'
import { BarChartOutlined, CloseSquareOutlined, DeleteOutlined, HistoryOutlined, InfoCircleOutlined, PauseCircleOutlined, PlayCircleOutlined, SettingOutlined, SyncOutlined, WarningOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'DatasetHome' })

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

const handleInitialize = () => {
  loading.value = true
  DatasetService.getAll(filter)
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
