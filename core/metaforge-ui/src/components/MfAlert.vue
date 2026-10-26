<script setup lang="ts">
import { computed, ref } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

type Variant = 'info' | 'success' | 'warning' | 'danger'

const props = withDefaults(defineProps<{
    variant?: Variant
    title?: string
    closable?: boolean
    showIcon?: boolean
}>(), {
    variant: 'info',
    closable: false,
    showIcon: true,
})

const emit = defineEmits<{ close: [] }>()

const closed = ref(false)

const STYLES = {
    info: 'bg-info-subtle text-info',
    success: 'bg-success-subtle text-success',
    warning: 'bg-warning-subtle text-warning',
    danger: 'bg-danger-subtle text-danger',
} as const

const ICON_PATHS = {
    info: 'M12 16v-6M12 8h.01',
    success: 'M5 13l4 4L19 7',
    warning: 'M12 9v4M12 17h.01',
    danger: 'M6 6l12 12M18 6L6 18',
} as const

const classes = computed(() => clsx(
    'flex w-full items-start gap-3 rounded-mf-lg px-4 py-3 text-sm',
    STYLES[props.variant],
    TRANSITION,
))

function onClose(): void {
    closed.value = true
    emit('close')
}
</script>

<template>
    <div v-if="!closed" :class="classes" role="alert">
        <svg v-if="showIcon" class="mt-0.5 size-4 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <path :d="ICON_PATHS[variant]" />
        </svg>
        <div class="min-w-0 flex-1">
            <p v-if="title" class="font-medium">{{ title }}</p>
            <div class="text-content-secondary">
                <slot />
            </div>
        </div>
        <button
            v-if="closable"
            type="button"
            class="inline-flex size-5 shrink-0 items-center justify-center rounded-mf-full opacity-60 hover:opacity-100"
            aria-label="close"
            @click="onClose"
        >
            <svg class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" />
            </svg>
        </button>
    </div>
</template>
