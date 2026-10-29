<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import MfLoadingIcon from './MfLoadingIcon.vue'
import { CONTROL_SIZES, DISABLED, FOCUS_RING, TRANSITION, type MfControlSize } from './common'

type Variant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'link'

const props = withDefaults(defineProps<{
    variant?: Variant
    size?: MfControlSize
    type?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
    block?: boolean
    shape?: 'default' | 'circle'
}>(), {
    variant: 'secondary',
    size: 'md',
    type: 'button',
    loading: false,
    disabled: false,
    block: false,
    shape: 'default',
})

const VARIANTS: Record<Variant, string> = {
    primary: 'bg-primary text-on-primary hover:bg-primary-hover active:bg-primary-active shadow-mf-sm',
    secondary: 'bg-surface text-content border border-line hover:bg-surface-hover hover:border-line-strong shadow-mf-sm',
    outline: 'bg-transparent text-primary border border-primary hover:bg-primary/10',
    ghost: 'bg-transparent text-content-secondary hover:bg-surface-hover hover:text-content',
    danger: 'bg-danger text-on-primary hover:opacity-90 shadow-mf-sm',
    link: 'bg-transparent text-primary underline-offset-4 hover:underline px-1! h-auto! py-0.5!',
}

const classes = computed(() => clsx(
    'inline-flex items-center justify-center font-medium rounded-mf-md select-none cursor-pointer',
    CONTROL_SIZES[props.size],
    VARIANTS[props.variant],
    FOCUS_RING,
    TRANSITION,
    DISABLED,
    { 'w-full': props.block, 'opacity-50 pointer-events-none': props.loading },
    { 'rounded-mf-full! px-0! aspect-square': props.shape === 'circle' },
))
</script>

<template>
    <button :type="type" :class="classes" :disabled="disabled || loading" :aria-busy="loading || undefined">
        <MfLoadingIcon v-if="loading" :size="16" class="-ml-0.5 shrink-0"/>
        <!-- While loading the shared icon replaces any slot icon: exactly one glyph shows. -->
        <span v-if="loading" class="inline-flex items-center [&_svg]:hidden"><slot /></span>
        <slot v-else />
    </button>
</template>
