<template>
  <MfEmpty v-if="!children.length" class="h-full" size="sm" :title="$t('dashboard.studio.containerEmpty')" :description="editable ? $t('dashboard.studio.containerEmptyHint') : undefined"/>
  <MfTabs v-else-if="widget.kind === 'tabsContainer'" v-model:active-key="activeTab" fill class="h-full min-w-0">
    <MfTabPane v-for="child in children" :key="child.id" :item-key="child.id" :tab="child.name">
      <div class="h-full min-h-0" :style="{ padding: layout.padding + 'px' }"><RenderChild :child="child"/></div>
    </MfTabPane>
  </MfTabs>
  <div v-else class="container-layout h-full min-h-0 min-w-0 overflow-auto" :style="layoutStyle">
    <div v-for="(child, index) in children" :key="child.id" class="min-h-0 min-w-0"
         :style="widget.kind === 'stackContainer' ? { gridArea: '1 / 1' } : widget.kind === 'headerContainer' && index === 0 ? { gridColumn: '1 / -1' } : undefined">
      <RenderChild :child="child"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, getCurrentInstance, h, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfButton } from 'metaforge-ui'
import { Maximize } from '@lucide/vue'
import { dashboardCardStyle, dashboardContainerTool, dashboardDescendantIds, dashboardLayoutSettings, dashboardMotionStyle, dashboardSnapshot,
  type DashboardWidget } from '@/model/dashboard-layout'
import type { QueryChartRow } from '@/model/query-chart'
import type { ReportModel } from '@/model/report'
import DashboardWidgetView from './DashboardWidgetView.vue'

const props = defineProps<{ widget: DashboardWidget; widgets: DashboardWidget[]; reports: ReportModel[]; days: number; wall?: boolean;
  editable?: boolean; selectedId?: string; refreshedFor: (widget: DashboardWidget) => QueryChartRow[] | undefined;
  selectionFor: (widget: DashboardWidget) => ReadonlySet<QueryChartRow> | undefined; failedCodes: ReadonlySet<string> }>()
const emit = defineEmits<{ filter: [widget: DashboardWidget, rows: QueryChartRow[]]; expand: [id: string] }>()
const { t } = useI18n()
const ContainerComponent = getCurrentInstance()!.type
const children = computed(() => props.widgets.filter(widget => widget.parentId === props.widget.id))
const layout = computed(() => dashboardLayoutSettings(props.widget.layoutOptions))
const activeTab = ref('')
watch(children, items => { if (!items.some(item => item.id === activeTab.value)) activeTab.value = items[0]?.id ?? '' }, { immediate: true })
watch(() => props.selectedId, id => {
  const branch = children.value.find(child => id && dashboardDescendantIds(props.widgets, [child.id]).has(id))
  if (branch) activeTab.value = branch.id
})
const reports = computed(() => new Map(props.reports.map(report => [report.code, report])))
const layoutStyle = computed(() => {
  const count = Math.max(1, children.value.length)
  const columns = props.widget.kind === 'rowContainer' ? `repeat(${count}, minmax(0, 1fr))`
    : props.widget.kind === 'columnContainer' || props.widget.kind === 'stackContainer' ? 'minmax(0, 1fr)'
    : props.widget.kind === 'splitContainer' ? `${layout.value.ratio}% minmax(0, 1fr)`
    : props.widget.kind === 'headerContainer' ? `repeat(${Math.max(1, count - 1)}, minmax(0, 1fr))`
    : props.widget.kind === 'responsiveContainer' ? `repeat(auto-fit, minmax(min(${layout.value.minWidth}px, 100%), 1fr))`
    : `repeat(${layout.value.columns}, minmax(0, 1fr))`
  return { display: 'grid', gridTemplateColumns: columns, gap: layout.value.gap + 'px', padding: layout.value.padding + 'px',
    gridTemplateRows: props.widget.kind === 'headerContainer' ? `${layout.value.header}% minmax(0, 1fr)` : undefined,
    gridAutoRows: 'minmax(0, 1fr)' }
})
const RenderChild = defineComponent({
  props: { child: { type: Object, required: true } },
  setup: (childProps: { child: DashboardWidget }) => () => {
    const child = childProps.child
    const report = reports.value.get(child.reportCode)
    const heading = child.showTitle || (!props.editable && child.expandable) ? h('div', { class: 'mb-1 flex shrink-0 items-center gap-1' }, [
      child.showTitle ? h('h3', { class: 'min-w-0 flex-1 truncate font-semibold', title: child.name, style: { fontSize: child.titleSize + 'px' } }, child.name) : h('span', { class: 'flex-1' }),
      !props.editable && child.expandable ? h(MfButton, { size: 'sm', variant: 'ghost', shape: 'circle', 'aria-label': t('dashboard.studio.expandWidget'), onClick: () => emit('expand', child.id) }, () => h(Maximize, { size: 13 })) : null,
    ]) : null
    const content = dashboardContainerTool(child.kind)
      ? h(ContainerComponent, { ...props, widget: child, onFilter: (widget: DashboardWidget, rows: QueryChartRow[]) => emit('filter', widget, rows), onExpand: (id: string) => emit('expand', id) })
      : h(DashboardWidgetView, { widget: child, report, snapshot: dashboardSnapshot(report), refreshed: props.refreshedFor(child),
        selection: props.selectionFor(child), days: props.days, wall: props.wall, editable: props.editable, onFilter: rows => emit('filter', child, rows) })
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
