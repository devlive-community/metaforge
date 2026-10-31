<template>
  <MfModal :open="open" :width="batch ? '560px' : '500px'" :close-disabled="saving" :close-on-overlay="!saving" :close-on-esc="!saving"
           @update:open="value => !value && emit('close')">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="20"/></span>
        <span>{{ $t(batch ? 'source.drop.batchTitle' : 'source.drop.rowTitle') }}</span>
      </span>
    </template>
    <div class="flex flex-col gap-4 pl-13">
      <div>
        <p class="m-0 text-base font-semibold text-content">{{ batch ? $t('source.drop.batchQuestion', { count: rows.length }) : $t('source.drop.rowQuestion') }}</p>
        <p class="m-0 mt-1.5 text-sm text-content-tertiary">{{ $t('source.drop.irreversible') }}</p>
      </div>
      <dl v-if="!batch" class="m-0 grid grid-cols-[auto_minmax(0,1fr)] gap-x-8 gap-y-2 rounded-mf-md border border-line bg-surface-hover/60 px-5 py-4 text-sm">
        <template v-for="name in preview" :key="name">
          <dt class="text-content-secondary">{{ name }}</dt>
          <dd class="m-0 truncate text-content" :title="display(rows[0]?.[name])">{{ display(rows[0]?.[name]) }}</dd>
        </template>
      </dl>
      <template v-else>
        <div class="rounded-mf-md bg-danger/5 px-4 py-3 text-sm">
          <p class="m-0 inline-flex items-center gap-2 font-medium text-danger"><CircleAlert :size="16"/>{{ $t('source.drop.recordsTitle') }}</p>
          <ul class="m-0 mt-2 flex max-h-48 list-none flex-col gap-1.5 overflow-y-auto p-0 pl-6 text-content-secondary">
            <li v-for="(row, index) in rows" :key="index" class="grid grid-cols-[minmax(0,10rem)_minmax(0,1fr)] gap-4">
              <span class="truncate">{{ keyColumn }}: {{ display(row[keyColumn]) }}</span>
              <span v-if="labelColumn" class="truncate" :title="display(row[labelColumn])">{{ display(row[labelColumn]) }}</span>
            </li>
          </ul>
        </div>
        <MfCheckbox v-model="acknowledged" :disabled="saving">{{ $t('source.drop.batchConfirm') }}</MfCheckbox>
      </template>
    </div>
    <template #footer>
      <MfButton :disabled="saving" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="saving" :disabled="batch && !acknowledged" @click="emit('confirm')">
        {{ batch ? $t('source.drop.batchDelete', { count: rows.length }) : $t('common.delete') }}
      </MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CircleAlert, Trash2 } from '@lucide/vue'
import type { DataField } from '@/model/source-data'

const props = defineProps<{ open: boolean; rows: Record<string, unknown>[]; fields: DataField[]; columns: string[]; primaryKeys: string[]; saving: boolean }>()
const emit = defineEmits<{ close: []; confirm: [] }>()
const acknowledged = ref(false)
watch(() => props.open, open => { if (open) acknowledged.value = false })
const batch = computed(() => props.rows.length > 1)
/** A single row shows its first visible columns; the batch list shows the key and one readable column. */
const preview = computed(() => props.columns.slice(0, 6))
const keyColumn = computed(() => props.primaryKeys.find(name => props.columns.includes(name)) || props.columns[0] || '')
const labelColumn = computed(() => props.fields.find(field => field.name !== keyColumn.value && field.kind === 'text' && props.columns.includes(field.name))?.name
    || props.columns.find(name => name !== keyColumn.value) || '')
const display = (value: unknown) => value === null || value === undefined ? 'NULL' : typeof value === 'object' ? JSON.stringify(value) : String(value)
</script>
