<template>
  <div class="compare-page flex h-full min-h-0 flex-col gap-2">
    <MfEmpty v-if="!source" class="flex-1" variant="initial" :title="$t('compare.chooseSource')">
      <MfSelect class="mt-3 w-72" :options="sourceOptions" :placeholder="$t('compare.chooseSource')" @update:model-value="value => openSource(String(value))"/>
    </MfEmpty>

    <template v-else>
      <header class="shrink-0 rounded-mf-lg bg-surface/95 px-6 pt-4">
        <h1 class="m-0 mb-1 text-xl font-semibold text-content">{{ sourceTable ? $t('compare.title', { table: sourceTable }) : $t('navigation.dataCompare') }}</h1>
        <MfTabs :active-key="mode" class="compare-tabs" @change="setMode">
          <MfTabPane item-key="structure" :tab="$t('compare.structure')"/>
          <MfTabPane item-key="data" :tab="$t('compare.data')"/>
        </MfTabs>
      </header>

      <!-- Structure -->
      <!-- Tables take the remaining height; on short screens the card itself scrolls rather than the page. -->
      <div v-if="mode === 'structure'" class="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto rounded-mf-lg bg-surface/95 p-5">
        <div class="grid shrink-0 grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto]">
          <div class="rounded-mf-md border border-line p-4">
            <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Database :size="18" class="text-info"/>{{ $t('compare.sourceTable') }}</h3>
            <div class="flex flex-wrap items-center gap-4">
              <MfSelect class="w-72" searchable :model-value="sourceTable" :options="sourceTables" :placeholder="$t('compare.chooseTable')" @update:model-value="value => setSourceTable(String(value))"/>
              <span class="flex items-center gap-1.5 text-sm text-content-secondary"><Database :size="15"/>{{ database }}（{{ $t('compare.current') }}）</span>
            </div>
          </div>
          <div class="rounded-mf-md border border-line p-4">
            <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Circle :size="18" class="text-info"/>{{ $t('compare.targetTable') }}</h3>
            <div class="flex flex-wrap items-center gap-3">
              <MfSelect v-model="targetTable" class="w-72" searchable :options="targetTables" :placeholder="$t('compare.chooseTable')"/>
              <MfSelect class="w-56" searchable :model-value="targetDatabase" :options="databases" @update:model-value="value => setTargetDatabase(String(value))">
                <template #prefix><Database :size="15"/></template>
              </MfSelect>
            </div>
          </div>
          <div class="flex flex-wrap items-start justify-between gap-3 lg:col-span-2 lg:flex-row-reverse xl:col-span-1 xl:flex-col xl:flex-nowrap xl:items-stretch">
            <div class="flex shrink-0 justify-end gap-2">
              <MfButton :disabled="!ready" @click="swap"><ArrowLeftRight :size="15"/>{{ $t('compare.swap') }}</MfButton>
              <MfButton :disabled="!ready" :loading="structureLoading" @click="compareStructure"><RefreshCw :size="15"/>{{ $t('compare.again') }}</MfButton>
            </div>
            <div class="rounded-mf-md bg-success/10 px-4 py-3">
              <p class="m-0 mb-2 text-sm font-semibold text-success">{{ $t('compare.result') }}</p>
              <div class="grid grid-cols-4 divide-x divide-line text-center">
                <div v-for="item in SUMMARY" :key="item.key" class="px-3">
                  <p class="m-0 text-lg font-semibold tabular-nums" :class="item.tone">{{ structureCounts[item.key] }}</p>
                  <p class="m-0 text-xs" :class="item.tone">{{ $t(`compare.status.${item.key}`) }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="structureLoading && !structureRows.length" class="flex flex-1 items-center justify-center py-16"><MfSpinner/></div>
        <MfAlert v-else-if="structureError" variant="danger" class="shrink-0" :title="structureError"/>
        <MfEmpty v-else-if="!ready" class="flex-1" :title="$t('compare.chooseBoth')"/>
        <!-- Each table scrolls on its own; the page keeps to the viewport. -->
        <div v-else class="grid min-h-72 flex-1 grid-cols-1 grid-rows-2 gap-3 lg:grid-cols-2 lg:grid-rows-1" :class="structureLoading && 'opacity-60'">
          <div v-for="side in (['source', 'target'] as const)" :key="side" class="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-mf-md border border-line">
            <MfTable fill :columns="structureColumns" :data-source="sides[side]" row-key="name" density="compact" :bordered="false"
                     :empty-text="$t(onlyDifferent ? 'compare.noDifference' : 'compare.noRows')" :row-class="(row: StructureSideRow) => rowTone(side, row.status)">
              <template #cell-name="{ record }">
                <span class="inline-flex min-w-0 items-center gap-2" :class="markTone(side, record.status)">
                  <component :is="markIcon(side, record.status)" v-if="markIcon(side, record.status)" :size="15" class="shrink-0"/>
                  <span class="truncate" :class="!markIcon(side, record.status) && 'pl-[23px]'">{{ record.name }}</span>
                </span>
              </template>
              <template #cell-type="{ record }"><span :class="cellTone(side, record, 'type')">{{ record.column.type }}</span></template>
              <template #cell-nullable="{ record }">
                <span :class="cellTone(side, record, 'nullable') === 'text-content' && !record.column.nullable ? 'text-danger' : cellTone(side, record, 'nullable')">{{ record.column.nullable ? 'YES' : 'NO' }}</span>
              </template>
              <template #cell-defaultValue="{ record }">
                <span :class="cellTone(side, record, 'defaultValue')"><EllipsisCell :text="record.column.defaultValue ?? (record.column.nullable ? 'NULL' : '-')"/></span>
              </template>
              <template #cell-comment="{ record }">
                <span :class="cellTone(side, record, 'comment')"><EllipsisCell :text="(record.column.comment || '-') + (onlyHere(side, record.status) ? `（${$t(`compare.only.${side}`)}）` : '')"/></span>
              </template>
            </MfTable>
          </div>
        </div>

        <footer class="flex shrink-0 items-center justify-between gap-3">
          <MfSelect class="w-48" :model-value="onlyDifferent ? 'different' : 'all'" :options="filterOptions" @update:model-value="value => (onlyDifferent = value === 'different')">
            <template #prefix><Filter :size="15"/></template>
          </MfSelect>
          <div class="flex shrink-0 gap-2">
            <MfButton @click="back">{{ $t('common.cancel') }}</MfButton>
            <ExportMenu :disabled="!ready" :loading="exporting" :label="$t('compare.exportStructure')" @export="format => exportResult('structure', format)"/>
          </div>
        </footer>
      </div>

      <!-- Data -->
      <!-- Settings and results are two cards with a draggable divider, like the table workspace. -->
      <div v-else class="flex min-h-0 flex-1 overflow-hidden">
        <aside :style="{ width: configWidth + 'px' }" class="flex h-full shrink-0 flex-col gap-4 overflow-y-auto rounded-mf-lg bg-surface/90 p-5">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('compare.config') }}</h3>
          <div class="grid grid-cols-[5rem_minmax(0,1fr)] items-center gap-x-3 gap-y-4 text-sm">
            <span class="text-content-secondary">{{ $t('compare.table') }}</span>
            <span class="min-w-0 truncate text-content">{{ sourceTable || '-' }} → {{ targetDatabase }}.{{ targetTable || '-' }}</span>
            <span class="text-content-secondary">{{ $t('compare.pairBy') }}</span>
            <MfRadioGroup v-model="dataForm.mode" class="flex flex-wrap items-center gap-4">
              <MfRadio value="primary">{{ $t('compare.byPrimary') }}</MfRadio>
              <MfRadio value="fields"><span class="inline-flex items-center gap-1">{{ $t('compare.byFields') }}
                <MfTooltip :content="$t('compare.byFieldsHint')"><CircleHelp :size="14" class="text-content-tertiary"/></MfTooltip></span></MfRadio>
            </MfRadioGroup>
            <span class="text-content-secondary">{{ $t('compare.keyFields') }}</span>
            <MfSelect v-if="dataForm.mode === 'primary'" :model-value="primaryLabel" disabled :options="[{ label: primaryLabel, value: primaryLabel }]"/>
            <MfSelect v-else v-model="dataForm.keys" multiple searchable :options="columnNames.map(name => ({ label: name, value: name }))" :placeholder="$t('compare.chooseFields')"/>
            <span class="text-content-secondary">{{ $t('compare.scope') }}</span>
            <div class="flex flex-wrap items-center gap-3">
              <MfRadioGroup v-model="dataForm.scope" class="flex items-center gap-4">
                <MfRadio value="all">{{ $t('source.export.scopeAll') }}</MfRadio>
                <MfRadio value="range">{{ $t('compare.range') }}</MfRadio>
              </MfRadioGroup>
              <div v-if="dataForm.scope === 'range'" class="flex items-center gap-2">
                <MfInputNumber v-model="dataForm.start" :min="1" :precision="0" :controls="false" class="w-20" :aria-label="$t('compare.rangeStart')"/>
                <span class="text-content-tertiary">-</span>
                <MfInputNumber v-model="dataForm.end" :min="1" :precision="0" :controls="false" class="w-24" :aria-label="$t('compare.rangeEnd')"/>
              </div>
            </div>
            <span class="self-start text-content-secondary">{{ $t('compare.kinds') }}</span>
            <div class="flex flex-col gap-2.5">
              <MfCheckbox v-model="dataForm.onlyChanged">{{ $t('compare.onlyChanged') }}</MfCheckbox>
              <MfCheckbox v-model="dataForm.includeMissing">{{ $t('compare.includeMissing') }}</MfCheckbox>
            </div>
          </div>
          <div class="mt-auto flex gap-3 pt-2">
            <MfButton :disabled="dataLoading" @click="resetData">{{ $t('compare.reset') }}</MfButton>
            <MfButton variant="primary" :loading="dataLoading" :disabled="!ready || (dataForm.mode === 'fields' && !dataForm.keys.length)" @click="compareData">{{ $t('compare.start') }}</MfButton>
          </div>
        </aside>
        <MfResizeHandle v-model="configWidth" :min="CONFIG_WIDTH.min" :max="CONFIG_WIDTH.max" :label="$t('compare.resize')" @change="saveConfigWidth"/>
        <div class="flex h-full min-w-0 flex-1 flex-col gap-3 overflow-hidden rounded-mf-lg bg-surface/95 p-5">
          <div class="flex shrink-0 flex-wrap items-center justify-between gap-3">
            <div class="flex items-baseline gap-3">
              <h3 class="m-0 text-base font-semibold text-content">{{ $t('compare.result') }}</h3>
              <span v-if="dataResult" class="text-xs text-content-tertiary">{{ $t('compare.dataSummary', { count: dataResult.total }) }}</span>
            </div>
            <div class="flex items-center gap-4 text-xs text-content-secondary">
              <span v-for="item in LEGEND" :key="item.key" class="inline-flex items-center gap-1.5"><span class="h-3 w-5 rounded-mf-sm" :class="item.tone"/>{{ $t(`compare.legend.${item.key}`) }}</span>
            </div>
          </div>
          <div v-if="dataLoading && !dataResult" class="flex flex-1 items-center justify-center"><MfSpinner/></div>
          <MfAlert v-else-if="dataError" variant="danger" class="shrink-0" :title="dataError"/>
          <MfEmpty v-else-if="!dataResult" class="flex-1" :title="$t(ready ? 'compare.startHint' : 'compare.chooseBoth')"/>
          <template v-else>
            <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-mf-md border border-line" :class="dataLoading && 'opacity-60'">
              <MfTable fill :columns="dataColumns" :data-source="dataRows" row-key="index" density="compact" :bordered="false" :empty-text="$t('compare.noDifference')"
                       :row-class="(row: DataTableRow) => TYPE_ROW[row.type]">
                <template #cell-index="{ record }"><span class="tabular-nums text-content-secondary">{{ record.index + 1 }}</span></template>
                <template #cell-type="{ record }">
                  <span class="inline-flex items-center gap-1.5" :class="TYPE_TEXT[record.type]"><component :is="TYPE_ICON[record.type]" :size="15"/>{{ $t(`compare.type.${record.type}`) }}</span>
                </template>
                <!-- A changed value shows both sides, the cell tinted edge to edge. -->
                <template v-for="(column, position) in dataResult.columns" :key="column" #[`cell-c${position}`]="{ record }">
                  <div v-if="record.changed.includes(column)" class="-mx-3 -my-1.5 bg-danger/10 px-3 py-1.5">
                    <p class="m-0 truncate text-content">- {{ cellText(record.source?.[position]) }}</p>
                    <p class="m-0 truncate text-danger">- {{ cellText(record.target?.[position]) }}</p>
                  </div>
                  <EllipsisCell v-else :text="cellText((record.source || record.target)?.[position])"/>
                </template>
                <template #cell-action="{ record }">
                  <button type="button" class="text-xs text-primary hover:underline" @click="detail = record">{{ $t('compare.detail') }}</button>
                </template>
              </MfTable>
            </div>
            <p v-if="dataResult.truncated" class="m-0 shrink-0 text-xs text-warning">{{ $t('compare.truncated', { shown: dataResult.rows.length, total: dataResult.total }) }}</p>
          </template>
          <footer class="mt-auto flex shrink-0 justify-end gap-2 pt-1">
            <MfButton @click="back">{{ $t('common.cancel') }}</MfButton>
            <ExportMenu :disabled="!ready || (dataForm.mode === 'fields' && !dataForm.keys.length)" :loading="exporting" :label="$t('compare.exportData')"
                        @export="format => exportResult('data', format)"/>
          </footer>
        </div>
      </div>
    </template>

    <MfModal :open="!!detail" width="720px" :title="$t('compare.detailTitle')" @update:open="value => !value && (detail = null)">
      <MfTable v-if="detail && dataResult" :columns="detailColumns" :data-source="detailRows" row-key="column" density="compact" :bordered="false"
               :row-class="(row: DetailRow) => row.changed ? 'bg-danger/10 [&>td]:bg-danger/10' : ''">
        <template #cell-column="{ record }"><span class="font-medium text-content">{{ record.column }}</span></template>
        <template #cell-left="{ record }"><EllipsisCell :text="record.left"/></template>
        <template #cell-right="{ record }"><span :class="record.changed && 'text-danger'"><EllipsisCell :text="record.right"/></span></template>
      </MfTable>
      <template #footer><MfButton variant="primary" @click="detail = null">{{ $t('source.executed.close') }}</MfButton></template>
    </MfModal>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeftRight, Circle, CircleHelp, CircleMinus, CirclePlus, Database, Filter, RefreshCw } from '@lucide/vue'
import SourceService from '@/services/source'
import { FilterModel } from '@/model/filter'
import { compareLocation } from '@/model/source-manage'
import { cellText, type DataRow, type DataType, type StructureSideRow, type StructureStatus } from '@/model/table-compare'
import { useTableCompare } from '@/composables/useTableCompare'
import ExportMenu from './components/CompareExportMenu.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { usePanelWidth } from '@/composables/usePanelWidth'

defineOptions({ name: 'CompareHome' })
const { t } = useI18n()
const router = useRouter()
const {
  source, database, mode, sourceTable, databases, sourceTables, targetTables, targetDatabase, targetTable, ready,
  structureLoading, structureRows, structureCounts, structureError, onlyDifferent, sides,
  dataForm, dataLoading, dataResult, dataError, columnNames, primaryKeys, detail, exporting,
  setMode, setSourceTable, setTargetDatabase, swap, back, compareStructure, compareData, resetData, exportResult,
} = useTableCompare()

// The settings card keeps the width the user dragged it to.
const CONFIG_WIDTH = { initial: 360, min: 300, max: 560 }
const { width: configWidth, save: saveConfigWidth } = usePanelWidth('MetaForgeCompareConfigWidth', CONFIG_WIDTH)
const structureColumns = computed(() => [
  { title: t('compare.column.name'), dataIndex: 'name', width: 180, ellipsis: true },
  { title: t('compare.column.type'), dataIndex: 'type' },
  { title: t('compare.column.nullable'), dataIndex: 'nullable' },
  { title: t('compare.column.defaultValue'), dataIndex: 'defaultValue', width: 160, ellipsis: true },
  { title: t('compare.column.comment'), dataIndex: 'comment', width: 200, ellipsis: true },
])
const SUMMARY: { key: StructureStatus; tone: string }[] = [
  { key: 'changed', tone: 'text-danger' }, { key: 'source_only', tone: 'text-success' }, { key: 'target_only', tone: 'text-warning' }, { key: 'same', tone: 'text-content' },
]
const LEGEND = [{ key: 'changed', tone: 'bg-danger/15' }, { key: 'source_only', tone: 'bg-success/15' }, { key: 'target_only', tone: 'bg-warning/15' }]
// Whole-row tints; written out in full so Tailwind generates them.
const TYPE_ROW: Record<DataType, string> = { same: '', changed: '', source_only: '[&>td]:bg-success/10', target_only: '[&>td]:bg-warning/10' }
const TYPE_TEXT: Record<DataType, string> = { same: 'text-content-secondary', changed: 'text-danger', source_only: 'text-success', target_only: 'text-content' }
const TYPE_ICON: Record<DataType, unknown> = { same: Circle, changed: CircleMinus, source_only: CirclePlus, target_only: CircleMinus }
// Result rows numbered for display; each compared column is a cell slot named by its position.
type DataTableRow = DataRow & { index: number }
const dataRows = computed<DataTableRow[]>(() => (dataResult.value?.rows || []).map((row, index) => ({ ...row, index })))
const dataColumns = computed(() => [
  { title: '#', dataIndex: 'index' },
  { title: t('compare.diffType'), dataIndex: 'type' },
  ...(dataResult.value?.columns || []).map((column, position) => ({ title: column, dataIndex: `c${position}`, width: 180, ellipsis: true })),
  { title: t('common.action'), dataIndex: 'action', fixed: 'right' as const },
])
// Row details: one line per column, long values truncated with the full text on hover.
type DetailRow = { column: string; left: string; right: string; changed: boolean }
const detailColumns = computed(() => [
  { title: t('compare.column.name'), dataIndex: 'column', width: 160, ellipsis: true },
  { title: t('compare.left'), dataIndex: 'left', width: 240, ellipsis: true },
  { title: t('compare.right'), dataIndex: 'right', width: 240, ellipsis: true },
])
const detailRows = computed<DetailRow[]>(() => {
  const row = detail.value
  if (!row || !dataResult.value) return []
  return dataResult.value.columns.map((column, position) => ({
    column,
    left: row.source ? cellText(row.source[position]) : '—',
    right: row.target ? cellText(row.target[position]) : '—',
    changed: row.changed.includes(column),
  }))
})
// The primary key is shown, not chosen: a single option holding the key columns.
const primaryLabel = computed(() => primaryKeys.value.join(', ') || t('compare.noPrimaryKey'))
const filterOptions = computed(() => [{ label: t('compare.showAll'), value: 'all' }, { label: t('compare.showDifferent'), value: 'different' }])

// Source columns only on the left are additions; on the right, changes and target-only columns are marked.
const onlyHere = (side: 'source' | 'target', status: StructureStatus) => (side === 'source' && status === 'source_only') || (side === 'target' && status === 'target_only')
const rowTone = (side: 'source' | 'target', status: StructureStatus) =>
  onlyHere(side, status) ? (side === 'source' ? '[&>td]:bg-success/10' : '[&>td]:bg-warning/10') : side === 'target' && status === 'changed' ? '[&>td]:bg-danger/10' : ''
const markTone = (side: 'source' | 'target', status: StructureStatus) =>
  onlyHere(side, status) ? (side === 'source' ? 'text-success' : 'text-warning') : side === 'target' && status === 'changed' ? 'text-danger' : 'text-content'
const markIcon = (side: 'source' | 'target', status: StructureStatus) =>
  side === 'source' && status === 'source_only' ? CirclePlus : (side === 'target' && (status === 'changed' || status === 'target_only')) ? CircleMinus : null
const cellTone = (side: 'source' | 'target', row: StructureSideRow, property: string) =>
  onlyHere(side, row.status) ? (side === 'source' ? 'text-success' : 'text-warning') : side === 'target' && row.changes.includes(property) ? 'text-danger' : 'text-content'

// Without a source in the address, pick one to start comparing in.
const sourceOptions = ref<{ label: string; value: string }[]>([])
if (!source.value) {
  SourceService.getAll(new FilterModel()).then(response => {
    if (response.status) sourceOptions.value = (response.data?.content || []).map((item: any) => ({ label: item.name, value: item.code }))
  })
}
const openSource = (code: string) => { if (code) router.replace(compareLocation({ source: code })) }
</script>

<style scoped>
.compare-tabs :deep([role='tablist']) { border-bottom: 0; }
.compare-page :deep(*) {
  scrollbar-width: thin;
  scrollbar-color: color-mix(in srgb, var(--mf-color-content-tertiary) 40%, transparent) transparent;
}
.compare-page :deep(*::-webkit-scrollbar) { width: 6px; height: 6px; }
.compare-page :deep(*::-webkit-scrollbar-track) { background: transparent; }
.compare-page :deep(*::-webkit-scrollbar-thumb) {
  background: color-mix(in srgb, var(--mf-color-content-tertiary) 40%, transparent);
  border-radius: var(--mf-radius-full);
}
</style>
