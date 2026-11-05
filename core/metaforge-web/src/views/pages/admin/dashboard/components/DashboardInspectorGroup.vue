<template>
  <details ref="group" :open="defaultOpen" class="group min-w-0 rounded-mf-md border border-line bg-surface shadow-mf-sm">
    <summary class="flex cursor-pointer list-none items-center gap-2 rounded-mf-md bg-surface-hover/80 px-3 py-2.5 text-sm font-semibold
                    focus-visible:outline-2 focus-visible:outline-primary [&::-webkit-details-marker]:hidden">
      <component :is="icons[icon]" :size="16" class="shrink-0 text-primary" aria-hidden="true"/>
      <span class="min-w-0 flex-1">{{ title }}</span>
      <ChevronDown :size="15" class="shrink-0 text-content-secondary transition-transform group-open:rotate-180" aria-hidden="true"/>
    </summary>
    <div class="grid min-w-0 grid-cols-1 gap-3 p-3"><slot/></div>
  </details>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown, Database, Info, LayoutGrid, Monitor, MousePointer2, Palette, RefreshCw, SlidersHorizontal } from '@lucide/vue'

const icons = {
  info: Info, data: Database, canvas: Monitor, refresh: RefreshCw, layout: LayoutGrid,
  appearance: Palette, interaction: MousePointer2, actions: SlidersHorizontal,
}
withDefaults(defineProps<{ title: string; icon?: keyof typeof icons; defaultOpen?: boolean }>(), {
  icon: 'info', defaultOpen: true,
})
const group = ref<HTMLDetailsElement>()
defineExpose({ expand: () => { if (group.value) group.value.open = true } })
</script>
