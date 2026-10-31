<template>
  <div class="relative flex min-h-full flex-col gap-3 pb-2">
    <div v-if="loading && !overview" class="flex flex-1 items-center justify-center py-16"><MfSpinner/></div>
    <MfEmpty v-else-if="!overview" class="flex-1" variant="initial" :title="$t('common.noData')"/>
    <template v-else>
      <header class="flex items-center gap-4 pt-1">
        <span class="inline-flex size-14 shrink-0 items-center justify-center rounded-mf-lg bg-success/10 text-success"><Database :size="30"/></span>
        <div class="min-w-0">
          <div class="flex items-center gap-2">
            <h1 class="m-0 truncate text-2xl font-semibold text-content">{{ overview.name }}</h1>
            <MfTag v-if="sourceType" size="sm">{{ sourceType }}</MfTag>
          </div>
          <p class="m-0 mt-1 text-sm text-content-tertiary">{{ $t('source.database.subtitle') }}</p>
        </div>
      </header>

      <section class="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <div v-for="tile in stats" :key="tile.key" class="flex items-center gap-4 rounded-mf-md border border-line px-5 py-4">
          <span class="inline-flex size-12 shrink-0 items-center justify-center rounded-mf-md" :class="tile.tone" :style="tile.style"><component :is="tile.icon" :size="24"/></span>
          <div class="min-w-0">
            <p class="m-0 text-sm text-content-secondary">{{ $t(`source.database.stat.${tile.key}`) }}</p>
            <p class="m-0 text-2xl font-semibold tabular-nums text-content">{{ tile.value }}</p>
          </div>
        </div>
      </section>

      <div class="grid grid-cols-1 gap-3 lg:grid-cols-2">
        <section class="rounded-mf-md border border-line p-4">
          <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><FileText :size="18"/>{{ $t('source.database.basic') }}</h3>
          <dl class="m-0 grid grid-cols-[8rem_minmax(0,1fr)] gap-y-2.5 text-sm">
            <template v-for="item in basics" :key="item.label">
              <dt class="text-content-secondary">{{ item.label }}</dt>
              <dd class="m-0 break-words tabular-nums" :class="item.muted ? 'text-content-tertiary' : 'text-content'">{{ item.value }}</dd>
            </template>
          </dl>
        </section>

        <section class="rounded-mf-md border border-line p-4">
          <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><HardDrive :size="18"/>{{ $t('source.database.space') }}</h3>
          <div class="flex flex-wrap items-center justify-around gap-6">
            <svg viewBox="0 0 160 160" class="size-40 shrink-0" role="img" :aria-label="$t('source.database.space')" @mouseleave="hovered = null">
              <circle cx="80" cy="80" :r="RADIUS" fill="none" class="stroke-surface-hover" stroke-width="18"/>
              <circle v-for="arc in arcs" :key="arc.key" cx="80" cy="80" :r="RADIUS" fill="none" stroke-width="18" :stroke="SPACE_COLORS[arc.key]"
                      :stroke-dasharray="arc.dash" :stroke-dashoffset="arc.offset" transform="rotate(-90 80 80)" class="cursor-default transition-opacity"
                      :class="hovered && hovered !== arc.key && 'opacity-40'" @mouseenter="hovered = arc.key"/>
              <text x="80" y="78" text-anchor="middle" class="fill-content text-[17px] font-semibold tabular-nums">{{ centerValue }}</text>
              <text x="80" y="98" text-anchor="middle" class="fill-content-tertiary text-[11px]">{{ centerLabel }}</text>
            </svg>
            <ul class="m-0 flex min-w-52 list-none flex-col gap-3 p-0 text-sm">
              <li v-for="segment in segments" :key="segment.key" class="grid grid-cols-[1rem_4rem_5rem_4rem] items-center gap-2 rounded-mf-sm px-1"
                  :class="hovered === segment.key && 'bg-surface-hover'" @mouseenter="hovered = segment.key" @mouseleave="hovered = null">
                <span class="size-2.5 rounded-full" :style="{ background: SPACE_COLORS[segment.key] }"/>
                <span class="text-content">{{ $t(`source.database.spaceKind.${segment.key}`) }}</span>
                <span class="tabular-nums text-content">{{ formatBytes(segment.bytes) }}</span>
                <span class="tabular-nums text-content-tertiary">({{ segment.percent }}%)</span>
              </li>
            </ul>
          </div>
        </section>
      </div>

      <section class="rounded-mf-md border border-line p-4">
        <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Boxes :size="18"/>{{ $t('source.database.objects') }}</h3>
        <div class="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
          <div v-for="tile in objects" :key="tile.key" class="flex items-center gap-3 rounded-mf-md bg-surface-hover/60 px-4 py-3">
            <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md" :class="tile.tone" :style="tile.style"><component :is="tile.icon" :size="20"/></span>
            <div class="min-w-0">
              <p class="m-0 text-xs text-content-secondary">{{ $t(`source.database.object.${tile.key}`) }}</p>
              <p class="m-0 text-lg font-semibold tabular-nums text-content">{{ tile.value }}</p>
            </div>
          </div>
        </div>
      </section>

      <div class="grid grid-cols-1 gap-3 xl:grid-cols-[minmax(0,2fr)_minmax(18rem,1fr)]">
        <section class="min-w-0 rounded-mf-md border border-line p-4">
          <div class="mb-3 flex items-center justify-between gap-2">
            <h3 class="m-0 flex items-center gap-2 text-base font-semibold text-content"><Table2 :size="18"/>{{ $t('source.database.tables') }}</h3>
            <button v-if="tables.length > 5" type="button" class="inline-flex items-center gap-1 text-xs text-primary hover:underline" @click="showAllTables = !showAllTables">
              {{ showAllTables ? $t('source.database.collapseTables') : $t('source.database.allTables', { count: tables.length }) }}<ArrowRight :size="13" :class="showAllTables && '-rotate-90'"/>
            </button>
          </div>
          <MfTable :columns="tableColumns" :data-source="visibleTables" row-key="name" density="compact" :bordered="false" :empty-text="$t('common.noData')">
            <template #cell-name="{ record }">
              <button type="button" class="inline-flex max-w-full items-center gap-2 text-primary hover:underline" @click="openTable(record.name)">
                <Table2 :size="15" class="shrink-0 text-content-secondary"/><span class="truncate">{{ record.name }}</span>
              </button>
            </template>
            <template #cell-rows="{ record }"><span class="tabular-nums">{{ record.rows === null ? '-' : record.rows.toLocaleString() }}</span></template>
            <template #cell-dataBytes="{ record }"><span class="tabular-nums">{{ formatBytes(record.dataBytes) }}</span></template>
            <template #cell-indexBytes="{ record }"><span class="tabular-nums">{{ formatBytes(record.indexBytes) }}</span></template>
            <template #cell-updated="{ record }"><span class="tabular-nums">{{ record.updated }}</span></template>
          </MfTable>
        </section>

        <section class="rounded-mf-md border border-line p-4">
          <h3 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Zap :size="18"/>{{ $t('source.database.quick') }}</h3>
          <div class="grid grid-cols-2 gap-2">
            <button v-for="action in quickActions" :key="action.key" type="button"
                    class="flex items-center gap-3 rounded-mf-md border border-line px-3 py-2.5 text-left text-sm text-content hover:bg-surface-hover" @click="quick(action.key)">
              <component :is="action.icon" :size="18" class="shrink-0 text-primary"/><span class="truncate">{{ $t(`source.database.action.${action.key}`) }}</span>
            </button>
          </div>
        </section>
      </div>
    </template>

    <SourceDatabasePropertiesEditor v-if="editorVisible && overview" v-model="form" :open="editorVisible" :database="overview.name" :charsets="charsets"
                                    :collations="charsetCollations" :preview-sql="previewSql" :preview-error="previewError" :saving="saving"
                                    :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @charset="onCharset" @save="submitEditor"/>
    <TableCreate v-if="tableCreateVisible" :is-visible="tableCreateVisible" @close="onTableCreated"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight, Boxes, Clock, Columns3, Database, Download, Eye, FileText, HardDrive, Layers, Link2, ListTree, Plus, SquareTerminal, Table2,
  Upload, Workflow, Zap } from '@lucide/vue'
import TableCreate from './components/TableCreate.vue'
import SourceDatabasePropertiesEditor from './components/SourceDatabasePropertiesEditor.vue'
import { formatBytes } from '@/model/source-table-properties'
import { ringArcs, type SpaceKey } from '@/model/source-database'
import { useSourceDatabase } from '@/composables/useSourceDatabase'

defineOptions({ name: 'SourceDatabase' })
const { t } = useI18n()
const {
  can, loading, overview, tables, visibleTables, showAllTables, sourceType, segments, totalBytes, tableCreateVisible, openTable, quick, onTableCreated,
  editorVisible, saving, form, charsets, charsetCollations, previewSql, previewError, fieldPanelHost, fieldPanelWidth, closeEditor, onCharset, submitEditor,
} = useSourceDatabase()

const RADIUS = 62
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
// "Other" is allocated free space, drawn in neutral gray as the remainder bucket.
const SPACE_COLORS: Record<SpaceKey, string> = { data: 'var(--mf-color-success)', index: 'var(--mf-color-info)', other: 'var(--mf-color-content-tertiary)' }
const COMPLEX = { color: 'var(--mf-color-data-complex)', background: 'color-mix(in srgb, var(--mf-color-data-complex) 12%, transparent)' }
const hovered = ref<SpaceKey | null>(null)
const arcs = computed(() => ringArcs(segments.value, CIRCUMFERENCE, 2))
const hoveredSegment = computed(() => segments.value.find(segment => segment.key === hovered.value) || null)
const centerValue = computed(() => formatBytes(hoveredSegment.value ? hoveredSegment.value.bytes : totalBytes.value, 'MB'))
const centerLabel = computed(() => hoveredSegment.value ? `${t(`source.database.spaceKind.${hoveredSegment.value.key}`)} ${hoveredSegment.value.percent}%` : t('source.database.stat.size'))

const number = (value: number) => value.toLocaleString()
const stats = computed(() => {
  const db = overview.value
  if (!db) return []
  return [
    { key: 'tables', icon: Table2, tone: 'bg-info/10 text-info', value: number(db.tables) },
    { key: 'columns', icon: Columns3, tone: '', style: COMPLEX, value: number(db.columns) },
    { key: 'indexes', icon: ListTree, tone: 'bg-success/10 text-success', value: number(db.indexes) },
    { key: 'size', icon: HardDrive, tone: 'bg-warning/10 text-warning', value: formatBytes(totalBytes.value, 'MB') },
  ]
})
const basics = computed(() => {
  const db = overview.value
  if (!db) return []
  return [
    { label: t('source.database.name'), value: db.name },
    { label: t('source.database.charset'), value: db.charset || '-' },
    { label: t('source.database.collation'), value: db.collation || '-' },
    { label: t('source.database.created'), value: db.created },
    { label: t('source.database.updated'), value: db.updated || t('source.common.notUpdated'), muted: !db.updated },
  ]
})
const objects = computed(() => {
  const db = overview.value
  if (!db) return []
  return [
    { key: 'tables', icon: Table2, tone: 'bg-success/10 text-success', value: db.tables },
    { key: 'views', icon: Eye, tone: 'bg-info/10 text-info', value: db.views },
    { key: 'procedures', icon: Workflow, tone: 'bg-info/10 text-info', value: db.procedures },
    { key: 'events', icon: Clock, tone: 'bg-warning/10 text-warning', value: db.events },
    { key: 'triggers', icon: Layers, tone: '', style: COMPLEX, value: db.triggers },
    { key: 'foreignKeys', icon: Link2, tone: 'bg-info/10 text-info', value: db.foreignKeys },
  ]
})
const tableColumns = computed(() => [
  { title: t('source.database.column.name'), dataIndex: 'name', width: 260, ellipsis: true },
  { title: t('source.database.column.engine'), dataIndex: 'engine' },
  { title: t('source.database.column.rows'), dataIndex: 'rows' },
  { title: t('source.database.column.data'), dataIndex: 'dataBytes' },
  { title: t('source.database.column.index'), dataIndex: 'indexBytes' },
  { title: t('source.database.column.updated'), dataIndex: 'updated' },
])
const quickActions = computed(() => [
  ...(can('table:create') ? [{ key: 'create' as const, icon: Plus }] : []),
  ...(can('tab:sql') ? [{ key: 'sql' as const, icon: SquareTerminal }] : []),
  { key: 'export' as const, icon: Download },
  { key: 'import' as const, icon: Upload },
])
</script>
