<template>
  <MfEmpty v-if="!before || !after" class="h-full" size="sm" :title="$t('dashboard.studio.compareImagesNeeded')"/>
  <div v-else ref="track" role="slider" tabindex="0" aria-orientation="horizontal" :aria-valuemin="0" :aria-valuemax="100" :aria-valuenow="Math.round(position)" :aria-label="$t('dashboard.studio.compareImages')"
       class="relative h-full min-h-20 touch-none overflow-hidden outline-none focus-visible:ring-2 focus-visible:ring-primary" @pointerdown="start" @pointermove="move" @pointerup="finish" @pointercancel="finish" @keydown="key">
    <img :src="after" :alt="afterLabel" class="pointer-events-none absolute inset-0 size-full object-cover" draggable="false"/>
    <img :src="before" :alt="beforeLabel" class="pointer-events-none absolute inset-0 size-full object-cover" :style="{clipPath:`inset(0 ${100-position}% 0 0)`}" draggable="false"/>
    <div class="pointer-events-none absolute inset-y-0 w-0.5 bg-primary" :style="{left:position+'%'}"><span class="absolute left-1/2 top-1/2 flex size-8 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-primary text-on-primary shadow-mf-sm"><ChevronLeft :size="13"/><ChevronRight :size="13"/></span></div>
    <span v-if="beforeLabel" class="pointer-events-none absolute left-2 top-2 max-w-2/5 truncate rounded-mf-sm bg-surface/90 px-2 py-1 text-[.8em]">{{ beforeLabel }}</span>
    <span v-if="afterLabel" class="pointer-events-none absolute right-2 top-2 max-w-2/5 truncate rounded-mf-sm bg-surface/90 px-2 py-1 text-[.8em]">{{ afterLabel }}</span>
  </div>
</template>
<script setup lang="ts">
import {ref,watch} from 'vue'
import {ChevronLeft,ChevronRight} from '@lucide/vue'
const props=defineProps<{before:string;after:string;beforeLabel?:string;afterLabel?:string;initial?:number}>()
const track=ref<HTMLElement>(),position=ref(props.initial??50)
watch(()=>props.initial,value=>{position.value=Math.max(0,Math.min(100,value??50))})
let pointer:number|undefined
function update(event:PointerEvent){const rect=track.value?.getBoundingClientRect();if(rect?.width)position.value=Math.max(0,Math.min(100,(event.clientX-rect.left)/rect.width*100))}
function start(event:PointerEvent){if(event.button!==0)return;pointer=event.pointerId;track.value?.setPointerCapture(pointer);track.value?.focus();update(event)}
function move(event:PointerEvent){if(event.pointerId===pointer)update(event)}
function finish(event:PointerEvent){if(event.pointerId===pointer){if(track.value?.hasPointerCapture(pointer))track.value.releasePointerCapture(pointer);pointer=undefined}}
function key(event:KeyboardEvent){const delta=event.shiftKey?10:1;const next=event.key==='ArrowLeft'?position.value-delta:event.key==='ArrowRight'?position.value+delta:event.key==='Home'?0:event.key==='End'?100:undefined;if(next!==undefined){event.preventDefault();position.value=Math.max(0,Math.min(100,next))}}
</script>
