<template>
  <div id="metaforge">
    <RouterView/>
  </div>
</template>

<script setup lang="ts">
// 主题（--mf-* token，light/dark）由 useTheme 在启动时直接写入 documentElement
import { useTheme } from '@/composables/useTheme'

useTheme()
</script>
