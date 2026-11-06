<script setup lang="ts">
import { computed, ref } from 'vue'
import { Copy, Download, RefreshCw, Search } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { useI18n } from 'vue-i18n'
const props = defineProps<{ lines: string[]; loading?: boolean; error?: string; compact?: boolean }>()
const emit = defineEmits<{ refresh: [] }>()
const { t } = useI18n(), search = ref('')
const filtered = computed(() => props.lines.filter(line => line.toLowerCase().includes(search.value.toLowerCase())))
async function copy() { try { await navigator.clipboard.writeText(props.lines.join('\n')); MfMessage.success(t('common.success')) } catch { MfMessage.error(t('workflow.studio.copyFailed')) } }
function download() { const url = URL.createObjectURL(new Blob([props.lines.join('\n')], { type: 'text/plain;charset=utf-8' })); const link = document.createElement('a'); link.href = url; link.download = 'workflow.log'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000) }
</script>
<template>
  <section class="flex h-full min-h-0 flex-col">
    <div class="flex shrink-0 items-center justify-between gap-3 border-b border-line px-4 py-2"><strong v-if="!compact" class="text-xs text-content">{{ $t('workflow.studio.logs') }}</strong><div class="flex items-center gap-2"><MfInput v-if="!compact" v-model="search" size="sm" :placeholder="$t('workflow.studio.searchLog')" class="w-44"><template #prefix><Search :size="13"/></template></MfInput><MfButton size="sm" variant="ghost" :loading="loading" :aria-label="$t('common.refresh')" @click="emit('refresh')"><RefreshCw :size="14"/></MfButton><MfButton size="sm" variant="ghost" :disabled="!lines.length" :aria-label="$t('workflow.studio.copyLog')" @click="copy"><Copy :size="14"/></MfButton><MfButton v-if="!compact" size="sm" variant="ghost" :disabled="!lines.length" :aria-label="$t('workflow.studio.downloadLog')" @click="download"><Download :size="14"/></MfButton></div></div>
    <MfAlert v-if="error" variant="danger" :title="error"/>
    <div v-else-if="!lines.length" class="flex min-h-0 flex-1 items-center justify-center"><MfSpinner v-if="loading"/><MfEmpty v-else size="sm" :title="$t('workflow.studio.noLogs')"/></div>
    <pre v-else class="m-0 min-h-0 flex-1 overflow-auto bg-[#102420] p-4 font-mono text-[11px] leading-6 text-[#c6eadd]"><span v-for="(line, index) in filtered" :key="index" class="block whitespace-pre-wrap break-all" :class="/ERROR|FAILURE/.test(line) && 'text-red-300'">{{ line }}</span></pre>
  </section>
</template>
