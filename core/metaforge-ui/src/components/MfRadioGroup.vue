<script setup lang="ts">
import { computed, provide, toRef } from 'vue'

const props = withDefaults(defineProps<{
    modelValue?: string | number
    disabled?: boolean
    name?: string
    /** `button` renders radios as a segmented control (a-radio-button style). */
    variant?: 'default' | 'button'
    size?: 'sm' | 'md' | 'lg'
}>(), {
    modelValue: '',
    disabled: false,
    variant: 'default',
    size: 'md',
})

const emit = defineEmits<{
    'update:modelValue': [value: string | number]
    change: [value: string | number]
}>()

provide('mfRadioGroup', {
    modelValue: computed(() => props.modelValue),
    disabled: toRef(props, 'disabled'),
    name: toRef(props, 'name'),
    variant: computed(() => props.variant),
    size: computed(() => props.size),
    select(value: string | number): void {
        if (value !== props.modelValue) {
            emit('update:modelValue', value)
            emit('change', value)
        }
    },
})
</script>

<template>
    <div
        class="inline-flex flex-wrap items-center"
        :class="variant === 'button' ? 'gap-0 rounded-mf-md border border-line bg-surface p-0.5' : 'gap-4'"
        role="radiogroup"
    >
        <slot />
    </div>
</template>
