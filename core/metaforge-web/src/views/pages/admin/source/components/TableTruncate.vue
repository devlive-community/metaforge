<template>
  <MfModal v-model:open="visible" width="1080px" height="min(760px, 90vh)" :close-disabled="submitting" :close-on-overlay="!submitting" :close-on-esc="!submitting">
    <template #title>
      <span class="flex items-center gap-4">
        <span class="inline-flex size-12 shrink-0 items-center justify-center rounded-mf-md bg-danger/10 text-danger"><Trash2 :size="24"/></span>
        <span class="flex min-w-0 flex-col">
          <span class="truncate text-lg">{{ $t('source.common.truncateTable') }}</span>
          <span class="mt-0.5 truncate text-sm font-normal text-content-tertiary">{{ $t('source.truncate.subtitle') }}</span>
        </span>
      </span>
    </template>

    <div v-if="loading" class="flex h-full min-h-80 items-center justify-center"><MfSpinner/></div>
    <div v-else class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <div class="flex min-w-0 flex-col gap-4">
        <section class="rounded-mf-md bg-surface-hover/60 p-4">
          <h4 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><Table2 :size="18"/>{{ $t('source.truncate.tableInfo') }}</h4>
          <dl class="m-0 grid grid-cols-[8rem_minmax(0,1fr)] gap-y-2.5 text-sm">
            <dt class="text-content-secondary">{{ $t('source.truncate.name') }}</dt><dd class="m-0 break-all text-content">{{ info?.name }}</dd>
            <dt class="text-content-secondary">{{ $t('source.truncate.database') }}</dt><dd class="m-0 break-all text-content">{{ info?.database }}</dd>
            <dt class="text-content-secondary">{{ $t('source.truncate.engine') }}</dt><dd class="m-0 text-content">{{ info?.engine }}</dd>
            <dt class="text-content-secondary">{{ $t('source.truncate.rows') }}</dt>
            <dd class="m-0 flex items-center gap-1.5 tabular-nums text-content">
              {{ info?.rows === null || info?.rows === undefined ? '-' : info.rows.toLocaleString() }}
              <MfTooltip :content="$t('source.truncate.rowsHint')"><Info :size="14" class="text-content-tertiary"/></MfTooltip>
            </dd>
          </dl>
        </section>

        <section>
          <h4 class="m-0 mb-2 flex items-center gap-2 text-base font-semibold text-content"><Info :size="18" class="text-info"/>{{ $t('source.truncate.about') }}</h4>
          <ul class="m-0 flex list-disc flex-col gap-1.5 rounded-mf-md bg-info/10 py-3 pl-9 pr-4 text-sm text-content-secondary">
            <li v-for="index in 4" :key="index">{{ $t(`source.truncate.aboutItem${index}`) }}</li>
          </ul>
        </section>

        <section class="rounded-mf-md bg-warning/10 p-4">
          <h4 class="m-0 mb-2 flex items-center gap-2 text-base font-semibold text-warning"><TriangleAlert :size="18"/>{{ $t('source.truncate.caution') }}</h4>
          <ul class="m-0 flex list-disc flex-col gap-1.5 pl-5 text-sm text-content-secondary">
            <li v-for="index in 4" :key="index">{{ $t(`source.truncate.cautionItem${index}`) }}</li>
          </ul>
        </section>

        <section>
          <h4 class="m-0 mb-3 flex items-center gap-2 text-base font-semibold text-content"><CircleCheck :size="18" class="text-success"/>{{ $t('source.truncate.confirm') }}</h4>
          <div class="flex flex-col gap-3 pl-6">
            <MfCheckbox v-model="acknowledged" :disabled="submitting">{{ $t('source.truncate.acknowledge', { table }) }}</MfCheckbox>
            <div class="flex flex-col gap-1.5">
              <label :for="`${uid}-name`" class="text-sm text-danger">{{ $t('source.truncate.typeName', { table }) }} *</label>
              <MfInput :id="`${uid}-name`" v-model="typedName" :disabled="submitting" :invalid="!!typedName && !nameMatches" autocomplete="off" spellcheck="false">
                <template v-if="nameMatches" #suffix><Check :size="16" class="text-success"/></template>
              </MfInput>
            </div>
          </div>
        </section>
      </div>

      <div class="flex min-w-0 flex-col gap-4">
        <section class="rounded-mf-md border border-line p-4">
          <div class="mb-3 flex items-start justify-between gap-3">
            <div class="flex min-w-0 gap-3">
              <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-full bg-surface-hover text-content-secondary"><CodeXml :size="16"/></span>
              <div class="min-w-0">
                <h4 class="m-0 text-base font-semibold text-content">{{ $t('source.truncate.sql') }}</h4>
                <p class="m-0 mt-0.5 text-xs text-content-tertiary">{{ $t('source.truncate.sqlHint') }}</p>
              </div>
            </div>
            <MfButton size="sm" :disabled="!statement" @click="copy"><ClipboardCopy :size="14"/>{{ $t('source.truncate.copySql') }}</MfButton>
          </div>
          <SqlCodeBlock :code="statement" :copyable="false" :placeholder="loadError" max-height="max-h-40"/>
        </section>

        <section>
          <h4 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.truncate.impactTitle') }}</h4>
          <ul class="m-0 flex list-none flex-col gap-2 p-0">
            <li v-for="item in IMPACTS" :key="item.key" class="flex items-center gap-3 rounded-mf-md bg-surface-hover/60 px-3 py-2.5">
              <span class="inline-flex size-9 shrink-0 items-center justify-center rounded-mf-md" :class="item.tone"><component :is="item.icon" :size="18"/></span>
              <span class="flex min-w-0 flex-col">
                <span class="text-sm font-medium text-content">{{ $t(`source.truncate.impact.${item.key}`) }}</span>
                <span class="text-xs text-content-secondary">{{ $t(`source.truncate.impact.${item.key}Hint`) }}</span>
              </span>
            </li>
          </ul>
        </section>
      </div>
    </div>

    <template #footer>
      <MfButton :disabled="submitting" @click="visible = false">{{ $t('common.cancel') }}</MfButton>
      <MfButton variant="danger" :loading="submitting" :disabled="!canSubmit" @click="submit"><Trash2 :size="15"/>{{ $t('source.truncate.submit') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance } from 'vue'
import { Check, CircleCheck, ClipboardCopy, CodeXml, Database, Info, Link2, Table2, TriangleAlert, Trash2, Wrench, Zap } from '@lucide/vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import { useTableTruncate } from '@/composables/useTableTruncate'

defineOptions({ name: 'TableTruncate' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})
const uid = `table-truncate-${getCurrentInstance()!.uid}`
const { loading, submitting, info, statement, loadError, acknowledged, typedName, table, nameMatches, canSubmit, copy, submit } = useTableTruncate(() => { visible.value = false })
const IMPACTS = [
  { key: 'data', icon: Database, tone: 'bg-success/10 text-success' },
  { key: 'autoIncrement', icon: Wrench, tone: 'bg-info/10 text-info' },
  { key: 'structure', icon: Table2, tone: 'bg-surface-hover text-content-secondary' },
  { key: 'performance', icon: Zap, tone: 'bg-success/10 text-success' },
  { key: 'transaction', icon: TriangleAlert, tone: 'bg-danger/10 text-danger' },
  { key: 'foreignKey', icon: Link2, tone: 'bg-warning/10 text-warning' },
]
</script>
