<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useDismissableLayer } from '../composables/useDismissableLayer'
import { useOverlayPosition, type OverlayPlacement } from '../composables/useOverlayPosition'

const props = withDefaults(defineProps<{
    trigger?: ('click' | 'hover' | 'contextmenu')[] | string
    placement?: OverlayPlacement
}>(), {
    trigger: 'hover',
    placement: 'bottomLeft',
})

const open = defineModel<boolean>('open', { default: false })
const root = ref<HTMLElement>()
const overlay = ref<HTMLElement>()
let timer: ReturnType<typeof setTimeout> | undefined

function close(): void {
    if (timer) clearTimeout(timer)
    timer = undefined
    open.value = false
}

useDismissableLayer(root, open, close, [overlay])
onBeforeUnmount(() => { if (timer) clearTimeout(timer) })

const triggers = computed(() => Array.isArray(props.trigger) ? props.trigger : [props.trigger])
const hoverable = computed(() => triggers.value.includes('hover'))

/** Clicking any actionable element inside the overlay closes the menu:
 *  menu items open targets (modals, pages) and must not linger under them. */
function onOverlayClick(event: Event): void {
    const target = event.target as HTMLElement | null
    if (target?.closest?.('button, a, [role="menuitem"]')) close()
}

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
    if (timer) clearTimeout(timer)
    timer = undefined
    if (triggers.value.includes('contextmenu')) {
        event.preventDefault()
    }
    open.value = !open.value
}

// Teleported to <body> and pinned with `position: fixed`, so the menu is no
// longer clipped by scrolling table/card ancestors. z-50 matches MfModal/MfDrawer.
const overlayStyle = useOverlayPosition({
    root,
    open,
    placement: () => props.placement,
})
</script>

<template>
    <div
        ref="root"
        class="relative inline-flex"
        @mouseenter="hoverable && show()"
        @mouseleave="hoverable && hide()"
        @click.stop="triggers.includes('click') && toggle($event)"
        @contextmenu="triggers.includes('contextmenu') && toggle($event)"
    >
        <slot />
        <Teleport to="body">
            <Transition
                enter-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
                enter-from-class="opacity-0 scale-95"
                leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
                leave-to-class="opacity-0 scale-95"
            >
                <div
                    v-if="open"
                    ref="overlay"
                    class="fixed z-50 min-w-40 rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg"
                    :style="overlayStyle"
                    @click="onOverlayClick"
                    @mouseenter="show"
                    @mouseleave="hoverable && hide()"
                >
                    <slot name="overlay" />
                </div>
            </Transition>
        </Teleport>
    </div>
</template>
