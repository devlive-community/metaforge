<template>
  <div class="flex h-full min-h-0 flex-col gap-3 overflow-hidden">
    <div class="flex shrink-0 flex-wrap items-center gap-2">
      <MfButton v-if="can('trigger:create')" size="sm" variant="primary" :disabled="saving" @click="openEditor('add')"><Plus :size="14"/>{{ $t('source.trg.create') }}</MfButton>
      <MfButton v-if="can('trigger:drop')" size="sm" :disabled="saving || !selected.size" @click="requestDrop(triggers.filter(trigger => selected.has(trigger.name)))"><Trash2 :size="14"/>{{ $t('source.trg.drop') }}</MfButton>
      <MfButton size="sm" :loading="loading" @click="reload"><RotateCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
      <MfButton v-if="executed" size="sm" @click="executedVisible = true">
        <CircleCheck v-if="executed.success" :size="14" class="text-success"/><CircleX v-else :size="14" class="text-danger"/>{{ $t('source.executed.view') }}
      </MfButton>
    </div>

    <div class="flex min-h-40 flex-1 flex-col overflow-hidden rounded-mf-md border border-line">
      <MfTable fill :columns="headers" :data-source="triggers" row-key="name" density="compact" :bordered="false" :loading="loading" :empty-text="$t('common.noData')"
               :row-class="rowClass" class="cursor-default" @row-click="onRowClick" @mousedown="onTableMouseDown">
        <template #header-__check__><MfCheckbox :model-value="allSelected" :indeterminate="partialSelected" :disabled="!triggers.length" :aria-label="$t('source.data.selectAll')" @update:model-value="value => toggleAll(!!value)"/></template>
        <template #cell-__check__="{ record, index }"><span class="inline-flex" @click.stop><MfCheckbox :model-value="selected.has(record.name)" :aria-label="$t('source.data.selectRow')" @update:model-value="value => onCheck(record, index, !!value)"/></span></template>
        <template #cell-__index__="{ index }"><span class="tabular-nums text-content-secondary">{{ index + 1 }}</span></template>
        <template #cell-timing="{ record }"><MfTag size="sm" :variant="record.timing === 'BEFORE' ? 'info' : 'default'" :color="record.timing === 'AFTER' ? 'var(--mf-color-data-complex)' : undefined">{{ record.timing }}</MfTag></template>
        <template #cell-event="{ record }"><MfTag size="sm" :variant="eventTone(record.event)">{{ record.event }}</MfTag></template>
        <template #cell-definer="{ record }"><EllipsisCell :text="record.definer"/></template>
        <template #cell-created="{ record }"><span class="tabular-nums">{{ record.created || '-' }}</span></template>
        <template #cell-status><MfTag size="sm" variant="success">{{ $t('source.fk.valid') }}</MfTag></template>
        <template #cell-__action__="{ record }">
          <div class="flex items-center gap-4 text-xs" @click.stop>
            <button type="button" class="text-content-secondary hover:text-content" @click="detailName = record.name">{{ $t('source.idx.view') }}</button>
            <button v-if="editable" type="button" class="text-primary hover:underline disabled:opacity-50" :disabled="saving" @click="openEditor('edit', record)">{{ $t('common.edit') }}</button>
            <button v-if="can('trigger:drop')" type="button" class="text-danger hover:underline disabled:opacity-50" :disabled="saving" @click="requestDrop([record])">{{ $t('common.delete') }}</button>
          </div>
        </template>
      </MfTable>
    </div>

    <!-- The header stays put; only the details below it scroll. -->
    <section v-if="detail" class="flex max-h-[55%] shrink-0 flex-col overflow-hidden rounded-mf-md border border-line">
      <header class="flex shrink-0 flex-wrap items-center justify-between gap-3 border-b border-line px-4 py-3">
        <div class="flex min-w-0 items-center gap-2">
          <h3 class="m-0 truncate text-lg font-semibold text-content">{{ detail.name }}</h3>
          <MfTag size="sm" variant="success">{{ $t('source.fk.valid') }}</MfTag>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <MfButton v-if="editable" size="sm" :disabled="saving" @click="openEditor('edit', detail)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
          <MfButton v-if="can('trigger:create')" size="sm" :disabled="saving" @click="openEditor('copy', detail)"><Copy :size="14"/>{{ $t('source.trg.copy') }}</MfButton>
          <MfButton v-if="can('trigger:drop')" size="sm" variant="danger" :disabled="saving" @click="requestDrop([detail])"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfButton>
        </div>
      </header>
      <div class="min-h-0 flex-1 overflow-y-auto">
        <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.9fr)]">
          <div class="border-line p-4 lg:border-r">
            <h4 class="m-0 mb-3 text-sm font-semibold text-content">{{ $t('source.idx.basic') }}</h4>
            <dl class="m-0 grid grid-cols-[auto_minmax(0,1fr)] gap-x-6 gap-y-2 text-sm">
              <template v-for="item in basics" :key="item.label">
                <dt class="text-content-secondary">{{ item.label }}</dt><dd class="m-0 break-words text-content">{{ item.value }}</dd>
              </template>
            </dl>
          </div>
          <div class="flex min-w-0 flex-col gap-4 p-4">
            <div>
              <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.trg.definition') }}</h4>
              <SqlCodeBlock :code="detailSql || detail.body"/>
            </div>
            <div class="border-t border-line pt-4">
              <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('source.trg.actions') }}</h4>
              <div class="flex flex-wrap gap-2">
                <MfButton size="sm" :disabled="!detailSql" @click="createSqlVisible = true"><FileCode :size="14"/>{{ $t('source.trg.viewCreate') }}</MfButton>
                <MfButton size="sm" :disabled="!detailSql" @click="copySql"><Copy :size="14"/>{{ $t('source.trg.copySql') }}</MfButton>
                <MfButton v-if="editable" size="sm" :loading="saving" @click="rebuild(detail)"><RefreshCw :size="14"/>{{ $t('source.trg.rebuild') }}</MfButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <SourceTriggerEditor v-if="editorVisible" v-model="form" :open="editorVisible" :mode="editorMode" :table="table" :issues="issues" :preview-sql="previewSql"
                         :preview-error="previewError" :saving="saving" :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @save="submitEditor"/>
    <SourceTriggerDrop :open="!!dropTargets.length" :triggers="dropTargets" :saving="saving" @close="closeDrop" @confirm="confirmDrop"/>
    <MfModal v-model:open="createSqlVisible" :title="$t('source.trg.createSqlTitle')" width="680px">
      <SqlCodeBlock :code="detailSql" max-height="max-h-[60vh]"/>
      <template #footer><MfButton variant="primary" @click="createSqlVisible = false">{{ $t('source.executed.close') }}</MfButton></template>
    </MfModal>
    <ExecutedSqlDialog v-model:open="executedVisible" :executed="executed"/>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleCheck, CircleX, Copy, FileCode, Pencil, Plus, RefreshCw, RotateCw, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ExecutedSqlDialog from '@/views/components/sql/ExecutedSqlDialog.vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import SourceTriggerEditor from './components/SourceTriggerEditor.vue'
import SourceTriggerDrop from './components/SourceTriggerDrop.vue'
import type { SourceTrigger, TriggerEvent } from '@/model/source-trigger'
import { useSourceManageRoute } from '@/composables/useSourceManageRoute'
import { useSourceTableTriggers } from '@/composables/useSourceTableTriggers'

defineOptions({ name: 'SourceTableTriggers' })
const { t } = useI18n()
const route = useSourceManageRoute()
const {
  can, editable, loading, saving, triggers, selected, allSelected, partialSelected, toggleAll, onRowClick, onCheck, detail, detailName, detailSql,
  createSqlVisible, editorVisible, editorMode, form, issues, previewSql, previewError, fieldPanelHost, fieldPanelWidth,
  openEditor, closeEditor, submitEditor, rebuild, dropTargets, requestDrop, closeDrop, confirmDrop, reload, executed, executedVisible,
} = useSourceTableTriggers()
const table = computed(() => String(route.params.table || ''))

const headers = computed(() => [
  { title: '', dataIndex: '__check__', width: 44 },
  { title: '#', dataIndex: '__index__', width: 48 },
  { title: t('source.trg.name'), dataIndex: 'name' },
  { title: t('source.trg.timing'), dataIndex: 'timing' },
  { title: t('source.trg.event'), dataIndex: 'event' },
  { title: t('source.trg.definer'), dataIndex: 'definer' },
  { title: t('source.trg.created'), dataIndex: 'created' },
  { title: t('source.fk.status'), dataIndex: 'status' },
  { title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const },
])
const eventTone = (event: TriggerEvent) => event === 'INSERT' ? 'success' : event === 'UPDATE' ? 'warning' : 'danger'
// Checked rows take the hover background; the open detail row keeps a light primary tint.
const rowClass = (trigger: SourceTrigger) => selected.value.has(trigger.name) ? 'bg-surface-hover [&>td]:bg-surface-hover'
    : trigger.name === detailName.value ? 'bg-primary/5 [&>td]:bg-primary/5' : ''
const onTableMouseDown = (event: MouseEvent) => { if (event.shiftKey) event.preventDefault() }
const basics = computed(() => {
  const trigger = detail.value
  if (!trigger) return []
  return [
    { label: t('source.trg.name'), value: trigger.name },
    { label: t('source.trg.table'), value: table.value },
    { label: t('source.trg.timing'), value: trigger.timing },
    { label: t('source.trg.event'), value: trigger.event },
    { label: t('source.trg.definer'), value: trigger.definer || '-' },
    { label: t('source.trg.created'), value: trigger.created || '-' },
    { label: t('source.trg.sqlMode'), value: trigger.sqlMode || '-' },
    { label: t('source.trg.charset'), value: trigger.charset || '-' },
  ]
})
function copySql() {
  ObjectUtils.copy(detailSql.value)
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
