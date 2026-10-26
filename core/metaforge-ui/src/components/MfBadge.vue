<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

type Variant = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const props = withDefaults(defineProps<{
    variant?: Variant
    count?: number | string
    max?: number
    dot?: boolean
    showZero?: boolean
}>(), {
    variant: 'danger',
    dot: false,
    showZero: false,
})

const VARIANTS = {
    primary: 'bg-primary',
    success: 'bg-success',
    warning: 'bg-warning',
    danger: 'bg-danger',
    info: 'bg-info',
} as const

const displayCount = computed(() => {
    if (typeof props.count === 'number' && props.max && props.count > props.max) {
        return `${props.max}+`
    }
    return props.count
})

const shouldShow = computed(() => props.dot
    || displayCount.value !== undefined
        && (displayCount.value !== 0 || props.showZero))

const classes = computed(() => clsx(
    'inline-flex items-center justify-center rounded-mf-full text-on-primary',
    props.dot ? 'size-2' : 'min-w-4.5 px-1.5 py-0.5 text-[10px] leading-4 font-semibold',
    VARIANTS[props.variant],
))
</script>

<template>
    <span class="relative inline-flex">
        <slot />
        <span v-if="shouldShow" :class="classes">
            <template v-if="!dot">{{ displayCount }}</template>
        </span>
    </span>
</template>
