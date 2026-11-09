<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw, Wand2 } from '@lucide/vue'
import MetadataService from '@/services/metadata'
import WorkflowService from '@/services/workflow'
import { matchColumns } from './utils'
/**
 * Maps incoming columns (from the upstream query) onto the columns of the target table. Both lists
 * are read from the server; same-name columns map automatically and every row can be changed.
 * The value is `incoming=target`, the shape executors read.
 */
const props = defineProps<{
  modelValue: unknown
  /** Resolved target: data source code, database and table; undefined until all are chosen. */
  target?: Record<string, string>
  /** Resolved upstream: data source code and query; undefined when no input is connected. */
  upstream?: Record<string, string>
  disabled?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [value: Record<string, string>] }>()
const { t } = useI18n()
type Column = { name: string; type: string }
const targets = ref<Column[]>([]), incoming = ref<Column[]>([])
const targetLoading = ref(false), incomingLoading = ref(false), targetError = ref(''), incomingError = ref('')
const mapping = computed<Record<string, string>>(() => props.modelValue && typeof props.modelValue === 'object' && !Array.isArray(props.modelValue)
  ? Object.fromEntries(Object.entries(props.modelValue as Record<string, unknown>).map(([key, value]) => [key, String(value)])) : {})
/** Target column -> incoming column, for display. */
const byTarget = computed(() => Object.fromEntries(Object.entries(mapping.value).map(([source, target]) => [target, source])))
const mappedCount = computed(() => targets.value.filter(column => byTarget.value[column.name]).length)
const incomingOptions = computed(() => incoming.value.map(column => ({ label: column.type ? `${column.name} · ${column.type}` : column.name, value: column.name })))
const tableColumns = computed(() => [
  { title: t('workflow.studio.mapping.target'), dataIndex: 'name', key: 'target' },
  { title: t('workflow.studio.mapping.incoming'), dataIndex: 'name', key: 'incoming', width: '55%' },
])

let targetTicket = 0, incomingTicket = 0
async function loadTargets() {
  const ticket = ++targetTicket, target = props.target
  targets.value = []; targetError.value = ''
  if (!target) return
  targetLoading.value = true
  try {
    const response = await MetadataService.getColumnsByTable(target.source, target.database, target.table)
    if (ticket !== targetTicket) return
    if (!response.status || response.data?.isSuccessful === false) throw Error(String(response.data?.message || response.message || t('workflow.studio.mapping.targetFailed')))
    targets.value = (response.data?.columns || []).filter((row: any) => !row.type_name || row.type_name === 'column')
      .map((row: any) => ({ name: String(row.object_name), type: String(row.object_data_type || '') })).filter((row: Column) => row.name)
  } catch (error) { if (ticket === targetTicket) targetError.value = error instanceof Error ? error.message : String(error) }
  finally { if (ticket === targetTicket) targetLoading.value = false }
}
async function loadIncoming() {
  const ticket = ++incomingTicket, upstream = props.upstream
  incoming.value = []; incomingError.value = ''
  if (!upstream) return
  incomingLoading.value = true
  try {
    const response = await WorkflowService.queryColumns(upstream.source, upstream.query)
    if (ticket !== incomingTicket) return
    if (!response.status) throw Error(String(response.message || t('workflow.studio.mapping.incomingFailed')))
    incoming.value = (response.data || []) as Column[]
  } catch (error) { if (ticket === incomingTicket) incomingError.value = error instanceof Error ? error.message : String(error) }
  finally { if (ticket === incomingTicket) incomingLoading.value = false }
}
watch(() => JSON.stringify(props.target || null), loadTargets, { immediate: true })
watch(() => JSON.stringify(props.upstream || null), loadIncoming, { immediate: true })
// A new target or a cleared mapping is filled with same-name columns once both lists are known.
watch(() => [targets.value, incoming.value], () => {
  if (props.disabled || !targets.value.length || !incoming.value.length || Object.keys(mapping.value).length) return
  const matched = matchColumns(targets.value.map(column => column.name), incoming.value.map(column => column.name))
  if (Object.keys(matched).length) emit('update:modelValue', matched)
})
function autoMatch() {
  emit('update:modelValue', matchColumns(targets.value.map(column => column.name), incoming.value.map(column => column.name), mapping.value))
}
/** One incoming column feeds one target column: choosing it again moves it. */
function choose(target: string, source: unknown) {
  const next = Object.fromEntries(Object.entries(mapping.value).filter(([key, value]) => value !== target && key !== source))
  if (source !== undefined && source !== null && String(source).trim()) next[String(source).trim()] = target
  emit('update:modelValue', next)
}
function refresh() { loadTargets(); loadIncoming() }
</script>
<template>
  <div class="min-w-0 space-y-2">
    <MfEmpty v-if="!target" size="sm" :title="$t('workflow.studio.mapping.chooseTarget')"/>
    <template v-else>
      <div class="flex items-center justify-between gap-2">
        <span class="truncate text-xs text-content-tertiary">{{ $t('workflow.studio.mapping.summary', { mapped: mappedCount, total: targets.length }) }}</span>
        <div class="flex shrink-0 items-center gap-1">
          <MfTooltip :content="$t('workflow.studio.mapping.autoMatch')"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.mapping.autoMatch')" :disabled="disabled || !targets.length || !incoming.length" @click="autoMatch"><Wand2 :size="14"/></MfButton></MfTooltip>
          <MfTooltip :content="$t('common.refresh')"><MfButton variant="ghost" size="sm" :aria-label="$t('common.refresh')" :loading="targetLoading || incomingLoading" @click="refresh"><RefreshCw :size="14"/></MfButton></MfTooltip>
        </div>
      </div>
      <MfAlert v-if="!upstream" variant="info" :title="$t('workflow.studio.mapping.noUpstream')"/>
      <MfAlert v-else-if="incomingError" variant="warning" :title="$t('workflow.studio.mapping.incomingFailed')"><span class="break-all">{{ incomingError }}</span></MfAlert>
      <MfAlert v-if="targetError" variant="danger" :title="$t('workflow.studio.mapping.targetFailed')"><span class="break-all">{{ targetError }}</span></MfAlert>
      <div v-if="targetLoading" class="flex h-40 items-center justify-center"><MfSpinner/></div>
      <MfTable v-else-if="!targetError" :columns="tableColumns" :data-source="targets" row-key="name" density="compact" :empty-text="$t('workflow.studio.mapping.noColumns')">
        <template #cell-target="{ record }"><div class="min-w-0"><div class="truncate text-xs font-medium text-content">{{ record.name }}</div><div v-if="record.type" class="truncate text-[11px] text-content-tertiary">{{ record.type }}</div></div></template>
        <template #cell-incoming="{ record }">
          <MfSelect v-if="incomingOptions.length" size="sm" class="w-full" searchable clearable :disabled="disabled" :options="incomingOptions" :model-value="byTarget[record.name] ?? ''" :placeholder="$t('workflow.studio.mapping.skip')" @update:model-value="choose(record.name, $event)"/>
          <MfInput v-else size="sm" :disabled="disabled || incomingLoading" :model-value="byTarget[record.name] ?? ''" :placeholder="$t('workflow.studio.mapping.skip')" @update:model-value="choose(record.name, $event)"/>
        </template>
      </MfTable>
    </template>
  </div>
</template>
