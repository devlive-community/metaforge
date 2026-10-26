<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    content?: string
    placement?: 'top' | 'bottom'
}>(), {
    content: '',
    placement: 'top',
})

const classes = computed(() => clsx(
    'pointer-events-none absolute left-1/2 z-40 -translate-x-1/2 whitespace-nowrap rounded-mf-md bg-content px-2 py-1 text-xs text-content-inverse opacity-0 shadow-mf-md',
    'transition-opacity duration-(--mf-duration-base) ease-(--mf-easing)',
    'group-hover:opacity-100 group-focus-within:opacity-100',
    props.placement === 'top' ? 'bottom-full mb-1.5' : 'top-full mt-1.5',
))
</script>

<template>
    <span class="group relative inline-flex">
        <slot />
        <span role="tooltip" :class="classes">
            <slot name="content">{{ content }}</slot>
        </span>
    </span>
</template>
