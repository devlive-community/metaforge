<template>
  <div ref="root" class="flex h-full min-h-0 min-w-0 flex-col" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <MfEmpty v-if="empty" class="flex-1" size="sm" :title="$t(empty)"/>
    <template v-else-if="widget.kind === 'rollingTable'">
      <ResultTable class="min-h-0 flex-1" fill :headers="fields" :rows="pageRows" :page-size="settings.limit"/>
      <div v-if="pageCount > 1" class="mt-2 flex shrink-0 items-center justify-end gap-2 text-xs text-content-tertiary">
        <MfButton size="sm" variant="ghost" :disabled="pageCount < 2" :aria-label="$t('dashboard.studio.previousPage')" @click="advance(-1)"><ChevronLeft :size="14"/></MfButton>
        <span>{{ page + 1 }} / {{ pageCount }}</span>
        <MfButton size="sm" variant="ghost" :disabled="pageCount < 2" :aria-label="$t('dashboard.studio.nextPage')" @click="advance(1)"><ChevronRight :size="14"/></MfButton>
      </div>
    </template>
    <MfTable v-else-if="widget.kind === 'comparisonTable'" fill :columns="comparisonColumns" :data-source="groups" row-key="name" density="compact">
      <template #cell-name="{ record }"><EllipsisCell :text="record.name"/></template>
      <template v-for="field in ['value','baseline','difference']" :key="field" #[`cell-${field}`]="{ record }"><span class="tabular-nums">{{ format(record[field]) }}</span></template>
      <template #cell-percent="{ record }"><span class="tabular-nums">{{ record.percent === null ? '—' : number.format(record.percent) + '%' }}</span></template>
    </MfTable>
    <MfTable v-else-if="widget.kind === 'statisticList'" fill :columns="statisticsColumns" :data-source="statistics" row-key="field" density="compact">
      <template #cell-field="{ record }"><EllipsisCell :text="record.field"/></template>
      <template v-for="field in ['count','min','max','avg','sum']" :key="field" #[`cell-${field}`]="{ record }"><span class="tabular-nums">{{ field === 'count' ? record.count : format(record[field]) }}</span></template>
    </MfTable>
    <template v-else-if="widget.kind === 'calendarHeatmap'">
      <span class="shrink-0 self-start rounded-mf-sm bg-surface-hover px-2 py-1 text-xs font-semibold tabular-nums text-content-secondary">{{ calendar.year }}</span>
      <EChart class="min-h-0 flex-1" :option="calendarOption"/>
    </template>
    <div v-else-if="widget.kind === 'radialProgress'" class="flex h-full min-h-0 items-center gap-3">
      <EChart class="h-full min-w-0 flex-1" :option="ringsOption"/>
      <ul class="max-h-full w-2/5 shrink-0 space-y-2 overflow-auto text-xs">
        <li v-for="(point,index) in rings" :key="point.name" class="min-w-0"><div class="flex min-w-0 items-center gap-1.5"><span class="size-2 shrink-0 rounded-full" :style="{ backgroundColor: colors.palette[index % colors.palette.length] }"/><EllipsisCell :text="point.name"/></div><strong class="mt-1 block tabular-nums">{{ format(point.value) }} / {{ format(widget.target) }}</strong></li>
      </ul>
    </div>
    <div v-else class="grid min-h-0 gap-2 overflow-auto" :style="{ gridTemplateColumns: 'repeat(auto-fit,minmax(min(160px,100%),1fr))' }">
      <article v-for="point in groups" :key="point.name" class="min-w-0 rounded-mf-md border border-line p-3" :class="point.value! >= widget.target ? 'bg-warning/10' : 'bg-surface-hover'">
        <div class="mb-2 flex min-w-0 items-center justify-between gap-2"><EllipsisCell :text="point.name"/><MfTag size="sm" :variant="point.value! >= widget.target ? 'warning' : 'success'">{{ $t(point.value! >= widget.target ? 'dashboard.studio.thresholdTriggered' : 'dashboard.studio.thresholdNormal') }}</MfTag></div>
        <strong class="block truncate text-xl tabular-nums" :title="format(point.value)">{{ format(point.value) }}</strong>
        <span class="mt-1 block text-xs text-content-tertiary">{{ $t('dashboard.studio.alarmThreshold') }} {{ format(widget.target) }}</span>
      </article>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronLeft, ChevronRight } from '@lucide/vue'
import type { EChartsOption } from 'echarts'
import { aggregateQueryChart, chartFieldKind, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { dashboardContentSettings, dashboardDataSettings, type DashboardWidget } from '@/model/dashboard-layout'
import { calendarMetrics, groupedMetrics, numericStatistics } from '@/model/dashboard-extended'
import { useChartColors } from '@/composables/useChartColors'
import EChart from '@/views/components/visual/EChart.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
const props = defineProps<{ widget: Pick<DashboardWidget, 'color' | 'contentOptions' | 'dataOptions' | 'displayFields' | 'field' | 'kind' | 'operation' | 'target' | 'timeField'>; options: QueryChartOptions; rows: QueryChartRow[]; editable?: boolean }>()
const { t, locale } = useI18n(), theme = useChartColors(), root = ref<HTMLElement>()
const colors = computed(() => {
  theme.value
  const style = getComputedStyle(root.value || document.documentElement), token = (name:string) => style.getPropertyValue('--mf-color-'+name).trim()
  return { primary: props.widget.color || token('primary'), text: token('content-secondary'), line: token('line'), surface: token('surface-hover'), palette: [...new Set([props.widget.color || token('primary'), ...(props.options.colors || '').split(/[,\s]+/).filter(value => /^#[0-9a-f]{6}$/i.test(value)), ...theme.value.colors])] }
})
const settings = computed(() => dashboardDataSettings(props.widget.dataOptions)), playback = computed(() => dashboardContentSettings(props.widget.contentOptions))
const number = computed(() => new Intl.NumberFormat(locale.value,{maximumFractionDigits:settings.value.decimals}))
const format = (value:number|null) => value === null ? '—' : settings.value.prefix+number.value.format(value)+settings.value.suffix
const filtered = computed(() => aggregateQueryChart(props.rows,{...props.options,type:'table'}).filtered)
const fields = computed(() => (props.widget.displayFields?.length ? props.widget.displayFields : props.options.fields).filter(field=>props.options.fields.includes(field)))
const category = computed(() => props.options.fields.includes(props.options.category) ? props.options.category : props.options.fields[0])
const groups = computed(() => groupedMetrics(filtered.value,category.value,props.widget.field,props.widget.operation,settings.value.baselineField,props.options).filter(point=>point.value!==null).slice(0,settings.value.limit))
const statistics = computed(() => numericStatistics(filtered.value,fields.value))
const dateField = computed(() => props.widget.timeField || props.options.fields.find(field => chartFieldKind(filtered.value, field) === 'date') || '')
const calendar = computed(() => calendarMetrics(filtered.value,dateField.value,props.widget.field,props.widget.operation,settings.value.year))
const rings = computed(() => groups.value.slice(0,5))
const empty = computed(() => {
  if(!filtered.value.length)return 'common.noData'
  if(props.widget.kind==='comparisonTable'&&!settings.value.baselineField)return 'dashboard.studio.chooseBaseline'
  if(props.widget.kind==='rollingTable'&&!fields.value.length)return 'dashboard.studio.chooseDisplayFields'
  if(props.widget.kind==='statisticList'&&!statistics.value.length)return 'dashboard.studio.summaryNeedsField'
  if(props.widget.kind==='calendarHeatmap'&&!dateField.value)return 'dashboard.studio.chooseDateField'
  if(props.widget.kind==='calendarHeatmap'&&!calendar.value.points.length)return 'common.noData'
  if(!['rollingTable','statisticList','calendarHeatmap'].includes(props.widget.kind)&&!groups.value.length)return 'dashboard.studio.summaryNeedsField'
})
const comparisonColumns = computed(()=>[{key:'name',title:category.value||t('queryChart.category'),ellipsis:true},...['value','baseline','difference','percent'].map(key=>({key,title:t('dashboard.studio.comparison.'+key),width:105}))])
const statisticsColumns = computed(()=>[{key:'field',title:t('dashboard.studio.statistics.field'),ellipsis:true},...['count','min','max','avg','sum'].map(key=>({key,title:t('dashboard.studio.statistics.'+key),width:95}))])
const page=ref(0),hovered=ref(false),pageCount=computed(()=>Math.max(1,Math.ceil(filtered.value.length/settings.value.limit)))
const pageRows=computed(()=>filtered.value.slice(page.value*settings.value.limit,(page.value+1)*settings.value.limit))
function advance(delta:number){page.value=(page.value+delta+pageCount.value)%pageCount.value}
watch([filtered,pageCount],()=>{if(page.value>=pageCount.value)page.value=0})
let timer:ReturnType<typeof setInterval>|undefined
watch([()=>props.editable,()=>props.widget.kind,()=>playback.value.autoplay,()=>playback.value.interval,pageCount],()=>{
  clearInterval(timer)
  if(props.editable||props.widget.kind!=='rollingTable'||!playback.value.autoplay||pageCount.value<2)return
  timer=setInterval(()=>{if(!document.hidden&&!(hovered.value&&playback.value.pauseOnHover)&&!window.matchMedia('(prefers-reduced-motion: reduce)').matches)advance(1)},playback.value.interval*1000)
},{immediate:true})
onScopeDispose(()=>clearInterval(timer))
const calendarWidth = ref(0)
watch([root, () => props.widget.kind], ([element, kind], _, cleanup) => {
  if (!element || kind !== 'calendarHeatmap') return
  const observer = new ResizeObserver(([entry]) => { calendarWidth.value = entry.contentRect.width })
  observer.observe(element)
  cleanup(() => observer.disconnect())
}, { flush: 'post' })
const calendarOption=computed<EChartsOption>(()=>({animation:false,tooltip:{trigger:'item',renderMode:'richText',confine:true},
 calendar:{range:String(calendar.value.year),cellSize:'auto',left:39,right:10,top:24,bottom:32,splitLine:{lineStyle:{color:colors.value.line}},itemStyle:{borderColor:colors.value.line},
  dayLabel:{firstDay:1,margin:6,fontSize:10,color:colors.value.text,nameMap:Array.from({length:7},(_,i)=>new Intl.DateTimeFormat(locale.value,{weekday:'short',timeZone:'UTC'}).format(new Date(Date.UTC(2024,0,7+i))))},
  monthLabel:{margin:5,fontSize:10,color:colors.value.text,nameMap:Array.from({length:12},(_,i)=>
    i % (calendarWidth.value && calendarWidth.value < 280 ? 3 : calendarWidth.value < 390 ? 2 : 1) ? '' :
      new Intl.DateTimeFormat(locale.value,{month:'short',timeZone:'UTC'}).format(new Date(Date.UTC(2024,i,1))))},yearLabel:{show:false}},
 visualMap:{min:Math.min(0,...calendar.value.points.map(point=>point[1])),max:Math.max(1,...calendar.value.points.map(point=>point[1])),orient:'horizontal',left:'center',bottom:0,itemHeight:90,itemWidth:10,textStyle:{color:colors.value.text},inRange:{color:[colors.value.surface,colors.value.primary]}},
 series:[{type:'heatmap',coordinateSystem:'calendar',data:calendar.value.points}]}))
const ringsOption=computed<EChartsOption>(()=>({animation:false,tooltip:{show:false},series:rings.value.map((point,index)=>{
 const progress=Math.max(0,Math.min(100,(point.value||0)/props.widget.target*100))
 return {type:'pie',radius:[(86-index*14)+'%',(94-index*14)+'%'],silent:true,label:{show:false},data:[{value:progress,itemStyle:{color:colors.value.palette[index%colors.value.palette.length]}},{value:100-progress,itemStyle:{color:colors.value.line}}]}
})}))
</script>
