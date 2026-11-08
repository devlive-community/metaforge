<template>
  <div class="relative flex h-8 overflow-hidden rounded-mf-sm" role="meter" :aria-label="label" :aria-valuenow="value" aria-valuemin="0" :aria-valuemax="maximum">
    <span class="w-1/2 bg-primary/8"/><span class="w-1/3 bg-primary/15"/><span class="flex-1 bg-primary/25"/>
    <span class="absolute top-2 bottom-2 left-0 bg-primary" :style="{ width: Math.max(0, value) / maximum * 100 + '%', backgroundColor: color || undefined }"/>
    <span class="absolute inset-y-1 w-0.5 bg-content" :style="{ left: target / maximum * 100 + '%' }" :title="String(target)"/>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ value: number; target: number; label: string; color?: string }>()
const maximum = computed(() => Math.max(1, props.target * 1.2, props.value * 1.1))
</script>
