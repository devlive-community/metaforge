<script setup lang="ts">
import { computed } from 'vue'
import clsx from 'clsx'
import MfSelect from './MfSelect.vue'

const props = withDefaults(defineProps<{
    current?: number
    total?: number
    pageSize?: number
    pageSizes?: number[]
    showSize?: boolean
    showTotal?: boolean
    simple?: boolean
    /** Page-size option label; `{size}` is replaced with the number. */
    sizeText?: string
}>(), {
    current: 1,
    total: 0,
    pageSize: 10,
    pageSizes: () => [10, 20, 50, 100],
    showSize: true,
    showTotal: false,
    simple: false,
    sizeText: '{size} / page',
})

const emit = defineEmits<{
    'update:current': [page: number]
    'update:pageSize': [size: number]
    change: [page: number, pageSize: number]
}>()

const pages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))

function go(page: number): void {
    const next = Math.min(Math.max(1, page), pages.value)
    if (next !== props.current) {
        emit('update:current', next)
        emit('change', next, props.pageSize)
    }
}

function changeSize(value: string | number | (string | number)[]): void {
    const size = Number(value)
    emit('update:pageSize', size)
    emit('change', 1, size)
}

const sizeOptions = computed(() => props.pageSizes.map(size => ({ label: props.sizeText.replace('{size}', String(size)), value: size })))

const items = computed<(number | '...')[]>(() => {
    const total = pages.value
    const current = props.current
    if (total <= 7) {
        return Array.from({ length: total }, (_, i) => i + 1)
    }
    const set = new Set<number>([1, total, current, current - 1, current + 1])
    const list: (number | '...')[] = []
    let prev = 0
    for (let i = 1; i <= total; i++) {
        if (set.has(i)) {
            if (i - prev > 1) list.push('...')
            list.push(i)
            prev = i
        }
    }
    return list
})

function itemClass(item: number | '...'): string {
    return clsx(
        'inline-flex h-8 min-w-8 items-center justify-center rounded-mf-md px-2 text-sm select-none',
        item === '...'
            ? 'text-content-tertiary cursor-default'
            : item === props.current
                ? 'bg-primary text-on-primary font-medium cursor-default'
                : 'cursor-pointer border border-line bg-surface text-content-secondary hover:border-line-strong hover:text-content transition-[color,border-color] duration-(--mf-duration-fast)',
    )
}
</script>

<template>
    <nav v-if="total > 0" class="flex flex-wrap items-center gap-1.5 text-sm" aria-label="pagination">
        <button
            type="button"
            class="inline-flex size-8 items-center justify-center rounded-mf-md border border-line bg-surface text-content-secondary hover:border-line-strong hover:text-content disabled:pointer-events-none disabled:opacity-40"
            :disabled="current <= 1"
            aria-label="previous"
            @click="go(current - 1)"
        >
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M15 18l-6-6 6-6" /></svg>
        </button>

        <template v-if="simple">
            <span class="px-2 text-content-secondary">{{ current }} / {{ pages }}</span>
        </template>
        <template v-else>
            <span v-for="(item, index) in items" :key="`${item}-${index}`" :class="itemClass(item)" @click="item !== '...' && item !== current && go(item)">
                {{ item }}
            </span>
        </template>

        <button
            type="button"
            class="inline-flex size-8 items-center justify-center rounded-mf-md border border-line bg-surface text-content-secondary hover:border-line-strong hover:text-content disabled:pointer-events-none disabled:opacity-40"
            :disabled="current >= pages"
            aria-label="next"
            @click="go(current + 1)"
        >
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M9 6l6 6-6 6" /></svg>
        </button>

        <div v-if="showSize" class="ml-2 w-28">
            <MfSelect
                :model-value="pageSize"
                :options="sizeOptions"
                @update:model-value="changeSize"
            />
        </div>

        <span v-if="showTotal" class="ml-1 text-content-tertiary">{{ total }}</span>
    </nav>
</template>
