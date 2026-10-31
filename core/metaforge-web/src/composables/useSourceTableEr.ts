import { computed, inject, nextTick, onMounted, onScopeDispose, ref, watch, type Ref } from 'vue'
import { ER_ZOOM, clampZoom, defaultErSettings, mapErColumns, type ErSettings } from '@/model/source-er'
import { useSourceManageRoute as useRoute } from './useSourceManageRoute'

/**
 * ER tab: the table drawn as a card on a pannable, zoomable canvas, built from the columns,
 * indexes and foreign keys the workspace already loaded. Table facts, display settings and the
 * legend sit in the workspace's right panel, like the DDL quick menu.
 */
export function useSourceTableEr() {
  const route = useRoute()
  const tableInfo = inject<Ref<any | null>>('metadataTableInfo', ref(null))
  const columnRows = inject<Ref<any[]>>('metadataColumnRows', ref([]))
  const stats = inject<{ indexes: Ref<any[]>; foreignKeys: Ref<any[]> }>('metadataTableStats', { indexes: ref([]), foreignKeys: ref([]) })
  const loading = inject<Ref<boolean>>('metadataLoading', ref(false))
  const fieldPanelOpen = inject<Ref<boolean>>('metadataFieldPanelOpen', ref(false))
  const fieldPanelHost = inject<Ref<HTMLElement | null>>('metadataFieldPanelHost', ref(null))
  const fieldPanelWidth = inject<Ref<number>>('metadataFieldPanelWidth', ref(324))

  const root = ref<HTMLElement | null>(null)
  const viewport = ref<HTMLElement | null>(null)
  const card = ref<HTMLElement | null>(null)
  const zoom = ref(1)
  const offset = ref({ x: 0, y: 0 })
  const panning = ref(false)
  const fullscreen = ref(false)
  const settings = ref<ErSettings>(defaultErSettings())
  const panelTab = ref<'info' | 'settings'>('info')
  let drag: { pointer: number; x: number; y: number; left: number; top: number } | null = null

  const table = computed(() => String(tableInfo.value?.object_name || route.params.table || ''))
  const database = computed(() => String(route.params.database || ''))
  const columns = computed(() => mapErColumns(columnRows.value, stats.indexes.value, stats.foreignKeys.value))
  const transform = computed(() => `translate(${offset.value.x}px, ${offset.value.y}px) scale(${zoom.value})`)
  const zoomLabel = computed(() => `${Math.round(zoom.value * 100)}%`)

  /** Places the card in the middle of the canvas at the current zoom; a card taller than the canvas starts at its top. */
  function center() {
    const box = viewport.value
    const node = card.value
    if (!box || !node) return
    const width = node.offsetWidth * zoom.value
    const height = node.offsetHeight * zoom.value
    offset.value = { x: Math.round((box.clientWidth - width) / 2), y: Math.round(Math.max(24, (box.clientHeight - height) / 2)) }
  }
  /** Zooms around a point of the canvas (its middle by default) so that point stays put. */
  function setZoom(value: number, anchor?: { x: number; y: number }) {
    const next = clampZoom(value)
    const box = viewport.value
    if (!box || next === zoom.value) { zoom.value = next; return }
    const point = anchor || { x: box.clientWidth / 2, y: box.clientHeight / 2 }
    const ratio = next / zoom.value
    offset.value = { x: point.x - (point.x - offset.value.x) * ratio, y: point.y - (point.y - offset.value.y) * ratio }
    zoom.value = next
  }
  const zoomIn = () => setZoom(zoom.value + ER_ZOOM.step)
  const zoomOut = () => setZoom(zoom.value - ER_ZOOM.step)
  function onWheel(event: WheelEvent) {
    const box = viewport.value
    if (!box) return
    event.preventDefault()
    if (event.ctrlKey || event.metaKey) {
      const rect = box.getBoundingClientRect()
      setZoom(zoom.value * (event.deltaY < 0 ? 1.1 : 1 / 1.1), { x: event.clientX - rect.left, y: event.clientY - rect.top })
    }
    else offset.value = { x: offset.value.x - event.deltaX, y: offset.value.y - event.deltaY }
  }
  function onPointerDown(event: PointerEvent) {
    if (event.button !== 0) return
    drag = { pointer: event.pointerId, x: event.clientX, y: event.clientY, left: offset.value.x, top: offset.value.y }
    panning.value = true
    ;(event.currentTarget as HTMLElement).setPointerCapture(event.pointerId)
  }
  function onPointerMove(event: PointerEvent) {
    if (!drag || drag.pointer !== event.pointerId) return
    offset.value = { x: drag.left + event.clientX - drag.x, y: drag.top + event.clientY - drag.y }
  }
  function onPointerUp(event: PointerEvent) {
    if (!drag || drag.pointer !== event.pointerId) return
    drag = null
    panning.value = false
  }
  async function toggleFullscreen() {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await root.value?.requestFullscreen?.()
  }
  function onFullscreenChange() {
    fullscreen.value = document.fullscreenElement === root.value
    nextTick(center)
  }

  // The first drawing of each table starts centered at 100%.
  watch(() => [table.value, columns.value.length] as const, () => { zoom.value = 1; nextTick(center) })
  // Hidden columns change the card size; keep it in the middle.
  watch(settings, () => nextTick(center), { deep: true })
  onMounted(() => {
    fieldPanelOpen.value = true
    document.addEventListener('fullscreenchange', onFullscreenChange)
    nextTick(center)
  })
  onScopeDispose(() => {
    fieldPanelOpen.value = false
    document.removeEventListener('fullscreenchange', onFullscreenChange)
    if (document.fullscreenElement && document.fullscreenElement === root.value) document.exitFullscreen()
  })

  return {
    root, viewport, card, table, database, tableInfo, columns, loading, zoomLabel, transform, panning, fullscreen, settings, panelTab, fieldPanelHost, fieldPanelWidth,
    zoomIn, zoomOut, center, toggleFullscreen, onWheel, onPointerDown, onPointerMove, onPointerUp, canZoomIn: computed(() => zoom.value < ER_ZOOM.max),
    canZoomOut: computed(() => zoom.value > ER_ZOOM.min),
  }
}
