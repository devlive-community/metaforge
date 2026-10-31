<template>
  <MfModal :open="open" width="560px" :close-disabled="saving" :close-on-overlay="!saving" :close-on-esc="!saving" @update:open="value => !value && emit('close')">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="20"/></span>
        <span>{{ $t('source.fk.dropTitle') }}</span>
      </span>
    </template>
    <div class="flex flex-col gap-4">
      <div class="flex items-start gap-3 rounded-mf-md bg-danger/10 px-4 py-3">
        <CircleAlert :size="20" class="mt-0.5 shrink-0 text-danger"/>
        <div class="min-w-0">
          <p class="m-0 break-words text-sm font-semibold text-danger">{{ $t('source.fk.dropQuestion', { name: names }) }}</p>
          <p class="m-0 mt-1 text-xs text-danger/80">{{ $t('source.fk.dropHint') }}</p>
        </div>
      </div>
      <section class="flex flex-col gap-2">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('source.fk.info') }}</h4>
        <div v-for="key in keys" :key="key.name" class="overflow-hidden rounded-mf-md border border-line text-sm">
          <div v-for="item in infoRows(key)" :key="item.label" class="grid grid-cols-[7rem_minmax(0,1fr)] border-b border-line last:border-b-0">
            <span class="bg-surface-hover px-3 py-1.5 text-content-secondary">{{ item.label }}</span>
            <span class="break-words px-3 py-1.5 text-content">{{ item.value }}</span>
          </div>
        </div>
      </section>
      <section class="flex flex-col gap-2">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('source.idx.impactTitle') }}</h4>
        <div class="flex items-start gap-2.5 rounded-mf-md bg-warning/10 px-4 py-3 text-sm">
          <CircleAlert :size="16" class="mt-0.5 shrink-0 text-warning"/>
          <ul class="m-0 flex list-disc flex-col gap-0.5 pl-4 text-content-secondary">
            <li>{{ $t('source.fk.impactCheck') }}</li>
            <li>{{ $t('source.fk.impactRules') }}</li>
            <li>{{ $t('source.fk.impactData') }}</li>
          </ul>
        </div>
      </section>
      <MfCheckbox v-model="acknowledged" :disabled="saving">{{ $t('source.fk.dropConfirm') }}</MfCheckbox>
    </div>
    <template #footer>
      <MfButton :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="saving" :disabled="!acknowledged" @click="emit('confirm')">{{ $t('source.fk.dropAction') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleAlert, Trash2 } from '@lucide/vue'
import type { SourceForeignKey } from '@/model/source-foreign-key'

const props = defineProps<{ open: boolean; keys: SourceForeignKey[]; saving: boolean }>()
const emit = defineEmits<{ close: []; confirm: [] }>()
const { t } = useI18n()
const acknowledged = ref(false)
watch(() => props.open, open => { if (open) acknowledged.value = false })
const names = computed(() => props.keys.map(key => `「${key.name}」`).join('、'))
const infoRows = (key: SourceForeignKey) => [
  { label: t('source.fk.name'), value: key.name },
  { label: t('source.fk.columns'), value: key.columns.join(', ') },
  { label: t('source.fk.referencedTable'), value: key.referencedTable },
  { label: t('source.fk.referencedColumns'), value: key.referencedColumns.join(', ') },
  { label: t('source.fk.onUpdate'), value: key.onUpdate },
  { label: t('source.fk.onDelete'), value: key.onDelete },
]
</script>
