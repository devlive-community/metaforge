<template>
  <section ref="frame" class="dashboard-frame relative isolate min-w-0" :class="[wall ? ['wall-frame', 'wall-' + board.theme] : 'dashboard-paper', editable && 'is-editing', board.grid && editable && 'show-grid']"
           :style="[frameStyle, transparent && { backgroundColor: 'transparent' }]" @dragover="onDragOver" @drop="onDrop" @click.self="emit('select', '')">
    <DashboardCanvasBackground v-if="editable || thumbnail" :board="board" class="-z-10"/>
    <header v-if="wall && board.showTitle" class="wall-heading">
      <span/>
      <h2 class="truncate text-center font-bold" :title="design.name">{{ design.name || $t('dashboard.studio.untitledWall') }}</h2>
      <span/>
      <time v-if="refreshTime" class="wall-timestamp" :title="$t('queryChart.refreshedAt', { time: refreshTime })">{{ refreshTime }}</time>
    </header>
    <header v-else-if="!wall && (board.showTitle || (!editable && !thumbnail && (hasDate || data.policies.value.size)))" class="flex shrink-0 flex-wrap items-start justify-between gap-3 px-1 pb-3">
      <div v-if="board.showTitle" class="min-w-0">
        <h2 class="break-words text-xl font-bold">{{ design.name || $t('dashboard.studio.untitled') }}</h2>
        <p v-if="design.description" class="mt-1 whitespace-pre-wrap break-words text-xs text-content-tertiary">{{ design.description }}</p>
      </div>
      <div v-if="!editable && !thumbnail && (hasDate || data.policies.value.size)" class="ml-auto flex flex-wrap items-center gap-2">
        <MfSelect v-if="hasDate" :model-value="days" :options="periods" class="w-32" size="sm"
                  :aria-label="$t('dashboard.studio.period')" @update:model-value="selectPeriod"/>
        <MfButton v-if="data.policies.value.size" size="sm" :loading="data.pending.value.size > 0"
                  @click="refreshBoard"><RefreshCw :size="14"/>{{ $t('common.refresh') }}</MfButton>
      </div>
    </header>

    <div v-if="selection" class="mb-2 flex flex-wrap items-center gap-2 text-xs">
      <MfTag variant="primary">{{ $t('dashboard.studio.filteredRows', { count: selection.rows.size }) }}</MfTag>
      <MfButton size="sm" variant="ghost" @click="selection = undefined">{{ $t('queryChart.clearLinkedFilter') }}</MfButton>
    </div>

    <div v-if="editable && screenEffects.length" class="mb-2 flex shrink-0 flex-wrap items-center gap-1.5" role="group" :aria-label="$t('dashboard.studio.screenEffects')">
      <MfButton v-for="effect in screenEffects" :key="effect.id" size="sm" :variant="selectedId === effect.id ? 'secondary' : 'ghost'"
                :data-widget-id="effect.id" :aria-pressed="selectedId === effect.id" :aria-label="$t('dashboard.studio.selectWidget', { name: effect.name })"
                @click="emit('select', effect.id)" @contextmenu="openContext($event, effect.id)">
        <Sparkles :size="14"/><span class="max-w-36 truncate">{{ effect.name }}</span>
      </MfButton>
    </div>
    <div ref="surface" class="board-surface" :class="wall ? 'wall-surface' : 'paper-surface'" @click.self="emit('select', '')"
         @contextmenu="openContext">
      <MfEmpty v-if="!board.widgets.length" :title="$t('dashboard.studio.emptyCanvas')" :description="editable ? $t('dashboard.studio.emptyHint') : undefined"
               class="board-empty"/>
      <article v-for="widget in regularWidgets" :key="widget.id"
               :data-animation-widget="editable ? widget.id : undefined"
               class="board-widget flex min-h-0 min-w-0 flex-col overflow-hidden"
               :class="{ 'is-selected': editable && !wall && selectedId === widget.id, 'is-marked': editable && wall && selectedIds?.includes(widget.id) && selectedId !== widget.id,
                         'is-framed': editable && wall && !!selectedId && (selectedId === widget.id || selectedId === widget.groupId),
                         'dashboard-widget-motion': !editable && !!widget.animation && widget.animation.effect !== 'none', 'motion-on-hover': widget.animation?.trigger === 'hover',
                         'widget-card': widget.showCard, 'widget-border': widget.showCard && widget.border, 'wall-widget': wall }"
               :style="widgetStyle(widget)">
        <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden" :inert="editable && !dashboardContainerTool(widget.kind)">
          <div v-if="widget.showTitle || (widget.expandable && !editable)" class="mb-1 flex shrink-0 items-center gap-1">
            <h3 v-if="widget.showTitle" class="min-w-0 flex-1 truncate font-semibold" :style="{ fontSize: widget.titleSize + 'px' }" :title="widget.name">{{ widget.name }}</h3>
            <span v-else class="flex-1"/>
            <MfButton v-if="widget.expandable && !editable" variant="ghost" size="sm" shape="circle" :aria-label="$t('dashboard.studio.expandWidget')"
                      @click="expanded = widget.id"><Maximize :size="13"/></MfButton>
          </div>
          <p v-if="widget.showTitle && widget.description" class="mb-1 truncate text-xs text-content-tertiary" :title="widget.description">{{ widget.description }}</p>
          <div v-if="widget.reportCode && data.failed.value.has(widget.reportCode)" class="mb-1 shrink-0 text-xs text-warning">{{ $t('queryChart.refreshError') }}</div>
          <div class="min-h-0 min-w-0 flex-1">
            <DashboardContainerView v-if="dashboardContainerTool(widget.kind)" :widget="widget" :widgets="board.widgets" :reports="reports"
                                    :days="days" :wall="wall" :editable="editable" :selected-id="selectedId" :refreshed-for="refreshed" :selection-for="selectionFor"
                                    :failed-codes="data.failed.value" :pending-codes="data.pending.value" @filter="applyFilter" @expand="expanded = $event"/>
            <DashboardWidgetView v-else :widget="widget" :report="assets.get(widget.reportCode)" :snapshot="snapshots.get(widget.reportCode)" :refreshed="refreshed(widget)"
                                 :days="days" :selection="selectionFor(widget)" :wall="wall" :editable="editable" :loading="data.pending.value.has(widget.reportCode)" @filter="applyFilter(widget, $event)"/>
          </div>
        </div>
        <button v-if="editable" type="button" class="widget-overlay absolute inset-0 z-10 cursor-pointer rounded-[inherit] outline-none"
                :class="interactionTarget(widget).movable && 'is-movable'" :data-widget-id="widget.id"
                :aria-label="$t('dashboard.studio.selectWidget', { name: widget.name })"
                :aria-pressed="selectedIds?.includes(widget.id)" @pointerdown.stop="startDrag($event, widget, false)"
                @click.stop="selectWidget($event, widget)" @keydown="onMoveKey($event, widget)"/>
        <button v-if="editable && !wall && widget.resizable && selectedId === widget.id" type="button"
                class="absolute bottom-0 right-0 z-20 grid size-6 cursor-nwse-resize touch-none place-items-center rounded-tl-mf-sm bg-surface/90 text-primary"
                :aria-label="$t('dashboard.studio.resizeWidget')" @pointerdown.stop="startDrag($event, widget, true)"
                @keydown="onMoveKey($event, widget, true)"><MoveDiagonal2 :size="13"/></button>
      </article>
      <div v-if="editable && wallSelection" class="wall-selection pointer-events-none absolute z-[2] outline-2 outline-primary"
           :style="wallSelectionStyle">
        <button v-if="wallSelection.movable" type="button" class="pointer-events-auto absolute top-0 left-0 grid size-6 cursor-grab touch-none place-items-center rounded-br-mf-sm bg-surface text-primary"
                :data-selection-id="wallSelection.id" :aria-label="$t(groupMembers.length ? 'dashboard.studio.moveGroup' : 'dashboard.studio.moveWidget')"
                @pointerdown.stop="startDrag($event, wallSelection, false)" @keydown="onMoveKey($event, wallSelection)"><Move :size="13"/></button>
        <button v-if="wallSelection.resizable" type="button" class="pointer-events-auto absolute right-0 bottom-0 grid size-6 cursor-nwse-resize touch-none place-items-center rounded-tl-mf-sm bg-surface text-primary"
                :aria-label="$t(groupMembers.length ? 'dashboard.studio.resizeGroup' : 'dashboard.studio.resizeWidget')"
                @pointerdown.stop="startDrag($event, wallSelection, true)" @keydown="onMoveKey($event, wallSelection, true)"><MoveDiagonal2 :size="13"/></button>
      </div>
    </div>
    <DashboardScreenEffect v-if="screenAnimationPreview" :widget="screenAnimationPreview" :kind="screenAnimationPreview.kind"
                           :data-animation-widget="screenAnimationPreview.id" class="z-[3]"/>
  </section>

  <Teleport to="body">
    <div v-if="context" ref="contextRoot" class="dashboard-context-menu fixed z-60 flex max-h-[min(360px,calc(100dvh-16px))] w-64 max-w-[calc(100vw-16px)] flex-col rounded-mf-lg border border-line bg-surface p-1.5 shadow-mf-lg"
         :style="{ left: context.x + 'px', top: context.y + 'px' }" @contextmenu.prevent>
      <div class="flex shrink-0 items-center gap-2 px-2.5 pt-1 pb-2 text-xs text-content-tertiary">
        <Layers :size="13" aria-hidden="true"/>
        <span class="flex-1">{{ $t('dashboard.studio.contextLayers') }}</span>
        <span class="tabular-nums">{{ contextChoices.length }}</span>
      </div>
      <div class="min-h-0 overflow-y-auto">
        <MfMenu :selected-keys="selectedIds">
          <MfMenuItem v-for="choice in contextChoices" :key="choice.id" :key_="choice.id" @click="chooseLayer(choice.id)">
            <template #icon>
              <QueryChartTypeIcon v-if="choice.type" :type="choice.type" class="shrink-0"/>
              <component :is="choice.icon" v-else :size="16" class="shrink-0" aria-hidden="true"/>
            </template>
            <MfButton class="context-menu-action" variant="ghost" size="sm" :title="choice.title"
                      :aria-label="choice.group ? $t('dashboard.studio.selectGroup') : $t('dashboard.studio.selectWidget', { name: choice.name })">
              <span class="min-w-0 flex-1 truncate text-left">{{ choice.name }}</span>
              <Check v-if="selectedIds?.includes(choice.id)" :size="14" class="shrink-0" aria-hidden="true"/>
            </MfButton>
          </MfMenuItem>
        </MfMenu>
      </div>
      <MfDivider v-if="context.group || context.groupIds.length > 1" class="my-1.5 shrink-0"/>
      <MfMenu v-if="context.group || context.groupIds.length > 1" class="shrink-0">
        <MfMenuItem v-if="context.group" @click="emit('ungroup', context.group); closeContext()">
          <template #icon><Ungroup :size="16" class="shrink-0" aria-hidden="true"/></template>
          <MfButton class="context-menu-action" variant="ghost" size="sm">{{ $t('dashboard.studio.ungroup') }}</MfButton>
        </MfMenuItem>
        <MfMenuItem v-if="context.groupIds.length > 1" @click="emit('group', context.groupIds); closeContext()">
          <template #icon><Group :size="16" class="shrink-0" aria-hidden="true"/></template>
          <MfButton class="context-menu-action" variant="ghost" size="sm">{{ $t('dashboard.studio.groupSelection') }}</MfButton>
        </MfMenuItem>
      </MfMenu>
      <MfDivider v-if="context.target" class="my-1.5 shrink-0"/>
      <MfMenu v-if="context.target" class="shrink-0">
        <MfMenuItem danger @click="deleteContext">
          <template #icon><Trash2 :size="16" class="shrink-0" aria-hidden="true"/></template>
          <MfButton class="context-menu-action" variant="ghost" size="sm" :title="context.targetName">
            {{ $t(context.group ? 'dashboard.studio.deleteGroup' : 'dashboard.studio.deleteWidget') }}
          </MfButton>
        </MfMenuItem>
      </MfMenu>
    </div>
  </Teleport>

  <MfModal v-if="expandedWidget" :open="true" :title="expandedWidget.name" width="min(1040px, 95vw)" height="min(720px, 90dvh)"
           trap-focus @close="expanded = ''">
    <DashboardContainerView v-if="dashboardContainerTool(expandedWidget.kind)" class="h-full" :widget="expandedWidget" :widgets="board.widgets" :reports="reports"
                            :days="days" :wall="wall" :refreshed-for="refreshed" :selection-for="selectionFor" :failed-codes="data.failed.value" :pending-codes="data.pending.value" @filter="applyFilter" @expand="expanded = $event"/>
    <DashboardWidgetView v-else class="h-full" :widget="expandedWidget" :report="assets.get(expandedWidget.reportCode)" :snapshot="snapshots.get(expandedWidget.reportCode)"
                         :refreshed="refreshed(expandedWidget)" :days="days" :selection="selectionFor(expandedWidget)" :loading="data.pending.value.has(expandedWidget.reportCode)"
                         @filter="applyFilter(expandedWidget, $event)"/>
  </MfModal>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/date'
import { computed, nextTick, onBeforeUnmount, provide, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Box, ChartNoAxesCombined, Check, Group, Layers, Maximize, Move, MoveDiagonal2, RefreshCw, Sparkles, Trash2, Ungroup } from '@lucide/vue'
import { useDismissableLayer } from 'metaforge-ui'
import type { ReportModel } from '@/model/report'
import { chartFieldKind, type QueryChartRow } from '@/model/query-chart'
import { constrainDashboardGroup, constrainDashboardRect, dashboardCanvasColor, dashboardCardStyle, dashboardChartType, dashboardColumns, dashboardContainerTool, dashboardFilterTool, dashboardGridTarget, dashboardGroupBounds,
  dashboardLayerMembers, dashboardMotionStyle, dashboardScreenEffectSettings, dashboardScreenEffectTool, dashboardSnapshot, resizeDashboardGroup,
  type DashboardDesign, type DashboardRect, type DashboardScreenEffectKind, type DashboardWidget } from '@/model/dashboard-layout'
import { useDashboardReports } from '@/composables/useDashboardReports'
import { dashboardFiltersContext } from '@/composables/dashboardFiltersContext'
import { dashboardFilteredReports, dashboardFilterHasValue, dashboardFilterMatchesWidget, type DashboardFilter } from '@/model/dashboard-filters'
import DashboardWidgetView from './DashboardWidgetView.vue'
import DashboardContainerView from './DashboardContainerView.vue'
import DashboardCanvasBackground from './DashboardCanvasBackground.vue'
import DashboardScreenEffect from './DashboardScreenEffect.vue'
import QueryChartTypeIcon from '@/views/components/visual/query/QueryChartTypeIcon.vue'
import './dashboard-motion.css'

const props = defineProps<{ design: DashboardDesign; reports: ReportModel[]; editable?: boolean; selectedId?: string; selectedIds?: string[]; wallFullscreen?: boolean; transparent?: boolean; thumbnail?: boolean }>()
const emit = defineEmits<{
  select: [id: string, additive?: boolean]; place: [id: string, rect: DashboardRect]; add: [value: string, position?: Pick<DashboardRect, 'x' | 'y'>, parentId?: string];
  group: [ids: string[]]; ungroup: [id: string]; remove: [id: string]
}>()
const { t } = useI18n()
const frame = ref<HTMLElement>()
const surface = ref<HTMLElement>()
const screenAnimationPreview = shallowRef<DashboardWidget & { kind: DashboardScreenEffectKind }>()
const expanded = ref('')
const days = ref(0)
const selection = shallowRef<{ reportCode: string; widgetId: string; rows: Set<QueryChartRow> }>()
const wall = computed(() => props.design.mode === 'wall')
const board = computed(() => props.design.boards[props.design.mode])
const screenEffects = computed(() => board.value.widgets.filter(widget => dashboardScreenEffectTool(widget.kind)))
const regularWidgets = computed(() => board.value.widgets.filter(widget => !widget.parentId && !dashboardScreenEffectTool(widget.kind)))
type DragTarget = DashboardRect & { id: string; movable: boolean; resizable: boolean }
const groupMembers = computed(() => wall.value && props.selectedId ? board.value.widgets.filter(widget => widget.groupId === props.selectedId) : [])
const wallSelection = computed<DragTarget | undefined>(() => {
  if (!wall.value) return
  const members = groupMembers.value
  return members.length ? { ...dashboardGroupBounds(members), id: props.selectedId!, movable: members.every(widget => widget.movable),
    resizable: members.every(widget => widget.resizable) } : regularWidgets.value.find(widget => widget.id === props.selectedId)
})
const reports = computed(() => props.reports)
const live = computed(() => !props.editable && !props.thumbnail)
const assets = computed(() => new Map(props.reports.map(report => [report.code, report])))
const snapshots = computed(() => new Map(props.reports.map(report => [report.code, dashboardSnapshot(report)])))
const data = useDashboardReports(board, reports, live)
const widgetFilters = shallowRef(new Map<string, DashboardFilter>())
const boundFilters = computed(() => {
  if (!live.value || !widgetFilters.value.size) return []
  return board.value.widgets.flatMap(widget => {
    const filter = widgetFilters.value.get(widget.id)
    return filter && dashboardFilterMatchesWidget(filter, widget) ? [filter] : []
  })
})
const filteredReports = computed(() => dashboardFilteredReports(boundFilters.value, code => data.data.value[code] ?? snapshots.value.get(code)?.columns ?? []))
const combinedSelection = computed(() => {
  const linked = selection.value
  if (!linked) return
  const filtered = filteredReports.value.get(linked.reportCode)
  return filtered ? new Set([...linked.rows].filter(row => filtered.has(row))) : linked.rows
})
provide(dashboardFiltersContext, {
  read(widget) {
    const filter = widgetFilters.value.get(widget.id)
    return filter && dashboardFilterMatchesWidget(filter, widget) ? filter : undefined
  },
  write(widget, filter) {
    if (!live.value || !widget.reportCode || !widget.field || widget.kind !== filter.kind) return
    const active = dashboardFilterHasValue(filter)
    if (!active && !widgetFilters.value.has(widget.id)) return
    const next = new Map(widgetFilters.value)
    if (active) next.set(widget.id, { ...filter, reportCode: widget.reportCode, field: widget.field })
    else next.delete(widget.id)
    widgetFilters.value = next
  },
  active: code => filteredReports.value.has(code) || selection.value?.reportCode === code,
  clear(code) {
    widgetFilters.value = new Map([...widgetFilters.value].filter(([, filter]) => filter.reportCode !== code))
    if (selection.value?.reportCode === code) selection.value = undefined
  },
})
const refreshTime = computed(() => data.refreshedAt.value ? formatDateTime(data.refreshedAt.value) : '')
const expandedWidget = computed(() => board.value.widgets.find(widget => widget.id === expanded.value))
const hasDate = computed(() => board.value.widgets.some(widget => {
  const snapshot = snapshots.value.get(widget.reportCode)
  return snapshot?.headers.some(field => chartFieldKind(snapshot.columns, field) === 'date')
}))
const periods = computed(() => [0, 7, 30, 90].map(value => ({
  value, label: value ? t('dashboard.studio.recentDays', { days: value }) : t('dashboard.studio.allDates')
})))

const frameStyle = computed(() => {
  const layout = board.value
  const backgroundColor = dashboardCanvasColor(layout.background) || undefined
  if (!wall.value) return {
    backgroundColor, '--canvas-padding': layout.padding + 'px', '--canvas-gap': layout.gap + 'px', '--canvas-row-height': layout.rowHeight + 'px',
  }
  const appearance = {
    backgroundColor,
    '--canvas-grid-size': `${layout.snapStep / layout.width * 100}% ${layout.snapStep / layout.height * 100}%`,
  }
  if (layout.resolutionMode === 'auto') return { ...appearance, width: '100%', height: '100%', borderRadius: '0' }
  const ratio = layout.width / layout.height
  const fit = layout.fit === 'cover' ? 'max' : 'min'
  return {
    ...appearance,
    width: props.wallFullscreen ? `${fit}(100cqw, calc(100cqh * ${ratio}))` : undefined,
    aspectRatio: ratio,
  }
})
watch(() => props.design.mode, () => { selection.value = undefined; widgetFilters.value = new Map(); days.value = 0; expanded.value = ''; closeContext() })
watch(data.data, () => { selection.value = undefined })
function refreshed(widget: DashboardWidget) { return widget.reportCode ? data.data.value[widget.reportCode] : undefined }
function selectPeriod(value: string | number | undefined) {
  days.value = Number(value) || 0
  selection.value = undefined
}
function applyFilter(widget: DashboardWidget, rows: QueryChartRow[]) {
  if (props.editable || !widget.reportCode) return
  selection.value = { reportCode: widget.reportCode, widgetId: widget.id, rows: new Set(rows) }
  expanded.value = ''
}
function selectionFor(widget: DashboardWidget) {
  if (!widget.reportCode || dashboardFilterTool(widget.kind)) return
  if (selection.value?.reportCode === widget.reportCode && selection.value.widgetId !== widget.id) return combinedSelection.value
  return filteredReports.value.get(widget.reportCode)
}
function refreshBoard() {
  selection.value = undefined
  void data.refresh()
}

const drag = shallowRef<{ id: string; rect: DashboardRect; valid: boolean }>()
const groupPreview = computed(() => {
  if (!drag.value) return
  const members = board.value.widgets.filter(widget => widget.groupId === drag.value?.id)
  return members.length ? resizeDashboardGroup(members, drag.value.rect) : undefined
})
const wallSelectionStyle = computed(() => {
  const target = wallSelection.value
  if (!target) return
  const widget = board.value.widgets.find(item => item.id === target.id)
  return { ...wallRect(drag.value?.id === target.id ? drag.value.rect : target),
    borderRadius: widget?.showCard ? widget.corners.map(value => value + 'px').join(' ') : '0' }
})
let stopPointer: (() => void) | undefined
let animationRequest = 0
let clearAnimationPreview: (() => void) | undefined
function stopAnimationPreview() {
  animationRequest++
  clearAnimationPreview?.()
  clearAnimationPreview = undefined
  screenAnimationPreview.value = undefined
}
watch([() => props.selectedId, () => props.design.mode, () => props.editable], stopAnimationPreview)
onBeforeUnmount(() => {
  stopPointer?.()
  stopAnimationPreview()
})
function wallRect(rect: DashboardRect) {
  return { left: rect.x / board.value.width * 100 + '%', top: rect.y / board.value.height * 100 + '%',
    width: rect.w / board.value.width * 100 + '%', height: rect.h / board.value.height * 100 + '%' }
}
function widgetStyle(widget: DashboardWidget) {
  const rect = drag.value?.id === widget.id && drag.value.valid ? drag.value.rect : groupPreview.value?.get(widget.id) ?? widget
  const appearance = dashboardCardStyle(widget)
  const motion = props.editable ? {} : dashboardMotionStyle(widget)
  if (wall.value) return { ...appearance, ...motion, ...wallRect(rect) }
  return { ...appearance, ...motion, gridColumn: (rect.x + 1) + ' / span ' + rect.w, gridRow: (rect.y + 1) + ' / span ' + rect.h }
}
function accept(widget: DragTarget, proposed: DashboardRect, relocate = false) {
  const members = wall.value ? board.value.widgets.filter(member => member.groupId === widget.id) : []
  const rect = members.length ? constrainDashboardGroup(proposed, members, board.value) : wall.value
    ? constrainDashboardRect(proposed, 'wall', board.value) : dashboardGridTarget(board.value, proposed, widget.id, relocate)
  return { rect: rect ?? widget, valid: !!rect }
}
function interactionTarget(widget: DragTarget): DragTarget {
  const groupId = wall.value ? board.value.widgets.find(member => member.id === widget.id)?.groupId : undefined
  if (!groupId) return widget
  const members = board.value.widgets.filter(member => member.groupId === groupId)
  return { ...dashboardGroupBounds(members), id: groupId, movable: members.every(member => member.movable),
    resizable: members.every(member => member.resizable) }
}
function startDrag(event: PointerEvent, widget: DragTarget, resize: boolean) {
  if (event.button !== 0 || !props.editable) return
  if (wall.value && (event.shiftKey || event.ctrlKey || event.metaKey || event.altKey)) return
  stopAnimationPreview()
  widget = interactionTarget(widget)
  if (event.currentTarget instanceof HTMLElement) event.currentTarget.focus({ preventScroll: true })
  emit('select', widget.id)
  const element = surface.value
  if (!element || (resize ? !widget.resizable : !widget.movable)) return
  if (!wall.value && getComputedStyle(element).display !== 'grid') return
  const bounds = element.getBoundingClientRect()
  if (!bounds.width || !bounds.height) return
  event.preventDefault()
  stopPointer?.()
  const start = { x: event.clientX, y: event.clientY }
  const layout = board.value
  const column = wall.value ? bounds.width / layout.width : (bounds.width + layout.gap) / dashboardColumns
  const row = wall.value ? bounds.height / layout.height : layout.rowHeight + layout.gap
  const snap = wall.value && layout.snap ? layout.snapStep : 1
  const offset = (edge: number, delta: number) => delta && wall.value ? Math.round((edge + delta) / snap) * snap - edge : delta
  function move(pointer: PointerEvent) {
    if (pointer.pointerId !== event.pointerId) return
    const dx = offset(resize ? widget.x + widget.w : widget.x, (pointer.clientX - start.x) / column)
    const dy = offset(resize ? widget.y + widget.h : widget.y, (pointer.clientY - start.y) / row)
    const result = accept(widget, resize ? { ...widget, w: widget.w + dx, h: widget.h + dy } : { ...widget, x: widget.x + dx, y: widget.y + dy }, !resize)
    drag.value = { id: widget.id, ...result }
  }
  function end(pointer: PointerEvent) {
    if (pointer.pointerId !== event.pointerId) return
    const current = drag.value
    if (pointer.type !== 'pointercancel' && current?.valid) emit('place', widget.id, current.rect)
    stopPointer?.()
  }
  stopPointer = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', end)
    window.removeEventListener('pointercancel', end)
    drag.value = undefined
    stopPointer = undefined
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', end)
  window.addEventListener('pointercancel', end)
}
function onMoveKey(event: KeyboardEvent, widget: DragTarget, resize = false) {
  const x = event.key === 'ArrowLeft' ? -1 : event.key === 'ArrowRight' ? 1 : 0
  const y = event.key === 'ArrowUp' ? -1 : event.key === 'ArrowDown' ? 1 : 0
  if (!x && !y) return
  widget = interactionTarget(widget)
  if (resize ? !widget.resizable : !widget.movable) return
  event.preventDefault()
  emit('select', widget.id)
  const step = wall.value && board.value.snap ? board.value.snapStep : 1
  const offset = (edge: number, direction: number) => !direction ? 0
    : (direction > 0 ? Math.floor(edge / step) + 1 : Math.ceil(edge / step) - 1) * step - edge
  const dx = offset(resize ? widget.x + widget.w : widget.x, x)
  const dy = offset(resize ? widget.y + widget.h : widget.y, y)
  const result = accept(widget, resize ? { ...widget, w: widget.w + dx, h: widget.h + dy } : { ...widget, x: widget.x + dx, y: widget.y + dy })
  if (result.valid) emit('place', widget.id, result.rect)
}
const dragType = 'application/x-metaforge-dashboard'
function onDragOver(event: DragEvent) {
  if (props.editable && event.dataTransfer?.types.includes(dragType)) event.preventDefault()
}
function onDrop(event: DragEvent) {
  const item = event.dataTransfer?.getData(dragType)
  const bounds = surface.value?.getBoundingClientRect()
  if (!props.editable || !item || !bounds?.width || !bounds.height) return
  event.preventDefault()
  const layout = board.value
  const x = (event.clientX - bounds.left) / (wall.value ? bounds.width / layout.width : (bounds.width + layout.gap) / dashboardColumns)
  const y = (event.clientY - bounds.top) / (wall.value ? bounds.height / layout.height : layout.rowHeight + layout.gap)
  const snap = layout.snap ? layout.snapStep : 1
  const container = [...regularWidgets.value].reverse().find(widget => dashboardContainerTool(widget.kind)
    && x >= widget.x && x < widget.x + widget.w && y >= widget.y && y < widget.y + widget.h)
  emit('add', item, wall.value ? { x: Math.round(x / snap) * snap, y: Math.round(y / snap) * snap } : { x, y }, container?.id)
}

function focusWidget(id: string) {
  const key = CSS.escape(id)
  surface.value?.parentElement?.querySelector<HTMLElement>(`[data-widget-id="${key}"], [data-selection-id="${key}"], [data-contained-widget="${key}"]`)?.focus({ preventScroll: true })
}

async function playAnimation(id: string) {
  stopAnimationPreview()
  const request = animationRequest
  const widget = board.value.widgets.find(item => item.id === id)
  if (!props.editable || !widget?.animation || widget.animation.effect === 'none'
    || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const style = dashboardMotionStyle(widget)
  const kind = widget.kind
  if (dashboardScreenEffectTool(kind)) {
    screenAnimationPreview.value = { ...widget, kind, animation: undefined,
      screenEffect: { ...dashboardScreenEffectSettings(kind, widget.screenEffect), enabled: true } }
  }

  await nextTick()
  if (request !== animationRequest) return
  const target = frame.value?.querySelector<HTMLElement>(`[data-animation-widget="${CSS.escape(id)}"]`)
  if (!target?.getClientRects().length) {
    stopAnimationPreview()
    return
  }
  if (!screenAnimationPreview.value) target.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  const names = Object.keys(style)
  for (const [name, value] of Object.entries(style)) target.style.setProperty(name, value)
  // Resolve the removed class before adding it again so the same preset restarts.
  void getComputedStyle(target).animationName
  target.classList.add('dashboard-motion-preview')
  clearAnimationPreview = () => {
    target.classList.remove('dashboard-motion-preview')
    for (const name of names) target.style.removeProperty(name)
  }
  const playback = target.getAnimations().find(item => 'animationName' in item && item.animationName === style['--widget-motion-name'])
  try { await playback?.finished }
  catch { /* A change of selection or motion preference can cancel playback. */ }
  finally { if (request === animationRequest) stopAnimationPreview() }
}

function hitWidgets(event: MouseEvent) {
  const bounds = surface.value?.getBoundingClientRect()
  if (!bounds?.width || !bounds.height) return []
  const x = (event.clientX - bounds.left) * board.value.width / bounds.width
  const y = (event.clientY - bounds.top) * board.value.height / bounds.height
  return [...regularWidgets.value].reverse().filter(widget => x >= widget.x && x < widget.x + widget.w && y >= widget.y && y < widget.y + widget.h)
}
function selectWidget(event: MouseEvent, widget: DashboardWidget) {
  if (wall.value && event.altKey) {
    const hits = [...new Set(hitWidgets(event).map(item => interactionTarget(item).id))]
    const next = hits[(hits.indexOf(props.selectedId ?? '') + 1) % hits.length]
    emit('select', next ?? interactionTarget(widget).id)
    return
  }
  const additive = wall.value && (event.shiftKey || event.ctrlKey || event.metaKey)
  emit('select', additive ? widget.id : interactionTarget(widget).id, additive)
}
const context = ref<{ x: number; y: number; widgets: DashboardWidget[]; group?: string; groupIds: string[]; target?: string; targetName?: string }>()
const contextRoot = ref<HTMLElement>()
const contextChoices = computed(() => {
  const choices = new Map<string, { id: string; name: string; title: string; group: boolean; icon: typeof Box; type?: ReturnType<typeof dashboardChartType> }>()
  for (const widget of context.value?.widgets ?? []) {
    const id = interactionTarget(widget).id
    if (choices.has(id)) continue
    const group = id !== widget.id
    const members = group ? dashboardLayerMembers(board.value.widgets, id) : []
    choices.set(id, { id, group, name: group ? t('dashboard.studio.layerGroup', { count: members.length }) : widget.name,
      title: group ? members.map(member => member.name).join(' · ') : widget.name,
      icon: group ? Group : widget.kind === 'report' ? ChartNoAxesCombined : Box,
      type: group ? undefined : dashboardChartType(widget.kind) })
  }
  return [...choices.values()]
})
function closeContext() { context.value = undefined }
useDismissableLayer(contextRoot, computed(() => !!context.value), closeContext)
function openContext(event: MouseEvent, id?: string) {
  if (!props.editable || (!wall.value && !id)) return
  const widgets = id ? dashboardLayerMembers(board.value.widgets, id).reverse() : hitWidgets(event)
  const selected = board.value.widgets.filter(widget => props.selectedIds?.includes(widget.id) && !widget.groupId)
  const groupIds = (selected.length > 1 ? selected : widgets.filter(widget => !widget.groupId)).map(widget => widget.id)
  const selectedTarget = props.selectedId && widgets.some(widget => interactionTarget(widget).id === props.selectedId) ? props.selectedId : undefined
  const target = selectedTarget ?? (widgets[0] ? interactionTarget(widgets[0]).id : undefined)
  const members = target ? board.value.widgets.filter(widget => widget.groupId === target) : []
  const group = members.length ? target : undefined
  const targetName = members.length ? t('dashboard.studio.layerGroup', { count: members.length }) : widgets.find(widget => widget.id === target)?.name
  if (!widgets.length && groupIds.length < 2 && !group) return
  event.preventDefault()
  context.value = { widgets, group, groupIds, target, targetName, x: event.clientX, y: event.clientY }
  const menu = context.value
  void nextTick(() => {
    if (!contextRoot.value || context.value !== menu) return
    const bounds = contextRoot.value.getBoundingClientRect()
    context.value.x = Math.max(8, Math.min(event.clientX, window.innerWidth - bounds.width - 8))
    context.value.y = Math.max(8, Math.min(event.clientY, window.innerHeight - bounds.height - 8))
    contextRoot.value.querySelector('button')?.focus({ preventScroll: true })
  })
}
function chooseLayer(id?: string) {
  if (id) {
    emit('select', id)
    void nextTick(() => focusWidget(id))
  }
  closeContext()
}
function deleteContext() {
  if (context.value?.target) emit('remove', context.value.target)
  closeContext()
}

defineExpose({ refresh: refreshBoard, focusWidget, openContext, playAnimation })
</script>

<style scoped>
.dashboard-context-menu .context-menu-action {
  width: 100%; height: auto; min-width: 0; min-height: 20px; justify-content: flex-start;
  padding: 0; border-radius: 0; color: inherit; background: transparent; box-shadow: none;
}
.dashboard-context-menu .context-menu-action:hover { color: inherit; background: transparent; }
.dashboard-frame { container-type: inline-size; }
.dashboard-paper { display: flex; flex: 1; flex-direction: column; background-color: var(--mf-color-surface); padding: var(--canvas-padding); }
.dashboard-paper.show-grid .paper-surface { background-image: radial-gradient(var(--mf-color-line) .8px, transparent .8px); background-size: 15px 15px; }
.paper-surface { display: grid; grid-template-columns: repeat(12, minmax(0, 1fr)); grid-auto-rows: var(--canvas-row-height); gap: var(--canvas-gap); }
.board-empty { grid-column: 1 / -1; grid-row: span 6; }
.is-editing .paper-surface { position: relative; flex: 1; min-height: 180px; align-content: start; }
.is-editing .paper-surface .board-empty { position: absolute; inset: 0; min-height: 0; }
.board-widget { position: relative; }
.widget-card { background-color: var(--mf-color-surface); box-shadow: 0 2px 8px color-mix(in srgb, var(--mf-color-line) 48%, transparent); }
.widget-border { border: 1px solid var(--mf-color-line); }
.board-widget:not(.is-selected):not(.is-framed) .widget-overlay:focus-visible { box-shadow: inset 0 0 0 2px var(--mf-color-primary); }
.widget-overlay.is-movable { cursor: grab; touch-action: none; }
.widget-overlay.is-movable:active { cursor: grabbing; }
.board-widget.is-selected { outline: 2px solid var(--mf-color-primary); outline-offset: 1px; z-index: 1; }
.wall-frame {
  --mf-color-surface: #092334;
  --mf-color-surface-hover: #103448;
  --mf-color-content: #e3fbff;
  --mf-color-content-secondary: #b6edf5;
  --mf-color-content-tertiary: #91bfcd;
  --mf-color-line: #19556a;
  --mf-color-primary: #15e5d1;
  color: var(--mf-color-content); background: #061728;
  border: 1px solid var(--mf-color-line); border-radius: 5px;
  display: flex; flex: none; flex-direction: column; padding: 1.4%; min-height: 0; overflow: hidden;
}
.wall-jade {
  --mf-color-surface: #0b292a; --mf-color-surface-hover: #123a3a; --mf-color-line: #23625c; --mf-color-primary: #45ecd0;
  background: #071d1f;
}
.wall-frame.show-grid .wall-surface { background-image: linear-gradient(90deg, #76eff41a 1px, transparent 1px), linear-gradient(#76eff41a 1px, transparent 1px); background-size: var(--canvas-grid-size); }
.wall-heading { position: relative; display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 5%; margin-bottom: 1.25%; min-height: 8%; color: #c6ffff; }
.wall-heading span { height: 1px; background: linear-gradient(90deg, transparent, var(--mf-color-primary)); }
.wall-heading span:last-child { transform: rotate(180deg); }
.wall-timestamp { position: absolute; top: 75%; right: 0; color: var(--mf-color-content-tertiary); font-size: clamp(8px, 1cqw, 14px); }
.wall-heading h2 { font-size: clamp(13px, 2cqw, 36px); letter-spacing: .18em; text-shadow: 0 0 16px #00a8f7; }
.wall-surface { position: relative; min-height: 0; flex: 1; }
.wall-surface .board-empty { height: 100%; }
.wall-widget { position: absolute; z-index: 0; box-shadow: none; color: var(--mf-color-content); }
.wall-widget.is-marked { outline: 1px dashed var(--mf-color-primary); }
@container (max-width: 610px) {
  .paper-surface { display: flex; flex-direction: column; }
  .paper-surface .board-widget { min-height: 180px; height: 280px; }
  .paper-surface .widget-overlay.is-movable { cursor: pointer; touch-action: pan-y; }
  .paper-surface .board-empty { min-height: 280px; }
}
</style>
