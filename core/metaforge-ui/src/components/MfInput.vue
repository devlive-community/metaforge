<script setup lang="ts">
import { computed, ref, useAttrs } from 'vue'
import clsx from 'clsx'
import { CONTROL_SIZES, FIELD_FOCUS_RING, TRANSITION, hasWidthClass, type MfControlSize } from './common'

const inputRef = ref<HTMLInputElement>()

const attrs = useAttrs()

const props = withDefaults(defineProps<{
    modelValue?: string | number
    type?: string
    size?: MfControlSize
    placeholder?: string
    clearable?: boolean
    showPassword?: boolean
    disabled?: boolean
    readonly?: boolean
    invalid?: boolean
    maxlength?: number
}>(), {
    modelValue: '',
    type: 'text',
    size: 'md',
    clearable: false,
    showPassword: false,
    disabled: false,
    readonly: false,
    invalid: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string]
    blur: [event: FocusEvent]
    enter: [event: KeyboardEvent]
}>()

const passwordVisible = ref(false)
const actualType = computed(() => props.showPassword || props.type === 'password'
    ? (passwordVisible.value ? 'text' : 'password')
    : props.type)

const wrapperClass = computed(() => clsx(
    // Fills its container unless the caller gives it a width.
    !hasWidthClass(attrs.class) && 'w-full',
    'mf-invalidable inline-flex items-center rounded-mf-md border bg-surface',
    CONTROL_SIZES[props.size],
    'px-0! gap-0!',
    TRANSITION,
    props.invalid ? 'border-danger' : 'border-line',
    !props.disabled && (props.invalid ? 'focus-within:ring-2 focus-within:ring-danger/15' : FIELD_FOCUS_RING),
    props.disabled && 'opacity-60 cursor-not-allowed',
))

function onInput(event: Event): void {
    emit('update:modelValue', (event.target as HTMLInputElement).value)
}

function clear(): void {
    emit('update:modelValue', '')
}

defineExpose({
    focus: (): void => inputRef.value?.focus(),
    blur: (): void => inputRef.value?.blur(),
})
</script>

<template>
    <div :class="wrapperClass">
        <span v-if="$slots.prefix" class="pl-3 inline-flex items-center text-content-tertiary shrink-0">
            <slot name="prefix" />
        </span>
        <input
            ref="inputRef"
            :value="modelValue"
            :type="actualType"
            :placeholder="placeholder"
            :disabled="disabled"
            :readonly="readonly"
            :maxlength="maxlength"
            class="h-full min-w-0 flex-1 appearance-none border-0 bg-transparent px-3 text-sm text-content placeholder:text-content-placeholder outline-none focus:ring-0"
            :class="readonly ? 'cursor-default' : ''"
            autocomplete="off"
            @input="onInput"
            @blur="emit('blur', $event)"
            @keyup.enter="emit('enter', $event)"
        >
        <button
            v-if="showPassword && !disabled"
            type="button"
            class="mr-1 inline-flex size-7 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary outline-none hover:bg-surface-hover hover:text-content focus-visible:bg-surface-hover focus-visible:text-content"
            :aria-label="passwordVisible ? 'hide password' : 'show password'"
            :aria-pressed="passwordVisible"
            @click="passwordVisible = !passwordVisible"
        >
            <svg v-if="passwordVisible" class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path d="M3 3l18 18M10.5 10.7a2.5 2.5 0 003.4 3.4M7.4 7.5C5.2 8.8 3.7 10.8 3 12c1.5 2.7 4.8 6 9 6 1.6 0 3-.4 4.3-1.1M10 6.2A9.8 9.8 0 0112 6c4.2 0 7.5 3.3 9 6-.5.9-1.4 2.1-2.6 3.2" stroke-linecap="round" />
            </svg>
            <svg v-else class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path d="M3 12c1.5-2.7 4.8-6 9-6s7.5 3.3 9 6c-1.5 2.7-4.8 6-9 6s-7.5-3.3-9-6z" />
                <circle cx="12" cy="12" r="2.5" />
            </svg>
        </button>
        <button
            v-if="clearable && modelValue && !disabled && !readonly"
            type="button"
            class="mr-1 inline-flex size-7 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary outline-none hover:bg-surface-hover hover:text-content focus-visible:bg-surface-hover focus-visible:text-content"
            aria-label="clear"
            @click="clear"
        >
            <svg class="size-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" />
            </svg>
        </button>
        <span v-if="$slots.suffix" class="pr-3 inline-flex items-center text-content-tertiary shrink-0">
            <slot name="suffix" />
        </span>
    </div>
</template>
