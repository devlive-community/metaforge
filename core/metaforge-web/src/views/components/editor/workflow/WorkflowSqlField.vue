<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Eye } from '@lucide/vue'
import AceEditor from '@/views/components/editor/AceEditor.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
import WorkflowService from '@/services/workflow'
import { TokenUtils } from '@/utils/token'
/**
 * SQL parameter edited in the code editor used by the query console. When the node has chosen
 * a saved data source, the editor completes its object names and the query can be previewed:
 * the server reads the first rows through the data source plugin.
 */
const props = defineProps<{ modelValue: unknown; source?: string; disabled?: boolean; height?: string; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const { t } = useI18n()
const text = computed(() => typeof props.modelValue === 'string' ? props.modelValue : '')
const auth = TokenUtils.getAuthUser()
const baseUrl = import.meta.env.VITE_API_BASE_URL
// Completion reads the chosen data source's objects, the same way the query console does.
const autoComplete = computed(() => props.source ? {
  endpoint: () => `${baseUrl || ''}/api/v1/metadata/${props.source}/suggests`,
  method: 'GET',
  trigger: ['.', '@'],
  headers: { Authorization: auth?.type + ' ' + auth?.token },
  requestParams: (context: any) => ({ keyword: context.word }),
  transform: (response: any) => (response[0]?.data?.columns || []).map((item: any) => ({ label: item.object_name, insertText: item.object_name, detail: t(item.object_key), icon: item.object_type })),
} : undefined)

const previewing = ref(false), loading = ref(false), failure = ref('')
const headers = ref<string[]>([]), rows = ref<Record<string, unknown>[]>([]), elapsed = ref<number>()
let ticket = 0
async function preview() {
  if (!props.source || !text.value.trim()) return
  const request = ++ticket
  previewing.value = true; loading.value = true; failure.value = ''; headers.value = []; rows.value = []; elapsed.value = undefined
  try {
    const response = await WorkflowService.queryPreview(props.source, text.value)
    if (request !== ticket) return
    if (!response.status) throw Error(String(response.message || t('workflow.studio.previewFailed')))
    headers.value = (response.data?.columns || []).map((column: { name: string }) => column.name)
    rows.value = response.data?.rows || []
    elapsed.value = response.data?.elapsed
  } catch (error) { if (request === ticket) failure.value = error instanceof Error ? error.message : String(error) }
  finally { if (request === ticket) loading.value = false }
}
function close() { ticket++; previewing.value = false; loading.value = false }
</script>
<template>
  <div class="min-w-0 space-y-2">
    <div class="overflow-hidden rounded-mf-md border border-line">
      <AceEditor :value="text" :read-only="disabled" :height="height || '240px'" :auto-complete="autoComplete" @update:value="emit('update:modelValue', $event)"/>
    </div>
    <div class="flex items-center justify-between gap-2">
      <span class="min-w-0 truncate text-[11px] text-content-tertiary">{{ source ? $t('workflow.studio.previewHint') : $t('workflow.studio.previewNeedsSource') }}</span>
      <MfButton size="sm" class="shrink-0" :disabled="!source || !text.trim()" :loading="loading" @click="preview"><Eye :size="14"/>{{ $t('workflow.studio.previewData') }}</MfButton>
    </div>
    <MfModal :open="previewing" width="960px" :title="$t('workflow.studio.previewData')" @close="close">
      <div class="flex h-[420px] min-h-0 flex-col">
        <div v-if="loading" class="flex h-full items-center justify-center"><MfSpinner :label="$t('common.loading')"/></div>
        <MfAlert v-else-if="failure" variant="danger" :title="$t('workflow.studio.previewFailed')"><span class="break-all">{{ failure }}</span></MfAlert>
        <template v-else>
          <p class="mb-2 shrink-0 text-xs text-content-tertiary">{{ $t('workflow.studio.previewSummary', { count: rows.length, elapsed: elapsed ?? 0 }) }}</p>
          <ResultTable v-if="headers.length" class="min-h-0 flex-1" fill :headers="headers" :rows="rows"/>
          <div v-else class="flex h-full items-center justify-center"><MfEmpty size="sm" :title="$t('workflow.studio.previewEmpty')"/></div>
        </template>
      </div>
    </MfModal>
  </div>
</template>
