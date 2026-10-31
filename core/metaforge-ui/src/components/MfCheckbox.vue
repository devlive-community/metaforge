<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

const props = withDefaults(defineProps<{
    modelValue?: boolean | (string | number)[]
    value?: string | number
    label?: string
    disabled?: boolean
    indeterminate?: boolean
}>(), {
    modelValue: false,
    disabled: false,
    indeterminate: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: boolean | (string | number)[]]
}>()

const group = computed((): (string | number)[] => {
    return Array.isArray(props.modelValue) ? [...props.modelValue] : []
})

const checked = computed(() => {
    if (props.value !== undefined && Array.isArray(props.modelValue)) {
        return group.value.some(v => v === props.value)
    }
    return props.modelValue === true
})

function toggle(): void {
    if (props.disabled) return
    if (props.value !== undefined && Array.isArray(props.modelValue)) {
        const value = props.value as string | number
        const next = checked.value
            ? group.value.filter(v => v !== value)
            : [...group.value, value]
        emit('update:modelValue', next)
    }
    else {
        emit('update:modelValue', !props.modelValue)
    }
}
</script>

<template>
    <label
        class="relative inline-flex cursor-pointer items-center gap-2 text-sm text-content"
        :class="disabled && 'cursor-not-allowed opacity-60'"
    >
        <span
            class="inline-flex size-4.5 items-center justify-center rounded-mf-sm border shadow-mf-xs"
            :class="clsx(
                TRANSITION,
                checked || indeterminate
                    ? 'border-primary bg-primary text-on-primary'
                    : 'border-line-strong bg-surface',
            )"
        >
            <svg v-if="indeterminate" class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
                <path d="M5 12h14" />
            </svg>
            <svg v-else-if="checked" class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true">
                <path d="M5 13l4 4L19 7" />
            </svg>
        </span>
        <input
            type="checkbox"
            class="sr-only"
            :checked="checked"
            :indeterminate="indeterminate"
            :aria-checked="indeterminate ? 'mixed' : checked"
            :disabled="disabled"
            @change="toggle"
        >
        <span v-if="label">{{ label }}</span>
        <slot v-else />
    </label>
</template>
