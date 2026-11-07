<template>
  <MfEmpty v-if="!children.length" class="h-full" size="sm" :title="$t('dashboard.studio.containerEmpty')" :description="editable ? $t('dashboard.studio.containerEmptyHint') : undefined"/>
  <MfTabs v-else-if="widget.kind === 'tabsContainer'" v-model:active-key="activeTab" fill class="h-full min-w-0">
    <MfTabPane v-for="child in children" :key="child.id" :item-key="child.id" :tab="child.name">
      <div class="h-full min-h-0" :style="{ padding: layout.padding + 'px' }"><RenderChild :child="child"/></div>
    </MfTabPane>
  </MfTabs>
  <div v-else-if="widget.kind === 'accordionContainer'" class="flex h-full min-h-0 min-w-0 flex-col overflow-auto"
       :style="{ padding: layout.padding + 'px', gap: layout.gap + 'px' }">
    <section v-for="(child, index) in children" :key="child.id" class="min-w-0 shrink-0 rounded-mf-md border border-line">
      <MfButton :id="accordionId + '-button-' + index" class="w-full justify-between rounded-mf-sm" variant="ghost"
                :aria-controls="accordionId + '-panel-' + index" :aria-expanded="activeAccordion === child.id"
                @click="openAccordion(activeAccordion === child.id ? null : child.id)">
        <span class="min-w-0 flex-1 truncate text-left" :title="child.name">{{ child.name }}</span>
        <ChevronDown :size="15" class="shrink-0 transition-transform motion-reduce:transition-none" :class="activeAccordion === child.id && 'rotate-180'"/>
      </MfButton>
      <div v-show="activeAccordion === child.id" :id="accordionId + '-panel-' + index" :aria-labelledby="accordionId + '-button-' + index"
           role="region" class="min-w-0 border-t border-line" :style="{ height: layout.itemHeight + 'px' }">
        <RenderChild v-if="visitedAccordion.has(child.id)" :child="child" hide-title/>
      </div>
    </section>
  </div>
  <div v-else-if="widget.kind === 'carouselContainer'" class="flex h-full min-h-0 flex-col" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <div class="relative min-h-0 flex-1 overflow-hidden">
      <div v-for="(child,index) in children" v-show="index === carouselIndex" :key="child.id" class="absolute inset-0 min-h-0" :style="{ padding: layout.padding + 'px' }"><RenderChild :child="child"/></div>
    </div>
    <div class="mt-2 flex shrink-0 items-center justify-center gap-2 text-xs">
      <MfButton size="sm" variant="ghost" :disabled="editable || children.length < 2" :aria-label="$t('dashboard.studio.previousWidget')" @click="advance(-1)"><ChevronLeft :size="14"/></MfButton>
      <span>{{ carouselIndex + 1 }} / {{ children.length }}</span>
      <MfButton size="sm" variant="ghost" :disabled="editable || children.length < 2" :aria-label="$t('dashboard.studio.nextWidget')" @click="advance(1)"><ChevronRight :size="14"/></MfButton>
    </div>
  </div>
  <div v-else-if="widget.kind === 'spotlightContainer'" class="spotlight-wrapper h-full min-h-0 min-w-0 overflow-auto" :tabindex="editable ? undefined : 0"
       role="region" :aria-label="widget.name">
    <div class="spotlight-grid grid" :style="{ '--spotlight-columns': Math.max(3, Math.min(4, layout.columns)), '--spotlight-row': layout.itemHeight + 'px', gap: layout.gap + 'px', padding: layout.padding + 'px' }">
      <div v-for="child in children" :key="child.id" class="min-h-0 min-w-0"><RenderChild :child="child"/></div>
    </div>
  </div>
  <div v-else class="container-layout h-full min-h-0 min-w-0 overflow-auto" :class="widget.kind === 'scrollContainer' && 'scroll-viewport'" :tabindex="widget.kind === 'scrollContainer' && !editable ? 0 : undefined" :role="widget.kind === 'scrollContainer' ? 'region' : undefined" :aria-label="widget.kind === 'scrollContainer' ? widget.name : undefined" :style="layoutStyle">
    <div v-for="(child, index) in children" :key="child.id" class="min-h-0 min-w-0"
         :style="widget.kind === 'stackContainer' ? { gridArea: '1 / 1' } : widget.kind === 'headerContainer' && index === 0 || widget.kind === 'footerContainer' && index === children.length - 1 ? { gridColumn: '1 / -1' } : widget.kind === 'sidebarContainer' && index === 0 ? { gridRow: '1 / -1' } : undefined">
      <RenderChild :child="child"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, getCurrentInstance, h, onScopeDispose, ref, useId, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfButton } from 'metaforge-ui'
import { ChevronDown, ChevronLeft, ChevronRight, Maximize } from '@lucide/vue'
import { dashboardCardStyle, dashboardContentSettings, dashboardContainerTool, dashboardDescendantIds, dashboardLayoutSettings, dashboardMotionStyle, dashboardSnapshot,
  type DashboardWidget } from '@/model/dashboard-layout'
import type { QueryChartRow } from '@/model/query-chart'
import type { ReportModel } from '@/model/report'
import DashboardWidgetView from './DashboardWidgetView.vue'

const props = defineProps<{ widget: DashboardWidget; widgets: DashboardWidget[]; reports: ReportModel[]; days: number; wall?: boolean;
  editable?: boolean; selectedId?: string; refreshedFor: (widget: DashboardWidget) => QueryChartRow[] | undefined;
  selectionFor: (widget: DashboardWidget) => ReadonlySet<QueryChartRow> | undefined; failedCodes: ReadonlySet<string>; pendingCodes?: ReadonlySet<string> }>()
const emit = defineEmits<{ filter: [widget: DashboardWidget, rows: QueryChartRow[]]; expand: [id: string] }>()
const { t } = useI18n()
const ContainerComponent = getCurrentInstance()!.type
const children = computed(() => props.widgets.filter(widget => widget.parentId === props.widget.id))
const layout = computed(() => dashboardLayoutSettings(props.widget.layoutOptions))
const activeTab = ref('')
const accordionId = useId()
const activeAccordion = ref<string | null>(null)
const visitedAccordion = ref(new Set<string>())
function openAccordion(id: string | null) {
  activeAccordion.value = id
  if (id) visitedAccordion.value.add(id)
}
watch([() => props.widget.kind, children], ([kind, items], [previousKind, previous]) => {
  if (kind !== 'accordionContainer') return
  if (previousKind !== kind || !previous?.length || activeAccordion.value !== null && !items.some(item => item.id === activeAccordion.value))
    openAccordion(items[0]?.id ?? null)
  for (const id of visitedAccordion.value) if (!items.some(item => item.id === id)) visitedAccordion.value.delete(id)
}, { immediate: true })
const carouselIndex = ref(0), hovered = ref(false)
const playback = computed(() => dashboardContentSettings(props.widget.contentOptions))
function advance(delta: number) { if (children.value.length) carouselIndex.value = (carouselIndex.value + delta + children.value.length) % children.value.length }
watch(children, items => { if (carouselIndex.value >= items.length) carouselIndex.value = 0 })
let timer: ReturnType<typeof setInterval> | undefined
watch([() => props.widget.kind, () => props.editable, () => playback.value.interval, () => playback.value.autoplay, () => children.value.length], () => {
  clearInterval(timer)
  if (props.widget.kind !== 'carouselContainer' || props.editable || !playback.value.autoplay || children.value.length < 2) return
  timer = setInterval(() => { if (!document.hidden && !(hovered.value && playback.value.pauseOnHover) && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) advance(1) }, playback.value.interval * 1000)
}, { immediate: true })
onScopeDispose(() => clearInterval(timer))
watch(children, items => { if (!items.some(item => item.id === activeTab.value)) activeTab.value = items[0]?.id ?? '' }, { immediate: true })
watch(() => props.selectedId, id => {
  const branch = children.value.find(child => id && dashboardDescendantIds(props.widgets, [child.id]).has(id))
  if (branch) {
    activeTab.value = branch.id
    carouselIndex.value = children.value.indexOf(branch)
    if (props.widget.kind === 'accordionContainer') openAccordion(branch.id)
  }
})
const reports = computed(() => new Map(props.reports.map(report => [report.code, report])))
const layoutStyle = computed(() => {
  const count = Math.max(1, children.value.length)
  const columns = props.widget.kind === 'rowContainer' ? `repeat(${count}, minmax(0, 1fr))`
    : props.widget.kind === 'columnContainer' || props.widget.kind === 'stackContainer' || props.widget.kind === 'scrollContainer' ? 'minmax(0, 1fr)'
    : props.widget.kind === 'splitContainer' || props.widget.kind === 'sidebarContainer' ? `${layout.value.ratio}% minmax(0, 1fr)`
    : props.widget.kind === 'headerContainer' || props.widget.kind === 'footerContainer' ? `repeat(${Math.max(1, count - 1)}, minmax(0, 1fr))`
    : props.widget.kind === 'responsiveContainer' ? `repeat(auto-fit, minmax(min(${layout.value.minWidth}px, 100%), 1fr))`
    : `repeat(${layout.value.columns}, minmax(0, 1fr))`
  return { display: 'grid', gridTemplateColumns: columns, gap: layout.value.gap + 'px', padding: layout.value.padding + 'px',
    gridTemplateRows: props.widget.kind === 'headerContainer' ? `${layout.value.header}% minmax(0, 1fr)` : props.widget.kind === 'footerContainer' && count > 1 ? `minmax(0, 1fr) ${layout.value.header}%` : props.widget.kind === 'sidebarContainer' ? `repeat(${Math.max(1,count-1)},minmax(0,1fr))` : undefined,
    gridAutoRows: props.widget.kind === 'scrollContainer' ? layout.value.itemHeight + 'px' : 'minmax(0, 1fr)' }
})
const RenderChild = defineComponent({
  props: { child: { type: Object, required: true }, hideTitle: Boolean },
  setup: (childProps: { child: DashboardWidget; hideTitle?: boolean }) => () => {
    const child = childProps.child
    const report = reports.value.get(child.reportCode)
    const showTitle = child.showTitle && !childProps.hideTitle
    const heading = showTitle || (!props.editable && child.expandable) ? h('div', { class: 'mb-1 flex shrink-0 items-center gap-1' }, [
      showTitle ? h('h3', { class: 'min-w-0 flex-1 truncate font-semibold', title: child.name, style: { fontSize: child.titleSize + 'px' } }, child.name) : h('span', { class: 'flex-1' }),
      !props.editable && child.expandable ? h(MfButton, { size: 'sm', variant: 'ghost', shape: 'circle', 'aria-label': t('dashboard.studio.expandWidget'), onClick: () => emit('expand', child.id) }, () => h(Maximize, { size: 13 })) : null,
    ]) : null
    const content = dashboardContainerTool(child.kind)
      ? h(ContainerComponent, { ...props, widget: child, onFilter: (widget: DashboardWidget, rows: QueryChartRow[]) => emit('filter', widget, rows), onExpand: (id: string) => emit('expand', id) })
      : h(DashboardWidgetView, { widget: child, report, snapshot: dashboardSnapshot(report), refreshed: props.refreshedFor(child),
        selection: props.selectionFor(child), days: props.days, wall: props.wall, editable: props.editable,
        loading: !!child.reportCode && props.pendingCodes?.has(child.reportCode), onFilter: rows => emit('filter', child, rows) })
    return h('div', { class: ['flex h-full min-h-0 min-w-0 flex-col overflow-hidden', child.showCard && 'border-line bg-surface shadow-mf-sm',
      child.showCard && child.border && 'border', props.editable && props.selectedId === child.id && 'outline-2 outline-primary',
      !props.editable && child.animation?.effect !== 'none' && child.animation && 'dashboard-widget-motion', child.animation?.trigger === 'hover' && 'motion-on-hover'],
      'data-contained-widget': child.id, tabindex: props.editable ? -1 : undefined,
      style: { ...dashboardCardStyle(child), ...(!props.editable ? dashboardMotionStyle(child) : {}) } }, [
      heading,
      child.reportCode && props.failedCodes.has(child.reportCode) ? h('p', { class: 'mb-1 text-xs text-warning' }, t('queryChart.refreshError')) : null,
      h('div', { class: 'min-h-0 min-w-0 flex-1', inert: props.editable && !dashboardContainerTool(child.kind) }, [content]),
    ])
  },
})
</script>

<style scoped>
.scroll-viewport { scrollbar-width: none; }
.scroll-viewport::-webkit-scrollbar { display: none; }
.spotlight-wrapper { container-type: inline-size; }
.spotlight-grid {
  grid-template-columns: repeat(var(--spotlight-columns), minmax(0, 1fr));
  grid-auto-rows: var(--spotlight-row);
}
.spotlight-grid > :first-child { grid-area: span 2 / span 2; }
.spotlight-grid > :only-child { grid-column: 1 / -1; }
@container (max-width: 540px) {
  .spotlight-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .spotlight-grid > :first-child { grid-column: 1 / -1; grid-row: span 1; }
}
@container (max-width: 300px) {
  .spotlight-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>
