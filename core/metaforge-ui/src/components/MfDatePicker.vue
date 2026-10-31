<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useDismissableLayer } from '../composables/useDismissableLayer'
import { useOverlayPosition } from '../composables/useOverlayPosition'
import { dateKey, monthDays, moveMonth, parseDate } from './calendar'
import { DISABLED, FOCUS_RING, TRANSITION } from './common'

const props = withDefaults(defineProps<{
    modelValue?: string
    locale?: string
    placeholder?: string
    label?: string
    disabled?: boolean
    min?: string
    max?: string
    todayText?: string
    clearText?: string
    previousText?: string
    nextText?: string
}>(), {
    modelValue: '', locale: 'en', placeholder: 'Select date', label: 'Date', disabled: false,
    todayText: 'Today', clearText: 'Clear', previousText: 'Previous period', nextText: 'Next period',
})
const emit = defineEmits<{ 'update:modelValue': [value: string]; change: [value: string] }>()
const root = ref<HTMLElement>()
const trigger = ref<HTMLButtonElement>()
const overlay = ref<HTMLElement>()
const open = ref(false)
const view = ref(new Date())
const mode = ref<'days' | 'months' | 'years'>('days')
const focused = ref('')
const dayElements = new Map<string, HTMLElement>()
const today = () => dateKey(new Date())
const days = computed(() => monthDays(view.value))
const weeks = computed(() => Array.from({ length: 6 }, (_, index) => days.value.slice(index * 7, index * 7 + 7)))
const weekdays = computed(() => Array.from({ length: 7 }, (_, index) => new Intl.DateTimeFormat(props.locale, { weekday: 'narrow' }).format(new Date(2026, 0, 5 + index))))
const months = computed(() => Array.from({ length: 12 }, (_, index) => new Intl.DateTimeFormat(props.locale, { month: 'short' }).format(new Date(2026, index, 1))))
const years = computed(() => Array.from({ length: 12 }, (_, index) => Math.floor(view.value.getFullYear() / 12) * 12 + index))
const display = computed(() => {
    const date = parseDate(props.modelValue)
    if (!date) return ''
    // 日期统一使用 - 分隔（2026-10-29），不随 locale 变成斜杠格式
    const pad = (value: number) => String(value).padStart(2, '0')
    return `${ date.getFullYear() }-${ pad(date.getMonth() + 1) }-${ pad(date.getDate()) }`
})
const allowed = (key: string) => (!props.min || key >= props.min) && (!props.max || key <= props.max)

function close(): void {
    open.value = false
    trigger.value?.focus()
}
useDismissableLayer(root, open, close, [overlay])
const position = useOverlayPosition({ root, open, placement: () => {
    const rect = root.value?.getBoundingClientRect()
    return rect && rect.bottom + 350 > window.innerHeight && rect.top > 350 ? 'topLeft' : 'bottomLeft'
} })
const panelStyle = computed(() => ({
    ...position.value,
    left: `${Math.max(8, Math.min(Number.parseFloat(position.value.left || '0'), window.innerWidth - 296))}px`,
}))

function toggle(): void {
    if (props.disabled) return
    if (open.value) { close(); return }
    view.value = parseDate(props.modelValue) || new Date()
    focused.value = dateKey(view.value)
    mode.value = 'days'
    open.value = true
    void nextTick(() => dayElements.get(focused.value)?.focus())
}
function choose(value: string): void {
    if (value && !allowed(value)) return
    emit('update:modelValue', value)
    emit('change', value)
    close()
}
function navigate(offset: number): void {
    view.value = mode.value === 'days' ? moveMonth(view.value, offset)
        : new Date(view.value.getFullYear() + offset * (mode.value === 'years' ? 12 : 1), view.value.getMonth(), 1, 12)
}
function remember(date: Date, element: unknown): void {
    if (element) dayElements.set(dateKey(date), element as HTMLElement)
    else dayElements.delete(dateKey(date))
}
function keydown(event: KeyboardEvent, date: Date): void {
    const offsets: Record<string, number> = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }
    let next: Date | undefined
    if (event.key in offsets) next = new Date(date.getFullYear(), date.getMonth(), date.getDate() + offsets[event.key], 12)
    else if (event.key === 'PageUp' || event.key === 'PageDown') next = moveMonth(date, event.key === 'PageUp' ? -1 : 1)
    else if (event.key === 'Home') next = new Date(date.getFullYear(), date.getMonth(), date.getDate() - (date.getDay() + 6) % 7, 12)
    else if (event.key === 'End') next = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 6 - (date.getDay() + 6) % 7, 12)
    if (!next) return
    event.preventDefault()
    if (!allowed(dateKey(next))) return
    view.value = next
    focused.value = dateKey(next)
    void nextTick(() => dayElements.get(focused.value)?.focus())
}
</script>

<template>
    <div ref="root" class="relative inline-flex w-full min-w-0">
        <button ref="trigger" type="button" :disabled="disabled" :aria-label="label" aria-haspopup="dialog" :aria-expanded="open"
                class="inline-flex h-9 w-full min-w-0 items-center justify-between gap-2 rounded-mf-md border bg-surface px-3 text-left text-sm text-content"
                :class="[FOCUS_RING, TRANSITION, DISABLED, open ? 'border-primary ring-2 ring-primary/15' : 'border-line hover:border-line-strong']" @click="toggle">
            <span class="truncate" :class="!display && 'text-content-placeholder'">{{ display || placeholder }}</span>
            <svg class="size-4 shrink-0 text-content-tertiary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M16 3v4M8 3v4M3 11h18"/></svg>
        </button>
        <Teleport to="body">
            <div v-if="open" ref="overlay" role="dialog" :aria-label="label" :style="panelStyle"
                 class="fixed z-60 max-h-[calc(100dvh-16px)] w-72 max-w-[calc(100vw-16px)] overflow-y-auto rounded-mf-lg border border-line bg-surface p-3 text-sm text-content shadow-mf-lg">
                <div class="mb-3 flex items-center justify-between gap-2">
                    <button type="button" :aria-label="previousText" class="inline-flex size-8 items-center justify-center rounded-mf-md hover:bg-surface-hover" :class="FOCUS_RING" @click="navigate(-1)">‹</button>
                    <div class="flex items-center gap-1 font-semibold">
                        <button type="button" class="rounded-mf-md px-2 py-1 hover:bg-surface-hover" :class="FOCUS_RING" @click="mode = mode === 'years' ? 'days' : 'years'">{{ mode === 'years' ? `${years[0]} – ${years[11]}` : view.getFullYear() }}</button>
                        <button v-if="mode !== 'years'" type="button" class="rounded-mf-md px-2 py-1 hover:bg-surface-hover" :class="FOCUS_RING" @click="mode = mode === 'months' ? 'days' : 'months'">{{ months[view.getMonth()] }}</button>
                    </div>
                    <button type="button" :aria-label="nextText" class="inline-flex size-8 items-center justify-center rounded-mf-md hover:bg-surface-hover" :class="FOCUS_RING" @click="navigate(1)">›</button>
                </div>
                <table v-if="mode === 'days'" role="grid" :aria-label="`${view.getFullYear()} ${months[view.getMonth()]}`" class="w-full table-fixed border-separate border-spacing-0.5 text-center">
                    <thead><tr><th v-for="(day, index) in weekdays" :key="index" class="h-8 text-xs font-normal text-content-tertiary">{{ day }}</th></tr></thead>
                    <tbody>
                        <tr v-for="(week, index) in weeks" :key="index">
                            <td v-for="date in week" :key="dateKey(date)" :aria-selected="dateKey(date) === modelValue">
                                <button :ref="element => remember(date, element)" type="button" :data-date="dateKey(date)" :aria-label="dateKey(date)"
                                        :aria-current="dateKey(date) === today() ? 'date' : undefined" :disabled="!allowed(dateKey(date))"
                                        :tabindex="dateKey(date) === focused ? 0 : -1" class="inline-flex size-8 items-center justify-center rounded-mf-md"
                                        :class="[FOCUS_RING, DISABLED, dateKey(date) === modelValue ? 'bg-primary text-on-primary' : 'hover:bg-surface-hover', date.getMonth() !== view.getMonth() && dateKey(date) !== modelValue && 'text-content-tertiary', dateKey(date) === today() && dateKey(date) !== modelValue && 'font-semibold text-primary']"
                                        @focus="focused = dateKey(date)" @click="choose(dateKey(date))" @keydown="keydown($event, date)">{{ date.getDate() }}</button>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div v-else-if="mode === 'months'" class="grid grid-cols-3 gap-2 py-2">
                    <button v-for="(month, index) in months" :key="index" type="button" class="rounded-mf-md px-2 py-3 hover:bg-surface-hover" :class="FOCUS_RING" @click="view = new Date(view.getFullYear(), index, 1, 12); mode = 'days'">{{ month }}</button>
                </div>
                <div v-else class="grid grid-cols-3 gap-2 py-2">
                    <button v-for="year in years" :key="year" type="button" class="rounded-mf-md px-2 py-3 hover:bg-surface-hover" :class="FOCUS_RING" @click="view = new Date(year, view.getMonth(), 1, 12); mode = 'months'">{{ year }}</button>
                </div>
                <div class="mt-2 flex items-center justify-between pt-2">
                    <button type="button" :disabled="!allowed(today())" class="rounded-mf-md px-2 py-1 text-primary hover:bg-surface-hover" :class="[FOCUS_RING, DISABLED]" @click="choose(today())">{{ todayText }}</button>
                    <button type="button" class="rounded-mf-md px-2 py-1 text-content-secondary hover:bg-surface-hover" :class="FOCUS_RING" @click="choose('')">{{ clearText }}</button>
                </div>
            </div>
        </Teleport>
    </div>
</template>
