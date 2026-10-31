<template>
  <div class="source-data flex h-full min-h-0 flex-col gap-3 overflow-hidden">
    <SourceDataFilter :conditions="conditions" :columns="columnNames" :type-of="typeOf" :loading="loading"
                      @add="addCondition()" @clear="clearConditions" @remove="removeCondition" @search="onSearch" @reset="onReset"
                      @change-column="changeColumn" @change-operator="changeOperator"/>

    <div class="flex shrink-0 flex-wrap items-center justify-between gap-2">
      <div class="flex flex-wrap items-center gap-2">
        <span class="mr-2 text-sm text-content-tertiary">{{ $t('source.data.total', { count: total.toLocaleString() }) }}</span>
        <MfButton v-if="can('data:insert')" size="sm" :disabled="saving" @click="openEditor('add')"><Plus :size="14"/>{{ $t('source.data.add') }}</MfButton>
        <MfButton v-if="can('data:modify')" size="sm" :disabled="saving || selectedRows.length !== 1" @click="openEditor('edit', selectedRows)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
        <MfButton v-if="can('data:delete')" size="sm" variant="danger" :disabled="saving || !selectedRows.length" @click="requestDelete(selectedRows)"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfButton>
        <MfButton v-if="can('data:insert')" size="sm" :disabled="saving || !selectedRows.length" @click="openEditor('copy', selectedRows)"><Copy :size="14"/>{{ $t('source.data.copy') }}</MfButton>
        <MfDropdown trigger="click">
          <MfButton size="sm" :disabled="!rows.length" :loading="exporting"><Download :size="14"/>{{ $t('source.data.export') }}<ChevronDown :size="12"/></MfButton>
          <template #overlay>
            <MfMenu>
              <MfMenuItem v-for="format in exportFormats" :key="format" @click="exportRows(format)">
                {{ $t(format === 'csv' ? 'source.data.exportCsv' : 'source.data.exportJson') }}<span v-if="format === settings.exportFormat" class="ml-2 text-xs text-content-tertiary">{{ $t('source.data.defaultMark') }}</span>
              </MfMenuItem>
            </MfMenu>
          </template>
        </MfDropdown>
      </div>
      <div class="flex items-center gap-2">
        <MfButton v-if="executed" size="sm" @click="executedVisible = true">
          <CircleCheck v-if="executed.success" :size="14" class="text-success"/><CircleX v-else :size="14" class="text-danger"/>{{ $t('source.executed.view') }}
        </MfButton>
        <MfButton size="sm" :disabled="!columnNames.length || saving" @click="openPanel('columns')"><Columns3 :size="14"/>{{ $t('source.data.columnSettings') }}</MfButton>
        <MfButton size="sm" :disabled="!columnNames.length || saving" @click="openPanel('sort')">
          <ArrowUpDown :size="14"/>{{ orders.length ? $t('source.data.sortCount', { count: orders.length }) : $t('source.data.sort') }}
        </MfButton>
        <MfButton size="sm" :disabled="saving" :aria-label="$t('source.data.settings')" @click="openPanel('settings')"><Settings :size="14"/></MfButton>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col rounded-mf-md border border-line" :class="columnView.stickyHeader ? 'overflow-hidden' : 'overflow-auto'">
      <MfTable :fill="columnView.stickyHeader" :columns="tableColumns" :data-source="rows" row-key="__key" density="compact" :bordered="columnView.bordered" :striped="settings.striped" :hoverable="settings.hoverable" :row-borders="settings.rowBorders" :loading="loading" :empty-text="$t('common.noData')"
               :row-class="rowClass" class="cursor-default" :resizable="!columnView.autoWidth"
               @resize="(column, width) => resizeColumn(column.dataIndex, width, false)" @resize-end="(column, width) => resizeColumn(column.dataIndex, width, true)" @row-click="onRowClick" @mousedown="onTableMouseDown">
        <template #header-__check__><MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :aria-label="$t('source.data.selectAll')" @update:model-value="value => toggleAll(!!value)"/></template>
        <template #cell-__check__="{ record, index }"><span class="inline-flex" @click.stop><MfCheckbox :model-value="selected.has(record.__key)" :aria-label="$t('source.data.selectRow')" @update:model-value="value => onCheck(record, index, !!value)"/></span></template>
        <template #cell-__index__="{ index }"><span class="tabular-nums text-content-secondary">{{ (page - 1) * size + index + 1 }}</span></template>
        <template v-for="name in visibleColumns" :key="name" #[`cell-${name}`]="{ record }">
          <span v-if="record[name] === null || record[name] === undefined" class="text-content-tertiary">NULL</span>
          <div v-else :class="columnView.autoWidth && 'max-w-72'"><EllipsisCell :text="cellText(record[name])"/></div>
        </template>
        <template #cell-__action__="{ record }">
          <div class="flex items-center gap-4 text-xs" @click.stop>
            <button v-if="can('data:modify')" type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="saving" @click="openEditor('edit', [record])">{{ $t('common.edit') }}</button>
            <button v-if="can('data:delete')" type="button" class="text-danger hover:underline disabled:opacity-50" :disabled="saving" @click="requestDelete([record])">{{ $t('common.delete') }}</button>
          </div>
        </template>
      </MfTable>
    </div>

    <div class="relative flex shrink-0 items-center justify-center pb-1">
      <MfPagination :current="page" :page-size="size" :total="total" :size-text="$t('source.data.pageSize', { size: '{size}' })" @change="onPageChange"/>
      <label v-if="total > size" class="absolute right-0 flex items-center gap-2 whitespace-nowrap text-sm text-content-secondary">
        <span>{{ $t('source.data.jumpTo') }}</span>
        <div class="w-16 shrink-0"><MfInput v-model="jump" size="sm" @keydown.enter="onJump"/></div>
        <span>{{ $t('source.data.jumpPage') }}</span>
      </label>
    </div>

    <SourceRowEditor v-if="editorVisible" v-model="editorValues" v-model:json="editorJson" v-model:continue-adding="continueAdding" :open="editorVisible"
                     :mode="editorMode" :tab="editorTab" :form-disabled="editorMultiple" :fields="fields" :issues="editorIssues" :json-error="jsonError" :saving="saving"
                     :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @tab="setEditorTab" @save="submitEditor"/>
    <SourceColumnSettings v-if="sidePanel === 'columns'" :open="sidePanel === 'columns'" :view="columnView" :defaults="defaultView"
                          :width="fieldPanelWidth" :container="fieldPanelHost" @close="closePanel" @apply="applyColumnView"/>
    <SourceSortSettings v-if="sidePanel === 'sort'" :open="sidePanel === 'sort'" :rules="orders" :fields="columnNames" :quick="quickSortRules"
                        :width="fieldPanelWidth" :container="fieldPanelHost" @close="closePanel" @apply="applyOrders"/>
    <SourceTableSettings v-if="sidePanel === 'settings'" :open="sidePanel === 'settings'" :settings="settings" :bordered="columnView.bordered" :default-sort="defaultSort"
                         :quick="quickSortRules" :width="fieldPanelWidth" :container="fieldPanelHost" @close="closePanel" @apply="applySettings"/>
    <SourceDataDelete :open="!!deleteTargets.length" :rows="deleteRows" :fields="fields" :columns="visibleColumns" :primary-keys="primaryKeys" :saving="saving"
                      @close="closeDelete" @confirm="confirmDelete"/>
    <ExecutedSqlDialog v-model:open="executedVisible" :executed="executed"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowUpDown, CircleCheck, CircleX, ChevronDown, Columns3, Copy, Download, Pencil, Plus, Settings, Trash2 } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import SourceDataDelete from './components/SourceDataDelete.vue'
import ExecutedSqlDialog from '@/views/components/sql/ExecutedSqlDialog.vue'
import SourceDataFilter from './components/SourceDataFilter.vue'
import SourceRowEditor from './components/SourceRowEditor.vue'
import SourceColumnSettings from './components/SourceColumnSettings.vue'
import SourceSortSettings from './components/SourceSortSettings.vue'
import SourceTableSettings from './components/SourceTableSettings.vue'
import { INDEX_COLUMN } from '@/model/source-data-view'
import { useSourceTableData } from '@/composables/useSourceTableData'

defineOptions({ name: 'SourceTableData' })
const { t } = useI18n()
const {
  can, loading, saving, rows, total, page, size, columnNames, visibleColumns, orders, typeOf,
  sidePanel, openPanel, closePanel, columnView, applyColumnView, resizeColumn, defaultView, settings, defaultSort, applySettings, exporting,
  conditions, addCondition, changeColumn, changeOperator, removeCondition, clearConditions, onSearch, onReset,
  onPageChange, applyOrders, quickSortRules, selected, selectedRows, allSelected, partialSelected, toggleAll, onRowClick, onCheck, rowClass,
  editorVisible, editorMode, editorValues, editorTab, editorJson, editorIssues, jsonError, continueAdding, editorMultiple, fields, openEditor, closeEditor, setEditorTab, submitEditor, fieldPanelHost, fieldPanelWidth,
  deleteTargets, deleteRows, requestDelete, closeDelete, confirmDelete, primaryKeys, executed, executedVisible, exportRows,
} = useSourceTableData()

// Columns follow the applied column settings: order, visibility, width, alignment and pinning.
const tableColumns = computed(() => {
  const view = columnView.value
  const configured = view.columns.filter(column => column.visible).map(column => ({
    title: column.name === INDEX_COLUMN ? '#' : column.name,
    dataIndex: column.name === INDEX_COLUMN ? '__index__' : column.name,
    width: view.autoWidth ? undefined : column.width,
    align: column.align,
    fixed: column.fixed ? 'left' as const : undefined,
    ellipsis: !view.autoWidth && column.name !== INDEX_COLUMN,
  }))
  return [
    { title: '', dataIndex: '__check__', width: 44, fixed: 'left' as const, resizable: false },
    ...configured,
    ...(settings.value.showActions && (can('data:modify') || can('data:delete')) ? [{ title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const, resizable: false }] : []),
  ]
})
// The default export format is listed first.
const exportFormats = computed(() => settings.value.exportFormat === 'json' ? ['json', 'csv'] as const : ['csv', 'json'] as const)
const cellText = (value: unknown) => typeof value === 'object' ? JSON.stringify(value) : String(value)
// Shift-click extends the row selection; keep the browser from selecting text across rows.
const onTableMouseDown = (event: MouseEvent) => { if (event.shiftKey) event.preventDefault() }
const jump = ref('')
function onJump() {
  const target = Math.trunc(Number(jump.value))
  const pages = Math.max(1, Math.ceil(total.value / size.value))
  if (target >= 1 && target <= pages) onPageChange(target, size.value)
  jump.value = ''
}
</script>
