<script setup lang="ts">
import { computed, inject, provide, ref, watch } from 'vue'
import clsx from 'clsx'
import { useDismissableLayer } from '../composables/useDismissableLayer'

const props = withDefaults(defineProps<{
    key_?: string | number
    title?: string
    disabled?: boolean
}>(), {
    key_: '',
    title: '',
    disabled: false,
})

interface MenuContext {
    openKeys: { value: (string | number)[] }
    mode: { value: string }
    toggleOpen: (key: string | number) => void
    selectedKeys: { value: (string | number)[] }
    itemClick: (key: string | number, keyPath: (string | number)[]) => void
}

const menu = inject<MenuContext | null>('mfMenu', null)
provide('mfSubMenu', { key: props.key_ })

const open = computed(() => menu?.openKeys.value.includes(props.key_) ?? false)
const horizontal = computed(() => menu?.mode.value === 'horizontal')
const popup = ref(false)
const root = ref<HTMLElement>()

useDismissableLayer(root, computed(() => horizontal.value && popup.value), () => { popup.value = false })
watch(horizontal, () => { popup.value = false })

function toggle(): void {
    if (props.disabled) return
    if (horizontal.value) {
        popup.value = !popup.value
    }
    else {
        menu?.toggleOpen(props.key_)
    }
}

const titleClass = computed(() => clsx(
    'flex w-full cursor-pointer items-center gap-2.5 rounded-mf-md px-3 py-2 font-medium',
    'transition-[color,background-color] duration-(--mf-duration-fast) ease-(--mf-easing)',
    props.disabled ? 'cursor-not-allowed opacity-40' : 'text-content-secondary hover:bg-surface-hover hover:text-content',
))

const chevron = computed(() => clsx(
    'ml-auto size-3.5 shrink-0 text-content-tertiary transition-transform duration-(--mf-duration-base) ease-(--mf-easing)',
    (open.value || popup.value) && !horizontal.value && 'rotate-180',
))
</script>

<template>
    <li
        ref="root"
        class="relative"
        :class="horizontal ? 'flex items-center' : 'w-full'"
        role="menuitem"
        aria-haspopup="true"
        :aria-expanded="open || popup"
        @mouseenter="horizontal && !disabled && (popup = true)"
        @mouseleave="horizontal && (popup = false)"
    >
        <div :class="titleClass" @click="toggle">
            <slot name="icon" />
            <span class="truncate">
                <slot name="title">{{ title }}</slot>
            </span>
            <svg :class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path d="M6 9l6 6 6-6" />
            </svg>
        </div>

        <!-- inline: collapsible -->
        <div v-if="!horizontal" v-show="open" class="relative ml-3 flex flex-col gap-0.5 border-l border-line pl-3">
            <slot />
        </div>

        <!-- horizontal: popup -->
        <div
            v-if="horizontal"
            v-show="popup"
            class="absolute left-0 top-full z-40 mt-1 min-w-40 rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg"
        >
            <div class="flex flex-col gap-0.5">
                <slot />
            </div>
        </div>
    </li>
</template>
