<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('report.common.list')"
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
        <template v-if="column.key === 'realtime'">
          <MfSwitch v-model="record.realtime" size="sm" :disabled="record.realtime"/>
        </template>
        <template v-else-if="column.key === 'source'">
          <MfTooltip v-if="record.source" :content="record.source?.type">
            <MfAvatar size="sm" :src="'/static/images/plugin/' + record.source?.type.toLowerCase() + '.svg'" :alt="record.source?.type"/>
          </MfTooltip>
          <MfTooltip v-else :content="record.dataset?.name">
            <MfTag>{{ $t('common.dataset') }}</MfTag>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('report.common.view').replace('$VALUE', record.name)">
              <MfButton variant="ghost" size="sm" class="rounded-full!" @click="visibleView(true, record)">
                <Eye :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click">
              <MfButton variant="ghost" size="sm" class="rounded-full!">
                <Settings :size="15" />
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem :disabled="record.type === 'QUERY'">
                    <router-link v-if="record.type !== 'QUERY'"
                                 :to="`/dataset/adhoc/${ record.dataset?.code }/${ record.code }`"
                                 target="_blank">
                      <div class="flex items-center space-x-2">
                        <Pencil :size="15" />
                        <span>{{ $t('report.common.modify') }}</span>
                      </div>
                    </router-link>
                    <div v-else class="flex items-center space-x-2">
                      <Pencil :size="15" />
                      <span>{{ $t('report.common.modify') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem @click="visibleDelete(true, record)">
                    <div class="flex items-center space-x-2">
                      <Trash2 :size="15" />
                      <span>{{ $t('report.common.delete') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <ReportView v-if="dataViewVisible"
              :is-visible="dataViewVisible"
              :info="dataInfo"
              @close="visibleView(false, null)"/>

  <ReportDelete v-if="dataDeleteVisible"
                :is-visible="dataDeleteVisible"
                :info="dataInfo"
                @close="visibleDelete(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { FilterModel, type TableFilters } from '@/model/filter'
import { useHeaders } from '@/views/pages/admin/report/ReportUtils'
import { ReportModel } from '@/model/report'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import ReportService from '@/services/report'
import ReportView from '@/views/pages/admin/report/ReportView.vue'
import ReportDelete from '@/views/pages/admin/report/ReportDelete.vue'
import { Eye, Pencil, Settings, Trash2 } from '@lucide/vue'

defineOptions({ name: 'ReportHome' })

const tableFilters = useTableFilters('report')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfo = ref<ReportModel | null>(null)
const dataViewVisible = ref(false)
const dataDeleteVisible = ref(false)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handleInitialize()
}

let latestRequest = 0
const handleInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  ReportService.getAll({ ...filter })
               .then(response => {
                 if (request !== latestRequest) return
                 if (response.status) {
                   data.value = response.data.content
                   dataCount.value = response.data.total
                   pageSize.value = response.data.size
                   pageIndex.value = response.data.page
                 }
                 else {
                   MfMessage.error(response.message)
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

const visibleView = (opened: boolean, value: ReportModel | null) => {
  dataViewVisible.value = opened
  dataInfo.value = value
}

const visibleDelete = (opened: boolean, value: ReportModel | null) => {
  dataDeleteVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    handleInitialize()
  }
}

onMounted(() => handleInitialize())
</script>
