<template>
  <aside class="flex h-full min-h-0 min-w-0 flex-col overflow-hidden border-r border-line bg-surface shadow-mf-sm">
    <DashboardPanelHeader :title="$t('dashboard.studio.library')" :description="$t('dashboard.studio.librarySubtitle')"
                          :pinned="pinned" :pinnable="pinnable" @pin="emit('pin')" @close="emit('close')"/>
    <nav class="mx-3 flex shrink-0 items-center gap-1 px-1 shadow-[inset_0_-1px_0_var(--mf-color-line)]" :aria-label="$t('dashboard.studio.library')">
      <button v-for="item in tabs" :key="item.key" type="button"
              class="flex min-w-0 flex-1 cursor-pointer select-none items-center justify-center gap-1.5 whitespace-nowrap border-b-2 px-1.5 py-2 text-sm font-medium transition-[color,border-color] duration-(--mf-duration-base) ease-(--mf-easing)"
              :class="tab === item.key ? 'border-b-primary text-primary' : 'border-b-transparent text-content-secondary hover:text-content'"
              :aria-pressed="tab === item.key" @click="tab = item.key">
        <component :is="item.icon" :size="15"/>{{ $t('dashboard.studio.' + item.label) }}
      </button>
    </nav>

    <div v-if="tab === 'tools'" class="mx-3 my-3 shrink-0">
      <MfInput v-model="toolQuery" size="sm" :placeholder="$t('dashboard.studio.searchTools')" clearable>
        <template #prefix><Search :size="14"/></template>
      </MfInput>
    </div>
    <div v-if="tab === 'tools'" class="grid min-h-0 flex-1 grid-cols-[108px_minmax(0,1fr)] border-t border-line">
      <nav class="overflow-y-auto border-r border-line p-2" :aria-label="$t('dashboard.studio.toolCategories')">
        <p class="mb-2 px-1 text-xs font-semibold text-content-tertiary">{{ $t('dashboard.studio.toolCategories') }}</p>
        <button v-for="group in toolGroups" :key="group.title" type="button" :aria-pressed="category === group.title && !toolQuery.trim()"
                class="mb-1 flex w-full items-center gap-1 rounded-mf-md px-1.5 py-2 text-left text-xs hover:bg-surface-hover"
                :class="category === group.title && !toolQuery.trim() ? 'bg-primary/10 font-semibold text-primary' : 'text-content-secondary'"
                @click="category = group.title; toolQuery = ''">
          <component :is="group.icon" :size="14" class="shrink-0"/>
          <span class="min-w-0 flex-1 truncate" :title="$t('dashboard.studio.' + group.title)">{{ $t('dashboard.studio.' + group.title) }}</span>
          <span class="shrink-0 rounded-full bg-surface-hover px-1 text-[10px] tabular-nums">{{ group.items.length }}</span>
        </button>
      </nav>
      <section class="min-h-0 overflow-y-auto p-2.5">
        <h3 class="mb-2 text-xs font-semibold">
          {{ $t(toolQuery.trim() ? 'dashboard.studio.toolMatches' : 'dashboard.studio.' + category) }} ({{ visibleTools.length }})
        </h3>
        <MfEmpty v-if="!visibleTools.length" class="min-h-28" size="sm" :title="$t('dashboard.studio.noTools')"/>
        <div v-else class="grid grid-cols-2 gap-2">
          <button v-for="tool in visibleTools" :key="tool.kind" type="button" draggable="true"
                  class="flex min-h-16 min-w-0 flex-col items-center justify-center gap-1 rounded-mf-md border border-line px-1 py-2 text-center text-[11px] font-semibold hover:border-primary/50 hover:bg-primary/8"
                  :title="toolLabel(tool.kind)" @click="emit('add', tool.kind)" @dragstart="startDrag($event, tool.kind)">
            <QueryChartTypeIcon v-if="isChart(tool.kind)" :type="tool.kind" :size="21" class="text-primary"/>
            <component :is="tool.icon" v-else :size="21" class="text-primary"/>
            {{ toolLabel(tool.kind) }}
          </button>
        </div>
      </section>
    </div>

    <div v-else class="min-h-0 flex-1 overflow-y-auto p-3">
      <template v-if="tab === 'assets'">
        <MfInput v-model="keyword" size="sm" :placeholder="$t('dashboard.studio.searchAssets')" clearable>
          <template #prefix><Search :size="14"/></template>
        </MfInput>
        <div v-if="loading" class="flex min-h-36 items-center justify-center"><MfSpinner size="sm"/></div>
        <MfAlert v-else-if="failed" class="mt-3" variant="danger" :title="$t('dashboard.studio.assetsFailed')">
          <MfButton size="sm" @click="emit('search', keyword, page)">{{ $t('common.refresh') }}</MfButton>
        </MfAlert>
        <MfEmpty v-else-if="!reports.length" class="min-h-36" size="sm" :title="$t('dashboard.studio.noAssets')"/>
        <div v-else class="mt-3 space-y-2">
          <button v-for="asset in reports" :key="asset.code" type="button" draggable="true"
                  class="flex w-full min-w-0 items-center gap-2 rounded-mf-md border border-line bg-surface-hover p-2 text-left hover:border-primary/50"
                  @click="emit('add', 'report:' + asset.code)" @dragstart="startDrag($event, 'report:' + asset.code)">
            <span class="grid size-8 shrink-0 place-items-center rounded-mf-sm bg-primary/10 text-primary"><ChartNoAxesCombined :size="16"/></span>
            <span class="min-w-0 flex-1">
              <strong class="block truncate text-xs">{{ asset.name }}</strong>
              <small class="block truncate text-content-tertiary">{{ asset.type || $t('dashboard.studio.tool.report') }}</small>
            </span>
            <Plus :size="14" class="shrink-0 text-primary"/>
          </button>
          <MfPagination v-if="total > pageSize" class="justify-center" :current="page" :total="total" :page-size="pageSize"
                        :show-size="false" @update:current="emit('search', keyword, $event)"/>
        </div>
      </template>

      <DashboardLayers v-else :widgets="widgets" :selected-ids="selectedIds" :wall="wall" :can-group="canGroup"
                       @select="(id, event) => emit('select', id, event)" @reorder="(from, to, above) => emit('reorder', from, to, above)"
                       @context="(event, id) => emit('context', event, id)" @group="emit('group')"/>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DashboardPanelHeader from './DashboardPanelHeader.vue'
import DashboardLayers from './DashboardLayers.vue'
import { ArrowDown, ArrowRight, AudioLines, Blend, ChartNoAxesCombined, Clock, CloudRain, Columns2, Flower2, Frame, Grid2x2, Heading, Image, Layers,
  LayoutGrid, Link, List, ListOrdered, MessageSquare, Minus, Network, PanelTop, PartyPopper, Plus, Search, Shapes, Snowflake, Sparkles, Square, SquareDashed, Star,
  Table2, Tag, Timer, Type, Video, Waves } from '@lucide/vue'
import { queryChartTypes, type QueryChartType } from '@/model/query-chart'
import QueryChartTypeIcon from '@/views/components/visual/query/QueryChartTypeIcon.vue'
import type { ReportModel } from '@/model/report'
import { dashboardWidgetLabelKey, type DashboardTool, type DashboardWidget } from '@/model/dashboard-layout'

defineProps<{ reports: ReportModel[]; widgets: DashboardWidget[]; selectedIds: string[]; wall: boolean; canGroup: boolean;
  loading: boolean; failed: boolean; page: number; pageSize: number; total: number; pinned: boolean; pinnable: boolean }>()
const emit = defineEmits<{ add: [value: string]; search: [keyword: string, page: number]; select: [id: string, event: MouseEvent];
  reorder: [from: string, to: string, above: boolean]; context: [event: MouseEvent, id: string]; group: []; pin: []; close: [] }>()
const tab = defineModel<'tools' | 'assets' | 'layers'>('tab', { required: true })
const { t } = useI18n()
const keyword = ref('')
const toolQuery = ref('')
const category = ref('chartTools')
const tabs = [{ key: 'tools', label: 'componentTab', icon: LayoutGrid }, { key: 'assets', label: 'assetTab', icon: ChartNoAxesCombined }, { key: 'layers', label: 'layers', icon: Layers }] as const
let timer: number | undefined
watch(keyword, value => {
  window.clearTimeout(timer)
  timer = window.setTimeout(() => emit('search', value, 1), 200)
})
onScopeDispose(() => window.clearTimeout(timer))

const toolGroups = [
  { title: 'chartTools', icon: ChartNoAxesCombined, items: [
    ...queryChartTypes.filter(kind => kind !== 'gauge' && kind !== 'table').map(kind => ({ kind, icon: undefined })),
    { kind: 'rank', icon: ListOrdered },
  ] },
  { title: 'metricTools', icon: ListOrdered, items: [
    { kind: 'table', icon: undefined }, { kind: 'metric', icon: Square }, { kind: 'progress', icon: Minus }, { kind: 'gauge', icon: undefined },
    { kind: 'pivot', icon: Table2 }, { kind: 'dataList', icon: List }, { kind: 'dataSummary', icon: ChartNoAxesCombined },
    { kind: 'metricGrid', icon: LayoutGrid }, { kind: 'recordDetail', icon: List }, { kind: 'progressList', icon: ListOrdered },
    { kind: 'dataTags', icon: Tag }, { kind: 'dataTimeline', icon: Clock },
    { kind: 'metricTicker', icon: Square }, { kind: 'metricComparison', icon: ChartNoAxesCombined }, { kind: 'metricTrend', icon: ChartNoAxesCombined },
    { kind: 'bulletMetric', icon: Minus }, { kind: 'rankList', icon: ListOrdered }, { kind: 'distributionList', icon: ChartNoAxesCombined },
    { kind: 'statusGrid', icon: LayoutGrid }, { kind: 'recordCards', icon: Table2 },
  ] },
  { title: 'contentTools', icon: Image, items: [
    { kind: 'text', icon: Type }, { kind: 'image', icon: Image }, { kind: 'video', icon: Video },
    { kind: 'clock', icon: Clock }, { kind: 'link', icon: Link }, { kind: 'heading', icon: Heading },
    { kind: 'note', icon: MessageSquare }, { kind: 'badge', icon: Tag }, { kind: 'audio', icon: AudioLines },
    { kind: 'imageText', icon: Image }, { kind: 'bulletList', icon: List }, { kind: 'linkList', icon: Link },
    { kind: 'countdown', icon: Timer }, { kind: 'icon', icon: Sparkles },
    { kind: 'imageCarousel', icon: Image }, { kind: 'imageGallery', icon: Grid2x2 }, { kind: 'announcement', icon: MessageSquare },
    { kind: 'textTicker', icon: Type }, { kind: 'tabsContent', icon: PanelTop }, { kind: 'accordion', icon: List },
    { kind: 'quote', icon: Type }, { kind: 'embed', icon: Square }, { kind: 'actionButton', icon: Link }, { kind: 'tagCloud', icon: Tag },
  ] },
  { title: 'layoutTools', icon: LayoutGrid, items: [
    { kind: 'shape', icon: Shapes }, { kind: 'container', icon: PanelTop }, { kind: 'divider', icon: Minus },
    { kind: 'panel', icon: Square }, { kind: 'verticalDivider', icon: Columns2 }, { kind: 'sectionDivider', icon: Heading },
    { kind: 'spacer', icon: SquareDashed },
    { kind: 'borderFrame', icon: Frame }, { kind: 'gridPanel', icon: Grid2x2 }, { kind: 'gradientPanel', icon: Blend },
    { kind: 'horizontalArrow', icon: ArrowRight }, { kind: 'verticalArrow', icon: ArrowDown },
    { kind: 'rowContainer', icon: Columns2 }, { kind: 'columnContainer', icon: List }, { kind: 'gridContainer', icon: Grid2x2 },
    { kind: 'tabsContainer', icon: PanelTop }, { kind: 'stackContainer', icon: Layers }, { kind: 'splitContainer', icon: Columns2 },
    { kind: 'headerContainer', icon: PanelTop }, { kind: 'responsiveContainer', icon: LayoutGrid },
  ] },
  { title: 'decorationTools', icon: Sparkles, items: [
    { kind: 'glowLine', icon: Minus }, { kind: 'glowRing', icon: Shapes }, { kind: 'dotMatrix', icon: Grid2x2 },
    { kind: 'cornerAccent', icon: Frame }, { kind: 'ripple', icon: Shapes }, { kind: 'ribbon', icon: Tag },
    { kind: 'doubleLine', icon: Minus }, { kind: 'dashedLine', icon: Minus }, { kind: 'chevrons', icon: ArrowRight }, { kind: 'zigzag', icon: Sparkles },
    { kind: 'waveLine', icon: Waves }, { kind: 'bracketPair', icon: Columns2 }, { kind: 'titlePlate', icon: PanelTop }, { kind: 'badgeSeal', icon: Tag },
    { kind: 'hexagon', icon: Shapes }, { kind: 'honeycomb', icon: Grid2x2 }, { kind: 'radarSweep', icon: Shapes }, { kind: 'scanFrame', icon: Frame },
    { kind: 'circuitPattern', icon: Network }, { kind: 'orbitalRings', icon: Shapes },
  ] },
  { title: 'screenEffectTools', icon: Sparkles, items: [
    { kind: 'snow', icon: Snowflake }, { kind: 'rain', icon: CloudRain }, { kind: 'stars', icon: Star }, { kind: 'meteors', icon: Sparkles },
    { kind: 'fireflies', icon: Sparkles }, { kind: 'bubbles', icon: Shapes }, { kind: 'confetti', icon: PartyPopper }, { kind: 'petals', icon: Flower2 },
    { kind: 'particleNetwork', icon: Network }, { kind: 'aurora', icon: Waves },
    { kind: 'fog', icon: CloudRain }, { kind: 'smoke', icon: Waves }, { kind: 'clouds', icon: CloudRain }, { kind: 'sandstorm', icon: Sparkles },
    { kind: 'embers', icon: Sparkles }, { kind: 'leaves', icon: Flower2 }, { kind: 'hearts', icon: Shapes }, { kind: 'sparkles', icon: Star },
    { kind: 'lightning', icon: Sparkles }, { kind: 'fireworks', icon: PartyPopper }, { kind: 'risingDigits', icon: Type },
    { kind: 'warpStars', icon: Star }, { kind: 'oceanWaves', icon: Waves }, { kind: 'sunRays', icon: Sparkles },
  ] },
] as const

const visibleTools = computed(() => {
  const search = toolQuery.value.trim().toLocaleLowerCase()
  if (search) return toolGroups.flatMap(group => [...group.items]).filter(tool => toolLabel(tool.kind).toLocaleLowerCase().includes(search) || tool.kind.toLowerCase().includes(search))
  return toolGroups.find(group => group.title === category.value)?.items ?? []
})
function toolLabel(kind: DashboardTool) { return t(dashboardWidgetLabelKey(kind)) }
function isChart(kind: string): kind is QueryChartType { return queryChartTypes.some(type => type === kind) }
function startDrag(event: DragEvent, value: string) {
  event.dataTransfer?.setData('application/x-metaforge-dashboard', value)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'copy'
}
</script>
