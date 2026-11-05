<template><span class="tabular-nums">{{ value === null ? '—' : prefix + formatted + suffix }}</span></template>
<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
const props = defineProps<{ value: number | null; decimals: number; prefix?: string; suffix?: string }>()
const { locale } = useI18n()
const displayed = ref(0)
let frame: number | undefined
watch(() => props.value, value => {
  if (frame !== undefined) cancelAnimationFrame(frame)
  if (value === null) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches || document.hidden) { displayed.value = value; return }
  const initial = displayed.value
  let start: number | undefined
  function tick(now: number) {
    start ??= now
    const progress = Math.min(1, (now - start) / 800)
    displayed.value = initial + (value! - initial) * (1 - (1 - progress) ** 3)
    if (progress < 1) frame = requestAnimationFrame(tick)
  }
  frame = requestAnimationFrame(tick)
}, { immediate: true })
onScopeDispose(() => { if (frame !== undefined) cancelAnimationFrame(frame) })
const formatted = computed(() => new Intl.NumberFormat(locale.value, { maximumFractionDigits: props.decimals }).format(displayed.value))
</script>
