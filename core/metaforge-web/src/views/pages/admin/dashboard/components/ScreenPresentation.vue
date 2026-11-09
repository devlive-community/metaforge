<template>
  <div class="dashboard-screen-host relative isolate flex min-h-0 min-w-0 flex-1 overflow-hidden" :style="{ backgroundColor }">
    <CanvasBackground :board="board" class="z-0"/>
    <div class="pointer-events-none absolute inset-0 z-[1]">
      <ParticleEffects v-for="effect in backgroundEffects" :key="effect.id" :widget="effect" :kind="effect.kind"/>
    </div>
    <div class="relative z-10 min-h-0 min-w-0 flex-1"
         :class="wall ? 'flex items-center justify-center overflow-hidden [container-type:size]' : 'overflow-auto'">
      <div :class="wall ? 'contents' : 'flex min-h-full flex-col'">
        <ScreenBoard :design="design" :reports="reports" :wall-fullscreen="wall" :transparent="backgroundEffects.length > 0 || !!dashboardResourceUrl(board.backgroundImage)"/>
      </div>
    </div>
    <div class="pointer-events-none absolute inset-0 z-20">
      <ParticleEffects v-for="effect in foregroundEffects" :key="effect.id" :widget="effect" :kind="effect.kind"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { dashboardCanvasColor, dashboardResourceUrl, dashboardScreenEffectSettings, dashboardScreenEffectTool, type DashboardDesign } from '@/model/dashboard-layout'
import type { ReportModel } from '@/model/report'
import ScreenBoard from '@/views/pages/admin/dashboard/components/ScreenBoard.vue'
import ParticleEffects from '@/views/components/visual/effects/ParticleEffects.vue'
import CanvasBackground from '@/views/components/visual/effects/CanvasBackground.vue'

const props = defineProps<{ design: DashboardDesign; reports: ReportModel[] }>()
const wall = computed(() => props.design.mode === 'wall')
const board = computed(() => props.design.boards[props.design.mode])
const backgroundColor = computed(() => dashboardCanvasColor(board.value.background) || (wall.value ? board.value.theme === 'jade' ? '#071d1f' : '#061728' : 'var(--mf-color-surface)'))
const effects = computed(() => board.value.widgets.flatMap(widget => dashboardScreenEffectTool(widget.kind)
  && dashboardScreenEffectSettings(widget.kind, widget.screenEffect).enabled ? [{ ...widget, kind: widget.kind }] : []))
const backgroundEffects = computed(() => effects.value.filter(widget => widget.screenEffect?.layer === 'background'))
const foregroundEffects = computed(() => effects.value.filter(widget => widget.screenEffect?.layer !== 'background'))
</script>
