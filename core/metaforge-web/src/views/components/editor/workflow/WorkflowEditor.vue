<template>
  <div class="flex w-full h-full">
    <WorkflowPanel class="border-r shrink-0"
                   :categories="categories"
                   :nodes="nodes"
                   :search-text="searchText"
                   :width="panelWidth"
                   @update:searchText="onSearchText"/>

    <div class="flex-1 min-w-0">
      <WorkflowCanvas :nodes="instanceNodes"
                      :connections="instanceConnections"
                      :selected-node-id="selectedNode?.id"
                      @onDropNode="onDropNode"
                      @onNodeMoved="onNodeMoved"
                      @onNodeSelected="onNodeSelected"
                      @onNodeDeleted="onNodeDeleted"
                      @onConnectionCreated="onConnectionCreated"/>
    </div>

    <WorkflowConfigure class="shrink-0"
                       :selected-node="selectedNode"
                       :width="configureWidth"
                       @update:node="onNodeUpdated"/>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, watch, PropType } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import WorkflowPanel from './WorkflowPanel.vue'
import WorkflowCanvas from './WorkflowCanvas.vue'
import WorkflowConfigure from './WorkflowConfigure.vue'
import {
  CategoryDefinition,
  NodeDefinition,
  NodeInstance,
  WorkflowConnection,
  WorkflowEditorModel
} from './types'
import { flattenNodes, mergeDefinitions, validateWorkflow } from './utils'

export default defineComponent({
  name: 'WorkflowEditor',
  components: { WorkflowPanel, WorkflowCanvas, WorkflowConfigure },
  props: {
    modelValue: {
      type: Object as PropType<WorkflowEditorModel>,
      default: () => ({ nodes: [], connections: [] })
    },
    nodes: {
      type: Array as PropType<NodeDefinition[]>,
      default: () => []
    },
    connections: {
      type: Array as PropType<WorkflowConnection[]>,
      default: () => []
    },
    categories: {
      type: Array as PropType<CategoryDefinition[]>,
      default: () => []
    },
    searchText: {
      type: String,
      default: ''
    },
    panelWidth: {
      type: Number,
      default: 250
    },
    configureWidth: {
      type: Number,
      default: 300
    }
  },
  emits: [
    'update:modelValue',
    'update:searchText',
    'onNodeMoved',
    'onNodeAdded',
    'onNodeSelected',
    'onNodeDeleted',
    'onConnectionCreated',
    'onConnectionRemoved'
  ],
  setup(props, { emit })
  {
    const instanceNodes = ref<NodeInstance[]>([])
    const instanceConnections = ref<WorkflowConnection[]>([])
    const selectedNode = ref<NodeInstance | null>(null)

    /**
     * Emits the current editor state, keeping the persisted shape:
     * { nodes, connections, data: { nodes: flattened, connections }, validation }.
     */
    const commit = () => {
      emit('update:modelValue', {
        nodes: instanceNodes.value,
        connections: instanceConnections.value,
        data: {
          nodes: flattenNodes(instanceNodes.value),
          connections: instanceConnections.value
        },
        validation: validateWorkflow(instanceNodes.value, instanceConnections.value)
      } as WorkflowEditorModel)
    }

    // Initial load: merge definitions into persisted instances
    instanceNodes.value = mergeDefinitions(props.nodes, props.modelValue?.nodes || [])
    instanceConnections.value = props.modelValue?.connections || []

    // Re-merge when externally reloaded (e.g. workflow fetched async)
    watch(() => props.modelValue?.nodes, (value, previous) => {
      if (!value || JSON.stringify(value) === JSON.stringify(previous)) {
        return
      }
      instanceNodes.value = mergeDefinitions(props.nodes, value)
      instanceConnections.value = props.modelValue?.connections || []
    }, { deep: true })

    // Node creation from panel drop
    const onDropNode = (payload: { tid: string; position: { x: number; y: number } }) => {
      const definition = props.nodes.find((node) => node.id === payload.tid)
      if (!definition) {
        return
      }

      const instance: NodeInstance = {
        id: uuidv4(),
        tid: definition.id,
        key: definition.key,
        category: definition.category,
        label: definition.label,
        description: definition.description,
        position: payload.position,
        data: {},
        ports: definition.ports || [],
        configure: (definition.configure || []).map((item) => ({ ...item }))
      }

      instanceNodes.value.push(instance)
      selectedNode.value = instance
      emit('onNodeAdded', instance)
      commit()
    }

    const onNodeMoved = (node: NodeInstance) => {
      emit('onNodeMoved', node)
      commit()
    }

    const onNodeSelected = (node: NodeInstance | null) => {
      selectedNode.value = node
      emit('onNodeSelected', node)
    }

    const onNodeDeleted = (node: NodeInstance) => {
      instanceNodes.value = instanceNodes.value.filter((item) => item.id !== node.id)
      instanceConnections.value = instanceConnections.value.filter((connection) => {
        const sourceNodeId = connection.source.split('-')[0]
        const targetNodeId = connection.target.split('-')[0]
        return sourceNodeId !== node.id && targetNodeId !== node.id
      })
      if (selectedNode.value?.id === node.id) {
        selectedNode.value = null
      }
      emit('onNodeDeleted', node)
      commit()
    }

    const onConnectionCreated = (connection: WorkflowConnection) => {
      instanceConnections.value.push(connection)
      emit('onConnectionCreated', connection)
      commit()
    }

    const onNodeUpdated = (node: NodeInstance) => {
      const index = instanceNodes.value.findIndex((item) => item.id === node.id)
      if (index !== -1) {
        instanceNodes.value[index] = node
      }
      commit()
    }

    const onSearchText = (value: string) => {
      emit('update:searchText', value)
    }

    return {
      instanceNodes,
      instanceConnections,
      selectedNode,
      onDropNode,
      onNodeMoved,
      onNodeSelected,
      onNodeDeleted,
      onConnectionCreated,
      onNodeUpdated,
      onSearchText
    }
  }
})
</script>
