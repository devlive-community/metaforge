<template>
  <div class="flex min-h-0 flex-1 flex-col overflow-hidden">
    <div class="flex shrink-0 flex-wrap items-center justify-between gap-2 border-b border-line py-1 pl-2 pr-3">
      <div class="flex items-center gap-1">
        <button v-for="tab in tabs"
                :key="tab.key"
                type="button"
                class="shrink-0 whitespace-nowrap border-b-2 px-3 py-2.5 text-sm font-medium transition-[color,border-color] duration-(--mf-duration-base) ease-(--mf-easing)"
                :class="activeTab === tab.key ? 'border-b-primary text-primary' : 'border-b-transparent text-content-secondary hover:text-content'"
                @click="activeTab = tab.key">
          {{ tab.label }}
        </button>


      </div>

      <div class="flex max-w-full flex-wrap items-center gap-2">
        <MfButton size="sm" :disabled="!exportable" @click="onExport">
          <Download :size="14"/>
          {{ $t('common.export') }}
        </MfButton>

        <MfButton size="sm" :disabled="!exportable" @click="visualVisible = true">
          <ChartColumn :size="14"/>
          {{ $t('dataset.common.visual') }}
        </MfButton>
        <MfButton size="sm" variant="primary" :disabled="!canAddDataset" @click="emit('add-dataset')"><Plus :size="14"/>{{ $t('dataset.common.create') }}</MfButton>
        <MfDropdown trigger="click" placement="bottomRight"><MfButton size="sm" :disabled="!canAddDataset" :aria-label="$t('dataset.prototype.moreCreation')">···</MfButton><template #overlay><button type="button" class="rounded-mf-md px-3 py-2 text-sm text-content hover:bg-surface-hover" @click="emit('existing-dataset')">{{ $t('dataset.prototype.applyExisting') }}</button></template></MfDropdown>
      </div>
    </div>

    <div v-if="hasResponse" class="flex shrink-0 items-center justify-between border-b border-line px-3 py-1.5">
      <div class="flex min-w-0 items-center gap-3">
        <span class="flex items-center gap-1.5 text-sm font-medium"
              :class="isSuccess ? 'text-success' : 'text-danger'">
          <CircleCheck v-if="isSuccess" :size="15"/>
          <CircleAlert v-else :size="15"/>
          {{ isSuccess ? $t('query.message.success') : $t('query.message.failed') }}
        </span>

        <span class="h-3.5 w-px bg-line"></span>

        <span class="text-xs text-content-secondary">
          {{ $t('query.message.elapsed') }} {{ elapsed }} ms
        </span>

        <span class="h-3.5 w-px bg-line"></span>

        <span class="text-xs text-content-secondary">
          {{ $t('query.message.returnedRows', [rowCount]) }}
        </span>
      </div>

      <div class="flex items-center gap-2">
        <MfInput v-model="quickFilter" size="sm" class="w-48" :placeholder="$t('query.result.searchResults')" clearable>
          <template #prefix>
            <Search :size="13"/>
          </template>
        </MfInput>

        <MfTooltip :content="$t('common.refresh')">
          <MfButton variant="ghost" size="sm" shape="circle" @click="emit('refresh')">
            <RefreshCw :size="14"/>
          </MfButton>
        </MfTooltip>
      </div>
    </div>

    <div class="relative min-h-0 flex-1">
      <div v-if="running" class="absolute inset-0 z-10 flex items-center justify-center bg-surface/70">
        <MfSpinner size="lg" :label="$t('query.common.execute') + '...'"/>
      </div>

      <template v-if="activeTab === 'results'">
        <div v-if="hasRows" class="flex h-full min-h-0 flex-col">
          <MfTable fill resizable :columns="tableColumns" :data-source="pagedRows" row-key="__row" density="compact" :bordered="false"
                   :empty-text="$t('query.result.noMatch')" @resize="onResize" @resize-end="onResize">
            <template v-for="column in tableColumns" :key="column.key" #[`cell-${column.key}`]="{ record }">
              <span v-if="record[column.dataIndex] === null || record[column.dataIndex] === undefined" class="text-content-tertiary">NULL</span>
              <div v-else :class="!widths[column.key] && 'max-w-80'"><EllipsisCell :text="cellText(record[column.dataIndex])"/></div>
            </template>
          </MfTable>
        </div>
        <MfEmpty v-else-if="resultState === 'initial'" class="h-full" variant="initial"
                 :title="$t('query.result.initial')" :description="$t('query.result.initialTip')"/>
        <MfAlert v-else-if="resultState === 'error'" variant="danger" class="m-4" :title="$t('query.message.failed')">
          <span class="whitespace-pre-wrap break-words">{{ failureMessage }}</span>
        </MfAlert>
        <MfEmpty v-else-if="resultState === 'empty'" class="h-full"
                 :title="$t('query.result.empty')" :description="$t('query.result.emptyTip')"/>
      </template>

      <div v-else-if="hasResponse" class="h-full overflow-y-auto p-4">
        <div class="flex flex-col gap-2">
          <div class="flex items-start gap-2 rounded-mf-md border border-line px-3 py-2">
            <CircleCheck v-if="isSuccess" :size="15" class="mt-0.5 shrink-0 text-success"/>
            <CircleAlert v-else :size="15" class="mt-0.5 shrink-0 text-danger"/>
            <div class="flex min-w-0 flex-col gap-1">
              <span class="text-sm font-medium" :class="isSuccess ? 'text-success' : 'text-danger'">
                {{ isSuccess ? $t('query.message.success') : $t('query.message.failed') }}
              </span>
              <span v-if="failureMessage" class="break-all text-xs text-content-secondary">{{ failureMessage }}</span>
              <span v-if="isSuccess" class="text-xs text-content-tertiary">
                {{ $t('query.common.connectionTime') }} {{ connectionElapsed }} ms ·
                {{ $t('query.common.executionTime') }} {{ elapsed }} ms
              </span>
            </div>
          </div>

          <div v-if="props.query" class="rounded-mf-md border border-line px-3 py-2">
            <div class="mb-1 text-xs font-medium text-content-tertiary">SQL</div>
            <pre class="overflow-x-auto whitespace-pre-wrap break-all font-mono text-xs text-content-secondary">{{ props.query }}</pre>
          </div>
        </div>
      </div>
      <MfEmpty v-else-if="!running" class="h-full" variant="initial"
               :title="$t('query.result.initial')" :description="$t('query.result.initialTip')"/>
    </div>

    <div v-if="activeTab === 'results' && hasRows" class="flex shrink-0 items-center justify-between border-t border-line px-3 py-2">
      <div class="flex items-center gap-2 text-sm text-content-secondary">
        {{ $t('query.result.perPage') }}

        <div class="w-24">
          <MfSelect v-model="pageSize"
                    size="sm"
                    :options="pageSizeOptions"
                    @update:model-value="onPageSizeChange"/>
        </div>

        {{ $t('query.result.rowsUnit') }}
      </div>

      <MfPagination :current="currentPage"
                    :total="filteredRows.length"
                    :page-size="pageSize"
                    :show-size="false"
                    @update:current="onPageChange"/>
    </div>

    <GridVisual v-if="visualVisible"
                :is-visible="visualVisible"
                :configure="visualConfigure"
                @close="visualVisible = $event"/>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChartColumn, CircleAlert, CircleCheck, Download, Plus, RefreshCw, Search } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import { GridConfigure } from '@/views/components/grid/GridConfigure'
import GridVisual from '@/views/components/grid/GridVisual.vue'
import { ResponseModel } from '@/model/response'
import { queryResultState } from '@/model/query-result-state'

defineOptions({ name: 'QueryResultPanel' })

const props = withDefaults(defineProps<{
  response?: ResponseModel | null
  running?: boolean
  sourceId?: number
  sourceCode?: string
  query?: string
  datasetAvailable?: boolean
}>(), {
  response: null,
  running: false,
  sourceId: undefined,
  sourceCode: undefined,
  query: '',
  datasetAvailable: true
})

const emit = defineEmits<{
  (e: 'add-dataset'): void
  (e: 'existing-dataset'): void
  (e: 'refresh'): void
}>()

const { t } = useI18n()

const activeTab = ref<'results' | 'messages'>('results')
const quickFilter = ref('')
const pageSize = ref(100)
const currentPage = ref(1)
const visualVisible = ref(false)
// Widths the user dragged or fitted; columns without one size to their content.
const widths = ref<Record<string, number>>({})

const tabs = computed(() => [
  { key: 'results' as const, label: t('query.result.results') },
  { key: 'messages' as const, label: t('query.result.messages') }
])

const pageSizeOptions = [50, 100, 200, 500, 1000].map(size => ({ label: String(size), value: size }))

const hasResponse = computed(() => !!props.response)
const resultState = computed(() => queryResultState(props.response, props.running))
const isSuccess = computed(() => !!props.response?.status)
const failureMessage = computed(() => (!isSuccess.value && props.response?.message) ? props.response.message : null)

const headers = computed<string[]>(() => (isSuccess.value ? props.response?.data?.headers ?? [] : []))
const rows = computed<any[]>(() => (isSuccess.value ? props.response?.data?.columns ?? [] : []))
const rowCount = computed(() => rows.value.length)
const hasRows = computed(() => isSuccess.value && rowCount.value > 0)
const exportable = computed(() => hasResponse.value && isSuccess.value)
const canAddDataset = computed(() => isSuccess.value && props.response?.data?.isSuccessful !== false
  && !!props.sourceCode && headers.value.length > 0 && !props.running && props.datasetAvailable)
const elapsed = computed(() => props.response?.data?.processor?.elapsed ?? 0)
const connectionElapsed = computed(() => props.response?.data?.connection?.elapsed ?? 0)

const cellText = (value: unknown) => typeof value === 'object' ? JSON.stringify(value) : String(value)
// Keys carry the position so repeated header names still get their own column.
const tableColumns = computed(() => headers.value.map((header, index) => {
  const key = `${index}:${header}`
  return { key, title: header, dataIndex: header, width: widths.value[key], ellipsis: !!widths.value[key] }
}))
/** The quick search keeps rows where any cell contains the text. */
const filteredRows = computed(() => {
  const text = quickFilter.value.trim().toLowerCase()
  const list = text ? rows.value.filter(row => headers.value.some(header => {
    const value = row[header]
    return value !== null && value !== undefined && cellText(value).toLowerCase().includes(text)
  })) : rows.value
  return list.map((row, index) => ({ ...row, __row: String(index) }))
})
const pagedRows = computed(() => filteredRows.value.slice((currentPage.value - 1) * pageSize.value, currentPage.value * pageSize.value))
const onResize = (column: { key?: string }, width: number) => { if (column.key) widths.value = { ...widths.value, [column.key]: width } }

const visualConfigure = computed<GridConfigure>(() => ({
  headers: headers.value as any,
  columns: rows.value as any,
  height: 400,
  width: 800,
  showSeriesNumber: false,
  sourceId: props.sourceId,
  query: props.query,
  code: props.sourceCode
}))

const onPageChange = (page: number) => {
  currentPage.value = page
}

const onPageSizeChange = () => {
  currentPage.value = 1
}

watch(quickFilter, () => { currentPage.value = 1 })

watch(() => props.response, () => {
  quickFilter.value = ''
  widths.value = {}
  currentPage.value = 1
  activeTab.value = props.response && !props.response.status ? 'messages' : 'results'
})

const onExport = () => {
  const header = headers.value
  const escape = (value: any) => {
    const text = value === null || value === undefined ? '' : String(value)
    return /[",\n]/.test(text) ? `"${ text.replace(/"/g, '""') }"` : text
  }
  const lines = [header.map(escape).join(',')]
  rows.value.forEach(row => lines.push(header.map(name => escape(row[name])).join(',')))

  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `query-result-${ Date.now() }.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}
</script>
