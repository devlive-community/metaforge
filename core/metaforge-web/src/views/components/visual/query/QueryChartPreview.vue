<template>
  <div class="flex h-full min-h-0 min-w-0 flex-col gap-3">
    <div v-if="!embedded" class="flex shrink-0 flex-wrap items-center justify-between gap-2">
      <h3 v-if="heading" class="text-base font-semibold">{{ $t('queryChart.preview') }}</h3>
      <div class="ml-auto flex flex-wrap gap-1 rounded-mf-md border border-line p-1">
        <MfButton v-if="options.type !== 'table'" size="sm" :variant="view === 'chart' ? 'secondary' : 'ghost'" :aria-pressed="view === 'chart'" @click="view = 'chart'">
          <ChartColumn :size="14"/>{{ $t('queryChart.chart') }}
        </MfButton>
        <MfButton size="sm" :variant="showData ? 'secondary' : 'ghost'" :aria-pressed="showData" @click="view = 'data'">
          <Table2 :size="14"/>{{ $t('queryChart.data') }}
        </MfButton>
        <template v-if="options.download && !showData && !showInlineDetails">
          <MfButton v-for="format in formats" :key="format" size="sm" variant="ghost" :disabled="chartLoading || !chartAvailable || invalidCircular || invalidShare || failed" @click="download(format)">
            <Download :size="14"/>{{ format.toUpperCase() }}
          </MfButton>
        </template>
      </div>
    </div>
    <section class="relative flex min-h-0 flex-1 flex-col overflow-hidden" :aria-busy="loading" :class="!embedded && 'rounded-mf-lg border border-line bg-surface p-4'">
      <div class="contents" :class="{ invisible: loading }" :inert="loading" :aria-hidden="loading || undefined">
      <h3 v-if="options.name" class="shrink-0 break-words text-base font-semibold">{{ options.name }}</h3>
      <p v-if="options.description" class="mb-2 shrink-0 whitespace-pre-wrap break-words text-xs text-content-tertiary">{{ options.description }}</p>
      <ResultTable v-if="options.type === 'table' || (showData && rawSeries)" class="flex-1" fill :headers="options.fields" :rows="result.filtered" :page-size="pageSize">
        <template v-if="embedded" #cell="{ header, value }">
          <MfTag v-if="statusTag(header, value)" :variant="statusTag(header, value)" size="sm">{{ value }}</MfTag>
        </template>
      </ResultTable>
      <div v-else-if="showData || showInlineDetails" class="flex min-h-0 flex-1 flex-col gap-2">
        <div v-if="showInlineDetails" class="flex items-center justify-between gap-2">
          <strong>{{ $t('queryChart.details') }}</strong>
          <MfButton size="sm" @click="activePoint = undefined">{{ $t('queryChart.back') }}</MfButton>
        </div>
        <MfTable fill density="compact" :columns="tableColumns" :data-source="pagedRows" :empty-text="$t('common.noData')">
          <template v-for="column in tableColumns" :key="column.dataIndex" #[`cell-${column.dataIndex}`]="{ record }">
            <MfTag v-if="embedded && statusTag(column.dataIndex, record[column.dataIndex])"
                   :variant="statusTag(column.dataIndex, record[column.dataIndex])" size="sm">{{ record[column.dataIndex] }}</MfTag>
            <EllipsisCell v-else :text="cellText(record[column.dataIndex])"/>
          </template>
        </MfTable>
        <MfPagination v-if="tableRows.length > pageSize" class="shrink-0" :current="page" :total="tableRows.length" :page-size="pageSize" :show-size="false" @update:current="page = $event"/>
      </div>
      <MfAlert v-else-if="prepared?.issue" variant="info" :title="$t('queryChart.issue.' + prepared.issue)"/>
      <MfAlert v-else-if="invalidCircular" variant="warning" :title="$t('queryChart.positiveValues')"/>
      <MfAlert v-else-if="invalidShare" variant="warning" :title="$t('queryChart.nonnegativeShares')"/>
      <MfAlert v-else-if="failed" variant="danger" :title="$t('queryChart.renderError')"/>
      <MfEmpty v-else-if="!chartAvailable" class="flex-1" :title="$t('queryChart.noPoints')"/>
      <QueryChartCanvas v-else ref="canvas" class="flex-1" :options="options" :points="chartRows" :prepared="prepared" :metric="metricLabel" :embedded="embedded" @point="selectPoint" @error="failed = true"/>
      </div>
      <ChartLoading v-if="loading" class="absolute inset-0 z-10 bg-surface/90"/>
    </section>

    <MfModal v-if="detailRows.length && options.detailsMode === 'dialog'" :open="activePoint !== undefined" width="min(880px, 94vw)" height="min(560px, 85dvh)"
             :title="$t('queryChart.details')" trap-focus @close="activePoint = undefined">
      <MfTable fill class="h-full" density="compact" :columns="rawColumns" :data-source="detailRows.slice((detailPage - 1) * pageSize, detailPage * pageSize)" :empty-text="$t('common.noData')"/>
      <template #footer>
        <MfPagination v-if="detailRows.length > pageSize" :current="detailPage" :total="detailRows.length" :page-size="pageSize" :show-size="false" @update:current="detailPage = $event"/>
        <MfButton @click="activePoint = undefined">{{ $t('common.cancel') }}</MfButton>
      </template>
    </MfModal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChartColumn, Download, Table2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import { chartDataSpec } from '@/model/query-chart-data'
import { prepareQueryChart } from '@/model/query-chart-series'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
import QueryChartCanvas from './QueryChartCanvas.vue'
import ChartLoading from '../ChartLoading.vue'

const props = defineProps<{ options: QueryChartOptions; rows: QueryChartRow[]; heading?: boolean; embedded?: boolean; linked?: boolean; loading?: boolean; aggregation?: ReturnType<typeof aggregateQueryChart> }>()
const emit = defineEmits<{ filter: [rows: QueryChartRow[]] }>()
const formats = ['png', 'svg'] as const
const { t } = useI18n()
const view = ref<'chart' | 'data'>('chart')
const page = ref(1)
const detailPage = ref(1)
const pageSize = 50
const activePoint = ref<number[]>()
const failed = ref(false)
const canvas = ref<InstanceType<typeof QueryChartCanvas>>()
const chartLoading = computed(() => props.loading || canvas.value?.loading !== false)
const showData = computed(() => props.options.type === 'table' || view.value === 'data')
const result = computed(() => props.aggregation ?? aggregateQueryChart(props.rows, props.options))
const prepared = computed(() => prepareQueryChart(props.options, result.value))
const rawSeries = computed(() => !!chartDataSpec(props.options.type)?.raw)
const metricLabel = computed(() => props.options.metric || t('queryChart.records'))
const circular = computed(() => ['pie', 'donut', 'rose', 'funnel'].includes(props.options.type))
const invalidCircular = computed(() => circular.value && result.value.points.some(point => (point.value ?? 0) < 0))
const invalidShare = computed(() => props.options.type === 'percentStackedBar' && result.value.points.some(point => (point.value ?? 0) < 0))
const chartRows = computed(() => {
  const points = result.value.points
  if (circular.value) return points.filter(point => (point.value ?? 0) > 0)
  return points.some(point => point.value !== null) ? points : []
})
const chartAvailable = computed(() => prepared.value ? !!prepared.value.option : chartRows.value.length > 0)
const rowsFor = (ids: number[]) => [...new Set(ids.flatMap(id => (prepared.value?.details ?? result.value.details).get(id) ?? []))]
const detailRows = computed(() => activePoint.value ? rowsFor(activePoint.value) : [])
const showInlineDetails = computed(() => !!detailRows.value.length && props.options.detailsMode === 'panel')
const rawColumns = computed(() => props.options.fields.map(name => ({ title: name, dataIndex: name })))
const tableColumns = computed(() => showInlineDetails.value ? rawColumns.value : [
  { title: props.options.category || t('queryChart.category'), dataIndex: 'category' },
  ...(props.options.series ? [{ title: props.options.series, dataIndex: 'series' }] : []),
  { title: metricLabel.value, dataIndex: 'value' }
])
const tableRows = computed(() => showInlineDetails.value ? detailRows.value : result.value.points)
const pagedRows = computed(() => tableRows.value.slice((page.value - 1) * pageSize, page.value * pageSize))
function statusTag(field: string, value: unknown): 'success' | 'danger' | undefined {
  if (!/^(status|state)$/i.test(field)) return
  if (value === 'SUCCESS') return 'success'
  if (value === 'FAILED' || value === 'FAILURE') return 'danger'
}
const cellText = (value: unknown) => value == null ? 'NULL' : typeof value === 'object' ? JSON.stringify(value) : String(value)
watch([result, view], () => { page.value = 1; activePoint.value = undefined; failed.value = false })
watch(() => props.options, () => { failed.value = false }, { deep: true })

function selectPoint(ids: number[]) {
  if (props.options.pointAction === 'filter' && props.linked) {
    const rows = rowsFor(ids)
    if (rows.length) emit('filter', rows)
    return
  }
  if (props.options.pointAction !== 'details' && props.options.pointAction !== 'filter') return
  page.value = 1
  detailPage.value = 1
  activePoint.value = ids
}
async function download(format: typeof formats[number]) {
  try { await canvas.value?.exportImage(props.options.name || 'chart', format) }
  catch { MfMessage.error(t('queryChart.exportError')) }
}
</script>
