<template>
  <div class="flex w-full bg-white border rounded" style="height: 100%;">
    <!-- Left: component panel -->
    <div class="dc-builder-panel flex flex-col border-r shrink-0" :style="{ width: `${ panelWidth }px` }">
      <div class="px-2 py-2 text-xs text-gray-500 font-semibold border-b">{{ $t('builder.text.components') }}</div>
      <div class="flex-1 overflow-y-auto p-2">
        <div v-for="group in items"
             :key="group.group"
             class="mb-3">
          <div class="text-xs text-gray-400 py-1">{{ group.group }}</div>
          <div v-for="child in group.children"
               :key="child.type + child.label"
               class="flex items-center gap-2 px-2 py-1.5 mb-1 rounded border bg-white cursor-grab hover:border-[var(--dc-primary)] hover:shadow-sm text-sm"
               draggable="true"
               @dragstart="onPanelDragStart($event, child)">
            <AppstoreOutlined :style="{ fontSize: '14px' }" class="text-gray-500"/>
            <span class="truncate">
              <slot name="panel-label" :item="child">{{ child.label }}</slot>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Center: canvas -->
    <div class="flex-1 min-w-0 overflow-auto bg-gray-50"
         @drop="onDrop"
         @dragover="onDragOver">
      <div ref="canvasRef"
           class="relative"
           :style="{
               width: `${ width }px`,
               height: `${ height }px`,
               backgroundImage: showGrid ? `linear-gradient(to right, rgba(0, 0, 0, 0.06) 1px, transparent 1px), linear-gradient(to bottom, rgba(0, 0, 0, 0.06) 1px, transparent 1px)` : undefined,
               backgroundSize: showGrid ? `${ gridSize }px ${ gridSize }px` : undefined
           }"
           @click.self="onSelect(null)">
        <!-- Guidelines -->
        <template v-if="showGuidelines">
          <div v-for="(line, index) in guidelineLines"
               :key="index"
               class="absolute bg-pink-500/60 pointer-events-none"
               :style="line.orientation === 'v'
                   ? { left: `${ line.position }px`, top: 0, width: '1px', height: '100%' }
                   : { top: `${ line.position }px`, left: 0, height: '1px', width: '100%' }"></div>
        </template>

        <!-- Placed components -->
        <div v-for="component in components"
             :key="component.id"
             class="absolute bg-white border rounded shadow-sm"
             :class="selectedId === component.id ? 'border-[var(--dc-primary)] ring-1 ring-[var(--dc-primary-light)]' : 'border-gray-200'"
             :style="{ left: `${ component.position.x }px`, top: `${ component.position.y }px`, minWidth: '160px', minHeight: '48px' }"
             @mousedown="onComponentMouseDown($event, component)"
             @click.stop="onSelect(component)">
          <div class="p-2">
            <slot v-if="$slots[component.type]"
                  :name="component.type"
                  :configure="component.configure"
                  :is-selected="selectedId === component.id"></slot>
            <div v-else class="text-sm text-gray-700">
              {{ component.label }}
            </div>
          </div>

          <span v-if="selectedId === component.id"
                class="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center cursor-pointer"
                @mousedown.stop
                @click.stop="onDelete(component)">✕</span>
        </div>
      </div>
    </div>

    <!-- Right: configure -->
    <div class="dc-builder-configure flex flex-col border-l shrink-0" :style="{ width: `${ configWidth }px` }">
      <template v-if="selected">
        <div class="px-2 py-2 text-xs text-gray-500 font-semibold border-b">{{ selected.label }}</div>
        <div class="flex-1 overflow-y-auto p-2">
          <div v-for="groupConfigure in selected.configure || []"
               :key="groupConfigure.key"
               class="mb-3">
            <div class="text-xs text-gray-400 py-1 border-b mb-1">{{ groupConfigure.group }}</div>

            <div v-for="entry in groupConfigure.items || []"
                 :key="entry.label"
                 class="mb-2">
              <div class="text-xs text-gray-600 mb-1">{{ entry.label }}</div>

              <MfTextarea v-if="entry.type === 'textarea'"
                          :model-value="entry.value"
                          :rows="3"
                          :placeholder="entry.placeholder"
                          @update:model-value="(value) => onEntryChange(entry, value)"/>

              <MfInputNumber v-else-if="entry.type === 'number'"
                             :model-value="entry.value"
                             class="w-full"
                             :min="entry.min"
                             :max="entry.max"
                             @change="(value: any) => onEntryChange(entry, value)"/>

              <MfInput v-else
                       :model-value="entry.value"
                       :placeholder="entry.placeholder"
                       @update:model-value="(value) => onEntryChange(entry, value)"/>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="flex-1 flex items-center justify-center text-sm text-gray-400 p-4 text-center">
        {{ $t('builder.placeholder.selectComponent') }}
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onBeforeUnmount, PropType } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import { AppstoreOutlined } from '@ant-design/icons-vue'

interface BuilderPanelItem
{
  type: string
  label: string
  configure?: Array<{ group: string; key: string; items: any[] }>
}

interface BuilderPanelGroup
{
  group: string
  children: BuilderPanelItem[]
}

interface PlacedComponent
{
  id: string
  type: string
  label: string
  position: { x: number; y: number }
  configure: Array<{ group: string; key: string; items: any[] }>
}

export default defineComponent({
  components: { AppstoreOutlined },
    props: {
    panelWidth: {
      type: Number,
      default: 200
    },
    configWidth: {
      type: Number,
      default: 200
    },
    items: {
      type: Array as PropType<BuilderPanelGroup[]>,
      default: () => []
    },
    showGrid: {
      type: Boolean,
      default: true
    },
    snapToGrid: {
      type: Boolean,
      default: true
    },
    gridSize: {
      type: Number,
      default: 20
    },
    showGuidelines: {
      type: Boolean,
      default: false
    },
    width: {
      type: Number,
      default: 1920
    },
    height: {
      type: Number,
      default: 1080
    }
  },
  emits: ['update-config'],
  setup(props, { emit })
  {
    const components = ref<PlacedComponent[]>([])
    const selectedId = ref<string | undefined>(undefined)
    const canvasRef = ref<HTMLElement | null>(null)

    const selected = computed(() => components.value.find((component) => component.id === selectedId.value))

    const guidelineLines = computed(() => {
      if (!props.showGuidelines || !selected.value) {
        return []
      }
      const lines: Array<{ orientation: 'v' | 'h'; position: number }> = []
      const threshold = 5
      components.value
                .filter((component) => component.id !== selected.value!.id)
                .forEach((component) => {
                  if (Math.abs(component.position.x - selected.value!.position.x) <= threshold) {
                    lines.push({ orientation: 'v', position: component.position.x })
                  }
                  if (Math.abs(component.position.y - selected.value!.position.y) <= threshold) {
                    lines.push({ orientation: 'h', position: component.position.y })
                  }
                })
      return lines
    })

    const snap = (value: number): number => {
      return props.snapToGrid ? Math.round(value / props.gridSize) * props.gridSize : value
    }

    const commit = () => {
      emit('update-config', {
        width: props.width,
        height: props.height,
        items: components.value,
        canvasStyle: {}
      })
    }

    const onPanelDragStart = (event: DragEvent, item: BuilderPanelItem) => {
      if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'copy'
        event.dataTransfer.setData('application/x-data-builder', JSON.stringify(item))
      }
    }

    const onDragOver = (event: DragEvent) => {
      event.preventDefault()
      if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'copy'
      }
    }

    const onDrop = (event: DragEvent) => {
      event.preventDefault()
      const payload = event.dataTransfer?.getData('application/x-data-builder')
      if (!payload || !canvasRef.value) {
        return
      }

      const canvasRect = canvasRef.value.getBoundingClientRect()

      const item = JSON.parse(payload) as BuilderPanelItem
      const component: PlacedComponent = {
        id: uuidv4(),
        type: item.type,
        label: item.label,
        position: {
          x: snap(event.clientX - canvasRect.left),
          y: snap(event.clientY - canvasRect.top)
        },
        configure: (item.configure || []).map((group) => ({
          ...group,
          items: (group.items || []).map((entry) => ({ ...entry }))
        }))
      }

      components.value.push(component)
      selectedId.value = component.id
      commit()
    }

    let dragging: { component: PlacedComponent; offsetX: number; offsetY: number } | null = null

    const onComponentMouseDown = (event: MouseEvent, component: PlacedComponent) => {
      if (!canvasRef.value) {
        return
      }
      const canvasRect = canvasRef.value.getBoundingClientRect()
      dragging = {
        component,
        offsetX: event.clientX - canvasRect.left - component.position.x,
        offsetY: event.clientY - canvasRect.top - component.position.y
      }
      window.addEventListener('mousemove', onMouseMove)
      window.addEventListener('mouseup', onMouseUp)
    }

    const onMouseMove = (event: MouseEvent) => {
      if (!dragging || !canvasRef.value) {
        return
      }
      const canvasRect = canvasRef.value.getBoundingClientRect()
      dragging.component.position = {
        x: Math.max(0, snap(event.clientX - canvasRect.left - dragging.offsetX)),
        y: Math.max(0, snap(event.clientY - canvasRect.top - dragging.offsetY))
      }
    }

    const onMouseUp = () => {
      if (dragging) {
        commit()
        dragging = null
      }
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
    }

    onBeforeUnmount(() => {
      window.removeEventListener('mousemove', onMouseMove)
      window.removeEventListener('mouseup', onMouseUp)
    })

    const onSelect = (component: PlacedComponent | null) => {
      selectedId.value = component?.id
    }

    const onDelete = (component: PlacedComponent) => {
      components.value = components.value.filter((item) => item.id !== component.id)
      if (selectedId.value === component.id) {
        selectedId.value = undefined
      }
      commit()
    }

    const onEntryChange = (entry: any, value: any) => {
      entry.value = value
      commit()
    }

    return {
      canvasRef,
      components,
      selectedId,
      selected,
      guidelineLines,
      onPanelDragStart,
      onDragOver,
      onDrop,
      onComponentMouseDown,
      onSelect,
      onDelete,
      onEntryChange
    }
  }
})
</script>
