<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    /** antd-compatible alias: `direction="vertical"` equals `vertical`. */
    direction?: 'horizontal' | 'vertical'
    vertical?: boolean
    size?: 'xs' | 'sm' | 'md' | 'lg' | number
    wrap?: boolean
    fill?: boolean
}>(), {
    direction: 'horizontal',
    vertical: false,
    size: 'md',
    wrap: true,
    fill: false,
})

const GAP = {
    xs: 'gap-1',
    sm: 'gap-2',
    md: 'gap-3',
    lg: 'gap-6',
} as const

const isVertical = computed(() => props.vertical || props.direction === 'vertical')

const classes = computed(() => clsx(
    'flex',
    isVertical.value ? 'flex-col' : clsx('items-center', props.wrap ? 'flex-wrap' : 'flex-nowrap'),
    typeof props.size === 'string' && GAP[props.size],
    props.fill && 'w-full',
))

const spacingStyle = computed(() => typeof props.size === 'number'
    ? { gap: `${Math.max(0, props.size)}px` }
    : undefined)
</script>

<template>
    <div :class="classes" :style="spacingStyle">
        <slot />
    </div>
</template>
