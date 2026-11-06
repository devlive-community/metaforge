<template>
  <div class="flex h-full min-h-0 flex-col items-center justify-center gap-1">
    <time class="text-2xl font-semibold tabular-nums" :datetime="now.toISOString()">{{ time }}</time>
    <span v-if="showDate" class="text-xs text-content-secondary">{{ date }}</span>
  </div>
</template>

<script setup lang="ts">
import { formatClock, formatDay } from '@/utils/date'
import { computed, onMounted, onScopeDispose, shallowRef } from 'vue'

defineProps<{ showDate: boolean }>()
const now = shallowRef(new Date())
const time = computed(() => formatClock(now.value))
const date = computed(() => formatDay(now.value))
let interval: number | undefined
onMounted(() => { interval = window.setInterval(() => { now.value = new Date() }, 1000) })
onScopeDispose(() => window.clearInterval(interval))
</script>
