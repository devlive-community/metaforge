<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'

const props = withDefaults(defineProps<{
    open?: boolean
    title?: string
    width?: string
    placement?: 'right' | 'left' | 'top' | 'bottom'
    closeOnOverlay?: boolean
    closable?: boolean
    /** Dock inside a workspace without a modal backdrop or viewport overlay. */
    inline?: boolean
    container?: string | HTMLElement
}>(), {
    open: false,
    width: '480px',
    placement: 'right',
    closeOnOverlay: true,
    closable: true,
    inline: false,
})

const emit = defineEmits<{
    'update:open': [value: boolean]
    close: []
}>()

function close(): void {
    emit('update:open', false)
    emit('close')
}

const panelStyle = computed(() => {
    if (props.placement === 'left' || props.placement === 'right') {
        return { width: props.width }
    }
    return {}
})

const ENTER: Record<string, string> = {
    right: 'translate-x-full',
    left: '-translate-x-full',
    top: '-translate-y-full',
    bottom: 'translate-y-full',
}

const panelPos = computed(() => clsx(
    'bg-surface flex min-h-0 flex-col',
    props.inline ? 'relative h-full min-w-0 max-w-full overflow-hidden border-l border-line' : 'absolute shadow-mf-lg',
    !props.inline && props.placement === 'right' && 'inset-y-0 right-0 rounded-l-mf-lg',
    !props.inline && props.placement === 'left' && 'inset-y-0 left-0 rounded-r-mf-lg',
    !props.inline && props.placement === 'top' && 'inset-x-0 top-0 rounded-b-mf-lg',
    !props.inline && props.placement === 'bottom' && 'inset-x-0 bottom-0 rounded-t-mf-lg',
))
</script>

<template>
    <Teleport :to="container || 'body'" :disabled="inline && !container">
        <Transition
            :css="!inline"
            enter-active-class="transition duration-(--mf-duration-slow) ease-(--mf-easing)"
            enter-from-class="opacity-0"
            leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0"
        >
            <div v-if="open" :class="inline ? 'relative h-full shrink-0' : 'fixed inset-0 z-50 bg-overlay'" @keydown.esc="close">
                <div v-if="!inline && closeOnOverlay" class="absolute inset-0" aria-hidden="true" @click="close" />
                <Transition
                    :css="!inline"
                    :appear="!inline"
                    :enter-active-class="`transition duration-(--mf-duration-slow) ease-(--mf-easing)`"
                    :enter-from-class="ENTER[placement]"
                    :leave-active-class="`transition duration-(--mf-duration-fast) ease-(--mf-easing)`"
                    :leave-to-class="ENTER[placement]"
                >
                    <section v-if="open" :class="panelPos" :style="panelStyle" role="dialog" :aria-modal="inline ? undefined : true" :aria-label="title">
                        <header v-if="title || $slots.title || $slots.extra" class="flex shrink-0 items-center justify-between border-b border-line px-5 py-4">
                            <h3 class="text-base font-medium text-content">
                                <slot name="title">{{ title }}</slot>
                            </h3>
                            <div class="flex items-center gap-2">
                                <slot name="extra" />
                                <button
                                    v-if="closable"
                                    type="button"
                                    class="inline-flex size-7 items-center justify-center rounded-mf-md text-content-tertiary hover:bg-surface-hover hover:text-content"
                                    aria-label="close"
                                    @click="close"
                                >
                                    <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                        <path d="M6 6l12 12M18 6L6 18" />
                                    </svg>
                                </button>
                            </div>
                        </header>
                        <!-- relative keeps absolutely positioned descendants (sr-only inputs) inside the scroller. -->
                        <div class="relative min-h-0 flex-1 overflow-y-auto px-5 py-4 text-sm text-content">
                            <slot />
                        </div>
                        <footer v-if="$slots.footer" class="flex shrink-0 justify-end gap-2 border-t border-line px-5 py-3">
                            <slot name="footer" />
                        </footer>
                    </section>
                </Transition>
            </div>
        </Transition>
    </Teleport>
</template>
