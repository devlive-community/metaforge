<template>
  <div class="dc-workflow-view w-full h-full overflow-auto outline-none">
    <div ref="canvasRef"
         class="relative bg-gray-50"
         :style="{
             width: `${ canvasWidth }px`,
             height: `${ canvasHeight }px`,
             backgroundImage: showGrid ? `linear-gradient(to right, rgba(0, 0, 0, 0.06) 1px, transparent 1px), linear-gradient(to bottom, rgba(0, 0, 0, 0.06) 1px, transparent 1px)` : undefined,
             backgroundSize: showGrid ? `${ gridSize }px ${ gridSize }px` : undefined
         }">
      <!-- Edges -->
      <svg class="absolute inset-0 pointer-events-none" style="width: 100%; height: 100%; overflow: visible;">
        <path v-for="connection in connections"
              :key="connection.id"
              :d="edgePaths[connection.id]"
              class="stroke-gray-400"
              fill="none"
              stroke-width="2"/>
      </svg>

      <!-- Nodes (read-only) -->
      <div v-for="node in mergedNodes"
           :key="node.id"
           class="absolute bg-white rounded-lg shadow-md border border-gray-200"
           :style="{ transform: `translate(${ node.position?.x || 0 }px, ${ node.position?.y || 0 }px)`, width: `${ nodeWidth }px` }">
        <div class="px-2 pt-1.5 pb-1.5">
          <div class="flex items-center justify-between border-b">
            <div class="flex items-center gap-1.5 py-1.5 min-w-0">
              <component :is="nodeIcon(node)" class="text-gray-500 shrink-0" :style="{ fontSize: '13px' }"/>
              <span class="text-xs text-gray-700 truncate">{{ node.label }}</span>
            </div>
            <span class="text-[10px] text-gray-300 px-1 shrink-0">{{ node.category }}</span>
          </div>

          <div class="flex justify-between items-center py-2 min-h-[28px]">
            <div class="flex flex-col gap-1.5">
              <div v-for="port in inputPorts(node)"
                   :key="port.id"
                   :data-port-id="`${ node.id }-${ port.id }`"
                   class="dc-workflow-port"
                   :title="port.label"></div>
            </div>
            <div class="flex flex-col gap-1.5 items-end">
              <div v-for="port in outputPorts(node)"
                   :key="port.id"
                   :data-port-id="`${ node.id }-${ port.id }`"
                   class="dc-workflow-port"
                   :title="port.label"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch, nextTick, onMounted, onBeforeUnmount, PropType } from 'vue'
import { NodeDefinition, NodeInstance, WorkflowConnection } from './types'
import { mergeDefinitions } from './utils'
import { CloudServerOutlined, DatabaseOutlined, SettingOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  name: 'WorkflowView',
  props: {
    nodes: {
      type: Array as PropType<NodeDefinition[]>,
      default: () => []
    },
    data: {
      type: Object,
      default: () => ({ nodes: [], connections: [] })
    },
    canvas: {
      type: Object,
      default: () => ({})
    }
  },
  setup(props)
  {
    const canvasRef = ref<HTMLElement | null>(null)
    const nodeWidth = 210

    const canvasWidth = computed(() => props.canvas?.width || 1920)
    const canvasHeight = computed(() => props.canvas?.height || 1080)
    const showGrid = computed(() => props.canvas?.showGrid !== false)
    const gridSize = computed(() => props.canvas?.gridSize || 20)

    const connections = computed<WorkflowConnection[]>(() => props.data?.connections || [])

    const mergedNodes = computed<NodeInstance[]>(() => {
      return mergeDefinitions(props.nodes, props.data?.nodes || [])
    })

    const inputPorts = (node: NodeInstance) => (node.ports || []).filter((port) => port.type === 'input')
    const outputPorts = (node: NodeInstance) => (node.ports || []).filter((port) => port.type === 'output')

    const nodeIcon = (node: NodeInstance): any => {
      if (node.category === 'source') {
        return DatabaseOutlined
      }
      if (node.category === 'sink') {
        return CloudServerOutlined
      }
      return SettingOutlined
    }

    const edgePaths = ref<Record<string, string>>({})

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

    const measureEdges = () => {
      const paths: Record<string, string> = {}
      connections.value.forEach((connection) => {
        const from = portCenter(connection.source)
        const to = portCenter(connection.target)
        if (from && to) {
          const distance = Math.abs(to.x - from.x)
          const middle = distance / 2
          const start = { x: from.x + 8, y: from.y }
          const end = { x: to.x - 8, y: to.y }
          paths[connection.id] = `M ${ start.x } ${ start.y } C ${ start.x + middle } ${ start.y }, ${ end.x - middle } ${ end.y }, ${ end.x } ${ end.y }`
        }
      })
      edgePaths.value = paths
    }

    watch([mergedNodes, connections], () => {
      nextTick(measureEdges)
    }, { deep: true })

    onMounted(() => {
      nextTick(measureEdges)
      window.addEventListener('resize', measureEdges)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('resize', measureEdges)
    })

    return {
      canvasRef,
      nodeWidth,
      canvasWidth,
      canvasHeight,
      showGrid,
      gridSize,
      connections,
      mergedNodes,
      edgePaths,
      inputPorts,
      outputPorts,
      nodeIcon
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
}

.dc-workflow-port--input {
  margin-left: -8px;
}

.dc-workflow-port--output {
  margin-right: -8px;
}
</style>
