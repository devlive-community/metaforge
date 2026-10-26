<template>
  <div class="relative min-h-screen">
    <MfSkeleton v-if="loading"/>

    <div v-else-if="dataInfo">
      <div class="mt-4 grid grid-cols-3 gap-5">
        <div class="flex items-center space-x-2">
          <FontSizeOutlined :style="{ fontSize: '16px' }"/>
          <span>{{ $t('common.'+ dataInfo.type_name) }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <TableOutlined :style="{ fontSize: '16px' }"/>
          <span>{{ dataInfo.object_name }}</span>
        </div>

        <MfTooltip :content="$t('common.createTime')">
          <div class="flex items-center space-x-2">
            <ClockCircleOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo?.object_create_time === null ? $t('source.common.notSpecified') : dataInfo.object_create_time }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('common.updateTime')">
          <div class="flex items-center space-x-2">
            <ClockCircleOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo?.object_update_time === null ? $t('source.common.notUpdated') : dataInfo.object_update_time }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.engine')">
          <div class="flex items-center space-x-2">
            <CalendarOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_engine === null ? $t('source.common.notSpecifiedEngine') : dataInfo.object_engine }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.collation')">
          <div class="flex items-center space-x-2">
            <ColumnHeightOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_collation === null ? $t('source.common.notSpecifiedCollation') : dataInfo.object_collation }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.totalRows')">
          <div class="flex items-center space-x-2">
            <SplitCellsOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_rows }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.format')">
          <div class="flex items-center space-x-2">
            <ClearOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_format === null ? $t('source.common.notSpecifiedFormat') : dataInfo.object_format }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.avgRowLength')">
          <div class="flex items-center space-x-2">
            <SortAscendingOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_avg_row_length === null ? 0 : dataInfo.object_avg_row_length }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.dataSize')">
          <div class="flex items-center space-x-2">
            <ColumnHeightOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_data_size === null ? 0 : dataInfo.object_data_size }} MB</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('source.common.indexSize')">
          <div class="flex items-center space-x-2">
            <SearchOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_index_size === null ? $t('source.common.notSpecifiedIndex') : dataInfo.object_index_size }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('common.columnCount')">
          <div class="flex items-center space-x-2">
            <InsertRowRightOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_column_count === null ? $t('source.common.notSpecifiedIndex') : dataInfo.object_column_count }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('common.indexCount')">
          <div class="flex items-center space-x-2">
            <SearchOutlined :style="{ fontSize: '16px' }"/>
            <span>{{ dataInfo.object_index_count === null ? $t('source.common.notSpecifiedIndex') : dataInfo.object_index_count }}</span>
          </div>
        </MfTooltip>

        <div class="flex items-center space-x-4 justify-between">
          <MfTooltip :content="$t('source.common.autoIncrement')">
            <div class="flex items-center space-x-2">
              <ColumnHeightOutlined :style="{ fontSize: '16px' }"/>
              <span>{{ dataInfo.object_auto_increment === null ? $t('source.common.notSpecifiedPrimaryKey') : dataInfo.object_auto_increment }}</span>
            </div>
          </MfTooltip>
          <div>
            <MfTooltip :content="$t('source.common.resetAutoIncrement')">
              <MfButton variant="ghost" size="sm" :disabled="dataInfo.object_auto_increment === null" @click="autoIncrement = true">
                <SettingOutlined :style="{ fontSize: '16px' }"/>
              </MfButton>
            </MfTooltip>
          </div>
        </div>

        <div class="col-span-3">
          <MfDivider class="mt-4 mb-4"/>

          <div class="mb-2">{{ $t('source.common.comment') }}</div>

          <MfTextarea v-model="dataInfo.object_comment"/>
        </div>
      </div>
    </div>
  </div>

  <TableAutoIncrement v-if="autoIncrement"
                      :visible="autoIncrement"
                      :info="dataInfo"
                      @close="autoIncrement = false"/>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata.ts'
import TableAutoIncrement from '@/views/pages/admin/source/components/TableAutoIncrement.vue'
import { CalendarOutlined, ClearOutlined, ClockCircleOutlined, ColumnHeightOutlined, FontSizeOutlined, InsertRowRightOutlined, SearchOutlined, SettingOutlined, SortAscendingOutlined, SplitCellsOutlined, TableOutlined } from '@ant-design/icons-vue'

defineOptions({ name: 'SourceTableInfo' })

const route = useRoute()

const loading = ref(false)
const autoIncrement = ref(false)
const dataInfo = ref<any | null>(null)

const handleInitialize = () => {
  const code = route?.params.source as string
  const database = route?.params.database as string
  const table = route?.params.table as string
  if (code && database && table) {
    loading.value = true
    MetadataService.getTable(code, database, table)
                   .then((response) => {
                     if (response.status && response.data && response.data.isSuccessful) {
                       dataInfo.value = response.data.columns[0] || null
                     }
                     else {
                       MfMessage.error(response.message)
                     }
                   })
                   .finally(() => (loading.value = false))
  }
}

watch(() => route?.params.table, () => handleInitialize())

handleInitialize()
</script>
