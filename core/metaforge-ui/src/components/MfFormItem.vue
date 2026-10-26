<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { MfFormRule } from './MfForm.vue'

const props = withDefaults(defineProps<{
    name: string
    label?: string
    rules?: MfFormRule[]
    requiredMark?: boolean
}>(), {
    label: '',
    rules: () => [],
    requiredMark: undefined,
})

interface FormContext {
    register: (field: { name: string, rules: () => MfFormRule[], value: () => unknown, setError: (message: string | undefined) => void, validate: () => string | undefined }) => void
    unregister: (name: string) => void
    disabled: { value: boolean }
    layout: { value: 'vertical' | 'horizontal' }
    labelWidth: { value: string }
    model: { value: Record<string, unknown> }
}

const form = inject<FormContext | null>('mfForm', null)
const error = ref<string | undefined>()

const value = computed(() => form?.model.value?.[props.name])

watch(value, () => {
    // clear error as soon as the user edits the field again
    if (error.value !== undefined) {
        error.value = undefined
    }
})

function validate(): string | undefined {
    if (!form) return undefined
    for (const rule of props.rules) {
        const v = value.value
        if (rule.required && (v === undefined || v === null || v === '')) {
            error.value = rule.message ?? `${props.name} is required`
            return error.value
        }
        if (typeof v === 'string') {
            if (rule.min !== undefined && v.length < rule.min) {
                error.value = rule.message ?? `${props.name} must be at least ${rule.min} characters`
                return error.value
            }
            if (rule.max !== undefined && v.length > rule.max) {
                error.value = rule.message ?? `${props.name} must be at most ${rule.max} characters`
                return error.value
            }
            if (rule.pattern && !rule.pattern.test(v)) {
                error.value = rule.message ?? `${props.name} format is invalid`
                return error.value
            }
        }
        if (typeof v === 'number') {
            if (rule.min !== undefined && v < rule.min) {
                error.value = rule.message ?? `${props.name} must be >= ${rule.min}`
                return error.value
            }
            if (rule.max !== undefined && v > rule.max) {
                error.value = rule.message ?? `${props.name} must be <= ${rule.max}`
                return error.value
            }
        }
        if (rule.validator) {
            const result = rule.validator(v, form.model.value)
            if (result === false) {
                error.value = rule.message ?? `${props.name} is invalid`
                return error.value
            }
            if (typeof result === 'string') {
                error.value = result
                return error.value
            }
        }
    }
    error.value = undefined
    return undefined
}

onMounted(() => {
    form?.register({
        name: props.name,
        rules: () => props.rules,
        value: () => value.value,
        setError: (message: string | undefined) => { error.value = message },
        validate,
    })
})

onBeforeUnmount(() => form?.unregister(props.name))

const required = computed(() => props.rules.some(rule => rule.required))
const showRequired = computed(() => props.requiredMark ?? required.value)
const horizontal = computed(() => form?.layout.value === 'horizontal')

const labelClasses = computed(() => [
    'shrink-0 text-sm font-medium text-content-secondary',
    horizontal.value ? 'pt-2' : '',
    showRequired.value ? "after:ml-0.5 after:text-danger after:content-['*']" : '',
])
</script>

<template>
    <div class="flex w-full" :class="horizontal ? 'flex-row items-start gap-3' : 'flex-col gap-1.5'">
        <label v-if="label" :class="labelClasses">
            {{ label }}
        </label>
        <div class="min-w-0 flex-1">
            <div class="flex w-full" :class="error ? ' [&_.mf-invalidable]:border-danger' : ''">
                <slot />
            </div>
            <p v-if="error" class="mt-1 text-xs text-danger">
                {{ error }}
            </p>
        </div>
    </div>
</template>
