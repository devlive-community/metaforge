<script setup lang="ts">
import clsx from 'clsx'
import { FOCUS_RING, TRANSITION } from './common'

withDefaults(defineProps<{
    modelValue?: string
    placeholder?: string
    rows?: number
    disabled?: boolean
    readonly?: boolean
    invalid?: boolean
    maxlength?: number
}>(), {
    modelValue: '',
    rows: 4,
    disabled: false,
    readonly: false,
    invalid: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string]
}>()
</script>

<template>
    <textarea
        :value="modelValue"
        :placeholder="placeholder"
        :rows="rows"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        class="w-full rounded-mf-md border bg-surface px-3 py-2 text-sm text-content placeholder:text-content-tertiary outline-none resize-y"
        :class="clsx(TRANSITION, FOCUS_RING, invalid ? 'border-danger' : 'border-line', disabled && 'opacity-60 cursor-not-allowed')"
        @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
</template>
