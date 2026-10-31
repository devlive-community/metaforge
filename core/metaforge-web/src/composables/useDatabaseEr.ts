import { computed, nextTick, onMounted, onScopeDispose, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import MetadataService from '@/services/metadata'
import { sourceManageContext, sourceManageLocation } from '@/model/source-manage'
import { ER_ZOOM, clampZoom } from '@/model/source-er'
import { drawingSize, layoutTables, mapDatabaseEr, relationPath, type ErBox, type ErRelation, type ErTable } from '@/model/database-er'

/**
 * ER diagram of the database the workbench header shows: every base table as a card on a grid,
 * foreign keys drawn between the rows they join. The canvas pans, zooms, fits the drawing and can
 * find a table; a card's header opens that table in the console.
 */
export function useDatabaseEr() {
  const { t } = useI18n()
  const route = useRoute()
  const router = useRouter()
  const context = computed(() => sourceManageContext(route))

  const loading = ref(false)
  const error = ref('')
  const tables = ref<ErTable[]>([])
  const relations = ref<ErRelation[]>([])
  const viewport = ref<HTMLElement | null>(null)
  const zoom = ref(1)
  const offset = ref({ x: 24, y: 24 })
  const panning = ref(false)
  const focused = ref('')
  const keyword = ref('')
  let drag: { pointer: number; x: number; y: number; left: number; top: number } | null = null
  let revision = 0

  const boxes = computed<ErBox[]>(() => layoutTables(tables.value))
  const boxByName = computed(() => new Map(boxes.value.map(box => [box.table.name, box])))
  const size = computed(() => drawingSize(boxes.value))
  const edges = computed(() => relations.value
      .map(relation => ({ relation, path: relationPath(boxByName.value, relation) }))
      .filter((edge): edge is { relation: ErRelation; path: string } => !!edge.path))
  const transform = computed(() => `translate(${offset.value.x}px, ${offset.value.y}px) scale(${zoom.value})`)
  const zoomLabel = computed(() => `${Math.round(zoom.value * 100)}%`)
  const related = (relation: ErRelation) => !!focused.value && (relation.table === focused.value || relation.referenceTable === focused.value)

  async function load() {
    const request = ++revision
    const { source, database } = context.value
    tables.value = []
    relations.value = []
    error.value = ''
    if (!source || !database) return
    loading.value = true
    const response = await MetadataService.getDatabaseEr(source, database)
    if (request !== revision) return
    loading.value = false
    if (!response.status || response.data?.isSuccessful === false) { error.value = response.message || response.data?.message || t('common.loadingFailed'); return }
    const mapped = mapDatabaseEr(response.data?.columns || [])
    tables.value = mapped.tables
    relations.value = mapped.relations
    await nextTick()
    fit()
  }

  /** Shows the whole drawing, never enlarged past 100%. */
  function fit() {
    const box = viewport.value
    if (!box || !boxes.value.length) return
    const margin = 32
    const scale = Math.min(1, (box.clientWidth - margin * 2) / size.value.width, (box.clientHeight - margin * 2) / size.value.height)
    zoom.value = clampZoom(scale)
    offset.value = { x: Math.round((box.clientWidth - size.value.width * zoom.value) / 2), y: margin }
  }
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
  /** Centres a table at 100% and highlights it with its relations. */
  function locate(name: string) {
    const target = boxByName.value.get(name) || boxes.value.find(box => box.table.name.toLowerCase().includes(name.trim().toLowerCase()))
    const box = viewport.value
    if (!target || !box) return
    focused.value = target.table.name
    zoom.value = 1
    offset.value = { x: Math.round(box.clientWidth / 2 - target.x - target.width / 2), y: Math.round(box.clientHeight / 2 - target.y - Math.min(target.height, box.clientHeight) / 2) }
  }
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
    if (event.button !== 0 || (event.target as HTMLElement).closest('button')) return
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
  const openTable = (table: string) => router.push(sourceManageLocation({ source: context.value.source, database: context.value.database, table, tab: 'structure' }))

  watch(() => [context.value.source, context.value.database], load)
  onMounted(load)
  onScopeDispose(() => { revision++ })

  return {
    context, loading, error, tables, relations, boxes, edges, viewport, transform, zoomLabel, panning, focused, keyword, size,
    zoomIn, zoomOut, fit, locate, related, onWheel, onPointerDown, onPointerMove, onPointerUp, openTable,
    canZoomIn: computed(() => zoom.value < ER_ZOOM.max), canZoomOut: computed(() => zoom.value > ER_ZOOM.min),
  }
}
