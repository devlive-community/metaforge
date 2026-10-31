<template>
  <div class="mx-auto max-w-4xl">
    <div class="mb-5">
      <h1 class="m-0 text-xl font-semibold text-content">{{ $t('tools.import.title') }}</h1>
      <p class="mb-0 mt-1 text-sm text-content-secondary">{{ $t('tools.import.desc') }}</p>
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

      <div class="mt-4">
        <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('common.content') }}</label>
        <AceEditor :value="pasteText" height="160px" @update:value="pasteText = $event"/>
        <div class="mt-2 flex items-center justify-between">
          <span class="text-xs text-content-tertiary">{{ $t('tools.import.pasteTip') }}</span>
          <label class="cursor-pointer text-xs text-primary hover:underline">
            {{ $t('function.import.chooseFile') }}
            <input type="file" accept=".json,.csv,.tsv,.txt" class="hidden" @change="onPickFile"/>
          </label>
        </div>
      </div>

      <template v-if="document.rows.length > 0">
        <div class="mb-2 mt-5 text-sm font-medium text-content">{{ $t('tools.import.preview') }}</div>
        <MfTable :columns="previewHeaders" :data-source="previewRows" row-key="__index">
          <template #cell-length="{ record }">
            <span class="font-mono text-xs">{{ record.__row?.join(' | ') }}</span>
          </template>
        </MfTable>
      </template>

      <div class="mt-5 flex justify-end">
        <MfButton variant="primary" :loading="importing" :disabled="!canImport" @click="onImport">
          <Upload :size="14"/>
          {{ $t('tools.import.execute') }}
        </MfButton>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSourceCascade } from '@/composables/useSourceCascade'
import { MfMessage } from 'metaforge-ui'
import { Upload } from '@lucide/vue'
import { FilterModel } from '@/model/filter'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'
import { parseFunctionDocument } from '@/views/pages/system/function/importParse'
import AceEditor from '@/views/components/editor/AceEditor.vue'

defineOptions({ name: 'ImportHome' })

const { t } = useI18n()

const { sourceCode, databaseName, tableName, sourceOptions, databaseOptions, tableOptions } = useSourceCascade()
const pasteText = ref('')
const document = ref<{ columns: string[]; rows: string[][] }>({ columns: [], rows: [] })
const importing = ref(false)

const canImport = computed(() => !!(sourceCode.value && databaseName.value && tableName.value && document.value.rows.length > 0))
const previewHeaders = computed(() => document.value.columns.map(column => ({ title: column, key: column, width: 140 })))
const previewRows = computed(() => document.value.rows.slice(0, 5).map((row, index) => ({
  __index: index + 1,
  __row: row,
  ...Object.fromEntries(document.value.columns.map((column, columnIndex) => [column, row[columnIndex]]))
})))

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

const applyDocument = (text: string) => {
  try {
    document.value = parseFunctionDocument(text)
  }
  catch {
    MfMessage.error(t('function.import.parseFailed'))
  }
}

const onPickFile = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    file.text().then(text => applyDocument(text))
  }
  input.value = ''
}

const onImport = () => {
  importing.value = true
  const rows = document.value.rows.map(row => Object.fromEntries(document.value.columns.map((column, index) => [column, row[index] ?? null])))
  MetadataService.insertData(sourceCode.value, databaseName.value, tableName.value, {
    preview: false,
    rows
  })
                 .then(response => {
                   if (response.status && response.data?.isSuccessful) {
                     MfMessage.success(t('tools.import.success').replace('{count}', String(rows.length)))
                     pasteText.value = ''
                     document.value = { columns: [], rows: [] }
                   }
                   else {
                     MfMessage.error(response.data?.message || response.message)
                   }
                 })
                 .finally(() => (importing.value = false))
}

watch(pasteText, value => {
  if (value.trim()) applyDocument(value)
})

SourceService.getAll(new FilterModel())
             .then(response => {
               if (response.status) {
                 sourceOptions.value = (response.data.content || [])
                     .map((item: any) => ({ label: item.name, value: item.code }))
               }
             })
</script>
