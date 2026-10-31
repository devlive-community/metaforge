<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="$t('source.props.edit')" :closable="!saving"
            @update:open="value => !value && emit('close')">
    <form class="flex flex-col gap-4" @submit.prevent="emit('save')">
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.props.name') }}</span>
        <MfInput :model-value="table" size="sm" disabled :aria-label="$t('source.props.name')"/>
        <p class="m-0 text-xs text-content-tertiary">{{ $t('source.props.nameHint') }}</p>
      </div>
      <div v-for="field in selects" :key="field.key" class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t(field.label) }} <span class="text-danger">*</span></span>
        <MfSelect :model-value="form[field.key]" size="sm" :searchable="field.searchable" :options="field.options.map(value => ({ label: value, value }))"
                  :disabled="saving" :invalid="!!issues[field.key]" :aria-label="$t(field.label)" @update:model-value="value => field.change(String(value))"/>
        <p v-if="issues[field.key]" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.props.issue.${issues[field.key]}`) }}</p>
      </div>
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-auto`" class="text-sm text-content">{{ $t('source.props.nextAutoIncrement') }}</label>
        <MfInput :id="`${uid}-auto`" v-model="form.autoIncrement" size="sm" inputmode="numeric" :disabled="saving" :invalid="!!issues.autoIncrement"/>
        <p v-if="issues.autoIncrement" role="alert" class="m-0 text-xs text-danger">{{ $t(`source.props.issue.${issues.autoIncrement}`) }}</p>
        <p v-else class="m-0 text-xs text-content-tertiary">{{ $t('source.props.nextAutoIncrementHint') }}</p>
      </div>
      <div class="flex flex-col gap-1.5">
        <label :for="`${uid}-comment`" class="text-sm text-content">{{ $t('source.props.comment') }}</label>
        <MfTextarea :id="`${uid}-comment`" ref="comment" v-model="form.comment" :rows="4" :maxlength="500" :disabled="saving"/>
        <span class="self-end text-xs text-success">{{ form.comment.length }} / 500</span>
      </div>
      <div class="flex items-start gap-2 rounded-mf-md border border-warning/30 bg-warning/10 px-3 py-2.5 text-xs text-warning">
        <CircleAlert :size="15" class="mt-px shrink-0"/>
        <ol class="m-0 flex list-decimal flex-col gap-0.5 pl-4">
          <li>{{ $t('source.props.warnEngine') }}</li>
          <li>{{ $t('source.props.warnCharset') }}</li>
        </ol>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm font-medium text-content">{{ $t('source.idx.generatedSql') }}</span>
        <SqlCodeBlock :code="previewSql" :placeholder="previewError || $t('source.props.noChange')" compact max-height="max-h-48"/>
      </div>
    </form>
    <template #footer>
      <MfButton size="sm" :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" :loading="saving" @click="emit('save')">{{ $t('source.props.save') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, ref, watch } from 'vue'
import { CircleAlert } from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import { ROW_FORMATS, type PropertiesForm, type PropertiesIssue } from '@/model/source-table-properties'

const form = defineModel<PropertiesForm>({ required: true })
const props = defineProps<{
  open: boolean; table: string; engines: string[]; charsets: string[]; collations: string[]; focusComment: boolean
  issues: Partial<Record<keyof PropertiesForm, PropertiesIssue>>; previewSql: string; previewError: string
  saving: boolean; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; save: []; charset: [charset: string] }>()
const uid = `source-props-${getCurrentInstance()!.uid}`
const comment = ref<{ $el?: HTMLElement } | null>(null)
/** Keeps the table's current value listed even before the choices have loaded. */
const withCurrent = (list: string[], current: string) => current && !list.some(item => item.toLowerCase() === current.toLowerCase()) ? [current, ...list] : list
const selects = computed(() => [
  { key: 'engine' as const, label: 'source.props.engine', searchable: false, options: withCurrent(props.engines, form.value.engine), change: (value: string) => { form.value.engine = value } },
  { key: 'rowFormat' as const, label: 'source.props.rowFormat', searchable: false, options: withCurrent(ROW_FORMATS, form.value.rowFormat), change: (value: string) => { form.value.rowFormat = value } },
  { key: 'charset' as const, label: 'source.props.charset', searchable: true, options: withCurrent(props.charsets, form.value.charset), change: (value: string) => emit('charset', value) },
  { key: 'collation' as const, label: 'source.props.collation', searchable: true, options: withCurrent(props.collations, form.value.collation), change: (value: string) => { form.value.collation = value } },
])
// Editing from the comment card goes straight to the comment.
watch(() => [props.open, props.focusComment], ([open, focus]) => {
  if (open && focus) nextTick(() => (comment.value?.$el?.querySelector?.('textarea') as HTMLTextAreaElement | null)?.focus())
}, { immediate: true })
</script>
