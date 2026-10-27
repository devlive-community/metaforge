<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('source.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handlerInitialize"
               @search="onSearch"
               @reset="onSearch"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <span>{{ record?.source?.name }}</span>
        </template>
        <template v-else-if="column.key === 'type'">
          <MfTooltip :content="record?.source?.type">
            <MfAvatar size="sm"
                      :src="'/static/images/plugin/' + record?.source?.type.toLowerCase() + '.svg'"
                      :alt="record?.source?.type"
                      class="cursor-pointer"/>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'mode'">
          <MfTag v-if="record.mode === 'ADHOC'">{{ $t('common.adhoc') }}</MfTag>
          <MfTag v-else-if="record.mode === 'HISTORY'">{{ $t('common.history') }}</MfTag>
          <MfTag v-else-if="record.mode === 'REPORT'">{{ $t('common.report') }}</MfTag>
          <MfTag v-else-if="record.mode === 'SNIPPET'">{{ $t('common.snippet') }}</MfTag>
          <MfTag v-else-if="record.mode === 'DATASET'">{{ $t('common.dataset') }}</MfTag>
          <MfTag v-else>{{ record.mode }}</MfTag>
        </template>
        <template v-else-if="column.key === 'state'">
          <MfTag :variant="record.state === 'SUCCESS' ? 'success' : 'danger'">{{ record.state }}</MfTag>
        </template>
        <template v-else-if="column.key === 'format'">
          <MfTooltip :content="record?.format">
            <MfAvatar size="sm"
                      :src="'/static/images/convert/' + record?.format.replace('Convert', '').toLowerCase() + '.svg'"
                      :alt="record?.source?.type"
                      class="cursor-pointer"/>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('common.error')">
              <MfButton variant="ghost"
                        size="sm"
                        class="rounded-full! text-danger!"
                        :disabled="record.state === 'SUCCESS'"
                        @click="handlerShowContent(true, record?.message)">
                <TriangleAlert :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm" class="rounded-full!">
                <EllipsisVertical :size="15" />
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem>
                    <router-link :to="`/query/history/${ record?.code }`" target="_blank">
                      <div class="flex items-center space-x-2">
                        <FileCode2 :size="15" />
                        <span>{{ $t('query.common.quoteRecord') }}</span>
                      </div>
                    </router-link>
                  </MfMenuItem>

                  <MfMenuItem @click="handlerShowContent(true, record?.content)">
                    <div class="flex items-center space-x-2">
                      <ChevronRight :size="15" />
                      <span>{{ $t('query.common.showSql') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem @click="handlerShowData(true, record)">
                    <div class="flex items-center space-x-2">
                      <Table :size="15" />
                      <span>{{ $t('query.common.historyData') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <SqlInfo v-if="contentVisible && content"
           :is-visible="contentVisible"
           :content="content"
           @close="handlerShowContent(false, null)"/>

  <HistoryData v-if="dataVisible && dataInfo"
               :is-visible="dataVisible"
               :info="dataInfo"
               @close="handlerShowData(false, null)"/>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FilterModel, type TableFilters } from '@/model/filter.ts'
import AuditService from '@/services/audit'
import { useHeaders } from '@/views/pages/admin/history/HistoryUtils'
import { HistoryModel } from '@/model/history'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import HistoryData from '@/views/pages/admin/history/HistoryData.vue'
import { ChevronRight, EllipsisVertical, FileCode2, Table, TriangleAlert } from '@lucide/vue'

defineOptions({ name: 'HistoryHome' })

const tableFilters = useTableFilters('history')
const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const contentVisible = ref(false)
const dataInfo = ref<HistoryModel | null>(null)
const dataVisible = ref(false)
const content = ref<string | null>(null)

const onSearch = (values: TableFilters) => {
  Object.assign(filter, values, { page: 1 })
  pageIndex.value = 1
  handlerInitialize()
}

let latestRequest = 0
const handlerInitialize = () => {
  const request = ++latestRequest
  loading.value = true
  AuditService.getAll({ ...filter })
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
  handlerInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const handlerShowContent = (opened: boolean, value: string | null) => {
  contentVisible.value = opened
  content.value = value
}

const handlerShowData = (opened: boolean, value: HistoryModel | null) => {
  dataVisible.value = opened
  dataInfo.value = value
}

onMounted(() => handlerInitialize())
</script>
