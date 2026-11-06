<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Database, Download, Search, Settings2 } from '@lucide/vue'
import { categoryLimit, workflowCategories, type WorkflowCategory, type WorkflowExecutor } from '@/model/workflow'
import DashboardPanelHeader from '@/views/pages/admin/dashboard/components/DashboardPanelHeader.vue'
import type { NodeDefinition } from './types'

const props = defineProps<{ executor: WorkflowExecutor; disabled?: boolean }>()
const emit = defineEmits<{ add: [node: NodeDefinition]; close: [] }>()
const search = ref('')
const category = ref<WorkflowCategory>()
const transforms = computed(() => categoryLimit(props.executor, 'transform'))
const groups = computed(() => workflowCategories(props.executor).map(key => ({
  category: key,
  nodes: props.executor.nodes.filter(node => node.category === key),
})))
const tabsKey = computed(() => props.executor.name + ':' + groups.value.map(group => group.category).join('|'))
const visibleNodes = (key: WorkflowCategory) => groups.value.find(group => group.category === key)?.nodes.filter(node =>
  `${node.label} ${node.key} ${node.description}`.toLowerCase().includes(search.value.trim().toLowerCase())) || []
const categoryIcon = (key: WorkflowCategory) => key === 'source' ? Database : key === 'sink' ? Download : Settings2

watch(() => groups.value.map(group => group.category).join('|'), () => {
  if (!groups.value.some(group => group.category === category.value)) category.value = groups.value[0]?.category
}, { immediate: true })
watch(() => props.executor.name, () => { search.value = ''; category.value = groups.value[0]?.category })

function start(event: DragEvent, node: NodeDefinition) {
  if (props.disabled || !event.dataTransfer) { event.preventDefault(); return }
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-workflow-node', JSON.stringify({ tid: node.id }))
}
</script>

<template>
  <aside class="flex h-full min-h-0 min-w-0 flex-col overflow-hidden border-r border-line bg-surface shadow-mf-sm">
    <DashboardPanelHeader :title="$t('workflow.studio.library')" :description="executor.name + ' · ' + $t('workflow.studio.dragHint')"
                          :pinned="true" :pinnable="false" @close="emit('close')"/>
    <MfTabs v-if="groups.length" :key="tabsKey" v-model:active-key="category" fill class="workflow-library-tabs">
      <MfTabPane v-for="group in groups" :key="group.category" :item-key="group.category" :tab="$t('workflow.studio.category.' + group.category)">
        <template #tab><span class="flex items-center justify-center gap-1.5 text-xs"><component :is="categoryIcon(group.category)" :size="14"/>{{ $t('workflow.studio.category.' + group.category) }}</span></template>
        <section class="flex h-full min-h-0 flex-col">
          <div class="shrink-0 px-3 pb-3">
            <MfInput v-model="search" size="sm" clearable :placeholder="$t('workflow.studio.searchNodes')">
              <template #prefix><Search :size="14"/></template>
            </MfInput>
          </div>
          <div class="min-h-0 flex-1 overflow-y-auto px-3 pb-3">
            <h3 class="mb-2 text-xs font-semibold text-content-secondary">{{ $t('workflow.studio.category.' + group.category) }} ({{ visibleNodes(group.category).length }})</h3>
            <div v-if="visibleNodes(group.category).length" class="grid grid-cols-2 gap-2">
              <button v-for="node in visibleNodes(group.category)" :key="node.id" type="button" :disabled="disabled" :draggable="!disabled"
                      class="flex min-h-20 min-w-0 flex-col items-center justify-center gap-1.5 rounded-mf-md border border-line px-2 py-3 text-center transition-colors hover:border-primary/50 hover:bg-primary/8 disabled:cursor-not-allowed disabled:opacity-50"
                      :title="node.label" @dragstart="start($event, node)" @click="emit('add', node)">
                <component :is="categoryIcon(group.category)" :size="22" class="shrink-0 text-primary"/>
                <span class="w-full truncate text-xs font-semibold text-content">{{ node.label }}</span>
                <span class="w-full truncate text-[10px] text-content-tertiary">{{ node.key }}</span>
              </button>
            </div>
            <MfEmpty v-else class="min-h-32" size="sm" :title="$t('common.noData')"/>
          </div>
        </section>
      </MfTabPane>
    </MfTabs>
    <MfEmpty v-else class="flex-1" size="sm" :title="$t('common.noData')"/>
    <p class="shrink-0 border-t border-line px-3 py-3 text-[11px] leading-5 text-content-tertiary">{{ $t(transforms ? 'workflow.studio.pipelineHint' : 'workflow.studio.directPipelineHint', { count: transforms }) }}</p>
  </aside>
</template>

<style scoped>
.workflow-library-tabs :deep([role='tablist']) { margin-inline: 12px; }
.workflow-library-tabs :deep([role='tab']) { flex: 1 0 auto; padding-inline: 6px; }
.workflow-library-tabs :deep([role='tabpanel']) { overflow: hidden; }
</style>
