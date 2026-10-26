<template>

    <DataTable :title="$t('source.common.list')"
               :columns="headers"
               :data-source="data"
               :loading="loading"
               :page-index="pageIndex"
               :page-size="pageSize"
               :total="dataCount"
               @refresh="handlerInitialize"
               @page-change="onPageChange"
               @size-change="onSizeChange">
      <template #actions>
        <MfButton variant="primary" @click="visibleInfo(true, null)">
          <PlusOutlined :style="{ fontSize: '16px' }"/>
          {{ $t('source.common.create') }}
        </MfButton>
      </template>

      <!-- #bodyCell 透传给 DataTable（其内部仍为 a-table），保持原插槽契约 -->
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'type'">
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
            <CloseCircleOutlined class="cursor-pointer text-red-500" :style="{ fontSize: '20px' }"/>
          </MfTooltip>
          <PlayCircleOutlined v-else="" class="text-green-500" :style="{ fontSize: '20px' }"/>
        </template>
        <template v-else-if="column.key === 'action'">
          <MfSpace>
            <MfTooltip :content="$t('source.common.modify').replace('$NAME', record.name)">
              <MfButton variant="ghost" size="sm" :disabled="loginUserCode !== record.user.code" @click="visibleInfo(true, record)">
                <EditOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>
            </MfTooltip>

            <MfDropdown trigger="click" placement="bottomRight">
              <MfButton variant="ghost" size="sm">
                <SettingOutlined :style="{ fontSize: '15px' }"/>
              </MfButton>

              <template #overlay>
                <MfMenu>
                  <MfMenuItem v-if="(loginUserCode === record.user.code) && record.available && record.isSupportMeta" key_="manager">
                    <router-link :to="`/admin/source/${ record?.code }`" target="_blank">
                      <div class="flex items-center space-x-2">
                        <SettingOutlined :style="{ fontSize: '15px' }"/>
                        <span>{{ $t('source.common.manager') }}</span>
                      </div>
                    </router-link>
                  </MfMenuItem>
                  <MfMenuItem v-else key_="manager" disabled>
                    <div class="flex items-center space-x-2">
                      <SettingOutlined :style="{ fontSize: '15px' }"/>
                      <span>{{ $t('source.common.manager') }}</span>
                    </div>
                  </MfMenuItem>

                  <MfMenuItem key_="delete"
                              :disabled="loginUserCode !== record.user.code"
                              @click="loginUserCode === record.user.code && visibleDelete(true, record)">
                    <div class="flex items-center space-x-2">
                      <DeleteOutlined :style="{ fontSize: '15px' }"/>
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
import { onMounted, ref } from 'vue'
import Common from '@/utils/common'
import { useHeaders } from '@/views/pages/admin/source/SourceUtils'
import { FilterModel } from '@/model/filter'
import { SourceModel } from '@/model/source'
import DataTable from '@/views/components/table/DataTable.vue'
import SourceService from '@/services/source'
import SourceInfo from '@/views/pages/admin/source/SourceInfo.vue'
import SourceHistory from '@/views/pages/admin/source/SourceHistory.vue'
import SourceDelete from '@/views/pages/admin/source/SourceDelete.vue'
import SourceMetadata from '@/views/pages/admin/source/SourceMetadata.vue'
import { CloseCircleOutlined, DeleteOutlined, EditOutlined, PlayCircleOutlined, PlusOutlined, SettingOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'SourceHome' })

const filter: FilterModel = new FilterModel()
const { headers } = useHeaders()
const loginUserCode = Common.getCurrentUserCode()

const loading = ref(false)
const data = ref<any[]>([])
const pageIndex = ref(1)
const pageSize = ref(10)
const dataCount = ref(0)
const dataInfoVisible = ref(false)
const dataInfo = ref<SourceModel | null>(null)
const dataDeleteVisible = ref(false)
const dataSyncMetadataVisible = ref(false)
const dataHistoryVisible = ref(false)

const handlerInitialize = () => {
  loading.value = true
  SourceService.getAll(filter)
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
  handlerInitialize()
}

const onPageChange = (value: number) => fetchData(value)

const onSizeChange = (_current: number, size: number) => {
  pageSize.value = size
  fetchData(pageIndex.value)
}

const visibleInfo = (opened: boolean, value: SourceModel | null) => {
  dataInfoVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    handlerInitialize()
  }
}

const visibleDelete = (opened: boolean, value: SourceModel | null) => {
  dataDeleteVisible.value = opened
  dataInfo.value = value
  if (!opened) {
    handlerInitialize()
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

onMounted(() => handlerInitialize())
</script>
