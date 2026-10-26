<script setup lang="ts">
import { computed } from 'vue'
import MfCheckbox from './MfCheckbox.vue'
import type { MfSelectOption } from './common'

const props = withDefaults(defineProps<{
    options?: (MfSelectOption | string)[] | Record<string, string>
    modelValue?: (string | number)[]
    disabled?: boolean
}>(), {
    options: () => [],
    modelValue: () => [],
    disabled: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: (string | number)[]]
    change: [value: (string | number)[]]
}>()

const normalized = computed<MfSelectOption[]>(() => {
    if (Array.isArray(props.options)) {
        return props.options.map(option =>
            typeof option === 'string' ? { label: option, value: option, disabled: false } : { disabled: false, ...option })
    }
    return Object.entries(props.options).map(([value, label]) => ({ label, value, disabled: false }))
})

function onChange(value: boolean | (string | number)[]): void {
    const next = Array.isArray(value) ? value : []
    emit('update:modelValue', next)
    emit('change', next)
}
</script>

<template>
    <div class="inline-flex flex-wrap items-center gap-x-5 gap-y-2">
        <MfCheckbox
            v-for="option in normalized"
            :key="String(option.value)"
            :model-value="modelValue"
            :value="option.value"
            :disabled="disabled || option.disabled"
            :label="option.label"
            @update:model-value="onChange"
        />
    </div>
</template>
