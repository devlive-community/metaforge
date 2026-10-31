<script setup lang="ts" generic="T extends Record<string, unknown>">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import clsx from 'clsx'
import MfSpinner from './MfSpinner.vue'
import MfEmpty from './MfEmpty.vue'

export interface MfTableColumn {
    title: string
    dataIndex: string
    key?: string
    width?: string | number
    align?: 'left' | 'center' | 'right'
    /** Pin the column while the table scrolls horizontally. */
    fixed?: 'left' | 'right'
    /**
     * Clamp the column to its width and truncate overflowing text;
     * pair it with a tooltip cell so the full content stays reachable.
     */
    ellipsis?: boolean
    /** Rendered as `--` when falsy. */
    placeholder?: string
    /** Opt out of header drag-resizing when the table is `resizable`. */
    resizable?: boolean
}

const props = withDefaults(defineProps<{
    columns?: MfTableColumn[]
    dataSource?: T[]
    rowKey?: string
    loading?: boolean
    bordered?: boolean
    striped?: boolean
    hoverable?: boolean
    /** Horizontal lines between rows. */
    rowBorders?: boolean
    density?: 'default' | 'compact'
    rowClass?: (record: T, index: number) => string
    /** Scroll rows inside a bounded workspace while keeping the header visible. */
    fill?: boolean
  /** Empty-state text. */
    emptyText?: string
    emptyDescription?: string
    emptyVariant?: 'data' | 'search' | 'initial'
    /** Drag a header edge to resize a column; double-click it to fit the content. */
    resizable?: boolean
}>(), {
    columns: () => [],
    dataSource: () => [],
    rowKey: 'id',
    loading: false,
    bordered: true,
    striped: false,
    hoverable: true,
    rowBorders: true,
    density: 'default',
    fill: false,
    emptyText: 'No data',
    emptyDescription: '',
    emptyVariant: 'data',
    resizable: false,
})

const emit = defineEmits<{
    rowClick: [record: T, index: number, event: MouseEvent]
    /** Live width while dragging a header edge. */
    resize: [column: MfTableColumn, width: number]
    /** Final width after a drag or a double-click fit. */
    resizeEnd: [column: MfTableColumn, width: number]
}>()

const alignClass = (align?: string) =>
    align === 'center' ? 'text-center' : align === 'right' ? 'text-right' : 'text-left'

const colKey = (column: MfTableColumn) => column.key ?? column.dataIndex

// A declared width is also the column's minimum: truncating cells would otherwise let a full-width
// table squeeze every column to fit instead of scrolling sideways.
const widthStyle = (column: MfTableColumn) => {
    if (column.width === undefined) return {}
    const width = typeof column.width === 'number' ? `${column.width}px` : column.width
    return { width, minWidth: width }
}

// Sticky cells need an opaque background; striped rows blend surface-hover over
// surface, so the striped sticky cell uses the pre-mixed solid color.
const STRIPED_SOLID = 'bg-[color-mix(in_srgb,var(--mf-color-surface-hover)_40%,var(--mf-color-surface))]'

const rows = computed(() => props.dataSource)

function display(record: T, column: MfTableColumn): unknown {
    return record[column.dataIndex]
}

const rowClasses = (record: T, index: number) => clsx(
    props.striped && index % 2 === 1 && 'bg-surface-hover/40',
    props.hoverable && 'hover:bg-surface-hover',
    props.rowBorders && 'border-b border-line last:border-b-0',
    props.rowClass?.(record, index),
)

const tdClass = (column: MfTableColumn, index: number) => [
    'whitespace-nowrap px-3 text-content',
    alignClass(column.align),
    props.density === 'compact' ? 'py-1.5' : 'py-2.5',
    props.bordered && 'border-r border-line last:border-r-0',
    column.ellipsis && 'max-w-0 overflow-hidden',
    column.fixed === 'right' && clsx('sticky right-0 z-10 border-l border-line', props.striped && index % 2 === 1 ? STRIPED_SOLID : 'bg-surface', props.hoverable && 'group-hover:bg-surface-hover'),
    column.fixed === 'left' && clsx('sticky left-0 z-10 border-r border-line', props.striped && index % 2 === 1 ? STRIPED_SOLID : 'bg-surface', props.hoverable && 'group-hover:bg-surface-hover'),
]

const thClass = (column: MfTableColumn) => [
    'whitespace-nowrap px-3 text-xs font-semibold text-content-secondary',
    alignClass(column.align),
    props.density === 'compact' ? 'py-2' : 'py-2.5',
    !props.fill && 'border-b border-line',
    props.bordered && 'border-r border-line last:border-r-0',
    column.fixed === 'right' && 'sticky right-0 z-10 bg-surface-hover border-l border-line',
    column.fixed === 'left' && 'sticky left-0 z-10 bg-surface-hover border-r border-line',
    !column.fixed && canResize(column) && 'relative',
]

/*
 * Fill mode splits the header from the scrolling body so the scrollbar only runs beside the rows.
 * The body keeps an invisible copy of the header row: it sizes the columns like a single table
 * would, and its measured cell widths are applied to the visible header.
 */
const headScroller = ref<HTMLElement>()
const bodyScroller = ref<HTMLElement>()
const bodyTable = ref<HTMLTableElement>()
const headWidths = ref<number[]>([])
const headHeight = ref(0)
const bodyWidth = ref(0)
const gutter = ref(0)

function measure(): void {
    const row = bodyTable.value?.tHead?.rows[0]
    const scroller = bodyScroller.value
    if (!row || !scroller) return
    headWidths.value = Array.from(row.cells).map(cell => cell.getBoundingClientRect().width)
    headHeight.value = row.getBoundingClientRect().height
    bodyWidth.value = bodyTable.value!.getBoundingClientRect().width
    gutter.value = scroller.offsetWidth - scroller.clientWidth
    syncScroll()
}

function syncScroll(): void {
    if (headScroller.value && bodyScroller.value) headScroller.value.scrollLeft = bodyScroller.value.scrollLeft
}

// Several pinned columns stack: each one sticks after the pinned columns before it (or after it, on the right).
const stickyOffsets = computed(() => {
    const widthOf = (index: number) => headWidths.value[index] ?? (typeof props.columns[index].width === 'number' ? props.columns[index].width as number : 0)
    const offsets: Record<number, { left?: string; right?: string }> = {}
    let left = 0
    props.columns.forEach((column, index) => {
        if (column.fixed !== 'left') return
        offsets[index] = { left: `${left}px` }
        left += widthOf(index)
    })
    let right = 0
    for (let index = props.columns.length - 1; index >= 0; index--) {
        if (props.columns[index].fixed !== 'right') continue
        offsets[index] = { right: `${right}px` }
        right += widthOf(index)
    }
    return offsets
})
const stickyStyle = (index: number) => stickyOffsets.value[index] ?? {}

let observer: ResizeObserver | undefined
onMounted(() => {
    if (!props.fill || typeof ResizeObserver === 'undefined') return
    observer = new ResizeObserver(measure)
    if (bodyTable.value) observer.observe(bodyTable.value)
    if (bodyScroller.value) observer.observe(bodyScroller.value)
    measure()
})
watch(() => [props.columns, props.dataSource, props.loading], () => { if (props.fill) nextTick(measure) }, { deep: false })
onBeforeUnmount(() => { observer?.disconnect(); stopResize() })

const MIN_WIDTH = 40
const MAX_WIDTH = 800
const plainTable = ref<HTMLTableElement>()
const resizingKey = ref<string | null>(null)
const canResize = (column: MfTableColumn) => props.resizable && column.resizable !== false
const clampWidth = (width: number) => Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, Math.round(width)))
let stopResize = () => {}

function startResize(event: PointerEvent, column: MfTableColumn): void {
    const cell = (event.currentTarget as HTMLElement).parentElement
    if (!cell) return
    const startX = event.clientX
    const startWidth = cell.getBoundingClientRect().width
    let width = startWidth
    resizingKey.value = colKey(column)
    const move = (moveEvent: PointerEvent) => {
        width = clampWidth(startWidth + moveEvent.clientX - startX)
        emit('resize', column, width)
    }
    const up = () => {
        stopResize()
        if (Math.round(width) !== Math.round(startWidth)) emit('resizeEnd', column, clampWidth(width))
    }
    stopResize = () => {
        resizingKey.value = null
        window.removeEventListener('pointermove', move)
        window.removeEventListener('pointerup', up)
        window.removeEventListener('pointercancel', up)
        stopResize = () => {}
    }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', up)
    window.addEventListener('pointercancel', up)
}

/**
 * The width of a cell's rendered text plus its padding. Only text is measured: block wrappers
 * (truncating spans, tooltips) take the current column width and would never let it shrink.
 */
function contentWidth(cell: HTMLElement): number {
    const range = document.createRange()
    const walker = document.createTreeWalker(cell, NodeFilter.SHOW_TEXT)
    let widest = 0
    for (let node = walker.nextNode(); node; node = walker.nextNode()) {
        if (!node.textContent?.trim()) continue
        range.selectNodeContents(node)
        widest = Math.max(widest, range.getBoundingClientRect().width)
    }
    const style = getComputedStyle(cell)
    // Room for tag padding and the resize handle around the text.
    return widest + parseFloat(style.paddingLeft) + parseFloat(style.paddingRight) + 12
}

/** Double-click on a header edge: fit the column to its header and every rendered cell. */
function autofit(column: MfTableColumn, index: number): void {
    const header = props.fill ? headScroller.value?.querySelector('thead tr') : plainTable.value?.tHead?.rows[0]
    const body = props.fill ? bodyTable.value?.tBodies[0] : plainTable.value?.tBodies[0]
    const cells = [header?.children[index], ...Array.from(body?.rows ?? []).map(row => row.cells[index])]
        .filter((cell): cell is HTMLElement => cell instanceof HTMLElement)
    if (!cells.length) return
    emit('resizeEnd', column, clampWidth(Math.max(...cells.map(contentWidth))))
}
</script>

<template>
    <!-- A filled table lives in a framed pane, so `bordered` only draws the cell borders here; an
         outer frame of its own doubled the pane's border and left gaps where the two radii met. -->
    <div v-if="fill" class="flex min-h-0 w-full flex-1 flex-col overflow-hidden">
        <div class="flex shrink-0 border-b border-line bg-surface-hover">
            <div ref="headScroller" class="min-w-0 flex-1 overflow-hidden">
                <table class="table-fixed border-collapse text-sm" :style="{ width: bodyWidth ? `${bodyWidth}px` : '100%' }">
                    <colgroup v-if="headWidths.length === columns.length">
                        <col v-for="(width, index) in headWidths" :key="index" :style="{ width: `${width}px` }">
                    </colgroup>
                    <thead>
                        <tr>
                            <th v-for="(column, columnIndex) in columns" :key="colKey(column)" :class="thClass(column)" :style="[widthStyle(column), stickyStyle(columnIndex)]">
                                <slot :name="`header-${colKey(column)}`" :column="column">{{ column.title }}</slot>
                                <span v-if="canResize(column)" class="absolute inset-y-0 -right-px z-30 w-1.5 cursor-col-resize touch-none transition-colors hover:bg-primary/40"
                                      :class="resizingKey === colKey(column) && 'bg-primary/40'" role="separator" aria-orientation="vertical" :aria-label="`resize ${column.title}`"
                                      @pointerdown.prevent.stop="startResize($event, column)" @dblclick.stop="autofit(column, columnIndex)" @click.stop />
                            </th>
                        </tr>
                    </thead>
                </table>
            </div>
            <div class="shrink-0" :style="{ width: `${gutter}px` }" />
        </div>
        <div ref="bodyScroller" class="flex min-h-0 flex-1 flex-col overflow-auto" @scroll="syncScroll">
            <table ref="bodyTable" class="w-full shrink-0 border-collapse text-sm" :style="{ marginTop: `-${headHeight}px` }">
                <thead class="invisible" aria-hidden="true">
                    <tr>
                        <th v-for="(column, columnIndex) in columns" :key="colKey(column)" :class="thClass(column)" :style="[widthStyle(column), stickyStyle(columnIndex)]">
                            <slot :name="`header-${colKey(column)}`" :column="column">{{ column.title }}</slot>
                        </th>
                    </tr>
                </thead>
                <tbody v-if="!loading">
                    <tr
                        v-for="(record, index) in rows"
                        :key="String(record[rowKey] ?? index)"
                        :data-row-key="String(record[rowKey] ?? index)"
                        class="group transition-[background-color] duration-(--mf-duration-fast)"
                        :class="rowClasses(record, index)"
                        @click="$emit('rowClick', record, index, $event)"
                    >
                        <td v-for="(column, columnIndex) in columns" :key="colKey(column)" :class="tdClass(column, index)" :style="stickyStyle(columnIndex)">
                            <slot :name="`cell-${colKey(column)}`" :record="record" :column="column" :index="index" :text="display(record, column)">
                                {{ display(record, column) ?? column.placeholder ?? '--' }}
                            </slot>
                        </td>
                    </tr>
                </tbody>
            </table>
            <!-- The empty and loading states fill the space under the header, centered and pinned to the
                 visible width while the table scrolls sideways. -->
            <div v-if="loading || rows.length === 0" class="sticky left-0 flex w-full min-h-40 flex-1 items-center justify-center px-3 py-6">
                <MfSpinner v-if="loading" label="Loading..." />
                <slot v-else name="emptyText">
                    <MfEmpty :title="emptyText" :description="emptyDescription" :variant="emptyVariant" size="sm"/>
                </slot>
            </div>
        </div>
    </div>
    <div v-else class="w-full overflow-x-auto">
        <table ref="plainTable" class="w-full border-collapse text-sm" :class="bordered && 'border border-line rounded-mf-lg'">
            <thead>
                <tr class="bg-surface-hover">
                    <th v-for="(column, columnIndex) in columns" :key="colKey(column)" :class="thClass(column)" :style="[widthStyle(column), stickyStyle(columnIndex)]">
                        <slot :name="`header-${colKey(column)}`" :column="column">{{ column.title }}</slot>
                        <span v-if="canResize(column)" class="absolute inset-y-0 -right-px z-30 w-1.5 cursor-col-resize touch-none transition-colors hover:bg-primary/40"
                              :class="resizingKey === colKey(column) && 'bg-primary/40'" role="separator" aria-orientation="vertical" :aria-label="`resize ${column.title}`"
                              @pointerdown.prevent.stop="startResize($event, column)" @dblclick.stop="autofit(column, columnIndex)" @click.stop />
                    </th>
                </tr>
            </thead>
            <tbody v-if="!loading">
                <tr
                    v-for="(record, index) in rows"
                    :key="String(record[rowKey] ?? index)"
                    :data-row-key="String(record[rowKey] ?? index)"
                    class="group transition-[background-color] duration-(--mf-duration-fast)"
                    :class="rowClasses(record, index)"
                    @click="$emit('rowClick', record, index, $event)"
                >
                    <td v-for="(column, columnIndex) in columns" :key="colKey(column)" :class="tdClass(column, index)" :style="stickyStyle(columnIndex)">
                        <slot :name="`cell-${colKey(column)}`" :record="record" :column="column" :index="index" :text="display(record, column)">
                            {{ display(record, column) ?? column.placeholder ?? '--' }}
                        </slot>
                    </td>
                </tr>
                <tr v-if="rows.length === 0">
                    <td :colspan="Math.max(columns.length, 1)" class="px-3 py-3 text-center text-sm text-content-tertiary">
                        <slot name="emptyText">
                            <MfEmpty :title="emptyText" :description="emptyDescription" :variant="emptyVariant" size="sm"/>
                        </slot>
                    </td>
                </tr>
            </tbody>
        </table>
        <div v-if="loading" class="flex items-center justify-center py-10">
            <MfSpinner label="Loading..." />
        </div>
    </div>
</template>
