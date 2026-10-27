<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useDismissableLayer } from '../composables/useDismissableLayer'
import { useOverlayPosition, type OverlayPlacement } from '../composables/useOverlayPosition'

const props = withDefaults(defineProps<{
    placement?: OverlayPlacement
    trigger?: 'hover' | 'click' | 'focus'
}>(), {
    placement: 'top',
    trigger: 'hover',
})

const open = defineModel<boolean>('open', { default: false })
const root = ref<HTMLElement>()
const content = ref<HTMLElement>()
let timer: ReturnType<typeof setTimeout> | undefined

function close(): void {
    if (timer) clearTimeout(timer)
    timer = undefined
    open.value = false
}

useDismissableLayer(root, open, close, [content])
onBeforeUnmount(() => { if (timer) clearTimeout(timer) })

function show(): void {
    if (timer) clearTimeout(timer)
    timer = undefined
    open.value = true
}

function hide(): void {
    if (timer) clearTimeout(timer)
    timer = setTimeout(close, 120)
}

function toggle(event: Event): void {
    if (content.value?.contains(event.target as Node)) return
    if (timer) clearTimeout(timer)
    timer = undefined
    open.value = !open.value
}

const events = computed(() => props.trigger === 'click'
    ? { click: toggle }
    : props.trigger === 'focus'
        ? { focus: show, blur: hide }
        : { mouseenter: show, mouseleave: hide })

// Teleported to <body> and pinned with `position: fixed`, so the panel is no
// longer clipped by scrolling table/card ancestors. z-50 matches MfModal/MfDrawer.
const contentStyle = useOverlayPosition({
    root,
    open,
    placement: () => props.placement,
    gap: 8,
})
</script>

<template>
    <span ref="root" class="relative inline-flex" v-on="events">
        <slot />
        <Teleport to="body">
            <Transition
                enter-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
                enter-from-class="opacity-0 scale-95"
                leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
                leave-to-class="opacity-0 scale-95"
            >
                <span
                    v-if="open"
                    ref="content"
                    role="tooltip"
                    class="fixed z-50 block w-max max-w-xs rounded-mf-md bg-surface px-3 py-2 text-xs text-content shadow-mf-md border border-line"
                    :style="contentStyle"
                    @mouseenter="show"
                    @mouseleave="hide"
                >
                    <slot name="content" />
                </span>
            </Transition>
        </Teleport>
    </span>
</template>
