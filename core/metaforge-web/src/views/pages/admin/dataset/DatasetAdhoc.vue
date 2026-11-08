<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChartColumn, ChevronLeft, ChevronRight, CircleCheck, CircleAlert, Download, Eraser, Filter, Hash, LayoutGrid, Play, Plus, Save, Search, Settings2, Type, X } from '@lucide/vue'
import { MfForm } from 'metaforge-ui'
import { useDatasetAdhoc } from '@/composables/useDatasetAdhoc'
import { useStudioPanels } from '@/composables/useStudioPanels'
import { adhocColumnIssue, adhocCsv, fieldLabel, isAdhocNumeric, type AdhocColumn, type AdhocField, type AdhocMode } from '@/model/dataset-adhoc'
import { createQueryChartSnapshot } from '@/model/query-chart'
import StudioBackLink from '@/views/components/studio/StudioBackLink.vue'
import StudioPanelHeader from '@/views/components/studio/StudioPanelHeader.vue'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import QueryChartPreview from '@/views/components/visual/query/QueryChartPreview.vue'
import VisualChart from '@/views/components/visual/components/VisualChart.vue'
import VisualConfigure from '@/views/components/visual/components/VisualConfigure.vue'
import { createdConfigure } from '@/views/components/visual/Utils'
import DatasetAdhocFieldSettings from './components/adhoc/DatasetAdhocFieldSettings.vue'
import DatasetAdhocChartSettings from './components/adhoc/DatasetAdhocChartSettings.vue'
import DatasetReport from './components/DatasetReport.vue'

const { t } = useI18n(), route = useRoute()
const code = computed(() => String(route.params.code || '')), reportCode = computed(() => String(route.params.id || ''))
const { fields, columns, dataset, report, limit, initializing, loadError, warning, running, queried, failure, sql, elapsed,
  headers, rows, options, legacy, query, stale, selected, selectedColumn, resultTab, canPublish, load, execute, add, remove, update, clear } = useDatasetAdhoc(code, reportCode, t)
const { wide, compact, resizing, left, right, max, leftWidth, rightWidth, leftVisible, rightVisible, show, close, toggle } = useStudioPanels('dataset-adhoc')
const keyword = ref(''), fieldTab = ref('all'), inspectorTab = ref('query'), addingTo = ref<AdhocMode>(), dragging = ref(false)
const publishVisible = ref(false), savedCode = ref('')
const legacySettings = ref(false)
const legacyPreview = computed(() => legacy.value ? { ...legacy.value, headers: headers.value, columns: rows.value } as unknown as import('@/views/components/visual/Configuration').Configuration : null)
const legacyGroups = computed(() => legacy.value ? createdConfigure(legacy.value.type, { t } as any) : [])
const settings = ref<InstanceType<typeof DatasetAdhocFieldSettings>>(), queryForm = ref<InstanceType<typeof MfForm>>()
const querySettingsGroup = ref<InstanceType<typeof InspectorSection>>()
const fieldSettingsGroup = ref<InstanceType<typeof InspectorSection>>()
const limitModel = computed(() => ({ limit: limit.value }))
const zones = [
  { mode: 'METRIC' as const, label: 'dataset.adhoc.measures', hint: 'dataset.adhoc.dropMetric', icon: ChartColumn },
  { mode: 'DIMENSION' as const, label: 'dataset.adhoc.dimensions', hint: 'dataset.adhoc.dropDimension', icon: LayoutGrid },
  { mode: 'FILTER' as const, label: 'dataset.common.columnModeFilter', hint: 'dataset.adhoc.dropFilter', icon: Filter },
]
const catalog = computed(() => fields.value.filter(field => (fieldTab.value === 'all' || field.mode === fieldTab.value)
  && (fieldLabel(field) + ' ' + field.name).toLowerCase().includes(keyword.value.trim().toLowerCase())))
const forMode = (mode: AdhocMode) => columns.value.filter(column => column.mode === mode)
function choose(field: AdhocField, mode = addingTo.value || field.mode) { add(field, mode); addingTo.value = undefined; inspectorTab.value = 'query'; show('right') }
function select(column: AdhocColumn) { selected.value = column.key; inspectorTab.value = 'query'; show('right') }
function startDrag(event: DragEvent, field: AdhocField, key?: string) {
  event.dataTransfer?.setData('application/metaforge-field', JSON.stringify({ id: field.id, key }))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copyMove'
  dragging.value = true
}
function dragOver(event: DragEvent) {
  if (!event.dataTransfer?.types.includes('application/metaforge-field')) return
  event.preventDefault()
  event.dataTransfer.dropEffect = 'copy'
}
function drop(event: DragEvent, mode: AdhocMode) {
  dragging.value = false
  try {
    const data = JSON.parse(event.dataTransfer?.getData('application/metaforge-field') || '{}')
    const field = fields.value.find(value => value.id === data.id)
    if (!field || (mode !== 'FILTER' && field.mode !== mode)) return
    if (data.key && data.key.startsWith(mode + ':')) {
      const column = columns.value.find(value => value.key === data.key)
      if (column) columns.value = [...columns.value.filter(value => value.key !== data.key), column]
    } else choose(field, mode)
  } catch { /* Ignore drags from outside the field library. */ }
}
async function run() {
  if (running.value) return
  const invalid = columns.value.find(column => adhocColumnIssue(column))
  if (invalid || !Number.isInteger(limit.value) || limit.value < 1) {
    if (invalid) selected.value = invalid.key
    inspectorTab.value = 'query'; show('right'); await nextTick()
    querySettingsGroup.value?.expand(); fieldSettingsGroup.value?.expand()
    await Promise.allSettled([settings.value?.validate(), queryForm.value?.validate()]); return
  }
  await execute()
}
function onKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key === 'Enter' && !publishVisible.value) { event.preventDefault(); void run() }
  if (event.key === 'Escape' && compact.value) close(compact.value)
}
function exportResults() {
  const url = URL.createObjectURL(new Blob([adhocCsv(headers.value, rows.value)], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a'); link.href = url; link.download = `metaforge-${code.value}.csv`; link.click(); URL.revokeObjectURL(url)
}
function configureChart() { resultTab.value = 'chart'; inspectorTab.value = 'chart'; show('right') }
watch([code, reportCode], () => { savedCode.value = ''; publishVisible.value = false })
</script>

<template>
  <div class="flex h-full min-h-0 min-w-0 flex-1 flex-col overflow-hidden" @keydown="onKeydown">
    <header class="relative z-20 flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-line bg-surface px-3 py-2 shadow-mf-sm sm:px-4">
      <div class="flex min-w-0 items-center gap-2.5">
        <StudioBackLink to="/dataset" :label="$t('dataset.editor.backToList')"/>
        <div class="min-w-0"><h1 class="truncate text-sm font-semibold sm:text-base" :title="report.name || dataset.name">{{ report.name || dataset.name || $t('dataset.adhoc.title') }}</h1></div>
        <MfTag v-if="stale" size="sm">{{ $t('dataset.adhoc.pending') }}</MfTag>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-2">
        <MfButton size="sm" :disabled="initializing || !columns.length || running" @click="clear"><Eraser :size="14"/>{{ $t('common.clear') }}</MfButton>
        <MfButton size="sm" :disabled="!canPublish" @click="publishVisible = true"><Save :size="14"/>{{ $t('dataset.adhoc.saveReport') }}</MfButton>
        <MfButton size="sm" variant="primary" :loading="running" :disabled="initializing || !!loadError || !columns.some(column => column.mode !== 'FILTER') || running" @click="run"><Play :size="14"/>{{ $t('dataset.adhoc.query') }}</MfButton>
      </div>
    </header>
    <div v-if="initializing" class="flex min-h-0 flex-1 items-center justify-center"><MfSpinner size="lg"/></div>
    <MfEmpty v-else-if="loadError" class="flex-1" :title="$t('dataset.adhoc.loadFailed')" :description="loadError"><MfButton @click="load">{{ $t('common.refresh') }}</MfButton></MfEmpty>
    <div v-else class="adhoc-workspace relative flex min-h-0 min-w-0 flex-1 overflow-hidden" :class="resizing && 'adhoc-resizing'" :style="{ '--adhoc-left': leftWidth + 'px', '--adhoc-right': rightWidth + 'px' }">
      <button v-if="!wide && compact" class="absolute inset-0 z-20 bg-overlay" :aria-label="$t('dashboard.studio.closePanel')" @click="close(compact)"/>
      <Transition name="adhoc-panel">
        <aside v-show="leftVisible" id="adhoc-field-library" :inert="!leftVisible" class="adhoc-side adhoc-left inset-y-0 left-0 z-30 border-r border-line bg-surface" :class="wide ? 'adhoc-docked relative' : 'absolute shadow-mf-lg'">
          <div class="adhoc-panel-content flex h-full min-h-0 flex-col">
            <StudioPanelHeader :title="$t('dataset.adhoc.fieldLibrary')" :pinnable="false" :pinned="true" @close="close('left')"/>
            <div class="shrink-0 p-3"><MfInput v-model="keyword" size="sm" :placeholder="$t('dataset.search.fields')" clearable><template #prefix><Search :size="14"/></template></MfInput></div>
            <MfTabs v-model:active-key="fieldTab" class="shrink-0 px-3"><MfTabPane item-key="all" :tab="$t('dataset.adhoc.all')"/><MfTabPane item-key="DIMENSION" :tab="$t('dataset.adhoc.dimensions')"/><MfTabPane item-key="METRIC" :tab="$t('dataset.adhoc.measures')"/></MfTabs>
            <div v-if="addingTo" class="flex shrink-0 items-center justify-between gap-2 bg-primary/5 px-3 py-2 text-xs text-primary"><span>{{ $t('dataset.adhoc.chooseField', { mode: $t(zones.find(zone => zone.mode === addingTo)!.label) }) }}</span><MfButton variant="ghost" size="sm" :aria-label="$t('common.cancel')" @click="addingTo = undefined"><X :size="13"/></MfButton></div>
            <div class="min-h-0 flex-1 overflow-y-auto px-3 py-2">
              <div v-if="catalog.length" class="flex flex-col gap-1">
                <div v-for="field in catalog" :key="field.id" class="group flex min-w-0 items-center gap-1 rounded-mf-md hover:bg-surface-hover">
                  <button type="button" draggable="true" class="flex min-w-0 flex-1 cursor-grab items-center gap-2 rounded-mf-md px-2 py-2 text-left text-sm outline-none focus-visible:ring-2 focus-visible:ring-primary/40 active:cursor-grabbing" :title="field.comment || fieldLabel(field)" :aria-label="$t('dataset.adhoc.addField', { name: fieldLabel(field) })" @click="choose(field)" @dragstart="startDrag($event, field)" @dragend="dragging = false">
                    <Hash v-if="isAdhocNumeric(field.type)" :size="15" class="shrink-0 text-primary"/><Type v-else :size="15" class="shrink-0 text-content-tertiary"/>
                    <span class="min-w-0 flex-1 truncate">{{ fieldLabel(field) }}</span><span class="shrink-0 text-[10px] text-content-tertiary">{{ field.type }}</span>
                  </button>
                  <MfButton size="sm" variant="ghost" class="size-7! shrink-0 p-0!" :aria-label="$t('dataset.adhoc.filterField', { name: fieldLabel(field) })" @click="choose(field, 'FILTER')"><Filter :size="13"/></MfButton>
                </div>
              </div>
              <MfEmpty v-else class="h-full" size="sm" :title="$t('common.noData')"/>
            </div>
            <p class="shrink-0 border-t border-line px-3 py-2.5 text-[11px] text-content-tertiary">{{ $t('dataset.adhoc.libraryHint') }}</p>
          </div>
          <MfResizeHandle v-if="wide" v-model="left.width.value" class="absolute inset-y-0 -right-1 z-10 mx-0!" :min="240" :max="max" :label="$t('dataset.adhoc.resizeFields')" @pointerdown.capture="resizing = true" @change="left.save"/>
        </aside>
      </Transition>
      <main class="flex min-h-0 min-w-0 flex-1 flex-col bg-surface/60">
        <div class="max-h-[42%] shrink-0 overflow-y-auto border-b border-line bg-surface px-3 py-2 sm:px-4">
          <MfAlert v-if="warning" variant="warning" class="mb-2" :title="warning"/>
          <section v-for="zone in zones" :key="zone.mode" class="my-1 flex min-h-12 items-start gap-2 rounded-mf-md border border-dashed p-2 transition-colors" :class="dragging ? 'border-primary/60 bg-primary/5' : 'border-line'" :aria-label="$t(zone.label)" @dragover="dragOver" @drop.prevent="drop($event, zone.mode)">
            <h2 class="flex w-16 shrink-0 items-center gap-1.5 pt-1 text-xs font-medium text-content-secondary"><component :is="zone.icon" :size="14"/>{{ $t(zone.label) }}</h2>
            <div class="flex min-w-0 flex-1 flex-wrap items-center gap-1.5">
              <div v-for="column in forMode(zone.mode)" :key="column.key" draggable="true" class="flex max-w-full items-center rounded-mf-md border text-xs" :class="selected === column.key ? 'border-primary bg-primary/8 text-primary' : 'border-line bg-surface-hover text-content-secondary'" @dragstart="startDrag($event, column, column.key)" @dragend="dragging = false">
                <button type="button" class="flex min-w-0 items-center gap-1.5 px-2 py-1.5 text-left" :title="fieldLabel(column)" @click="select(column)"><Settings2 :size="12" class="shrink-0"/><span class="max-w-40 truncate">{{ column.alias || fieldLabel(column) }}</span><span v-if="column.expression" class="text-[10px] opacity-70">{{ $t('dataset.adhoc.operator.' + column.expression) }}</span></button>
                <button type="button" class="mr-1 grid size-5 shrink-0 place-items-center rounded-mf-sm hover:bg-danger/10 hover:text-danger" :aria-label="$t('dataset.adhoc.removeField', { name: fieldLabel(column) })" @click="remove(column.key)"><X :size="12"/></button>
              </div>
              <span v-if="!forMode(zone.mode).length" class="py-1.5 text-xs text-content-tertiary">{{ $t(zone.hint) }}</span>
            </div>
            <MfButton size="sm" variant="ghost" class="size-7! shrink-0 p-0!" :aria-label="$t('dataset.adhoc.addTo', { mode: $t(zone.label) })" @click="addingTo = zone.mode; fieldTab = zone.mode === 'FILTER' ? 'all' : zone.mode; show('left')"><Plus :size="14"/></MfButton>
          </section>
        </div>
        <div class="flex min-h-0 flex-1 flex-col overflow-hidden">
          <div class="flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-line px-3 py-1.5 text-xs sm:px-4">
            <span v-if="queried" class="flex flex-wrap items-center gap-2" role="status"><component :is="failure ? CircleAlert : CircleCheck" :size="14" :class="failure ? 'text-danger' : 'text-success'"/><span :class="failure ? 'text-danger' : 'text-success'">{{ $t(failure ? 'query.message.failed' : 'query.message.success') }}</span><span class="text-content-tertiary">{{ elapsed }} ms · {{ $t('query.message.returnedRows', [rows.length]) }}</span><span v-if="stale" class="text-warning">{{ $t('dataset.adhoc.staleHint') }}</span></span>
            <span v-else class="text-content-tertiary">{{ $t('dataset.adhoc.resultTab') }}</span>
            <div class="ml-auto flex gap-1"><MfButton size="sm" variant="ghost" :disabled="!headers.length || running" @click="configureChart"><ChartColumn :size="14"/>{{ $t('dataset.common.visual') }}</MfButton><MfButton size="sm" variant="ghost" :disabled="!headers.length || running" @click="exportResults"><Download :size="14"/>{{ $t('common.export') }}</MfButton></div>
          </div>
          <div class="relative flex min-h-0 flex-1 flex-col overflow-hidden px-3 pb-3 sm:px-4">
            <div v-if="running" class="absolute inset-0 z-10 flex items-center justify-center bg-surface/75"><MfSpinner size="lg" :label="$t('dataset.adhoc.query')"/></div>
            <MfTabs v-model:active-key="resultTab" fill class="min-h-0 flex-1">
              <MfTabPane item-key="data" :tab="$t('dataset.adhoc.resultTab')"><ResultTable v-if="headers.length" fill searchable :headers="headers" :rows="rows"/><MfEmpty v-else class="h-full" :variant="queried ? 'data' : 'initial'" :title="$t(queried ? 'query.result.empty' : 'dataset.adhoc.initial')" :description="$t(queried ? 'query.result.emptyTip' : 'dataset.adhoc.initialHint')"/></MfTabPane>
              <MfTabPane item-key="chart" :tab="$t('queryChart.chart')"><VisualChart v-if="legacyPreview && legacyPreview.type !== 'TABLE'" :configuration="legacyPreview" :submitted="false" height="100%"/><QueryChartPreview v-else-if="headers.length" :options="options" :rows="rows" :heading="false"/><MfEmpty v-else class="h-full" variant="initial" :title="$t('dataset.adhoc.queryFirst')"/></MfTabPane>
              <MfTabPane item-key="sql" tab="SQL"><SqlCodeBlock fill :code="sql" :placeholder="$t('dataset.adhoc.sqlHint')"/></MfTabPane>
              <MfTabPane item-key="message" :tab="$t('dataset.adhoc.messages')"><MfAlert v-if="queried" class="mt-3" :variant="failure ? 'danger' : 'success'" :title="$t(failure ? 'query.message.failed' : 'query.message.success')">{{ failure || $t('query.message.returnedRows', [rows.length]) }}</MfAlert><MfEmpty v-else class="h-full" size="sm" :title="$t('dataset.adhoc.noMessages')"/></MfTabPane>
            </MfTabs>
          </div>
        </div>
      </main>
      <Transition name="adhoc-panel">
        <aside v-show="rightVisible" id="adhoc-properties" :inert="!rightVisible" class="adhoc-side adhoc-right inset-y-0 right-0 z-30 border-l border-line bg-surface" :class="wide ? 'adhoc-docked relative' : 'absolute shadow-mf-lg'">
          <div class="adhoc-panel-content flex h-full min-h-0 flex-col">
            <StudioPanelHeader :title="$t('dataset.adhoc.properties')" :pinnable="false" :pinned="true" @close="close('right')"/>
            <MfTabs v-model:active-key="inspectorTab" fill class="adhoc-inspector-tabs min-h-0 flex-1">
              <MfTabPane item-key="query" :tab="$t('dataset.adhoc.querySettings')">
                <div class="grid min-w-0 grid-cols-[minmax(0,1fr)] gap-3 p-3">
                  <InspectorSection ref="querySettingsGroup" icon="actions" :title="$t('dataset.adhoc.querySettings')">
                    <MfForm ref="queryForm" :model="limitModel" :disabled="running">
                      <MfFormItem name="limit" :label="$t('dataset.adhoc.rowLimit')" :rules="[{ required: true, type: 'number', min: 1, validator: value => Number.isInteger(value), message: $t('dataset.adhoc.invalidLimit') }]">
                        <MfInputNumber v-model="limit" class="w-full" decimal-only commit-on-change :min="1" :precision="0" :aria-label="$t('dataset.adhoc.rowLimit')"/>
                      </MfFormItem>
                    </MfForm>
                  </InspectorSection>
                  <InspectorSection ref="fieldSettingsGroup" icon="data" :title="$t('dataset.adhoc.fieldSettings')">
                    <DatasetAdhocFieldSettings v-if="selectedColumn" ref="settings" :key="selectedColumn.key" :column="selectedColumn" :disabled="running" @change="update"/>
                    <MfEmpty v-else size="sm" :title="$t('dataset.adhoc.selectField')"/>
                  </InspectorSection>
                </div>
              </MfTabPane>
              <MfTabPane item-key="chart" :tab="$t('queryChart.chart')">
                <div v-if="legacy" class="p-3">
                  <InspectorSection icon="appearance" :title="$t('dataset.adhoc.chartSettings')">
                    <MfAlert variant="info" :title="$t('dataset.adhoc.legacyChart')"/>
                    <MfButton v-if="legacy.type !== 'TABLE'" @click="legacySettings = true">{{ $t('common.configure') }}</MfButton>
                    <MfButton @click="legacy = null">{{ $t('dataset.adhoc.upgradeChart') }}</MfButton>
                  </InspectorSection>
                </div>
                <DatasetAdhocChartSettings v-else-if="headers.length" class="p-3" :options="options" :headers="headers" :rows="rows" @change="options = $event; resultTab = 'chart'"/>
                <MfEmpty v-else class="h-full" size="sm" variant="initial" :title="$t('dataset.adhoc.queryFirst')"/>
              </MfTabPane>
            </MfTabs>
          </div>
          <MfResizeHandle v-if="wide" v-model="right.width.value" reverse class="absolute inset-y-0 -left-1 z-10 mx-0!" :min="280" :max="max" :label="$t('dataset.adhoc.resizeProperties')" @pointerdown.capture="resizing = true" @change="right.save"/>
        </aside>
      </Transition>
      <MfButton id="dataset-adhoc-left-toggle" size="sm" class="adhoc-toggle rounded-l-none! rounded-r-mf-md!" :style="{ left: leftVisible ? leftWidth + 'px' : '0px' }" :aria-expanded="leftVisible" aria-controls="adhoc-field-library" :aria-label="$t('dataset.adhoc.fieldLibrary')" @click="toggle('left')"><ChevronLeft v-if="leftVisible" :size="16"/><ChevronRight v-else :size="16"/></MfButton>
      <MfButton id="dataset-adhoc-right-toggle" size="sm" class="adhoc-toggle rounded-r-none! rounded-l-mf-md!" :style="{ right: rightVisible ? rightWidth + 'px' : '0px' }" :aria-expanded="rightVisible" aria-controls="adhoc-properties" :aria-label="$t('dataset.adhoc.properties')" @click="toggle('right')"><ChevronRight v-if="rightVisible" :size="16"/><ChevronLeft v-else :size="16"/></MfButton>
    </div>
    <VisualConfigure v-if="legacySettings && legacy" :is-visible="legacySettings" :configuration="legacyPreview!" :field-group="legacyGroups" @close="legacySettings = false" @change="legacy.chartConfigure = $event || undefined"/>
    <DatasetReport v-if="publishVisible" :visible="publishVisible" :info="{ ...report, name: report.name || dataset.name }" :code="savedCode || reportCode || undefined" :dataset-code="code" :commit-options="legacy || createQueryChartSnapshot(options, rows)" :configure="query" @saved="savedCode = $event.code; report = { name: $event.name, description: $event.description }" @close="publishVisible = false"/>
  </div>
</template>

<style scoped>
.adhoc-side { flex: none; min-height: 0; }
.adhoc-left, .adhoc-left > .adhoc-panel-content { width: var(--adhoc-left); }
.adhoc-right, .adhoc-right > .adhoc-panel-content { width: var(--adhoc-right); }
.adhoc-inspector-tabs :deep([role="tablist"]) { margin: 0 12px; }
.adhoc-inspector-tabs :deep([role="tab"]) { flex: 1; min-width: 0; padding-inline: 6px; }
.adhoc-toggle { position: absolute; top: 50%; z-index: 40; width: 25px; height: 46px; padding: 0; transform: translateY(-50%); }
@media (prefers-reduced-motion: no-preference) {
  .adhoc-panel-enter-active, .adhoc-panel-leave-active { transition: transform var(--mf-duration-base) var(--mf-easing); }
  .adhoc-docked.adhoc-panel-enter-active, .adhoc-docked.adhoc-panel-leave-active { overflow: clip; transition: width var(--mf-duration-base) var(--mf-easing); }
  .adhoc-docked.adhoc-panel-enter-from, .adhoc-docked.adhoc-panel-leave-to { width: 0; }
  .adhoc-left:not(.adhoc-docked).adhoc-panel-enter-from, .adhoc-left:not(.adhoc-docked).adhoc-panel-leave-to { transform: translateX(-100%); }
  .adhoc-right:not(.adhoc-docked).adhoc-panel-enter-from, .adhoc-right:not(.adhoc-docked).adhoc-panel-leave-to { transform: translateX(100%); }
  .adhoc-toggle { transition: left var(--mf-duration-base) var(--mf-easing), right var(--mf-duration-base) var(--mf-easing); }
  .adhoc-resizing .adhoc-toggle { transition: none; }
}
</style>
