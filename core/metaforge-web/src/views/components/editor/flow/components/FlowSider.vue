<template>
  <div v-if="data" class="space-y-2">
    <MfCard>
      <template #header>
        <span class="text-gray-500 text-sm">{{ $t('pipeline.common.input') }}</span>
      </template>

      <div v-for="node in inputs" :key="node.id" class="dndflow p-1">
        <div class="nodes">
          <div :class="'flex items-center space-x-1 vue-flow__node-' + node.nodeType" :draggable="true" @dragstart="onDragStart($event, node.nodeType, node)">
            <MfAvatar size="sm" :src="'/static/images/plugin/' + (node.type as string).toLowerCase().split(' ')[0] + '.svg'"/>
            <span>{{ node.name }}</span>
          </div>
        </div>
      </div>
    </MfCard>

    <MfCard>
      <template #header>
        <span class="text-gray-500 text-sm">{{ $t('pipeline.common.output') }}</span>
      </template>

      <div v-for="node in outputs" :key="node.id" class="dndflow p-1">
        <div class="nodes">
          <div :class="'flex items-center space-x-1 vue-flow__node-' + node.nodeType" :draggable="true" @dragstart="onDragStart($event, node.nodeType, node)">
            <MfAvatar size="sm" :src="'/static/images/plugin/' + (node.type as string).toLowerCase().split(' ')[0] + '.svg'"/>
            <span>{{ node.name }}</span>
          </div>
        </div>
      </div>
    </MfCard>
  </div>
</template>

<script setup lang="ts">
import '../style.css'
import { onBeforeMount, ref } from 'vue'
import { Configuration } from '@/views/components/editor/flow/Configuration.ts'

defineOptions({ name: 'FlowSider' })

const props = defineProps<{ data?: Configuration[] }>()

const inputs = ref<Configuration[]>([])
const outputs = ref<Configuration[]>([])

onBeforeMount(() => {
  if (props.data) {
    inputs.value = props.data.filter(v => v.nodeType === 'input')
    outputs.value = props.data.filter(v => v.nodeType === 'output')
  }
})

/**
 * Sets the data type and value to be transferred during drag start event.
 *
 * @param {object} event - The drag start event object.
 * @param {any} nodeType - The type of node being dragged.
 */
const onDragStart = (event: any, nodeType: any, configure: any) => {
  if (event.dataTransfer) {
    const data = { type: nodeType, configure: configure }
    event.dataTransfer.setData('application/vueflow', JSON.stringify(data))
    event.dataTransfer.effectAllowed = 'move'
  }
}
</script>
