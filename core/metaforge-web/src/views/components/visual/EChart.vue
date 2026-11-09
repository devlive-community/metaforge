<template>
  <div class="relative min-h-0 min-w-0" :aria-busy="pending">
    <div ref="host" class="size-full" :class="{ invisible: pending }" :inert="pending"/>
    <ChartLoading v-if="pending" class="absolute inset-0 z-10 bg-surface/90"/>
    <ChartError v-else-if="failed" class="absolute inset-0"/>
  </div>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import { useEChart, type EChartState } from '@/composables/useEChart'
import { chartCardLayoutContext } from '@/composables/chartCardLayoutContext'
import ChartLoading from './ChartLoading.vue'
import ChartError from './ChartError.vue'
const props = withDefaults(defineProps<EChartState>(), { fit: undefined })
const emit = defineEmits<{ point: [data: unknown, name?: string]; error: [] }>()
const host = ref<HTMLElement>()
const inheritedFit = inject(chartCardLayoutContext, false)
const { pending, failed, exportImage } = useEChart(host, props, {
  point: (data, name) => emit('point', data, name), error: () => emit('error'),
}, () => props.fit ?? inheritedFit)
defineExpose({ exportImage, loading: pending })
</script>
