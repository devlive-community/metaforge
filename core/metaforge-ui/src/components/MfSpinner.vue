<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    size?: 'xs' | 'sm' | 'md' | 'lg'
    label?: string
}>(), {
    size: 'md',
})

const SIZES = {
    xs: 'size-3',
    sm: 'size-4',
    md: 'size-6',
    lg: 'size-8',
} as const

const classes = computed(() => clsx(
    'inline-block animate-spin text-primary',
    SIZES[props.size],
))
</script>

<template>
    <span class="inline-flex items-center gap-2" role="status" :aria-label="label">
        <svg :class="classes" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
        </svg>
        <span v-if="label" class="text-sm text-content-secondary">{{ label }}</span>
    </span>
</template>
