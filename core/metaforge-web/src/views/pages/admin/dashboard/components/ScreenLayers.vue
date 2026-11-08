<template>
  <MfEmpty v-if="!widgets.length" class="min-h-36" size="sm" :title="$t('dashboard.studio.emptyCanvas')"/>
  <template v-else>
    <p v-if="wall" class="mb-2 text-xs leading-5 text-content-tertiary">{{ $t('dashboard.studio.layerHint') }}</p>
    <MfButton v-if="canGroup" class="mb-2" size="sm" @click="emit('group')"><Group :size="14"/>{{ $t('dashboard.studio.groupSelection') }}</MfButton>
    <div v-for="row in rows" :key="row.id" :data-layer-id="row.id"
         class="relative mb-1 flex min-w-0 items-center gap-1 rounded-mf-md border border-transparent"
         :style="{ marginLeft: row.depth * 12 + 'px' }" :class="[row.parent && 'border-l-line pl-1.5', selectedIds.includes(row.id) && 'bg-primary/10',
                  target?.id === row.id && (target.above ? 'border-t-primary' : 'border-b-primary')]"
         @contextmenu="wall && emit('context', $event, row.id)" @dragover="dragOver($event, row)"
         @dragleave="leaveRow($event, row.id)" @drop="drop($event, row)">
      <button v-if="row.count" type="button" class="grid size-6 shrink-0 place-items-center rounded-mf-sm hover:bg-surface-hover"
              :aria-label="$t('dashboard.studio.toggleGroupLayers')" :aria-expanded="!collapsed.has(row.id)" @click="toggleGroup(row.id)">
        <ChevronRight :size="13" :class="!collapsed.has(row.id) && 'rotate-90'"/>
      </button>
      <button type="button" class="flex min-w-0 flex-1 items-center gap-1.5 px-1.5 py-2 text-left text-xs hover:text-primary"
              :class="row.count && 'font-semibold'" :aria-pressed="selectedIds.includes(row.id)" @click="emit('select', row.id, $event)">
        <Group v-if="row.kind === 'group'" :size="14" class="shrink-0"/><LayoutGrid v-else-if="row.kind === 'container'" :size="14" class="shrink-0"/>
        <span class="truncate" :title="row.name">{{ row.name }}</span>
      </button>
      <button type="button" draggable="true" class="grid size-7 shrink-0 cursor-grab place-items-center rounded-mf-sm text-content-tertiary hover:bg-surface-hover"
              :aria-label="$t('dashboard.studio.dragLayer', { name: row.name })"
              @dragstart="startDrag($event, row)" @dragend="clearDrag">
        <GripVertical :size="14"/>
      </button>
      <MfButton size="sm" variant="ghost" :aria-label="$t('dashboard.studio.raiseLayer')"
                :disabled="rows.find(other => other.parent === row.parent)?.id === row.id" @click="raise(row)"><ArrowUp :size="13"/></MfButton>
    </div>
  </template>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowUp, ChevronRight, GripVertical, Group, LayoutGrid } from '@lucide/vue'
import { dashboardContainerTool, type DashboardWidget } from '@/model/dashboard-layout'

const props = defineProps<{ widgets: DashboardWidget[]; selectedIds: string[]; wall: boolean; canGroup: boolean }>()
const emit = defineEmits<{ select: [id: string, event: MouseEvent]; reorder: [from: string, to: string, above: boolean];
  context: [event: MouseEvent, id: string]; group: [] }>()
const { t } = useI18n()
type Layer = { id: string; name: string; parent?: string; count?: number; depth: number; kind: 'group' | 'container' | 'widget' }
const collapsed = ref(new Set<string>())
const rows = computed(() => {
  const items: Layer[] = []
  const seen = new Set<string>()
  const widgets = [...props.widgets].reverse()
  function append(parent: string | undefined, depth: number) {
    for (const widget of widgets.filter(widget => widget.parentId === parent)) {
      const groupId = props.wall ? widget.groupId : undefined
      if (groupId) {
        if (seen.has(groupId)) continue
        seen.add(groupId)
        const members = widgets.filter(member => member.groupId === groupId)
        items.push({ id: groupId, name: t('dashboard.studio.layerGroup', { count: members.length }), count: members.length, parent, depth, kind: 'group' })
        if (!collapsed.value.has(groupId)) for (const member of members) {
          items.push({ id: member.id, name: member.name, parent: groupId, depth: depth + 1, kind: dashboardContainerTool(member.kind) ? 'container' : 'widget',
            count: widgets.filter(item => item.parentId === member.id).length })
          if (!collapsed.value.has(member.id)) append(member.id, depth + 2)
        }
        continue
      }
      if (seen.has(widget.id)) continue
      seen.add(widget.id)
      const count = widgets.filter(child => child.parentId === widget.id).length
      items.push({ id: widget.id, name: widget.name, parent, count, depth, kind: dashboardContainerTool(widget.kind) ? 'container' : 'widget' })
      if (!collapsed.value.has(widget.id)) append(widget.id, depth + 1)
    }
  }
  append(undefined, 0)
  return items
})
const dragging = ref<Layer>()
const target = ref<{ id: string; above: boolean }>()
function toggleGroup(id: string) {
  if (collapsed.value.has(id)) collapsed.value.delete(id)
  else collapsed.value.add(id)
}
function startDrag(event: DragEvent, row: Layer) {
  if (!event.dataTransfer) return
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('application/x-metaforge-layer', row.id)
  dragging.value = row
}
function clearDrag() { dragging.value = undefined; target.value = undefined }
function dragOver(event: DragEvent, row: Layer) {
  if (!dragging.value || dragging.value.id === row.id || dragging.value.parent !== row.parent) return
  event.preventDefault()
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  target.value = { id: row.id, above: event.clientY < bounds.top + bounds.height / 2 }
}
function leaveRow(event: DragEvent, id: string) {
  if (target.value?.id === id && !(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null)) target.value = undefined
}
function drop(event: DragEvent, row: Layer) {
  if (!dragging.value || target.value?.id !== row.id) return
  event.preventDefault()
  emit('reorder', dragging.value.id, row.id, target.value.above)
  clearDrag()
}
function raise(row: Layer) {
  const first = rows.value.find(other => other.parent === row.parent)
  if (first) emit('reorder', row.id, first.id, true)
}
</script>
