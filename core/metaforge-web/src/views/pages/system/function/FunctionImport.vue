<template>
  <MfModal v-model:open="visible" width="min(880px, 94vw)" body-class="px-5 py-4">
    <template #title>
      <span class="flex items-center gap-2.5">
        <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary">
          <Database :size="18"/>
        </span>
        <span class="flex min-w-0 flex-col">
          <span class="text-base font-semibold leading-6">{{ $t('function.common.import') }}</span>
          <span class="text-xs font-normal text-content-tertiary">{{ $t('function.import.headerSubtitle') }}</span>
        </span>
      </span>
    </template>

    <!-- 步骤条 -->
    <div class="mb-4 flex items-center gap-2">
      <template v-for="(item, index) in steps" :key="item.key">
        <div class="flex items-center gap-2.5">
          <span class="inline-flex size-7 shrink-0 items-center justify-center rounded-mf-full text-sm font-semibold"
                :class="step >= index + 1 ? 'bg-primary text-on-primary' : 'border border-line bg-surface text-content-tertiary'">
            <Check v-if="step > index + 1" :size="14"/>
            <template v-else>{{ index + 1 }}</template>
          </span>
          <div class="leading-tight">
            <div class="text-sm font-medium" :class="step >= index + 1 ? 'text-content' : 'text-content-tertiary'">{{ $t(item.title) }}</div>
            <div class="text-[11px] text-content-tertiary">{{ $t(item.desc) }}</div>
          </div>
        </div>
        <span v-if="index < steps.length - 1" class="h-px flex-1 bg-line"/>
      </template>
    </div>

    <!-- 第一步：选择数据源 -->
    <template v-if="step === 1">
      <div class="mb-3 flex items-start gap-3">
        <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-mf-full bg-primary text-sm font-semibold text-on-primary">1</span>
        <div class="min-w-0">
          <div class="text-sm font-semibold text-content">{{ $t('function.import.step1') }}</div>
          <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.import.tipBatch') }}</div>
        </div>
      </div>

      <div class="mb-3 flex gap-1.5 rounded-mf-md border border-line bg-surface p-1">
        <button v-for="tab in sourceTabs" :key="tab.key" type="button"
                class="flex flex-1 items-center justify-center gap-1.5 rounded-mf-sm border border-transparent px-2 py-1.5 text-sm transition-colors"
                :class="source === tab.key ? 'border-primary bg-primary/5 font-medium text-primary' : 'text-content-secondary hover:bg-surface-hover'"
                @click="source = tab.key">
          <component :is="tab.icon" :size="14"/>
          {{ $t(tab.label) }}
        </button>
      </div>

      <div v-if="source === 'file'" class="space-y-2">
        <button type="button"
                class="w-full rounded-mf-md border border-dashed border-line px-4 py-6 text-center transition-colors hover:border-primary hover:bg-primary/5"
                @click="fileInput?.click()"
                @dragover.prevent
                @drop.prevent="onDropFile">
          <CloudUpload :size="26" class="mx-auto mb-1 text-content-tertiary"/>
          <span class="block text-sm font-medium text-content">{{ $t('function.import.dropTitle') }}</span>
          <span class="mt-0.5 block text-xs text-content-tertiary">{{ $t('function.import.dropHint') }}</span>
          <span class="mt-2 inline-flex items-center gap-1.5 rounded-mf-md border border-primary px-3 py-1.5 text-sm text-primary">
            <FolderOpen :size="14"/>
            {{ $t('function.import.chooseFile') }}
          </span>
        </button>
        <input ref="fileInput" type="file" accept=".json,.csv,.tsv,.txt,.md,.markdown,.xlsx,.xls" class="hidden" @change="onPickFile"/>

        <div v-if="fileName" class="flex items-center gap-3 rounded-mf-md border border-success/30 bg-success/5 px-3 py-2">
          <span class="inline-flex size-8 items-center justify-center rounded-mf-md bg-success/15 text-success">
            <FileSpreadsheet :size="16"/>
          </span>
          <div class="min-w-0 flex-1">
            <div class="truncate text-sm font-medium text-content">{{ fileName }}</div>
            <div class="text-[11px] text-content-tertiary">{{ formatSize(fileSize) }} · {{ fileDate }}</div>
          </div>
          <button type="button" class="text-content-tertiary transition-colors hover:text-content" @click="clearFile">
            <X :size="14"/>
          </button>
        </div>
      </div>

      <div v-else-if="source === 'url'" class="space-y-2">
        <div class="flex gap-2">
          <MfInput v-model="url" placeholder="https://example.com/functions.csv" class="flex-1"/>
          <MfButton :loading="fetching" @click="onFetchUrl">
            <CloudDownload :size="14"/>
            {{ $t('function.import.urlFetch') }}
          </MfButton>
        </div>
        <p v-if="urlFetchedTip" class="text-xs text-primary">{{ urlFetchedTip }}</p>
      </div>

      <AceEditor v-else-if="source === 'paste'" :value="pasteText" height="140px" @update:value="pasteText = $event"/>

      <div v-else class="rounded-mf-md border border-dashed border-line px-4 py-8 text-center text-xs text-content-tertiary">
        {{ $t('function.import.sourceExternalTip') }}
      </div>

      <!-- 文件格式示例 -->
      <div class="mt-4">
        <div class="text-sm font-semibold text-content">{{ $t('function.import.exampleTitle') }}</div>
        <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.import.exampleDesc') }}</div>
        <div class="mt-2 overflow-hidden rounded-mf-md border border-line">
          <MfTable :columns="exampleColumns" :data-source="exampleRows" row-key="name" density="compact" :bordered="false">
            <template #cell-name="{ record }"><span class="font-mono text-content">{{ record.name }}</span></template>
            <template #cell-syntax="{ record }"><span class="font-mono text-content">{{ record.syntax }}</span></template>
            <template #cell-example="{ record }"><span class="font-mono text-content">{{ record.example }}</span></template>
          </MfTable>
        </div>
        <div class="mt-2 flex gap-2">
          <MfButton size="sm" @click="onDownloadTemplate">
            <Download :size="14"/>
            {{ $t('function.import.downloadTemplate') }}
          </MfButton>
          <MfButton size="sm" @click="formatVisible = true">
            <BookOpen :size="14"/>
            {{ $t('function.import.formatInfo') }}
          </MfButton>
        </div>
      </div>

      <!-- 提示 -->
      <div class="mt-3 flex items-start gap-2.5 rounded-mf-md border border-primary/20 bg-primary/5 px-3 py-2.5">
        <Info :size="15" class="mt-0.5 shrink-0 text-primary"/>
        <div class="min-w-0">
          <div class="text-sm font-medium text-primary">{{ $t('function.import.tipsTitle') }}</div>
          <ul class="mt-1 list-disc space-y-0.5 pl-4 text-xs text-content-secondary">
            <li>{{ $t('function.import.tipSize') }}</li>
            <li>{{ $t('function.import.tipEncoding') }}</li>
            <li>{{ $t('function.import.tipHeader') }}</li>
            <li>{{ $t('function.import.tipBatch') }}</li>
          </ul>
        </div>
      </div>
    </template>

    <!-- 第二步：配置导入 -->
    <template v-else-if="step === 2">
      <div class="mb-3 flex items-start gap-3">
        <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-mf-full bg-primary text-sm font-semibold text-on-primary">2</span>
        <div class="min-w-0">
          <div class="text-sm font-semibold text-content">{{ $t('function.import.config') }}</div>
          <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.import.mappingDesc') }}</div>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3">
        <div>
          <div class="mb-1 text-sm font-medium text-content-secondary">{{ $t('function.import.targetPlugin') }} <span class="text-danger">*</span></div>
          <MfSelect v-model="plugin" multiple :options="plugins" :placeholder="$t('function.tip.selectPluginHolder')"/>
          <div class="mt-1 text-[11px] text-content-tertiary">
            {{ $t('function.import.pluginHelp', { plugin: plugin[0] ?? $t('function.import.pluginNotSelected') }) }}
          </div>
        </div>
        <div>
          <div class="mb-1 text-sm font-medium text-content-secondary">{{ $t('function.import.dataType') }} <span class="text-danger">*</span></div>
          <MfSelect v-model="type" :options="typeOptions"/>
          <div class="mt-1 text-[11px] text-content-tertiary">{{ $t('function.import.' + typeHelpKey) }}</div>
        </div>
      </div>

      <div class="mt-3 flex items-start gap-2.5 rounded-mf-md border border-primary/20 bg-primary/5 px-3 py-2">
        <Info :size="14" class="mt-0.5 shrink-0 text-primary"/>
        <span class="text-xs text-content-secondary">{{ $t('function.import.' + typeInfoKey) }}</span>
      </div>

      <div class="mt-4">
        <div class="text-sm font-semibold text-content">{{ $t('function.import.mapping') }}</div>
        <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.import.mappingDesc') }}</div>
        <div class="mt-2 overflow-hidden rounded-mf-md border border-line">
          <MfTable :columns="mappingColumns" :data-source="mappingFields" row-key="key" density="compact" :bordered="false">
            <template #cell-field="{ record }">
              <span class="text-content">{{ $t(`function.field.${ record.key }`) }}<span v-if="record.required" class="ml-0.5 text-danger">*</span></span>
            </template>
            <template #cell-column="{ record }">
              <MfSelect v-model="mapping[record.key]" size="sm" :options="columnOptions" :placeholder="$t('function.import.selectColumn')" clearable/>
            </template>
          </MfTable>
        </div>
      </div>

      <div class="mt-4">
        <div class="text-sm font-semibold text-content">{{ $t('function.import.options') }}</div>
        <div class="mt-2 grid grid-cols-2 gap-x-4 gap-y-3">
          <div>
            <MfCheckbox v-model="optionHeaderRow">{{ $t('function.import.optionHeader') }}</MfCheckbox>
            <div class="pl-6 text-[11px] text-content-tertiary">{{ $t('function.import.optionHeaderDesc') }}</div>
          </div>
          <div>
            <MfCheckbox v-model="optionSkipEmpty">{{ $t('function.import.optionSkipEmpty') }}</MfCheckbox>
            <div class="pl-6 text-[11px] text-content-tertiary">{{ $t('function.import.optionSkipEmptyDesc') }}</div>
          </div>
          <div>
            <div class="text-sm text-content">{{ $t('function.import.optionDuplicate') }}</div>
            <MfSelect v-model="duplicate" size="sm" class="mt-1" :options="duplicateOptions"/>
          </div>
          <div>
            <MfCheckbox v-model="optionContinueOnError">{{ $t('function.import.optionContinue') }}</MfCheckbox>
            <div class="pl-6 text-[11px] text-content-tertiary">{{ $t('function.import.optionContinueDesc') }}</div>
          </div>
        </div>
      </div>
    </template>

    <!-- 第三步：确认导入 -->
    <template v-else>
      <div class="mb-3 flex items-start gap-3">
        <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-mf-full bg-primary text-sm font-semibold text-on-primary">3</span>
        <div class="min-w-0">
          <div class="text-sm font-semibold text-content">{{ $t('function.import.preview') }}</div>
          <div class="mt-0.5 text-xs text-content-tertiary">{{ $t('function.import.previewDesc') }}</div>
        </div>
      </div>

      <div class="grid grid-cols-4 gap-2">
        <div v-for="stat in stats" :key="stat.label" class="flex items-center gap-2 rounded-mf-md border border-line px-3 py-2">
          <component :is="stat.icon" :size="16" :class="stat.tone"/>
          <div class="leading-tight">
            <div class="text-[11px] text-content-tertiary">{{ stat.label }}</div>
            <div class="text-sm font-semibold text-content">{{ stat.value }}</div>
          </div>
        </div>
      </div>

      <div class="mt-3 overflow-hidden rounded-mf-md border border-line">
        <MfTable :columns="previewColumns" :data-source="previewRows" row-key="__index" density="compact" :bordered="false"
                 :row-class="record => 'cursor-pointer' + (record.__index === selected ? ' bg-primary/5' : '')" @row-click="(record: any) => toggleSelected(record.__index)">
          <template #cell-index="{ record }"><span class="text-content-tertiary">{{ record.__index + 1 }}</span></template>
          <template #cell-name="{ record }">
            <span class="inline-flex items-center gap-1 font-mono text-content">
              <ChevronRight :size="11" :class="['text-content-tertiary transition-transform duration-200', record.__index === selected && 'rotate-90']"/>
              {{ record.name || $t('function.import.invalidReason') }}
            </span>
          </template>
          <template #cell-description="{ record }"><EllipsisCell :text="record.description || ''"/></template>
          <template #cell-syntax="{ record }"><span class="font-mono"><EllipsisCell :text="record.syntax || ''"/></span></template>
          <template #cell-examples="{ record }"><span class="font-mono"><EllipsisCell :text="record.examples || ''"/></span></template>
        </MfTable>
        <!-- Clicking a function shows every attribute it was read with. -->
        <div v-if="selected !== null && previewEntries[selected]" class="border-t border-line bg-surface-hover/40 px-3 py-2">
          <div class="mb-1 text-[11px] font-medium text-content-tertiary">{{ $t('function.import.detailTitle') }}</div>
          <div class="space-y-1">
            <div v-for="(value, key) in previewEntries[selected]" :key="key" class="grid grid-cols-[110px_1fr] items-start gap-2">
              <span class="text-[11px] leading-5 text-content-tertiary">{{ attributeLabel(String(key)) }}</span>
              <pre class="m-0 overflow-x-auto whitespace-pre-wrap font-mono text-xs leading-5 text-content">{{ value }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- 无效数据 -->
      <div v-if="invalidEntries.length" class="mt-3 overflow-hidden rounded-mf-md border border-danger/30">
        <button type="button" class="flex w-full items-center gap-2 bg-danger/5 px-3 py-2 text-left" @click="invalidOpen = !invalidOpen">
          <TriangleAlert :size="14" class="text-danger"/>
          <span class="flex-1 text-sm font-medium text-danger">{{ $t('function.import.invalidTitle') }}（{{ invalidEntries.length }}）</span>
          <ChevronDown :size="13" :class="['text-content-tertiary transition-transform duration-200', invalidOpen && 'rotate-180']"/>
        </button>
        <div v-if="invalidOpen" class="border-t border-danger/30">
          <MfTable :columns="invalidColumns" :data-source="invalidRows" row-key="__index" density="compact" :bordered="false">
            <template #cell-index="{ record }"><span class="text-content-tertiary">{{ record.__index + 1 }}</span></template>
            <template #cell-reason><span class="text-danger">{{ $t('function.import.invalidReason') }}</span></template>
            <template #cell-content="{ record }"><span class="font-mono"><EllipsisCell :text="record.content || ''"/></span></template>
          </MfTable>
        </div>
      </div>
    </template>

    <template #footer>
      <MfButton v-if="step > 1" @click="step--">{{ $t('function.import.prev') }}</MfButton>
      <MfButton @click="visible = false">{{ $t('common.cancel') }}</MfButton>
      <MfButton v-if="step < 3" variant="primary" @click="onNext">{{ $t('common.next') }}</MfButton>
      <MfButton v-else variant="primary" :loading="importing" @click="onImport">{{ $t('function.import.beginImport') }}</MfButton>
    </template>
  </MfModal>

  <!-- 格式说明 -->
  <MfModal v-model:open="formatVisible" :title="$t('function.import.formatInfoTitle')" width="480px">
    <div class="space-y-2 text-xs text-content-secondary">
      <p>{{ $t('function.import.exampleDesc') }}</p>
      <div v-for="field in mappingFields" :key="field.key" class="flex gap-2">
        <span class="w-32 shrink-0 font-medium text-content">{{ $t(`function.field.${ field.key }`) }}<span v-if="field.required" class="ml-0.5 text-danger">*</span></span>
        <span class="min-w-0 flex-1">{{ fieldHint(field.key) }}</span>
      </div>
    </div>
  </MfModal>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/date'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import {
  BookOpen, Check, ChevronDown, ChevronRight, CircleCheck, CircleX, CloudDownload, CloudUpload,
  Database, Download, FileSpreadsheet, FileText, FolderOpen, Info, Link, Sheet, SquareDashedBottomCode,
  TriangleAlert, Upload, X
} from '@lucide/vue'
import {
  FunctionMappingField,
  FunctionSourceDocument,
  FunctionType
} from '@/model/function'
import {
  guessColumn,
  mapDocumentEntries,
  parseFunctionDocument
} from './importParse'
import FunctionService from '@/services/function'
import PluginService from '@/services/plugin'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

defineOptions({ name: 'FunctionImport' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), {
  isVisible: false
})
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const { t } = useI18n()

const steps = [
  { key: 'source', title: 'function.import.step1', desc: 'function.import.step1Desc' },
  { key: 'configure', title: 'function.import.step2', desc: 'function.import.step2Desc' },
  { key: 'confirm', title: 'function.import.step3', desc: 'function.import.step3Desc' }
]

// Dynamic system fields per data type; entries do not have to share fields,
// unmapped source columns ride along as plugin-specific attributes.
const MAPPING_FIELDS: Record<string, FunctionMappingField[]> = {
  [FunctionType.KEYWORD]: [
    { key: 'name', required: true },
    { key: 'description' },
    { key: 'syntax' },
    { key: 'examples' },
    { key: 'notes' },
    { key: 'version' }
  ],
  [FunctionType.OPERATOR]: [
    { key: 'name', required: true },
    { key: 'description' },
    { key: 'syntax' },
    { key: 'examples' },
    { key: 'parameters' },
    { key: 'return_type' },
    { key: 'notes' },
    { key: 'version' }
  ],
  [FunctionType.FUNCTION]: [
    { key: 'name', required: true },
    { key: 'category' },
    { key: 'description' },
    { key: 'syntax' },
    { key: 'examples' },
    { key: 'parameters' },
    { key: 'return_type' },
    { key: 'notes' },
    { key: 'version' }
  ]
}

const FIELD_HINTS: Record<string, Record<string, string>> = {
  'zh-cn': {
    name: '函数名，如 SELECT、COUNT、CONCAT，必填',
    category: '查询、聚合、字符串等分类',
    description: '该函数的功能描述',
    syntax: 'SQL 语法，如 CONCAT(str1, str2,...)',
    examples: '一个或多个使用示例，每行一条',
    parameters: '参数说明（可选）',
    return_type: '返回类型（可选）',
    notes: '使用限制及注意事项',
    version: '数据库适用版本'
  },
  en: {
    name: 'Function name such as SELECT, COUNT or CONCAT; required',
    category: 'Category such as query, aggregate or string',
    description: 'What the function does',
    syntax: 'SQL syntax such as CONCAT(str1, str2,...)',
    examples: 'One or more usage examples, one per line',
    parameters: 'Parameter explanation (optional)',
    return_type: 'Return type (optional)',
    notes: 'Limitations and notes',
    version: 'Database versions the entry applies to'
  }
}

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const step = ref(1)
const source = ref<string>('file')
const pasteText = ref('')
const url = ref('')
const urlText = ref('')
const fetching = ref(false)
const urlFetchedTip = ref('')
const fileName = ref('')
const fileSize = ref(0)
const fileDate = ref('')
const fileText = ref('')
const fileInput = ref<HTMLInputElement>()
const formatVisible = ref(false)

const plugin = ref<string[]>([])
const type = ref<string>(FunctionType.FUNCTION)
const duplicate = ref<'skip' | 'overwrite'>('skip')
const optionHeaderRow = ref(true)
const optionSkipEmpty = ref(true)
const optionContinueOnError = ref(true)
const mapping = ref<Record<string, string | undefined>>({})
const sourceDocument = ref<FunctionSourceDocument>({ columns: [], rows: [] })

const plugins = ref<{ label: string; value: string }[]>([])
const importing = ref(false)
// The preview row whose attributes are shown under the table.
const selected = ref<number | null>(null)
const invalidOpen = ref(true)

const sourceTabs = [
  { key: 'file', label: 'function.import.sourceFile', icon: Upload },
  { key: 'url', label: 'function.import.sourceUrl', icon: Link },
  { key: 'paste', label: 'function.import.sourcePaste', icon: SquareDashedBottomCode },
  { key: 'external', label: 'function.import.sourceExternal', icon: Database }
]

const typeOptions = computed(() => [
  { label: t('function.common.function'), value: FunctionType.FUNCTION },
  { label: t('function.common.keyword'), value: FunctionType.KEYWORD },
  { label: t('function.common.operator'), value: FunctionType.OPERATOR }
])
const duplicateOptions = computed(() => [
  { label: t('function.import.duplicateSkip'), value: 'skip' },
  { label: t('function.import.duplicateOverwrite'), value: 'overwrite' }
])
const mappingFields = computed(() => MAPPING_FIELDS[type.value] || MAPPING_FIELDS[FunctionType.FUNCTION])
const columnOptions = computed(() => sourceDocument.value.columns.map(column => ({ label: column, value: column })))
const typeHelpKey = computed(() => `typeHelp${ String(type.value).toLowerCase().replace(/^\w/, c => c.toUpperCase()) }`)
const typeInfoKey = computed(() => `typeInfo${ String(type.value).toLowerCase().replace(/^\w/, c => c.toUpperCase()) }`)

const activeText = computed(() => {
  if (source.value === 'paste') return pasteText.value
  if (source.value === 'url') return urlText.value
  return fileText.value
})

const entries = computed<Record<string, string>[]>(() => {
  if (!sourceDocument.value.rows.length) return []
  const mapped = mapDocumentEntries(sourceDocument.value, mapping.value)
  const mappedColumns = new Set(Object.values(mapping.value).filter(Boolean) as string[])
  // unmapped source columns ride along so the backend can store plugin-specific attributes
  return sourceDocument.value.rows.map((row, index) => {
    const entry = { ...mapped[index] }
    sourceDocument.value.columns.forEach((column, columnIndex) => {
      if (mappedColumns.has(column)) return
      const value = row[columnIndex]
      if (value && String(value).trim() !== '') entry[column] = String(value).trim()
    })
    return entry
  })
})

const validEntries = computed(() => entries.value.filter(entry => !!entry.name))
const invalidEntries = computed(() => entries.value
  .map((entry, index) => ({ entry, index }))
  .filter(({ entry }) => !entry.name)
  .map(({ entry, index }) => ({
    row: index + (optionHeaderRow.value ? 2 : 1),
    content: Object.values(entry).join(' ') || sourceDocument.value.rows[index]?.join(' ') || ''
  })))
const previewEntries = computed(() => entries.value.slice(0, 10))
const previewRows = computed(() => previewEntries.value.map((entry, index) => ({ ...entry, __index: index })))
const invalidRows = computed(() => invalidEntries.value.map((entry, index) => ({ ...entry, __index: index })))
const exampleColumns = computed(() => [
  { key: 'name', title: t('function.field.name'), dataIndex: 'name' },
  { key: 'description', title: t('function.import.colDescription'), dataIndex: 'description' },
  { key: 'syntax', title: t('function.import.exampleUsage'), dataIndex: 'syntax' },
  { key: 'example', title: t('function.import.colExample'), dataIndex: 'example' },
  { key: 'notes', title: t('function.field.notes'), dataIndex: 'notes' },
])
const exampleRows = computed(() => [{
  name: 'CONCAT', description: t('function.import.exampleDescCell'), syntax: t('function.import.exampleSyntax'), example: t('function.import.exampleSql'), notes: t('function.import.exampleNotes'),
}])
const mappingColumns = computed(() => [
  { key: 'field', title: t('function.import.mappingSystemField'), dataIndex: 'key', width: 200 },
  { key: 'column', title: t('function.import.mappingFileField'), dataIndex: 'key' },
])
const previewColumns = computed(() => [
  { key: 'index', title: t('function.import.colIndex'), dataIndex: '__index', width: 56 },
  { key: 'name', title: t('function.import.colName'), dataIndex: 'name', width: 200 },
  { key: 'description', title: t('function.import.colDescription'), dataIndex: 'description', width: 200 },
  { key: 'syntax', title: t('function.import.colUsage'), dataIndex: 'syntax', width: 200 },
  { key: 'examples', title: t('function.import.colExample'), dataIndex: 'examples', width: 200 },
])
const invalidColumns = computed(() => [
  { key: 'index', title: t('function.import.colIndex'), dataIndex: '__index', width: 56 },
  { key: 'row', title: t('function.import.colRow'), dataIndex: 'row', width: 80 },
  { key: 'reason', title: t('function.import.colReason'), dataIndex: 'row', width: 140 },
  { key: 'content', title: t('function.import.colContent'), dataIndex: 'content' },
])
const estimate = computed(() => Math.max(1, Math.round((validEntries.value.length || 1) / 200)))
const stats = computed(() => [
  { label: t('function.import.statTotal'), value: String(entries.value.length), icon: FileText, tone: 'text-content-tertiary' },
  { label: t('function.import.statValid'), value: String(validEntries.value.length), icon: CircleCheck, tone: 'text-success' },
  { label: t('function.import.statInvalid'), value: String(invalidEntries.value.length), icon: CircleX, tone: 'text-danger' },
  { label: t('function.import.statEstimate'), value: t('function.import.estimateSeconds', { count: estimate.value }), icon: Sheet, tone: 'text-content-tertiary' }
])

const attributeLabel = (key: string) => {
  const known = t(`function.field.${ key }`)
  return known === `function.field.${ key }` ? key : known
}

const fieldHint = (key: string) => {
  const hints = FIELD_HINTS[localStorage.getItem('locale') || 'zh-cn'] || FIELD_HINTS['zh-cn']
  return hints[key] || ''
}

const toggleSelected = (index: number) => { selected.value = selected.value === index ? null : index }

const applyDocument = (text: string) => {
  try {
    const parsed = parseFunctionDocument(text, {
      firstRowHeader: optionHeaderRow.value,
      skipEmptyRows: optionSkipEmpty.value
    })
    if (!parsed.rows.length) {
      MfMessage.warning(t('function.import.emptyDocument'))
      return false
    }
    sourceDocument.value = parsed
    // auto-guess the mapping for the new document
    const guessed: Record<string, string | undefined> = {}
    mappingFields.value.forEach(field => {
      guessed[field.key] = guessColumn(field.key, parsed.columns)
    })
    mapping.value = guessed
    return true
  }
  catch {
    MfMessage.error(t('function.import.parseFailed'))
    return false
  }
}

const onFetchUrl = () => {
  if (!url.value.trim()) {
    MfMessage.warning(t('function.import.noDocument'))
    return
  }
  fetching.value = true
  urlFetchedTip.value = ''
  FunctionService.previewUrl(url.value.trim())
               .then(response => {
                 if (response.status) {
                   urlText.value = String(response.data ?? '')
                   urlFetchedTip.value = t('function.import.urlFetched', { count: urlText.value.length })
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (fetching.value = false))
}

const readPickedFile = (file: File) => {
  fileName.value = file.name
  fileSize.value = file.size
  fileDate.value = formatDateTime(new Date())
  file.text()
      .then(text => {
        fileText.value = text
      })
      .catch(() => MfMessage.error(t('function.import.parseFailed')))
}

const onPickFile = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) readPickedFile(file)
  input.value = ''
}

const onDropFile = (event: DragEvent) => {
  const file = event.dataTransfer?.files?.[0]
  if (file) readPickedFile(file)
}

const clearFile = () => {
  fileName.value = ''
  fileSize.value = 0
  fileDate.value = ''
  fileText.value = ''
}

const onDownloadTemplate = () => {
  const headers = mappingFields.value.map(field => t(`function.field.${ field.key }`))
  const example = [t('function.field.name'), t('function.import.exampleDescCell'), t('function.import.exampleSyntax'), t('function.import.exampleSql'), '', '', '', '', t('function.import.exampleNotes')]
  const rows = [headers, example.slice(0, headers.length)]
  const csv = rows.map(row => row.map(cell => `"${ String(cell).replace(/"/g, '""') }"`).join(',')).join('\r\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  // 挂到 DOM 再点击，并延迟释放 blob URL，避免部分浏览器取消下载
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'metaforge_function_template.csv'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 4000)
}

const loadPlugins = () => {
  PluginService.getPlugins()
               .then(response => {
                 if (response.status) {
                   plugins.value = response.data
                       .filter((value: { type: string }) => value.type === 'CONNECTOR')
                       .map((value: { name: string }) => ({ label: value.name, value: value.name }))
                 }
               })
}

const onNext = () => {
  if (step.value === 1) {
    if (!activeText.value.trim()) {
      MfMessage.warning(t('function.import.noDocument'))
      return
    }
    if (!applyDocument(activeText.value)) return
    step.value = 2
    return
  }
  if (step.value === 2) {
    if (!plugin.value.length) {
      MfMessage.warning(t('function.import.noPlugin'))
      return
    }
    const missing = mappingFields.value.some(field => field.required && !mapping.value[field.key])
    if (missing) {
      MfMessage.warning(t('function.import.noMapping'))
      return
    }
    selected.value = null
    invalidOpen.value = true
    step.value = 3
  }
}

const onImport = () => {
  importing.value = true
  FunctionService.import({
    plugin: plugin.value,
    type: type.value,
    entries: entries.value,
    duplicate: duplicate.value,
    continueOnError: optionContinueOnError.value
  })
               .then((response) => {
                 if (response.status) {
                   const result = response.data || {}
                   MfMessage.success(t('function.import.success', {
                     imported: String(result.imported ?? 0),
                     duplicated: String(result.duplicated ?? 0),
                     invalid: String(result.invalid ?? 0)
                   }))
                   visible.value = false
                 }
                 else {
                   MfMessage.error(response.message)
                 }
               })
               .finally(() => (importing.value = false))
}

const formatSize = (size: number) => (size > 1024 * 1024 ? `${ (size / 1024 / 1024).toFixed(1) } MB` : `${ Math.max(1, Math.round(size / 1024)) } KB`)

loadPlugins()
</script>
