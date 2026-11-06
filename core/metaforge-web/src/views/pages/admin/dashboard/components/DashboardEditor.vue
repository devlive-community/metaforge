<template>
  <div class="flex h-full min-h-0 w-full min-w-0 flex-1 flex-col overflow-hidden bg-surface-hover"
       @pointerdown.capture="closeOutsidePanels" @keydown.esc="closeFloatingPanels" @keydown="deleteSelected">
    <header class="relative z-20 shrink-0 border-b border-line bg-surface shadow-mf-sm">
      <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 px-2 py-2 sm:px-4">
        <div class="flex min-w-0 flex-1 items-center gap-2.5">
          <StudioBackLink :to="SCREEN_LIST_PATH" :label="$t('dashboard.studio.backToList')"/>
          <div class="min-w-0">
            <h1 class="truncate text-sm font-semibold sm:text-base" :title="workspaceName">{{ workspaceName }}</h1>
          </div>
          <MfTag size="sm" class="shrink-0" :variant="draftStatus === 'published' ? 'success' : 'primary'">{{ $t('dashboard.studio.status.' + draftStatus) }}</MfTag>
        </div>

        <div class="flex shrink-0 flex-wrap items-center gap-1.5 max-sm:w-full">
          <MfButton size="sm" :disabled="!!saving" :aria-label="$t(wall ? 'dashboard.studio.switchDashboard' : 'dashboard.studio.switchWall')"
                    :title="$t(wall ? 'dashboard.studio.switchDashboard' : 'dashboard.studio.switchWall')" @click="switchMode">
            <PanelsTopLeft :size="15"/><span class="hidden xl:inline">{{ $t(wall ? 'dashboard.studio.switchDashboard' : 'dashboard.studio.switchWall') }}</span>
          </MfButton>
          <MfButton size="sm" :disabled="!!saving" @click="previewing = true"><Play :size="15"/>{{ $t('dashboard.studio.preview') }}</MfButton>
          <MfButton size="sm" :loading="saving === 'draft'" :disabled="!!saving" @click="save(false)"><Save :size="15"/>{{ $t('common.save') }}</MfButton>
          <MfButton size="sm" variant="primary" :loading="saving === 'publish'" :disabled="!!saving" @click="save(true)">
            <ArrowUpRight :size="15"/>{{ $t('dashboard.studio.publish') }}
          </MfButton>
        </div>
      </div>

      <div role="group" :aria-label="$t('dashboard.studio.toolbar')"
           class="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 border-t border-line px-2 py-1.5 sm:px-4" :inert="!!saving">
        <div class="flex min-w-0 flex-wrap items-center gap-1">
          <template v-if="wall">
            <MfSelect :model-value="board.resolutionMode === 'auto' ? 'auto' : board.width + 'x' + board.height" :options="resolutions" size="sm" class="w-41"
                      :aria-label="$t('dashboard.studio.resolution')" @update:model-value="resolution"/>
            <MfSelect v-model="zoom" :options="zoomOptions" size="sm" class="w-30" :aria-label="$t('dashboard.studio.zoom')"/>
          </template>
          <template v-else>
            <MfTag size="sm" class="hidden sm:inline-flex">{{ $t('dashboard.studio.columns', { count: 12 }) }}</MfTag>
            <MfButton v-for="item in devices" :key="item.id" size="sm" :variant="device === item.id ? 'secondary' : 'ghost'"
                      :aria-pressed="device === item.id" :aria-label="$t('dashboard.studio.device.' + item.id)"
                      :title="$t('dashboard.studio.device.' + item.id)" @click="device = item.id">
              <component :is="item.icon" :size="15"/><span class="hidden sm:inline">{{ $t('dashboard.studio.device.' + item.id) }}</span>
            </MfButton>
          </template>
        </div>

        <div class="flex flex-wrap items-center gap-1">
          <MfButton size="sm" variant="ghost" data-dashboard-panel="properties" @click="openCanvasSettings">{{ $t('dashboard.studio.canvasSettings') }}</MfButton>
          <MfCheckbox class="px-1 text-xs!" :model-value="board.grid" @update:model-value="updateBoard({ grid: !!$event })">{{ $t('dashboard.studio.grid') }}</MfCheckbox>
          <MfCheckbox class="px-1 text-xs!" :model-value="board.snap" @update:model-value="updateBoard({ snap: !!$event })">{{ $t('dashboard.studio.snap') }}</MfCheckbox>
          <MfButton v-if="wall" size="sm" variant="ghost" data-dashboard-panel="library"
                    :aria-label="$t('dashboard.studio.layers')" :title="$t('dashboard.studio.layers')" @click="showLayers">
            <Layers :size="15"/>
          </MfButton>
          <MfButton size="sm" variant="ghost" :aria-label="$t('dashboard.studio.undo')" :title="$t('dashboard.studio.undo')" :disabled="!undoItems.length" @click="undo"><Undo2 :size="15"/></MfButton>
          <MfButton size="sm" variant="ghost" :aria-label="$t('dashboard.studio.redo')" :title="$t('dashboard.studio.redo')" :disabled="!redoItems.length" @click="redo"><Redo2 :size="15"/></MfButton>
          <MfButton v-if="fullscreenAvailable" size="sm" variant="ghost"
                    :aria-label="$t(fullscreen ? 'dashboard.studio.exitFullscreen' : 'dashboard.studio.fullscreen')"
                    :title="$t(fullscreen ? 'dashboard.studio.exitFullscreen' : 'dashboard.studio.fullscreen')" @click="toggleFullscreen">
            <Minimize v-if="fullscreen" :size="15"/><Maximize v-else :size="15"/>
          </MfButton>
        </div>
      </div>
    </header>

    <div class="editor-workspace relative flex min-h-0 flex-1 overflow-hidden" :class="resizingPanel && 'panel-resizing'" :style="{ '--library-width': libraryWidth + 'px', '--properties-width': propertiesWidth + 'px' }" :inert="!!saving">
      <Transition name="side-panel-slide">
        <div v-show="libraryVisible" id="dashboard-library" data-dashboard-panel="library" tabindex="-1" :inert="!libraryVisible"
             class="library-panel inset-y-0 left-0 z-10 shrink-0 outline-none"
             :class="wide && pinnedPanels.library ? 'side-panel-docked relative' : 'absolute'" @focusout="schedulePanelHide('library')">
          <DashboardLibrary v-model:tab="libraryTab" class="side-panel-content" :reports="assetPage" :widgets="board.widgets" :selected-ids="selectedIds"
                            :wall="wall" :can-group="groupable.length > 1"
                            :loading="assetsLoading" :failed="assetsFailed" :page="page" :page-size="pageSize" :total="total"
                            :pinned="pinnedPanels.library" :pinnable="wide" @pin="togglePin('library')" @close="togglePanel('library')"
                            @add="add" @select="selectFromLibrary" @reorder="reorder" @search="loadAssets"
                            @context="(event, id) => editorBoard?.openContext(event, id)" @group="group(groupable)"/>
          <MfResizeHandle v-if="wide" v-model="librarySizing.width.value" class="absolute inset-y-0 -right-1 z-20 mx-0!" :min="280" :max="panelMax" :label="$t('dashboard.studio.resizeLibrary')" @pointerdown.capture="startPanelResize('library')" @change="librarySizing.save"/>
        </div>
      </Transition>

      <section ref="viewport" tabindex="-1" class="min-h-0 min-w-0 flex-1 overflow-auto outline-none"
               :class="wall && ['wall-editor-viewport flex flex-col', board.resolutionMode === 'fixed' && 'p-2']"
               :inert="!wide && !!compactPanel" :aria-label="$t('dashboard.studio.canvas')">
        <div :class="wall ? 'm-auto shrink-0' : 'mx-auto flex min-h-full min-w-0 flex-col'"
             :style="canvasSize">
          <DashboardBoard ref="editorBoard" :design="design" :reports="assets" :selected-id="selectedId" :selected-ids="selectedIds" editable
                          @select="select" @place="place" @add="add" @group="group" @ungroup="ungroup" @remove="remove"/>
        </div>
      </section>

      <Transition name="side-panel-backdrop">
        <button v-if="!wide && compactPanel" type="button" tabindex="-1" aria-hidden="true"
                class="absolute inset-0 z-[5] bg-black/25" @click="closeCompactPanel()"/>
      </Transition>

      <Transition name="side-panel-slide">
        <div v-show="propertiesVisible" id="dashboard-properties" data-dashboard-panel="properties" tabindex="-1" :inert="!propertiesVisible"
             class="properties-panel inset-y-0 right-0 z-10 shrink-0 outline-none"
             :class="wide && pinnedPanels.properties ? 'side-panel-docked relative' : 'absolute'" @focusout="schedulePanelHide('properties')">
          <DashboardInspector ref="inspector" class="side-panel-content" :design="design" :board="board" :widget="selected" :reports="assets"
                              :group-rect="selectedGroup.length ? dashboardGroupBounds(selectedGroup) : undefined" :group-count="selectedGroup.length"
                              :group-movable="selectedGroup.every(widget => widget.movable)" :group-resizable="selectedGroup.every(widget => widget.resizable)"
                              :pinned="pinnedPanels.properties" :pinnable="wide" @pin="togglePin('properties')" @close="togglePanel('properties')"
                              @widget="updateWidget" @board="updateBoard" @page="updatePage" @group="updateGroup" @ungroup="ungroup(selectedId)"
                              @children="assignChildren" @add-child="kind => add(kind, undefined, selectedId)" @select-child="id => selectedId = id" @detach="detachChild"
                              @background-image="setBackgroundImage"
                              @remove="remove" @save="save(false)" @preview="previewing = true"/>
          <MfResizeHandle v-if="wide" v-model="propertiesSizing.width.value" reverse class="absolute inset-y-0 -left-1 z-20 mx-0!" :min="280" :max="panelMax" :label="$t('dashboard.studio.resizeProperties')" @pointerdown.capture="startPanelResize('properties')" @change="propertiesSizing.save"/>
        </div>
      </Transition>

      <MfButton id="dashboard-library-toggle" data-dashboard-panel="library" size="sm"
                class="panel-toggle panel-toggle-left rounded-l-none! rounded-r-mf-md!"
                aria-controls="dashboard-library" :aria-expanded="libraryVisible"
                :aria-label="$t('dashboard.studio.library')"
                :title="$t(libraryVisible ? 'dashboard.studio.hideLibrary' : 'dashboard.studio.showLibrary')"
                @pointermove="previewPanel('library', $event)" @focusout="schedulePanelHide('library')"
                @click="togglePanel('library', $event)">
        <ChevronLeft v-if="libraryVisible" :size="15"/><ChevronRight v-else :size="15"/>
      </MfButton>
      <MfButton id="dashboard-properties-toggle" data-dashboard-panel="properties" size="sm"
                class="panel-toggle panel-toggle-right rounded-l-mf-md! rounded-r-none!"
                aria-controls="dashboard-properties" :aria-expanded="propertiesVisible"
                :aria-label="$t('dashboard.studio.properties')"
                :title="$t(propertiesVisible ? 'dashboard.studio.hideProperties' : 'dashboard.studio.showProperties')"
                @pointermove="previewPanel('properties', $event)" @focusout="schedulePanelHide('properties')"
                @click="togglePanel('properties', $event)">
        <ChevronRight v-if="propertiesVisible" :size="15"/><ChevronLeft v-else :size="15"/>
      </MfButton>
    </div>
  </div>

  <MfModal v-if="previewing" :open="true" :title="workspaceName"
           width="100vw" height="100dvh" overlay-class="p-0!" panel-class="rounded-none!"
           body-class="flex min-h-0 flex-col overflow-hidden"
           trap-focus @close="previewing = false">
    <DashboardPresentationCanvas :design="design" :reports="assets"/>
    <template #footer>
      <MfButton size="sm" @click="previewing = false"><Pencil :size="14"/>{{ $t('dashboard.studio.backToEditor') }}</MfButton>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { SCREEN_LIST_PATH, screenDesignPath, screenViewPath } from '@/model/screen-route'
import { computed, nextTick, onMounted, onScopeDispose, reactive, ref, shallowRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import { usePanelWidth } from '@/composables/usePanelWidth'
import { v4 as uuid } from 'uuid'
import { ArrowUpRight, ChevronLeft, ChevronRight, Laptop, Layers, Maximize, Minimize, PanelsTopLeft, Pencil, Play, Redo2, Save, Smartphone, Tablet, Undo2 } from '@lucide/vue'
import StudioBackLink from '@/views/components/studio/StudioBackLink.vue'
import DashboardService from '@/services/dashboard'
import ReportService from '@/services/report'
import { FilterModel } from '@/model/filter'
import type { DashboardModel } from '@/model/dashboard'
import type { ReportModel } from '@/model/report'
import { constrainDashboardGroup, constrainDashboardRect, copyDashboardDesign, dashboardCanContain, dashboardCanvasColor, dashboardContainerTool, dashboardDescendantIds, dashboardGridTarget, dashboardGroupBounds, dashboardLayerMembers, dashboardOverlap,
  dashboardPlacement, dashboardReportCodes, dashboardScreenEffectTool, dashboardTools, dashboardWallPresets, dashboardWallResolution, dashboardWidget, dashboardWidgetLabelKey,
  groupDashboardWidgets, readDashboardDocument, reorderDashboardLayers, resizeDashboardGroup, switchDashboardLayout, type DashboardBoard as Board, type DashboardDesign,
  type DashboardMode, type DashboardRect, type DashboardWidget, type DashboardWidgetKind } from '@/model/dashboard-layout'
import DashboardBoard from './DashboardBoard.vue'
import DashboardInspector from './DashboardInspector.vue'
import DashboardLibrary from './DashboardLibrary.vue'
import DashboardPresentationCanvas from './DashboardPresentationCanvas.vue'

const props = defineProps<{ info?: DashboardModel | null }>()
const { t } = useI18n()
const router = useRouter()
const existing = readDashboardDocument(props.info?.configure, props.info?.name, props.info?.description, t)
const design = ref(existing.draft)
const published = shallowRef(existing.published)
const code = ref(props.info?.code)
const selectedIds = ref<string[]>([])
const selectedId = computed({ get: () => selectedIds.value.at(-1) ?? '', set: id => { selectedIds.value = id ? [id] : [] } })
const viewport = ref<HTMLElement>()
const editorBoard = ref<InstanceType<typeof DashboardBoard>>()
const wall = computed(() => design.value.mode === 'wall')
const board = computed(() => design.value.boards[design.value.mode])
const otherBoard = computed(() => design.value.boards[wall.value ? 'dashboard' : 'wall'])
const selected = computed(() => board.value.widgets.find(item => item.id === selectedId.value))
const selectedGroup = computed(() => wall.value && selectedId.value ? board.value.widgets.filter(widget => widget.groupId === selectedId.value) : [])
const groupable = computed(() => wall.value ? board.value.widgets.filter(widget => !widget.parentId && !widget.groupId && !dashboardScreenEffectTool(widget.kind) && selectedIds.value.includes(widget.id)).map(widget => widget.id) : [])
const previewing = ref(false)
const saving = ref<'draft' | 'publish'>()
type Panel = 'library' | 'properties'
const panels: Panel[] = ['library', 'properties']
const desktopPanels = reactive<Record<Panel, boolean>>({ library: true, properties: true })
const pinnedPanels = reactive<Record<Panel, boolean>>({ library: true, properties: true })
const panelTimers: Partial<Record<Panel, number>> = {}
let hoveredPanel: Panel | undefined
const compactPanel = ref<Panel>()
const desktopMedia = window.matchMedia('(min-width: 64rem)')
const wide = ref(desktopMedia.matches)
const libraryVisible = computed(() => wide.value ? desktopPanels.library : compactPanel.value === 'library')
const propertiesVisible = computed(() => wide.value ? desktopPanels.properties : compactPanel.value === 'properties')
const viewportWidth = ref(window.innerWidth)
const librarySizing = usePanelWidth('dashboard.library.width', { initial: 320, min: 280, max: 520 })
const propertiesSizing = usePanelWidth('dashboard.properties.width', { initial: 320, min: 280, max: 520 })
const panelMax = computed(() => Math.min(520, Math.max(280, Math.floor((viewportWidth.value - 320) / 2))))
const libraryWidth = computed(() => Math.min(librarySizing.width.value, wide.value ? panelMax.value : viewportWidth.value * .85))
const propertiesWidth = computed(() => Math.min(propertiesSizing.width.value, wide.value ? panelMax.value : viewportWidth.value * .85))
const resizingPanel = ref<Panel>()
function startPanelResize(panel: Panel) { resizingPanel.value = panel; panels.forEach(cancelPanelTimer) }
function endPanelResize() { const panel = resizingPanel.value; resizingPanel.value = undefined; if (panel) schedulePanelHide(panel) }
function resizeViewport() { viewportWidth.value = window.innerWidth }
const fullscreen = ref(false)
const fullscreenAvailable = document.fullscreenEnabled
let enteredFullscreen = false
const libraryTab = ref<'tools' | 'assets' | 'layers'>('tools')
const inspector = ref<InstanceType<typeof DashboardInspector>>()
const workspaceName = computed(() => design.value.name || t(wall.value ? 'dashboard.studio.untitledWall' : 'dashboard.studio.untitled'))
const draftStatus = computed(() => !published.value ? 'draft' : JSON.stringify(published.value) === JSON.stringify(design.value) ? 'published' : 'changed')

const devices = [{ id: 'desktop', icon: Laptop }, { id: 'tablet', icon: Tablet }, { id: 'phone', icon: Smartphone }] as const
const device = ref<typeof devices[number]['id']>('desktop')
const zoom = ref<'fit' | number>('fit')
const zoomOptions = computed(() => [
  { value: 'fit', label: t('dashboard.studio.zoomFit') },
  ...[25, 50, 75, 100, 125].map(value => ({ value, label: value + '%' })),
])
const resolutions = computed(() => [{ value: 'auto', label: t('dashboard.studio.resolutionAuto') }, ...dashboardWallPresets])
const canvasSize = computed(() => {
  if (!wall.value) {
    return { width: '100%', maxWidth: device.value === 'tablet' ? '768px' : device.value === 'phone' ? '390px' : undefined }
  }
  const { width, height, resolutionMode } = board.value
  if (resolutionMode === 'auto') {
    const percent = zoom.value === 'fit' ? 100 : zoom.value
    return { width: percent + 'cqw', height: percent + 'cqh' }
  }
  return { width: zoom.value === 'fit' ? `min(100cqw, calc(100cqh * ${width / height}))` : `${width * zoom.value / 100}px` }
})

const assetCache = shallowRef(new Map((props.info?.reports ?? []).flatMap(report => report.code ? [[report.code, report] as const] : [])))
const assets = computed(() => [...assetCache.value.values()])
const assetPage = shallowRef<ReportModel[]>([])
const assetsLoading = ref(false)
const assetsFailed = ref(false)
const pageSize = 12
const total = ref(0)
const page = ref(1)
let request = 0
onScopeDispose(() => { request++ })

async function loadAssets(keyword = '', current = 1) {
  const id = ++request
  assetsLoading.value = true
  assetsFailed.value = false
  page.value = current
  try {
    const response = await ReportService.getAll({ ...new FilterModel(), page: current, size: pageSize, keyword })
    if (id !== request) return
    if (!response.status || !Array.isArray(response.data?.content)) throw new Error('Invalid reports')
    assetPage.value = response.data.content.filter((item: ReportModel) => !!item.code)
    total.value = response.data.total
    assetCache.value = new Map([...assetCache.value, ...assetPage.value.map(item => [item.code!, item] as const)])
  }
  catch { if (id === request) assetsFailed.value = true }
  finally { if (id === request) assetsLoading.value = false }
}
void loadAssets()

const undoItems = shallowRef<DashboardDesign[]>([])
const redoItems = shallowRef<DashboardDesign[]>([])
function record() {
  undoItems.value = [...undoItems.value.slice(-49), copyDashboardDesign(design.value)]
  redoItems.value = []
}
function undo() {
  const previous = undoItems.value.at(-1)
  if (!previous) return
  redoItems.value = [...redoItems.value, copyDashboardDesign(design.value)]
  undoItems.value = undoItems.value.slice(0, -1)
  design.value = previous
  selectedId.value = ''
}
function redo() {
  const next = redoItems.value.at(-1)
  if (!next) return
  undoItems.value = [...undoItems.value, copyDashboardDesign(design.value)]
  redoItems.value = redoItems.value.slice(0, -1)
  design.value = next
  selectedId.value = ''
}

function switchMode() {
  record()
  const skipped = switchDashboardLayout(design.value)
  if (skipped) MfMessage.warning(t('dashboard.studio.switchLayoutFull', { count: skipped }))
  selectedId.value = ''
}
function select(id: string, additive = false) {
  if (wall.value) id = board.value.widgets.find(widget => widget.id === id)?.groupId ?? id
  if (!wall.value || !additive || !id || !board.value.widgets.some(widget => widget.id === id)) { selectedId.value = id; return }
  const previous = selectedIds.value.filter(key => board.value.widgets.some(widget => widget.id === key))
  selectedIds.value = previous.includes(id) ? previous.filter(key => key !== id) : [...previous, id]
}

function panelElement(panel: Panel) { return document.getElementById('dashboard-' + panel) }
function panelToggle(panel: Panel) { return document.getElementById('dashboard-' + panel + '-toggle') }
function cancelPanelTimer(panel: Panel) {
  window.clearTimeout(panelTimers[panel])
  delete panelTimers[panel]
}
function isFloating(panel: Panel) { return wide.value && !pinnedPanels[panel] }
function closeDesktopPanel(panel: Panel, restoreFocus = false) {
  cancelPanelTimer(panel)
  desktopPanels[panel] = false
  if (restoreFocus && panelElement(panel)?.contains(document.activeElement)) panelToggle(panel)?.focus()
}
function togglePin(panel: Panel) {
  cancelPanelTimer(panel)
  pinnedPanels[panel] = !pinnedPanels[panel]
}
function togglePanel(panel: Panel, event?: MouseEvent) {
  if (wide.value) {
    if (desktopPanels[panel]) closeDesktopPanel(panel, true)
    else {
      openPanel(panel)
      if (isFloating(panel) && event?.detail === 0) void nextTick(() => panelElement(panel)?.focus())
    }
  }
  else if (compactPanel.value === panel) closeCompactPanel()
  else compactPanel.value = panel
}
function openPanel(panel: Panel) {
  cancelPanelTimer(panel)
  if (wide.value) desktopPanels[panel] = true
  else compactPanel.value = panel
}
function previewPanel(panel: Panel, event: PointerEvent) {
  if (event.pointerType !== 'mouse' || event.buttons || !isFloating(panel) || desktopPanels[panel] || panelTimers[panel] !== undefined) return
  panelTimers[panel] = window.setTimeout(() => openPanel(panel), 150)
}
function schedulePanelHide(panel: Panel) {
  cancelPanelTimer(panel)
  if (resizingPanel.value || !isFloating(panel) || !desktopPanels[panel] || hoveredPanel === panel) return
  panelTimers[panel] = window.setTimeout(() => {
    cancelPanelTimer(panel)
    const focus = document.activeElement
    const keyboardActive = focus?.matches(':focus-visible') && (panelElement(panel)?.contains(focus) || panelToggle(panel) === focus)
    const selectOpen = [...document.querySelectorAll('[role="listbox"]')].some(listbox => panelAt(listbox) === panel)
    if (!keyboardActive && !selectOpen) closeDesktopPanel(panel)
  }, 280)
}
function panelAt(target: EventTarget | null): Panel | undefined {
  if (!(target instanceof Element)) return
  const panel = target.closest<HTMLElement>('[data-dashboard-panel]')?.dataset.dashboardPanel
  if (panel === 'library' || panel === 'properties') return panel

  // Select menus are teleported to the document and keep their trigger's width.
  const listbox = target.closest('[role="listbox"]')?.getBoundingClientRect()
  if (listbox) return panels.find(side => {
    if (!isFloating(side) || !desktopPanels[side]) return false
    const bounds = panelElement(side)?.getBoundingClientRect()
    return bounds && listbox.left >= bounds.left && listbox.right <= bounds.right
  })
}
function trackPanelHover(event: PointerEvent) {
  if (!wide.value || event.pointerType !== 'mouse') return
  const panel = panelAt(event.type === 'pointerout' ? event.relatedTarget : event.target)
  if (hoveredPanel === panel) return
  const previous = hoveredPanel
  hoveredPanel = panel
  if (panel) cancelPanelTimer(panel)
  if (previous) schedulePanelHide(previous)
}
function closeOutsidePanels(event: PointerEvent) {
  const panel = panelAt(event.target)
  for (const side of panels) {
    if (side !== panel && isFloating(side) && desktopPanels[side]) closeDesktopPanel(side)
  }
}
function closeFloatingPanels(event: KeyboardEvent) {
  if (event.defaultPrevented) return
  if (!wide.value) closeCompactPanel(event)
  else for (const panel of panels) {
    if (isFloating(panel) && desktopPanels[panel]) closeDesktopPanel(panel, true)
  }
}
function closeCompactPanel(event?: KeyboardEvent) {
  const panel = compactPanel.value
  if (wide.value || !panel || event?.defaultPrevented) return
  compactPanel.value = undefined
  panelToggle(panel)?.focus()
}
function selectFromLibrary(id: string, event: MouseEvent) {
  const additive = wall.value && (event.shiftKey || event.ctrlKey || event.metaKey)
  select(id, additive)
  if (!additive) closeCompactPanel()
}
function openCanvasSettings() {
  select('')
  openPanel('properties')
  void nextTick(() => inspector.value?.showPageSettings())
}
function showLayers() {
  libraryTab.value = 'layers'
  openPanel('library')
}
function syncViewport() {
  wide.value = desktopMedia.matches
  hoveredPanel = undefined
  for (const panel of panels) {
    cancelPanelTimer(panel)
    if (!pinnedPanels[panel]) desktopPanels[panel] = false
  }
  if (wide.value) compactPanel.value = undefined
}
function syncFullscreen() {
  fullscreen.value = enteredFullscreen && document.fullscreenElement === document.documentElement
  if (!fullscreen.value) enteredFullscreen = false
}
async function toggleFullscreen() {
  try {
    if (fullscreen.value) await document.exitFullscreen()
    else {
      await document.documentElement.requestFullscreen()
      enteredFullscreen = true
    }
    syncFullscreen()
  }
  catch { MfMessage.error(t('dashboard.studio.fullscreenFailed')) }
}
onMounted(() => {
  window.addEventListener('resize', resizeViewport)
  window.addEventListener('pointerup', endPanelResize)
  window.addEventListener('pointercancel', endPanelResize)
  desktopMedia.addEventListener('change', syncViewport)
  document.addEventListener('fullscreenchange', syncFullscreen)
  document.addEventListener('pointerover', trackPanelHover)
  document.addEventListener('pointerout', trackPanelHover)
})
onScopeDispose(() => {
  window.removeEventListener('resize', resizeViewport)
  window.removeEventListener('pointerup', endPanelResize)
  window.removeEventListener('pointercancel', endPanelResize)
  desktopMedia.removeEventListener('change', syncViewport)
  document.removeEventListener('fullscreenchange', syncFullscreen)
  document.removeEventListener('pointerover', trackPanelHover)
  document.removeEventListener('pointerout', trackPanelHover)
  panels.forEach(cancelPanelTimer)
  if (enteredFullscreen && document.fullscreenElement === document.documentElement) void document.exitFullscreen().catch(() => {})
})
function updatePage(change: Partial<Pick<DashboardDesign, 'name' | 'description'>>) {
  record()
  Object.assign(design.value, change)
}
function updateBoard(change: Partial<Board>) {
  const previous = board.value
  record()
  const next = { ...previous, ...change }
  if (change.resolutionMode) zoom.value = 'fit'
  if (wall.value && (next.width !== previous.width || next.height !== previous.height)) {
    next.widgets = previous.widgets.map(widget => {
      const ratioX = next.width / previous.width
      const ratioY = next.height / previous.height
      return { ...widget, ...constrainDashboardRect({ x: widget.x * ratioX, y: widget.y * ratioY, w: widget.w * ratioX, h: widget.h * ratioY }, 'wall', next) }
    })
  }
  design.value.boards[design.value.mode] = next
}
function setBackgroundImage(mode: DashboardMode, url: string) {
  record()
  design.value.boards[mode].backgroundImage = url
}
function resolution(value: string | number | undefined) {
  const update = dashboardWallResolution(value)
  if (!update) return
  updateBoard(update)
}
function add(value: string, position?: Pick<DashboardRect, 'x' | 'y'>, parentId?: string) {
  if (board.value.widgets.length >= 100) {
    MfMessage.warning(t('dashboard.studio.widgetLimit'))
    return
  }
  const reportCode = value.startsWith('report:') ? value.slice(7) : undefined
  const asset = reportCode ? assetCache.value.get(reportCode) : undefined
  if (reportCode ? !asset : !dashboardTools.includes(value as typeof dashboardTools[number])) return
  const kind = (asset ? 'report' : value) as DashboardWidgetKind
  const widget = dashboardWidget(uuid(), kind, asset?.name || t(dashboardWidgetLabelKey(kind)), design.value.mode)
  const containerId = !dashboardScreenEffectTool(kind) ? parentId : undefined
  if (containerId && !dashboardCanContain([...board.value.widgets, widget], containerId, widget.id)) {
    MfMessage.warning(t('dashboard.studio.containerDepthLimit'))
    return
  }
  const rect = dashboardPlacement(board.value, design.value.mode, kind)
  const proposed = position && !containerId && !dashboardScreenEffectTool(kind) ? { ...rect, ...position } : undefined
  const placement = proposed ? (wall.value ? constrainDashboardRect(proposed, 'wall', board.value) : dashboardGridTarget(board.value, proposed)) : rect
  if (!placement) {
    MfMessage.warning(t('dashboard.studio.layoutConflict'))
    return
  }
  record()
  board.value.widgets.push({ ...widget, ...placement, reportCode, parentId: containerId,
    text: kind === 'text' ? t('dashboard.studio.textPrompt') : '' })
  select(widget.id)
  closeCompactPanel()
  void nextTick(() => {
    if (selectedId.value === widget.id) editorBoard.value?.focusWidget(widget.id)
  })
}
function place(id: string, position: DashboardRect) {
  const item = board.value.widgets.find(widget => widget.id === id)
  if (!item) {
    if (id === selectedId.value) updateGroup(position)
    return
  }
  if (item.x === position.x && item.y === position.y && item.w === position.w && item.h === position.h) return
  record()
  Object.assign(item, position)
}
function updateWidget(change: Partial<DashboardWidget>) {
  const widget = selected.value
  if (!widget) return
  const kind = change.kind ?? widget.kind
  const rect = change.kind && dashboardScreenEffectTool(widget.kind) && !dashboardScreenEffectTool(kind)
    ? dashboardPlacement(board.value, design.value.mode, kind) : constrainDashboardRect({ ...widget, ...change }, design.value.mode, board.value)
  if (!wall.value && !widget.parentId && !dashboardScreenEffectTool(kind) && board.value.widgets.some(item => !item.parentId && !dashboardScreenEffectTool(item.kind) && item.id !== widget.id && dashboardOverlap(item, rect))) {
    MfMessage.warning(t('dashboard.studio.layoutConflict'))
    return
  }
  record()
  Object.assign(widget, change, rect)
  const counterpart = otherBoard.value.widgets.find(item => item.id === widget.id)
  if (counterpart) {
    const { x, y, w, h, ...content } = change
    Object.assign(counterpart, content)
  }
  if (change.kind && !dashboardContainerTool(kind)) {
    for (const mode of ['dashboard', 'wall'] as const) {
      const layout = design.value.boards[mode]
      for (const child of layout.widgets.filter(item => item.parentId === widget.id)) {
        const placement = dashboardPlacement(layout, mode, child.kind)
        child.parentId = undefined
        Object.assign(child, placement)
      }
    }
  }
}
function remove(id = selectedId.value) {
  const targetId = board.value.widgets.find(widget => widget.id === id)?.groupId ?? id
  const members = dashboardLayerMembers(board.value.widgets, targetId)
  if (!members.length) return
  const ids = dashboardDescendantIds([...board.value.widgets, ...otherBoard.value.widgets], members.map(widget => widget.id))
  record()
  board.value.widgets = board.value.widgets.filter(item => !ids.has(item.id))
  otherBoard.value.widgets = otherBoard.value.widgets.filter(item => !ids.has(item.id))
  const counts = new Map<string, number>()
  for (const item of design.value.boards.wall.widgets) if (item.groupId) counts.set(item.groupId, (counts.get(item.groupId) ?? 0) + 1)
  for (const item of design.value.boards.wall.widgets) if (item.groupId && counts.get(item.groupId) === 1) item.groupId = undefined
  selectedId.value = ''
}
function deleteSelected(event: KeyboardEvent) {
  if ((!selected.value && !selectedGroup.value.length) || saving.value || previewing.value || event.defaultPrevented || event.isComposing
      || event.metaKey || event.ctrlKey || event.altKey || (event.key !== 'Delete' && event.key !== 'Backspace')) return
  const target = event.target
  if (target instanceof HTMLElement
      && (target.isContentEditable || target.closest('input, textarea, select, [role="textbox"], [role="combobox"], [role="listbox"]'))) return
  event.preventDefault()
  remove()
  if (target instanceof Node && viewport.value?.contains(target)) viewport.value.focus({ preventScroll: true })
}
function reorder(from: string, to: string, above: boolean) {
  const order = reorderDashboardLayers(board.value.widgets, from, to, above)
  if (order === board.value.widgets) return
  record()
  board.value.widgets = order
}
function group(ids: string[]) {
  if (!wall.value) return
  const id = uuid()
  const widgets = groupDashboardWidgets(board.value.widgets, ids, id)
  if (widgets === board.value.widgets) return
  record()
  board.value.widgets = widgets
  selectedId.value = id
  libraryTab.value = 'layers'
  void nextTick(() => editorBoard.value?.focusWidget(id))
}
function ungroup(id: string) {
  const members = board.value.widgets.filter(widget => widget.groupId === id)
  if (!members.length) return
  record()
  for (const item of members) item.groupId = undefined
  selectedIds.value = members.map(item => item.id)
}
function updateGroup(change: Partial<DashboardRect>) {
  const members = selectedGroup.value
  if (!members.length) return
  const current = dashboardGroupBounds(members)
  const next = constrainDashboardGroup({ ...current, ...change }, members, board.value)
  if (current.x === next.x && current.y === next.y && current.w === next.w && current.h === next.h) return
  const placements = resizeDashboardGroup(members, next)
  record()
  for (const item of members) Object.assign(item, placements.get(item.id))
}
function assignChildren(ids: string[]) {
  const parent = selected.value
  if (!parent || !dashboardContainerTool(parent.kind)) return
  const allowed = new Set(ids.filter(id => dashboardCanContain(board.value.widgets, parent.id, id)))
  const current = board.value.widgets.filter(widget => widget.parentId === parent.id)
  if (current.length === allowed.size && current.every(widget => allowed.has(widget.id))) return
  record()
  for (const child of current) if (!allowed.has(child.id)) {
    const placement = dashboardPlacement(board.value, design.value.mode, child.kind)
    child.parentId = undefined
    Object.assign(child, placement)
  }
  for (const child of board.value.widgets) if (allowed.has(child.id)) child.parentId = parent.id
}
function detachChild() {
  const child = selected.value
  if (!child?.parentId) return
  record()
  const placement = dashboardPlacement(board.value, design.value.mode, child.kind)
  child.parentId = undefined
  Object.assign(child, placement)
}

async function save(publish: boolean) {
  if (saving.value) return
  if (!design.value.name.trim()) {
    selectedId.value = ''
    openPanel('properties')
    await nextTick()
    try { await inspector.value?.validate() }
    catch { /* Field validation is shown in the inspector. */ }
    return
  }
  saving.value = publish ? 'publish' : 'draft'
  const draft = copyDashboardDesign(design.value)
  draft.name = draft.name.trim()
  for (const mode of ['dashboard', 'wall'] as const) {
    draft.boards[mode].background = dashboardCanvasColor(draft.boards[mode].background)
    for (const widget of draft.boards[mode].widgets) {
      if (!/^#[0-9a-f]{6}$/i.test(widget.background)) widget.background = ''
      if (!/^#[0-9a-f]{6}$/i.test(widget.color)) widget.color = ''
    }
  }
  const document = { version: 3 as const, draft, published: publish ? draft : published.value }
  const payload: DashboardModel = {
    code: code.value, name: draft.name, description: draft.description,
    avatar: props.info?.avatar, version: '3.0', configure: JSON.stringify(document),
    reports: dashboardReportCodes(document).filter(code => assetCache.value.has(code)).map(code => ({ code }))
  }
  try {
    const response = await DashboardService.saveOrUpdate(payload)
    if (!response.status || !response.data?.code) throw new Error(response.message || t('dashboard.studio.saveFailed'))
    const isNew = !code.value
    code.value = response.data.code
    design.value = draft
    published.value = document.published
    MfMessage.success(t(publish ? 'dashboard.studio.published' : 'dashboard.studio.saved'))
    if (publish) await router.push(screenViewPath(code.value!))
    else if (isNew) await router.replace(screenDesignPath(code.value!))
  }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('dashboard.studio.saveFailed')) }
  finally { saving.value = undefined }
}
</script>

<style scoped>
.wall-editor-viewport { container-type: size; }
.library-panel, .properties-panel { display: flex; width: var(--panel-width); }
.library-panel { --panel-width: var(--library-width); --panel-slide-offset: -100%; }
.properties-panel { --panel-width: var(--properties-width); --panel-slide-offset: 100%; justify-content: flex-end; }
.side-panel-content { width: 100%; flex-shrink: 0; }
.side-panel-docked .side-panel-content { width: var(--panel-width); }
.panel-toggle { position: absolute; top: 50%; z-index: 15; width: 25px; height: 46px; padding: 0; transform: translateY(-50%); }
.panel-toggle-left { --panel-width: var(--library-width); left: 0; }
.panel-toggle-right { --panel-width: var(--properties-width); right: 0; }
.panel-toggle-left[aria-expanded='true'] { left: var(--panel-width); }
.panel-toggle-right[aria-expanded='true'] { right: var(--panel-width); }
@media (prefers-reduced-motion: no-preference) {
  .side-panel-slide-enter-active, .side-panel-slide-leave-active {
    transition: transform var(--mf-duration-base) var(--mf-easing);
  }
  .side-panel-slide-enter-active.side-panel-docked, .side-panel-slide-leave-active.side-panel-docked {
    overflow: clip;
    transition-property: width;
  }
  .side-panel-slide-enter-from:not(.side-panel-docked), .side-panel-slide-leave-to:not(.side-panel-docked) {
    transform: translateX(var(--panel-slide-offset));
  }
  .side-panel-slide-enter-from.side-panel-docked, .side-panel-slide-leave-to.side-panel-docked { width: 0; }
  .side-panel-backdrop-enter-active, .side-panel-backdrop-leave-active { transition: opacity var(--mf-duration-base) var(--mf-easing); }
  .side-panel-backdrop-enter-from, .side-panel-backdrop-leave-to { opacity: 0; }
  .panel-toggle { transition: left var(--mf-duration-base) var(--mf-easing), right var(--mf-duration-base) var(--mf-easing); }
  .panel-resizing .panel-toggle { transition: none; }
}
</style>
