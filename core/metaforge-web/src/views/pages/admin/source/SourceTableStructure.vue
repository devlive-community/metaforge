<template>
  <div class="source-structure flex h-full min-h-0 min-w-0 flex-col overflow-hidden">
    <div v-if="loading" class="flex justify-center py-16"><MfSpinner :label="$t('common.loading')"/></div>
    <template v-else>
      <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-md border border-line bg-surface/60">
        <div v-if="can('column:create') || can('column:modify') || can('column:drop')" class="flex shrink-0 flex-wrap items-center gap-2 px-2.5 py-2.5">
          <MfButton v-if="can('column:create')" size="sm" variant="primary" :disabled="saving" @click="openPanel('add')"><Plus :size="14"/>{{ $t('source.structure.addField') }}</MfButton>
          <MfButton v-if="can('column:create')" size="sm" :disabled="saving || !detailColumn" @click="onInsertField"><ListPlus :size="14"/>{{ $t('source.structure.insertField') }}</MfButton>
          <MfButton v-if="can('column:drop')" size="sm" :disabled="saving || (!selectedNames.size && !detailColumn)" :loading="dropChecking" @click="onRequestDrop"><Trash2 :size="14"/>{{ $t('source.structure.dropTitle') }}</MfButton>
          <MfButton v-if="can('column:reorder')" size="sm" :disabled="saving || !detailColumn || detailColumn.position <= 1" @click="onMove(-1)"><ArrowUp :size="14"/>{{ $t('source.structure.moveUp') }}</MfButton>
          <MfButton v-if="can('column:reorder')" size="sm" :disabled="saving || !detailColumn || detailColumn.position >= columns.length" @click="onMove(1)"><ArrowDown :size="14"/>{{ $t('source.structure.moveDown') }}</MfButton>
          <MfButton v-if="can('column:reorder')" size="sm" :disabled="saving || !columns.length" @click="reorderVisible = true"><ArrowUpDown :size="14"/>{{ $t('source.structure.reorder') }}</MfButton>
          <MfButton v-if="executed" size="sm" @click="executedVisible = true"><CircleCheck v-if="executed.success" :size="14" class="text-success"/><CircleX v-else :size="14" class="text-danger"/>{{ $t('source.executed.view') }}</MfButton>
        </div>
        <MfTable fill :columns="fieldHeaders" :data-source="sortedColumns" row-key="name" density="compact" :bordered="false" :row-class="rowClass" @dragover="onRowDragOver" @drop="onRowDrop" @dragleave="sort.leave" :empty-text="$t('common.noData')" @row-click="selectDetail">
          <template #header-__check__><MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :aria-label="$t('source.prototype.selectAll')" @update:model-value="checked => toggleAll(!!checked)"/></template>
          <template #header-name><button type="button" class="inline-flex items-center gap-1" @click="toggleNameSort">{{ $t('source.structure.fieldName') }}<ArrowUpDown :size="10" :class="nameSort ? 'text-primary' : 'text-content-tertiary'"/></button></template>
          <template #cell-__check__="{ record }">
            <button type="button" :draggable="can('column:reorder') && !saving && !nameSort" class="flex size-4 cursor-grab items-center justify-center rounded text-content-secondary active:cursor-grabbing" :class="selectedNames.has(record.name) && 'bg-primary/15 text-primary'" :aria-label="$t('source.prototype.selectField', { name: record.name })" :aria-pressed="selectedNames.has(record.name)" @click.stop="toggleSelect(record.name, !selectedNames.has(record.name))" @dragstart="sort.start(record.name, $event)" @dragend="sort.end"><GripVertical :size="14"/></button>
          </template>
          <template #cell-name="{ record }"><div class="flex min-w-0 items-center gap-2"><EllipsisCell :text="record.name"/><KeyRound v-if="primaryNames.has(record.name)" :size="13" class="shrink-0 text-warning"/></div></template>
          <template #cell-type="{ record }"><MfTag size="sm" :variant="typeTone(record.type)" :color="typeColor(record.type)">{{ record.type }}</MfTag></template>
          <template #cell-length="{ record }">{{ record.length || '-' }}</template>
          <template #cell-nullable="{ record }"><span :class="record.nullable === 'NO' && 'text-danger'">{{ $t(record.nullable === 'NO' ? 'common.no' : 'common.yes') }}</span></template>
          <template #cell-defaultValue="{ record }"><EllipsisCell :text="displayDefault(record)" :class="record.default === null && record.autoincrement !== 'YES' && 'text-content-tertiary'"/></template>
          <template #cell-comment="{ record }"><EllipsisCell :text="record.comment || '-'"/></template>
          <template #cell-action="{ record }"><div class="flex items-center gap-4 text-xs" @click.stop><button type="button" v-if="can('column:modify')" class="text-primary hover:underline disabled:opacity-50" :disabled="saving" @click="openPanel('edit', record)">{{ $t('common.edit') }}</button><button type="button" v-if="can('column:drop')" class="text-danger hover:underline disabled:opacity-50" :disabled="saving" @click="confirmDrop([record.name])">{{ $t('common.delete') }}</button></div></template>
        </MfTable>
      </div>
      <section v-if="detailColumn" class="source-field-detail mt-3 flex min-h-0 shrink-0 flex-col overflow-hidden rounded-mf-md border border-line bg-surface/60 px-3 py-3">
        <div class="mb-1 flex shrink-0 items-center justify-between px-1.5">
          <div class="flex items-center gap-4"><Columns3 :size="23" class="text-content-secondary"/><MfTag size="sm" :variant="typeTone(detailColumn.type)" :color="typeColor(detailColumn.type)">{{ detailColumn.type }}</MfTag></div>
          <MfButton v-if="can('column:modify')" size="sm" :disabled="saving" @click="openPanel('edit', detailColumn)"><Pencil :size="13"/>{{ $t('common.edit') }}</MfButton>
        </div>
        <MfTabs v-model:active-key="detailTab" fill class="source-field-details">
          <MfTabPane v-for="tab in detailTabs" :key="tab.key" :item-key="tab.key" :tab="tab.label">
            <div v-if="detailTab === 'basic'" class="grid grid-cols-2 gap-6 rounded-mf-sm border border-line p-3.5 text-xs">
              <dl v-for="(items, group) in [detailBasics, detailAdditional]" :key="group" class="m-0 grid content-start grid-cols-[minmax(72px,1fr)_minmax(0,1.6fr)] gap-x-3 gap-y-2" :class="group && 'border-l border-line pl-6'">
                <template v-for="item in items" :key="item.label"><dt class="text-content-tertiary">{{ item.label }}</dt><dd class="m-0 min-w-0" :class="item.tone || 'text-content-secondary'"><EllipsisCell :text="item.value"/></dd></template>
              </dl>
            </div>
            <dl v-else-if="detailTab === 'constraints'" class="m-0 grid grid-cols-[140px_1fr] gap-3 rounded-mf-sm border border-line p-4 text-sm"><template v-for="item in detailConstraints" :key="item.label"><dt class="text-content-tertiary">{{ item.label }}</dt><dd class="m-0" :class="item.tone">{{ item.value }}</dd></template></dl>
            <template v-else-if="detailTab === 'indexes'">
              <MfEmpty v-if="!detailIndexes.length" size="sm" :title="$t('common.noData')"/>
              <div v-for="index in detailIndexes" :key="index.name" class="mb-2 flex items-center gap-4 rounded-mf-md border border-line px-4 py-3 text-sm"><KeyRound :size="14" class="text-content-tertiary"/><span>{{ index.name }}</span><MfTag size="sm">{{ $t(index.unique ? 'source.index.unique' : 'source.index.normal') }}</MfTag><span class="text-content-tertiary">{{ index.columns.join(', ') }}</span></div>
            </template>
            <template v-else-if="detailTab === 'foreignkeys'">
              <MfEmpty v-if="!detailForeignKeys.length" size="sm" :title="$t('common.noData')"/>
              <div v-for="(key, index) in detailForeignKeys" :key="index" class="mb-2 rounded-mf-md border border-line px-4 py-3 text-sm">{{ key.object_name }} · {{ key.object_column }} → {{ key.object_reference_table }}.{{ key.object_reference_column }}</div>
            </template>
            <MfEmpty v-else size="sm" :title="$t('source.detail.noUsage')"/>
          </MfTabPane>
        </MfTabs>
      </section>
    </template>
    <SourceColumnEditor v-if="panelVisible" v-model="panelForm" v-model:more="moreOptionsOpen" :open="panelVisible" :can-create-index="can('index:create')" :width="fieldPanelWidth" :mode="panelMode" :anchor="panelAnchor" :saving="saving" :index-only="indexOnly" :container="fieldPanelHost" :issues="panelIssues" :columns="columns" :supported-types="dataTypes" :types-loading="typesLoading" :type-options="typeOptions" :default-mode-options="defaultModeOptions" :charset-options="charsetOptions" :collate-options="collateOptions" :index-type-options="indexTypeOptions" :composite-column-options="compositeColumnOptions" @close="closePanel()" @save="onSubmitPanel"/>
    <ExecutedSqlDialog v-model:open="executedVisible" :executed="executed"/>
    <SourceColumnReorder v-model:open="reorderVisible" :columns="columns" :saving="saving" :type-tone="typeTone" :type-color="typeColor" @apply="onApplyOrder"/>
    <SourceColumnDrop :open="dropVisible" :names="dropNames" :dependencies="dropDependencies" :saving="saving" @close="dropVisible = false" @confirm="onDropFields"/>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, ArrowUp, ArrowUpDown, CircleCheck, CircleX, Columns3, GripVertical, KeyRound, ListPlus, Pencil, Plus, Trash2 } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import SourceColumnEditor from './components/SourceColumnEditor.vue'
import SourceColumnReorder from './components/SourceColumnReorder.vue'
import ExecutedSqlDialog from '@/views/components/sql/ExecutedSqlDialog.vue'
import SourceColumnDrop from './components/SourceColumnDrop.vue'
import { useSourceTableStructure } from '@/composables/useSourceTableStructure'
defineOptions({ name: 'SourceTableStructure' })
const { can, loading, columns, primaryNames, selectedNames, allSelected, partialSelected, toggleAll, toggleSelect,
  panelVisible, panelMode, panelAnchor, saving, moreOptionsOpen, dropVisible, dropChecking, dropNames, dropDependencies, panelForm, panelIssues, indexOnly, fieldPanelHost, fieldPanelWidth,
  reorderVisible, nameSort, sortedColumns, toggleNameSort, typeOptions, dataTypes, typesLoading, defaultModeOptions,
  charsetOptions, collateOptions, indexTypeOptions, compositeColumnOptions, detailColumn, detailTab, detailTabs,
  detailIndexes, detailForeignKeys, detailBasics, detailConstraints, detailAdditional, fieldHeaders, typeTone, typeColor,
  displayDefault, openPanel, closePanel, onInsertField, onSubmitPanel, confirmDrop, onRequestDrop, onDropFields,
  onMove, applyOrder, selectDetail, rowClass, sort, onRowDragOver, onRowDrop, executed, executedVisible } = useSourceTableStructure()
async function onApplyOrder(names: string[]) {
  if (await applyOrder(names)) reorderVisible.value = false
}
</script>

<style scoped>
.source-field-detail { flex-basis: min(42%, 300px); }
.source-structure :deep(td), .source-structure :deep(th) { padding-left: 10px; padding-right: 10px; }
.source-structure :deep(td) { font-size: 12px; line-height: 20px; }
.source-structure :deep(td .mf-tag) { line-height: 16px; }
.source-field-details :deep([role='tablist']) { gap: 8px; overflow-x: auto; }
.source-field-details :deep([role='tabpanel']) { padding-top: 0; }
</style>
