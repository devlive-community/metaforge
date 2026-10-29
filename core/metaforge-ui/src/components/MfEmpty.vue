<script setup lang="ts">
import { computed } from 'vue'
import MfLoadingIcon from './MfLoadingIcon.vue'

type Variant = 'data' | 'search' | 'initial'

const props = withDefaults(defineProps<{
    title?: string
    description?: string
    variant?: Variant
    size?: 'sm' | 'md'
}>(), {
    description: '',
    variant: 'data',
    size: 'md',
})

const defaults: Record<Variant, string> = {
    data: 'No data',
    search: 'No matches',
    initial: 'Ready when you are',
}
const heading = computed(() => props.title ?? defaults[props.variant])
</script>

<template>
    <div
        class="flex w-full min-w-0 flex-col items-center justify-center text-center"
        :class="size === 'sm' ? 'gap-2 px-3 py-3' : 'gap-3 px-5 py-10'"
        role="status"
    >
        <slot name="icon">
            <span class="inline-flex shrink-0 items-center justify-center rounded-mf-lg bg-primary/5 text-primary/40" :class="size === 'sm' ? 'size-12' : 'size-20'" aria-hidden="true">
                <MfLoadingIcon :size="size === 'sm' ? 32 : 56" :loading="false"/>
            </span>
        </slot>
        <div v-if="heading || $slots.title" class="max-w-full break-words text-sm font-medium text-content-secondary">
            <slot name="title">{{ heading }}</slot>
        </div>
        <div v-if="description || $slots.description" class="max-w-sm break-words text-xs leading-5 text-content-tertiary">
            <slot name="description">{{ description }}</slot>
        </div>
        <div v-if="$slots.extra" class="mt-1 flex flex-wrap items-center justify-center gap-2">
            <slot name="extra"/>
        </div>
    </div>
</template>
