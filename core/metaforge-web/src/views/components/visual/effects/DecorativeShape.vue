<template>
  <div v-if="widget.kind === 'borderFrame'" class="tech-frame size-full" :style="style"><span/><span/><span/><span/></div>
  <div v-else-if="widget.kind === 'gridPanel'" class="size-full" :style="{ ...style, backgroundImage: 'linear-gradient(90deg, color-mix(in srgb, currentColor 24%, transparent) 1px, transparent 1px), linear-gradient(color-mix(in srgb, currentColor 24%, transparent) 1px, transparent 1px)', backgroundSize: `${widget.gridSize ?? 24}px ${widget.gridSize ?? 24}px` }"/>
  <div v-else-if="widget.kind === 'gradientPanel'" class="size-full" :style="{ background: `linear-gradient(135deg, ${primary}, ${dashboardCanvasColor(widget.secondaryColor) || 'transparent'})` }"/>
  <div v-else-if="widget.kind === 'glowLine'" class="flex size-full items-center" :style="style"><span class="glow-line w-full"/></div>
  <div v-else-if="widget.kind === 'glowRing' || widget.kind === 'ripple'" class="flex size-full items-center justify-center [container-type:size]" :style="style">
    <div class="glow-ring" :class="widget.kind === 'ripple' && 'is-ripple'"><template v-if="widget.kind === 'ripple'"><span/><span/></template></div>
  </div>
  <div v-else-if="widget.kind === 'dotMatrix'" class="size-full" :style="{ ...style, backgroundImage: 'radial-gradient(currentColor 1.5px, transparent 1.5px)', backgroundSize: `${widget.gridSize ?? 24}px ${widget.gridSize ?? 24}px` }"/>
  <div v-else-if="widget.kind === 'cornerAccent'" class="corner-accent size-full" :class="'corner-' + (widget.cornerPosition ?? 'topLeft')"
       :style="{ ...style, '--corner-size': `${widget.cornerSize ?? 64}px` }">
    <svg viewBox="0 0 72 72" fill="none" aria-hidden="true">
      <path d="M8 64V20L20 8H64" stroke="currentColor" :stroke-width="widget.strokeWidth ?? 2" stroke-linecap="round" stroke-linejoin="round" vector-effect="non-scaling-stroke"/>
    </svg>
  </div>
  <div v-else-if="widget.kind === 'ribbon'" class="flex size-full items-center justify-center px-4 text-center font-semibold text-on-primary [clip-path:polygon(0_0,100%_0,calc(100%_-_12px)_50%,100%_100%,0_100%,12px_50%)]" :style="{ backgroundColor: primary }">{{ widget.text || widget.name }}</div>
  <div v-else class="flex size-full items-center justify-center" :style="style">
    <component :is="widget.kind === 'verticalArrow' ? ArrowDown : ArrowRight" class="dashboard-arrow size-full" preserveAspectRatio="none" :stroke-width="widget.strokeWidth ?? 2" aria-hidden="true"/>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowDown, ArrowRight } from '@lucide/vue'
import { dashboardCanvasColor, type DashboardWidget } from '@/model/dashboard-layout'
const props = defineProps<{ widget: Pick<DashboardWidget, 'color' | 'cornerPosition' | 'cornerSize' | 'gridSize' | 'kind' | 'name' | 'secondaryColor' | 'strokeWidth' | 'text'> }>()
const primary = computed(() => dashboardCanvasColor(props.widget.color) || 'var(--mf-color-primary)')
const style = computed(() => ({ color: primary.value, '--decoration-stroke': `${props.widget.strokeWidth ?? 2}px` }))
</script>

<style scoped>
.tech-frame { position: relative; border: 1px solid color-mix(in srgb, currentColor 28%, transparent); }
.tech-frame > span { position: absolute; width: 22px; height: 22px; border-color: currentColor; }
.tech-frame > span:nth-child(1) { inset: -1px auto auto -1px; border-top: var(--decoration-stroke) solid; border-left: var(--decoration-stroke) solid; }
.tech-frame > span:nth-child(2) { inset: -1px -1px auto auto; border-top: var(--decoration-stroke) solid; border-right: var(--decoration-stroke) solid; }
.tech-frame > span:nth-child(3) { inset: auto -1px -1px auto; border-bottom: var(--decoration-stroke) solid; border-right: var(--decoration-stroke) solid; }
.tech-frame > span:nth-child(4) { inset: auto auto -1px -1px; border-bottom: var(--decoration-stroke) solid; border-left: var(--decoration-stroke) solid; }
.dashboard-arrow :deep(path) { vector-effect: non-scaling-stroke; }
.corner-accent { position: relative; container-type: size; }
.corner-accent > svg { position: absolute; width: min(var(--corner-size), 100cqw, 100cqh); height: min(var(--corner-size), 100cqw, 100cqh); }
.corner-topLeft > svg { top: 0; left: 0; }
.corner-topRight > svg { top: 0; right: 0; transform: rotate(90deg); }
.corner-bottomRight > svg { right: 0; bottom: 0; transform: rotate(180deg); }
.corner-bottomLeft > svg { bottom: 0; left: 0; transform: rotate(270deg); }
.glow-line { height: var(--decoration-stroke); background: currentColor; box-shadow: 0 0 10px currentColor; }
.glow-ring { position: relative; width: min(90cqw, 90cqh); height: min(90cqw, 90cqh); border: var(--decoration-stroke) solid currentColor; border-radius: 50%; box-shadow: 0 0 14px color-mix(in srgb, currentColor 45%, transparent); }
.is-ripple > span { position: absolute; inset: 18%; border: 1px solid currentColor; border-radius: 50%; opacity: .65; }
.is-ripple > span:last-child { inset: 35%; opacity: .35; }
</style>
