<template>

  <DataTable filterable :filter-fields="tableFilters"
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
      <MfButton variant="primary" @click="openWizard()">
        <Plus :size="16"/>
        {{ $t('dataset.common.create') }}
      </MfButton>
    </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="flex min-w-0 items-center gap-2">
            <Database :size="16" class="shrink-0 text-primary"/>
            <router-link :to="`/dataset/detail/${record.code}`" class="min-w-0 truncate font-medium text-content hover:text-primary">{{ record.name }}</router-link>
          </div>
        </template>
        <template v-else-if="column.key === 'source'">
          <MfTooltip :content="record.source?.name">
            <span class="flex min-w-0 items-center gap-1.5">
              <MfAvatar size="xs" :src="`/static/images/plugin/${ String(record.source?.type ?? '').toLowerCase() }.svg`" :alt="record.source?.type"/>
              <span class="min-w-0 truncate text-content-secondary">{{ record.source?.name }}</span>
            </span>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'columnCount'"><MfLoadingIcon v-if="fieldCounts[record.code] === undefined" :size="14"/><span v-else>{{ fieldCounts[record.code] ?? '—' }}</span></template>
        <template v-else-if="column.key === 'totalRows'"><span>{{ record.totalRows ?? '—' }}</span></template>
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
          <!-- An unfinished sync: its state, progress and write rate, linking to the run -->
          <router-link v-if="activeSyncs[record.code]" :to="`/dataset/runs/${record.code}?run=${activeSyncs[record.code].historyId}`" class="mt-1 block min-w-0">
            <span class="flex items-center justify-between gap-2 text-xs text-content-secondary">
              <span class="shrink-0">{{ $t('dataTable.state' + activeSyncs[record.code].state) }}</span>
              <span class="min-w-0 truncate tabular-nums">{{ syncRate(activeSyncs[record.code]) }}</span>
            </span>
            <span class="mt-1 block h-1 overflow-hidden rounded-mf-full bg-surface-active">
              <span class="block h-full rounded-mf-full bg-primary transition-[width]" :class="activeSyncs[record.code].progress < 0 && 'w-full animate-pulse'"
                    :style="activeSyncs[record.code].progress >= 0 ? { width: activeSyncs[record.code].progress + '%' } : undefined"/>
            </span>
          </router-link>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('dataset.common.info')"><router-link :to="`/dataset/detail/${record.code}`"><MfButton variant="ghost" size="sm" :aria-label="$t('dataset.common.info')" shape="circle"><Eye :size="15"/></MfButton></router-link></MfTooltip>
            <MfTooltip :content="$t('dataset.common.modify')"><MfButton variant="ghost" size="sm" :aria-label="$t('dataset.common.modify')" @click="openWizard(record.code)" shape="circle"><Pencil :size="15"/></MfButton></MfTooltip>
            <MfTooltip :content="$t('dataset.common.adhoc')">
              <router-link v-if="tableReady(record?.state)" :to="`/dataset/adhoc/${ record?.code }`" target="_blank">
                <MfButton variant="ghost" size="sm" shape="circle">
                  <ChartColumn :size="15" />
                </MfButton>
              </router-link>
              <MfButton v-else variant="ghost" size="sm" disabled shape="circle">
                <ChartColumn :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm" shape="circle">
                <Settings :size="15" />
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem key_="info">
                    <router-link :to="`/dataset/detail/${ record?.code }`" target="_blank" class="flex items-center">
                      <Info :size="15" />
                      <span class="ml-2">{{ $t('dataset.common.info') }}</span>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem key_="sync" :disabled="!tableReady(record?.state)" @click="visibleSyncData(record, true)">
                    <div class="flex items-center space-x-2">
                      <RefreshCw :size="15" />
                      <span>{{ $t('dataset.common.syncData') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="history" @click="router.push(`/dataset/runs/${record.code}`)">
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

                  <MfMenuItem key_="rebuild" :disabled="tableReady(record?.state)" @click="visibleRebuild(record, true)">
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
  <DatasetWizard v-if="wizardVisible" embedded :dataset-code="editingCode" @close="wizardVisible = false" @submitted="onSubmitted"/>

  <DatasetRebuild v-if="rebuildVisible"
                  :is-visible="rebuildVisible"
                  :data="contextData"
                  @close="visibleRebuild(null, false)"/>

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
import { onMounted, ref, onBeforeUnmount } from 'vue'
import { metricText } from '@/model/workflow-metrics'
import { useDatasetHeaders } from './DatasetUtils'
import { DatasetModel } from '@/model/dataset'
import { useUtil } from '@/utils/common'
import DataTable from '@/views/components/table/DataTable.vue'
import { usePagedList } from '@/composables/usePagedList'
import { useTableFilters } from '@/views/components/table/tableFilters'
import DatasetWizard from './DatasetWizard.vue'
import { useRouter } from 'vue-router'
import DatasetService from '@/services/dataset'
import DatasetState from '@/views/pages/admin/dataset/components/DatasetState.vue'
import DatasetSync from '@/views/pages/admin/dataset/DatasetSync.vue'
import MarkdownPreview from '@/views/components/markdown/MarkdownView.vue'
import DatasetRebuild from '@/views/pages/admin/dataset/DatasetRebuild.vue'
import DatasetClear from '@/views/pages/admin/dataset/DatasetClear.vue'
import DatasetDelete from '@/views/pages/admin/dataset/DatasetDelete.vue'
import { ChartColumn, CirclePause, CirclePlay, History, Database, Eye, Info, Pencil, Plus, RefreshCw, Settings, SquareX, Trash2, TriangleAlert } from '@lucide/vue'

defineOptions({ name: 'DatasetHome' })

const router = useRouter()
const wizardVisible = ref(false)
const editingCode = ref('')
const fieldCounts = ref<Record<string, number | null>>({})
const openWizard = (code = '') => { editingCode.value = code; wizardVisible.value = true }
const onSubmitted = (result: {code:string}) => { wizardVisible.value = false; router.push(result.code ? `/dataset/detail/${encodeURIComponent(result.code)}?submitted=1` : '/dataset') }
const tableFilters = useTableFilters('dataset')
const { headers } = useDatasetHeaders()
const { getDatasetStateText } = useUtil()

const contextData = ref<DatasetModel | null>(null)
const rebuildVisible = ref(false)
const syncDataVisible = ref(false)
const clearDataVisible = ref(false)
const errorVisible = ref(false)
const deleteVisible = ref(false)

let fieldCountRequest = 0
const loadFieldCounts = (rows: any[]) => {
  const request = ++fieldCountRequest
  fieldCounts.value = {}
  Promise.allSettled(rows.map(async (record) => {
    try {
      const fields = await DatasetService.getColumnsByCode(record.code)
      if (request === fieldCountRequest) {
        fieldCounts.value[record.code] = fields.status ? fields.data.length : null
        record.columnCount = fieldCounts.value[record.code]
      }
    }
    catch {
      if (request === fieldCountRequest) {
        fieldCounts.value[record.code] = null
      }
    }
  }))
}

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
  } = usePagedList<any>(filter => DatasetService.getAll(filter), {
    onData: rows => {
      loadFieldCounts(rows)
      loadActiveSyncs()
    }
  })

// Progress of unfinished syncs, refreshed while there are any; the list reloads once they end.
const ACTIVE_REFRESH_MS = 3000
interface ActiveSync { datasetCode: string; historyId: number; state: string; progress: number; rowsWritten: number; writeRowsPerSecond: number }
const activeSyncs = ref<Record<string, ActiveSync>>({})
let activeTimer: ReturnType<typeof setTimeout> | undefined
let activeRequest = 0
const syncRate = (sync: ActiveSync) => sync.state === 'RUNNING' ? metricText('writeRowsPerSecond', sync) : undefined
async function loadActiveSyncs()
{
  const request = ++activeRequest
  clearTimeout(activeTimer)
  const hadActive = Object.keys(activeSyncs.value).length > 0
  const response = await DatasetService.getActiveSyncs().catch(() => null)
  if (request !== activeRequest || !response?.status) {
    return
  }
  const next: Record<string, ActiveSync> = {}
  for (const sync of response.data || []) {
    next[sync.datasetCode] = sync
  }
  activeSyncs.value = next
  if (Object.keys(next).length > 0) {
    activeTimer = setTimeout(() => (document.hidden ? (activeTimer = setTimeout(loadActiveSyncs, ACTIVE_REFRESH_MS)) : loadActiveSyncs()), ACTIVE_REFRESH_MS)
  }
  else if (hadActive) {
    initialize()
  }
}
onBeforeUnmount(() => {
  activeRequest++
  clearTimeout(activeTimer)
})

/** Ad hoc queries and syncs need the dataset's table; a failed sync leaves it usable. */
const tableReady = (state: Array<any> | null) => !!state?.includes('TABLE_SUCCESS')

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

// Rebuilding recreates the metadata and table, so it is offered only when the build failed.
const visibleRebuild = (record: DatasetModel | null, opened: boolean) => {
  if (record && tableReady(record.state)) {
    return
  }
  rebuildVisible.value = opened
  contextData.value = record
}


const visibleSyncData = (record: DatasetModel | null, opened: boolean) => {
  if (record && !tableReady(record.state)) {
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
    initialize()
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
    initialize()
  }
}

onMounted(() => initialize())
</script>
