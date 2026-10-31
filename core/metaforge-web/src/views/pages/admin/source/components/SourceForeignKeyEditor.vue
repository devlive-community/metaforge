<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'"
            :title="$t(mode === 'edit' ? 'source.fk.editTitle' : 'source.fk.createTitle')" :closable="!saving" @update:open="value => !value && emit('close')">
    <form class="flex flex-col gap-4" @submit.prevent="emit('save')">
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-name`" class="text-sm text-content">{{ $t('source.fk.name') }} <span class="text-danger">*</span></label>
        <MfInput :id="`${uid}-name`" v-model="form.name" size="sm" :disabled="saving" :invalid="!!issues.name"/>
        <p v-if="issues.name" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.fk.issue.${issues.name}`) }}</p>
      </div>
      <div class="flex flex-col gap-2">
        <span class="text-sm text-content">{{ $t('source.fk.columns') }} <span class="text-danger">*</span></span>
        <div v-for="(pair, index) in form.pairs" :key="`local-${index}`" class="flex items-center gap-2">
          <span v-if="form.pairs.length > 1" class="w-4 shrink-0 text-center text-sm tabular-nums text-content-secondary">{{ index + 1 }}</span>
          <div class="min-w-0 flex-1"><MfSelect v-model="pair.column" size="sm" searchable :options="localOptions(pair.column)" :disabled="saving" :aria-label="$t('source.fk.columns')"/></div>
          <MfButton v-if="index === 0" size="sm" :disabled="saving || form.pairs.length >= columns.length" @click="form.pairs.push({ column: '', referenced: '' })"><Plus :size="14"/>{{ $t('source.fk.addColumn') }}</MfButton>
          <button v-else type="button" class="inline-flex size-7 shrink-0 items-center justify-center rounded-mf-md text-danger hover:bg-danger/10" :disabled="saving"
                  :aria-label="$t('source.fk.removeColumn')" @click="form.pairs.splice(index, 1)"><Trash2 :size="15"/></button>
        </div>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.fk.referencedTable') }} <span class="text-danger">*</span></span>
        <MfSelect :model-value="form.referencedTable" size="sm" searchable :options="tables.map(name => ({ label: name, value: name }))" :disabled="saving" :invalid="!!issues.table"
                  :aria-label="$t('source.fk.referencedTable')" @update:model-value="value => onTable(String(value))"/>
        <p v-if="issues.table" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.fk.issue.${issues.table}`) }}</p>
      </div>
      <div class="flex flex-col gap-2">
        <span class="text-sm text-content">{{ $t('source.fk.referencedColumns') }} <span class="text-danger">*</span></span>
        <div v-for="(pair, index) in form.pairs" :key="`ref-${index}`" class="flex items-center gap-2">
          <span v-if="form.pairs.length > 1" class="w-4 shrink-0 text-center text-sm tabular-nums text-content-secondary">{{ index + 1 }}</span>
          <div class="min-w-0 flex-1"><MfSelect v-model="pair.referenced" size="sm" searchable :options="referencedOptions" :disabled="saving || !form.referencedTable" :aria-label="$t('source.fk.referencedColumns')"/></div>
        </div>
        <p v-if="issues.columns" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.fk.issue.${issues.columns}`) }}</p>
      </div>
      <div v-for="rule in (['onUpdate', 'onDelete'] as const)" :key="rule" class="flex flex-col gap-1.5">
        <span class="inline-flex items-center gap-1 text-sm text-content">{{ $t(`source.fk.${rule}`) }} <span class="text-danger">*</span>
          <MfTooltip :content="$t('source.fk.ruleHelp')"><CircleHelp :size="14" class="text-content-tertiary"/></MfTooltip>
        </span>
        <MfSelect v-model="form[rule]" size="sm" :options="ruleOptions" :disabled="saving" :aria-label="$t(`source.fk.${rule}`)"/>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm font-medium text-content">{{ $t('source.idx.generatedSql') }}</span>
        <div class="overflow-hidden rounded-mf-md bg-code-bg">
          <div class="flex items-center justify-between bg-code-header px-3 py-1.5">
            <span class="rounded-mf-sm bg-code-bg px-2 py-0.5 text-xs font-medium text-code-muted">SQL</span>
            <button type="button" class="inline-flex items-center gap-1.5 rounded-mf-sm px-2 py-1 text-xs text-code-fg hover:bg-code-bg disabled:opacity-50" :disabled="!previewSql" @click="copy"><Copy :size="13"/>{{ $t('source.executed.copy') }}</button>
          </div>
          <pre v-if="previewSql" class="m-0 whitespace-pre-wrap break-words px-3 py-2.5 font-mono text-xs leading-5 text-code-fg"><span v-for="(token, index) in tokens" :key="index" :class="CODE_CLASS[token.kind]">{{ token.text }}</span></pre>
          <p v-else class="m-0 px-3 py-2.5 text-xs text-code-muted">{{ previewError || $t('source.fk.previewHint') }}</p>
        </div>
      </div>
    </form>
    <template #footer>
      <MfButton size="sm" :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" :loading="saving" @click="emit('save')">{{ $t(mode === 'edit' ? 'common.save' : 'source.prototype.confirm') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleHelp, Copy, Plus, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import { tokenizeSql, type SqlTokenKind } from '@/model/executed-sql'
import { FOREIGN_KEY_RULES, type ForeignKeyForm, type ForeignKeyIssue } from '@/model/source-foreign-key'

const form = defineModel<ForeignKeyForm>({ required: true })
const props = defineProps<{
  open: boolean; mode: 'add' | 'edit'; columns: string[]; tables: string[]; referencedColumns: string[]
  issues: Partial<Record<'name' | 'table' | 'columns', ForeignKeyIssue>>; previewSql: string; previewError: string
  saving: boolean; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; save: [] }>()
const { t } = useI18n()
const uid = `source-fk-${getCurrentInstance()!.uid}`
/** Each row offers its own column plus the ones not used yet. */
const localOptions = (current: string) => props.columns
  .filter(name => name === current || !form.value.pairs.some(pair => pair.column === name))
  .map(name => ({ label: name, value: name }))
// The chosen columns stay listed while the referenced table's columns are still loading.
const referencedOptions = computed(() => [...new Set([...props.referencedColumns, ...form.value.pairs.map(pair => pair.referenced).filter(Boolean)])]
  .map(name => ({ label: name, value: name })))
const ruleOptions = FOREIGN_KEY_RULES.map(value => ({ label: value, value }))
/** A different referenced table invalidates the referenced columns picked so far. */
function onTable(table: string) {
  if (table === form.value.referencedTable) return
  form.value.referencedTable = table
  form.value.pairs = form.value.pairs.map(pair => ({ ...pair, referenced: '' }))
}
const CODE_CLASS: Record<SqlTokenKind, string> = {
  keyword: 'text-code-keyword', type: 'text-code-type', string: 'text-code-string', identifier: 'text-code-identifier',
  number: 'text-code-number', comment: 'text-code-muted', space: '', plain: '',
}
const tokens = computed(() => tokenizeSql(props.previewSql))
function copy() {
  ObjectUtils.copy(props.previewSql)
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
