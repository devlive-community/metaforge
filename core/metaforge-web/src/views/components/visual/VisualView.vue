<template>
  <div class="relative h-full w-full" :style="{ width: width, height: height }">
    <template v-if="configuration?.queryChart">
      <div v-if="snapshot" class="flex h-full min-h-0 flex-col gap-2">
        <div v-if="dynamicCode" class="flex shrink-0 flex-wrap items-center justify-between gap-2">
          <span class="text-xs text-content-secondary" aria-live="polite">
            {{ refreshedAt ? $t('queryChart.refreshedAt', { time: refreshedAt }) : $t('queryChart.dynamic') }}
          </span>
          <MfButton size="sm" variant="secondary" :loading="refreshing" @click="refreshChart">
            <RefreshCw :size="14"/>{{ $t('common.refresh') }}
          </MfButton>
        </div>
        <MfAlert v-if="refreshFailed" variant="warning" :title="$t('queryChart.refreshError')"/>
        <QueryChartPreview class="min-h-0 flex-1" :options="snapshot.options" :rows="refreshedRows ?? snapshot.rows"/>
      </div>
      <MfAlert v-else variant="danger" :title="$t('queryChart.renderError')"/>
    </template>
    <div v-else-if="loading" class="flex items-center justify-center absolute inset-0">
      <MfSpinner size="lg"/>
    </div>

    <div v-if="!configuration?.queryChart && localConfiguration && !loading">
      <div v-if="localConfiguration.message" class="p-4">
        <MfAlert variant="danger" :title="localConfiguration.message"/>
      </div>

      <div v-else-if="hasError && message" class="flex items-center justify-center absolute inset-0">
        <MfAlert variant="danger" :title="message"/>
      </div>

        <div v-else>
          <VisualTable v-if="configuration?.type === Type.TABLE"
                       :configuration="localConfiguration as any"
                       :submitted="false"
                       :width="width"
                       :height="height"/>

          <VisualChart v-else :configuration="localConfiguration as Configuration" :submitted="false" :width="width" :height="height"/>
        </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatClock } from '@/utils/date'
import { computed, defineAsyncComponent, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { MfMessage } from 'metaforge-ui'
import { RefreshCw } from '@lucide/vue'
import ReportService from '@/services/report'
import PublicReportService from '@/services/public-report'
import type { QueryChartRow } from '@/model/query-chart'
import { cloneDeep } from 'lodash'
import { Type } from '@/views/components/visual/Type'
import { Configuration } from './Configuration'
import VisualChart from './components/VisualChart.vue'
import VisualTable from '@/views/components/visual/components/VisualTable.vue'
import DatasetService from '@/services/dataset'
import { adhocRows } from '@/model/dataset-adhoc'
import ExecuteService from '@/services/execute.ts'
import { ExecuteModel } from '@/model/execute.ts'

defineOptions({ name: 'VisualView' })
const QueryChartPreview = defineAsyncComponent(() => import('./query/QueryChartPreview.vue'))

const props = withDefaults(defineProps<{
  configuration?: Configuration | null
  query?: any
  code?: string
  type?: string
  width?: string
  height?: string
  original?: string
  reportCode?: string
  realtime?: boolean
  /** Renders a shared report: data comes from the public API by token, never from visitor SQL. */
  shareToken?: string
}>(), {
  width: '100%',
  height: '400px'
})

const snapshot = computed(() => {
  const config = props.configuration
  const saved = config?.queryChart
  if (saved?.version !== 1 || !Array.isArray(config?.columns)
      || !Array.isArray(saved.options?.fields) || !Array.isArray(saved.options?.groups)) return null
  return { options: saved.options, rows: config.columns }
})

const dynamicCode = computed(() => props.realtime && snapshot.value ? (props.shareToken || props.reportCode) : undefined)
const refreshInterval = computed(() => {
  const seconds = snapshot.value?.options.refreshSeconds
  return seconds === 60 || seconds === 300 || seconds === 900 ? seconds : 0
})
const refreshing = ref(false)
const refreshFailed = ref(false)
const refreshedAt = ref('')
const refreshedRows = shallowRef<QueryChartRow[]>()
let generation = 0
let timer: number | undefined

async function refreshChart() {
  const code = dynamicCode.value
  if (!code || refreshing.value) return
  const run = generation
  refreshing.value = true
  try {
    const datasetReport = props.type === 'DATASET'
    const response = datasetReport
      ? props.shareToken ? await PublicReportService.data(props.shareToken) : await DatasetService.adhoc(props.code!, props.query)
      : props.shareToken ? await PublicReportService.refresh(props.shareToken) : await ReportService.refreshChart(code)
    if (run !== generation) return
    if (!response.status || (datasetReport ? response.data?.isSuccessful === false || !Array.isArray(response.data?.columns) : !Array.isArray(response.data))) {
      refreshFailed.value = true
      return
    }
    refreshedRows.value = datasetReport ? adhocRows(response.data.headers || [], response.data.columns) : response.data
    refreshedAt.value = formatClock(new Date())
    refreshFailed.value = false
  }
  catch { if (run === generation) refreshFailed.value = true }
  finally { if (run === generation) refreshing.value = false }
}

watch([dynamicCode, refreshInterval], ([code, seconds]) => {
  generation++
  window.clearInterval(timer)
  refreshing.value = false
  refreshedRows.value = undefined
  refreshedAt.value = ''
  refreshFailed.value = false
  if (!code) return
  void refreshChart()
  if (seconds) timer = window.setInterval(() => {
    if (!document.hidden) void refreshChart()
  }, seconds * 1000)
}, { immediate: true })
onBeforeUnmount(() => {
  generation++
  window.clearInterval(timer)
})

const loading = ref(false)
const hasError = ref<any>(false)
const message = ref<string | null>(null)
const localConfiguration = ref<Configuration | null>(null)

const formatRaw = (response: any) => {
  if (localConfiguration.value) {
    if (response.data.isSuccessful) {
      localConfiguration.value.headers = response.data.headers
      localConfiguration.value.columns = response.data.columns
      localConfiguration.value.message = null
    }
    else {
      localConfiguration.value.headers = []
      localConfiguration.value.columns = []
      localConfiguration.value.message = response.data.message
    }
  }
}

const handleInitialize = () => {
  localConfiguration.value = cloneDeep(props.configuration) as Configuration
  setTimeout(() => {
    loading.value = true
    if (props.shareToken) {
      PublicReportService.data(props.shareToken)
                         .then((response) => {
                           if (response.status) {
                             formatRaw(response)
                           }
                           else {
                             hasError.value = true
                             message.value = response.message
                           }
                         })
                         .finally(() => (loading.value = false))
    }
    else if (props.type === 'QUERY') {
      const configure: ExecuteModel = { name: props.original as any, content: props.query as any, mode: 'REPORT' }
      ExecuteService.execute(configure, null)
                    .then((response) => {
                      if (response.status && response.data.isSuccessful) {
                        formatRaw(response)
                        message.value = null
                      }
                      else {
                        hasError.value = response.message
                        message.value = response.message
                      }
                    })
                    .finally(() => (loading.value = false))
    }
    else {
      DatasetService.adhoc(props.code!, props.query)
                    .then((response) => {
                      if (response.status) {
                        formatRaw(response)
                      }
                      else {
                        MfMessage.error(response.message)
                      }
                    })
                    .finally(() => (loading.value = false))
    }
  })
}

if (!props.configuration?.queryChart) handleInitialize()
</script>
