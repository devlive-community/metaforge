<template>
  <div ref="root" class="relative flex items-start justify-between gap-16 py-2">
    <svg class="pointer-events-none absolute inset-0 size-full overflow-visible text-primary" aria-hidden="true">
      <defs><marker :id="`${uid}-arrow`" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="7" markerHeight="7" orient="auto"><path d="M0,0 L8,4 L0,8 z" fill="currentColor"/></marker></defs>
      <template v-if="link">
        <circle :cx="link.x1" :cy="link.y1" r="3" fill="currentColor"/>
        <path :d="link.path" fill="none" stroke="currentColor" stroke-width="1.5" :marker-end="`url(#${uid}-arrow)`"/>
      </template>
    </svg>
    <div v-for="side in sides" :key="side.key" class="w-1/2 min-w-0 max-w-72 overflow-hidden rounded-mf-md border shadow-mf-xs"
         :class="side.key === 'local' ? 'border-success/30' : 'border-info/30'">
      <div class="flex items-center gap-2 px-3 py-2 text-sm font-semibold text-content" :class="side.key === 'local' ? 'bg-success/10' : 'bg-info/10'">
        <Table2 :size="15" class="shrink-0"/><span class="truncate" :title="side.table">{{ side.table }}</span>
      </div>
      <p v-if="!side.shape" class="m-0 px-3 py-4 text-center text-xs text-content-tertiary">{{ $t('source.fk.columnsUnavailable') }}</p>
      <ul v-else class="m-0 list-none bg-surface p-0 py-1 text-xs">
        <li v-for="column in side.shape.columns" :key="column.name" :data-column="`${side.key}:${column.name}`"
            class="flex items-center gap-1.5 px-3 py-1" :class="side.linked.includes(column.name) && (side.key === 'local' ? 'bg-success/10' : 'bg-info/10')">
          <KeyRound v-if="side.shape.primaryKeys.includes(column.name)" :size="12" class="shrink-0 text-warning"/><span v-else class="w-3 shrink-0"/>
          <span class="min-w-0 flex-1 truncate text-content" :class="side.linked.includes(column.name) && 'font-medium'">{{ column.name }}</span>
          <span class="shrink-0 text-content-tertiary">{{ column.type }}{{ column.length ? `(${column.length})` : '' }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { KeyRound, Table2 } from '@lucide/vue'
import type { TableShape } from '@/composables/useSourceTableForeignKeys'

const props = defineProps<{ table: string; shape: TableShape | null; referencedTable: string; referencedShape: TableShape | null; columns: string[]; referencedColumns: string[] }>()
const uid = `fk-diagram-${getCurrentInstance()!.uid}`
const root = ref<HTMLElement>()
const sides = computed(() => [
  { key: 'local', table: props.table, shape: props.shape, linked: props.columns },
  { key: 'referenced', table: props.referencedTable, shape: props.referencedShape, linked: props.referencedColumns },
])
const link = ref<{ x1: number; y1: number; path: string } | null>(null)
/** Draws the curve from the first local column's right edge to the referenced column's left edge. */
function measure() {
  const container = root.value
  const from = container?.querySelector<HTMLElement>(`[data-column="local:${CSS.escape(props.columns[0] || '')}"]`)
  const to = container?.querySelector<HTMLElement>(`[data-column="referenced:${CSS.escape(props.referencedColumns[0] || '')}"]`)
  if (!container || !from || !to) { link.value = null; return }
  const box = container.getBoundingClientRect()
  const start = from.getBoundingClientRect()
  const end = to.getBoundingClientRect()
  const x1 = start.right - box.left
  const y1 = start.top + start.height / 2 - box.top
  const x2 = end.left - box.left - 2
  const y2 = end.top + end.height / 2 - box.top
  const bend = Math.max(24, (x2 - x1) / 2)
  link.value = { x1, y1, path: `M ${x1} ${y1} C ${x1 + bend} ${y1}, ${x2 - bend} ${y2}, ${x2} ${y2}` }
}
let observer: ResizeObserver | undefined
onMounted(() => {
  if (typeof ResizeObserver !== 'undefined' && root.value) {
    observer = new ResizeObserver(measure)
    observer.observe(root.value)
  }
  nextTick(measure)
})
watch(() => [props.shape, props.referencedShape, props.columns, props.referencedColumns], () => nextTick(measure))
onBeforeUnmount(() => observer?.disconnect())
</script>
