<template>
  <div class="flex h-full min-h-0 flex-col items-center justify-center gap-1">
    <time class="text-2xl font-semibold tabular-nums" :datetime="now.toISOString()">{{ time }}</time>
    <span v-if="showDate" class="text-xs text-content-secondary">{{ date }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onScopeDispose, shallowRef } from 'vue'
import { useI18n } from 'vue-i18n'

defineProps<{ showDate: boolean }>()
const { locale } = useI18n()
const now = shallowRef(new Date())
const timeFormat = computed(() => new Intl.DateTimeFormat(locale.value, { timeStyle: 'medium' }))
const dateFormat = computed(() => new Intl.DateTimeFormat(locale.value, { dateStyle: 'long' }))
const time = computed(() => timeFormat.value.format(now.value))
const date = computed(() => dateFormat.value.format(now.value))
let interval: number | undefined
onMounted(() => { interval = window.setInterval(() => { now.value = new Date() }, 1000) })
onScopeDispose(() => window.clearInterval(interval))
</script>
