<script setup lang="ts">
import { RouterLink, type RouteLocationRaw } from 'vue-router'
import { ArrowLeft } from '@lucide/vue'

defineProps<{ to: RouteLocationRaw; label: string }>()
</script>

<template>
  <RouterLink :to="to" :aria-label="label" :title="label"
              class="grid size-8 shrink-0 place-items-center rounded-mf-md text-content-secondary outline-none transition-colors hover:bg-surface-hover hover:text-primary focus-visible:ring-2 focus-visible:ring-primary/40">
    <ArrowLeft :size="16" aria-hidden="true"/>
  </RouterLink>
</template>
