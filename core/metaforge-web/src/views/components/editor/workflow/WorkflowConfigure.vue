<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Maximize2, X } from '@lucide/vue'
import SourceService from '@/services/source'
import { FilterModel } from '@/model/filter'
import type { ConfigureItem, NodeInstance } from './types'
const props = defineProps<{ selectedNode: NodeInstance | null; expanded?: boolean; readonly?: boolean }>()
const emit = defineEmits<{ 'update:node': [node: NodeInstance]; expand: []; close: [] }>()
const { t, te } = useI18n()
const tab = ref('basic'), form = ref<any>(), validated = ref(false), mapDrafts = ref<Record<string, string>>({})
const values = computed(() => props.selectedNode?.data || {})
const fields = computed(() => props.selectedNode?.configure || [])
const sqlFields = computed(() => fields.value.filter(field => /query|sql$/i.test(field.field) && field.type === 'textarea'))
const currentFields = computed(() => tab.value === 'sql' ? sqlFields.value : fields.value.filter(field => !sqlFields.value.includes(field) && (tab.value === 'advanced' ? !field.required : field.required)))
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
    const rows = response.data?.content || []
    const options = rows.map((row: any) => ({ label: `${row.name} · ${row.type}`, value: row.code }))
    sources.value = reset ? options : [...sources.value, ...options]; sourcePage.value = page; sourceTotal.value = response.data?.total || 0
  } catch { if (ticket === sourceTicket) sourcesFailed.value = true }
  finally { if (ticket === sourceTicket) sourcesBusy.value = false }
}
watch(() => props.selectedNode?.tid, () => { if (fields.value.some(field => field.type === 'source') && !sources.value.length) loadSources() }, { immediate: true })
watch(() => props.selectedNode?.id, () => { tab.value = 'basic'; validated.value = false; mapDrafts.value = {}; form.value?.clearValidate?.() })
function update(field: string, value: unknown) {
  if (props.selectedNode) emit('update:node', { ...props.selectedNode, data: { ...values.value, [field]: value } })
  validated.value = false
}
function setLabel(value: string) { if (props.selectedNode) emit('update:node', { ...props.selectedNode, label: value }) }
const mapText = (value: unknown) => typeof value === 'string' ? value : value && typeof value === 'object' ? Object.entries(value).map(([key, val]) => `${key}=${val}`).join('\n') : ''
const parseMap = (value: string) => Object.fromEntries(value.split('\n').filter(line => line.includes('=')).map(line => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]).filter(([key]) => key))
function mapValue(text: string) {
  const lines = text.split('\n').filter(line => line.trim())
  const keys = lines.map(line => line.slice(0, line.indexOf('=')).trim())
  return lines.every(line => line.indexOf('=') > 0 && line.slice(line.indexOf('=') + 1).trim()) && new Set(keys).size === keys.length ? parseMap(text) : text
}
function updateMap(field: string, text: string) { mapDrafts.value[field] = text; update(field, mapValue(text)) }
watch(() => JSON.stringify(values.value), () => {
  fields.value.filter(field => field.type === 'map').forEach(field => {
    const raw = mapDrafts.value[field.field]
    if (raw !== undefined && JSON.stringify(mapValue(raw)) !== JSON.stringify(values.value[field.field])) mapDrafts.value[field.field] = mapText(values.value[field.field])
  })
})
const listValue = (value: unknown) => Array.isArray(value) ? value.map(String) : typeof value === 'string' ? value.split('\n').filter(Boolean) : []
const rulesFor = (field: ConfigureItem) => [
  ...(field.required ? [{ required: true, message: t('workflow.studio.required') }] : []),
  ...(field.type === 'map' ? [{ validator: (value: unknown) => (!value && !field.required) || !!value && typeof value === 'object' && !Array.isArray(value) && (!field.required || Object.keys(value).length > 0) || t('workflow.studio.invalidMap') }] : []),
  ...(field.type === 'array' ? [{ type: 'array' as const, required: field.required, message: t('workflow.studio.required') }] : []),
]
async function validate() {
  try { await form.value?.validate(); validated.value = true } catch { validated.value = false }
}
const nodeDescription = computed(() => { const key = 'workflow.studio.nodeDescription.' + props.selectedNode?.tid; return te(key) ? t(key) : props.selectedNode?.description })
const fieldDescription = (field: ConfigureItem) => { const key = 'workflow.studio.fieldDescription.' + field.field; return te(key) ? t(key) : field.description }
const fieldLabel = (field: ConfigureItem) => { const key = 'workflow.studio.field.' + field.field; return te(key) ? t(key) : field.label }
</script>
<template>
  <aside class="flex h-full min-h-0 flex-col border-l border-line bg-surface">
    <div class="flex items-start justify-between border-b border-line px-4 py-3"><div class="min-w-0"><strong class="block truncate text-sm text-content">{{ selectedNode?.label || $t('workflow.studio.nodeConfig') }}</strong><p class="mt-1 text-xs text-content-tertiary">{{ $t('workflow.studio.nodeConfigHint') }}</p></div><div class="flex"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.fullConfig')" @click="emit('expand')"><Maximize2 :size="14"/></MfButton><MfButton variant="ghost" size="sm" :aria-label="$t('common.close')" @click="emit('close')"><X :size="14"/></MfButton></div></div>
    <template v-if="selectedNode">
      <div class="flex shrink-0 border-b border-line px-3"><button v-for="item in ['basic', ...(sqlFields.length ? ['sql'] : []), 'advanced']" :key="item" type="button" class="flex-1 border-b-2 px-2 py-3 text-xs font-medium" :class="tab === item ? 'border-primary text-primary' : 'border-transparent text-content-tertiary hover:text-content'" @click="tab = item">{{ $t('workflow.studio.config.' + item) }}</button></div>
      <div class="min-h-0 flex-1 overflow-y-auto p-4">
        <MfForm ref="form" :model="values" layout="vertical" :key="`${selectedNode.id}-${tab}`">
          <MfFormItem v-if="tab === 'basic'" :label="$t('workflow.studio.nodeName')"><MfInput :model-value="selectedNode.label" :disabled="readonly" @update:model-value="setLabel"/></MfFormItem>
          <div v-if="tab === 'basic'" class="mb-5 rounded-mf-md bg-surface-hover px-3 py-2 text-xs leading-5 text-content-secondary">{{ nodeDescription }}</div>
          <MfFormItem v-for="field in currentFields" :key="field.field" :name="field.field" :label="fieldLabel(field)" :rules="rulesFor(field)">
            <div class="min-w-0 w-full">
            <div v-if="field.type === 'source'" class="space-y-2">
              <div class="flex gap-2"><MfInput v-model="sourceSearch" clearable :placeholder="$t('workflow.studio.searchSources')" @enter="loadSources()"/><MfButton size="sm" :loading="sourcesBusy" @click="loadSources()">{{ $t('common.search') }}</MfButton></div>
              <MfSelect :model-value="values[field.field]" :disabled="readonly" class="w-full" :options="sources" :placeholder="$t('workflow.studio.selectSource')" @update:model-value="update(field.field, $event)"/>
              <p v-if="values[field.field] && !sources.some(source => source.value === values[field.field])" class="break-all text-xs text-content-tertiary">{{ values[field.field] }}</p>
              <MfButton v-if="sources.length < sourceTotal" size="sm" :loading="sourcesBusy" @click="loadSources(false)">{{ $t('workflow.studio.loadMore') }}</MfButton>
              <MfAlert v-if="sourcesFailed" variant="danger" :title="$t('workflow.studio.sourcesFailed')"/>
            </div>
            <MfInputNumber v-else-if="field.type === 'number'" :model-value="Number(values[field.field]) || 0" :disabled="readonly" decimal-only commit-on-change class="w-full" :step="field.step || 1" :min="field.min" :max="field.max" @update:model-value="update(field.field, $event)"/>
            <MfSwitch v-else-if="field.type === 'switch'" :model-value="values[field.field] === true" :disabled="readonly" @update:model-value="update(field.field, $event)"/>
            <MfSelect v-else-if="field.type === 'select'" :model-value="values[field.field]" :disabled="readonly" class="w-full" :options="field.options || []" @update:model-value="update(field.field, $event)"/>
            <MfSelect v-else-if="field.type === 'array'" :model-value="listValue(values[field.field])" :disabled="readonly" tags searchable class="w-full" :options="listValue(values[field.field]).map(value => ({ label: value, value }))" @update:model-value="update(field.field, $event)"/>
            <MfTextarea v-else-if="field.type === 'map'" :model-value="mapDrafts[field.field] ?? mapText(values[field.field])" :disabled="readonly" :rows="6" placeholder="source_column=target_column" @update:model-value="updateMap(field.field, $event)"/>
            <MfTextarea v-else-if="field.type === 'textarea'" :model-value="values[field.field]" :disabled="readonly" :rows="tab === 'sql' ? (expanded ? 18 : 10) : 4" class="font-mono text-xs" :placeholder="field.placeholder" @update:model-value="update(field.field, $event)"/>
            <MfInput v-else :model-value="values[field.field]" :disabled="readonly" :type="field.type === 'password' ? 'password' : 'text'" :show-password="field.type === 'password'" :placeholder="field.placeholder" @update:model-value="update(field.field, $event)"/>
            <p v-if="field.description" class="mt-1.5 text-[11px] leading-5 text-content-tertiary">{{ fieldDescription(field) }}</p>
            </div>
          </MfFormItem>
          <MfEmpty v-if="!currentFields.length && tab !== 'basic'" size="sm" :title="$t('workflow.studio.noOptions')"/>
        </MfForm>
      </div>
      <footer class="flex shrink-0 items-center justify-between gap-2 border-t border-line p-3"><span class="text-xs text-content-tertiary">{{ $t(validated ? 'workflow.studio.fieldsValid' : 'workflow.studio.autoApplied') }}</span><MfButton size="sm" :disabled="readonly" @click="validate"><Check :size="14"/>{{ $t('workflow.studio.checkFields') }}</MfButton></footer>
    </template>
    <MfEmpty v-else class="flex-1" :title="$t('workflow.placeholder.selectNode')" :description="$t('workflow.studio.configEmpty')"/>
  </aside>
</template>
