<template>
  <div ref="canvasWrapRef"
       class="dc-workflow-canvas w-full h-full overflow-auto"
       @drop="onDrop"
       @dragover="onDragOver"
       @click.self="onSelect(null)">
    <div ref="canvasRef"
         class="relative bg-gray-50"
         :style="{
             width: `${ canvasWidth }px`,
             height: `${ canvasHeight }px`,
             backgroundImage: showGrid ? `linear-gradient(to right, rgba(0, 0, 0, 0.06) 1px, transparent 1px), linear-gradient(to bottom, rgba(0, 0, 0, 0.06) 1px, transparent 1px)` : undefined,
             backgroundSize: showGrid ? `${ gridSize }px ${ gridSize }px` : undefined
         }"
         @click.self="onSelect(null)">
      <!-- Edges -->
      <svg class="absolute inset-0 pointer-events-none" style="width: 100%; height: 100%; overflow: visible;">
        <path v-for="connection in connections"
              :key="connection.id"
              :d="edgePaths[connection.id]"
              class="stroke-gray-400"
              fill="none"
              stroke-width="2"/>
        <path v-if="pendingConnection"
              :d="pendingPath"
              class="stroke-[var(--dc-primary)]"
              fill="none"
              stroke-dasharray="6 4"
              stroke-width="2"/>
      </svg>

      <!-- Nodes -->
      <div v-for="node in nodes"
           :key="node.id"
           :data-node-id="node.id"
           class="absolute bg-white rounded-lg shadow-md border select-none"
           :class="selectedNodeId === node.id ? 'border-[var(--dc-primary)] ring-1 ring-[var(--dc-primary-light)]' : 'border-gray-200'"
           :style="{ transform: `translate(${ node.position.x }px, ${ node.position.y }px)`, width: `${ nodeWidth }px` }"
           @mousedown="onNodeMouseDown($event, node)">
        <div class="px-2 pt-1.5 pb-1.5">
          <div class="flex items-center justify-between border-b">
            <div class="flex items-center gap-1.5 py-1.5 min-w-0">
              <component :is="nodeIcon(node)" class="text-gray-500 shrink-0" :style="{ fontSize: '13px' }"/>
              <span class="text-xs text-gray-700 truncate">{{ node.label }}</span>
            </div>
            <span v-if="selectedNodeId === node.id"
                  class="text-gray-400 hover:text-red-500 cursor-pointer px-1 shrink-0"
                  @mousedown.stop
                  @click.stop="onDelete(node)">✕</span>
          </div>

          <div class="flex justify-between items-center py-2 min-h-[28px]">
            <div class="flex flex-col gap-1.5">
              <template v-for="port in inputPorts(node)" :key="port.id">
                <div :data-port-id="`${ node.id }-${ port.id }`"
                     class="dc-workflow-port dc-workflow-port--input"
                     :title="port.label"
                     @mousedown.stop="onPortMouseDown($event, node, port, 'input')">
                </div>
              </template>
            </div>
            <span class="text-[10px] text-gray-300 px-2">{{ node.category }}</span>
            <div class="flex flex-col gap-1.5 items-end">
              <template v-for="port in outputPorts(node)" :key="port.id">
                <div :data-port-id="`${ node.id }-${ port.id }`"
                     class="dc-workflow-port dc-workflow-port--output"
                     :title="port.label"
                     @mousedown.stop="onPortMouseDown($event, node, port, 'output')">
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, watch, nextTick, onMounted, onBeforeUnmount, PropType } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import { NodeInstance, PortItem, WorkflowConnection } from './types'
import { CloudServerOutlined, DatabaseOutlined, SettingOutlined } from '@ant-design/icons-vue'

interface PendingConnection
{
  source: string
  x: number
  y: number
}

export default defineComponent({
  name: 'WorkflowCanvas',
  props: {
    nodes: {
      type: Array as PropType<NodeInstance[]>,
      default: () => []
    },
    connections: {
      type: Array as PropType<WorkflowConnection[]>,
      default: () => []
    },
    selectedNodeId: {
      type: String,
      default: undefined
    },
    canvasWidth: {
      type: Number,
      default: 1920
    },
    canvasHeight: {
      type: Number,
      default: 1080
    },
    showGrid: {
      type: Boolean,
      default: true
    },
    gridSize: {
      type: Number,
      default: 20
    }
  },
  emits: [
    'onNodeMoved',
    'onNodeSelected',
    'onNodeDeleted',
    'onConnectionCreated',
    'onDropNode'
  ],
  setup(props, { emit })
  {
    const canvasWrapRef = ref<HTMLElement | null>(null)
    const canvasRef = ref<HTMLElement | null>(null)
    const nodeWidth = 210

    const edgePaths = ref<Record<string, string>>({})
    const pendingConnection = ref<PendingConnection | null>(null)
    const pendingPath = ref('')

    const inputPorts = (node: NodeInstance): PortItem[] => (node.ports || []).filter((port) => port.type === 'input')
    const outputPorts = (node: NodeInstance): PortItem[] => (node.ports || []).filter((port) => port.type === 'output')

    const nodeIcon = (node: NodeInstance): any => {
      if (node.category === 'source') {
        return DatabaseOutlined
      }
      if (node.category === 'sink') {
        return CloudServerOutlined
      }
      return SettingOutlined
    }

    const portCenter = (portId: string): { x: number; y: number } | null => {
      const element = canvasRef.value?.querySelector(`[data-port-id="${ portId }"]`) as HTMLElement | null
      const canvas = canvasRef.value
      if (!element || !canvas) {
        return null
      }
      const portRect = element.getBoundingClientRect()
      const canvasRect = canvas.getBoundingClientRect()
      return {
        x: portRect.left + portRect.width / 2 - canvasRect.left,
        y: portRect.top + portRect.height / 2 - canvasRect.top
      }
    }

    const bezierPath = (from: { x: number; y: number }, to: { x: number; y: number }): string => {
      const distance = Math.abs(to.x - from.x)
      const middle = distance / 2
      return `M ${ from.x } ${ from.y } C ${ from.x + middle } ${ from.y }, ${ to.x - middle } ${ to.y }, ${ to.x } ${ to.y }`
    }

    const measureEdges = () => {
      const paths: Record<string, string> = {}
      props.connections.forEach((connection) => {
        const from = portCenter(connection.source)
        const to = portCenter(connection.target)
        if (from && to) {
          const start = { x: from.x + 8, y: from.y }
          const end = { x: to.x - 8, y: to.y }
          paths[connection.id] = bezierPath(start, end)
        }
      })
      edgePaths.value = paths
    }

    watch(() => [props.nodes, props.connections], () => {
      nextTick(measureEdges)
    }, { deep: true })

    watch(pendingConnection, () => {
      nextTick(measureEdges)
    })

    onMounted(() => {
      nextTick(measureEdges)
      window.addEventListener('resize', measureEdges)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('resize', measureEdges)
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
    })

    // ---- Node dragging -------------------------------------------------
    let dragging: { node: NodeInstance; offsetX: number; offsetY: number } | null = null

    const onNodeMouseDown = (event: MouseEvent, node: NodeInstance) => {
      onSelect(node)
      const canvas = canvasRef.value
      if (!canvas) {
        return
      }
      const canvasRect = canvas.getBoundingClientRect()
      dragging = {
        node,
        offsetX: event.clientX - canvasRect.left - node.position.x,
        offsetY: event.clientY - canvasRect.top - node.position.y
      }
      window.addEventListener('mousemove', onMouseMove)
      window.addEventListener('mouseup', onMouseUp)
    }

    const onMouseMove = (event: MouseEvent) => {
      const canvas = canvasRef.value
      if (dragging && canvas) {
        const canvasRect = canvas.getBoundingClientRect()
        const x = Math.max(0, event.clientX - canvasRect.left - dragging.offsetX)
        const y = Math.max(0, event.clientY - canvasRect.top - dragging.offsetY)
        dragging.node.position = { x, y }
      }
      else if (pendingConnection.value && canvas) {
        const canvasRect = canvas.getBoundingClientRect()
        pendingConnection.value = {
          ...pendingConnection.value,
          x: event.clientX - canvasRect.left,
          y: event.clientY - canvasRect.top
        }
        const from = portCenter(pendingConnection.value.source)
        if (from) {
          pendingPath.value = bezierPath({ x: from.x + 8, y: from.y }, pendingConnection.value)
        }
      }
    }

    const onMouseUp = () => {
      if (dragging) {
        emit('onNodeMoved', dragging.node)
        dragging = null
      }

      if (pendingConnection.value) {
        pendingConnection.value = null
        pendingPath.value = ''
      }
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
    }

    // ---- Port connection ------------------------------------------------
    const onPortMouseDown = (event: MouseEvent, node: NodeInstance, port: PortItem, direction: 'input' | 'output') => {
      if (direction !== 'output') {
        return
      }
      pendingConnection.value = {
        source: `${ node.id }-${ port.id }`,
        x: event.clientX,
        y: event.clientY
      }
      window.addEventListener('mousemove', onMouseMove)
      window.addEventListener('mouseup', onPortMouseUp)
    }

    const onPortMouseUp = (event: MouseEvent) => {
      const pending = pendingConnection.value
      window.removeEventListener('mouseup', onPortMouseUp)

      if (!pending) {
        return
      }

      const target = document.elementFromPoint(event.clientX, event.clientY) as HTMLElement | null
      const portElement = target?.closest('[data-port-id]') as HTMLElement | null
      const targetPortId = portElement?.getAttribute('data-port-id')

      pendingConnection.value = null
      pendingPath.value = ''

      if (!targetPortId || targetPortId === pending.source) {
        return
      }

      const sourceNodeId = pending.source.split('-')[0]
      const targetNodeId = targetPortId.split('-')[0]
      if (sourceNodeId === targetNodeId) {
        return
      }

      const duplicated = props.connections.some((connection) => connection.source === pending.source && connection.target === targetPortId)
      if (duplicated) {
        return
      }

      emit('onConnectionCreated', { id: uuidv4(), source: pending.source, target: targetPortId } as WorkflowConnection)
    }

    // ---- Selection / deletion / drop ------------------------------------
    const onSelect = (node: NodeInstance | null) => {
      emit('onNodeSelected', node)
    }

    const onDelete = (node: NodeInstance) => {
      emit('onNodeDeleted', node)
    }

    const onDragOver = (event: DragEvent) => {
      event.preventDefault()
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'copy'
      }
    }

    const onDrop = (event: DragEvent) => {
      event.preventDefault()
      const payload = event.dataTransfer?.getData('application/x-workflow-node')
      if (!payload || !canvasRef.value) {
        return
      }

      const canvasRect = canvasRef.value.getBoundingClientRect()
      const { tid } = JSON.parse(payload)
      emit('onDropNode', {
        tid,
        position: {
          x: event.clientX - canvasRect.left - nodeWidth / 2,
          y: event.clientY - canvasRect.top - 20
        }
      })
    }

    return {
      canvasWrapRef,
      canvasRef,
      nodeWidth,
      edgePaths,
      pendingConnection,
      pendingPath,
      inputPorts,
      outputPorts,
      nodeIcon,
      onNodeMouseDown,
      onPortMouseDown,
      onSelect,
      onDelete,
      onDragOver,
      onDrop
    }
  }
})
</script>

<style scoped>
.dc-workflow-port {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #94a3b8;
  cursor: crosshair;
}

.dc-workflow-port:hover {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.dc-workflow-port--input {
  margin-left: -8px;
}

.dc-workflow-port--output {
  margin-right: -8px;
}
</style>
