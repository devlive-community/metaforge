<template>
  <div id="metaforge">
    <PublicScreen :token="token" :embed="embed"/>
  </div>
</template>

<script setup lang="ts">
// Root of a data screen behind a public link: the theme and the screen, without the app router or layout.
import { useTheme } from '@/composables/useTheme'
import PublicScreen from './PublicScreen.vue'

defineOptions({ name: 'PublicScreenApp' })
defineProps<{ token: string; embed: boolean }>()

useTheme()
</script>
