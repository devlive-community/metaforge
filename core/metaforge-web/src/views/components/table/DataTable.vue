<template>
  <div class="dc-table">
    <div v-if="title || description" class="dc-table__head">
      <div class="dc-table__head-main">
        <h1 v-if="title" class="m-0 text-xl leading-7 font-semibold text-content">{{ title }}</h1>
        <p v-if="description" class="mt-1 mb-0 text-sm leading-5 text-content-secondary">{{ description }}</p>
      </div>
    </div>

    <MfTableToolbar :loading="loading"
                    :search-text="t('dataTable.search')"
                    :reset-text="t('common.reset')"
                    :more-text="t('dataTable.moreFilters')"
                    :less-text="t('dataTable.lessFilters')"
                    @search="onSearch"
                    @reset="onReset">
      <template v-if="filterable || $slots.filter" #filter>
        <slot name="filter">
          <TableFilterControl v-for="field in basicFilters" :key="field.key" :field="field" :model-value="filterValue(field.key)" @update:model-value="setFilter(field, $event)"/>
        </slot>
      </template>
      <template v-if="advancedFilters.length || $slots['advanced-filter']" #advanced-filter>
        <slot name="advanced-filter">
          <TableFilterControl v-for="field in advancedFilters" :key="field.key" :field="field" :model-value="filterValue(field.key)" @update:model-value="setFilter(field, $event)"/>
        </slot>
      </template>
      <template v-if="$slots.actions" #actions><slot name="actions"/></template>
      <template #tools>
        <slot name="tools"/>
        <MfTooltip :content="t('common.refresh')">
          <MfButton :aria-label="t('common.refresh')" :disabled="loading" @click="emit('refresh')">
            <RotateCw :size="14"/>
          </MfButton>
        </MfTooltip>

        <MfPopover trigger="click" placement="bottomRight">
          <template #content>
            <div class="dc-table__columns">
              <MfCheckbox v-for="col in toggleableColumns"
                          :key="col.key"
                          :model-value="!hiddenKeys.has(col.key)"
                          @update:model-value="(checked: any) => toggleColumn(col.key, !!checked)">
                {{ col.title }}
              </MfCheckbox>
            </div>
          </template>
          <MfTooltip :content="columnsText">
            <MfButton :aria-label="columnsText">
              <Settings :size="14"/>
            </MfButton>
          </MfTooltip>
        </MfPopover>

        <MfTooltip :content="downloadText">
          <MfButton :aria-label="downloadText" :disabled="loading || dataSource.length === 0" @click="onDownload">
            <DownloadIcon :size="14"/>
          </MfButton>
        </MfTooltip>
      </template>
    </MfTableToolbar>
    <p v-if="filterError" role="alert" class="mb-3 text-sm text-danger">{{ filterError }}</p>

    <!-- 表格 + 分页 -->
    <MfTable :columns="tableColumns"
             :data-source="dataSource"
             :loading="loading"
             :row-key="rowKey"
             :empty-text="$t('common.noData')">
      <!-- 行多选：复选框列 -->
      <template v-if="rowSelection" #header-__selection__>
        <MfCheckbox :model-value="allSelected"
                    :indeterminate="indeterminate"
                    @update:model-value="(checked: any) => onToggleSelectAll(!!checked)"/>
      </template>
      <template v-if="rowSelection" #cell-__selection__="{ record }">
        <MfCheckbox :model-value="selectedRowKeys.includes(record[rowKey])"
                    @update:model-value="() => onToggleRow(record)"/>
      </template>

      <!-- bodyCell 插槽契约透传：与原 a-table 的 #bodyCell="{ column, record, text }" 保持一致 -->
      <template v-for="col in displayColumns" :key="col.key" #[`cell-${col.key}`]="cellProps">
        <slot name="bodyCell" v-bind="cellProps">{{ cellProps.text }}</slot>
      </template>
    </MfTable>

    <!-- 页脚：已选择 + 批量操作 | 分页 -->
    <div class="dc-table__footer">
      <div v-if="rowSelection" class="dc-table__batch">
        <span class="dc-table__selected">{{ $t('dataTable.selected', { count: selectedRowKeys.length }) }}</span>
        <MfButton v-for="action in batchActions"
                  :key="action.key"
                  size="sm"
                  :variant="action.danger ? 'danger' : 'secondary'"
                  :disabled="selectedRowKeys.length === 0 || loading"
                  @click="emit('batch', action.key, selectedRows)">
          {{ action.label }}
        </MfButton>
      </div>

      <div v-if="total > 0" class="dc-table__pagination">
        <span class="dc-table__total">{{ $t('dataTable.total', { count: total }) }}</span>
        <MfPagination :current="pageIndex"
                      :page-size="pageSize"
                      :total="total"
                      :page-sizes="pageSizeOptions.map(Number)"
                      @change="onPageChange"/>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { DownloadIcon, RotateCw, Settings } from '@lucide/vue'
import { emptyTableFilters, type TableFilters } from '@/model/filter'
import TableFilterControl from './TableFilterControl.vue'
import type { TableFilterField } from './tableFilters'

interface TableColumn
{
  key: string
  title?: string
  dataIndex?: string

  [prop: string]: any
}

export interface TableBatchAction
{
  key: string
  label: string
  danger?: boolean
}

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  columns: TableColumn[]
  dataSource: any[]
  loading?: boolean
  pageIndex?: number
  pageSize?: number
  total?: number
  rowKey?: string
  size?: 'small' | 'middle' | 'large'
  pageSizeOptions?: string[]
  /** 不允许在“列设置”里隐藏的列（如操作列） */
  lockedColumnKeys?: string[]
  /** 是否启用行多选（复选框列 + 底部已选择计数） */
  rowSelection?: boolean
  /** 批量操作按钮（行多选启用时渲染于表格底部） */
  batchActions?: TableBatchAction[]
  filterable?: boolean
  filterFields?: TableFilterField[]
}>(), {
  title: '',
  description: '',
  loading: false,
  pageIndex: 1,
  pageSize: 10,
  total: 0,
  rowKey: 'id',
  size: 'small',
  pageSizeOptions: () => ['10', '20', '50'],
  lockedColumnKeys: () => ['action'],
  rowSelection: false,
  batchActions: () => [],
  filterable: false,
  filterFields: () => [],
})

const emit = defineEmits<{
  (e: 'refresh'): void
  (e: 'search', filters: TableFilters): void
  (e: 'reset', filters: TableFilters): void
  (e: 'download'): void
  (e: 'page-change', page: number, size: number): void
  (e: 'size-change', current: number, size: number): void
  (e: 'batch', key: string, rows: any[]): void
  (e: 'selection-change', keys: any[], rows: any[]): void
}>()

const { t } = useI18n()
const draft = reactive(emptyTableFilters())
const filterError = ref('')
const basicFilters = computed(() => props.filterFields.filter(field => !field.advanced))
const advancedFilters = computed(() => props.filterFields.filter(field => field.advanced))
const directFields = ['keyword', 'code', 'createdFrom', 'createdTo'] as const
const isDirect = (key: string): key is typeof directFields[number] => directFields.some(field => field === key)
const filterValue = (key: string) => isDirect(key) ? draft[key] : draft.conditions[key]?.value || ''
const setFilter = (field: TableFilterField, value: string) => {
  if (isDirect(field.key)) draft[field.key] = value
  else if (!value) delete draft.conditions[field.key]
  else draft.conditions[field.key] = { value, exact: field.type === 'select' }
}
const snapshot = () => ({ ...draft, conditions: Object.fromEntries(Object.entries(draft.conditions).map(([key, value]) => [key, { ...value }])) })
const onSearch = () => {
  if (draft.createdFrom && draft.createdTo && draft.createdFrom > draft.createdTo) {
    filterError.value = t('dataTable.invalidDateRange')
    return
  }
  filterError.value = ''
  emit('search', snapshot())
}
const onReset = () => {
  Object.assign(draft, emptyTableFilters())
  filterError.value = ''
  emit('reset', snapshot())
}

// download / columns 键在服务端 i18n 可能尚未提供，缺失时回退到中性文案
const columnsText = computed(() => {
  const value = t('common.columns')
  return value === 'common.columns' ? 'Columns' : value
})
const downloadText = computed(() => {
  const value = t('common.download')
  return value === 'common.download' ? 'Download' : value
})

const hiddenKeys = ref<Set<string>>(new Set())

// 可在“列设置”里切换显隐的列：有 title、且不在锁定列表内
const toggleableColumns = computed(() =>
  props.columns.filter(col => col.key && col.title && !props.lockedColumnKeys.includes(col.key))
)

const displayColumns = computed(() =>
  props.columns
       .filter(col => !hiddenKeys.value.has(col.key))
       // 操作等锁定列始终固定在右侧，配合 MfTable 的横向滚动
       .map(col => (props.lockedColumnKeys.includes(col.key) && !col.fixed ? { ...col, fixed: 'right' } : col))
)

const toggleColumn = (key: string, checked: boolean) => {
  const next = new Set(hiddenKeys.value)
  if (checked) {
    next.delete(key)
  }
  else {
    next.add(key)
  }
  hiddenKeys.value = next
}

// 行多选内部复选框列
const SELECTION_KEY = '__selection__'

const tableColumns = computed<any[]>(() => {
  const columns = displayColumns.value
  return props.rowSelection
      ? [{ key: SELECTION_KEY, dataIndex: SELECTION_KEY, title: '', width: '40px' }, ...columns]
      : columns
})

// ---- 行多选 ----
const selectedRowKeys = ref<any[]>([])

const rowKeyValue = (row: any) => row[props.rowKey as string]

const allSelected = computed(() =>
  props.dataSource.length > 0 && props.dataSource.every((row) => selectedRowKeys.value.includes(rowKeyValue(row)))
)

const indeterminate = computed(() =>
  !allSelected.value && props.dataSource.some((row) => selectedRowKeys.value.includes(rowKeyValue(row)))
)

const applySelection = (keys: any[]) => {
  selectedRowKeys.value = keys
  emit('selection-change', keys, props.dataSource.filter((row) => keys.includes(rowKeyValue(row))))
}

const onToggleRow = (record: any) => {
  const key = rowKeyValue(record)
  applySelection(selectedRowKeys.value.includes(key)
      ? selectedRowKeys.value.filter((k) => k !== key)
      : [...selectedRowKeys.value, key])
}

const onToggleSelectAll = (checked: boolean) => {
  const keys = props.dataSource.map(rowKeyValue)
  applySelection(checked
      ? Array.from(new Set([...selectedRowKeys.value, ...keys]))
      : selectedRowKeys.value.filter((k) => !keys.includes(k)))
}

const selectedRows = computed(() =>
  props.dataSource.filter(row => selectedRowKeys.value.includes(row[props.rowKey as string]))
)

// 数据刷新后清理已不存在的选中项
watch(() => props.dataSource, () => {
  const keys = new Set(props.dataSource.map(row => row[props.rowKey as string]))
  selectedRowKeys.value = selectedRowKeys.value.filter(key => keys.has(key))
})

// 分页：页码点击与每页条数变更都通过 change 触发；条数变化时同时对外发出 size-change（对齐原 a-pagination 行为）
const onPageChange = (page: number, size: number) => {
  if (size !== props.pageSize) {
    emit('size-change', page, size)
  }
  emit('page-change', page, size)
}

// 默认下载：把当前页数据按可见列导出为 CSV；父组件可监听 @download 覆盖
const onDownload = () => {
  emit('download')

  const cols = displayColumns.value.filter(col => col.dataIndex)
  if (!cols.length || !props.dataSource.length) {
    return
  }
  const escape = (v: any) => {
    const s = v === null || v === undefined ? '' : String(v)
    return /[",\n]/.test(s) ? `"${ s.replace(/"/g, '""') }"` : s
  }
  const header = cols.filter(col => col.dataIndex).map(col => escape(col.title))
  const rows = props.dataSource.map(row => cols.map(col => escape(row[col.dataIndex as string])))
  const csv = [header.join(','), ...rows.map(r => r.join(','))].join('\n')

  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `export-${ Date.now() }.csv`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.dc-table__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
}

.dc-table__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    padding: 12px 0 4px;
}

.dc-table__batch {
    display: flex;
    align-items: center;
    gap: 8px;
}

.dc-table__selected {
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-table__pagination {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-left: auto;
}

.dc-table__total {
    font-size: 12px;
    color: var(--dc-text-secondary);
    white-space: nowrap;
}

.dc-table__columns {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 150px;
    max-height: 320px;
    overflow: auto;
}
</style>
