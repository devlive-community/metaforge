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
}>(), {
    open: false,
    width: '480px',
    placement: 'right',
    closeOnOverlay: true,
    closable: true,
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
    'absolute bg-surface shadow-mf-lg flex flex-col',
    props.placement === 'right' && 'inset-y-0 right-0 rounded-l-mf-lg',
    props.placement === 'left' && 'inset-y-0 left-0 rounded-r-mf-lg',
    props.placement === 'top' && 'inset-x-0 top-0 rounded-b-mf-lg',
    props.placement === 'bottom' && 'inset-x-0 bottom-0 rounded-t-mf-lg',
))
</script>

<template>
    <Teleport to="body">
        <Transition
            enter-active-class="transition duration-(--mf-duration-slow) ease-(--mf-easing)"
            enter-from-class="opacity-0"
            leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0"
        >
            <div v-if="open" class="fixed inset-0 z-50 bg-overlay" @keydown.esc="close">
                <div v-if="closeOnOverlay" class="absolute inset-0" aria-hidden="true" @click="close" />
                <Transition
                    appear
                    :enter-active-class="`transition duration-(--mf-duration-slow) ease-(--mf-easing)`"
                    :enter-from-class="ENTER[placement]"
                    :leave-active-class="`transition duration-(--mf-duration-fast) ease-(--mf-easing)`"
                    :leave-to-class="ENTER[placement]"
                >
                    <section v-if="open" :class="panelPos" :style="panelStyle" role="dialog" aria-modal="true">
                        <header v-if="title || $slots.title || $slots.extra" class="flex items-center justify-between border-b border-line px-5 py-4">
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
                        <div class="flex-1 overflow-y-auto px-5 py-4 text-sm text-content">
                            <slot />
                        </div>
                        <footer v-if="$slots.footer" class="flex justify-end gap-2 border-t border-line px-5 py-3">
                            <slot name="footer" />
                        </footer>
                    </section>
                </Transition>
            </div>
        </Transition>
    </Teleport>
</template>
