<template>
  <div class="flex h-full min-h-0 flex-col gap-3 overflow-hidden">
    <div class="flex shrink-0 flex-wrap items-center gap-2">
      <MfButton v-if="can('foreign-key:create')" size="sm" variant="primary" :disabled="saving" @click="openEditor('add')"><Plus :size="14"/>{{ $t('source.fk.create') }}</MfButton>
      <MfButton v-if="can('foreign-key:drop')" size="sm" :disabled="saving || !selected.size" @click="requestDrop(keys.filter(key => selected.has(key.name)))"><Trash2 :size="14"/>{{ $t('source.fk.drop') }}</MfButton>
      <MfButton size="sm" :loading="loading" @click="reload"><RotateCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
      <MfButton v-if="executed" size="sm" @click="executedVisible = true">
        <CircleCheck v-if="executed.success" :size="14" class="text-success"/><CircleX v-else :size="14" class="text-danger"/>{{ $t('source.executed.view') }}
      </MfButton>
    </div>

    <div class="flex min-h-40 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <MfTable fill :columns="headers" :data-source="keys" row-key="name" density="compact" :bordered="false" :loading="loading" :empty-text="$t('common.noData')"
               :row-class="rowClass" class="cursor-default" @row-click="onRowClick" @mousedown="onTableMouseDown">
        <template #header-__check__><MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :disabled="!keys.length" :aria-label="$t('source.data.selectAll')" @update:model-value="value => toggleAll(!!value)"/></template>
        <template #cell-__check__="{ record, index }"><span class="inline-flex" @click.stop><MfCheckbox :model-value="selected.has(record.name)" :aria-label="$t('source.data.selectRow')" @update:model-value="value => onCheck(record, index, !!value)"/></span></template>
        <template #cell-__index__="{ index }"><span class="tabular-nums text-content-secondary">{{ index + 1 }}</span></template>
        <template #cell-columns="{ record }"><EllipsisCell :text="record.columns.join(', ')"/></template>
        <template #cell-referencedTable="{ record }"><EllipsisCell :text="referencedLabel(record)"/></template>
        <template #cell-referencedColumns="{ record }"><EllipsisCell :text="record.referencedColumns.join(', ')"/></template>
        <template #cell-valid="{ record }"><MfTag size="sm" :variant="record.valid ? 'success' : 'danger'">{{ $t(record.valid ? 'source.fk.valid' : 'source.fk.invalid') }}</MfTag></template>
        <template #cell-__action__="{ record }">
          <div class="flex items-center gap-4 text-xs" @click.stop>
            <button type="button" class="text-content-secondary hover:text-content" @click="detailName = record.name">{{ $t('source.idx.view') }}</button>
            <button v-if="can('foreign-key:create') && can('foreign-key:drop')" type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="saving" @click="openEditor('edit', record)">{{ $t('common.edit') }}</button>
            <button v-if="can('foreign-key:drop')" type="button" class="text-danger hover:underline disabled:opacity-50" :disabled="saving" @click="requestDrop([record])">{{ $t('common.delete') }}</button>
          </div>
        </template>
      </MfTable>
    </div>

    <!-- The header stays put; only the details below it scroll. -->
    <section v-if="detail" class="flex max-h-[55%] shrink-0 flex-col overflow-hidden rounded-mf-md border border-line">
      <header class="flex shrink-0 flex-wrap items-center justify-between gap-3 border-b border-line px-4 py-3">
        <div class="flex min-w-0 items-center gap-2">
          <h3 class="m-0 truncate text-lg font-semibold text-content">{{ detail.name }}</h3>
          <MfTag size="sm" :variant="detail.valid ? 'success' : 'danger'">{{ $t(detail.valid ? 'source.fk.valid' : 'source.fk.invalid') }}</MfTag>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <MfButton v-if="can('foreign-key:create') && can('foreign-key:drop')" size="sm" :disabled="saving" @click="openEditor('edit', detail)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
          <MfButton v-if="can('foreign-key:create') && can('foreign-key:drop')" size="sm" :loading="saving" @click="rebuild(detail)"><RefreshCw :size="14"/>{{ $t('source.idx.rebuild') }}</MfButton>
          <MfButton v-if="can('foreign-key:drop')" size="sm" variant="danger" :disabled="saving" @click="requestDrop([detail])"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfButton>
        </div>
      </header>
      <div class="min-h-0 flex-1 overflow-y-auto">
        <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.7fr)]">
          <div class="flex flex-col gap-4 border-line p-4 lg:border-r">
            <div>
              <h4 class="m-0 mb-3 text-sm font-semibold text-content">{{ $t('source.idx.basic') }}</h4>
              <dl class="m-0 grid grid-cols-[auto_minmax(0,1fr)] gap-x-6 gap-y-2 text-sm">
                <template v-for="item in basics" :key="item.label">
                  <dt class="text-content-secondary">{{ item.label }}</dt><dd class="m-0 break-words" :class="item.tone || 'text-content'">{{ item.value }}</dd>
                </template>
              </dl>
            </div>
            <div>
              <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.fk.mapping') }}</h4>
              <div class="overflow-hidden rounded-mf-md border border-line">
                <MfTable :columns="mappingHeaders" :data-source="mappingRows" row-key="key" density="compact" :bordered="false" :hoverable="false">
                  <template #cell-arrow><ArrowRight :size="14" class="text-content-tertiary"/></template>
                </MfTable>
              </div>
            </div>
          </div>
          <div class="flex min-w-0 flex-col gap-4 p-4">
            <div>
              <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.fk.diagram') }}</h4>
              <SourceForeignKeyDiagram :table="table" :shape="localShape" :referenced-table="detail.referencedTable" :referenced-shape="shapeFor(detail.referencedDatabase, detail.referencedTable)"
                                       :columns="detail.columns" :referenced-columns="detail.referencedColumns"/>
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
        </div>
      </div>
    </section>

    <SourceForeignKeyEditor v-if="editorVisible" v-model="form" :open="editorVisible" :mode="editorMode" :columns="localShape.columns.map(column => column.name)" :tables="tables"
                            :referenced-columns="(editorShape?.columns || []).map(column => column.name)" :issues="issues" :preview-sql="previewSql" :preview-error="previewError"
                            :saving="saving" :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @save="submitEditor"/>
    <SourceForeignKeyDrop :open="!!dropTargets.length" :keys="dropTargets" :saving="saving" @close="closeDrop" @confirm="confirmDrop"/>
    <ExecutedSqlDialog v-model:open="executedVisible" :executed="executed"/>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight, CircleCheck, CircleX, Copy, Pencil, Plus, RefreshCw, RotateCw, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ExecutedSqlDialog from '@/views/components/sql/ExecutedSqlDialog.vue'
import SourceForeignKeyEditor from './components/SourceForeignKeyEditor.vue'
import SourceForeignKeyDrop from './components/SourceForeignKeyDrop.vue'
import SourceForeignKeyDiagram from './components/SourceForeignKeyDiagram.vue'
import { tokenizeSql, type SqlTokenKind } from '@/model/executed-sql'
import type { SourceForeignKey } from '@/model/source-foreign-key'
import { useSourceManageRoute } from '@/composables/useSourceManageRoute'
import { useSourceTableForeignKeys } from '@/composables/useSourceTableForeignKeys'

defineOptions({ name: 'SourceTableForeignKeys' })
const { t } = useI18n()
const route = useSourceManageRoute()
const {
  can, loading, saving, keys, tables, localShape, shapeFor, editorShape, selected, allSelected, partialSelected, toggleAll, onRowClick, onCheck,
  detail, detailName, detailSql, editorVisible, editorMode, form, issues, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
  openEditor, closeEditor, submitEditor, rebuild, dropTargets, requestDrop, closeDrop, confirmDrop, reload, executed, executedVisible,
} = useSourceTableForeignKeys()
const table = computed(() => String(route.params.table || ''))

const headers = computed(() => [
  { title: '', dataIndex: '__check__', width: 44 },
  { title: '#', dataIndex: '__index__', width: 48 },
  { title: t('source.fk.name'), dataIndex: 'name' },
  { title: t('source.fk.columns'), dataIndex: 'columns' },
  { title: t('source.fk.referencedTable'), dataIndex: 'referencedTable' },
  { title: t('source.fk.referencedColumns'), dataIndex: 'referencedColumns' },
  { title: t('source.fk.onUpdate'), dataIndex: 'onUpdate' },
  { title: t('source.fk.onDelete'), dataIndex: 'onDelete' },
  { title: t('source.fk.status'), dataIndex: 'valid' },
  { title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const },
])
/** A table in another database is shown with its database. */
const referencedLabel = (key: SourceForeignKey) => key.referencedDatabase && key.referencedDatabase !== String(route.params.database) ? `${key.referencedDatabase}.${key.referencedTable}` : key.referencedTable
// Checked rows take the hover background; the open detail row keeps a light primary tint.
const rowClass = (key: SourceForeignKey) => selected.value.has(key.name) ? 'bg-surface-hover [&>td]:bg-surface-hover'
    : key.name === detailName.value ? 'bg-primary/5 [&>td]:bg-primary/5' : ''
const onTableMouseDown = (event: MouseEvent) => { if (event.shiftKey) event.preventDefault() }
const basics = computed(() => {
  const key = detail.value
  if (!key) return []
  return [
    { label: t('source.fk.name'), value: key.name },
    { label: t('source.fk.ownerTable'), value: table.value },
    { label: t('source.fk.referencedTable'), value: referencedLabel(key) },
    { label: t('source.fk.columns'), value: key.columns.join(', ') },
    { label: t('source.fk.referencedColumns'), value: key.referencedColumns.join(', ') },
    { label: t('source.fk.onUpdate'), value: key.onUpdate },
    { label: t('source.fk.onDelete'), value: key.onDelete },
    { label: t('source.fk.status'), value: t(key.valid ? 'source.fk.valid' : 'source.fk.invalid'), tone: key.valid ? 'text-success' : 'text-danger' },
  ]
})
const mappingHeaders = computed(() => [
  { title: '#', dataIndex: 'position', width: 40 },
  { title: t('source.fk.columns'), dataIndex: 'column' },
  { title: '', dataIndex: 'arrow', width: 40 },
  { title: t('source.fk.referencedColumns'), dataIndex: 'referenced' },
])
const mappingRows = computed(() => (detail.value?.columns || []).map((column, index) => ({ key: `${index}`, position: index + 1, column, referenced: detail.value?.referencedColumns[index] || '' })))
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
