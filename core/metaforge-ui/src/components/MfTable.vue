<script setup lang="ts" generic="T extends Record<string, unknown>">
import { computed } from 'vue'
import clsx from 'clsx'
import MfSpinner from './MfSpinner.vue'

export interface MfTableColumn {
    title: string
    dataIndex: string
    key?: string
    width?: string
    align?: 'left' | 'center' | 'right'
    /** Rendered as `--` when falsy. */
    placeholder?: string
}

const props = withDefaults(defineProps<{
    columns?: MfTableColumn[]
    dataSource?: T[]
    rowKey?: string
    loading?: boolean
    bordered?: boolean
    striped?: boolean
    hoverable?: boolean
  /** Empty-state text. */
    emptyText?: string
}>(), {
    columns: () => [],
    dataSource: () => [],
    rowKey: 'id',
    loading: false,
    bordered: true,
    striped: false,
    hoverable: true,
    emptyText: 'No data',
})

defineEmits<{
    rowClick: [record: T, index: number]
}>()

const alignClass = (align?: string) =>
    align === 'center' ? 'text-center' : align === 'right' ? 'text-right' : 'text-left'

const colKey = (column: MfTableColumn) => column.key ?? column.dataIndex

const rows = computed(() => props.dataSource)

function display(record: T, column: MfTableColumn): unknown {
    return record[column.dataIndex]
}
</script>

<template>
    <div class="w-full overflow-x-auto">
        <table class="w-full border-collapse text-sm" :class="bordered && 'border border-line rounded-mf-lg'">
            <thead>
                <tr class="bg-surface-hover">
                    <th
                        v-for="column in columns"
                        :key="colKey(column)"
                        class="whitespace-nowrap border-b border-line px-3 py-2.5 text-xs font-semibold text-content-secondary"
                        :class="[alignClass(column.align), bordered && 'border-r border-line last:border-r-0']"
                        :style="column.width ? { width: column.width } : {}"
                    >
                        <slot :name="`header-${colKey(column)}`" :column="column">
                            {{ column.title }}
                        </slot>
                    </th>
                </tr>
            </thead>
            <tbody v-if="!loading">
                <tr
                    v-for="(record, index) in rows"
                    :key="String(record[rowKey] ?? index)"
                    class="transition-[background-color] duration-(--mf-duration-fast)"
                    :class="clsx(
                        striped && index % 2 === 1 && 'bg-surface-hover/40',
                        hoverable && 'hover:bg-surface-hover',
                        'border-b border-line last:border-b-0',
                    )"
                    @click="$emit('rowClick', record, index)"
                >
                    <td
                        v-for="column in columns"
                        :key="colKey(column)"
                        class="px-3 py-2.5 text-content"
                        :class="[alignClass(column.align), bordered && 'border-r border-line last:border-r-0']"
                    >
                        <slot
                            :name="`cell-${colKey(column)}`"
                            :record="record"
                            :column="column"
                            :index="index"
                            :text="display(record, column)"
                        >
                            {{ display(record, column) ?? column.placeholder ?? '--' }}
                        </slot>
                    </td>
                </tr>
                <tr v-if="rows.length === 0">
                    <td :colspan="columns.length" class="px-3 py-8 text-center text-sm text-content-tertiary">
                        <slot name="emptyText">{{ emptyText }}</slot>
                    </td>
                </tr>
            </tbody>
        </table>
        <div v-if="loading" class="flex items-center justify-center py-10">
            <MfSpinner label="Loading..." />
        </div>
    </div>
</template>
