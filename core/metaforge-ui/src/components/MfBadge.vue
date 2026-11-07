<script setup lang="ts">
import { computed, useSlots } from 'vue'
import clsx from 'clsx'

type Variant = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const props = withDefaults(defineProps<{
    variant?: Variant
    count?: number | string
    max?: number
    dot?: boolean
    showZero?: boolean
    /**
     * Inset of the badge's centre from the wrapped content's top-right corner, as [right, top] in px.
     * Use it when the content has padding around its glyph, e.g. an icon button.
     */
    offset?: [number, number]
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

// Wrapping content (an icon, a button) pins the badge to its top-right corner; on its own it is inline.
const slots = useSlots()
const attached = computed(() => !!slots.default)

const classes = computed(() => clsx(
    'inline-flex items-center justify-center rounded-mf-full text-on-primary',
    props.dot ? 'size-2' : attached.value ? 'h-4 min-w-4 px-1 text-[10px] leading-none font-semibold tabular-nums' : 'min-w-4.5 px-1.5 py-0.5 text-[10px] leading-4 font-semibold',
    attached.value && 'pointer-events-none absolute translate-x-1/2 -translate-y-1/2 ring-2 ring-surface',
    VARIANTS[props.variant],
))
const position = computed(() => attached.value ? { right: `${props.offset?.[0] ?? 0}px`, top: `${props.offset?.[1] ?? 0}px` } : undefined)
</script>

<template>
    <span class="relative inline-flex">
        <slot />
        <span v-if="shouldShow" :class="classes" :style="position">
            <template v-if="!dot">{{ displayCount }}</template>
        </span>
    </span>
</template>
