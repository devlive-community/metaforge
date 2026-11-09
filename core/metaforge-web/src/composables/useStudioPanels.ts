import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'
import { usePanelWidth } from './usePanelWidth'

/** Docked editor panels on desktop, one overlay at a time on compact screens. */
export function useStudioPanels(key: string) {
  type Side = 'left' | 'right'
  const viewport = ref(window.innerWidth), compact = ref<Side>(), resizing = ref(false)
  const wide = computed(() => viewport.value >= 1100)
  const opened = reactive({ left: true, right: true })
  const left = usePanelWidth(key + '.left', { initial: 300, min: 240, max: 480 })
  const right = usePanelWidth(key + '.right', { initial: 300, min: 280, max: 480 })
  const max = computed(() => Math.min(480, Math.max(280, (viewport.value - 380) / 2)))
  const leftWidth = computed(() => Math.min(left.width.value, wide.value ? max.value : viewport.value * .85))
  const rightWidth = computed(() => Math.min(right.width.value, wide.value ? max.value : viewport.value * .85))
  const leftVisible = computed(() => wide.value ? opened.left : compact.value === 'left')
  const rightVisible = computed(() => wide.value ? opened.right : compact.value === 'right')
  function show(side: Side) { if (wide.value) opened[side] = true; else compact.value = side }
  function close(side: Side) {
    if (wide.value) opened[side] = false
    else compact.value = undefined
    nextTick(() => document.getElementById(`${key}-${side}-toggle`)?.focus({ preventScroll: true }))
  }
  function toggle(side: Side) { (side === 'left' ? leftVisible.value : rightVisible.value) ? close(side) : show(side) }
  const resize = () => { viewport.value = window.innerWidth }
  const stop = () => { resizing.value = false }
  window.addEventListener('resize', resize)
  window.addEventListener('pointerup', stop)
  window.addEventListener('pointercancel', stop)
  onBeforeUnmount(() => { window.removeEventListener('resize', resize); window.removeEventListener('pointerup', stop); window.removeEventListener('pointercancel', stop) })
  return { wide, compact, resizing, left, right, max, leftWidth, rightWidth, leftVisible, rightVisible, show, close, toggle }
}
