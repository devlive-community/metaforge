<template>
  <MfDrawer v-model:open="visible" width="min(1100px, 94vw)" :title="$t('source.common.menuNewTable')">
    <div v-if="loading" class="flex h-full min-h-96 items-center justify-center">
      <MfSpinner/>
    </div>

    <MfForm v-else ref="formRef" :model="formModel" class="flex h-full min-h-0 flex-col gap-4">
      <!-- 步骤条 -->
      <div class="flex shrink-0 items-center gap-3 rounded-mf-lg border border-line bg-surface px-5 py-4">
        <template v-for="(step, index) in steps" :key="step.titleKey">
          <div class="flex items-center gap-3">
            <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-full text-sm font-semibold"
                  :class="currentStep >= index + 1 ? 'bg-primary text-on-primary' : 'border border-line bg-surface text-content-tertiary'">
              <Check v-if="currentStep > index + 1" :size="15"/>
              <template v-else>{{ index + 1 }}</template>
            </span>
            <div class="leading-tight">
              <div class="text-sm font-semibold" :class="currentStep >= index + 1 ? 'text-content' : 'text-content-tertiary'">{{ $t(step.titleKey) }}</div>
              <div class="mt-0.5 text-xs text-content-tertiary">{{ $t(step.descKey) }}</div>
            </div>
          </div>
          <span v-if="index < steps.length - 1" class="h-px flex-1 bg-line"/>
        </template>
      </div>

      <!-- 第一步：基础信息（左）与表选项（右）两栏 -->
      <div v-if="currentStep === 1" class="grid flex-1 grid-cols-2 items-start gap-4">
        <section class="rounded-mf-lg border border-line bg-surface p-4">
          <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.create.basicInfo') }}</h3>
          <div class="flex flex-col gap-3">
            <MfFormItem name="name"
                        :label="$t('source.common.tableName')"
                        :rules="[
                            { required: true, message: $t('source.validator.tableName.required') },
                            { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: $t('source.validator.tableName.pattern') }
                        ]">
              <MfInput v-model="formState.name" placeholder="user"/>
            </MfFormItem>
            <MfFormItem :label="$t('source.create.sourceDb')">
              <MfInput :model-value="databaseName" disabled/>
            </MfFormItem>
            <MfFormItem name="comment" :label="$t('source.common.comment')">
              <MfTextarea v-model="formState.comment" :rows="3" :maxlength="500"/>
              <div class="mt-1 text-right text-[11px] text-content-tertiary">
                {{ $t('source.structure.commentCounter', { count: (formState.comment || '').length }) }}
              </div>
            </MfFormItem>
          </div>
        </section>

        <section class="rounded-mf-lg border border-line bg-surface p-4">
          <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.create.optionsCard') }}</h3>
          <div class="flex flex-col gap-3">
            <MfFormItem :label="$t('source.create.engine')">
              <MfSelect v-model="formState.engine" :options="engineOptions"/>
            </MfFormItem>
            <MfFormItem :label="$t('source.structure.charset')">
              <MfSelect v-model="options.charset" :options="charsetOptions"/>
            </MfFormItem>
            <MfFormItem :label="$t('source.common.collation')">
              <MfSelect v-model="options.collate" :options="collateOptions"/>
            </MfFormItem>
            <MfFormItem :label="$t('source.create.rowFormat')">
              <MfSelect v-model="options.rowFormat" :options="rowFormatOptions"/>
            </MfFormItem>
            <MfFormItem :label="$t('source.create.autoIncStart')">
              <MfInput v-model="options.autoIncrement" placeholder="1"/>
            </MfFormItem>
          </div>
        </section>
      </div>

      <!-- 第二步：字段定义 -->
      <section v-else-if="currentStep === 2" class="flex min-h-0 flex-1 flex-col gap-3 overflow-hidden rounded-mf-lg border border-line bg-surface p-4">
        <MfFormItem name="columns" class="shrink-0"
                    :rules="[{ required: true, type: 'array', message: $t('source.validator.columns.required') }]">
          <div class="flex w-full flex-wrap items-center justify-between gap-2">
            <h3 class="m-0 text-base font-semibold text-content">{{ $t('source.create.fieldDefine') }}</h3>
            <div class="flex items-center gap-2">
              <MfButton size="sm" @click="onAdd"><Plus :size="13"/>{{ $t('source.structure.addField') }}</MfButton>
              <MfButton size="sm" :disabled="selectedRows.size === 0" @click="onMoveSelected(-1)"><ArrowUp :size="13"/>{{ $t('source.structure.moveUp') }}</MfButton>
              <MfButton size="sm" :disabled="selectedRows.size === 0" @click="onMoveSelected(1)"><ArrowDown :size="13"/>{{ $t('source.structure.moveDown') }}</MfButton>
              <MfButton size="sm" variant="danger" :disabled="selectedRows.size === 0" @click="onBatchDelete"><Trash2 :size="13"/>{{ $t('source.create.batchDelete') }}</MfButton>
            </div>
          </div>
        </MfFormItem>

        <div class="flex min-h-0 flex-1 overflow-hidden">
          <MfTable fill :columns="fieldColumns" :data-source="formState.columns" row-key="__row" density="compact" :bordered="false"
                   :empty-text="$t('source.create.noColumns')" :empty-description="$t('source.create.addColumnsHint')" empty-variant="initial">
            <template #header-__check__>
              <MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :aria-label="$t('source.data.selectAll')"
                          @update:model-value="value => toggleAll(!!value)"/>
            </template>
            <template #cell-__check__="{ index }">
              <span class="inline-flex" @click.stop>
                <MfCheckbox :model-value="selectedRows.has(index)" :aria-label="$t('source.data.selectRow')"
                            @update:model-value="value => toggleRow(index, !!value)"/>
              </span>
            </template>
            <template #cell-name="{ index }">
              <MfFormItem :name="`columns.${index}.name`"
                          :rules="[
                              { required: true, message: $t('source.validator.columnName.required') },
                              { pattern: /^[A-Za-z][A-Za-z0-9_-]*$/, message: $t('source.validator.columnName.pattern') }
                          ]">
                <MfInput v-model="formState.columns[index].name" size="sm"/>
              </MfFormItem>
            </template>
            <template #cell-type="{ index }">
              <MfFormItem :name="`columns.${index}.type`"
                          :rules="[{ required: true, message: $t('source.validator.columnType.required') }]">
                <MfSelect v-model="formState.columns[index].type" size="sm" :options="dataTypeOptions"/>
              </MfFormItem>
            </template>
            <template #cell-length="{ index }">
              <div class="w-16">
                <MfInput v-model="formState.columns[index].length" size="sm" class="[&_input]:px-2"/>
              </div>
            </template>
            <template #cell-defaultValue="{ index }">
              <MfInput v-model="formState.columns[index].defaultValue" size="sm"/>
            </template>
            <template #cell-nullable="{ index }">
              <MfCheckbox v-model="formState.columns[index].nullable"/>
            </template>
            <template #cell-primaryKey="{ index }">
              <MfCheckbox v-model="formState.columns[index].primaryKey"/>
            </template>
            <template #cell-autoIncrement="{ index }">
              <MfCheckbox v-model="formState.columns[index].autoIncrement"/>
            </template>
            <template #cell-unique="{ index }">
              <MfCheckbox v-model="formState.columns[index].unique"/>
            </template>
            <template #cell-comment="{ index }">
              <MfInput v-model="formState.columns[index].comment" size="sm"/>
            </template>
            <template #cell-action="{ index }">
              <div class="flex items-center gap-2 text-xs">
                <button type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="index === 0" @click="onMoveColumn(index, -1)">{{ $t('source.structure.moveUp') }}</button>
                <button type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="index === formState.columns.length - 1" @click="onMoveColumn(index, 1)">{{ $t('source.structure.moveDown') }}</button>
                <button type="button" class="text-danger hover:underline" @click="onRemove(index)">{{ $t('common.delete') }}</button>
              </div>
            </template>
          </MfTable>
        </div>
      </section>

      <!-- 第三步：索引与约束 -->
      <section v-else-if="currentStep === 3" class="flex min-h-0 flex-1 flex-col gap-3 overflow-hidden rounded-mf-lg border border-line bg-surface p-4">
        <div class="flex shrink-0 items-center justify-between gap-2">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('source.create.indexCard') }}</h3>
          <MfButton size="sm" :disabled="!canAddIndex" @click="onAddIndex">
            <Plus :size="13"/>{{ $t('source.create.addIndexConstraint') }}
          </MfButton>
        </div>

        <div class="flex shrink-0 items-center gap-1 overflow-x-auto border-b border-line">
          <button v-for="tab in indexTabs" :key="tab.key" type="button"
                  class="whitespace-nowrap border-b-2 px-3 py-2 text-sm font-medium transition-colors"
                  :class="indexTab === tab.key ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'"
                  @click="indexTab = tab.key">
            {{ $t(tab.labelKey) }} ({{ indexCounts[tab.key] }})
          </button>
        </div>

        <div class="flex min-h-0 flex-1 overflow-hidden">
          <MfTable fill :columns="indexColumns" :data-source="visibleIndexRows" row-key="__key" density="compact" :bordered="false" :empty-text="$t('common.noData')">
            <template #cell-__index__="{ index }">{{ index + 1 }}</template>
            <template #cell-name="{ record: row }">
              <MfFormItem v-if="indexRows.includes(row)"
                          :name="`indexes.${row.__key}.name`"
                          :rules="[{ required: true, validator: value => !!String(value || '').trim(), message: $t('source.create.indexNameRequired') }]">
                <MfInput v-model="row.name" size="sm"/>
              </MfFormItem>
              <span v-else class="font-mono text-xs text-content">{{ row.name }}</span>
            </template>
            <template #cell-kind="{ record: row }">
              <MfTag size="sm" :variant="row.kind === 'primary' ? 'success' : row.kind === 'unique' ? 'info' : 'default'">
                {{ $t(row.kind === 'primary' ? 'source.create.tabPrimary' : row.kind === 'unique' ? 'source.create.tabUnique' : 'source.create.tabNormal') }}
              </MfTag>
            </template>
            <template #cell-columns="{ record: row }">
              <MfFormItem v-if="indexRows.includes(row)"
                          :name="`indexes.${row.__key}.columns`"
                          :rules="[{ required: true, type: 'array', message: $t('source.create.indexColumnsRequired') }]">
                <MfSelect v-model="row.columns" size="sm" multiple searchable :options="columnOptions"
                          :placeholder="$t('source.create.indexColumnsRequired')"/>
              </MfFormItem>
              <MfTooltip v-else :content="row.columns.join(', ')">
                <span class="block truncate font-mono text-xs text-content">{{ row.columns.join(', ') }}</span>
              </MfTooltip>
            </template>
            <template #cell-method="{ record: row }">
              <MfSelect v-model="row.method" size="sm" :options="methodOptions" :disabled="!indexRows.includes(row)"/>
            </template>
            <template #cell-comment="{ record: row }">
              <MfInput v-if="indexRows.includes(row)" v-model="row.comment" size="sm"/>
              <span v-else class="text-xs text-content-secondary">{{ row.comment || '-' }}</span>
            </template>
            <template #cell-action="{ record: row }">
              <button type="button" class="text-xs text-danger hover:underline" @click="onDeleteIndexRow(row)">{{ $t('common.delete') }}</button>
            </template>
          </MfTable>
        </div>
      </section>

      <!-- 第四步：生成的 SQL 与创建行为选项 -->
      <div v-else-if="currentStep === 4" class="flex min-h-0 flex-1 flex-col gap-4">
        <section class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface p-4">
          <h3 class="m-0 mb-3 shrink-0 text-base font-semibold text-content">{{ $t('source.create.sqlCard') }}</h3>
          <div class="mb-3 flex shrink-0 items-center gap-1 overflow-x-auto border-b border-line">
            <button v-for="tab in sqlTabs" :key="tab.kind" type="button"
                    class="whitespace-nowrap border-b-2 px-2.5 py-1.5 text-xs font-medium transition-colors"
                    :class="sqlTab === tab.kind ? 'border-primary text-primary' : 'border-transparent text-content-secondary hover:text-content'"
                    @click="sqlTab = tab.kind">
              {{ $t(tab.labelKey) }}
            </button>
          </div>
          <div v-if="sqlLoading" class="flex min-h-0 flex-1 items-center justify-center rounded-mf-md border border-line"><MfSpinner size="sm"/></div>
          <div v-else-if="sqlError" class="flex min-h-0 flex-1 flex-col items-center justify-center gap-3">
            <MfAlert variant="danger" :title="sqlError"/>
            <MfButton size="sm" @click="retryPreview">{{ $t('common.refresh') }}</MfButton>
          </div>
          <SqlCodeBlock v-else fill :code="sqlContent" :copyable="true" :placeholder="$t('common.noData')"/>
          <div class="mt-3 flex shrink-0 flex-wrap items-center justify-end gap-2">
            <MfButton size="sm" :disabled="!sqlContent" @click="onCopySql"><Copy :size="13"/>{{ $t('source.executed.copy') }} SQL</MfButton>
            <MfButton size="sm" :loading="formatting" :disabled="!sqlContent" @click="onFormatSql"><Scan :size="13"/>{{ $t('source.ddl.format') }}</MfButton>
            <MfButton size="sm" :disabled="!sqlContent" @click="onDownloadSql"><Download :size="13"/>{{ $t('source.common.downloadFile') }}</MfButton>
          </div>
        </section>
      </div>
    </MfForm>

    <template #footer>
      <div class="flex w-full flex-wrap items-center justify-end gap-2">
        <MfCheckbox v-if="currentStep === 4" v-model="advanced.continueIndex" class="mr-2">{{ $t('source.create.optContinueIndex') }}</MfCheckbox>
        <MfButton v-if="currentStep > 1" @click="onPrev">{{ $t('source.create.prev') }}</MfButton>
        <MfButton @click="onCancel">{{ $t('common.cancel') }}</MfButton>
        <MfButton v-if="currentStep < 4" variant="primary" :disabled="loading" @click="onNext">
          {{ $t('common.next') }}
          <ArrowRight :size="14"/>
        </MfButton>
        <MfButton v-else variant="primary" :loading="saving" :disabled="!sqlContent" @click="onSubmit">
          <Check :size="14"/>
          {{ $t('source.create.confirmCreate') }}
        </MfButton>
      </div>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSourceManageRoute as useRoute } from '@/composables/useSourceManageRoute'
import { useTableCreateIndexes, type TableCreateIndexRow as IndexRow } from '@/composables/useTableCreateIndexes'
import { useTableCreatePreview } from '@/composables/useTableCreatePreview'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import MetadataService from '@/services/metadata'
import FormatService from '@/services/format'
import HttpUtils from '@/utils/http'
import { ObjectUtils } from '@/utils/object'
import { sourceManageLocation } from '@/model/source-manage'
import {
  ArrowDown, ArrowRight, ArrowUp, Check, Copy, Download, Plus, Scan, Trash2
} from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'

defineOptions({ name: 'TableCreate' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})

const databaseName = String(route.params.database || '')
const loading = ref(false)
const saving = ref(false)
const engines = ref<any[]>([])
const dataTypes = ref<any[]>([])
const enginesFallback = ['InnoDB', 'MyISAM', 'MEMORY', 'ARCHIVE']
const sourceCode = route.params.source as string
const code = sourceCode

const formState = ref<any>({
  name: '',
  engine: 'InnoDB',
  comment: '',
  columns: []
})

const options = ref({
  charset: 'utf8mb4',
  collate: 'utf8mb4_0900_ai_ci',
  rowFormat: 'DYNAMIC',
  autoIncrement: ''
})

const advanced = ref({ continueIndex: false })

const { indexRows, indexTab, indexCounts, visibleIndexRows, canAddIndex, columnOptions,
  onAddIndex, removeIndex, firstInvalidIndex, indexDefinitions } = useTableCreateIndexes(
    () => formState.value.columns, () => formState.value.name)

const selectedRows = ref<Set<number>>(new Set())
const allSelected = computed(() => formState.value.columns.length > 0 && formState.value.columns.every((_column: any, index: number) => selectedRows.value.has(index)))
const partialSelected = computed(() => selectedRows.value.size > 0 && !allSelected.value)

const toggleAll = (checked: boolean) => {
  selectedRows.value = checked
      ? new Set(formState.value.columns.map((_column: any, index: number) => index))
      : new Set()
}

const toggleRow = (index: number, checked: boolean) => {
  const next = new Set(selectedRows.value)
  if (checked) next.add(index)
  else next.delete(index)
  selectedRows.value = next
}

const steps = [
  { titleKey: 'source.create.step1', descKey: 'source.create.step1Desc' },
  { titleKey: 'source.create.step2', descKey: 'source.create.step2Desc' },
  { titleKey: 'source.create.step3', descKey: 'source.create.step3Desc' },
  { titleKey: 'source.create.step4', descKey: 'source.create.step4Desc' }
]
const currentStep = ref(1)

const indexTabs = [
  { key: 'primary', labelKey: 'source.create.tabPrimary' },
  { key: 'normal', labelKey: 'source.create.tabNormal' },
  { key: 'unique', labelKey: 'source.create.tabUnique' },
  { key: 'foreign', labelKey: 'source.create.tabForeign' },
  { key: 'check', labelKey: 'source.create.tabCheck' }
] as const

const engineOptions = computed(() => {
  const list = engines.value.length ? engines.value : enginesFallback
  return list.map((engine: any) => ({ label: typeof engine === 'string' ? engine : engine.name || String(engine), value: typeof engine === 'string' ? engine : engine.name || String(engine) }))
})
const dataTypeOptions = computed(() => dataTypes.value.map((dataType: any) => ({ label: dataType, value: dataType })))

const CHARSETS = ['utf8mb4', 'utf8', 'latin1', 'gbk']
const COLLATES = ['utf8mb4_0900_ai_ci', 'utf8mb4_general_ci', 'utf8mb4_bin', 'utf8_general_ci', 'latin1_swedish_ci', 'gbk_chinese_ci']
const ROW_FORMATS = ['DYNAMIC', 'COMPACT', 'REDUNDANT', 'COMPRESSED']
const charsetOptions = CHARSETS.map(value => ({ label: value, value }))
const collateOptions = COLLATES.map(value => ({ label: value, value }))
const rowFormatOptions = ROW_FORMATS.map(value => ({ label: value, value }))
const methodOptions = ['BTREE', 'HASH'].map(value => ({ label: value, value }))

const sqlTabs = [
  { kind: 'full', labelKey: 'source.create.tabFullSql' },
  { kind: 'columns', labelKey: 'source.create.tabColumnsOnly' },
  { kind: 'indexes', labelKey: 'source.create.tabIndexesOnly' },
  { kind: 'options', labelKey: 'source.create.tabOptionsOnly' }
] as const
const sqlTab = ref<'full' | 'columns' | 'indexes' | 'options'>('full')
const formatting = ref(false)

const fieldColumns = [
  { title: '', dataIndex: '__check__', width: 44 },
  { title: t('source.structure.fieldName'), dataIndex: 'name' },
  { title: t('source.structure.type'), dataIndex: 'type', width: 140 },
  { title: t('source.create.lengthValue'), dataIndex: 'length', width: 88 },
  { title: t('source.common.defaultValue'), dataIndex: 'defaultValue', width: 140 },
  { title: t('source.structure.nullable'), dataIndex: 'nullable', width: 60 },
  { title: t('source.create.primary'), dataIndex: 'primaryKey', width: 60 },
  { title: t('source.create.autoIncCol'), dataIndex: 'autoIncrement', width: 60 },
  { title: t('source.create.uniqueCol'), dataIndex: 'unique', width: 60 },
  { title: t('source.structure.comment'), dataIndex: 'comment', width: 150 },
  { title: t('common.action'), dataIndex: 'action', fixed: 'right' as const }
]

const indexColumns = [
  { title: t('common.id'), dataIndex: '__index__', width: 48 },
  { title: t('common.name'), dataIndex: 'name' },
  { title: t('source.index.indexType'), dataIndex: 'kind', width: 110 },
  { title: t('source.create.indexColumns'), dataIndex: 'columns', width: 240, ellipsis: true },
  { title: t('source.create.indexMethod'), dataIndex: 'method', width: 130 },
  { title: t('source.structure.comment'), dataIndex: 'comment', width: 150 },
  { title: t('common.action'), dataIndex: 'action', fixed: 'right' as const }
]

const buildDefinition = () => ({
  name: formState.value.name?.trim(),
  comment: formState.value.comment || undefined,
  engine: formState.value.engine,
  charset: options.value.charset,
  collate: options.value.collate,
  format: options.value.rowFormat,
  autoIncrement: Number(options.value.autoIncrement) || 0,
  columns: formState.value.columns
      .filter((column: any) => column.name?.trim())
      .map((column: any) => ({
        name: column.name.trim(),
        type: column.type,
        length: Number(column.length) > 0 ? Number(column.length) : undefined,
        defaultValue: column.defaultValue || undefined,
        nullable: !!column.nullable,
        primaryKey: !!column.primaryKey,
        autoIncrement: !!column.autoIncrement,
        comment: column.comment || undefined
      })),
  indexes: indexDefinitions.value
})

const { sqlContent, sqlLoading, sqlError, previewContext, updateSqlContent, retryPreview } = useTableCreatePreview({
  active: () => visible.value && currentStep.value === 4,
  source: () => sourceCode,
  database: () => databaseName,
  definition: buildDefinition,
  kind: sqlTab,
  failureMessage: () => t('source.create.previewFailed')
})

const formRef = ref<any | null>(null)

// 字段按位置、索引按稳定行键解析，删除索引后不会打乱已注册的校验字段。
const formModel = new Proxy({} as Record<string, unknown>, {
  get(_target, prop: string) {
    const segments = String(prop).split('.')
    if (segments[0] === 'indexes') {
      const row = indexRows.value.find(index => index.__key === segments[1])
      return segments.length > 2 ? row?.[segments[2] as keyof IndexRow] : row
    }
    return segments.reduce((obj: any, key) => (obj ? obj[key] : undefined), formState.value)
  }
})

const onAdd = () => {
  formState.value.columns = [...formState.value.columns, {
    name: '', type: dataTypes.value[0] || 'VARCHAR', length: '', defaultValue: '',
    nullable: false, primaryKey: false, autoIncrement: false, unique: false, comment: ''
  }]
}

const onRemove = (index: number) => {
  formState.value.columns.splice(index, 1)
  const next = new Set<number>()
  selectedRows.value.forEach(value => { if (value !== index) next.add(value > index ? value - 1 : value) })
  selectedRows.value = next
}

const onMoveColumn = (index: number, direction: -1 | 1) => {
  const target = index + direction
  const list = formState.value.columns
  if (target < 0 || target >= list.length) return
  ;[list[index], list[target]] = [list[target], list[index]]
}

const onMoveSelected = (direction: -1 | 1) => {
  const indexes = [...selectedRows.value].sort((a, b) => a - b)
  if (!indexes.length) return
  const ordered = direction === -1 ? indexes : [...indexes].reverse()
  ordered.forEach(index => onMoveColumn(index, direction))
}

const onBatchDelete = () => {
  if (selectedRows.value.size === 0) {
    MfMessage.warning(t('source.create.selectRows'))
    return
  }
  formState.value.columns = formState.value.columns.filter((_column, index) => !selectedRows.value.has(index))
  selectedRows.value = new Set()
}

const onDeleteIndexRow = (row: IndexRow) => {
  removeIndex(row)
  MfMessage.success(t('source.create.indexRemoved'))
}

const onCopySql = () => {
  ObjectUtils.copy(sqlContent.value)
  MfMessage.success(t('query.tip.copySuccess'))
}

const onFormatSql = () => {
  const context = previewContext.value
  formatting.value = true
  FormatService.formatSql({ sql: sqlContent.value })
                 .then(response => {
                   if (response.status && typeof response.data === 'string') {
                     updateSqlContent(response.data, context)
                   }
                   else {
                     MfMessage.error(response.message)
                   }
                 })
                 .finally(() => (formatting.value = false))
}

const onDownloadSql = () => {
  const blob = new Blob([sqlContent.value], { type: 'text/sql;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${ formState.value.name || 'table' }.sql`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 4000)
}

const onNext = async () => {
  if (currentStep.value === 3) {
    const invalid = firstInvalidIndex()
    if (invalid) {
      indexTab.value = invalid.kind
      await nextTick()
    }
  }
  try {
    await formRef.value?.validate()
  }
  catch {
    return
  }
  if (currentStep.value < 4) {
    currentStep.value += 1
  }
}

const onPrev = () => {
  currentStep.value = Math.max(1, currentStep.value - 1)
}

const onCancel = () => {
  visible.value = false
}

const onSubmit = () => {
  saving.value = true
  const database = route.params.database as string
  const definition = buildDefinition()

  MetadataService.createTable(code, database, definition)
                 .then(response => {
                   if (response.status && response.data && response.data.isSuccessful) {
                     MfMessage.success(t('source.tip.createTableSuccess').replace('$VALUE', String(definition.name)))
                     visible.value = false
                     router.push(sourceManageLocation({ source: code, database, table: definition.name,
                       tab: advanced.value.continueIndex ? 'indexes' : 'structure' }))
                   }
                   else {
                     MfMessage.error(response.data?.message || response.message)
                   }
                 })
                 .finally(() => (saving.value = false))
}

loading.value = true
HttpUtils.all([MetadataService.getEngines(code), MetadataService.getDataTypes(code)])
         .then(HttpUtils.spread((...responses: any[]) => {
           const [enginesResponse, dataTypesResponse] = responses

           if (enginesResponse.status && enginesResponse.data && enginesResponse.data.isSuccessful) {
             engines.value = enginesResponse.data.columns
           }

           if (dataTypesResponse.status && dataTypesResponse.data && dataTypesResponse.data.isSuccessful) {
             dataTypes.value = dataTypesResponse.data.columns
           }
         }))
         .finally(() => (loading.value = false))

</script>
