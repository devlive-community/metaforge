<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="$t('source.database.edit')" :closable="!saving"
            @update:open="value => !value && emit('close')">
    <form class="flex flex-col gap-4" @submit.prevent="emit('save')">
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.database.name') }}</span>
        <MfInput :model-value="database" size="sm" disabled :aria-label="$t('source.database.name')"/>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.database.charset') }} <span class="text-danger">*</span></span>
        <MfSelect :model-value="form.charset" size="sm" searchable :options="withCurrent(charsets, form.charset).map(value => ({ label: value, value }))"
                  :disabled="saving" :aria-label="$t('source.database.charset')" @update:model-value="value => emit('charset', String(value))"/>
      </div>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content">{{ $t('source.database.collation') }} <span class="text-danger">*</span></span>
        <MfSelect v-model="form.collation" size="sm" searchable :options="withCurrent(collations, form.collation).map(value => ({ label: value, value }))"
                  :disabled="saving" :aria-label="$t('source.database.collation')"/>
      </div>
      <div class="flex items-start gap-2 rounded-mf-md border border-warning/30 bg-warning/10 px-3 py-2.5 text-xs text-warning">
        <CircleAlert :size="15" class="mt-px shrink-0"/>{{ $t('source.database.editWarning') }}
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
import { CircleAlert } from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'

const form = defineModel<{ charset: string; collation: string }>({ required: true })
defineProps<{
  open: boolean; database: string; charsets: string[]; collations: string[]; previewSql: string; previewError: string
  saving: boolean; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; save: []; charset: [charset: string] }>()
/** Keeps the current value listed even before the choices have loaded. */
const withCurrent = (list: string[], current: string) => current && !list.includes(current) ? [current, ...list] : list
</script>
