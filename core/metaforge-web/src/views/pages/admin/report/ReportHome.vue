<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('report.common.list')"
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
        <template v-else-if="column.key === 'realtime'">
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
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('report.common.view').replace('$VALUE', record.name)">
              <MfButton variant="ghost" size="sm" shape="circle" @click="visibleView(true, record)">
                <Eye :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click">
              <MfButton variant="ghost" size="sm" shape="circle">
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
import { useHeaders } from '@/views/pages/admin/report/ReportUtils'
import { ReportModel } from '@/model/report'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import ReportService from '@/services/report'
import ReportView from '@/views/pages/admin/report/ReportView.vue'
import ReportDelete from '@/views/pages/admin/report/ReportDelete.vue'
import { Eye, Pencil, Settings, Trash2 } from '@lucide/vue'

defineOptions({ name: 'ReportHome' })

const tableFilters = useTableFilters('report')
const { headers } = useHeaders()

const dataInfo = ref<ReportModel | null>(null)
const dataViewVisible = ref(false)
const dataDeleteVisible = ref(false)

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
  } = usePagedList<any>(filter => ReportService.getAll(filter))

const visibleView = (opened: boolean, value: ReportModel | null) => {
  dataViewVisible.value = opened
  dataInfo.value = value
}

const visibleDelete = (opened: boolean, value: ReportModel | null) => {
  dataDeleteVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    initialize()
  }
}

onMounted(() => initialize())
</script>
