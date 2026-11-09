import { computed, onScopeDispose, ref, watch, type Ref } from 'vue'
import type { ECharts, EChartsOption, registerMap } from 'echarts'
import { loadECharts } from '@/utils/echarts'
import { fitChartToCard } from '@/model/chart-card-layout'

export interface EChartState {
  option: EChartsOption
  map?: { name: string; source: Parameters<typeof registerMap>[1] }
  loading?: boolean
  fit?: boolean
}

/** Wait for the engine and the first painted frame, without flashing on ordinary option edits. */
export function useEChart(host: Ref<HTMLElement | undefined>, state: EChartState,
  events: { point: (data: unknown, name?: string) => void; error: () => void }, shouldFit = () => !!state.fit) {
  const rendering = ref(true), failed = ref(false)
  const pending = computed(() => !!state.loading || rendering.value)
  let engine: Awaited<ReturnType<typeof loadECharts>> | undefined
  let chart: ECharts | undefined, observer: ResizeObserver | undefined, frame: number | undefined
  let version = 0, disposed = false, needsRender = true
  let lastWidth = 0, lastHeight = 0

  function chartOption(element: HTMLElement) {
    lastWidth = element.clientWidth
    lastHeight = element.clientHeight
    return shouldFit() ? fitChartToCard(state.option, lastWidth, lastHeight, getComputedStyle(element).color) : state.option
  }

  function cancelPaint() { if (frame !== undefined) cancelAnimationFrame(frame); frame = undefined }
  function discard() { version++; cancelPaint(); chart?.dispose(); chart = undefined }
  function painted(ticket: number) {
    // Two frames let the loading state paint before revealing a synchronously rendered chart.
    frame = requestAnimationFrame(() => {
      frame = requestAnimationFrame(() => {
        frame = undefined
        if (!disposed && ticket === version) rendering.value = false
      })
    })
  }
  async function render() {
    const ticket = ++version, element = host.value
    needsRender = true
    cancelPaint()
    if (disposed) return
    if (state.loading) { rendering.value = true; return }
    if (!element?.clientWidth || !element.clientHeight) return
    if (!chart) rendering.value = true
    failed.value = false
    try {
      const loaded = engine ?? await loadECharts()
      if (disposed || ticket !== version || element !== host.value || state.loading) return
      if (!element.clientWidth || !element.clientHeight) return
      engine = loaded
      if (state.map && !engine.getMap(state.map.name)) engine.registerMap(state.map.name, state.map.source)
      if (!chart) {
        chart = engine.init(element)
        chart.on('click', event => events.point(event.data, event.name))
      }
      chart.setOption(chartOption(element), { notMerge: true })
      needsRender = false
      if (rendering.value) painted(ticket)
    } catch {
      if (disposed || ticket !== version) return
      discard(); rendering.value = false; failed.value = true; events.error()
    }
  }
  watch(host, element => {
    observer?.disconnect(); discard(); rendering.value = true; failed.value = false
    if (!element) return
    observer = new ResizeObserver(() => {
      if (!element.clientWidth || !element.clientHeight) return
      if (!chart || needsRender) { void render(); return }
      if (lastWidth === element.clientWidth && lastHeight === element.clientHeight) return
      try {
        chart.resize()
        if (shouldFit()) {
          // Merge the layout without clearing legend selection or the active zoom window.
          chart.setOption(chartOption(element))
        } else {
          lastWidth = element.clientWidth
          lastHeight = element.clientHeight
        }
      } catch {
        void render()
      }
    })
    observer.observe(element)
    void render()
  }, { immediate: true, flush: 'post' })
  watch([() => state.option, () => state.map, () => state.loading, () => state.fit], () => { void render() }, { flush: 'post' })
  onScopeDispose(() => { disposed = true; observer?.disconnect(); discard() })

  function exportImage(name: string, type: 'png' | 'svg') {
    if (!chart || pending.value || !engine) return
    const link = document.createElement('a')
    link.download = name + '.' + type
    if (type === 'png') { link.href = chart.getDataURL({ type: 'png', pixelRatio: 2 }); link.click(); return }
    const svg = engine.init(null, undefined, { renderer: 'svg', ssr: true, width: chart.getWidth(), height: chart.getHeight() })
    try {
      svg.setOption({ ...chart.getOption() as EChartsOption, animation: false })
      const url = URL.createObjectURL(new Blob([svg.renderToSVGString()], { type: 'image/svg+xml;charset=utf-8' }))
      link.href = url; link.click(); window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } finally { svg.dispose() }
  }
  return { pending, failed, exportImage }
}
