<template>
  <MfModal :open="open" width="560px" :close-disabled="saving" :close-on-overlay="!saving" :close-on-esc="!saving" @update:open="value => !value && emit('close')">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="20"/></span>
        <span>{{ $t('source.idx.dropTitle') }}</span>
      </span>
    </template>
    <div class="flex flex-col gap-4">
      <div class="flex items-start gap-3 rounded-mf-md bg-danger/10 px-4 py-3">
        <CircleAlert :size="20" class="mt-0.5 shrink-0 text-danger"/>
        <div class="min-w-0">
          <p class="m-0 break-words text-sm font-semibold text-danger">{{ $t('source.idx.dropQuestion', { name: names }) }}</p>
          <p class="m-0 mt-1 text-xs text-danger/80">{{ $t('source.idx.dropHint') }}</p>
        </div>
      </div>
      <section class="flex flex-col gap-2">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('source.idx.info') }}</h4>
        <div v-for="index in indexes" :key="index.name" class="overflow-hidden rounded-mf-md border border-line text-sm">
          <div v-for="item in infoRows(index)" :key="item.label" class="grid grid-cols-[7rem_minmax(0,1fr)] border-b border-line last:border-b-0">
            <span class="bg-surface-hover px-3 py-1.5 text-content-secondary">{{ item.label }}</span>
            <span class="break-words px-3 py-1.5" :class="item.tone || 'text-content'">{{ item.value }}</span>
          </div>
        </div>
      </section>
      <section class="flex flex-col gap-2">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('source.idx.impactTitle') }}</h4>
        <div class="flex items-start gap-2.5 rounded-mf-md bg-warning/10 px-4 py-3 text-sm">
          <CircleAlert :size="16" class="mt-0.5 shrink-0 text-warning"/>
          <div class="min-w-0 text-content-secondary">
            <p class="m-0">{{ $t('source.idx.impactLead') }}</p>
            <ul class="m-0 mt-1 flex list-disc flex-col gap-0.5 pl-5">
              <li>{{ $t('source.idx.impactFilter', { columns: impactColumns }) }}</li>
              <li>{{ $t('source.idx.impactSort') }}</li>
              <li>{{ $t('source.idx.impactScan') }}</li>
            </ul>
          </div>
        </div>
      </section>
      <MfCheckbox v-model="acknowledged" :disabled="saving">{{ $t('source.idx.dropConfirm') }}</MfCheckbox>
    </div>
    <template #footer>
      <MfButton :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="saving" :disabled="!acknowledged" @click="emit('confirm')">{{ $t('source.idx.dropAction') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleAlert, Trash2 } from '@lucide/vue'
import { columnsWithOrder, type SourceIndex } from '@/model/source-index'

const props = defineProps<{ open: boolean; indexes: SourceIndex[]; saving: boolean }>()
const emit = defineEmits<{ close: []; confirm: [] }>()
const { t } = useI18n()
const acknowledged = ref(false)
watch(() => props.open, open => { if (open) acknowledged.value = false })
const names = computed(() => props.indexes.map(index => `「${index.name}」`).join('、'))
const impactColumns = computed(() => [...new Set(props.indexes.flatMap(index => index.columns.map(column => column.name)))].join(t('source.idx.and')))
const infoRows = (index: SourceIndex) => [
  { label: t('source.idx.name'), value: index.name },
  { label: t('source.idx.method'), value: index.method },
  { label: t('source.idx.uniqueness'), value: t(index.unique ? 'common.yes' : 'common.no'), tone: index.unique ? 'text-success' : 'text-danger' },
  { label: t('source.idx.columns'), value: columnsWithOrder(index) },
  { label: t('source.idx.comment'), value: index.comment || '-' },
]
</script>
