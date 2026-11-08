<template>
  <div role="progressbar" :aria-label="label" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100"
       class="h-2 overflow-hidden rounded-full bg-surface-hover">
    <div class="h-full rounded-full bg-primary" :style="{ width: percent + '%', backgroundColor: color || undefined }"/>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ value: number; target: number; label: string; color?: string }>()
const percent = computed(() => Math.max(0, Math.min(100, props.target > 0 ? props.value / props.target * 100 : 0)))
</script>
