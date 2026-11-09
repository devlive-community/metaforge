<template>
  <button type="button" class="flex w-full min-w-0 items-start gap-3 rounded-mf-lg border p-4 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-60"
          :class="selected ? 'border-primary bg-primary/5' : 'border-line hover:bg-surface-hover'" :aria-pressed="selected" :disabled="disabled" @click="emit('select')">
    <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><component :is="icon" :size="18"/></span>
    <span class="flex min-w-0 flex-1 flex-col">
      <span class="font-semibold text-content">{{ title }}</span>
      <span class="mt-0.5 text-xs text-content-tertiary">{{ description }}</span>
    </span>
    <Check v-if="selected" :size="16" class="shrink-0 text-primary"/>
  </button>
</template>

<script setup lang="ts">
import { Check } from '@lucide/vue'
import type { Component } from 'vue'

/** A selectable card: icon, title and description, checked when selected. */
defineProps<{ icon: Component; title: string; description: string; selected?: boolean; disabled?: boolean }>()
const emit = defineEmits<{ select: [] }>()
</script>
