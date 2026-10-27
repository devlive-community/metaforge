<template>
  <div class="relative min-h-screen">
    <div v-if="loading" class="flex items-center justify-center py-8">
      <MfSpinner size="lg" label="Loading..."/>
    </div>

    <div v-else-if="dataInfo">
      <div class="mt-4 grid grid-cols-3 gap-5">
        <div class="flex items-center space-x-2">
          <Database :size="16" />
          <span>{{ dataInfo.object_name || dataInfo.schema_name || dataInfo.SCHEMA_NAME }}</span>
        </div>

        <MfTooltip :content="$t('common.createTime')">
          <div class="flex items-center space-x-2">
            <Clock :size="16" />
            <span>{{ dataInfo.object_create_time === null ? $t('source.common.notSpecified') : dataInfo.object_create_time }}</span>
          </div>
        </MfTooltip>

        <MfTooltip :content="$t('common.updateTime')">
          <div class="flex items-center space-x-2">
            <Clock :size="16" />
            <span>{{ dataInfo.object_update_time === null ? $t('source.common.notUpdated') : dataInfo.object_update_time }}</span>
          </div>
        </MfTooltip>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.charset') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_charset }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.collation') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_collation }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.dataSize') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_data_size }} MB</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.indexSize') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_index_size }} MB</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.totalSize') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_total_size }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.tableCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_table_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.columnCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_column_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.indexCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_index_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.viewCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_view_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.procedureCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_procedure_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.triggerCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_trigger_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.foreignKeyCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_foreign_key_count }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <span>{{ $t('common.totalCount') }}</span>
          <span class="text-content-tertiary">{{ dataInfo.object_total_rows }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata.ts'
import { Clock, Database } from '@lucide/vue'

defineOptions({ name: 'SourceDatabase' })

const route = useRoute()

const loading = ref(false)
const dataInfo = ref<any | null>(null)

const handleInitialize = () => {
  const code = route?.params.source as string
  const database = route?.params.database as string
  if (code && database) {
    loading.value = true
    MetadataService.getDatabase(code, database)
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

watch(() => route?.params.database, () => handleInitialize())

handleInitialize()
</script>
