import { computed, onMounted, onScopeDispose, ref, type Ref } from 'vue'
import { fitSourceWorkspaceLayout, readSourceWorkspaceLayout } from '@/model/source-workspace-layout'

const STORAGE_KEY = 'MetaForgeSourceWorkspaceLayout'

export function useSourceWorkspaceLayout(rightOpen: Ref<boolean>) {
  const workspaceRoot = ref<HTMLElement | null>(null)
  const width = ref(typeof window === 'undefined' ? 1536 : window.innerWidth)
  let stored: string | null = null
  try { stored = localStorage.getItem(STORAGE_KEY) } catch { /* Layout preferences are optional. */ }
  const initial = readSourceWorkspaceLayout(stored)
  const preferredLeft = ref(initial.left)
  const preferredRight = ref(initial.right)
  const limits = computed(() => fitSourceWorkspaceLayout(width.value, preferredLeft.value, preferredRight.value, rightOpen.value))
  const leftWidth = computed({ get: () => limits.value.left, set: value => { preferredLeft.value = value } })
  const rightWidth = computed({ get: () => rightOpen.value ? limits.value.right : preferredRight.value, set: value => { preferredRight.value = value } })
  function saveLayout() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify({ left: preferredLeft.value, right: preferredRight.value })) } catch { /* Private storage may be unavailable. */ }
  }
  let observer: ResizeObserver | undefined
  const measure = () => { if (workspaceRoot.value) width.value = workspaceRoot.value.clientWidth }
  onMounted(() => {
    measure()
    if (typeof ResizeObserver !== 'undefined' && workspaceRoot.value) {
      observer = new ResizeObserver(measure)
      observer.observe(workspaceRoot.value)
    }
    window.addEventListener('resize', measure)
  })
  onScopeDispose(() => {
    observer?.disconnect()
    if (typeof window !== 'undefined') window.removeEventListener('resize', measure)
    saveLayout()
  })
  return { workspaceRoot, leftWidth, rightWidth, limits, saveLayout }
}
