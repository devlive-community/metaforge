<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import { CONTROL_SIZES, FIELD_FOCUS_RING, TRANSITION, type MfControlSize } from './common'

const props = withDefaults(defineProps<{
    modelValue?: number | string
    min?: number
    max?: number
    step?: number
    precision?: number
    size?: MfControlSize
    placeholder?: string
    disabled?: boolean
    invalid?: boolean
    controls?: boolean
}>(), {
    modelValue: undefined,
    step: 1,
    size: 'md',
    disabled: false,
    invalid: false,
    controls: true,
})

const emit = defineEmits<{
    'update:modelValue': [value: number | undefined]
    change: [value: number | undefined]
}>()

function clamp(value: number): number {
    let v = value
    if (props.min !== undefined) v = Math.max(props.min, v)
    if (props.max !== undefined) v = Math.min(props.max, v)
    return v
}

function emitValue(value: number | undefined): void {
    emit('update:modelValue', value)
    emit('change', value)
}

function fromRaw(raw: string): number | undefined {
    if (raw === '' || raw === '-') return undefined
    const parsed = Number(raw)
    if (Number.isNaN(parsed)) return undefined
    return clamp(parsed)
}

function onInput(event: Event): void {
    const raw = (event.target as HTMLInputElement).value
    const value = fromRaw(raw)
    if (value !== undefined && props.precision !== undefined) {
        emitValue(Number(value.toFixed(props.precision)))
    }
    else {
        emitValue(value)
    }
}

function stepBy(delta: number): void {
    if (props.disabled) return
    const base = typeof props.modelValue === 'number' ? props.modelValue : (props.min ?? 0)
    let next = clamp(base + delta * props.step)
    if (props.precision !== undefined) next = Number(next.toFixed(props.precision))
    emitValue(next)
}

const display = computed(() => (props.modelValue === undefined || props.modelValue === '' ? '' : String(props.modelValue)))

const wrapperClass = computed(() => clsx(
    'mf-invalidable inline-flex items-center w-full rounded-mf-md border bg-surface overflow-hidden',
    props.invalid ? 'border-danger' : 'border-line',
    !props.disabled && (props.invalid ? 'focus-within:ring-2 focus-within:ring-danger/15' : FIELD_FOCUS_RING),
    CONTROL_SIZES[props.size].split(' ').filter(c => !c.startsWith('px-') && !c.startsWith('gap-')).join(' '),
    'px-0!',
    TRANSITION,
    props.disabled && 'opacity-60 cursor-not-allowed',
))
</script>

<template>
    <div :class="wrapperClass">
        <button
            v-if="controls"
            type="button"
            class="flex h-full w-8 shrink-0 items-center justify-center border-r border-line text-content-tertiary hover:bg-surface-hover hover:text-content disabled:pointer-events-none disabled:opacity-40"
            :disabled="disabled"
            aria-label="decrease"
            @click="stepBy(-1)"
        >
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M5 12h14" /></svg>
        </button>
        <input
            :value="display"
            type="text"
            inputmode="decimal"
            :placeholder="placeholder"
            :disabled="disabled"
            class="h-full min-w-0 flex-1 appearance-none border-0 bg-transparent text-sm text-content placeholder:text-content-placeholder outline-none focus:ring-0"
            :class="controls ? 'px-2 text-center' : 'px-3 text-left'"
            @input="onInput"
            @change="onInput"
        >
        <button
            v-if="controls"
            type="button"
            class="flex h-full w-8 shrink-0 items-center justify-center border-l border-line text-content-tertiary hover:bg-surface-hover hover:text-content disabled:pointer-events-none disabled:opacity-40"
            :disabled="disabled"
            aria-label="increase"
            @click="stepBy(1)"
        >
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>
        </button>
    </div>
</template>
