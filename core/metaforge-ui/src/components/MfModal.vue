<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
    open?: boolean
    title?: string
    width?: string
    height?: string
    subtitle?: string
    bodyClass?: string
    trapFocus?: boolean
    overlayClass?: string
    closeDisabled?: boolean
    panelClass?: string
    closeOnOverlay?: boolean
    closeOnEsc?: boolean
}>(), {
    open: false,
    width: '480px',
    bodyClass: 'px-5 py-4',
    trapFocus: false,
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
    if (props.trapFocus && event.key === 'Tab') {
        const controls = focusable()
        const first = controls[0]
        const last = controls[controls.length - 1]
        if (first && event.shiftKey && panel.value?.ownerDocument.activeElement === first) { event.preventDefault(); last.focus() }
        else if (last && !event.shiftKey && panel.value?.ownerDocument.activeElement === last) { event.preventDefault(); first.focus() }
    }
    if (props.closeOnEsc && event.key === 'Escape') {
        close()
    }
}

const style = computed(() => ({ maxWidth: props.width, height: props.height }))
const panel = ref<HTMLElement>()
const body = ref<HTMLElement>()
defineExpose({ scrollToTop: () => { if (body.value) body.value.scrollTop = 0 } })
let previousFocus: HTMLElement | null = null
let restoreOverflow: string | undefined
function focusable(): HTMLElement[] {
    if (!panel.value?.querySelectorAll) return []
    return Array.from(panel.value?.querySelectorAll<HTMLElement>('button, a[href], input, textarea, [tabindex="0"]') ?? [])
        .filter(element => !(element as HTMLButtonElement).disabled && element.getClientRects().length > 0)
}
function releaseFocus(): void {
    if (restoreOverflow !== undefined && typeof document !== 'undefined' && document.documentElement) document.documentElement.style.overflow = restoreOverflow
    restoreOverflow = undefined
    if (previousFocus?.isConnected) previousFocus.focus()
    previousFocus = null
}
watch(() => props.open, open => {
    if (!props.trapFocus) return
    if (!open) { releaseFocus(); return }
    if (typeof document !== 'undefined' && document.documentElement) {
        previousFocus = document.activeElement as HTMLElement | null
        restoreOverflow = document.documentElement.style.overflow
        document.documentElement.style.overflow = 'hidden'
    }
    nextTick(() => { if (props.open && panel.value?.querySelectorAll) focusable()[0]?.focus() })
}, { immediate: true })
onBeforeUnmount(releaseFocus)
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
                class="fixed inset-0 z-50 flex items-center justify-center bg-overlay p-4" :class="overlayClass"
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
                        ref="panel"
                        role="dialog"
                        aria-modal="true"
                        :style="style"
                        :class="panelClass"
                        class="relative flex max-h-full w-full flex-col overflow-hidden rounded-mf-lg bg-surface shadow-mf-lg"
                    >
                        <header v-if="title || $slots.title" class="flex shrink-0 items-center justify-between border-b border-line px-5 py-4">
                            <div class="min-w-0">
                                <h3 class="truncate text-base font-semibold text-content" :title="title"><slot name="title">{{ title }}</slot></h3>
                                <p v-if="subtitle" class="mt-1 text-xs text-content-tertiary">{{ subtitle }}</p>
                            </div>
                            <button
                                type="button"
                                class="inline-flex size-7 items-center justify-center rounded-mf-md text-content-tertiary hover:bg-surface-hover hover:text-content"
                                aria-label="close"
                                :disabled="closeDisabled"
                                @click="close"
                            >
                                <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                                    <path d="M6 6l12 12M18 6L6 18" />
                                </svg>
                            </button>
                        </header>
                        <slot name="after-header" />
                        <div ref="body" class="min-h-0 flex-1 overflow-y-auto text-sm text-content" :class="bodyClass">
                            <slot />
                        </div>
                        <footer v-if="$slots.footer" class="flex shrink-0 justify-end gap-2 border-t border-line px-5 py-3">
                            <slot name="footer" />
                        </footer>
                    </div>
                </Transition>
            </div>
        </Transition>
    </Teleport>
</template>
