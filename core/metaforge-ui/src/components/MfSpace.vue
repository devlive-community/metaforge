<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    /** antd-compatible alias: `direction="vertical"` equals `vertical`. */
    direction?: 'horizontal' | 'vertical'
    vertical?: boolean
    size?: 'xs' | 'sm' | 'md' | 'lg'
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
    isVertical.value ? 'flex-col' : 'items-center flex-wrap',
    GAP[props.size],
    props.fill && 'w-full',
))
</script>

<template>
    <div :class="classes">
        <slot />
    </div>
</template>
