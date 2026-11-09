<script setup lang="ts">
// Consume async-component errors without exposing transport details in the page.
defineProps<{ error?: unknown }>()
</script>
<template>
  <div class="flex size-full min-h-0 min-w-0 items-center justify-center p-3">
    <MfAlert variant="danger" :title="$t('queryChart.renderError')"/>
  </div>
</template>
