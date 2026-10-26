<script setup lang="ts">
import clsx from 'clsx'
import { messageState } from '../theme/message-state'

const ICONS = {
    success: 'M5 13l4 4L19 7',
    error: 'M6 6l12 12M18 6L6 18',
    info: 'M12 16v-6M12 8h.01',
    warning: 'M12 9v4M12 17h.01',
    loading: '',
} as const

const TONES = {
    success: 'text-success',
    error: 'text-danger',
    info: 'text-info',
    warning: 'text-warning',
    loading: 'text-primary',
} as const
</script>

<template>
    <Teleport to="body">
        <div class="pointer-events-none fixed left-1/2 top-5 z-[70] flex -translate-x-1/2 flex-col items-center gap-2" role="status" aria-live="polite">
            <TransitionGroup
                enter-active-class="transition duration-(--mf-duration-base) ease-(--mf-easing)"
                enter-from-class="opacity-0 -translate-y-2"
                leave-active-class="transition duration-(--mf-duration-fast) ease-(--mf-easing)"
                leave-to-class="opacity-0 -translate-y-1"
                move-class="transition-transform duration-(--mf-duration-base)"
            >
                <div
                    v-for="item in messageState.items"
                    :key="item.id"
                    class="pointer-events-auto flex max-w-md items-center gap-2.5 rounded-mf-md border border-line bg-surface px-4 py-2.5 text-sm text-content shadow-mf-lg"
                >
                    <svg v-if="item.type === 'loading'" class="size-4 shrink-0 animate-spin text-primary" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
                    </svg>
                    <svg v-else :class="clsx('size-4 shrink-0', TONES[item.type])" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <circle cx="12" cy="12" r="10" />
                        <path :d="ICONS[item.type]" stroke-linecap="round" />
                    </svg>
                    <span>{{ item.content }}</span>
                </div>
            </TransitionGroup>
        </div>
    </Teleport>
</template>
