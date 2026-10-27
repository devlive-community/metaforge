<template>
  <div class="dc-workflow-panel flex flex-col h-full bg-white" :style="{ width: `${ width }px` }">
    <div class="p-2 border-b">
      <MfInput v-model="searchTextValue"
               size="sm"
               clearable
               :placeholder="$t('common.search') || 'Search'">
        <template #prefix>
          <SearchIcon :size="12" />
        </template>
      </MfInput>
    </div>

    <div class="flex-1 overflow-y-auto p-2">
      <div v-for="category in categories"
           :key="category.value"
           class="mb-3">
        <div class="text-xs text-gray-500 font-semibold py-1 border-b mb-1">{{ category.label }}</div>

        <div v-for="node in nodesByCategory(category.value)"
             :key="node.id"
             class="dc-workflow-panel__node flex items-center gap-2 px-2 py-1.5 mb-1 rounded border bg-white cursor-grab hover:border-[var(--dc-primary)] hover:shadow-sm text-sm"
             draggable="true"
             @dragstart="onDragStart($event, node)">
          <component :size="14" :is="categoryIcon(category.value)" />
          <div class="flex-1 min-w-0">
            <div class="truncate">{{ node.label }}</div>
            <div class="text-xs text-gray-400 truncate">{{ node.description }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed, PropType } from 'vue'
import { CategoryDefinition, NodeDefinition } from './types'
import { Database, SearchIcon, Server, Settings } from '@lucide/vue'

export default defineComponent({
  components: { SearchIcon },
    props: {
    categories: {
      type: Array as PropType<CategoryDefinition[]>,
      default: () => []
    },
    nodes: {
      type: Array as PropType<NodeDefinition[]>,
      default: () => []
    },
    searchText: {
      type: String,
      default: ''
    },
    width: {
      type: Number,
      default: 250
    }
  },
  emits: ['update:searchText'],
  setup(props, { emit })
  {
    const searchTextValue = computed({
      get: () => props.searchText,
      set: (value: string) => emit('update:searchText', value)
    })

    const nodesByCategory = (category: string): NodeDefinition[] => {
      const keyword = props.searchText?.toLowerCase() || ''
      return props.nodes
          .filter((node) => node.category === category)
          .filter((node) => !keyword || node.label?.toLowerCase().includes(keyword) || node.description?.toLowerCase().includes(keyword))
    }

    const categoryIcon = (category: string): any => {
      if (category === 'source') {
        return Database
      }
      if (category === 'sink') {
        return Server
      }
      return Settings
    }

    const onDragStart = (event: DragEvent, node: NodeDefinition) => {
      if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'copy'
        event.dataTransfer.setData('application/x-workflow-node', JSON.stringify({ tid: node.id }))
      }
    }

    return { searchTextValue, nodesByCategory, categoryIcon, onDragStart }
  }
})
</script>
