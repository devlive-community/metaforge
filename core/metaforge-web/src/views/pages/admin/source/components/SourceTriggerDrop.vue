<template>
  <MfModal :open="open" width="560px" :close-disabled="saving" :close-on-overlay="!saving" :close-on-esc="!saving" @update:open="value => !value && emit('close')">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="20"/></span>
        <span>{{ $t('source.trg.dropTitle') }}</span>
      </span>
    </template>
    <div class="flex flex-col gap-4">
      <div class="flex items-start gap-3 rounded-mf-md bg-danger/10 px-4 py-3">
        <CircleAlert :size="20" class="mt-0.5 shrink-0 text-danger"/>
        <div class="min-w-0">
          <p class="m-0 break-words text-sm font-semibold text-danger">{{ $t('source.trg.dropQuestion', { name: names }) }}</p>
          <p class="m-0 mt-1 text-xs text-danger/80">{{ $t('source.trg.dropHint') }}</p>
        </div>
      </div>
      <section class="flex flex-col gap-2">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('source.trg.info') }}</h4>
        <div v-for="trigger in triggers" :key="trigger.name" class="overflow-hidden rounded-mf-md border border-line text-sm">
          <div v-for="item in infoRows(trigger)" :key="item.label" class="grid grid-cols-[7rem_minmax(0,1fr)] border-b border-line last:border-b-0">
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
            <li>{{ $t('source.trg.impactRun', { events: events }) }}</li>
            <li>{{ $t('source.trg.impactData') }}</li>
            <li>{{ $t('source.trg.impactExisting') }}</li>
          </ul>
        </div>
      </section>
      <MfCheckbox v-model="acknowledged" :disabled="saving">{{ $t('source.trg.dropConfirm') }}</MfCheckbox>
    </div>
    <template #footer>
      <MfButton :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="saving" :disabled="!acknowledged" @click="emit('confirm')">{{ $t('source.trg.dropAction') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleAlert, Trash2 } from '@lucide/vue'
import type { SourceTrigger } from '@/model/source-trigger'

const props = defineProps<{ open: boolean; triggers: SourceTrigger[]; saving: boolean }>()
const emit = defineEmits<{ close: []; confirm: [] }>()
const { t } = useI18n()
const acknowledged = ref(false)
watch(() => props.open, open => { if (open) acknowledged.value = false })
const names = computed(() => props.triggers.map(trigger => `「${trigger.name}」`).join('、'))
const events = computed(() => [...new Set(props.triggers.map(trigger => trigger.event))].join(' / '))
const infoRows = (trigger: SourceTrigger) => [
  { label: t('source.trg.name'), value: trigger.name },
  { label: t('source.trg.timing'), value: trigger.timing },
  { label: t('source.trg.event'), value: trigger.event },
  { label: t('source.trg.definer'), value: trigger.definer || '-' },
  { label: t('source.trg.created'), value: trigger.created || '-' },
]
</script>
