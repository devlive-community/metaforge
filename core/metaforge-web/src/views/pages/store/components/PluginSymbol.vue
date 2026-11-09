<template>
  <span class="flex shrink-0 items-center justify-center rounded-2xl font-black tracking-tight"
        :class="[sizeClass, toneClass]">
    <img v-if="plugin.logo && !failed"
         :src="plugin.logo"
         alt=""
         loading="lazy"
         decoding="async"
         class="size-full rounded-2xl object-contain p-1"
         @error="failed = true">
    <template v-else>{{ glyph }}</template>
  </span>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { StorePlugin } from '@/model/store'

defineOptions({ name: 'PluginSymbol' })

const props = withDefaults(defineProps<{
  plugin: StorePlugin
  label: string
  size?: 'sm' | 'md' | 'lg'
  official?: boolean
}>(), {
  size: 'md',
  official: false,
})

const failed = ref(false)
watch(() => props.plugin.logo, () => { failed.value = false })

const sizeClass = computed(() => ({
  sm: 'size-10 text-base',
  md: 'size-12 text-lg',
  lg: 'size-16 text-2xl',
}[props.size]))

// Token-only tones; no raw palette classes.
const toneClass = computed(() => {
  if (props.official) {
    return 'bg-primary/10 text-primary'
  }
  const type = props.plugin.type ?? ''
  if (type === 'Convert') {
    return 'bg-info-subtle text-info'
  }
  if (type === 'FileSystem' || type === 'Notify') {
    return 'bg-warning-subtle text-warning'
  }
  if (type === 'Executor' || type === 'Scheduler') {
    return 'bg-warning-subtle text-warning'
  }
  return 'bg-primary/10 text-primary'
})

const glyph = computed(() => {
  const label = props.label ?? ''
  if (label) {
    return label.slice(0, 1).toUpperCase()
  }
  return (props.plugin.key ?? '?').slice(0, 1).toUpperCase()
})
</script>
