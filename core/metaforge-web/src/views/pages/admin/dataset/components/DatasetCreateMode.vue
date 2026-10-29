<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleCheck, Code2, Plus, Table2 } from '@lucide/vue'
import type { DatasetCreationEntry } from '@/model/dataset-editor'
const props = defineProps<{ open: boolean, result: DatasetCreationEntry | null, sql: DatasetCreationEntry | null }>()
const emit = defineEmits<{ close: [], create: [entry: DatasetCreationEntry] }>()
const { t } = useI18n()
const selected = ref<'result' | 'sql' | 'manual'>('result')
watch(() => props.open, value => { if (value) selected.value = props.result ? 'result' : props.sql ? 'sql' : 'manual' }, { immediate: true })
const choices = computed(() => [
  { key:'result' as const, title:t('dataset.prototype.resultCreation'), hint:t('dataset.prototype.resultCreationHint'), icon:Table2, enabled:!!props.result },
  { key:'sql' as const, title:t('dataset.prototype.sqlCreation'), hint:t('dataset.prototype.sqlCreationHint'), icon:Code2, enabled:!!props.sql },
  { key:'manual' as const, title:t('dataset.prototype.manualCreation'), hint:t('dataset.prototype.manualModeHint'), icon:Plus, enabled:true }
])
function create() {
  const entry = selected.value === 'result' ? props.result : selected.value === 'sql' ? props.sql : { origin:'manual' as const }
  if (entry) emit('create', entry)
}
</script>
<template>
  <MfModal :open="open" :title="t('dataset.prototype.chooseCreation')" :subtitle="t('dataset.prototype.chooseCreationHint')" width="660px" trap-focus @close="emit('close')">
    <div class="space-y-3 py-2">
      <button v-for="choice in choices" :key="choice.key" type="button" class="flex w-full items-start gap-3 rounded-mf-lg border p-4 text-left disabled:cursor-not-allowed disabled:opacity-40" :class="selected === choice.key ? 'border-primary/40 bg-primary/5' : 'border-line hover:bg-surface-hover'" :disabled="!choice.enabled" :aria-pressed="selected === choice.key" @click="selected = choice.key">
        <span class="flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><component :is="choice.icon" :size="20"/></span><span class="min-w-0 flex-1"><span class="block text-sm font-semibold">{{ choice.title }}</span><span class="mt-1 block text-xs leading-5 text-content-tertiary">{{ choice.hint }}</span><MfTag v-if="choice.key === 'result' && result" variant="primary" size="sm" class="mt-2">{{ t('dataset.prototype.resultSize', [result.rowCount || 0, result.headers?.length || 0]) }}</MfTag></span><CircleCheck v-if="selected === choice.key" :size="18" class="shrink-0 text-primary"/>
      </button>
    </div>
    <template #footer><div class="flex w-full flex-wrap items-center justify-between gap-3"><p class="max-w-sm text-xs text-content-tertiary">{{ t('dataset.prototype.creationCarry') }}</p><div class="ml-auto flex gap-2"><MfButton @click="emit('close')">{{ t('common.cancel') }}</MfButton><MfButton variant="primary" @click="create">{{ t('dataset.prototype.continueCreation') }}<span aria-hidden="true">→</span></MfButton></div></div></template>
  </MfModal>
</template>
