<template>
  <div class="relative size-full overflow-hidden" :style="{ color: primary, opacity: settings.opacity / 100, transform: `rotate(${settings.rotation}deg)` }">
    <template v-if="widget.kind === 'cornerFrame'">
      <div v-for="(corner,index) in ['topLeft','topRight','bottomRight','bottomLeft']" :key="corner" class="pointer-events-none absolute" :style="{ width: `min(${widget.cornerSize || 32}px,50%)`, height: `min(${widget.cornerSize || 32}px,50%)`, top: index < 2 ? 0 : undefined, bottom: index > 1 ? 0 : undefined, left: index === 0 || index === 3 ? 0 : undefined, right: index === 1 || index === 2 ? 0 : undefined, borderStyle: 'solid', borderColor: 'currentColor', borderTopWidth: index < 2 ? stroke + 'px' : 0, borderBottomWidth: index > 1 ? stroke + 'px' : 0, borderLeftWidth: index === 0 || index === 3 ? stroke + 'px' : 0, borderRightWidth: index === 1 || index === 2 ? stroke + 'px' : 0 }"/>
    </template>
    <svg v-else-if="widget.kind === 'radarSweep'" viewBox="0 0 100 100" class="size-full" fill="none" stroke="currentColor" :stroke-width="stroke" aria-hidden="true">
      <circle v-for="radius in [15,30,45]" :key="radius" cx="50" cy="50" :r="radius" opacity=".35"/><path d="M5 50H95M50 5V95" opacity=".35"/>
      <path d="M50 50L50 5A45 45 0 0 1 95 50Z" fill="currentColor" fill-opacity=".12" :class="active && 'radar-sector'"/>
      <circle cx="68" cy="32" r="2" fill="currentColor"/>
    </svg>
    <svg v-else-if="widget.kind === 'hexagon' || widget.kind === 'badgeSeal' || widget.kind === 'orbitalRings'" viewBox="0 0 100 100" class="size-full" :fill="settings.filled ? 'currentColor' : 'none'" fill-opacity=".1" stroke="currentColor" :stroke-width="stroke" aria-hidden="true">
      <polygon v-if="widget.kind === 'hexagon'" points="25,7 75,7 97,50 75,93 25,93 3,50"/>
      <polygon v-else-if="widget.kind === 'badgeSeal'" :points="seal"/>
      <g v-else fill="none"><ellipse v-for="angle in [0,60,120]" :key="angle" cx="50" cy="50" rx="45" ry="18" :transform="`rotate(${angle} 50 50)`"/><circle cx="50" cy="50" r="5" fill="currentColor"/></g>
    </svg>
    <svg v-else viewBox="0 0 200 100" preserveAspectRatio="none" class="size-full decor-lines" fill="none" stroke="currentColor" :stroke-width="stroke" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <g v-if="widget.kind === 'techRuler'"><path d="M3 70H197"/><path v-for="index in settings.count + 1" :key="index" :d="`M${3+(index-1)*194/settings.count} 70v-${(index-1)%5===0?35:18}`"/></g>
      <g v-else-if="widget.kind === 'sectionBanner'"><path d="M3 50H52M148 50H197"/><path d="M62 18H138L148 50L138 82H62L52 50Z" :fill="settings.filled?'currentColor':'none'" fill-opacity=".1"/></g>
      <g v-else-if="widget.kind === 'bezierConnector'"><path d="M3 82C70 82 130 18 197 18" opacity=".25"/><path d="M3 82C70 82 130 18 197 18" pathLength="200" :stroke-dasharray="`${200/settings.count/3} ${200/settings.count*2/3}`" :class="active && 'flow-connector'"/></g>
      <path v-else-if="widget.kind === 'doubleLine'" d="M3 44H197M3 56H197"/>
      <path v-else-if="widget.kind === 'dashedLine'" d="M3 50H197" stroke-dasharray="8 6"/>
      <g v-else-if="widget.kind === 'chevrons'"><path v-for="index in settings.count" :key="index" :d="`M${(index-1)*190/settings.count+3} 20l${90/settings.count} 30l-${90/settings.count} 30`"/></g>
      <polyline v-else-if="widget.kind === 'zigzag'" :points="zigzag"/>
      <path v-else-if="widget.kind === 'waveLine'" :d="wave"/>
      <path v-else-if="widget.kind === 'bracketPair'" d="M32 7H5V93H32M168 7H195V93H168"/>
      <path v-else-if="widget.kind === 'titlePlate'" d="M15 8H185L197 28V72L185 92H15L3 72V28Z" :fill="settings.filled ? 'currentColor' : 'none'" fill-opacity=".1"/>
      <template v-else-if="widget.kind === 'honeycomb'">
        <defs><pattern :id="patternId" :width="200/settings.count" :height="175/settings.count" viewBox="0 0 40 35" patternUnits="userSpaceOnUse"><path d="M10 1H30L40 17.5L30 34H10L0 17.5Z" opacity=".45"/></pattern></defs>
        <rect width="200" height="100" :fill="`url(#${patternId})`" stroke="none"/>
      </template>
      <template v-else-if="widget.kind === 'scanFrame'"><path d="M30 4H4V30M170 4H196V30M4 70V96H30M170 96H196V70"/><path d="M12 20H188" opacity=".6" :class="active && 'scan-line'"/></template>
      <g v-else-if="widget.kind === 'circuitPattern'" opacity=".7"><path d="M2 20H55L80 45H130L155 20H198M2 80H35L65 50H100L130 80H198M25 2V20M175 80V98"/><circle v-for="point in [[55,20],[80,45],[130,45],[35,80],[100,50],[175,80]]" :key="point.join('-')" :cx="point[0]" :cy="point[1]" r="3" fill="currentColor"/></g>
    </svg>
    <div v-if="widget.kind === 'titlePlate' || widget.kind === 'badgeSeal' || widget.kind === 'sectionBanner'" class="pointer-events-none absolute inset-0 flex items-center justify-center p-5 text-center font-semibold">{{ widget.text || widget.name }}</div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { v4 as uuid } from 'uuid'
import { dashboardCanvasColor, dashboardDecorationSettings, type DashboardWidget } from '@/model/dashboard-layout'
const props = defineProps<{ widget: Pick<DashboardWidget, 'color' | 'cornerSize' | 'decorationOptions' | 'kind' | 'name' | 'strokeWidth' | 'text'>; editable?: boolean }>()
const settings = computed(() => dashboardDecorationSettings(props.widget.decorationOptions))
const primary = computed(() => dashboardCanvasColor(props.widget.color) || 'var(--mf-color-primary)')
const stroke = computed(() => props.widget.strokeWidth ?? 2)
const active = computed(() => !props.editable && settings.value.animate)
const patternId = 'honeycomb-' + uuid()
const zigzag = computed(() => Array.from({length:settings.value.count*2+1},(_,index)=>`${3+index*194/(settings.value.count*2)},${index%2?65:35}`).join(' '))
const wave = computed(() => Array.from({length:81},(_,index)=>`${index?'L':'M'}${index*2.5} ${50+Math.sin(index/80*Math.PI*settings.value.count)*18}`).join(' '))
const seal = computed(() => Array.from({length:24},(_,index)=>{const angle=index*Math.PI/12-Math.PI/2,r=index%2?39:47;return `${50+Math.cos(angle)*r},${50+Math.sin(angle)*r}`}).join(' '))
</script>
<style scoped>
.decor-lines :deep(path), .decor-lines :deep(polyline), .decor-lines :deep(circle) { vector-effect: non-scaling-stroke; }
.flow-connector { animation: connector-flow 3s linear infinite; }
@keyframes connector-flow { to { stroke-dashoffset: -200; } }
.radar-sector { transform-origin: 50% 50%; transform-box: view-box; animation: radar-turn 5s linear infinite; }
.scan-line { animation: scan-pass 3s ease-in-out infinite alternate; }
@keyframes radar-turn { to { transform: rotate(360deg); } }
@keyframes scan-pass { to { transform: translateY(60px); } }
@media (prefers-reduced-motion: reduce) { .radar-sector,.scan-line,.flow-connector { animation: none; } }
</style>
