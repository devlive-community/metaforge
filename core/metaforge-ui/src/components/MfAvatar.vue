<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    src?: string
    alt?: string
    /** Named preset or an explicit pixel size (antd compatibility). */
    size?: 'xs' | 'sm' | 'md' | 'lg' | number
    shape?: 'circle' | 'square'
}>(), {
    size: 'md',
    shape: 'circle',
})

const SIZES = {
    xs: 'size-6 text-xs',
    sm: 'size-8 text-sm',
    md: 'size-10 text-base',
    lg: 'size-14 text-xl',
} as const

const failed = ref(false)
watch(() => props.src, () => {
    failed.value = false
})

const sizeStyle = computed(() => (typeof props.size === 'number'
    ? { width: `${props.size}px`, height: `${props.size}px` }
    : {}))

const classes = computed(() => clsx(
    'inline-flex shrink-0 items-center justify-center bg-primary/15 font-medium text-primary select-none overflow-hidden',
    typeof props.size === 'number' ? 'text-base' : SIZES[props.size],
    props.shape === 'circle' ? 'rounded-mf-full' : 'rounded-mf-lg',
))
</script>

<template>
    <span :class="classes" :style="sizeStyle">
        <img v-if="src && !failed" :src="src" :alt="alt ?? ''" class="size-full object-cover" @error="failed = true">
        <slot v-else />
    </span>
</template>
