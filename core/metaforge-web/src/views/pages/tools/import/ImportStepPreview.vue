<template>
  <div class="flex flex-col gap-4">
    <div v-if="previewLoading && !preview" class="flex items-center justify-center py-16"><MfSpinner/></div>
    <MfAlert v-else-if="previewError" variant="danger" :title="previewError"/>
    <template v-else-if="preview">
      <!-- The running job replaces the plan once started. -->
      <section v-if="job" class="flex flex-col gap-3 rounded-mf-lg border border-line p-5">
        <div class="flex items-center justify-between gap-3">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.preview.progress') }}</h3>
          <MfTag size="sm" :variant="job.status === 'done' ? 'success' : job.status === 'failed' ? 'danger' : 'info'">{{ $t(`import.preview.jobStatus.${job.status}`) }}</MfTag>
        </div>
        <ul class="m-0 flex list-none flex-col gap-3 p-0">
          <li v-for="table in job.tables || []" :key="table.source" class="flex flex-col gap-1.5 rounded-mf-md border border-line px-4 py-3 text-sm">
            <div class="flex items-center justify-between gap-3">
              <span class="min-w-0 truncate"><span class="font-semibold text-content">{{ table.source }}</span><span class="text-content-tertiary"> → {{ table.target }}</span></span>
              <MfTag size="sm" :variant="TABLE_TONE[table.status] || 'default'">{{ $t(`import.preview.tableStatus.${table.status}`) }}</MfTag>
            </div>
            <div class="h-1.5 overflow-hidden rounded-full bg-surface-hover">
              <div class="h-full rounded-full bg-primary transition-[width]" :style="{ width: progressOf(table) + '%' }"/>
            </div>
            <span class="text-xs tabular-nums text-content-tertiary">{{ $t('import.preview.written', { written: Number(table.written || 0).toLocaleString(), rows: Number(table.rows || 0).toLocaleString(), skipped: Number(table.skipped || 0).toLocaleString() }) }}</span>
            <span v-if="table.message" class="break-words text-xs text-danger">{{ table.message }}</span>
          </li>
        </ul>
      </section>

      <div class="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <div v-for="tile in tiles" :key="tile.key" class="rounded-mf-lg border border-line px-5 py-4">
          <p class="m-0 text-sm text-content-secondary">{{ $t(`import.preview.tile.${tile.key}`) }}</p>
          <p class="m-0 mt-1 text-2xl font-semibold tabular-nums" :class="tile.tone">{{ tile.value.toLocaleString() }}</p>
        </div>
      </div>

      <section class="flex flex-col gap-3 rounded-mf-lg border border-line p-5">
        <div class="flex items-center justify-between gap-3">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.preview.plan') }}</h3>
          <span class="text-xs text-content-tertiary">{{ $t('import.preview.order') }}</span>
        </div>
        <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
          <MfTable :columns="planColumns" :data-source="preview.tables" row-key="source" density="compact" :bordered="false">
            <template #cell-action="{ record }">{{ actionLabel(record) }}</template>
            <template #cell-rows="{ record }"><span class="tabular-nums">{{ Number(record.rows).toLocaleString() }}</span></template>
            <template #cell-status="{ record }">
              <MfTooltip :content="record.messages.map((message: any) => messageText(message)).join('；') || $t('import.preview.status.pass')">
                <MfTag size="sm" :variant="STATUS_TONE[record.status]">{{ $t(`import.preview.status.${record.status}`) }}</MfTag>
              </MfTooltip>
            </template>
          </MfTable>
        </div>
      </section>

      <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <section class="flex min-w-0 flex-col gap-3 rounded-mf-lg border border-line p-5">
          <div class="flex items-center justify-between gap-3">
            <h3 class="m-0 truncate text-base font-semibold text-content">{{ $t('import.preview.sample', { table: preview.sample?.table || '-', count: preview.sample?.rows.length || 0 }) }}</h3>
            <MfTag size="sm" variant="info">{{ $t('import.preview.sampleOnly') }}</MfTag>
          </div>
          <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
            <MfTable :columns="sampleColumns" :data-source="sampleRows" row-key="__index" density="compact" :bordered="false" :empty-text="$t('common.noData')">
              <template v-for="(column, position) in preview.sample?.columns || []" :key="column" #[`cell-c${position}`]="{ record }">
                <EllipsisCell :text="record.values[position]"/>
              </template>
            </MfTable>
          </div>
        </section>
        <section class="flex min-w-0 flex-col gap-3 rounded-mf-lg border border-line p-5">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.preview.sql') }}</h3>
          <SqlCodeBlock :code="preview.sql" max-height="max-h-64"/>
        </section>
      </div>

      <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <MfAlert v-if="creates" variant="warning" :title="$t('import.preview.createWarning', { count: creates })"/>
        <MfAlert v-if="preview.errors === 0" variant="success" :title="$t('import.preview.ready', { database: context.database })"/>
        <MfAlert v-else variant="danger" :title="$t('import.preview.blocked', { count: preview.errors })"/>
      </div>
      <MfCheckbox v-if="!job" v-model="confirmed" :disabled="preview.errors > 0">
        <span class="font-semibold text-content">{{ $t('import.preview.confirm') }}</span><span class="ml-2 text-xs text-content-tertiary">{{ $t('import.preview.confirmHint') }}</span>
      </MfCheckbox>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import type { ImportWizard } from '@/composables/useImportWizard'

const props = defineProps<{ wizard: ImportWizard }>()
const { context, preview, previewLoading, previewError, confirmed, job } = props.wizard
const { t } = useI18n()
const STATUS_TONE: Record<string, 'success' | 'warning' | 'danger'> = { pass: 'success', warn: 'warning', error: 'danger' }
const TABLE_TONE: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'default'> = { done: 'success', failed: 'danger', running: 'info', pending: 'default', cancelled: 'warning' }
const tiles = computed(() => [
  { key: 'tables', value: preview.value?.tables.length || 0, tone: 'text-content' },
  { key: 'rows', value: Number(preview.value?.rows || 0), tone: 'text-content' },
  { key: 'errors', value: Number(preview.value?.errors || 0), tone: preview.value?.errors ? 'text-danger' : 'text-success' },
  { key: 'warnings', value: Number(preview.value?.warnings || 0), tone: preview.value?.warnings ? 'text-warning' : 'text-content' },
])
const planColumns = computed(() => [
  { title: t('import.tables.source'), dataIndex: 'source', width: 200, ellipsis: true },
  { title: t('import.tables.target'), dataIndex: 'target', width: 240, ellipsis: true },
  { title: t('import.preview.action'), dataIndex: 'action' },
  { title: t('import.preview.rows'), dataIndex: 'rows' },
  { title: t('import.preview.check'), dataIndex: 'status' },
])
const sampleColumns = computed(() => (preview.value?.sample?.columns || []).map((column: string, position: number) => ({ title: column, dataIndex: `c${ position }`, width: 160, ellipsis: true })))
const sampleRows = computed(() => (preview.value?.sample?.rows || []).map((values: string[], index: number) => ({ __index: index, values })))
const creates = computed(() => (preview.value?.tables || []).filter((table: any) => table.action === 'create').length)
const actionLabel = (row: any) => row.action === 'create' ? t('import.preview.createAndImport') : t(`import.settings.writeModeOption.${row.writeMode}`)
const messageText = (message: any) => t(`import.preview.message.${message.code}`, { subject: message.subject })
const progressOf = (table: any) => table.status === 'done' ? 100 : Number(table.rows) ? Math.min(100, Math.round(Number(table.written || 0) / Number(table.rows) * 100)) : 0
</script>
