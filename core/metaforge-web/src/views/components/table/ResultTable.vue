<template>
  <div class="flex min-h-0 flex-col gap-3" :class="fill && 'h-full'">
    <div v-if="searchable" class="flex shrink-0 flex-wrap items-center justify-between gap-3">
      <MfInput v-model="keyword" size="sm" class="w-64" :placeholder="$t('query.result.searchResults')">
        <template #prefix><Search :size="13"/></template>
      </MfInput>
      <span class="text-xs tabular-nums text-content-tertiary">{{ filtered.length.toLocaleString() }} {{ $t('query.result.rowsUnit') }}</span>
    </div>
    <div class="min-h-0 overflow-hidden rounded-mf-md border border-line" :class="fill && 'flex flex-1 flex-col'">
      <MfTable :fill="fill" resizable :columns="columns" :data-source="paged" row-key="__row" density="compact" :bordered="false"
               :empty-text="$t(keyword ? 'query.result.noMatch' : 'common.noData')" @resize="onResize" @resize-end="onResize">
        <template v-for="column in columns" :key="column.key" #[`cell-${column.key}`]="{ record }">
          <slot name="cell" :header="column.title" :value="record[column.dataIndex]">
            <span v-if="record[column.dataIndex] === null || record[column.dataIndex] === undefined" class="text-content-tertiary">NULL</span>
            <div v-else :class="!widths[column.key] && 'max-w-80'"><EllipsisCell :text="cellText(record[column.dataIndex])"/></div>
          </slot>
        </template>
      </MfTable>
    </div>
    <div v-if="filtered.length > pageSize" class="flex shrink-0 justify-end">
      <MfPagination :current="page" :total="filtered.length" :page-size="pageSize" :show-size="false" @update:current="page = $event"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Search } from '@lucide/vue'
import EllipsisCell from './EllipsisCell.vue'

/**
 * Rows of a query result in MfTable, as the query page shows them: resizable columns, NULL for
 * missing values and long values cut with their full text on hover. Rows are objects keyed by
 * header, or arrays in header order.
 */
const props = withDefaults(defineProps<{
  headers: string[]
  rows: any[]
  /** Search box above the table; the search keeps rows where any cell contains the text. */
  searchable?: boolean
  /** Fills the parent's height, the table scrolling inside. */
  fill?: boolean
  pageSize?: number
}>(), { searchable: false, fill: false, pageSize: 100 })

defineSlots<{
  cell?(props: { header: string; value: unknown }): unknown
}>()

const keyword = ref('')
const page = ref(1)
const widths = ref<Record<string, number>>({})

const cellText = (value: unknown) => typeof value === 'object' ? JSON.stringify(value) : String(value)
// Keys carry the position so repeated header names still get their own column.
const columns = computed(() => props.headers.map((header, index) => {
  const key = `${ index }:${ header }`
  return { key, title: header, dataIndex: key, width: widths.value[key], ellipsis: !!widths.value[key] }
}))
const records = computed(() => props.rows.map((row, index) => {
  const record: Record<string, unknown> = { __row: String(index) }
  props.headers.forEach((header, position) => {
    record[`${ position }:${ header }`] = Array.isArray(row) ? row[position] : row?.[header]
  })
  return record
}))
const filtered = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return records.value
  return records.value.filter(record => columns.value.some(column => {
    const value = record[column.dataIndex]
    return value !== null && value !== undefined && cellText(value).toLowerCase().includes(text)
  }))
})
const paged = computed(() => filtered.value.slice((page.value - 1) * props.pageSize, page.value * props.pageSize))
const onResize = (column: { key?: string }, width: number) => { if (column.key) widths.value = { ...widths.value, [column.key]: width } }

watch([keyword, () => props.rows], () => { page.value = 1 })
</script>
