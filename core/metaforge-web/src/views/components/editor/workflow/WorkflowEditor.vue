<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronLeft, ChevronRight, Grid2X2, Maximize, Minus, Plus, Redo2, Undo2, Workflow } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { usePanelWidth } from '@/composables/usePanelWidth'
import { categoryLimit, type WorkflowExecutor, type WorkflowGraph } from '@/model/workflow'
import type { NodeInstance, WorkflowConnection } from './types'
import { arrangeNodes, canConnect, defaultValues, flattenNodes, mergeDefinitions, upstreamOf } from './utils'
import WorkflowCanvas from './WorkflowCanvas.vue'
import WorkflowConfigure from './WorkflowConfigure.vue'
import WorkflowPanel from './WorkflowPanel.vue'

const props = defineProps<{ modelValue: WorkflowGraph; executor: WorkflowExecutor; readonly?: boolean; requestedNode?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: WorkflowGraph] }>()
const { t } = useI18n()
const canvas = ref<InstanceType<typeof WorkflowCanvas>>()
const viewportWidth = ref(window.innerWidth)
const wide = computed(() => viewportWidth.value >= 1024)
type Panel = 'library' | 'properties'
const desktopPanels = reactive<Record<Panel, boolean>>({ library: true, properties: true })
const compactPanel = ref<Panel>()
const libraryVisible = computed(() => wide.value ? desktopPanels.library : compactPanel.value === 'library')
const propertiesVisible = computed(() => wide.value ? desktopPanels.properties : compactPanel.value === 'properties')
const grid = ref(true), zoom = ref(100), selected = ref<string>(), resizing = ref(false)
const libraryWidth = usePanelWidth('workflow.library.width', { initial: 320, min: 240, max: 520 })
const inspectorWidth = usePanelWidth('workflow.inspector.width', { initial: 320, min: 280, max: 520 })
const panelMax = computed(() => Math.min(520, Math.max(280, Math.floor((viewportWidth.value - 240) / 2))))
const displayedLibraryWidth = computed(() => Math.min(libraryWidth.width.value, wide.value ? panelMax.value : viewportWidth.value * .85))
const displayedInspectorWidth = computed(() => Math.min(inspectorWidth.width.value, wide.value ? panelMax.value : viewportWidth.value * .85))
const nodes = computed(() => mergeDefinitions(props.executor.nodes, props.modelValue.nodes || []))
const selectedNode = computed(() => nodes.value.find(node => node.id === selected.value) || null)
const upstreamNode = computed(() => upstreamOf(nodes.value, props.modelValue.connections || [], selected.value) || null)
const past = ref<string[]>([]), future = ref<string[]>([])
const snapshot = () => JSON.stringify(props.modelValue)
const onResize = () => { viewportWidth.value = window.innerWidth }
const endResize = () => { resizing.value = false }
window.addEventListener('resize', onResize)
window.addEventListener('pointerup', endResize)
window.addEventListener('pointercancel', endResize)
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('pointerup', endResize)
  window.removeEventListener('pointercancel', endResize)
})

function showPanel(panel: Panel) {
  if (wide.value) desktopPanels[panel] = true
  else compactPanel.value = panel
}
function closePanel(panel: Panel) {
  if (wide.value) desktopPanels[panel] = false
  else if (compactPanel.value === panel) compactPanel.value = undefined
  nextTick(() => document.getElementById(`workflow-${panel}-toggle`)?.focus({ preventScroll: true }))
}
function togglePanel(panel: Panel) {
  const visible = panel === 'library' ? libraryVisible.value : propertiesVisible.value
  if (visible) closePanel(panel)
  else showPanel(panel)
}
function closeCompactPanel() { if (compactPanel.value) closePanel(compactPanel.value) }
function selectNode(node: NodeInstance | null) {
  selected.value = node?.id
  if (node) showPanel('properties')
}
function selectById(id: string) { selected.value = id; showPanel('properties') }
function commit(value: WorkflowGraph) {
  if (props.readonly) return
  past.value.push(snapshot())
  if (past.value.length > 60) past.value.shift()
  future.value = []
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
  update([...nodes.value, node])
  selectNode(node)
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
watch(() => props.requestedNode, value => { if (value) selectById(value) }, { immediate: true })
defineExpose({ undo, redo, select: selectById })
</script>

<template>
  <div class="workflow-studio-workspace relative flex h-full min-h-0 min-w-0 overflow-hidden"
       :class="resizing && 'workflow-resizing'" :style="{ '--workflow-library-width': displayedLibraryWidth + 'px', '--workflow-properties-width': displayedInspectorWidth + 'px' }"
       @keydown.esc="!wide && closeCompactPanel()">
    <Transition name="workflow-panel-slide">
      <div v-show="libraryVisible" id="workflow-library" :inert="!libraryVisible"
           class="workflow-side-panel workflow-library-panel inset-y-0 left-0 z-20 outline-none" :class="wide ? 'workflow-panel-docked relative' : 'absolute shadow-mf-lg'">
        <WorkflowPanel class="workflow-panel-content" :executor="executor" :disabled="readonly" @add="add({ tid: $event.id })" @close="closePanel('library')"/>
        <MfResizeHandle v-if="wide" v-model="libraryWidth.width.value" class="absolute inset-y-0 -right-1 z-10 mx-0!" :min="240" :max="panelMax"
                        :label="$t('workflow.studio.resizeLibrary')" @pointerdown.capture="resizing = true" @change="libraryWidth.save"/>
      </div>
    </Transition>

    <div class="flex min-h-0 min-w-0 flex-1 flex-col" :inert="!wide && !!compactPanel">
      <Teleport to="#workflow-tools">
        <div class="flex min-h-11 w-full shrink-0 flex-wrap items-center justify-between gap-2 bg-surface px-2 py-1 sm:px-4">
          <div class="flex items-center gap-1">
            <MfButton variant="ghost" size="sm" :disabled="readonly || !past.length" :aria-label="$t('workflow.studio.undo')" @click="undo"><Undo2 :size="15"/></MfButton>
            <MfButton variant="ghost" size="sm" :disabled="readonly || !future.length" :aria-label="$t('workflow.studio.redo')" @click="redo"><Redo2 :size="15"/></MfButton>
            <MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.zoomOut')" @click="canvas?.zoom(zoom / 100 - .1)"><Minus :size="14"/></MfButton>
            <span class="w-10 text-center text-xs text-content-secondary">{{ zoom }}%</span>
            <MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.zoomIn')" @click="canvas?.zoom(zoom / 100 + .1)"><Plus :size="14"/></MfButton>
            <MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.fit')" @click="canvas?.fit()"><Maximize :size="15"/></MfButton>
          </div>
          <div class="flex items-center gap-1">
            <MfTooltip :content="$t('workflow.studio.arrange')"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.arrange')" :disabled="readonly || !nodes.length" @click="update(arrangeNodes(nodes, modelValue.connections)); nextTick(() => canvas?.fit())"><Workflow :size="15"/></MfButton></MfTooltip>
            <MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.grid')" :class="grid && 'text-primary'" @click="grid = !grid"><Grid2X2 :size="15"/></MfButton>
          </div>
        </div>
      </Teleport>
      <WorkflowCanvas ref="canvas" class="min-h-0 flex-1" :nodes="nodes" :connections="modelValue.connections" :selected-node-id="selected" :show-grid="grid" :readonly="readonly" @zoom="zoom = $event"
                      @on-drop-node="add" @on-node-selected="selectNode" @on-node-moved="update(nodes.map(node => node.id === $event.id ? $event : node))" @on-node-deleted="remove"
                      @on-connection-created="connect" @on-connection-removed="update(nodes, modelValue.connections.filter(edge => edge.id !== $event))"/>
      <slot name="bottom"/>
    </div>

    <Transition name="workflow-panel-backdrop">
      <button v-if="!wide && compactPanel" type="button" tabindex="-1" aria-hidden="true" class="absolute inset-0 z-10 bg-black/25" @click="closeCompactPanel"/>
    </Transition>
    <Transition name="workflow-panel-slide">
      <div v-show="propertiesVisible" id="workflow-properties" :inert="!propertiesVisible"
           class="workflow-side-panel workflow-properties-panel inset-y-0 right-0 z-20 outline-none" :class="wide ? 'workflow-panel-docked relative' : 'absolute shadow-mf-lg'">
        <WorkflowConfigure class="workflow-panel-content" :selected-node="selectedNode" :upstream-node="upstreamNode" :readonly="readonly"
                           @update:node="update(nodes.map(node => node.id === $event.id ? $event : node))" @close="closePanel('properties')"/>
        <MfResizeHandle v-if="wide" v-model="inspectorWidth.width.value" class="absolute inset-y-0 -left-1 z-10 mx-0!" :min="280" :max="panelMax" reverse
                        :label="$t('workflow.studio.resizeInspector')" @pointerdown.capture="resizing = true" @change="inspectorWidth.save"/>
      </div>
    </Transition>

    <MfButton id="workflow-library-toggle" size="sm" class="workflow-panel-toggle rounded-l-none! rounded-r-mf-md!"
              :style="{ left: (libraryVisible ? displayedLibraryWidth : 0) + 'px' }" aria-controls="workflow-library" :aria-expanded="libraryVisible"
              :aria-label="$t('workflow.studio.library')" :title="$t(libraryVisible ? 'dashboard.studio.hideLibrary' : 'dashboard.studio.showLibrary')" @click="togglePanel('library')">
      <ChevronLeft v-if="libraryVisible" :size="15"/><ChevronRight v-else :size="15"/>
    </MfButton>
    <MfButton id="workflow-properties-toggle" size="sm" class="workflow-panel-toggle rounded-l-mf-md! rounded-r-none!"
              :style="{ right: (propertiesVisible ? displayedInspectorWidth : 0) + 'px' }" aria-controls="workflow-properties" :aria-expanded="propertiesVisible"
              :aria-label="$t('workflow.studio.nodeConfig')" :title="$t(propertiesVisible ? 'dashboard.studio.hideProperties' : 'dashboard.studio.showProperties')" @click="togglePanel('properties')">
      <ChevronRight v-if="propertiesVisible" :size="15"/><ChevronLeft v-else :size="15"/>
    </MfButton>
  </div>
</template>

<style scoped>
.workflow-side-panel { display: flex; flex-shrink: 0; width: var(--workflow-panel-width); }
.workflow-library-panel { --workflow-panel-width: var(--workflow-library-width); --workflow-panel-offset: -100%; }
.workflow-properties-panel { --workflow-panel-width: var(--workflow-properties-width); --workflow-panel-offset: 100%; justify-content: flex-end; }
.workflow-panel-content { width: var(--workflow-panel-width); flex-shrink: 0; }
.workflow-panel-toggle { position: absolute; top: 50%; z-index: 30; width: 25px; height: 46px; padding: 0; transform: translateY(-50%); }
@media (prefers-reduced-motion: no-preference) {
  .workflow-panel-slide-enter-active, .workflow-panel-slide-leave-active { transition: transform var(--mf-duration-base) var(--mf-easing); }
  .workflow-panel-slide-enter-active.workflow-panel-docked, .workflow-panel-slide-leave-active.workflow-panel-docked { overflow: clip; transition: width var(--mf-duration-base) var(--mf-easing); }
  .workflow-panel-slide-enter-from:not(.workflow-panel-docked), .workflow-panel-slide-leave-to:not(.workflow-panel-docked) { transform: translateX(var(--workflow-panel-offset)); }
  .workflow-panel-slide-enter-from.workflow-panel-docked, .workflow-panel-slide-leave-to.workflow-panel-docked { width: 0; }
  .workflow-panel-backdrop-enter-active, .workflow-panel-backdrop-leave-active { transition: opacity var(--mf-duration-base) var(--mf-easing); }
  .workflow-panel-backdrop-enter-from, .workflow-panel-backdrop-leave-to { opacity: 0; }
  .workflow-panel-toggle { transition: left var(--mf-duration-base) var(--mf-easing), right var(--mf-duration-base) var(--mf-easing); }
  .workflow-resizing .workflow-panel-toggle { transition: none; }
}
</style>
