<template>
  <component :is="icons[type]" :size="size" aria-hidden="true"/>
</template>

<script setup lang="ts">
import {
  Activity, Boxes, ChartArea, ChartBar, ChartBarStacked, ChartCandlestick, ChartColumn, ChartColumnStacked, ChartLine, ChartNoAxesColumnIncreasing,
  ChartPie, ChartScatter, ChartSpline, Circle, CodeXml, Flower2, Funnel, Gauge, Grid2x2, Map, Network, Radar, Route,
  Sparkles, Sun, Table2, Workflow,
} from '@lucide/vue'
import type { QueryChartType } from '@/model/query-chart'

withDefaults(defineProps<{ type: QueryChartType; size?: number }>(), { size: 16 })
const icons = {
  line: ChartLine, bar: ChartColumn, area: ChartArea, pie: ChartPie, donut: Circle,
  scatter: ChartScatter, horizontal: ChartBar, radar: Radar, funnel: Funnel, rose: Flower2, table: Table2,
  effectScatter: Sparkles, pictorialBar: ChartNoAxesColumnIncreasing, heatmap: Grid2x2, gauge: Gauge,
  candlestick: ChartCandlestick, boxplot: ChartNoAxesColumnIncreasing, histogram: ChartColumn, parallel: Activity, themeRiver: ChartSpline,
  tree: Network, treemap: Boxes, sunburst: Sun, graph: Network, sankey: Workflow, map: Map, lines: Route, custom: CodeXml,
  stepLine: Activity, stackedBar: ChartColumnStacked, stackedHorizontal: ChartBarStacked, stackedArea: ChartArea,
  percentStackedBar: ChartColumnStacked, bubble: ChartScatter,
  stackedLine: ChartSpline, percentStackedHorizontal: ChartBarStacked,
  waterfall: ChartNoAxesColumnIncreasing, lollipop: ChartScatter, dumbbell: Route, rangeBand: ChartArea,
  dualAxis: Activity, polarBar: Radar, polarStackedBar: ChartPie, polarLine: Circle,
  pareto: ChartColumnStacked, cumulativeLine: ChartLine, deviationBar: ChartColumn, mirrorBar: ChartBarStacked,
  stripPlot: ChartScatter, ecdf: Activity, scatterFit: ChartScatter, calendarMatrix: Grid2x2, radialBar: Radar,
  frequencyPolygon: ChartSpline, targetComparison: ChartNoAxesColumnIncreasing, rankTrend: Route,
  tukeyBox: ChartCandlestick, violinPlot: ChartArea, ridgePlot: ChartArea, quantileComparison: ChartScatter,
  percentileProfile: ChartSpline, lorenzCurve: ChartLine, ganttTimeline: ChartBarStacked, signedShareBar: ChartBarStacked,
  quadrantScatter: ChartScatter, lagScatter: ChartScatter, indexTrend: ChartLine, movingAverage: ChartSpline,
} satisfies Record<QueryChartType, typeof ChartLine>
</script>
