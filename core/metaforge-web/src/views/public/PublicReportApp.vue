<template>
  <div id="metaforge">
    <PublicReport :token="token" :embed="embed"/>
  </div>
</template>

<script setup lang="ts">
// Root of a shared report page: the theme and the public report, without the app router or layout.
import { useTheme } from '@/composables/useTheme'
import PublicReport from './PublicReport.vue'

defineOptions({ name: 'PublicReportApp' })
defineProps<{ token: string; embed: boolean }>()

useTheme()
</script>
