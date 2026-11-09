<template>
  <div v-if="kind === 'glossaryIndex'" class="flex h-full min-h-0 flex-col gap-2">
    <MfInput v-model="keyword" class="shrink-0" :aria-label="$t(key + 'searchGlossary')" :placeholder="$t(key + 'searchGlossary')" clearable/>
    <dl class="min-h-0 space-y-2 overflow-auto">
      <div v-for="item in filtered" :key="item.id" class="rounded-mf-md border border-line p-2">
        <dt class="flex flex-wrap items-center gap-2 break-words text-sm font-semibold">
          {{ item.title }} <MfTag v-if="item.value" size="sm">{{ item.value }}</MfTag>
        </dt>
        <dd class="mt-1 whitespace-pre-wrap break-words text-xs text-content-secondary">{{ item.detail }}</dd>
      </div>
    </dl>
    <MfEmpty v-if="!filtered.length" size="sm" :title="$t(key + 'noGlossaryMatch')"/>
  </div>
  <div v-else-if="kind === 'statusBoard' || kind === 'deliveryQueue'" class="grid h-full auto-rows-min grid-cols-[repeat(auto-fit,minmax(min(100%,9rem),1fr))] gap-2 overflow-auto">
    <section v-for="column in columns" :key="column.status" class="min-w-0 space-y-2 rounded-mf-md bg-surface-hover p-2">
      <h4 class="flex flex-wrap items-center justify-between gap-2 text-sm font-semibold">
        <MfTag :variant="column.status" size="sm">{{ $t(key + 'workStatus.' + column.status) }}</MfTag><span class="tabular-nums">{{ column.items.length }}</span>
      </h4>
      <ul class="space-y-1.5">
        <li v-for="item in column.items" :key="item.id" class="rounded-mf-sm bg-surface p-2">
          <p class="break-words text-sm font-medium">{{ item.title }}</p>
          <p v-if="item.value" class="break-words text-xs text-content-tertiary">{{ item.value }}</p>
          <p v-if="kind === 'deliveryQueue' && item.secondary" class="break-words text-xs text-content-secondary">{{ $t(key + 'dueLabel') }} · {{ item.secondary }}</p>
          <p v-if="item.detail" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-secondary">{{ item.detail }}</p>
        </li>
      </ul>
    </section>
  </div>
  <div v-else-if="kind === 'acceptanceChecklist' || kind === 'releaseReadiness'" class="flex h-full min-h-0 flex-col gap-2">
    <div class="shrink-0 space-y-1 text-xs text-content-secondary">
      <p>{{ $t(key + (kind === 'releaseReadiness' ? 'gatesReady' : 'checksPassed'), { count: passed, total: items.length }) }}</p>
      <ProgressMeter :value="passed" :target="items.length" :label="$t(key + (kind === 'releaseReadiness' ? 'releaseGate' : 'acceptanceProgress'))"/>
    </div>
    <ul class="space-y-2 overflow-auto">
      <li v-for="item in items" :key="item.id" class="flex gap-2 rounded-mf-md border border-line p-2">
        <CircleCheck v-if="item.tone === 'success'" :size="16" class="mt-0.5 shrink-0 text-success" aria-hidden="true"/>
        <Circle v-else :size="16" class="mt-0.5 shrink-0 text-content-tertiary" aria-hidden="true"/>
        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center justify-between gap-1">
            <strong class="break-words text-sm">{{ item.title }}</strong>
            <MfTag :variant="item.tone" size="sm">{{ $t(key + (kind === 'releaseReadiness' ? 'readinessStatus.' : 'checkResult.') + item.tone) }}</MfTag>
          </div>
          <p v-if="item.value" class="break-words text-xs text-content-tertiary">{{ item.value }}</p>
          <p v-if="item.detail" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-secondary">{{ item.detail }}</p>
        </div>
      </li>
    </ul>
  </div>
  <ul v-else-if="kind === 'configDiffCards'" class="h-full space-y-2 overflow-auto">
    <li v-for="item in items" :key="item.id" class="rounded-mf-md border border-line p-2">
      <strong class="text-sm">{{ item.title }}</strong>
      <div class="mt-1 grid grid-cols-[repeat(auto-fit,minmax(min(100%,8rem),1fr))] gap-2">
        <div v-for="(snippet, index) in [item.value, item.secondary]" :key="index" class="min-w-0">
          <p class="text-xs text-content-secondary">{{ $t(key + (index === 0 ? 'previousConfig' : 'currentConfig')) }}</p>
          <pre class="select-text overflow-auto rounded-mf-sm p-2 text-xs" :class="index === 0 ? 'bg-surface-hover' : 'bg-primary/8'"><code>{{ snippet || '—' }}</code></pre>
        </div>
      </div>
      <p v-if="item.detail" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-secondary">{{ item.detail }}</p>
    </li>
  </ul>
  <ul v-else class="h-full space-y-2 overflow-auto">
    <li v-for="item in items" :key="item.id" class="rounded-mf-md border border-line p-2">
      <div class="mb-1 flex flex-wrap items-center justify-between gap-1">
        <div class="flex min-w-0 flex-wrap items-center gap-2">
          <strong class="break-words text-sm">{{ item.title }}</strong><MfTag v-if="item.value" size="sm">{{ item.value }}</MfTag>
        </div>
        <MfButton size="sm" variant="ghost" :disabled="!item.detail" :aria-label="$t(key + 'copyNamedSnippet', { name: item.title })" @click="copy(item)">
          <Copy :size="14" aria-hidden="true"/>{{ $t(key + 'copySnippet') }}
        </MfButton>
      </div>
      <pre class="select-text overflow-auto rounded-mf-sm bg-surface-hover p-2 text-xs"><code>{{ item.detail }}</code></pre>
    </li>
  </ul>
</template>

<script setup lang="ts">
import { Circle, CircleCheck, Copy } from '@lucide/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { type InformationItem } from '@/model/information-block'
import ProgressMeter from '@/views/components/visual/metrics/ProgressMeter.vue'

const props = defineProps<{
  kind: 'glossaryIndex' | 'statusBoard' | 'acceptanceChecklist' | 'copySnippets' | 'deliveryQueue' | 'releaseReadiness' | 'configDiffCards'
  items: readonly InformationItem[]
}>()
const { t } = useI18n()
const key = 'informationBlock.'
const keyword = ref('')
const filtered = computed(() => {
  const term = keyword.value.trim().toLocaleLowerCase()
  return term ? props.items.filter(item => [item.title, item.value, item.detail].some(value => value.toLocaleLowerCase().includes(term))) : props.items
})
const columns = computed(() => (['info', 'warning', 'danger', 'success'] as const).map(status => ({
  status, items: props.items.filter(item => item.tone === status),
})))
const passed = computed(() => props.items.filter(item => item.tone === 'success').length)
async function copy(item: InformationItem) {
  try {
    await navigator.clipboard.writeText(item.detail)
    MfMessage.success(t(key + 'snippetCopied'))
  } catch {
    MfMessage.error(t(key + 'snippetCopyFailed'))
  }
}
</script>
