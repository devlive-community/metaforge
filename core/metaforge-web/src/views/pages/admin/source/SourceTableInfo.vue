<template>
  <div class="flex h-full min-h-0 flex-col gap-3 overflow-y-auto">
    <MfEmpty v-if="!properties && !loading" class="flex-1" variant="initial" :title="$t('common.noData')"/>
    <div v-else-if="!properties" class="flex flex-1 items-center justify-center"><MfSpinner/></div>
    <template v-else>
      <section class="shrink-0 rounded-mf-md border border-line p-4">
        <div class="mb-3 flex items-center justify-between gap-2">
          <h3 class="m-0 text-base font-semibold text-content">{{ $t('source.props.basic') }}</h3>
          <MfButton v-if="executed" size="sm" @click="executedVisible = true">
            <CircleCheck v-if="executed.success" :size="14" class="text-success"/><CircleX v-else :size="14" class="text-danger"/>{{ $t('source.executed.view') }}
          </MfButton>
        </div>
        <div class="grid grid-cols-1 gap-x-10 gap-y-2 md:grid-cols-2">
          <dl v-for="(group, index) in basics" :key="index" class="m-0 grid grid-cols-[8rem_minmax(0,1fr)] gap-y-2.5 text-sm" :class="index === 1 && 'md:border-l md:border-line md:pl-10'">
            <template v-for="item in group" :key="item.label">
              <dt class="text-content-secondary">{{ item.label }}</dt>
              <dd class="m-0 flex min-w-0 items-center gap-1.5 text-content">
                <span class="break-words">{{ item.value }}</span>
                <button v-if="item.copy" type="button" class="inline-flex shrink-0 text-content-tertiary hover:text-content" :aria-label="$t('source.executed.copy')" @click="copy(String(item.value))"><Copy :size="14"/></button>
                <button v-if="item.edit && editable" type="button" class="inline-flex shrink-0 text-content-tertiary hover:text-content" :aria-label="$t('source.props.editComment')" @click="openEditor(true)"><Pencil :size="14"/></button>
              </dd>
            </template>
          </dl>
        </div>
      </section>

      <section class="shrink-0 rounded-mf-md border border-line p-4">
        <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.props.statistics') }}</h3>
        <div class="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
          <div v-for="tile in tiles" :key="tile.label" class="flex flex-col gap-1 rounded-mf-md border border-line px-3 py-3">
            <div class="flex items-center gap-3">
              <component :is="tile.icon" :size="22" class="shrink-0" :class="tile.tone"/>
              <div class="min-w-0">
                <p class="m-0 text-xs text-content-secondary">{{ tile.label }}</p>
                <p class="m-0 text-lg font-semibold tabular-nums text-content">{{ tile.value }}</p>
              </div>
            </div>
            <p class="m-0 text-xs text-content-tertiary">{{ tile.hint }}</p>
          </div>
        </div>
        <p class="m-0 mt-3 flex items-center gap-1.5 rounded-mf-md bg-surface-hover px-3 py-2 text-xs text-content-tertiary"><Info :size="13" class="shrink-0"/>{{ $t('source.props.statisticsNote') }}</p>
      </section>

      <section class="flex shrink-0 items-start justify-between gap-4 rounded-mf-md border border-line p-4">
        <div class="flex min-w-0 gap-3">
          <FileText :size="22" class="shrink-0 text-content-secondary"/>
          <div class="min-w-0">
            <h3 class="m-0 text-base font-semibold text-content">{{ $t('source.props.comment') }}</h3>
            <p class="m-0 mt-1 break-words text-sm" :class="properties.comment ? 'text-content-secondary' : 'text-content-tertiary'">{{ properties.comment || $t('source.props.noComment') }}</p>
          </div>
        </div>
        <MfButton v-if="editable" size="sm" :disabled="saving" @click="openEditor(true)"><Pencil :size="14"/>{{ $t('common.edit') }}</MfButton>
      </section>

      <section class="shrink-0 rounded-mf-md border border-line p-4">
        <h3 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.props.currentSql') }}</h3>
        <SqlCodeBlock :code="currentSql"/>
      </section>
    </template>

    <SourceTablePropertiesEditor v-if="editorVisible" v-model="form" :open="editorVisible" :table="properties?.name || ''" :engines="engines" :charsets="charsets"
                                 :collations="charsetCollations" :focus-comment="focusComment" :issues="issues" :preview-sql="previewSql" :preview-error="previewError"
                                 :saving="saving" :width="fieldPanelWidth" :container="fieldPanelHost" @close="closeEditor()" @charset="onCharset" @save="submitEditor"/>
    <ExecutedSqlDialog v-model:open="executedVisible" :executed="executed"/>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleCheck, CircleX, Copy, Database, FileText, Hash, Info, Layers, Link, Link2, Pencil, Table2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import ExecutedSqlDialog from '@/views/components/sql/ExecutedSqlDialog.vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import SourceTablePropertiesEditor from './components/SourceTablePropertiesEditor.vue'
import { formatBytes } from '@/model/source-table-properties'
import { useSourceTableProperties } from '@/composables/useSourceTableProperties'

defineOptions({ name: 'SourceTableInfo' })
const { t } = useI18n()
const {
  editable, loading, saving, properties, currentSql, engines, charsets, charsetCollations, editorVisible, form, issues, previewSql, previewError,
  focusComment, fieldPanelHost, fieldPanelWidth, openEditor, closeEditor, onCharset, submitEditor, executed, executedVisible,
} = useSourceTableProperties()

const number = (value: number | null) => value === null ? '-' : value.toLocaleString()
type BasicItem = { label: string; value: string; copy?: boolean; edit?: boolean }
const basics = computed<BasicItem[][]>(() => {
  const table = properties.value
  if (!table) return []
  return [
    [
      { label: t('source.props.name'), value: table.name, copy: true },
      { label: t('source.props.database'), value: table.database },
      { label: t('source.props.type'), value: table.type || '-' },
      { label: t('source.props.engine'), value: table.engine || '-' },
      { label: t('source.props.rowFormat'), value: table.rowFormat || '-' },
      { label: t('source.props.created'), value: table.created || '-' },
    ],
    [
      { label: t('source.props.charset'), value: table.charset || '-' },
      { label: t('source.props.collation'), value: table.collation || '-' },
      { label: t('source.props.autoIncrement'), value: number(table.autoIncrement) },
      { label: t('source.props.dataLength'), value: formatBytes(table.dataLength) },
      { label: t('source.props.indexLength'), value: formatBytes(table.indexLength) },
      { label: t('source.props.comment'), value: table.comment || '-', edit: true },
    ],
  ]
})
const tiles = computed(() => {
  const table = properties.value
  if (!table) return []
  return [
    { icon: Table2, tone: 'text-success', label: t('source.props.rows'), value: number(table.rows), hint: t('source.props.rowsHint') },
    { icon: Database, tone: 'text-info', label: t('source.props.dataSize'), value: formatBytes(table.dataLength, 'MB'), hint: t('source.props.dataSizeHint') },
    { icon: Link, tone: 'text-success', label: t('source.props.indexCount'), value: number(table.indexCount), hint: t('source.props.indexCountHint') },
    { icon: Layers, tone: 'text-success', label: t('source.props.indexSize'), value: formatBytes(table.indexLength, 'MB'), hint: t('source.props.indexSizeHint') },
    { icon: Link2, tone: 'text-content-secondary', label: t('source.props.foreignKeyCount'), value: number(table.foreignKeyCount), hint: t('source.props.foreignKeyCountHint') },
    { icon: Hash, tone: 'text-info', label: t('source.props.nextAutoIncrement'), value: number(table.autoIncrement), hint: t('source.props.nextAutoIncrementTile') },
  ]
})
function copy(text: string) {
  ObjectUtils.copy(text)
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
