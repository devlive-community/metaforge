<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SourceService from '@/services/source'
import MetadataService from '@/services/metadata'
import { FilterModel } from '@/model/filter'
import { useSourceDatabasesStore } from '@/stores/source-databases'
import type { ConfigureItem } from './types'
import { resolveFrom } from './utils'
import WorkflowColumnMapping from './WorkflowColumnMapping.vue'
import WorkflowSqlField from './WorkflowSqlField.vue'
/**
 * Renders one declared executor field (node parameter or job setting) by its type, so every
 * executor plugin gets a consistent form without editor changes.
 */
const props = defineProps<{
  field: ConfigureItem; modelValue: unknown; disabled?: boolean; rows?: number
  /** Values of the field's own node and of the node feeding it, for fields whose choices depend on them. */
  context?: { values?: Record<string, any>; upstream?: Record<string, any> }
}>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()
const { t, te } = useI18n()

const mapText = (value: unknown) => typeof value === 'string' ? value : value && typeof value === 'object' ? Object.entries(value).map(([key, val]) => `${key}=${val}`).join('\n') : ''
const parseMap = (value: string) => Object.fromEntries(value.split('\n').filter(line => line.includes('=')).map(line => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]).filter(([key]) => key))
/** Valid text becomes a map; anything else stays text so validation can point at it. */
function mapValue(text: string) {
  const lines = text.split('\n').filter(line => line.trim())
  const keys = lines.map(line => line.slice(0, line.indexOf('=')).trim())
  return lines.every(line => line.indexOf('=') > 0 && line.slice(line.indexOf('=') + 1).trim()) && new Set(keys).size === keys.length ? parseMap(text) : text
}
const mapDraft = ref<string>()
watch(() => props.modelValue, value => { if (mapDraft.value !== undefined && JSON.stringify(mapValue(mapDraft.value)) !== JSON.stringify(value)) mapDraft.value = mapText(value) })
function updateMap(text: string) { mapDraft.value = text; emit('update:modelValue', mapValue(text)) }
const listValue = (value: unknown) => Array.isArray(value) ? value.map(String) : typeof value === 'string' ? value.split('\n').filter(Boolean) : []
/** Empty numbers stay empty so optional settings fall back to the executor configuration. */
const numberValue = computed(() => typeof props.modelValue === 'number' ? props.modelValue : props.modelValue === undefined || props.modelValue === null || props.modelValue === '' ? undefined : Number(props.modelValue))
const description = computed(() => { const key = 'workflow.studio.fieldDescription.' + props.field.field; return te(key) ? t(key) : props.field.description })

const sources = ref<{ label: string; value: string }[]>([]), sourcesBusy = ref(false), sourcesFailed = ref(false), sourceSearch = ref(''), sourcePage = ref(1), sourceTotal = ref(0)
let sourceTicket = 0
async function loadSources(reset = true) {
  const ticket = ++sourceTicket
  sourcesBusy.value = true; sourcesFailed.value = false
  const page = reset ? 1 : sourcePage.value + 1
  try {
    const response = await SourceService.getAll(Object.assign(new FilterModel(), { page, size: 20, keyword: sourceSearch.value }))
    if (ticket !== sourceTicket) return
    if (!response.status) throw Error(String(response.message))
    const options = (response.data?.content || []).map((row: any) => ({ label: `${row.name} · ${row.type}`, value: row.code }))
    sources.value = reset ? options : [...sources.value, ...options]; sourcePage.value = page; sourceTotal.value = response.data?.total || 0
  } catch { if (ticket === sourceTicket) sourcesFailed.value = true }
  finally { if (ticket === sourceTicket) sourcesBusy.value = false }
}
watch(() => props.field.type, type => { if (type === 'source' && !sources.value.length) loadSources() }, { immediate: true })

/** Database and table choices are read from the data source chosen in the same node. */
const dependency = computed(() => resolveFrom(props.field.from, props.context?.values))
const upstream = computed(() => resolveFrom(props.field.upstream, props.context?.upstream))
const databases = useSourceDatabasesStore()
const tables = ref<string[]>([]), tablesBusy = ref(false), tablesError = ref('')
let tableTicket = 0
async function loadTables() {
  const ticket = ++tableTicket, from = dependency.value
  tables.value = []; tablesError.value = ''
  if (!from?.source || !from.database) return
  tablesBusy.value = true
  try {
    const response = await MetadataService.getTablesByDatabase(from.source, from.database)
    if (ticket !== tableTicket) return
    if (!response.status || response.data?.isSuccessful === false) throw Error(String(response.data?.message || response.message))
    tables.value = (response.data?.columns || []).map((row: any) => row.object_name).filter(Boolean)
  } catch (error) { if (ticket === tableTicket) tablesError.value = error instanceof Error ? error.message : String(error) }
  finally { if (ticket === tableTicket) tablesBusy.value = false }
}
watch(() => props.field.type === 'database' ? dependency.value?.source : undefined, source => { if (source) databases.load(source) }, { immediate: true })
watch(() => props.field.type === 'table' ? JSON.stringify(dependency.value || null) : undefined, value => { if (value !== undefined) loadTables() }, { immediate: true })
const databaseEntry = computed(() => dependency.value?.source ? databases.entryOf(dependency.value.source) : undefined)
/** Keep a saved value selectable even when the list no longer contains it. */
const withCurrent = (options: { label: string; value: string }[]) => {
  const current = typeof props.modelValue === 'string' ? props.modelValue : ''
  return current && !options.some(option => option.value === current) ? [{ label: current, value: current }, ...options] : options
}
const databaseOptions = computed(() => withCurrent(databaseEntry.value?.options || []))
const tableOptions = computed(() => withCurrent(tables.value.map(table => ({ label: table, value: table }))))
</script>
<template>
  <div class="min-w-0 w-full">
    <div v-if="field.type === 'source'" class="space-y-2">
      <div class="flex gap-2"><MfInput v-model="sourceSearch" clearable :placeholder="$t('workflow.studio.searchSources')" @enter="loadSources()"/><MfButton size="sm" class="shrink-0" :loading="sourcesBusy" @click="loadSources()">{{ $t('common.search') }}</MfButton></div>
      <MfSelect :model-value="modelValue" :disabled="disabled" class="w-full" :options="sources" :placeholder="$t('workflow.studio.selectSource')" @update:model-value="emit('update:modelValue', $event)"/>
      <p v-if="modelValue && !sources.some(source => source.value === modelValue)" class="break-all text-xs text-content-tertiary">{{ modelValue }}</p>
      <MfButton v-if="sources.length < sourceTotal" size="sm" :loading="sourcesBusy" @click="loadSources(false)">{{ $t('workflow.studio.loadMore') }}</MfButton>
      <MfAlert v-if="sourcesFailed" variant="danger" :title="$t('workflow.studio.sourcesFailed')"/>
    </div>
    <template v-else-if="field.type === 'database' || field.type === 'table'">
      <MfSelect :model-value="(modelValue as string) ?? ''" class="w-full" searchable :disabled="disabled || !dependency"
                :loading="field.type === 'database' ? databaseEntry?.loading : tablesBusy"
                :options="field.type === 'database' ? databaseOptions : tableOptions"
                :placeholder="$t(dependency ? (field.type === 'database' ? 'workflow.studio.selectDatabase' : 'workflow.studio.selectTable') : 'workflow.studio.chooseDependency')"
                @update:model-value="emit('update:modelValue', $event)"/>
      <MfAlert v-if="field.type === 'database' ? databaseEntry?.error : tablesError" class="mt-2" variant="danger" :title="$t('workflow.studio.metadataFailed')"><span class="break-all">{{ field.type === 'database' ? databaseEntry?.error : tablesError }}</span></MfAlert>
    </template>
    <WorkflowSqlField v-else-if="field.type === 'sql'" :model-value="modelValue" :source="dependency?.source" :disabled="disabled" :height="`${Math.max(10, rows || 10) * 22}px`" @update:model-value="emit('update:modelValue', $event)"/>
    <WorkflowColumnMapping v-else-if="field.type === 'mapping'" :model-value="modelValue" :target="dependency" :upstream="upstream" :disabled="disabled" @update:model-value="emit('update:modelValue', $event)"/>
    <MfInputNumber v-else-if="field.type === 'number'" :model-value="numberValue" :disabled="disabled" decimal-only commit-on-change class="w-full" :step="field.step || 1" :min="field.min" :max="field.max" @update:model-value="emit('update:modelValue', $event)"/>
    <MfSwitch v-else-if="field.type === 'switch'" :model-value="modelValue === true" :disabled="disabled" @update:model-value="emit('update:modelValue', $event)"/>
    <MfSelect v-else-if="field.type === 'select'" :model-value="modelValue" :disabled="disabled" class="w-full" :options="field.options || []" @update:model-value="emit('update:modelValue', $event)"/>
    <MfSelect v-else-if="field.type === 'array'" :model-value="listValue(modelValue)" :disabled="disabled" tags searchable class="w-full" :options="listValue(modelValue).map(value => ({ label: value, value }))" @update:model-value="emit('update:modelValue', $event)"/>
    <MfTextarea v-else-if="field.type === 'map'" :model-value="mapDraft ?? mapText(modelValue)" :disabled="disabled" :rows="6" placeholder="source_column=target_column" @update:model-value="updateMap"/>
    <MfTextarea v-else-if="field.type === 'textarea'" :model-value="modelValue as string" :disabled="disabled" :rows="rows || 4" class="font-mono text-xs" :placeholder="field.placeholder" @update:model-value="emit('update:modelValue', $event)"/>
    <MfInput v-else :model-value="modelValue as string" :disabled="disabled" :type="field.type === 'password' ? 'password' : 'text'" :show-password="field.type === 'password'" :placeholder="field.placeholder" @update:model-value="emit('update:modelValue', $event)"/>
    <p v-if="description" class="mt-1.5 text-[11px] leading-5 text-content-tertiary">{{ description }}</p>
  </div>
</template>
