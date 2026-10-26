<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
    open?: boolean
    title?: string
    width?: string
    closeOnOverlay?: boolean
    closeOnEsc?: boolean
}>(), {
    open: false,
    width: '480px',
    closeOnOverlay: true,
    closeOnEsc: true,
})

const emit = defineEmits<{
    'update:open': [value: boolean]
    close: []
}>()

function close(): void {
    emit('update:open', false)
    emit('close')
}

function onOverlayClick(): void {
    if (props.closeOnOverlay) {
        close()
    }
}

function onKeydown(event: KeyboardEvent): void {
    if (props.closeOnEsc && event.key === 'Escape') {
        close()
    }
}

const style = computed(() => ({ maxWidth: props.width }))
</script>

<template>
    <Teleport to="body">
        <Transition
            enter-active-class="transition duration-(--mf-duration-slow) ease-(--mf-easing)"
            enter-from-class="opacity-0"
            leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0"
        >
            <div
                v-if="open"
                class="fixed inset-0 z-50 flex items-center justify-center bg-overlay p-4"
                @keydown="onKeydown"
            >
                <div class="absolute inset-0" aria-hidden="true" @click="onOverlayClick" />
                <Transition
                    appear
                    enter-active-class="transition duration-(--mf-duration-slow) ease-(--mf-easing)"
                    enter-from-class="opacity-0 scale-95 translate-y-2"
                    leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
                    leave-to-class="opacity-0 scale-95 translate-y-2"
                >
                    <div
                        v-if="open"
                        role="dialog"
                        aria-modal="true"
                        :style="style"
                        class="relative w-full rounded-mf-lg bg-surface shadow-mf-lg"
                    >
                        <header v-if="title || $slots.title" class="flex items-center justify-between border-b border-line px-5 py-4">
                            <h3 class="text-base font-medium text-content">
                                <slot name="title">{{ title }}</slot>
                            </h3>
                            <button
                                type="button"
                                class="inline-flex size-7 items-center justify-center rounded-mf-md text-content-tertiary hover:bg-surface-hover hover:text-content"
                                aria-label="close"
                                @click="close"
                            >
                                <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <path d="M6 6l12 12M18 6L6 18" />
                                </svg>
                            </button>
                        </header>
                        <div class="px-5 py-4 text-sm text-content">
                            <slot />
                        </div>
                        <footer v-if="$slots.footer" class="flex justify-end gap-2 border-t border-line px-5 py-3">
                            <slot name="footer" />
                        </footer>
                    </div>
                </Transition>
            </div>
        </Transition>
    </Teleport>
</template>
