<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EChartsOption } from 'echarts'
import type { HomeActivity } from '@/composables/useHomeAnalytics'
import { useChartColors } from '@/composables/useChartColors'
import EChart from '@/views/components/visual/EChart.vue'
const props = defineProps<{ activity: HomeActivity }>()
const { t, locale } = useI18n(), theme = useChartColors()
const sources = computed(() => props.activity.sources.map(source => ({ ...source, name: source.name || t('homeDashboard.unknownSource') })))
const option = computed<EChartsOption>(() => ({ animation: false, color: theme.value.colors,
  tooltip: { trigger: 'item', renderMode: 'richText', confine: true, formatter: '{b}: {c} ({d}%)' },
  graphic: [{ type: 'text', left: 'center', top: '38%', style: { text: t('homeDashboard.periodQueries'), fill: theme.value.text, fontSize: 11 } },
    { type: 'text', left: 'center', top: '50%', style: { text: props.activity.total.toLocaleString(locale.value), fill: theme.value.title, fontSize: 25, fontWeight: 700 } }],
  series: [{ type: 'pie', radius: ['64%', '90%'], center: ['50%', '50%'], label: { show: false },
    emphasis: { scaleSize: 3 }, data: sources.value.map(source => ({ name: source.name, value: source.count })) }],
}))
</script>
<template>
  <div class="flex h-full min-h-0 min-w-0 items-center gap-3">
    <EChart :option="option" class="h-full max-h-48 min-w-0 flex-1"/>
    <ul class="flex max-h-48 w-[42%] shrink-0 flex-col gap-3 overflow-y-auto text-xs">
      <li v-for="(source,index) in sources" :key="source.name" class="flex min-w-0 items-center gap-2"><span class="size-2 shrink-0 rounded-sm" :style="{ backgroundColor: theme.colors[index % theme.colors.length] }"/><MfTooltip :content="source.name" class="min-w-0 flex-1"><span class="block max-w-full truncate">{{ source.name }}</span></MfTooltip><span class="shrink-0 tabular-nums">{{ Math.round(source.count / activity.total * 100) }}%</span></li>
    </ul>
  </div>
</template>
