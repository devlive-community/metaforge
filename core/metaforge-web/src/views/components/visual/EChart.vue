<template>
  <div class="relative min-h-0 min-w-0" :aria-busy="pending">
    <div ref="host" class="size-full" :class="{ invisible: pending }" :inert="pending"/>
    <ChartLoading v-if="pending" class="absolute inset-0 z-10 bg-surface/90"/>
    <ChartError v-else-if="failed" class="absolute inset-0"/>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useEChart, type EChartState } from '@/composables/useEChart'
import ChartLoading from './ChartLoading.vue'
import ChartError from './ChartError.vue'
const props = defineProps<EChartState>()
const emit = defineEmits<{ point: [data: unknown, name?: string]; error: [] }>()
const host = ref<HTMLElement>()
const { pending, failed, exportImage } = useEChart(host, props, {
  point: (data, name) => emit('point', data, name), error: () => emit('error'),
})
defineExpose({ exportImage, loading: pending })
</script>
