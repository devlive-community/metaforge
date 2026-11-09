<template>
  <div id="metaforge">
    <!-- Until the first page resolves (setup check, a workbench's capabilities) nothing else is on screen. -->
    <div v-if="!ready" class="flex h-dvh items-center justify-center"><MfSpinner size="lg"/></div>
    <RouterView/>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
// 主题（--mf-* token，light/dark）由 useTheme 在启动时直接写入 documentElement
import { useTheme } from '@/composables/useTheme'

useTheme()

const ready = ref(false)
useRouter().isReady().catch(() => undefined).finally(() => (ready.value = true))
</script>
