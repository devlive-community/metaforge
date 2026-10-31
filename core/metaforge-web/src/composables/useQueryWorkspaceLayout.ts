import { computed, onScopeDispose, ref } from 'vue'

export function useQueryWorkspaceLayout() {
  const centerRef = ref<HTMLElement | null>(null)
  const leftWidth = ref(280)
  const rightWidth = ref(320)
  const editorRatio = ref(0.42)
  const editorHeight = computed(() => `${ Math.round(editorRatio.value * 1000) / 10 }%`)

  const LAYOUT_KEY = 'MetaForgeQueryLayout'

  const loadLayout = () => {
    try {
      const raw = localStorage.getItem(LAYOUT_KEY)
      if (!raw) {
        return
      }
      const layout = JSON.parse(raw)
      if (layout.leftWidth) {
        leftWidth.value = Math.min(420, Math.max(240, Number(layout.leftWidth) || 280))
      }
      if (layout.rightWidth) {
        rightWidth.value = Math.min(420, Math.max(240, Number(layout.rightWidth) || 320))
      }
      if (layout.editorRatio) {
        editorRatio.value = Math.min(0.7, Math.max(0.2, Number(layout.editorRatio) || 0.42))
      }
    }
    catch {
      // Keep defaults when the stored layout is unreadable.
    }
  }

  type ResizeKind = 'left' | 'right' | 'editor'

  let activeCleanup: (() => void) | undefined
  const startResize = (kind: ResizeKind, event: PointerEvent) => {
    activeCleanup?.()
    event.preventDefault()
    const start = kind === 'editor'
      ? { startY: event.clientY, startRatio: editorRatio.value }
      : { startX: event.clientX, startWidth: kind === 'left' ? leftWidth.value : rightWidth.value }

    const onMove = (moveEvent: PointerEvent) => {
      if (kind === 'editor') {
        const container = centerRef.value?.clientHeight ?? 1
        const delta = (moveEvent.clientY - start.startY) / container
        editorRatio.value = Math.min(0.7, Math.max(0.2, start.startRatio + delta))
      }
      else if (kind === 'left') {
        leftWidth.value = Math.min(420, Math.max(240, start.startWidth + (moveEvent.clientX - start.startX)))
      }
      else {
        rightWidth.value = Math.min(420, Math.max(240, start.startWidth - (moveEvent.clientX - start.startX)))
      }
    }
    const previousSelection = document.documentElement.style.userSelect
    const onUp = () => {
      document.documentElement.style.userSelect = previousSelection
      activeCleanup = undefined
      window.removeEventListener('pointermove', onMove)
      window.removeEventListener('pointerup', onUp)
      saveLayout()
    }
    activeCleanup = onUp
    document.documentElement.style.userSelect = 'none'
    window.addEventListener('pointermove', onMove)
    window.addEventListener('pointerup', onUp)
  }

  const saveLayout = () => {
    try { localStorage.setItem(LAYOUT_KEY, JSON.stringify({ leftWidth: leftWidth.value, rightWidth: rightWidth.value, editorRatio: editorRatio.value })) } catch { /* Layout storage is optional. */ }
  }
  onScopeDispose(() => activeCleanup?.())
  return { centerRef, leftWidth, rightWidth, editorHeight, loadLayout, startResize, saveLayout }
}
