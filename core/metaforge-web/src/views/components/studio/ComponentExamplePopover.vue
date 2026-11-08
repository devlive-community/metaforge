<template>
  <Teleport to="body">
    <aside :id="id" role="tooltip" :aria-label="title" :style="position"
           class="pointer-events-none fixed z-40 flex min-h-0 flex-col overflow-hidden rounded-mf-lg border border-line bg-surface shadow-mf-lg">
      <header class="flex shrink-0 items-center justify-between gap-2 border-b border-line px-3 py-2">
        <strong class="min-w-0 truncate text-sm">{{ title }}</strong>
        <span class="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-medium text-primary">{{ caption }}</span>
      </header>
      <div class="min-h-0 flex-1 overflow-hidden p-2" inert aria-hidden="true">
        <slot/>
      </div>
    </aside>
  </Teleport>
</template>

<script setup lang="ts">
import { onMounted, onScopeDispose, shallowRef, watch, type CSSProperties } from 'vue'

const props = defineProps<{ id: string; title: string; caption: string; anchor: HTMLElement; owner: HTMLElement }>()
const position = shallowRef<CSSProperties>({ visibility: 'hidden' })

function place() {
  const gutter = 12
  const gap = 10
  const bounds = props.anchor.getBoundingClientRect()
  const owner = props.owner.getBoundingClientRect()
  const viewportWidth = document.documentElement.clientWidth
  const viewportHeight = document.documentElement.clientHeight
  const right = viewportWidth - owner.right - gap - gutter
  const beside = right >= 230
  const width = Math.min(348, beside ? right : viewportWidth - gutter * 2)
  const height = Math.min(258, viewportHeight - gutter * 2)
  const left = beside ? owner.right + gap : bounds.right - width
  const top = beside ? bounds.top + (bounds.height - height) / 2
    : bounds.bottom + gap + height <= viewportHeight - gutter ? bounds.bottom + gap : bounds.top - height - gap
  const clamp = (value: number, limit: number) => Math.max(gutter, Math.min(limit - gutter, value))
  position.value = { left: clamp(left, viewportWidth - width) + 'px', top: clamp(top, viewportHeight - height) + 'px',
    width: width + 'px', height: height + 'px' }
}

let observer: ResizeObserver | undefined
onMounted(() => {
  place()
  window.addEventListener('resize', place)
  observer = new ResizeObserver(place)
  observer.observe(props.owner)
})
watch(() => props.anchor, place, { flush: 'post' })
onScopeDispose(() => {
  window.removeEventListener('resize', place)
  observer?.disconnect()
})
</script>
