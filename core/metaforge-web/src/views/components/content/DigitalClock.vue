<template>
  <div class="flex h-full min-h-0 flex-col items-center justify-center gap-1">
    <time class="text-2xl font-semibold tabular-nums" :style="clockAppearance" :datetime="now.toISOString()">{{ time }}</time>
    <span v-if="showDate" class="text-xs text-content-secondary">{{ date }}</span>
  </div>
</template>

<script setup lang="ts">
import { formatClock, formatDay } from '@/utils/date'
import { computed, onMounted, onScopeDispose, shallowRef, type CSSProperties } from 'vue'

const props = defineProps<{ showDate: boolean; timezone?: string; clockAppearance?: CSSProperties }>()
const now = shallowRef(new Date())
const time = computed(() => formatClock(now.value, { timeZone: props.timezone }))
const date = computed(() => formatDay(now.value, { timeZone: props.timezone }))
let interval: number | undefined
onMounted(() => { interval = window.setInterval(() => { now.value = new Date() }, 1000) })
onScopeDispose(() => window.clearInterval(interval))
</script>
