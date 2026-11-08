<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Code2, Settings2, SlidersHorizontal } from '@lucide/vue'
import StudioPanelHeader from '@/views/components/studio/StudioPanelHeader.vue'
import type { ConfigureItem, NodeInstance } from './types'
import { dependentFields, fieldGroup } from './utils'
import WorkflowField from './WorkflowField.vue'
const props = defineProps<{ selectedNode: NodeInstance | null; upstreamNode?: NodeInstance | null; readonly?: boolean }>()
const emit = defineEmits<{ 'update:node': [node: NodeInstance]; close: [] }>()
const { t, te } = useI18n()
const tab = ref('basic'), form = ref<any>(), validated = ref(false)
const values = computed(() => props.selectedNode?.data || {})
const fields = computed(() => props.selectedNode?.configure || [])
// Basic always shows (it holds the component name); SQL and advanced only when the component declares such fields.
const tabs = computed(() => ['basic', ...(['sql', 'advanced'] as const).filter(group => fields.value.some(field => fieldGroup(field) === group))])
watch(tabs, value => { if (!value.includes(tab.value)) tab.value = 'basic' })
const currentFields = computed(() => fields.value.filter(field => fieldGroup(field) === tab.value))
const tabIcon = (key: string) => key === 'sql' ? Code2 : key === 'advanced' ? SlidersHorizontal : Settings2
watch(() => props.selectedNode?.id, () => { tab.value = 'basic'; validated.value = false; form.value?.clearValidate?.() })
/** Field values of this node and the node feeding it, for fields whose choices depend on them. */
const context = computed(() => ({ values: values.value, upstream: props.upstreamNode?.data }))
function update(field: string, value: unknown) {
  if (!props.selectedNode) return
  // A new data source invalidates its database, a new database its table, and so on.
  const cleared = Object.fromEntries(dependentFields(fields.value, field).map(name => [name, undefined]))
  emit('update:node', { ...props.selectedNode, data: { ...values.value, ...cleared, [field]: value } })
  validated.value = false
}
function setLabel(value: string) { if (props.selectedNode) emit('update:node', { ...props.selectedNode, label: value }) }
const rulesFor = (field: ConfigureItem) => [
  ...(field.required ? [{ required: true, message: t('workflow.studio.required') }] : []),
  ...(field.type === 'map' ? [{ validator: (value: unknown) => (!value && !field.required) || !!value && typeof value === 'object' && !Array.isArray(value) && (!field.required || Object.keys(value).length > 0) || t('workflow.studio.invalidMap') }] : []),
  ...(field.type === 'array' ? [{ type: 'array' as const, required: field.required, message: t('workflow.studio.required') }] : []),
]
async function validate() {
  try { await form.value?.validate(); validated.value = true } catch { validated.value = false }
}
const nodeDescription = computed(() => { const key = 'workflow.studio.nodeDescription.' + props.selectedNode?.tid; return te(key) ? t(key) : props.selectedNode?.description })
const fieldLabel = (field: ConfigureItem) => { const key = 'workflow.studio.field.' + field.field; return te(key) ? t(key) : field.label }
</script>
<template>
  <aside class="flex h-full min-h-0 min-w-0 flex-col overflow-hidden border-l border-line bg-surface shadow-mf-sm">
    <StudioPanelHeader :title="$t('workflow.studio.nodeConfig')"
                          :pinned="true" :pinnable="false" @close="emit('close')"/>
    <template v-if="selectedNode">
      <nav class="mx-3 flex shrink-0 items-center gap-1 px-1 shadow-[inset_0_-1px_0_var(--mf-color-line)]" :aria-label="$t('workflow.studio.nodeConfig')">
        <button v-for="item in tabs" :key="item" type="button" :aria-pressed="tab === item"
                class="flex min-w-0 flex-1 items-center justify-center gap-1.5 whitespace-nowrap border-b-2 px-1.5 py-2 text-xs font-medium transition-[color,border-color] duration-(--mf-duration-base) ease-(--mf-easing)"
                :class="tab === item ? 'border-b-primary text-primary' : 'border-b-transparent text-content-secondary hover:text-content'" @click="tab = item">
          <component :is="tabIcon(item)" :size="14"/>{{ $t('workflow.studio.config.' + item) }}
        </button>
      </nav>
      <div class="min-h-0 flex-1 overflow-y-auto p-4">
        <MfForm ref="form" :model="values" layout="vertical" :key="`${selectedNode.id}-${tab}`">
          <MfFormItem v-if="tab === 'basic'" :label="$t('workflow.studio.nodeName')"><MfInput :model-value="selectedNode.label" :disabled="readonly" @update:model-value="setLabel"/></MfFormItem>
          <div v-if="tab === 'basic'" class="mb-5 rounded-mf-md bg-surface-hover px-3 py-2 text-xs leading-5 text-content-secondary">{{ nodeDescription }}</div>
          <MfFormItem v-for="field in currentFields" :key="field.field" :name="field.field" :label="fieldLabel(field)" :rules="rulesFor(field)">
            <WorkflowField :field="field" :context="context" :model-value="values[field.field]" :disabled="readonly" :rows="tab === 'sql' ? 14 : 4" @update:model-value="update(field.field, $event)"/>
          </MfFormItem>
        </MfForm>
      </div>
      <footer class="flex shrink-0 items-center justify-between gap-2 border-t border-line p-3"><span class="text-xs text-content-tertiary">{{ $t(validated ? 'workflow.studio.fieldsValid' : 'workflow.studio.autoApplied') }}</span><MfButton size="sm" :disabled="readonly" @click="validate"><Check :size="14"/>{{ $t('workflow.studio.checkFields') }}</MfButton></footer>
    </template>
    <MfEmpty v-else class="flex-1" :title="$t('workflow.placeholder.selectNode')" :description="$t('workflow.studio.configEmpty')"/>
  </aside>
</template>
