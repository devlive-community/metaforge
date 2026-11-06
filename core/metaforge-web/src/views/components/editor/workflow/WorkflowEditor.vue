<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Grid2X2, Maximize, Minus, PanelLeftClose, PanelRightClose, Plus, Redo2, Undo2, Workflow } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { usePanelWidth } from '@/composables/usePanelWidth'
import { categoryLimit, type WorkflowExecutor, type WorkflowGraph } from '@/model/workflow'
import type { NodeInstance, WorkflowConnection } from './types'
import { arrangeNodes, canConnect, defaultValues, flattenNodes, mergeDefinitions } from './utils'
import WorkflowCanvas from './WorkflowCanvas.vue'
import WorkflowConfigure from './WorkflowConfigure.vue'
import WorkflowPanel from './WorkflowPanel.vue'
const props = defineProps<{ modelValue: WorkflowGraph; executor: WorkflowExecutor; readonly?: boolean; expanded?: boolean; requestedNode?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: WorkflowGraph]; expand: [node?: string] }>()
const { t } = useI18n()
const canvas = ref<InstanceType<typeof WorkflowCanvas>>()
const viewportWidth = ref(window.innerWidth)
const wide = computed(() => viewportWidth.value >= 1100)
const onResize = () => { viewportWidth.value = window.innerWidth }
window.addEventListener('resize', onResize); onBeforeUnmount(() => window.removeEventListener('resize', onResize))
const left = ref(wide.value), right = ref(wide.value), grid = ref(true), zoom = ref(100), selected = ref<string>()
const libraryWidth = usePanelWidth('workflow.library.width', { initial: 248, min: 200, max: 360 })
const inspectorWidth = usePanelWidth('workflow.inspector.width', { initial: 340, min: 280, max: 520 })
const nodes = computed(() => mergeDefinitions(props.executor.nodes, props.modelValue.nodes || []))
const selectedNode = computed(() => nodes.value.find(node => node.id === selected.value) || null)
const past = ref<string[]>([]), future = ref<string[]>([])
const snapshot = () => JSON.stringify(props.modelValue)
function commit(value: WorkflowGraph) {
  if (props.readonly) return
  past.value.push(snapshot()); if (past.value.length > 60) past.value.shift(); future.value = []
  emit('update:modelValue', value)
}
function update(nodesValue: NodeInstance[], connections = props.modelValue.connections) {
  commit({ ...props.modelValue, nodes: flattenNodes(nodesValue), connections })
}
function undo() { if (props.readonly || !past.value.length) return; future.value.push(snapshot()); emit('update:modelValue', JSON.parse(past.value.pop()!)) }
function redo() { if (props.readonly || !future.value.length) return; past.value.push(snapshot()); emit('update:modelValue', JSON.parse(future.value.pop()!)) }
function add(payload: { tid: string; position?: { x: number; y: number } }) {
  const definition = props.executor.nodes.find(node => node.id === payload.tid)
  if (!definition) return
  const count = nodes.value.filter(node => node.category === definition.category).length
  const max = categoryLimit(props.executor, definition.category)
  if (count >= max) { MfMessage.warning(t('workflow.studio.nodeLimit', { count: max })); return }
  const node: NodeInstance = { id: crypto.randomUUID(), tid: definition.id, label: definition.label, key: definition.key, category: definition.category,
    position: payload.position || { x: 60 + nodes.value.length * 270, y: 180 }, data: defaultValues(definition.configure) }
  update([...nodes.value, node]); selected.value = node.id; right.value = true
  if (!payload.position) nextTick(() => canvas.value?.fit())
}
function remove(node: NodeInstance) {
  update(nodes.value.filter(item => item.id !== node.id), props.modelValue.connections.filter(edge => !['input', 'output'].some(port => edge.source === `${node.id}-${port}` || edge.target === `${node.id}-${port}`)))
  if (selected.value === node.id) selected.value = undefined
}
function connect(edge: WorkflowConnection) {
  if (!canConnect(nodes.value, props.modelValue.connections, edge.source, edge.target)) { MfMessage.warning(t('workflow.studio.invalidConnection')); return }
  update(nodes.value, [...props.modelValue.connections, edge])
}
watch(() => props.executor.name, () => { selected.value = undefined; past.value = []; future.value = [] })
watch(() => props.requestedNode, value => { if (value) { selected.value = value; right.value = true } }, { immediate: true })
defineExpose({ undo, redo, select: (id: string) => { selected.value = id; right.value = true } })
</script>
<template>
  <div class="relative flex h-full min-h-0 min-w-0 overflow-hidden">
    <WorkflowPanel v-if="left" class="shrink-0" :class="!wide && 'absolute inset-y-0 left-0 z-20 shadow-mf-lg'" :style="{ width: libraryWidth.width.value + 'px' }" :executor="executor" :disabled="readonly" @add="add({ tid: $event.id })"/>
    <MfResizeHandle v-if="left && wide" v-model="libraryWidth.width.value" :min="200" :max="360" :label="$t('workflow.studio.resizeLibrary')" @change="libraryWidth.save"/>
    <div class="flex min-w-0 flex-1 flex-col" :inert="!wide && (left || right)">
      <Teleport to="#workflow-tools"><div class="flex min-h-11 w-full shrink-0 flex-wrap items-center justify-between gap-2 bg-surface px-2 py-1 sm:px-4">
        <div class="flex items-center gap-1"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.library')" @click="left = !left; if (!wide && left) right = false"><PanelLeftClose :size="16"/></MfButton><MfButton variant="ghost" size="sm" :disabled="readonly || !past.length" :aria-label="$t('workflow.studio.undo')" @click="undo"><Undo2 :size="15"/></MfButton><MfButton variant="ghost" size="sm" :disabled="readonly || !future.length" :aria-label="$t('workflow.studio.redo')" @click="redo"><Redo2 :size="15"/></MfButton>
          <MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.zoomOut')" @click="canvas?.zoom(zoom / 100 - .1)"><Minus :size="14"/></MfButton><span class="w-10 text-center text-xs text-content-secondary">{{ zoom }}%</span><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.zoomIn')" @click="canvas?.zoom(zoom / 100 + .1)"><Plus :size="14"/></MfButton><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.fit')" @click="canvas?.fit()"><Maximize :size="15"/></MfButton>
        </div>
        <div class="flex items-center gap-1"><MfTooltip :content="$t('workflow.studio.arrange')"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.arrange')" :disabled="readonly || !nodes.length" @click="update(arrangeNodes(nodes, modelValue.connections)); nextTick(() => canvas?.fit())"><Workflow :size="15"/></MfButton></MfTooltip><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.grid')" :class="grid && 'text-primary'" @click="grid = !grid"><Grid2X2 :size="15"/></MfButton><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.nodeConfig')" @click="right = !right; if (!wide && right) left = false"><PanelRightClose :size="16"/></MfButton></div>
      </div>
      </Teleport>
      <WorkflowCanvas ref="canvas" class="min-h-0 flex-1" :nodes="nodes" :connections="modelValue.connections" :selected-node-id="selected" :show-grid="grid" :readonly="readonly" @zoom="zoom = $event"
        @on-drop-node="add" @on-node-selected="selected = $event?.id; if ($event) { right = true; if (!wide) left = false }" @on-node-moved="update(nodes.map(node => node.id === $event.id ? $event : node))" @on-node-deleted="remove" @on-connection-created="connect" @on-connection-removed="update(nodes, modelValue.connections.filter(edge => edge.id !== $event))"/>
      <slot name="bottom"/>
    </div>
    <div v-if="!wide && (left || right)" class="absolute inset-0 z-10" role="button" tabindex="0" :aria-label="$t('workflow.studio.closePanels')" @pointerdown="left = false; right = false" @keydown.enter="left = false; right = false"/>
    <MfResizeHandle v-if="right && wide" v-model="inspectorWidth.width.value" :min="280" :max="520" reverse :label="$t('workflow.studio.resizeInspector')" @change="inspectorWidth.save"/>
    <WorkflowConfigure v-if="right" class="shrink-0" :class="!wide && 'absolute inset-y-0 right-0 z-20 shadow-mf-lg'" :style="{ width: Math.min(viewportWidth - 24, expanded ? Math.max(440, inspectorWidth.width.value) : inspectorWidth.width.value) + 'px' }" :selected-node="selectedNode" :expanded="expanded" :readonly="readonly" @update:node="update(nodes.map(node => node.id === $event.id ? $event : node))" @close="right = false" @expand="emit('expand', selected)"/>
  </div>
</template>
