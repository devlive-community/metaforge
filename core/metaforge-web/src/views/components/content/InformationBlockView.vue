<template>
  <MfEmpty v-if="!entries.length" size="sm" class="h-full" :title="$t(key + 'empty')"/>
  <MfTable v-else-if="tabular" fill density="compact" class="h-full" row-key="id" :columns="columns" :data-source="entries">
    <template #cell-title="{ record }"><EllipsisCell :text="record.title"/></template>
    <template #cell-value="{ record }"><EllipsisCell :text="record.value"/></template>
    <template #cell-tone="{ record }"><MfTag size="sm" :variant="record.tone">{{ $t(key + 'severity.' + record.tone) }}</MfTag></template>
    <template #cell-detail="{ record }"><EllipsisCell :text="record.detail"/></template>
  </MfTable>
  <div v-else-if="kind === 'faqList'" class="flex h-full flex-col gap-2 overflow-auto">
    <details v-for="item in entries" :key="item.id" class="rounded-mf-md border border-line bg-surface">
      <summary class="cursor-pointer break-words px-3 py-2 text-sm font-semibold">{{ item.title }}</summary>
      <p class="whitespace-pre-wrap break-words px-3 pb-3 text-sm text-content-secondary">{{ item.detail }}</p>
    </details>
  </div>
  <ol v-else-if="kind === 'releaseTimeline'" class="h-full space-y-3 overflow-auto p-1">
    <li v-for="item in entries" :key="item.id" class="border-l-2 border-primary/45 py-0.5 pl-3">
      <span v-if="item.value" class="block text-xs text-content-tertiary">{{ item.value }}</span>
      <p class="break-words text-sm font-semibold">{{ item.title }}</p>
      <p v-if="item.detail" class="whitespace-pre-wrap break-words text-sm text-content-secondary">{{ item.detail }}</p>
    </li>
  </ol>
  <dl v-else-if="kind === 'statTiles'" class="grid h-full auto-rows-min grid-cols-[repeat(auto-fit,minmax(min(100%,9rem),1fr))] gap-2 overflow-auto">
    <div v-for="item in entries" :key="item.id" class="min-w-0 rounded-mf-md border border-line bg-surface-hover px-3 py-2">
      <dt class="break-words text-xs text-content-secondary">{{ item.title }}</dt>
      <dd class="break-words text-xl font-bold tabular-nums">{{ item.value || '—' }}</dd>
      <dd v-if="item.detail" class="whitespace-pre-wrap break-words text-xs text-content-tertiary">{{ item.detail }}</dd>
    </div>
  </dl>
  <ul v-else class="grid h-full auto-rows-min gap-2 overflow-auto"
      :class="kind !== 'serviceStatus' && 'grid-cols-[repeat(auto-fit,minmax(min(100%,10rem),1fr))]'">
    <li v-for="item in entries" :key="item.id" class="min-w-0 rounded-mf-md border border-line bg-surface px-3 py-2">
      <div class="flex flex-wrap items-center justify-between gap-1">
        <strong class="break-all text-sm">{{ item.title }}</strong>
        <MfTag v-if="kind === 'serviceStatus'" size="sm" :variant="item.tone">{{ $t(key + 'status.' + item.tone) }}</MfTag>
      </div>
      <p v-if="item.detail" class="whitespace-pre-wrap break-words text-xs text-content-secondary">{{ item.detail }}</p>
      <a v-if="kind === 'resourceCards' && resourceUrl(item.value)" :href="resourceUrl(item.value)"
         target="_blank" rel="noopener noreferrer" class="mt-1 inline-flex items-center gap-1 text-xs text-primary hover:underline">
        {{ $t(key + 'openResource') }}<ArrowUpRight :size="13"/>
      </a>
      <a v-else-if="kind === 'contactDirectory' && mailUrl(item.value)" :href="mailUrl(item.value)" class="block break-all text-xs text-primary hover:underline">{{ item.value }}</a>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { ArrowUpRight } from '@lucide/vue'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { informationFields, type InformationBlockKind, type InformationItem } from '@/model/information-block'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'

const props = defineProps<{ kind: InformationBlockKind; items: readonly InformationItem[] }>()
const { t } = useI18n()
const key = 'informationBlock.'
const entries = computed(() => props.items.filter(item => item.title.trim()))
const tabular = computed(() => props.kind === 'riskRegister' || props.kind === 'shiftRoster')
const columns = computed(() => Object.entries(informationFields[props.kind]).map(([dataIndex, label]) => ({ dataIndex, title: t(key + label) })))
function resourceUrl(value: string) {
  try {
    const url = new URL(value)
    if (url.protocol === 'https:' || url.protocol === 'http:') return url.href
  } catch { /* The resource becomes a link when the address is valid. */ }
  return ''
}
function mailUrl(value: string) {
  const address = value.trim()
  return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(address)
    ? 'mailto:' + encodeURIComponent(address).replace('%40', '@') : ''
}
</script>
