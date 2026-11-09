<template>
  <div ref="host" class="relative size-full min-h-0 min-w-0 overflow-hidden">
    <div :class="fitContent ? 'absolute left-1/2 top-1/2 origin-center' : 'size-full'" :style="contentSize">
      <div v-if="effect" class="relative isolate h-full overflow-hidden rounded-mf-md bg-[#101e36]">
        <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_60%_70%,#305175,transparent_72%)]"/>
        <ParticleEffects :widget="example.widget" :kind="effect"/>
      </div>
      <ScreenContainer v-else-if="example.children.length" :widget="example.widget" :widgets="example.children" :reports="[]" :days="0"
                       :refreshed-for="noRows" :selection-for="noRows" :failed-codes="emptyCodes"/>
      <div v-else-if="kind === 'video'" class="relative grid h-full place-items-center overflow-hidden rounded-mf-md bg-[#101e36]">
        <img :src="art[0]" alt="" class="absolute inset-0 size-full object-cover opacity-70"/>
        <span class="relative grid size-11 place-items-center rounded-full bg-black/55 text-white"><Play :size="21"/></span>
        <span class="absolute inset-x-3 bottom-2 flex items-center gap-2 text-xs text-white"><span class="h-1 flex-1 rounded-full bg-white/60"/>0:24</span>
      </div>
      <div v-else-if="kind === 'audio'" class="flex h-full flex-col justify-center gap-3 rounded-mf-md bg-surface-hover p-4">
        <div class="flex h-12 items-center gap-1 text-primary">
          <span v-for="(height, index) in wave" :key="index" class="flex-1 rounded-full bg-current" :style="{ height: height + '%' }"/>
        </div>
        <span class="flex items-center gap-2 text-sm font-medium"><Play :size="15"/>0:18</span>
      </div>
      <div v-else-if="kind === 'embed'" class="flex h-full flex-col overflow-hidden rounded-mf-md border border-line">
        <div class="flex shrink-0 items-center gap-2 bg-surface-hover p-2 text-xs"><Globe :size="14"/>{{ $t('dashboard.example.embeddedPage') }}</div>
        <img :src="art[1]" alt="" class="min-h-0 w-full flex-1 object-cover"/>
      </div>
      <div v-else-if="kind === 'spacer'" class="grid h-full place-items-center rounded-mf-md border border-dashed border-line text-xs text-content-tertiary">
        {{ $t('dashboard.example.spacer') }}
      </div>
      <ScreenWidget v-else :widget="example.widget" :snapshot="example.snapshot" :report="example.report" :days="0" class="size-full"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Globe, Play } from '@lucide/vue'
import { computed, onMounted, onScopeDispose, provide, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardChartType, dashboardDataTool, dashboardScreenEffectTool, type DashboardWidgetKind } from '@/model/dashboard-layout'
import { screenComponentExample } from '@/model/screen-component-example'
import { dashboardFiltersContext } from '@/composables/dashboardFiltersContext'
import ParticleEffects from '@/views/components/visual/effects/ParticleEffects.vue'
import ScreenContainer from './ScreenContainer.vue'
import ScreenWidget from './ScreenWidget.vue'

const props = defineProps<{ kind: DashboardWidgetKind; title: string }>()
const { t } = useI18n()
const art = [
  import.meta.env.BASE_URL + 'component-examples/landscape.svg',
  import.meta.env.BASE_URL + 'component-examples/landscape-alt.svg',
] as const
const effect = computed(() => dashboardScreenEffectTool(props.kind) ? props.kind : undefined)
const example = computed(() => screenComponentExample(props.kind, props.title, t, art))
const host = ref<HTMLElement>()
const viewport = ref({ width: 0, height: 0 })
const fitContent = computed(() => !effect.value && !dashboardChartType(props.kind) && !['report', 'video', 'audio', 'embed', 'spacer'].includes(props.kind))
const contentSize = computed(() => {
  if (!fitContent.value) return
  const width = 448
  const height = dashboardDataTool(props.kind) ? 350 : 310
  const scale = Math.min(viewport.value.width / width, viewport.value.height / height, 1)
  return { width: width + 'px', height: height + 'px', transform: `translate(-50%, -50%) scale(${scale})` }
})
let observer: ResizeObserver | undefined
onMounted(() => {
  if (!host.value) return
  viewport.value = { width: host.value.clientWidth, height: host.value.clientHeight }
  observer = new ResizeObserver(([entry]) => { viewport.value = { width: entry.contentRect.width, height: entry.contentRect.height } })
  observer.observe(host.value)
})
onScopeDispose(() => observer?.disconnect())
const wave = [21, 35, 44, 26, 60, 75, 41, 52, 90, 63, 30, 56, 72, 44, 88, 56, 36, 68, 44, 62, 33, 52, 27, 17]
const emptyCodes = new Set<string>()
const noRows = () => undefined
provide(dashboardFiltersContext, {
  read: widget => {
    switch (widget.kind) {
      case 'selectFilter': return { kind: 'selectFilter', value: 'North' }
      case 'multiValueFilter': return { kind: 'multiValueFilter', values: ['North', 'South'] }
      case 'searchFilter': return { kind: 'searchFilter', value: 'North' }
      case 'numberRangeFilter': return { kind: 'numberRangeFilter', min: 40, max: 90 }
      case 'dateRangeFilter': return { kind: 'dateRangeFilter', start: '2026-10-01', end: '2026-10-31' }
      case 'relativeDateFilter': return { kind: 'relativeDateFilter', days: 7 }
      case 'valueStateFilter': return { kind: 'valueStateFilter', presence: 'present' }
      case 'booleanFilter': return { kind: 'booleanFilter', checked: true }
    }
  },
  active: () => true,
  write: () => {},
  clear: () => {},
})
</script>
