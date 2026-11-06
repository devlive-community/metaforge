<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { Database, Download, Settings2, Trash2 } from '@lucide/vue'
import type { NodeInstance, WorkflowConnection } from './types'
import { nodeForPort } from './utils'
const props = withDefaults(defineProps<{ nodes: NodeInstance[]; connections: WorkflowConnection[]; selectedNodeId?: string; showGrid?: boolean; readonly?: boolean }>(), { showGrid: true })
const emit = defineEmits<{
  onNodeMoved: [node: NodeInstance]; onNodeSelected: [node: NodeInstance | null]; onNodeDeleted: [node: NodeInstance]
  onDropNode: [node: { tid: string; position: { x: number; y: number } }]
  onConnectionCreated: [edge: WorkflowConnection]; onConnectionRemoved: [id: string]; zoom: [value: number]
}>()
const root = ref<HTMLElement>(), pan = ref({ x: 50, y: 30 }), scale = ref(1), pending = ref<string>(), selectedEdge = ref<string>()
const pointer = ref({ x: 0, y: 0 }), preview = ref<NodeInstance>()
let automaticFit = true
let observer: ResizeObserver | undefined
const marker = useId().replace(/[^a-z0-9]/gi, '')
const nodes = computed(() => props.nodes.map(node => preview.value?.id === node.id ? preview.value : node))
const local = (event: { clientX: number; clientY: number }) => {
  const rect = root.value!.getBoundingClientRect()
  return { x: (event.clientX - rect.left - pan.value.x) / scale.value, y: (event.clientY - rect.top - pan.value.y) / scale.value }
}
const endpoint = (id: string, output: boolean) => {
  const node = nodeForPort(id, nodes.value)
  return node ? { x: node.position.x + (output ? 210 : 0), y: node.position.y + 44 } : { x: 0, y: 0 }
}
const curve = (from: { x: number; y: number }, to: { x: number; y: number }) => {
  const bend = Math.max(50, Math.abs(to.x - from.x) / 2)
  return `M${from.x},${from.y} C${from.x + bend},${from.y} ${to.x - bend},${to.y} ${to.x},${to.y}`
}
function zoom(value: number) {
  automaticFit = false
  const rect = root.value?.getBoundingClientRect(); if (!rect) return
  const next = Math.max(.25, Math.min(2, value)), ratio = next / scale.value
  pan.value = { x: rect.width / 2 - (rect.width / 2 - pan.value.x) * ratio, y: rect.height / 2 - (rect.height / 2 - pan.value.y) * ratio }
  scale.value = next; emit('zoom', Math.round(next * 100))
}
function fit() {
  automaticFit = true
  if (!root.value || !props.nodes.length) return
  const rect = root.value.getBoundingClientRect()
  const minX = Math.min(...props.nodes.map(node => node.position.x)), minY = Math.min(...props.nodes.map(node => node.position.y))
  const width = Math.max(...props.nodes.map(node => node.position.x + 210)) - minX, height = Math.max(...props.nodes.map(node => node.position.y + 88)) - minY
  scale.value = Math.max(.25, Math.min(1, (rect.width - 100) / width, (rect.height - 100) / height))
  pan.value = { x: (rect.width - width * scale.value) / 2 - minX * scale.value, y: (rect.height - height * scale.value) / 2 - minY * scale.value }
  emit('zoom', Math.round(scale.value * 100))
}
let cleanup: (() => void) | undefined
function drag(event: PointerEvent, node?: NodeInstance) {
  if (event.button !== 0 || (event.target as HTMLElement).closest('button')) return
  if (node && props.readonly) { emit('onNodeSelected', node); return }
  cleanup?.(); pending.value = undefined; selectedEdge.value = undefined
  if (node) emit('onNodeSelected', node)
  else { automaticFit = false; emit('onNodeSelected', null) }
  root.value?.focus({ preventScroll: true })
  const start = { x: event.clientX, y: event.clientY }, origin = { ...(node?.position ?? pan.value) }
  const move = (e: PointerEvent) => {
    const dx = e.clientX - start.x, dy = e.clientY - start.y
    if (node) preview.value = { ...node, position: { x: Math.round((origin.x + dx / scale.value) / 10) * 10, y: Math.round((origin.y + dy / scale.value) / 10) * 10 } }
    else pan.value = { x: origin.x + dx, y: origin.y + dy }
  }
  const finish = () => {
    if (preview.value) emit('onNodeMoved', preview.value)
    preview.value = undefined; cleanup?.()
  }
  cleanup = () => { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', finish); window.removeEventListener('pointercancel', finish); cleanup = undefined }
  window.addEventListener('pointermove', move); window.addEventListener('pointerup', finish); window.addEventListener('pointercancel', finish)
}
function port(node: NodeInstance, type: 'input' | 'output') {
  if (props.readonly) return
  const id = `${node.id}-${type}`
  if (type === 'output') { pending.value = pending.value === id ? undefined : id; pointer.value = endpoint(id, true) }
  else if (pending.value) {
    emit('onConnectionCreated', { id: crypto.randomUUID(), source: pending.value, target: id }); pending.value = undefined
  }
}
function wheel(event: WheelEvent) {
  if (event.ctrlKey || event.metaKey) zoom(scale.value - event.deltaY * .002)
  else { automaticFit = false; pan.value = { x: pan.value.x - event.deltaX, y: pan.value.y - event.deltaY } }
}
function drop(event: DragEvent) {
  if (props.readonly) return
  const raw = event.dataTransfer?.getData('application/x-workflow-node'); if (!raw) return
  try {
    const { tid } = JSON.parse(raw); if (typeof tid !== 'string') return
    const point = local(event); emit('onDropNode', { tid, position: { x: Math.round(point.x - 105), y: Math.round(point.y - 44) } })
  } catch { /* Ignore unrelated drag payloads. */ }
}
function key(event: KeyboardEvent) {
  if ((event.target as HTMLElement).closest('button,input,textarea,select') || props.readonly) return
  if (event.key === 'Escape') { pending.value = undefined; selectedEdge.value = undefined; emit('onNodeSelected', null) }
  if (['Delete', 'Backspace'].includes(event.key)) {
    event.preventDefault()
    if (selectedEdge.value) { emit('onConnectionRemoved', selectedEdge.value); selectedEdge.value = undefined }
    else { const node = props.nodes.find(node => node.id === props.selectedNodeId); if (node) emit('onNodeDeleted', node) }
  }
}
watch(() => props.nodes.length, (length, previous) => { if (!previous && length) nextTick(fit) })
onMounted(() => { nextTick(fit); observer = new ResizeObserver(() => { if (automaticFit) fit() }); if (root.value) observer.observe(root.value) }); onBeforeUnmount(() => { cleanup?.(); observer?.disconnect() })
defineExpose({ fit, zoom })
</script>

<template>
  <div ref="root" tabindex="0" class="workflow-canvas relative h-full w-full overflow-hidden bg-surface-hover outline-none"
       :class="showGrid && 'workflow-grid'" :style="showGrid ? { backgroundSize: `${20 * scale}px ${20 * scale}px`, backgroundPosition: `${pan.x}px ${pan.y}px` } : undefined" @pointerdown.self="drag($event)" @keydown="key" @wheel.prevent="wheel"
       @pointermove="pending && (pointer = local($event))" @dragover.prevent @drop.prevent="drop">
    <div class="absolute left-0 top-0 h-0 w-0 origin-top-left" :style="{ transform: `translate(${pan.x}px, ${pan.y}px) scale(${scale})` }">
      <svg class="pointer-events-none absolute h-px w-px overflow-visible" aria-hidden="true">
        <defs><marker :id="marker" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0L10 5L0 10Z" fill="var(--mf-color-primary)" /></marker></defs>
        <g v-for="edge in connections" :key="edge.id">
          <path :d="curve(endpoint(edge.source, true), endpoint(edge.target, false))" fill="none" stroke="var(--mf-color-primary)" :stroke-width="selectedEdge === edge.id ? 3 : 1.8" :marker-end="`url(#${marker})`" />
          <path :d="curve(endpoint(edge.source, true), endpoint(edge.target, false))" fill="none" stroke="transparent" stroke-width="15" class="pointer-events-auto cursor-pointer" @pointerdown.stop="selectedEdge = edge.id; emit('onNodeSelected', null); root?.focus()" />
        </g>
        <path v-if="pending" :d="curve(endpoint(pending, true), pointer)" stroke="var(--mf-color-primary)" stroke-dasharray="5 5" fill="none" />
      </svg>
      <div v-for="node in nodes" :key="node.id" :data-node-id="node.id" role="button" tabindex="0" :aria-label="node.label"
           class="absolute flex h-[88px] w-[210px] cursor-grab select-none items-center gap-3 rounded-mf-lg border bg-surface px-4 shadow-mf-sm transition-[border-color,box-shadow]"
           :class="selectedNodeId === node.id ? 'border-primary shadow-[0_0_0_2px_var(--mf-color-primary-light)]' : 'border-line hover:border-primary/50'"
           :style="{ left: node.position.x + 'px', top: node.position.y + 'px' }" @pointerdown.stop="drag($event, node)" @keydown.enter.stop.prevent="emit('onNodeSelected', node)">
        <div class="rounded-mf-md bg-primary-light p-2 text-primary"><component :is="node.category === 'source' ? Database : node.category === 'sink' ? Download : Settings2" :size="17" /></div>
        <div class="min-w-0 flex-1"><div class="truncate text-sm font-semibold text-content">{{ node.label }}</div><div class="mt-1 truncate text-xs text-content-tertiary">{{ node.key }}</div></div>
        <button v-if="selectedNodeId === node.id && !readonly" type="button" class="absolute right-1 top-1 rounded p-1 text-content-tertiary hover:bg-danger-light hover:text-danger" :aria-label="$t('common.delete')" @click.stop="emit('onNodeDeleted', node)"><Trash2 :size="12"/></button>
        <button v-for="item in node.ports || []" :key="item.id" type="button" :aria-label="`${node.label} ${item.type}`" class="absolute top-[36px] h-4 w-4 rounded-full border-2 border-primary bg-surface hover:bg-primary-light"
                :class="item.type === 'input' ? '-left-2' : '-right-2'" @click.stop="port(node, item.type)" />
      </div>
    </div>
    <div v-if="!nodes.length" class="pointer-events-none absolute inset-0 flex items-center justify-center"><MfEmpty :title="$t('workflow.studio.emptyCanvas')" :description="$t('workflow.studio.dragHint')" /></div>
    <div v-if="pending" class="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-mf-md border border-primary/20 bg-surface px-3 py-2 text-xs text-primary shadow-mf-sm">{{ $t('workflow.studio.connectHint') }}</div>
    <MfButton v-if="selectedEdge && !readonly" class="absolute bottom-3 right-3" size="sm" @click="emit('onConnectionRemoved', selectedEdge); selectedEdge = undefined"><Trash2 :size="14"/>{{ $t('workflow.studio.deleteConnection') }}</MfButton>
  </div>
</template>
<style scoped>
.workflow-grid { background-image: radial-gradient(var(--mf-color-line) 1px, transparent 1px); background-size: 20px 20px; }
.workflow-canvas { touch-action: none; }
</style>
