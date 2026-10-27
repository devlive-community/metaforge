<script setup lang="ts">
import { computed, ref, useSlots } from 'vue'
import { useDismissableLayer } from '../composables/useDismissableLayer'
import { useOverlayPosition } from '../composables/useOverlayPosition'

const props = withDefaults(defineProps<{
    content?: string
    placement?: 'top' | 'bottom'
}>(), {
    content: '',
    placement: 'top',
})

const root = ref<HTMLElement>()
const overlay = ref<HTMLElement>()
const hovered = ref(false)
const focused = ref(false)
const dismissed = ref(false)
const slots = useSlots()
const open = computed(() => (hovered.value || focused.value) && !dismissed.value && !!(props.content || slots.content))
const style = useOverlayPosition({ root, overlay, open, maxWidth: 320, placement: () => props.placement })
useDismissableLayer(root, open, () => { dismissed.value = true }, [overlay])

function focusOut(event: FocusEvent): void {
    if (!root.value?.contains(event.relatedTarget as Node | null)) {
        focused.value = false
        dismissed.value = false
    }
}
</script>

<template>
    <span ref="root" class="relative inline-flex"
          @mouseenter="hovered = true; dismissed = false" @mouseleave="hovered = false"
          @pointerdown="dismissed = true"
          @focusin="focused = true" @focusout="focusOut">
        <slot />
        <Teleport to="body">
            <Transition enter-active-class="transition-opacity duration-(--mf-duration-fast)" enter-from-class="opacity-0"
                        leave-active-class="transition-opacity duration-(--mf-duration-fast)" leave-to-class="opacity-0">
                <span v-if="open" ref="overlay" role="tooltip" :style="style"
                      class="pointer-events-none fixed z-60 w-max overflow-hidden whitespace-normal rounded-mf-md bg-content px-2 py-1 text-xs leading-5 text-content-inverse shadow-mf-md [overflow-wrap:anywhere]">
                    <slot name="content">{{ content }}</slot>
                </span>
            </Transition>
        </Teleport>
    </span>
</template>
