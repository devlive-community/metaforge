<script setup lang="ts">
import { computed, inject, ref, type ComputedRef, type Ref } from 'vue'
import clsx from 'clsx'
import { TRANSITION } from './common'

interface RadioGroupContext {
    modelValue: ComputedRef<string | number>
    disabled: Ref<boolean>
    name: Ref<string | undefined>
    variant: ComputedRef<'default' | 'button'>
    size: ComputedRef<'sm' | 'md' | 'lg'>
    select: (value: string | number) => void
}

const props = withDefaults(defineProps<{
    value: string | number
    label?: string
    disabled?: boolean
}>(), {
    disabled: false,
})

const group = inject<RadioGroupContext | null>('mfRadioGroup', null)
const inputRef = ref<HTMLInputElement>()

const checked = computed(() => group ? group.modelValue.value === props.value : false)
const isDisabled = computed(() => props.disabled || (group?.disabled.value ?? false))
const isButton = computed(() => group?.variant.value === 'button')

const SIZE_PAD = {
    sm: 'px-2.5 py-1 text-xs',
    md: 'px-3.5 py-1.5 text-sm',
    lg: 'px-5 py-2 text-sm',
} as const

const labelClass = computed(() => {
    if (isButton.value) {
        return clsx(
            'inline-flex cursor-pointer items-center gap-1.5 rounded-mf-sm font-medium',
            SIZE_PAD[group?.size.value ?? 'md'],
            TRANSITION,
            isDisabled.value && 'cursor-not-allowed opacity-40',
            checked.value
                ? 'bg-primary text-on-primary shadow-mf-sm'
                : 'text-content-secondary hover:text-content',
        )
    }
    return clsx(
        'inline-flex cursor-pointer items-center gap-2 text-sm text-content',
        isDisabled.value && 'cursor-not-allowed opacity-60',
    )
})

function activate(): void {
    if (!isDisabled.value) {
        group?.select(props.value)
    }
}
</script>

<template>
    <label :class="labelClass" @click.prevent="activate">
        <template v-if="!isButton">
            <span
                class="inline-flex size-4.5 items-center justify-center rounded-mf-full border shadow-mf-xs"
                :class="clsx(
                    TRANSITION,
                    checked ? 'border-primary' : 'border-line-strong bg-surface',
                )"
            >
                <span
                    class="size-2 rounded-mf-full bg-primary transition-[transform,opacity] duration-(--mf-duration-fast) ease-(--mf-easing)"
                    :class="checked ? 'scale-100 opacity-100' : 'scale-0 opacity-0'"
                />
            </span>
        </template>
        <input
            ref="inputRef"
            type="radio"
            class="sr-only"
            :checked="checked"
            :disabled="isDisabled"
            :name="group?.name.value"
        >
        <span v-if="label">{{ label }}</span>
        <slot v-else />
    </label>
</template>
