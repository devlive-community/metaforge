<script setup lang="ts">
import { computed } from 'vue'
import MfLoadingIcon from './MfLoadingIcon.vue'

const props = withDefaults(defineProps<{
    size?: 'xs' | 'sm' | 'md' | 'lg' | number
    label?: string
    loading?: boolean
}>(), {
    size: 'md',
    loading: true,
})

const SIZES = {
    xs: 12,
    sm: 16,
    md: 24,
    lg: 32,
} as const

const iconSize = computed(() => typeof props.size === 'number' ? props.size : SIZES[props.size])
</script>

<template>
    <span class="inline-flex items-center gap-2 text-primary" role="status" :aria-busy="loading" :aria-label="label || (loading ? 'Loading' : 'MetaForge')">
        <MfLoadingIcon :size="iconSize" :loading="loading"/>
        <span v-if="label" class="text-sm text-content-secondary">{{ label }}</span>
    </span>
</template>
