<script setup lang="ts">
import { computed, ref } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    placement?: 'top' | 'bottom' | 'topLeft' | 'topRight' | 'bottomLeft' | 'bottomRight'
    trigger?: 'hover' | 'click' | 'focus'
}>(), {
    placement: 'top',
    trigger: 'hover',
})

const open = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined

function show(): void {
    if (timer) clearTimeout(timer)
    open.value = true
}

function hide(): void {
    timer = setTimeout(() => { open.value = false }, 120)
}

function toggle(): void {
    open.value = !open.value
}

const events = computed(() => props.trigger === 'click'
    ? { click: toggle }
    : props.trigger === 'focus'
        ? { focus: show, blur: hide }
        : { mouseenter: show, mouseleave: hide })

const POS = {
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
    topLeft: 'bottom-full left-0 mb-2',
    topRight: 'bottom-full right-0 mb-2',
    bottomLeft: 'top-full left-0 mt-2',
    bottomRight: 'top-full right-0 mt-2',
} as const
</script>

<template>
    <span class="relative inline-flex" v-on="events">
        <slot />
        <Transition
            enter-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            enter-from-class="opacity-0 scale-95"
            leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0 scale-95"
        >
            <span
                v-if="open"
                role="tooltip"
                :class="clsx('absolute z-40 block w-max max-w-xs rounded-mf-md bg-surface px-3 py-2 text-xs text-content shadow-mf-md border border-line', POS[placement])"
                @mouseenter="show"
                @mouseleave="hide"
            >
                <slot name="content" />
            </span>
        </Transition>
    </span>
</template>
