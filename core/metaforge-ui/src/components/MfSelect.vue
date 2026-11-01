<script setup lang="ts">
import { computed, ref, useAttrs, watch } from 'vue'
import clsx from 'clsx'
import MfLoadingIcon from './MfLoadingIcon.vue'
import MfEmpty from './MfEmpty.vue'
import MfSpinner from './MfSpinner.vue'
import { CONTROL_SIZES, TRANSITION, hasWidthClass, type MfControlSize, type MfSelectOption } from './common'
import { useDismissableLayer } from '../composables/useDismissableLayer'
import { useOverlayPosition } from '../composables/useOverlayPosition'

// The root fills its container unless the caller gives it a width.
const attrs = useAttrs()

const props = withDefaults(defineProps<{
    modelValue?: string | number | (string | number)[]
    options?: MfSelectOption[]
    size?: MfControlSize
    placeholder?: string
    multiple?: boolean
    /** antd `mode="tags"`: pressing enter creates a new option. */
    tags?: boolean
    clearable?: boolean
    disabled?: boolean
    invalid?: boolean
    loading?: boolean
    emptyText?: string
    noMatchText?: string
    loadingText?: string
    /** Typing filters options by label (single select behaves like a combobox). */
    searchable?: boolean
    /** Override the default label-includes filter. */
    filterOption?: (option: MfSelectOption, query: string) => boolean
}>(), {
    modelValue: '',
    options: () => [],
    size: 'md',
    multiple: false,
    tags: false,
    clearable: false,
    disabled: false,
    invalid: false,
    loading: false,
    emptyText: 'No data',
    noMatchText: 'No match',
    loadingText: 'Loading…',
    searchable: false,
})

const emit = defineEmits<{
    'update:modelValue': [value: string | number | (string | number)[]]
    change: [value: string | number | (string | number)[]]
    search: [query: string]
}>()

const open = ref(false)
const root = ref<HTMLElement>()
const overlay = ref<HTMLElement>()
const query = ref('')
const searchInput = ref<HTMLInputElement>()

/** The popup teleports to body with fixed positioning, so scroll and
 *  overflow containers cannot clip it; the composable flips it above
 *  the trigger when the viewport has no room below. */
const popupStyle = useOverlayPosition({ root, open, placement: () => 'bottomLeft', overlay, gap: 6, matchTriggerWidth: true })

function toggle(): void {
    if (!props.disabled) {
        open.value = !open.value
        if (open.value && props.searchable) {
            requestAnimationFrame(() => searchInput.value?.focus())
        }
    }
}

function close(): void {
    open.value = false
    query.value = ''
}

useDismissableLayer(root, open, close, [overlay])

const isMultiple = computed(() => props.multiple || props.tags)

const selectedValues = computed<(string | number)[]>(() =>
    Array.isArray(props.modelValue)
        ? props.modelValue
        : props.modelValue === '' || props.modelValue === undefined || props.modelValue === null
            ? []
            : [props.modelValue])

function isSelected(option: MfSelectOption): boolean {
    return selectedValues.value.includes(option.value)
}

function commit(next: string | number | (string | number)[]): void {
    emit('update:modelValue', next)
    emit('change', next)
}

function select(option: MfSelectOption): void {
    if (option.disabled || props.disabled) return
    if (isMultiple.value) {
        const next = isSelected(option)
            ? selectedValues.value.filter(v => v !== option.value)
            : [...selectedValues.value, option.value]
        commit(next)
        query.value = ''
    }
    else {
        commit(option.value)
        close()
    }
}

function remove(value: string | number): void {
    commit(selectedValues.value.filter(item => item !== value))
}

function clear(): void {
    commit(isMultiple.value ? [] : '')
    query.value = ''
}

function createTag(): void {
    const text = query.value.trim()
    if (!text) return
    const matched = filteredOptions.value.find(option =>
        option.label.toLowerCase() === text.toLowerCase() && !option.disabled)
    if (matched) {
        select(matched)
        return
    }
    if (props.tags && isMultiple.value && !selectedValues.value.includes(text)) {
        commit([...selectedValues.value, text])
        query.value = ''
    }
}

function onQueryKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
        event.preventDefault()
        createTag()
    }
    else if (event.key === 'Backspace' && isMultiple.value && query.value === '' && selectedValues.value.length > 0) {
        commit(selectedValues.value.slice(0, -1))
    }
}

const filteredOptions = computed(() => {
    if (!props.searchable || !query.value.trim()) return props.options
    const q = query.value.trim().toLowerCase()
    return props.options.filter(option =>
        props.filterOption ? props.filterOption(option, q) : option.label.toLowerCase().includes(q))
})

const selectedOptions = computed(() => {
    const byValue = new Map(props.options.map(option => [option.value, option]))
    return [...new Set(selectedValues.value)].map(value => byValue.get(value))
        .filter((option): option is MfSelectOption => option !== undefined)
})

const label = computed(() => selectedOptions.value.map(option => option.label).join(', '))

const grouped = computed(() => {
    const groups = new Map<string, MfSelectOption[]>()
    for (const option of filteredOptions.value) {
        const key = option.group ?? ''
        if (!groups.has(key)) groups.set(key, [])
        groups.get(key)!.push(option)
    }
    return Array.from(groups.entries())
})

const heightClass = computed(() => CONTROL_SIZES[props.size].split(' ').find(c => c.startsWith('h-')) ?? 'h-9')
const searchActive = computed(() => props.searchable && open.value)

const triggerClass = computed(() => clsx(
    'mf-invalidable inline-flex min-w-0 w-full max-w-full cursor-pointer items-center gap-2 rounded-mf-md border bg-surface pl-3 pr-2.5 text-sm',
    isMultiple.value ? 'min-h-9 py-1' : heightClass.value,
    TRANSITION,
    props.invalid ? 'border-danger' : open.value ? 'border-primary' : 'border-line',
    props.disabled && 'cursor-not-allowed opacity-60',
))

watch(open, value => {
    if (!value) query.value = ''
})
</script>

<template>
    <div ref="root" class="relative inline-flex min-w-0 max-w-full" :class="!hasWidthClass(attrs.class) && 'w-full'">
        <div :class="triggerClass" @click="toggle">
            <span v-if="$slots.prefix" class="inline-flex shrink-0 items-center text-content-secondary"><slot name="prefix" /></span>
            <span
                class="flex min-w-0 flex-1 flex-wrap items-center gap-1 overflow-y-auto text-left"
                :class="selectedOptions.length === 0 && 'text-content-placeholder'"
                :style="isMultiple && selectedOptions.length > 0 ? { maxHeight: '96px' } : {}"
            >
                <template v-if="isMultiple">
                    <span
                        v-for="option in selectedOptions"
                        :key="String(option.value)"
                        class="inline-flex min-w-0 max-w-40 items-center gap-1 rounded-mf-sm bg-surface-active px-1.5 py-0.5 text-xs text-content"
                    >
                        <span class="truncate">{{ option.label }}</span>
                        <button
                            v-if="!disabled"
                            type="button"
                            class="inline-flex size-3.5 shrink-0 items-center justify-center rounded-mf-full text-content-tertiary hover:text-content"
                            :aria-label="`remove ${option.label}`"
                            @click.stop="remove(option.value)"
                        >
                            <svg class="size-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
                        </button>
                    </span>
                </template>
                <template v-else-if="!searchActive && selectedOptions.length > 0">
                    <span class="truncate text-content">{{ label }}</span>
                </template>
                <input
                    v-if="searchActive"
                    ref="searchInput"
                    v-model="query"
                    class="w-0 min-w-0 bg-transparent text-sm text-content placeholder:text-content-placeholder outline-none"
                    :class="isMultiple ? 'flex-[1_1_3rem]' : 'flex-1'"
                    :placeholder="selectedOptions.length === 0 ? placeholder : isMultiple ? '' : label"
                    @click.stop
                    @keydown="onQueryKeydown"
                    @input="emit('search', query)"
                >
                <span v-else-if="selectedOptions.length === 0">{{ placeholder }}</span>
            </span>
            <span
                v-if="clearable && selectedOptions.length > 0 && !disabled"
                class="inline-flex size-4 shrink-0 items-center justify-center rounded-mf-full text-content-tertiary hover:bg-surface-hover hover:text-content"
                @click.stop="clear"
            >
                <svg class="size-2.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
            </span>
            <MfLoadingIcon v-if="loading" :size="14" class="text-primary"/>
            <svg
                v-else
                class="size-3.5 shrink-0 text-content-tertiary transition-transform duration-(--mf-duration-base) ease-(--mf-easing)"
                :class="open && 'rotate-180'"
                viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"
            >
                <path d="M6 9l6 6 6-6" />
            </svg>
        </div>

        <Teleport to="body">
        <!-- Fade only: a scale transition changes the measured box during the
             enter animation and the fixed position would overlap the trigger. -->
        <Transition
            enter-active-class="transition-opacity duration-(--mf-duration-fast) ease-(--mf-easing)"
            enter-from-class="opacity-0"
            leave-active-class="transition-opacity duration-(--mf-duration-fast) ease-(--mf-easing)"
            leave-to-class="opacity-0"
        >
            <ul
                v-if="open"
                ref="overlay"
                role="listbox"
                class="fixed z-50 flex max-h-64 flex-col gap-1 overflow-y-auto rounded-mf-lg border border-line bg-surface p-1 shadow-mf-lg"
                :style="{ ...popupStyle, maxHeight: '16rem' }"
            >
                <template v-for="[group, groupOptions] in grouped" :key="group || 'default'">
                    <li v-if="group" class="px-2.5 pt-1.5 pb-1 text-xs font-semibold text-content-tertiary">
                        {{ group }}
                    </li>
                    <li
                        v-for="option in groupOptions"
                        :key="String(option.value)"
                        role="option"
                        :aria-selected="isSelected(option)"
                        class="flex cursor-pointer items-center justify-between gap-2 rounded-mf-md px-2.5 py-1.5 text-sm"
                        :class="clsx(
                            option.disabled
                                ? 'cursor-not-allowed opacity-40'
                                : isSelected(option)
                                    ? 'bg-surface-active font-medium text-primary'
                                    : 'text-content-secondary hover:bg-surface-hover hover:text-content',
                        )"
                        @click="select(option)"
                    >
                        <span class="truncate">{{ option.label }}</span>
                        <svg v-if="isSelected(option)" class="size-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                            <path d="M5 13l4 4L19 7" />
                        </svg>
                    </li>
                </template>
                <li
                    v-if="tags && query.trim() && !filteredOptions.some(o => o.label.toLowerCase() === query.trim().toLowerCase())"
                    class="cursor-pointer rounded-mf-md px-2.5 py-1.5 text-sm text-primary hover:bg-surface-hover"
                    @click="createTag"
                >
                    Create "{{ query.trim() }}"
                </li>
                <li v-if="loading && filteredOptions.length === 0" class="flex justify-center px-2.5 py-4" role="presentation">
                    <MfSpinner size="sm" :label="loadingText"/>
                </li>
                <li v-else-if="!loading && filteredOptions.length === 0 && !(tags && query.trim())" role="presentation">
                    <MfEmpty size="sm" :variant="query.trim() ? 'search' : 'data'" :title="query.trim() ? noMatchText : emptyText"/>
                </li>
            </ul>
        </Transition>
        </Teleport>
    </div>
</template>
