import { computed, onMounted, onScopeDispose, ref } from 'vue'

export function useChartColors() {
  const version = ref(0)
  let observer: MutationObserver | undefined
  onMounted(() => {
    observer = new MutationObserver(() => { version.value++ })
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'style', 'data-theme'] })
  })
  onScopeDispose(() => observer?.disconnect())
  return computed(() => {
    version.value
    const style = getComputedStyle(document.documentElement)
    const token = (name: string) => style.getPropertyValue('--mf-color-' + name).trim()
    return { primary: token('primary'), text: token('content-secondary'), title: token('content'), line: token('line'),
      colors: ['primary', 'success', 'info', 'data-complex', 'warning', 'danger'].map(token) }
  })
}
