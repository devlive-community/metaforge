<script setup lang="ts">
import { computed, ref } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    trigger?: ('click' | 'hover' | 'contextmenu')[] | string
    placement?: 'bottomLeft' | 'bottom' | 'bottomRight' | 'topLeft' | 'top' | 'topRight'
}>(), {
    trigger: 'hover',
    placement: 'bottomLeft',
})

const open = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined

const triggers = computed(() => Array.isArray(props.trigger) ? props.trigger : [props.trigger])
const hoverable = computed(() => triggers.value.includes('hover'))

function show(): void {
    if (timer) clearTimeout(timer)
    open.value = true
}

function hide(): void {
    timer = setTimeout(() => { open.value = false }, 120)
}

function toggle(event: Event): void {
    if (triggers.value.includes('contextmenu')) {
        event.preventDefault()
    }
    open.value = !open.value
}

const POS = {
    bottomLeft: 'top-full left-0 mt-1.5',
    bottom: 'top-full left-1/2 -translate-x-1/2 mt-1.5',
    bottomRight: 'top-full right-0 mt-1.5',
    topLeft: 'bottom-full left-0 mb-1.5',
    top: 'bottom-full left-1/2 -translate-x-1/2 mb-1.5',
    topRight: 'bottom-full right-0 mb-1.5',
} as const

const overlayClass = computed(() => clsx(
    'absolute z-40 min-w-40 rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg',
    POS[props.placement],
))
</script>

<template>
    <div
        class="relative inline-flex"
        @mouseenter="hoverable && show()"
        @mouseleave="hoverable && hide()"
        @click.stop="triggers.includes('click') && toggle($event)"
        @contextmenu="triggers.includes('contextmenu') && toggle($event)"
    >
        <slot />
        <Transition
            enter-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            enter-from-class="opacity-0 scale-95"
            leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0 scale-95"
        >
            <div v-if="open" :class="overlayClass" @mouseenter="show" @mouseleave="hoverable && hide()">
                <slot name="overlay" />
            </div>
        </Transition>
    </div>
</template>
