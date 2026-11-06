<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Maximize2, X } from '@lucide/vue'
import type { ConfigureItem, NodeInstance } from './types'
import { fieldGroup } from './utils'
import WorkflowField from './WorkflowField.vue'
const props = defineProps<{ selectedNode: NodeInstance | null; expanded?: boolean; readonly?: boolean }>()
const emit = defineEmits<{ 'update:node': [node: NodeInstance]; expand: []; close: [] }>()
const { t, te } = useI18n()
const tab = ref('basic'), form = ref<any>(), validated = ref(false)
const values = computed(() => props.selectedNode?.data || {})
const fields = computed(() => props.selectedNode?.configure || [])
const tabs = computed(() => ['basic', ...(fields.value.some(field => fieldGroup(field) === 'sql') ? ['sql'] : []), 'advanced'])
const currentFields = computed(() => fields.value.filter(field => fieldGroup(field) === tab.value))
watch(() => props.selectedNode?.id, () => { tab.value = 'basic'; validated.value = false; form.value?.clearValidate?.() })
function update(field: string, value: unknown) {
  if (props.selectedNode) emit('update:node', { ...props.selectedNode, data: { ...values.value, [field]: value } })
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
  <aside class="flex h-full min-h-0 flex-col border-l border-line bg-surface">
    <div class="flex items-start justify-between border-b border-line px-4 py-3"><div class="min-w-0"><strong class="block truncate text-sm text-content">{{ selectedNode?.label || $t('workflow.studio.nodeConfig') }}</strong><p class="mt-1 text-xs text-content-tertiary">{{ $t('workflow.studio.nodeConfigHint') }}</p></div><div class="flex"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.fullConfig')" @click="emit('expand')"><Maximize2 :size="14"/></MfButton><MfButton variant="ghost" size="sm" :aria-label="$t('common.close')" @click="emit('close')"><X :size="14"/></MfButton></div></div>
    <template v-if="selectedNode">
      <div class="flex shrink-0 border-b border-line px-3"><button v-for="item in tabs" :key="item" type="button" class="flex-1 border-b-2 px-2 py-3 text-xs font-medium" :class="tab === item ? 'border-primary text-primary' : 'border-transparent text-content-tertiary hover:text-content'" @click="tab = item">{{ $t('workflow.studio.config.' + item) }}</button></div>
      <div class="min-h-0 flex-1 overflow-y-auto p-4">
        <MfForm ref="form" :model="values" layout="vertical" :key="`${selectedNode.id}-${tab}`">
          <MfFormItem v-if="tab === 'basic'" :label="$t('workflow.studio.nodeName')"><MfInput :model-value="selectedNode.label" :disabled="readonly" @update:model-value="setLabel"/></MfFormItem>
          <div v-if="tab === 'basic'" class="mb-5 rounded-mf-md bg-surface-hover px-3 py-2 text-xs leading-5 text-content-secondary">{{ nodeDescription }}</div>
          <MfFormItem v-for="field in currentFields" :key="field.field" :name="field.field" :label="fieldLabel(field)" :rules="rulesFor(field)">
            <WorkflowField :field="field" :model-value="values[field.field]" :disabled="readonly" :rows="tab === 'sql' ? (expanded ? 18 : 10) : 4" @update:model-value="update(field.field, $event)"/>
          </MfFormItem>
          <MfEmpty v-if="!currentFields.length && tab !== 'basic'" size="sm" :title="$t('workflow.studio.noOptions')"/>
        </MfForm>
      </div>
      <footer class="flex shrink-0 items-center justify-between gap-2 border-t border-line p-3"><span class="text-xs text-content-tertiary">{{ $t(validated ? 'workflow.studio.fieldsValid' : 'workflow.studio.autoApplied') }}</span><MfButton size="sm" :disabled="readonly" @click="validate"><Check :size="14"/>{{ $t('workflow.studio.checkFields') }}</MfButton></footer>
    </template>
    <MfEmpty v-else class="flex-1" :title="$t('workflow.placeholder.selectNode')" :description="$t('workflow.studio.configEmpty')"/>
  </aside>
</template>
