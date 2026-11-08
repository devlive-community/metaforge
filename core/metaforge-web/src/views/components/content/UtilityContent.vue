<template>
  <template v-if="widget.kind === 'copyText'">
    <div v-if="widget.text.trim()" class="flex h-full min-w-0 flex-col items-start justify-center gap-2">
      <MfButton :disabled="editable" :loading="copying" @click="copy"><Copy :size="15"/>{{ widget.name || $t('dashboard.studio.tool.copyText') }}</MfButton>
    </div>
    <MfEmpty v-else class="h-full" size="sm" :title="$t('dashboard.studio.copyTextPrompt')"/>
  </template>
  <template v-else-if="widget.kind === 'definitionList'">
    <dl v-if="entries.length" class="h-full min-h-0 divide-y divide-line overflow-auto rounded-mf-md border border-line text-sm">
      <div v-for="(entry, index) in entries" :key="entry.id + index" class="grid grid-cols-[minmax(0,1fr)_minmax(0,2fr)] gap-3 px-3 py-2">
        <dt class="break-words font-medium text-content">{{ entry.label }}</dt>
        <dd class="whitespace-pre-wrap break-words text-content-secondary">{{ entry.text || '—' }}</dd>
      </div>
    </dl>
    <MfEmpty v-else class="h-full" size="sm" :title="$t('dashboard.studio.business.definitionEmpty')"/>
  </template>
  <ul v-else-if="entries.length" class="grid h-full min-h-0 content-start gap-2 overflow-auto">
    <li v-for="(entry, index) in entries" :key="entry.id + index" class="flex min-w-0 flex-wrap items-start gap-2">
      <MfTag size="sm" :variant="entry.tone" class="max-w-full"><span class="truncate" :title="entry.label">{{ entry.label }}</span></MfTag>
      <span v-if="entry.text" class="min-w-24 flex-1 whitespace-pre-wrap break-words text-xs leading-5 text-content-secondary">{{ entry.text }}</span>
    </li>
  </ul>
  <MfEmpty v-else class="h-full" size="sm" :title="$t('dashboard.studio.legendEmpty')"/>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Copy } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import type { DashboardWidget } from '@/model/dashboard-layout'

const props = defineProps<{ widget: Pick<DashboardWidget, 'kind' | 'links' | 'name' | 'text'>; editable?: boolean }>()
const { t } = useI18n()
const entries = computed(() => props.widget.links?.filter(entry => entry.label.trim()) ?? [])
const copying = ref(false)
async function copy() {
  if (props.editable || copying.value || !props.widget.text.trim()) return
  copying.value = true
  try {
    await navigator.clipboard.writeText(props.widget.text)
    MfMessage.success(t('dashboard.studio.textCopied'))
  } catch {
    MfMessage.error(t('dashboard.studio.textCopyFailed'))
  } finally {
    copying.value = false
  }
}
</script>
