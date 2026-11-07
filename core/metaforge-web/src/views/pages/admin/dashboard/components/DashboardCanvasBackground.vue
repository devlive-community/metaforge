<template>
  <div v-if="url" class="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden="true">
    <div class="size-full" :style="style"/>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { dashboardResourceUrl, type DashboardBoard } from '@/model/dashboard-layout'
const props = defineProps<{ board: DashboardBoard }>()
const url = computed(() => dashboardResourceUrl(props.board.backgroundImage))
const style = computed(() => ({ backgroundImage: `url(${JSON.stringify(url.value)})`,
  backgroundSize: props.board.backgroundFit === 'stretch' ? '100% 100%' : props.board.backgroundFit === 'tile' ? 'auto' : props.board.backgroundFit,
  backgroundRepeat: props.board.backgroundFit === 'tile' ? 'repeat' : 'no-repeat',
  backgroundPosition: props.board.backgroundPosition === 'top' ? 'center top' : props.board.backgroundPosition === 'bottom' ? 'center bottom' : 'center',
  opacity: props.board.backgroundOpacity / 100, filter: props.board.backgroundBlur ? `blur(${props.board.backgroundBlur}px)` : undefined }))
</script>
