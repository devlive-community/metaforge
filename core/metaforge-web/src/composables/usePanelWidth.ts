import { ref } from 'vue'

/**
 * Width of a resizable panel, kept within its limits and remembered in this browser under `key`.
 * Pair it with MfResizeHandle: v-model the width and call `save` on change.
 */
export function usePanelWidth(key: string, limits: { initial: number; min: number; max: number }) {
  const clamp = (value: number) => Math.min(limits.max, Math.max(limits.min, value))
  let stored = NaN
  try { stored = Number(localStorage.getItem(key)) } catch { /* Preferences are optional. */ }
  const width = ref(clamp(Number.isFinite(stored) && stored > 0 ? stored : limits.initial))
  function save() {
    try { localStorage.setItem(key, String(width.value)) } catch { /* Private storage may be unavailable. */ }
  }
  return { width, save }
}
