<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

type Status = 'info' | 'success' | 'warning' | 'error' | '404' | '403' | '500'

const props = withDefaults(defineProps<{
    status?: Status
    title?: string
    subTitle?: string
}>(), {
    status: 'info',
    title: '',
    subTitle: '',
})

const STYLES: Record<string, { icon: string, tone: string }> = {
    info: { icon: 'M12 16v-6M12 8h.01', tone: 'text-info' },
    success: { icon: 'M5 13l4 4L19 7', tone: 'text-success' },
    warning: { icon: 'M12 9v4M12 17h.01', tone: 'text-warning' },
    error: { icon: 'M6 6l12 12M18 6L6 18', tone: 'text-danger' },
    404: { icon: 'M9 9h.01M15 9h.01M9.5 15a3.5 3.5 0 007 0', tone: 'text-info' },
    403: { icon: 'M12 15v2M6 11a6 6 0 1112 0v4H6v-4z', tone: 'text-warning' },
    500: { icon: 'M12 9v4M12 17h.01', tone: 'text-danger' },
}

const style = computed(() => STYLES[props.status] ?? STYLES.info)
const heading = computed(() => props.title || (['404', '403', '500'].includes(props.status) ? props.status : ''))
</script>

<template>
    <div class="flex w-full flex-col items-center justify-center gap-4 px-6 py-12 text-center">
        <svg :class="clsx('size-14', style.tone)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true">
            <circle cx="12" cy="12" r="10" />
            <path :d="style.icon" stroke-linecap="round" />
        </svg>
        <div>
            <h1 class="text-2xl font-semibold text-content">{{ heading }}</h1>
            <p v-if="subTitle" class="mt-1.5 text-sm text-content-secondary">{{ subTitle }}</p>
            <div v-if="$slots.default" class="mt-2 text-sm text-content-secondary">
                <slot />
            </div>
        </div>
        <div v-if="$slots.extra" class="flex items-center gap-2">
            <slot name="extra" />
        </div>
    </div>
</template>
