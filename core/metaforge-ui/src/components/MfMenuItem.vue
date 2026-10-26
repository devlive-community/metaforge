<script setup lang="ts">
import { computed, inject } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    key_?: string | number
    disabled?: boolean
    danger?: boolean
}>(), {
    key_: '',
    disabled: false,
    danger: false,
})

interface MenuContext {
    selectedKeys: { value: (string | number)[] }
    mode: { value: string }
    itemClick: (key: string | number, keyPath: (string | number)[]) => void
}

const menu = inject<MenuContext | null>('mfMenu', null)
const subKey = inject<{ key: string | number } | null>('mfSubMenu', null)
const itemKey = computed(() => props.key_)
const selected = computed(() => menu?.selectedKeys.value.includes(itemKey.value) ?? false)

function onClick(): void {
    if (props.disabled || !menu) return
    const keyPath = subKey ? [itemKey.value, subKey.key] : [itemKey.value]
    menu.itemClick(itemKey.value, keyPath)
}

const horizontal = computed(() => menu?.mode.value === 'horizontal')

const itemClass = computed(() => clsx(
    'relative flex cursor-pointer items-center gap-2.5 rounded-mf-md px-3 py-2 font-medium',
    'transition-[color,background-color] duration-(--mf-duration-fast) ease-(--mf-easing)',
    horizontal ? '' : 'w-full',
    props.danger ? 'text-danger' : 'text-content-secondary',
    props.disabled && 'cursor-not-allowed opacity-40',
    !props.disabled && !selected && 'hover:bg-surface-hover hover:text-content',
    selected && clsx('bg-surface-active', props.danger ? 'text-danger' : 'text-primary'),
))
</script>

<template>
    <li
        role="menuitem"
        :aria-selected="selected"
        :class="itemClass"
        @click="onClick"
    >
        <slot name="icon" />
        <span class="truncate"><slot /></span>
    </li>
</template>
