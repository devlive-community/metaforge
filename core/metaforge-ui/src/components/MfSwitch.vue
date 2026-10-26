<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

const props = withDefaults(defineProps<{
    modelValue?: boolean | string | number
    disabled?: boolean
    size?: 'sm' | 'md'
    /** antd compatibility: map the checked/unchecked state to custom values. */
    checkedValue?: boolean | string | number
    uncheckedValue?: boolean | string | number
}>(), {
    modelValue: false,
    disabled: false,
    size: 'md',
    checkedValue: true,
    uncheckedValue: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: boolean | string | number]
    change: [value: boolean | string | number]
}>()

const isChecked = computed(() => props.modelValue === props.checkedValue)

const track = computed(() => clsx(
    'relative inline-flex shrink-0 cursor-pointer items-center rounded-mf-full border-2 border-transparent',
    TRANSITION,
    props.size === 'sm' ? 'h-5 w-9' : 'h-6 w-11',
    isChecked.value ? 'bg-primary' : 'bg-surface-active',
    props.disabled && 'cursor-not-allowed opacity-60',
))

const thumb = computed(() => clsx(
    'inline-block translate-x-0 rounded-mf-full bg-white shadow-mf-sm transition-transform duration-(--mf-duration-base) ease-(--mf-easing)',
    props.size === 'sm' ? 'size-3.5' : 'size-4.5',
    isChecked.value && (props.size === 'sm' ? 'translate-x-4' : 'translate-x-5'),
))

function toggle(): void {
    if (!props.disabled) {
        const next = isChecked.value ? props.uncheckedValue : props.checkedValue
        emit('update:modelValue', next)
        emit('change', next)
    }
}
</script>

<template>
    <button type="button" role="switch" :aria-checked="isChecked" :class="track" :disabled="disabled" @click="toggle">
        <span :class="thumb" />
    </button>
</template>
