<template>
  <div class="flex h-full min-h-0 flex-col gap-3 overflow-hidden">
    <div class="flex shrink-0 flex-wrap items-center gap-2">
      <MfButton v-if="can('index:create')" size="sm" variant="primary" :disabled="saving" @click="openEditor('add')"><Plus :size="14"/>{{ $t('source.idx.create') }}</MfButton>
      <MfButton v-if="can('index:drop')" size="sm" :disabled="saving || !selected.size" @click="requestDrop(indexes.filter(index => selected.has(index.name)))"><Trash2 :size="14"/>{{ $t('source.idx.drop') }}</MfButton>
      <MfButton size="sm" :loading="loading" @click="reload"><RotateCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
      <MfButton v-if="executed" size="sm" @click="executedVisible = true">
        <CircleCheck v-if="executed.success" :size="14" class="text-success"/><CircleX v-else :size="14" class="text-danger"/>{{ $t('source.executed.view') }}
      </MfButton>
    </div>

    <div class="flex min-h-48 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <MfTable fill :columns="headers" :data-source="indexes" row-key="name" density="compact" :bordered="false" :loading="loading" :empty-text="$t('common.noData')"
               :row-class="rowClass" class="cursor-default" @row-click="onRowClick" @mousedown="onTableMouseDown">
        <template #header-__check__><MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :disabled="!indexes.some(index => !index.primary)" :aria-label="$t('source.data.selectAll')" @update:model-value="value => toggleAll(!!value)"/></template>
        <template #cell-__check__="{ record, index }"><span class="inline-flex" @click.stop><MfCheckbox :model-value="selected.has(record.name)" :disabled="record.primary" :aria-label="$t('source.data.selectRow')" @update:model-value="value => onCheck(record, index, !!value)"/></span></template>
        <template #cell-__index__="{ index }"><span class="tabular-nums text-content-secondary">{{ index + 1 }}</span></template>
        <template #cell-name="{ record }"><span class="inline-flex items-center gap-1.5">{{ record.name }}<Crown v-if="record.primary" :size="14" class="text-warning"/></span></template>
        <template #cell-method="{ record }"><MfTag size="sm" :variant="methodTone(record.method)" :color="record.method === 'FULLTEXT' ? 'var(--mf-color-data-complex)' : undefined">{{ record.method }}</MfTag></template>
        <template #cell-unique="{ record }"><MfTag size="sm" :variant="record.unique ? 'success' : 'danger'">{{ $t(record.unique ? 'source.idx.uniqueTag' : 'common.no') }}</MfTag></template>
        <template #cell-columns="{ record }"><div class="max-w-64"><EllipsisCell :text="columnsLabel(record)"/></div></template>
        <template #cell-cardinality="{ record }"><span class="tabular-nums">{{ formatNumber(record.cardinality) }}</span></template>
        <template #cell-keyLength="{ record }"><span class="tabular-nums">{{ formatNumber(record.keyLength) }}</span></template>
        <template #cell-comment="{ record }"><div class="max-w-64"><EllipsisCell :text="record.comment"/></div></template>
        <template #cell-__action__="{ record }">
          <div class="flex items-center gap-4 text-xs" @click.stop>
            <button type="button" class="text-content-secondary hover:text-content" @click="detailName = record.name">{{ $t('source.idx.view') }}</button>
            <button v-if="can('index:create') && can('index:drop')" type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="saving" @click="openEditor('edit', record)">{{ $t('common.edit') }}</button>
            <button v-if="can('index:drop')" type="button" class="text-danger hover:underline disabled:cursor-not-allowed disabled:text-content-tertiary disabled:no-underline" :disabled="saving || record.primary" @click="requestDrop([record])">{{ $t('common.delete') }}</button>
          </div>
        </template>
      </MfTable>
    </div>

    <section v-if="detail" class="flex max-h-[48%] shrink-0 flex-col overflow-hidden rounded-mf-md border border-line">
      <header class="flex shrink-0 flex-wrap items-start justify-between gap-3 border-b border-line px-4 py-3">
        <div class="min-w-0">
          <div class="flex flex-wrap items-center gap-2">
            <h3 class="m-0 truncate text-lg font-semibold text-content">{{ detail.name }}</h3>
            <MfTag size="sm" :variant="detail.unique ? 'success' : 'default'">{{ kindLabel(detail) }}</MfTag>
            <MfTag size="sm" :variant="methodTone(detail.method)">{{ detail.method }}</MfTag>
          </div>
          <p v-if="detail.comment" class="m-0 mt-1 text-sm text-content-secondary">{{ detail.comment }}</p>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <MfButton v-if="can('index:create') && can('index:drop')" size="sm" :disabled="saving" @click="openEditor('edit', detail)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
          <MfButton v-if="can('index:create') && can('index:drop')" size="sm" :loading="saving" @click="rebuild(detail)"><RefreshCw :size="14"/>{{ $t('source.idx.rebuild') }}</MfButton>
          <MfButton v-if="can('index:drop')" size="sm" variant="danger" :disabled="saving || detail.primary" @click="requestDrop([detail])"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfButton>
        </div>
      </header>
      <div class="grid min-h-0 flex-1 grid-cols-1 gap-0 overflow-y-auto lg:grid-cols-[minmax(0,1fr)_minmax(0,1.6fr)_minmax(0,1fr)]">
        <div class="border-line p-4 lg:border-r">
          <h4 class="m-0 mb-3 text-sm font-semibold text-content">{{ $t('source.idx.basic') }}</h4>
          <dl class="m-0 grid grid-cols-[auto_minmax(0,1fr)] gap-x-6 gap-y-2 text-sm">
            <template v-for="item in basics" :key="item.label">
              <dt class="text-content-secondary">{{ item.label }}</dt><dd class="m-0 break-words" :class="item.tone || 'text-content'">{{ item.value }}</dd>
            </template>
          </dl>
        </div>
        <div class="flex min-w-0 flex-col gap-4 border-line p-4 lg:border-r">
          <div>
            <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.idx.columns') }}</h4>
            <div class="overflow-hidden rounded-mf-md border border-line">
              <MfTable :columns="columnHeaders" :data-source="detailColumns" row-key="key" density="compact" :bordered="false" :hoverable="false"/>
            </div>
          </div>
          <div>
            <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.idx.usage') }}</h4>
            <div class="grid grid-cols-3 gap-2">
              <div v-for="tile in usage" :key="tile.label" class="rounded-mf-md bg-surface-hover px-3 py-2" :title="tile.title">
                <p class="m-0 text-xs text-content-tertiary">{{ tile.label }}</p>
                <p class="m-0 mt-1 text-sm tabular-nums text-content">{{ tile.value }}</p>
              </div>
            </div>
          </div>
          <div>
            <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.idx.statement') }}</h4>
            <div class="overflow-hidden rounded-mf-md bg-code-bg">
              <div class="flex items-center justify-between bg-code-header px-3 py-1.5">
                <span class="rounded-mf-sm bg-code-bg px-2 py-0.5 text-xs font-medium text-code-muted">SQL</span>
                <button type="button" class="inline-flex items-center gap-1.5 rounded-mf-sm px-2 py-1 text-xs text-code-fg hover:bg-code-bg disabled:opacity-50" :disabled="!detailSql" @click="copy"><Copy :size="13"/>{{ $t('source.executed.copy') }}</button>
              </div>
              <pre class="m-0 whitespace-pre-wrap break-words px-4 py-3 font-mono text-xs leading-5 text-code-fg"><span v-for="(token, index) in statementTokens" :key="index" :class="CODE_CLASS[token.kind]">{{ token.text }}</span><span v-if="!detailSql" class="text-code-muted">-</span></pre>
            </div>
          </div>
        </div>
        <div class="flex min-w-0 flex-col p-4">
          <h4 class="m-0 mb-3 text-sm font-semibold text-content">{{ $t('source.idx.structure') }}</h4>
          <div class="flex flex-col items-center overflow-x-auto py-2">
            <div class="rounded-mf-md border border-primary/30 bg-primary/10 px-4 py-2 text-center text-xs text-content">{{ detail.name }}<br><span class="text-content-secondary">({{ detail.method }})</span></div>
            <div class="h-4 w-px bg-primary/40"/>
            <div class="flex">
              <div v-for="(column, index) in detail.columns" :key="column.name" class="relative flex flex-col items-center px-2 pt-4">
                <span v-if="index > 0" class="absolute left-0 right-1/2 top-0 h-px bg-primary/40"/>
                <span v-if="index < detail.columns.length - 1" class="absolute left-1/2 right-0 top-0 h-px bg-primary/40"/>
                <span class="absolute left-1/2 top-0 h-4 w-px bg-primary/40"/>
                <div class="rounded-mf-md border border-primary/30 bg-primary/5 px-3 py-2 text-center text-xs text-content">{{ column.name }}<template v-if="column.order"><br><span class="text-content-secondary">({{ column.order }})</span></template></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <SourceIndexEditor v-if="editorVisible" v-model="form" :open="editorVisible" :mode="editorMode" :columns="tableColumns" :issues="issues" :preview-sql="previewSql"
                       :preview-error="previewError" :saving="saving" :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @save="submitEditor"/>
    <SourceIndexDrop :open="!!dropTargets.length" :indexes="dropTargets" :saving="saving" @close="closeDrop" @confirm="confirmDrop"/>
    <ExecutedSqlDialog v-model:open="executedVisible" :executed="executed"/>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleCheck, CircleX, Copy, Crown, Pencil, Plus, RefreshCw, RotateCw, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ExecutedSqlDialog from '@/views/components/sql/ExecutedSqlDialog.vue'
import SourceIndexEditor from './components/SourceIndexEditor.vue'
import SourceIndexDrop from './components/SourceIndexDrop.vue'
import { tokenizeSql, type SqlTokenKind } from '@/model/executed-sql'
import { columnsLabel, type IndexMethod, type SourceIndex } from '@/model/source-index'
import { useSourceTableIndexes } from '@/composables/useSourceTableIndexes'

defineOptions({ name: 'SourceTableIndexes' })
const { t } = useI18n()
const {
  can, loading, saving, indexes, tableColumns, selected, allSelected, partialSelected, toggleAll, onRowClick, onCheck, detail, detailName, detailSql,
  editorVisible, editorMode, form, issues, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
  openEditor, closeEditor, submitEditor, rebuild, dropTargets, requestDrop, closeDrop, confirmDrop, reload, executed, executedVisible,
} = useSourceTableIndexes()

const headers = computed(() => [
  { title: '', dataIndex: '__check__', width: 44 },
  { title: '#', dataIndex: '__index__', width: 48 },
  { title: t('source.idx.name'), dataIndex: 'name' },
  { title: t('source.idx.method'), dataIndex: 'method' },
  { title: t('source.idx.uniqueness'), dataIndex: 'unique' },
  { title: t('source.idx.columns'), dataIndex: 'columns' },
  { title: t('source.idx.cardinality'), dataIndex: 'cardinality' },
  { title: t('source.idx.keyLength'), dataIndex: 'keyLength' },
  { title: t('source.idx.comment'), dataIndex: 'comment' },
  { title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const },
])
// Checked rows take the hover background; the open detail row keeps a light primary tint.
const rowClass = (index: SourceIndex) => selected.value.has(index.name) ? 'bg-surface-hover [&>td]:bg-surface-hover'
    : index.name === detailName.value ? 'bg-primary/5 [&>td]:bg-primary/5' : ''
// Shift-click extends the selection; keep the browser from selecting text across rows.
const onTableMouseDown = (event: MouseEvent) => { if (event.shiftKey) event.preventDefault() }
const methodTone = (method: IndexMethod) => method === 'BTREE' ? 'info' : method === 'HASH' ? 'warning' : 'default'
const formatNumber = (value: number | null) => value === null ? '-' : value.toLocaleString()
const kindLabel = (index: SourceIndex) => t(index.primary ? 'source.idx.kindPrimary' : index.method === 'FULLTEXT' ? 'source.idx.kindFulltext'
    : index.method === 'SPATIAL' ? 'source.idx.kindSpatial' : index.unique ? 'source.idx.kindUnique' : 'source.idx.kindNormal')
const basics = computed(() => {
  const index = detail.value
  if (!index) return []
  return [
    { label: t('source.idx.name'), value: index.name },
    { label: t('source.idx.uniqueness'), value: t(index.unique ? 'common.yes' : 'common.no'), tone: index.unique ? 'text-success' : 'text-danger' },
    { label: t('source.idx.method'), value: index.method },
    { label: t('source.idx.cardinality'), value: formatNumber(index.cardinality) },
    { label: t('source.idx.keyLength'), value: formatNumber(index.keyLength) },
    { label: t('source.idx.comment'), value: index.comment || '-' },
  ]
})
const columnHeaders = computed(() => [
  { title: '#', dataIndex: 'position', width: 40 },
  { title: t('source.idx.column'), dataIndex: 'name' },
  { title: t('source.idx.order'), dataIndex: 'order' },
  { title: t('source.idx.subPart'), dataIndex: 'subPart' },
])
const detailColumns = computed(() => (detail.value?.columns || []).map((column, index) => ({
  key: `${index}:${column.name}`, position: index + 1, name: column.name,
  order: column.order ? t(column.order === 'ASC' ? 'source.idx.asc' : 'source.idx.desc') : '-', subPart: column.subPart ?? '-',
})))
const usage = computed(() => [
  { label: t('source.idx.usageCount'), value: formatNumber(detail.value?.usageCount ?? null), title: t('source.idx.usageSource') },
  { label: t('source.idx.usageRows'), value: formatNumber(detail.value?.usageRows ?? null), title: t('source.idx.usageSource') },
  { label: t('source.idx.lastUsed'), value: '-', title: t('source.idx.lastUsedUnavailable') },
])
const CODE_CLASS: Record<SqlTokenKind, string> = {
  keyword: 'text-code-keyword', type: 'text-code-type', string: 'text-code-string', identifier: 'text-code-identifier',
  number: 'text-code-number', comment: 'text-code-muted', space: '', plain: '',
}
const statementTokens = computed(() => tokenizeSql(detailSql.value))
function copy() {
  ObjectUtils.copy(detailSql.value)
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
