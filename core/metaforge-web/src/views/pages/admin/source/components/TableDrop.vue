<template>
  <MfModal v-model:open="visible" width="1080px" height="min(760px, 90vh)" :close-disabled="submitting" :close-on-overlay="!submitting" :close-on-esc="!submitting">
    <template #title>
      <span class="flex items-center gap-4">
        <span class="inline-flex size-12 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="24"/></span>
        <span class="flex min-w-0 flex-col">
          <span class="truncate text-lg">{{ $t('source.common.dropTable') }}</span>
          <span class="mt-0.5 truncate text-sm font-normal text-content-tertiary">{{ $t('source.dropTable.subtitle') }}</span>
        </span>
      </span>
    </template>

    <div v-if="loading" class="flex h-full min-h-80 items-center justify-center"><MfSpinner/></div>
    <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <div class="flex min-w-0 flex-col gap-4">
        <section class="rounded-mf-md bg-surface-hover/60 p-4">
          <h4 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Table2 :size="18"/>{{ $t('source.truncate.tableInfo') }}</h4>
          <dl class="m-0 grid grid-cols-[8rem_minmax(0,1fr)] gap-y-2 text-sm">
            <template v-for="item in facts" :key="item.label">
              <dt class="text-content-secondary">{{ item.label }}</dt>
              <dd class="m-0 flex min-w-0 items-center gap-1.5 break-all text-content" :class="item.numeric && 'tabular-nums'">
                {{ item.value }}
                <MfTooltip v-if="item.hint" :content="item.hint"><Info :size="14" class="shrink-0 text-content-tertiary"/></MfTooltip>
              </dd>
            </template>
          </dl>
        </section>

        <section class="rounded-mf-md bg-danger/10 p-4">
          <h4 class="m-0 mb-2 flex items-center gap-2 text-base font-semibold text-danger"><TriangleAlert :size="18"/>{{ $t('source.dropTable.warning') }}</h4>
          <ul class="m-0 flex list-disc flex-col gap-1.5 pl-5 text-sm text-danger">
            <li v-for="index in 4" :key="index">{{ $t(`source.dropTable.warningItem${index}`) }}</li>
          </ul>
        </section>

        <section>
          <h4 class="m-0 mb-2 flex items-center gap-2 text-base font-semibold text-content"><Network :size="18"/>{{ $t('source.dropTable.related') }}</h4>
          <ul class="m-0 flex list-none flex-col gap-1 p-0">
            <li v-for="kind in DEPENDENCY_KINDS" :key="kind" class="overflow-hidden rounded-mf-md border border-line">
              <button type="button" class="flex w-full items-center gap-3 px-3 py-2 text-left text-sm text-content hover:bg-surface-hover disabled:cursor-default disabled:hover:bg-transparent"
                      :disabled="!count(kind)" :aria-expanded="expanded === kind" @click="toggle(kind)">
                <span class="min-w-0 flex-1 truncate">{{ $t(`source.dropTable.kind.${kind}`) }}</span>
                <span class="rounded-mf-sm px-2 py-0.5 text-xs font-medium tabular-nums" :class="countTone(kind)">{{ $t('source.dropTable.count', { count: count(kind) ?? '-' }) }}</span>
                <ChevronRight :size="15" class="shrink-0 text-content-tertiary transition-transform" :class="expanded === kind && 'rotate-90'"/>
              </button>
              <ul v-if="expanded === kind && dependencies" class="m-0 flex list-none flex-col gap-1 border-t border-line bg-surface-hover/40 px-3 py-2 text-xs">
                <li v-for="item in dependencies[kind]" :key="item.schema + '.' + item.name" class="flex min-w-0 gap-2">
                  <span class="truncate font-medium text-content">{{ item.schema }}.{{ item.name }}</span>
                  <span v-if="item.detail" class="truncate text-content-tertiary">{{ item.detail }}</span>
                </li>
              </ul>
            </li>
          </ul>
          <p v-if="dependencyError" class="m-0 mt-2 text-xs text-warning">{{ $t('source.dropTable.relatedPartial', { message: dependencyError }) }}</p>
        </section>

        <div class="flex flex-col gap-3">
          <MfCheckbox v-model="acknowledged" :disabled="submitting">{{ $t('source.dropTable.acknowledge', { table }) }}</MfCheckbox>
          <div class="flex flex-col gap-1.5">
            <label :for="`${uid}-name`" class="text-sm text-danger">{{ $t('source.truncate.typeName', { table }) }} *</label>
            <MfInput :id="`${uid}-name`" v-model="typedName" :disabled="submitting" :invalid="!!typedName && !nameMatches" autocomplete="off" spellcheck="false">
              <template v-if="nameMatches" #suffix><Check :size="16" class="text-success"/></template>
            </MfInput>
          </div>
        </div>
      </div>

      <div class="flex min-w-0 flex-col gap-4">
        <section class="rounded-mf-md border border-line p-4">
          <div class="mb-3 flex items-start justify-between gap-3">
            <div class="flex min-w-0 gap-3">
              <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-full bg-surface-hover text-content-secondary"><CodeXml :size="16"/></span>
              <div class="min-w-0">
                <h4 class="m-0 text-base font-semibold text-content">{{ $t('source.truncate.sql') }}</h4>
                <p class="m-0 mt-0.5 text-xs text-content-tertiary">{{ $t('source.dropTable.sqlHint') }}</p>
              </div>
            </div>
            <MfButton size="sm" :disabled="!statement" @click="copy"><ClipboardCopy :size="14"/>{{ $t('source.truncate.copySql') }}</MfButton>
          </div>
          <SqlCodeBlock :code="statement" :copyable="false" :placeholder="loadError" max-height="max-h-40"/>
        </section>

        <section>
          <h4 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.dropTable.impactTitle') }}</h4>
          <ul class="m-0 flex list-none flex-col gap-2 p-0">
            <li v-for="item in impacts" :key="item.key" class="flex items-center gap-3 rounded-mf-md bg-surface-hover/60 px-3 py-2.5">
              <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-md" :class="item.tone"><component :is="item.icon" :size="18"/></span>
              <span class="flex min-w-0 flex-col">
                <span class="text-sm font-medium text-content">{{ $t(`source.dropTable.impact.${item.key}`) }}</span>
                <span class="text-xs text-content-secondary">{{ item.hint }}</span>
              </span>
            </li>
          </ul>
        </section>
      </div>
    </div>

    <template #footer>
      <MfButton :disabled="submitting" @click="visible = false">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="submitting" :disabled="!canSubmit" @click="submit"><Trash2 :size="15"/>{{ $t('source.dropTable.submit') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, ChevronRight, ClipboardCopy, CodeXml, Database, Eye, Info, Link2, Network, Table2, TriangleAlert, Trash2 } from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import { DEPENDENCY_KINDS, type DependencyKind } from '@/model/source-table-drop'
import { useTableDrop } from '@/composables/useTableDrop'

defineOptions({ name: 'TableDrop' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})
const { t } = useI18n()
const uid = `table-drop-${getCurrentInstance()!.uid}`
const { loading, submitting, info, dependencies, dependencyError, statement, loadError, expanded, acknowledged, typedName, table, nameMatches, canSubmit,
  count, toggle, copy, submit } = useTableDrop(() => { visible.value = false })

const rows = computed(() => info.value?.rows === null || info.value?.rows === undefined ? '-' : info.value.rows.toLocaleString())
const facts = computed(() => {
  const table = info.value
  if (!table) return []
  return [
    { label: t('source.truncate.name'), value: table.name },
    { label: t('source.truncate.database'), value: table.database },
    { label: t('source.truncate.engine'), value: table.engine },
    { label: t('source.dropTable.rows'), value: rows.value, numeric: true, hint: t('source.truncate.rowsHint') },
    { label: t('source.dropTable.size'), value: table.sizeMb === null ? '-' : `${table.sizeMb} MB`, numeric: true },
    { label: t('source.dropTable.created'), value: table.created, numeric: true },
    { label: t('source.dropTable.updated'), value: table.updated, numeric: true },
    { label: t('source.dropTable.comment'), value: table.comment },
  ]
})
// Triggers go with the table; anything else depending on it breaks or blocks the drop.
const countTone = (kind: DependencyKind) => {
  const value = count(kind)
  if (!value) return value === 0 ? 'bg-success/10 text-success' : 'bg-surface-hover text-content-tertiary'
  return kind === 'trigger' ? 'bg-warning/10 text-warning' : 'bg-danger/10 text-danger'
}
const dependencyHint = (kind: 'foreign_key' | 'view') => {
  const value = count(kind)
  if (value === null) return t('source.dropTable.impact.unknown')
  return value ? t(`source.dropTable.impact.${kind}Hint`, { count: value }) : t(`source.dropTable.impact.${kind}None`)
}
const impacts = computed(() => [
  { key: 'data', icon: Database, tone: 'bg-danger/10 text-danger', hint: t('source.dropTable.impact.dataHint', { rows: rows.value }) },
  { key: 'structure', icon: Table2, tone: 'bg-info/10 text-info', hint: t('source.dropTable.impact.structureHint') },
  { key: 'foreign_key', icon: Link2, tone: 'bg-warning/10 text-warning', hint: dependencyHint('foreign_key') },
  { key: 'view', icon: Eye, tone: 'bg-primary/10 text-primary', hint: dependencyHint('view') },
])
</script>
