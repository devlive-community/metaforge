<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'"
            :title="$t(mode === 'edit' ? 'source.idx.editTitle' : 'source.idx.createTitle')" :closable="!saving" @update:open="value => !value && emit('close')">
    <form class="flex flex-col gap-4" @submit.prevent="emit('save')">
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-name`" class="text-sm text-content">{{ $t('source.idx.name') }} <span class="text-danger">*</span></label>
        <MfInput :id="`${uid}-name`" v-model="form.name" size="sm" :disabled="saving || primary" :invalid="!!issues.name"/>
        <p v-if="issues.name" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.idx.issue.${issues.name}`) }}</p>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="inline-flex items-center gap-1 text-sm text-content">{{ $t('source.idx.method') }}
          <MfTooltip :content="$t('source.idx.methodHelp')"><CircleHelp :size="14" class="text-content-tertiary"/></MfTooltip>
        </span>
        <MfSelect v-model="form.method" size="sm" :options="methodOptions" :disabled="saving || primary" :aria-label="$t('source.idx.method')"/>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.idx.uniqueness') }}</span>
        <MfRadioGroup :model-value="form.unique && ordered ? 'unique' : 'normal'" :disabled="saving || primary || !ordered"
                      @update:model-value="value => form.unique = value === 'unique'">
          <MfRadio value="normal" :label="$t('source.idx.notUnique')"/>
          <MfRadio value="unique" :label="$t('source.idx.unique')"/>
        </MfRadioGroup>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.idx.algorithm') }}</span>
        <!-- MfSelect reads an empty value as "nothing chosen", so the default option uses its own value. -->
        <MfSelect :model-value="form.algorithm || 'DEFAULT'" size="sm" :options="algorithmOptions" :disabled="saving" :aria-label="$t('source.idx.algorithm')"
                  @update:model-value="value => form.algorithm = value === 'DEFAULT' ? '' : value as IndexForm['algorithm']"/>
      </div>
      <div class="flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <span class="text-sm text-content">{{ $t('source.idx.columns') }} <span class="text-danger">*</span></span>
          <MfButton size="sm" :disabled="saving || form.columns.length >= columns.length" @click="addColumn"><Plus :size="14"/>{{ $t('source.idx.addColumn') }}</MfButton>
        </div>
        <div v-for="(column, index) in form.columns" :key="index" class="grid grid-cols-[1rem_minmax(0,1fr)_minmax(0,1fr)_1.75rem] items-center gap-2">
          <span class="text-center text-sm tabular-nums text-content-secondary">{{ index + 1 }}</span>
          <div class="min-w-0"><MfSelect v-model="column.name" size="sm" searchable :options="columnOptions(column.name)" :disabled="saving" :aria-label="$t('source.idx.column')"/></div>
          <div class="min-w-0"><MfSelect v-model="column.order" size="sm" :options="orderOptions" :disabled="saving || !ordered" :aria-label="$t('source.idx.order')"/></div>
          <button type="button" class="inline-flex size-7 shrink-0 items-center justify-center rounded-mf-md text-danger hover:bg-danger/10 disabled:opacity-40"
                  :disabled="saving || form.columns.length === 1" :aria-label="$t('source.idx.removeColumn')" @click="form.columns.splice(index, 1)"><Trash2 :size="15"/></button>
        </div>
        <p v-if="issues.columns" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.idx.issue.${issues.columns}`) }}</p>
      </div>
      <div class="border-t border-line pt-3">
        <button type="button" class="flex w-full items-center justify-between text-sm font-medium text-content" :aria-expanded="advanced" @click="advanced = !advanced">
          {{ $t('source.idx.advanced') }}<ChevronDown :size="14" :class="advanced && 'rotate-180'"/>
        </button>
        <div v-show="advanced" class="mt-3 flex flex-col gap-1.5">
          <label :for="`${uid}-comment`" class="text-sm text-content">{{ $t('source.idx.comment') }}</label>
          <MfTextarea :id="`${uid}-comment`" v-model="form.comment" :rows="3" :maxlength="500" :disabled="saving"/>
          <span class="self-end text-xs text-success">{{ form.comment.length }} / 500</span>
        </div>
      </div>
      <div class="flex flex-col gap-1.5 border-t border-line pt-3">
        <span class="text-sm font-medium text-content">{{ $t('source.idx.generatedSql') }}</span>
        <div class="relative rounded-mf-md border border-line bg-surface-hover/60 px-3 py-2.5">
          <button v-if="previewSql" type="button" class="absolute right-2 top-2 inline-flex items-center gap-1 rounded-mf-sm px-1.5 py-0.5 text-xs text-content-secondary hover:bg-surface-hover"
                  @click="copy"><Copy :size="12"/>{{ $t('source.executed.copy') }}</button>
          <pre v-if="previewSql" class="m-0 whitespace-pre-wrap break-words pr-12 font-mono text-xs leading-5 text-content"><span v-for="(token, index) in tokens" :key="index" :class="TOKEN_CLASS[token.kind]">{{ token.text }}</span></pre>
          <p v-else class="m-0 text-xs text-content-tertiary">{{ previewError || $t('source.idx.previewHint') }}</p>
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
import { computed, getCurrentInstance, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown, CircleHelp, Copy, Plus, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { ObjectUtils } from '@/utils/object'
import { tokenizeSql, type SqlTokenKind } from '@/model/executed-sql'
import { INDEX_METHODS, orderedMethod, type IndexForm, type IndexIssue } from '@/model/source-index'

const form = defineModel<IndexForm>({ required: true })
const props = defineProps<{
  open: boolean; mode: 'add' | 'edit'; columns: string[]; issues: Partial<Record<'name' | 'columns', IndexIssue>>
  previewSql: string; previewError: string; saving: boolean; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; save: [] }>()
const { t } = useI18n()
const uid = `source-index-${getCurrentInstance()!.uid}`
const advanced = ref(true)
const primary = computed(() => props.mode === 'edit' && form.value.name === 'PRIMARY')
const ordered = computed(() => orderedMethod(form.value.method))
const methodOptions = INDEX_METHODS.map(value => ({ label: value, value }))
const algorithmOptions = computed(() => [
  { label: t('source.idx.algorithmDefault'), value: 'DEFAULT' }, { label: 'INPLACE', value: 'INPLACE' }, { label: 'COPY', value: 'COPY' },
])
const orderOptions = computed(() => [{ label: t('source.idx.asc'), value: 'ASC' }, { label: t('source.idx.desc'), value: 'DESC' }])
/** Each row offers its own column plus the ones not used yet. */
const columnOptions = (current: string) => props.columns
  .filter(name => name === current || !form.value.columns.some(column => column.name === name))
  .map(name => ({ label: name, value: name }))
function addColumn() {
  const name = props.columns.find(column => !form.value.columns.some(item => item.name === column)) || ''
  form.value.columns.push({ name, order: 'ASC', subPart: null })
}
// Light code block: semantic colors instead of the dark-block syntax tokens.
const TOKEN_CLASS: Record<SqlTokenKind, string> = {
  keyword: 'text-info', type: 'text-data-complex', string: 'text-success', identifier: 'text-warning', number: 'text-warning', comment: 'text-content-tertiary', space: '', plain: '',
}
const tokens = computed(() => tokenizeSql(props.previewSql))
function copy() {
  ObjectUtils.copy(props.previewSql)
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
