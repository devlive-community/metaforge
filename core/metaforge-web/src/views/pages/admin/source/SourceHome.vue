<template>

    <DataTable filterable :filter-fields="tableFilters" :title="$t('source.common.list')"
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
        <MfButton variant="primary" @click="visibleInfo(true, null)">
          <Plus :size="16" />
          {{ $t('source.common.create') }}
        </MfButton>
      </template>

      <!-- #bodyCell 透传给 DataTable（其内部仍为 a-table），保持原插槽契约 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <EllipsisCell :text="record.name"/>
        </template>
        <template v-else-if="column.key === 'type'">
          <MfTooltip :content="record.type">
            <MfAvatar class="cursor-pointer" size="sm" :src="'/static/images/plugin/' + record.type.toLowerCase() + '.svg'" :alt="record.type"/>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'public'">
          <MfSwitch v-model="record.public" disabled size="sm"/>
        </template>
        <template v-else-if="column.key === 'version'">
          <MfTooltip v-if="record.version" :content="record.version">
            <MfTag variant="info" class="cursor-pointer">{{ formatVersionText(record.version) }}</MfTag>
          </MfTooltip>
        </template>
        <template v-else-if="column.key === 'available'">
          <MfTooltip v-if="!record.available" :content="record.message">
            <CircleX :size="20" class="cursor-pointer text-red-500" />
          </MfTooltip>
          <CirclePlay :size="20" v-else="" class="text-green-500" />
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace :wrap="false" :size="2">
            <MfTooltip :content="$t('source.common.modify').replace('$NAME', record.name)">
              <MfButton variant="ghost" size="sm" :disabled="loginUserCode !== record.user.code" @click="visibleInfo(true, record)" shape="circle">
                <Pencil :size="15" />
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm" shape="circle">
                <Settings :size="15" />
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem v-if="(loginUserCode === record.user.code) && record.available" key_="manager">
                    <router-link :to="sourceManageLocation({ source: String(record.code) })" target="_blank">
                      <div class="flex items-center space-x-2">
                        <Settings :size="15" />
                        <span>{{ $t('source.common.manager') }}</span>
                      </div>
                    </router-link>
                  </MfMenuItem>
                  <MfMenuItem v-else key_="manager" disabled>
                    <div class="flex items-center space-x-2">
                      <Settings :size="15" />
                      <span>{{ $t('source.common.manager') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="delete"
                              :disabled="loginUserCode !== record.user.code"
                              @click="loginUserCode === record.user.code && visibleDelete(true, record)">
                    <div class="flex items-center space-x-2">
                      <Trash2 :size="15" />
                      <span>{{ $t('common.deleteData') }}</span>
                    </div>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </MfSpace>
        </template>
      </template>
    </DataTable>

  <SourceInfo v-if="dataInfoVisible"
              :is-visible="dataInfoVisible"
              :info="dataInfo"
              @close="visibleInfo(false, null)"/>

  <SourceDelete v-if="dataDeleteVisible"
                :is-visible="dataDeleteVisible"
                :info="dataInfo"
                @close="visibleDelete(false, null)"/>

  <SourceMetadata v-if="dataSyncMetadataVisible"
                  :is-visible="dataSyncMetadataVisible"
                  :info="dataInfo"
                  @close="visibleSyncMetadata(false, null)"/>

  <SourceHistory v-if="dataHistoryVisible"
                 :is-visible="dataHistoryVisible"
                 :info="dataInfo"
                 @close="visibleHistory(false, null)"/>
</template>

<script setup lang="ts">
import { sourceManageLocation } from '@/model/source-manage'
import { onMounted, ref } from 'vue'
import Common from '@/utils/common'
import { useHeaders } from '@/views/pages/admin/source/SourceUtils'
import { SourceModel } from '@/model/source'
import DataTable from '@/views/components/table/DataTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import { usePagedList } from '@/composables/usePagedList'
import SourceService from '@/services/source'
import SourceInfo from '@/views/pages/admin/source/SourceInfo.vue'
import SourceHistory from '@/views/pages/admin/source/SourceHistory.vue'
import SourceDelete from '@/views/pages/admin/source/SourceDelete.vue'
import SourceMetadata from '@/views/pages/admin/source/SourceMetadata.vue'
import { CirclePlay, CircleX, Pencil, Plus, Settings, Trash2 } from '@lucide/vue'

defineOptions({ name: 'SourceHome' })

const tableFilters = useTableFilters('source')
const { headers } = useHeaders()
const loginUserCode = Common.getCurrentUserCode()

const dataInfoVisible = ref(false)
const dataInfo = ref<SourceModel | null>(null)
const dataDeleteVisible = ref(false)
const dataSyncMetadataVisible = ref(false)
const dataHistoryVisible = ref(false)

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
  } = usePagedList<any>(filter => SourceService.getAll(filter))

const visibleInfo = (opened: boolean, value: SourceModel | null) => {
  dataInfoVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    initialize()
  }
}

const visibleDelete = (opened: boolean, value: SourceModel | null) => {
  dataDeleteVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    initialize()
  }
}

const visibleSyncMetadata = (opened: boolean, value: SourceModel | null) => {
  dataSyncMetadataVisible.value = opened
  dataInfo.value = value
}

const visibleHistory = (opened: boolean, value: SourceModel | null) => {
  dataHistoryVisible.value = opened
  dataInfo.value = value
}

const formatVersionText = (version: string, maxLength = 20) => {
  if (!version) {
    return '-'
  }
  if (version.length <= maxLength) {
    return version
  }
  const frontLength = Math.ceil((maxLength - 3) / 2)
  const backLength = Math.floor((maxLength - 3) / 2)
  return `${ version.substring(0, frontLength) }...${ version.substring(version.length - backLength) }`
}

onMounted(() => initialize())
</script>
