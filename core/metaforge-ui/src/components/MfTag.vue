<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

type Variant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info'

const props = withDefaults(defineProps<{
    variant?: Variant
    size?: 'sm' | 'md'
    closable?: boolean
    /** Custom color (antd compatibility); overrides the variant palette. */
    color?: string
}>(), {
    variant: 'default',
    size: 'md',
    closable: false,
})

const emit = defineEmits<{ close: [] }>()

const VARIANTS: Record<Variant, string> = {
    default: 'bg-surface-hover text-content-secondary border-line',
    primary: 'bg-primary/10 text-primary border-primary/30',
    success: 'bg-success-subtle text-success border-success/30',
    warning: 'bg-warning-subtle text-warning border-warning/30',
    danger: 'bg-danger-subtle text-danger border-danger/30',
    info: 'bg-info-subtle text-info border-info/30',
}

const classes = computed(() => clsx(
    'inline-flex items-center gap-1 rounded-mf-md border font-medium',
    props.size === 'sm' ? 'px-1.5 py-0.5 text-xs' : 'px-2 py-0.5 text-sm',
    props.color ? '' : VARIANTS[props.variant],
    TRANSITION,
))

const colorStyle = computed(() => props.color
    ? {
        color: props.color,
        borderColor: props.color,
        backgroundColor: `color-mix(in srgb, ${props.color} 12%, transparent)`,
    }
    : undefined)
</script>

<template>
    <span :class="classes" :style="colorStyle">
        <slot />
        <button
            v-if="closable"
            type="button"
            class="-mr-0.5 inline-flex size-3.5 items-center justify-center rounded-mf-full opacity-60 hover:opacity-100"
            aria-label="close"
            @click="emit('close')"
        >
            <svg class="size-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" />
            </svg>
        </button>
    </span>
</template>
