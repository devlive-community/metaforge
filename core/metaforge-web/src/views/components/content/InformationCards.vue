<template>
  <nav v-if="kind === 'linkButtonBar'" :aria-label="$t(key + 'quickLinks')" class="flex h-full flex-wrap content-start gap-2 overflow-auto p-1">
    <template v-for="item in items" :key="item.id">
      <a v-if="resourceUrl(item.value)" :href="resourceUrl(item.value)" target="_blank" rel="noopener noreferrer"
         class="inline-flex min-w-0 items-center gap-2 rounded-mf-md border border-line px-3 py-2 text-sm font-medium text-primary hover:bg-primary/8">
        <span class="break-words">{{ item.title }}</span><ArrowUpRight :size="14" class="shrink-0" aria-hidden="true"/>
      </a>
      <span v-else class="rounded-mf-md border border-line px-3 py-2 text-sm text-content-tertiary">{{ item.title }}</span>
    </template>
  </nav>
  <ul v-else-if="kind === 'noticeBoard'" class="h-full space-y-2 overflow-auto">
    <li v-for="item in items" :key="item.id">
      <MfAlert :variant="item.tone" :title="item.title"><p class="whitespace-pre-wrap break-words">{{ item.detail }}</p></MfAlert>
    </li>
  </ul>
  <ul v-else class="grid h-full auto-rows-min grid-cols-[repeat(auto-fit,minmax(min(100%,10rem),1fr))] gap-2 overflow-auto">
    <li v-for="{ item, capacity, score } in cards" :key="item.id" class="min-w-0 rounded-mf-md border border-line bg-surface p-3">
      <div v-if="kind === 'imageBadges'" class="mb-2 grid h-14 place-items-center rounded-mf-sm bg-surface-hover">
        <img v-if="resourceUrl(item.value) && !failedImages.has(item.value)" :src="resourceUrl(item.value)" alt=""
             class="h-full max-h-14 w-full object-contain p-1" loading="lazy" decoding="async" @error="failedImages.add(item.value)">
        <ImageIcon v-else :size="22" class="text-content-tertiary" aria-hidden="true"/>
      </div>
      <p class="mb-1 break-words text-sm font-semibold">{{ item.title }}</p>
      <div v-if="kind === 'comparisonCards'" class="grid grid-cols-2 gap-2">
        <div v-for="(entry, index) in [item.value, item.secondary]" :key="index" class="min-w-0 rounded-mf-sm bg-surface-hover px-2 py-1">
          <p class="text-xs text-content-tertiary">{{ $t(key + (index === 0 ? 'referenceValue' : 'comparisonValue')) }}</p>
          <p class="break-words font-semibold tabular-nums">{{ entry || '—' }}</p>
        </div>
      </div>
      <template v-else-if="kind === 'capacityGrid'">
        <template v-if="capacity">
          <p class="mb-1 text-sm tabular-nums" :class="capacity.exceeded && 'text-danger'">{{ item.value }} / {{ item.secondary }}</p>
          <ProgressMeter :value="capacity.used" :target="capacity.limit" :label="item.title"
                         :aria-valuetext="item.value + ' / ' + item.secondary"
                         :color="capacity.exceeded ? 'var(--mf-color-danger)' : undefined"/>
        </template>
        <p v-else class="text-xs text-content-tertiary">{{ $t(key + 'capacityHint') }}</p>
      </template>
      <div v-else-if="kind === 'ratingCards' && score !== undefined" class="flex flex-wrap items-center gap-1.5">
        <div class="flex gap-0.5" aria-hidden="true">
          <span v-for="star in 5" :key="star" class="relative block size-4">
            <Star :size="16" class="text-content-tertiary"/>
            <span class="absolute inset-y-0 left-0 overflow-hidden" :style="{ width: Math.max(0, Math.min(1, score - star + 1)) * 100 + '%' }">
              <Star :size="16" class="max-w-none fill-warning text-warning"/>
            </span>
          </span>
        </div>
        <span class="text-xs text-content-secondary tabular-nums">{{ $t(key + 'ratingOutOf', { rating: score }) }}</span>
      </div>
      <p v-if="item.detail" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-secondary">{{ item.detail }}</p>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { ArrowUpRight, ImageIcon, Star } from '@lucide/vue'
import { computed, reactive } from 'vue'
import { informationResourceUrl as resourceUrl, type InformationItem } from '@/model/information-block'
import { reportNumber } from '@/model/report-values'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'

const props = defineProps<{
  kind: 'linkButtonBar' | 'imageBadges' | 'comparisonCards' | 'noticeBoard' | 'capacityGrid' | 'ratingCards'
  items: readonly InformationItem[]
}>()
const key = 'informationBlock.'
const failedImages = reactive(new Set<string>())
const cards = computed(() => props.items.map(item => ({
  item, capacity: props.kind === 'capacityGrid' ? capacity(item) : undefined,
  score: props.kind === 'ratingCards' ? rating(item.value) : undefined,
})))
function capacity(item: InformationItem) {
  const used = reportNumber(item.value), limit = reportNumber(item.secondary)
  if (used !== undefined && limit !== undefined && used >= 0 && limit > 0) return { used, limit, exceeded: used > limit }
}
function rating(value: string) {
  const parsed = reportNumber(value)
  return parsed === undefined ? undefined : Math.max(0, Math.min(5, parsed))
}
</script>
