<script setup lang="ts">
import { ref } from 'vue'
import MfButton from './MfButton.vue'

withDefaults(defineProps<{
    loading?: boolean
    searchText?: string
    resetText?: string
    moreText?: string
    lessText?: string
}>(), {
    loading: false,
    searchText: 'Search',
    resetText: 'Reset',
    moreText: 'More filters',
    lessText: 'Fewer filters',
})

const emit = defineEmits<{
    search: []
    reset: []
}>()

const expanded = ref(false)
</script>

<template>
    <section class="mb-4 min-w-0 rounded-mf-lg border border-line bg-surface p-4 sm:p-5">
        <form v-if="$slots.filter || $slots['advanced-filter']" class="mb-5" @submit.prevent="emit('search')">
            <div class="flex items-start gap-4">
                <div class="flex min-w-0 flex-1 flex-wrap items-end gap-4">
                    <div v-if="$slots.filter" class="contents [&>*]:w-40 [&>*]:max-w-full">
                        <slot name="filter" />
                    </div>
                    <div v-if="$slots['advanced-filter']" v-show="expanded" class="contents [&>*]:w-40 [&>*]:max-w-full">
                        <slot name="advanced-filter" />
                    </div>
                    <div class="flex shrink-0 items-center gap-2">
                        <MfButton type="submit" variant="primary" :loading="loading">{{ searchText }}</MfButton>
                        <MfButton :disabled="loading" @click="emit('reset')">{{ resetText }}</MfButton>
                    </div>
                </div>
                <button
                    v-if="$slots['advanced-filter']"
                    type="button"
                    class="ml-auto inline-flex shrink-0 items-center gap-1 self-start rounded-mf-sm py-1 text-sm text-primary outline-none hover:text-primary-hover focus-visible:ring-2 focus-visible:ring-primary/40"
                    :aria-expanded="expanded"
                    @click="expanded = !expanded"
                >
                    {{ expanded ? lessText : moreText }}
                    <svg class="size-4 transition-transform" :class="expanded && 'rotate-180'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M6 9l6 6 6-6" />
                    </svg>
                </button>
            </div>
        </form>
        <div class="flex flex-wrap items-center justify-between gap-3">
            <div v-if="$slots.actions" class="flex min-w-0 flex-wrap items-center gap-2">
                <slot name="actions" />
            </div>
            <div v-if="$slots.tools" class="ml-auto flex shrink-0 flex-wrap items-center gap-2">
                <slot name="tools" />
            </div>
        </div>
    </section>
</template>
