<template>
  <div class="mx-auto max-w-4xl">
    <div class="mb-5">
      <h1 class="m-0 text-xl font-semibold text-content">{{ $t('tools.export.title') }}</h1>
      <p class="mb-0 mt-1 text-sm text-content-secondary">{{ $t('tools.export.desc') }}</p>
    </div>

    <div class="rounded-mf-lg border border-line bg-surface p-5">
      <div class="grid grid-cols-3 gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.name') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="sourceCode" :options="sourceOptions" @update:model-value="onSourceChange"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.database') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="databaseName" :options="databaseOptions" @update:model-value="onDatabaseChange"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.tableName') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="tableName" :options="tableOptions"/>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.format') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="format" :options="formatOptions"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('tools.export.count') }}</label>
          <MfInput v-model="count" placeholder="5000"/>
        </div>
      </div>

      <div class="mt-5 flex items-center justify-end gap-3">
        <a v-if="downloadPath" :href="downloadPath" target="_blank"
           class="text-sm text-primary hover:underline">{{ $t('source.common.downloadFile') }}</a>
        <MfButton variant="primary" :loading="exporting" :disabled="!canExport" @click="onExport">
          <Download :size="14"/>
          {{ $t('tools.export.execute') }}
        </MfButton>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSourceCascade } from '@/composables/useSourceCascade'
import { MfMessage } from 'metaforge-ui'
import { Download } from '@lucide/vue'
import { FilterModel } from '@/model/filter'
import SourceService from '@/services/source'
import PluginService from '@/services/plugin'
import MetadataService from '@/services/metadata'

defineOptions({ name: 'ExportHome' })

const { t } = useI18n()

const { sourceCode, databaseName, tableName, sourceOptions, databaseOptions, tableOptions } = useSourceCascade()
const format = ref('JsonConvert')
const formatOptions = ref<{ label: string; value: string }[]>([])
const count = ref('5000')
const downloadPath = ref('')
const exporting = ref(false)

const canExport = computed(() => !!(sourceCode.value && databaseName.value && tableName.value))

const loadDatabases = () => {
  databaseName.value = ''
  tableOptions.value = []
  databaseOptions.value = []
  if (!sourceCode.value) return
  MetadataService.getDatabaseBySource(sourceCode.value)
                 .then(response => {
                   if (response.status) {
                     databaseOptions.value = (response.data?.columns || [])
                         .map((item: any) => ({ label: item.object_name || item.schema_name, value: item.object_name || item.schema_name }))
                         .filter((item: any) => item.value)
                   }
                 })
}

const loadTables = () => {
  tableName.value = ''
  tableOptions.value = []
  if (!sourceCode.value || !databaseName.value) return
  MetadataService.getTablesByDatabase(sourceCode.value, databaseName.value)
                 .then(response => {
                   if (response.status) {
                     tableOptions.value = (response.data?.columns || [])
                         .filter((item: any) => item.type_name === 'table')
                         .map((item: any) => ({ label: item.object_name, value: item.object_name }))
                   }
                 })
}

const onSourceChange = () => loadDatabases()
const onDatabaseChange = () => loadTables()

const onExport = () => {
  exporting.value = true
  downloadPath.value = ''
  MetadataService.exportData(sourceCode.value, databaseName.value, tableName.value, {
    format: format.value,
    pagination: { page: 1, size: Number(count.value) || 5000 }
  })
                 .then(response => {
                   if (response.status) {
                     downloadPath.value = String(response.data || '')
                     MfMessage.success(t('tools.export.success'))
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (exporting.value = false))
}

SourceService.getAll(new FilterModel())
             .then(response => {
               if (response.status) {
                 sourceOptions.value = (response.data.content || [])
                     .map((item: any) => ({ label: item.name, value: item.code }))
               }
             })

PluginService.getPlugins()
             .then(response => {
               if (response.status && response.data) {
                 formatOptions.value = response.data
                     .filter((item: any) => item.type === 'CONVERT')
                     .map((item: any) => ({ label: item.name, value: item.name }))
               }
             })

</script>
