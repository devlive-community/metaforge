<script setup lang="ts">
import { computed, ref } from 'vue'
import { Database, Download, Search, Settings2 } from '@lucide/vue'
import type { NodeDefinition } from './types'
const props = defineProps<{ nodes: NodeDefinition[]; executor: string; maxTransforms?: number; disabled?: boolean }>()
const emit = defineEmits<{ add: [node: NodeDefinition] }>()
const search = ref('')
const groups = computed(() => ['source', 'transform', 'sink'].map(category => ({ category, nodes: props.nodes.filter(node => node.category === category
  && `${node.label} ${node.key} ${node.description}`.toLowerCase().includes(search.value.trim().toLowerCase())) })).filter(group => group.nodes.length))
function start(event: DragEvent, node: NodeDefinition) {
  if (props.disabled || !event.dataTransfer) { event.preventDefault(); return }
  event.dataTransfer.effectAllowed = 'copy'; event.dataTransfer.setData('application/x-workflow-node', JSON.stringify({ tid: node.id }))
}
</script>
<template>
  <aside class="flex h-full min-h-0 flex-col border-r border-line bg-surface">
    <div class="border-b border-line px-4 py-3"><strong class="text-sm text-content">{{ $t('workflow.studio.library') }}</strong><p class="mt-1 text-xs text-content-tertiary">{{ executor }} · {{ $t('workflow.studio.dragHint') }}</p></div>
    <div class="px-3 pt-3"><MfInput v-model="search" clearable :placeholder="$t('workflow.studio.searchNodes')"><template #prefix><Search :size="14"/></template></MfInput></div>
    <div class="min-h-0 flex-1 space-y-5 overflow-y-auto p-3">
      <section v-for="group in groups" :key="group.category"><h3 class="mb-2 px-1 text-xs font-semibold text-content-tertiary">{{ $t('workflow.studio.category.' + group.category) }}</h3>
        <div class="flex flex-col gap-2"><button v-for="node in group.nodes" :key="node.id" type="button" :disabled="disabled" :draggable="!disabled" @dragstart="start($event, node)" @click="emit('add', node)"
          class="flex items-center gap-3 rounded-mf-md border border-line bg-surface px-3 py-3 text-left transition-colors hover:border-primary/50 hover:bg-primary-light/40 disabled:cursor-not-allowed disabled:opacity-50">
          <span class="rounded-mf-md bg-primary-light p-2 text-primary"><component :is="group.category === 'source' ? Database : group.category === 'sink' ? Download : Settings2" :size="16"/></span>
          <span class="min-w-0"><span class="block truncate text-sm font-medium text-content">{{ node.label }}</span><span class="mt-0.5 block truncate text-[11px] text-content-tertiary">{{ node.key }}</span></span>
        </button></div>
      </section>
      <MfEmpty v-if="!groups.length" size="sm" :title="$t('common.noData')"/>
    </div>
    <p class="border-t border-line px-4 py-3 text-[11px] leading-5 text-content-tertiary">{{ $t(maxTransforms ? 'workflow.studio.pipelineHint' : 'workflow.studio.directPipelineHint', { count: maxTransforms || 0 }) }}</p>
  </aside>
</template>
