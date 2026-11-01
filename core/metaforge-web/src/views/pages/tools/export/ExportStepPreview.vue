<template>
  <div v-if="checkLoading && !check" class="flex items-center justify-center py-16"><MfSpinner/></div>
  <MfAlert v-else-if="checkError" variant="danger" :title="checkError"/>
  <div v-else-if="check" class="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
    <div class="flex min-w-0 flex-col gap-4">
      <div class="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <div v-for="tile in tiles" :key="tile.key" class="rounded-mf-lg border border-line px-5 py-4">
          <p class="m-0 text-sm text-content-secondary">{{ $t(`export.preview.tile.${tile.key}`) }}</p>
          <p class="m-0 mt-1 truncate text-2xl font-semibold tabular-nums" :class="tile.tone">{{ tile.value }}</p>
        </div>
      </div>

      <section class="flex flex-col gap-3 rounded-mf-lg border border-line p-5">
        <div class="flex items-center justify-between gap-3">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.preview.list') }}</h3>
          <MfTag v-if="job" size="sm" :variant="JOB_TONE[job.status] || 'info'">{{ $t(`export.preview.jobStatus.${job.status}`) }}</MfTag>
        </div>
        <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
          <MfTable :columns="listColumns" :data-source="listRows" row-key="name" density="compact" :bordered="false">
            <template #cell-index="{ record }"><span class="tabular-nums text-content-tertiary">{{ record.index }}</span></template>
            <template #cell-name="{ record }"><EllipsisCell :text="record.name"/></template>
            <template #cell-structure="{ record }">{{ $t(record.structure ? 'export.preview.yes' : 'export.preview.no') }}</template>
            <template #cell-data="{ record }">{{ $t(record.data ? 'export.preview.yes' : 'export.preview.no') }}</template>
            <template #cell-rule="{ record }">{{ $t(`export.preview.rule.${record.rule}`) }}</template>
            <template #cell-file="{ record }"><EllipsisCell :text="record.file"/></template>
            <template #cell-status="{ record }">
              <div v-if="record.progress" class="flex min-w-0 flex-col gap-1">
                <MfTag size="sm" :variant="TABLE_TONE[record.progress.status] || 'default'">{{ $t(`export.preview.tableStatus.${record.progress.status}`) }}</MfTag>
                <span class="text-xs tabular-nums text-content-tertiary">{{ $t('export.preview.written', { count: Number(record.progress.written || 0).toLocaleString() }) }}</span>
                <span v-if="record.progress.message" class="break-words text-xs text-danger">{{ record.progress.message }}</span>
              </div>
              <MfTooltip v-else :content="record.messages.join('；') || $t('export.preview.status.pass')">
                <MfTag size="sm" :variant="record.status === 'pass' ? 'success' : 'danger'">{{ $t(`export.preview.status.${record.status}`) }}</MfTag>
              </MfTooltip>
            </template>
          </MfTable>
        </div>
        <MfAlert v-if="job?.status === 'failed'" variant="danger" :title="job.message || $t('export.failed')"/>
      </section>

      <section class="flex min-w-0 flex-col gap-3 rounded-mf-lg border border-line p-5">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h3 class="m-0 truncate text-base font-semibold text-content">{{ $t('export.preview.sample', { table: tables[0] || '-', count: rows?.rows.length || 0 }) }}</h3>
          <MfButton size="sm" :disabled="!tables.length" @click="openFull"><Eye :size="14"/>{{ $t('export.preview.full') }}</MfButton>
        </div>
        <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
          <MfTable :columns="sampleColumns" :data-source="sampleRows" row-key="__index" density="compact" :bordered="false" :empty-text="$t('common.noData')">
            <template v-for="(header, position) in rows?.headers || []" :key="header" #[`cell-c${position}`]="{ record }">
              <EllipsisCell :text="record.values[position] === null || record.values[position] === undefined ? 'NULL' : String(record.values[position])"/>
            </template>
          </MfTable>
        </div>
        <MfAlert variant="info" :title="$t('export.preview.sampleNote')"/>
      </section>
    </div>

    <aside class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.preview.confirm') }}</h3>
      <dl class="m-0 flex flex-col divide-y divide-line text-sm">
        <div v-for="item in summary" :key="item.key" class="flex items-center justify-between gap-3 py-2.5">
          <dt class="shrink-0 text-content-tertiary">{{ $t(`export.preview.summary.${item.key}`) }}</dt>
          <dd class="m-0 min-w-0 truncate font-semibold text-content" :title="item.value">{{ item.value }}</dd>
        </div>
      </dl>
      <MfAlert v-if="check.errors === 0" variant="success" :title="$t('export.preview.ready', { file: check.output })"/>
      <MfAlert v-else variant="danger" :title="$t('export.preview.blocked', { count: check.errors })"/>
      <MfButton v-if="job?.file" variant="primary" @click="download"><Download :size="15"/>{{ $t('export.preview.download', { file: job.file }) }}</MfButton>
    </aside>

    <MfModal v-model:open="fullOpen" :title="$t('export.preview.fullTitle', { file: sample?.file || '-' })" width="48rem">
      <div v-if="sampleLoading" class="flex items-center justify-center py-12"><MfSpinner/></div>
      <MfAlert v-else-if="sampleError" variant="danger" :title="sampleError"/>
      <pre v-else class="m-0 max-h-[60vh] overflow-auto rounded-mf-md bg-[#17262a] p-4 font-mono text-xs leading-5 text-[#d6e7e2]">{{ sample?.content || '' }}</pre>
    </MfModal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Download, Eye } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import type { ExportWizard } from '@/composables/useExportWizard'

const props = defineProps<{ wizard: ExportWizard }>()
const { context, state, objects, format, tables, check, checkLoading, checkError, rows, sample, sampleLoading, sampleError, job, runSample, download } = props.wizard
const { t } = useI18n()
const fullOpen = ref(false)
const JOB_TONE: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { running: 'info', done: 'success', partial: 'warning', failed: 'danger' }
const TABLE_TONE: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'default'> = { waiting: 'default', running: 'info', done: 'success', failed: 'danger', cancelled: 'warning' }

// Row counts come from the source's statistics and are only an estimate; filtered tables may write fewer.
const estimated = computed(() => tables.value.reduce((sum, name) => sum + (objects.value.find(item => item.name === name)?.rows || 0), 0))
const tiles = computed(() => [
  { key: 'tables', value: tables.value.length.toLocaleString(), tone: 'text-content' },
  { key: 'rows', value: estimated.value.toLocaleString(), tone: 'text-content' },
  { key: 'files', value: (check.value?.files || []).length.toLocaleString(), tone: 'text-content' },
  { key: 'errors', value: Number(check.value?.errors || 0).toLocaleString(), tone: check.value?.errors ? 'text-danger' : 'text-success' },
])
const listColumns = computed(() => [
  { title: '#', dataIndex: 'index', width: 56 },
  { title: t('export.preview.column.object'), dataIndex: 'name', width: 200, ellipsis: true },
  { title: t('export.preview.column.structure'), dataIndex: 'structure', width: 90 },
  { title: t('export.preview.column.data'), dataIndex: 'data', width: 90 },
  { title: t('export.preview.column.rule'), dataIndex: 'rule', width: 120 },
  { title: t('export.preview.column.file'), dataIndex: 'file', width: 200, ellipsis: true },
  { title: t('export.preview.column.status'), dataIndex: 'status' },
])
const listRows = computed(() => (check.value?.tables || []).map((row: any, index: number) => ({
  ...row, index: index + 1, progress: (job.value?.tables || []).find((item: any) => item.name === row.name) || null,
})))
const sampleColumns = computed(() => (rows.value?.headers || []).map((header: string, position: number) => ({ title: header, dataIndex: `c${ position }`, width: 160, ellipsis: true })))
const sampleRows = computed(() => (rows.value?.rows || []).map((values: any[], index: number) => ({ __index: index, values })))
const summary = computed(() => [
  { key: 'database', value: context.value.database || '-' },
  { key: 'objects', value: t('export.preview.objectCount', { count: tables.value.length }) },
  { key: 'content', value: t(state.value.content === 'all' ? 'export.content.all' : 'export.content.data') },
  { key: 'format', value: format.value ? `${ format.value.name } (.${ format.value.extension })` : '-' },
  { key: 'output', value: String(check.value?.output || '-') },
  { key: 'compress', value: t(state.value.compress ? 'export.preview.yes' : 'export.preview.no') },
  { key: 'concurrency', value: t(state.value.concurrency === 1 ? 'export.output.serial' : 'export.output.parallel', { count: state.value.concurrency }) },
  { key: 'failure', value: t(`export.output.failureOption.${ state.value.failure }`) },
])
function openFull() {
  fullOpen.value = true
  runSample()
}
</script>
