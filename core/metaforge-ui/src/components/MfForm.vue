<script setup lang="ts">
import { computed, provide, ref } from 'vue'

export interface MfFormRule {
    required?: boolean
    /** `type: 'array'` makes `required` reject empty arrays. */
    type?: 'string' | 'number' | 'array'
    message?: string
    min?: number
    max?: number
    pattern?: RegExp
    /** return true = valid, string = error message */
    validator?: (value: unknown, model: Record<string, unknown>) => boolean | string
}

export type MfFormRules = Record<string, MfFormRule[]>

interface FieldRegistration {
    name: string
    rules: () => MfFormRule[]
    value: () => unknown
    setError: (message: string | undefined) => void
    validate: () => string | undefined
}

const props = withDefaults(defineProps<{
    model?: Record<string, unknown>
    layout?: 'vertical' | 'horizontal'
    labelWidth?: string
    disabled?: boolean
}>(), {
    model: () => ({}),
    layout: 'vertical',
    labelWidth: '120px',
    disabled: false,
})

const emit = defineEmits<{
    finish: [model: Record<string, unknown>]
    finishFailed: [error: { values: Record<string, unknown>, errorFields: Array<{ name: string, errors: string[] }> }]
    submit: [event: Event]
}>()

const fields = ref<FieldRegistration[]>([])

provide('mfForm', {
    register(field: FieldRegistration): void {
        if (!fields.value.some(f => f.name === field.name)) {
            fields.value.push(field)
        }
    },
    unregister(name: string): void {
        fields.value = fields.value.filter(f => f.name !== name)
    },
    model: computed(() => props.model),
    disabled: computed(() => props.disabled),
    layout: computed(() => props.layout),
    labelWidth: computed(() => props.labelWidth),
})

function validateField(field: FieldRegistration, model: Record<string, unknown>): string | undefined {
    const value = field.value()
    for (const rule of field.rules()) {
        if (rule.required && (value === undefined || value === null || value === ''
            || (rule.type === 'array' && Array.isArray(value) && value.length === 0))) {
            return rule.message ?? `${field.name} is required`
        }
        if (typeof value === 'string') {
            if (rule.min !== undefined && value.length < rule.min) {
                return rule.message ?? `${field.name} must be at least ${rule.min} characters`
            }
            if (rule.max !== undefined && value.length > rule.max) {
                return rule.message ?? `${field.name} must be at most ${rule.max} characters`
            }
            if (rule.pattern && !rule.pattern.test(value)) {
                return rule.message ?? `${field.name} format is invalid`
            }
        }
        if (typeof value === 'number') {
            if (rule.min !== undefined && value < rule.min) {
                return rule.message ?? `${field.name} must be >= ${rule.min}`
            }
            if (rule.max !== undefined && value > rule.max) {
                return rule.message ?? `${field.name} must be <= ${rule.max}`
            }
        }
        if (rule.validator) {
            const result = rule.validator(value, model)
            if (result === false) return rule.message ?? `${field.name} is invalid`
            if (typeof result === 'string') return result
        }
    }
    return undefined
}

async function onSubmit(event: Event): Promise<void> {
    emit('submit', event)
    const errorFields: Array<{ name: string, errors: string[] }> = []
    for (const field of fields.value) {
        const error = validateField(field, props.model)
        field.setError(error)
        if (error) {
            errorFields.push({ name: field.name, errors: [error] })
        }
    }
    if (errorFields.length === 0) {
        emit('finish', props.model)
    }
    else {
        emit('finishFailed', { values: props.model, errorFields })
    }
}

defineExpose({
    validate: async (): Promise<Record<string, unknown>> => {
        const errorFields: Array<{ name: string, errors: string[] }> = []
        for (const field of fields.value) {
            const error = validateField(field, props.model)
            field.setError(error)
            if (error) {
                errorFields.push({ name: field.name, errors: [error] })
            }
        }
        if (errorFields.length > 0) {
            throw { values: props.model, errorFields }
        }
        return props.model
    },
    resetFields: (): void => {
        for (const field of fields.value) {
            field.setError(undefined)
        }
    },
    clearValidate: (): void => {
        for (const field of fields.value) {
            field.setError(undefined)
        }
    },
})
</script>

<template>
    <form class="flex w-full flex-col gap-4" novalidate @submit.prevent="onSubmit">
        <slot />
    </form>
</template>
