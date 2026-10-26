<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    title?: string
    padding?: 'none' | 'sm' | 'md' | 'lg'
    shadow?: 'none' | 'sm' | 'md' | 'lg'
    bordered?: boolean
    hoverable?: boolean
}>(), {
    padding: 'md',
    shadow: 'sm',
    bordered: true,
    hoverable: false,
})

const PADDING = {
    none: '',
    sm: 'p-3',
    md: 'p-4',
    lg: 'p-6',
} as const

const SHADOW = {
    none: '',
    sm: 'shadow-mf-sm',
    md: 'shadow-mf-md',
    lg: 'shadow-mf-lg',
} as const

const classes = computed(() => clsx(
    'rounded-mf-lg bg-surface',
    props.bordered && 'border border-line',
    SHADOW[props.shadow],
    props.hoverable && 'transition-shadow duration-(--mf-duration-base) ease-(--mf-easing) hover:shadow-mf-md',
))

const bodyClass = computed(() => PADDING[props.padding])
</script>

<template>
    <section :class="classes">
        <header v-if="title || $slots.header || $slots.extra" class="flex items-center justify-between border-b border-line px-4 py-3">
            <div class="text-base font-medium text-content">
                <slot name="header">{{ title }}</slot>
            </div>
            <div v-if="$slots.extra" class="text-sm text-content-secondary">
                <slot name="extra" />
            </div>
        </header>
        <div :class="bodyClass">
            <slot />
        </div>
        <footer v-if="$slots.footer" class="border-t border-line px-4 py-3">
            <slot name="footer" />
        </footer>
    </section>
</template>
