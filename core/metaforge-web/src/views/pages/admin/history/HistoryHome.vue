<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('common.history')"
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
        <template v-if="column.key === 'source'">
          <EllipsisCell :text="record?.source?.name"/>
        </template>
        <template v-else-if="column.key === 'type'">
          <MfTooltip v-if="record?.source?.type" :content="record.source.type">
            <MfAvatar size="sm"
                      :src="'/static/images/plugin/' + record.source.type.toLowerCase() + '.svg'"
                      :alt="record.source.type"
                      class="cursor-pointer"/>
          </MfTooltip>
          <span v-else class="text-content-tertiary">-</span>
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
          <MfTag :variant="record.state === 'SUCCESS' ? 'success' : 'danger'">
            {{ record.state === 'SUCCESS' ? $t('common.success') : $t('common.failure') }}
          </MfTag>
        </template>
        <template v-else-if="column.key === 'format'">
          <MfTooltip v-if="record?.format" :content="record?.format">
            <MfAvatar size="sm"
                      :src="'/static/images/convert/' + record.format.replace('Convert', '').toLowerCase() + '.svg'"
                      :alt="record.format"
                      class="cursor-pointer"/>
          </MfTooltip>
          <span v-else class="text-content-tertiary">-</span>
        </template>
        <template v-else-if="column.key === 'content'">
          <EllipsisCell :text="record?.content" class="font-mono text-xs"/>
        </template>
        <template v-else-if="column.key === 'user'">
          <div v-if="record?.user" class="flex items-center gap-2">
            <MfAvatar size="xs" :src="record.user.avatarConfigure?.path" :alt="record.user.username"/>
            <span class="min-w-0 truncate text-xs">{{ record.user.username }}</span>
          </div>
          <span v-else class="text-content-tertiary">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('common.error')">
              <MfButton variant="ghost"
                        size="sm"
                        shape="circle" class="text-danger!"
                        :disabled="record.state === 'SUCCESS'"
                        @click="handlerShowContent(true, record?.message)">
                <TriangleAlert :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm" shape="circle">
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

  <SqlInfo v-if="contentVisible"
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
import AuditService from '@/services/audit'
import { useHeaders } from '@/views/pages/admin/history/HistoryUtils'
import { HistoryModel } from '@/model/history'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import HistoryData from '@/views/pages/admin/history/HistoryData.vue'
import { ChevronRight, EllipsisVertical, FileCode2, Table, TriangleAlert } from '@lucide/vue'

defineOptions({ name: 'HistoryHome' })

const tableFilters = useTableFilters('history')
const { headers } = useHeaders()

const contentVisible = ref(false)
const dataInfo = ref<HistoryModel | null>(null)
const dataVisible = ref(false)
const content = ref<string | null>(null)

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
  } = usePagedList<any>(filter => AuditService.getAll(filter))

const handlerShowContent = (opened: boolean, value: string | null) => {
  contentVisible.value = opened
  content.value = value
}

const handlerShowData = (opened: boolean, value: HistoryModel | null) => {
  dataVisible.value = opened
  dataInfo.value = value
}

onMounted(() => initialize())
</script>
