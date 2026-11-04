<template>
  <div class="flex h-dvh min-h-0 flex-col" :class="embed ? 'p-2' : 'px-4 py-6 sm:px-8'">
    <div v-if="loading" class="flex min-h-0 flex-1 items-center justify-center">
      <MfSpinner size="lg"/>
    </div>

    <div v-else-if="!report" class="flex min-h-0 flex-1 items-center justify-center">
      <MfEmpty :title="$t('report.share.unavailable')" :description="$t('report.share.unavailableHint')"/>
    </div>

    <template v-else>
      <header v-if="showHeader || hasTools" class="mb-3 flex shrink-0 items-start justify-between gap-3">
        <div class="min-w-0">
          <h1 v-if="report.showTitle" class="truncate font-semibold text-content" :class="embed ? 'text-sm' : 'text-xl'">{{ report.name }}</h1>
          <p v-if="showDescription" class="mt-1 text-content-secondary" :class="embed ? 'text-xs' : 'text-sm'">{{ report.description }}</p>
        </div>
        <div v-if="hasTools" class="flex shrink-0 items-center gap-2">
          <div v-if="report.allowData" class="flex items-center gap-1 rounded-mf-md bg-surface-hover p-0.5">
            <MfButton size="sm" :variant="view === 'chart' ? 'secondary' : 'ghost'" @click="view = 'chart'">
              <ChartColumn :size="14"/>{{ $t('report.share.viewChart') }}
            </MfButton>
            <MfButton size="sm" :variant="view === 'data' ? 'secondary' : 'ghost'" @click="showData">
              <Table2 :size="14"/>{{ $t('report.share.viewData') }}
            </MfButton>
          </div>
          <MfButton v-if="report.allowDownload" size="sm" @click="download">
            <Download :size="14"/>{{ $t('report.share.download') }}
          </MfButton>
        </div>
      </header>

      <main class="min-h-0 flex-1">
        <template v-if="view === 'chart'">
          <MfAlert v-if="!configuration" variant="danger" :title="$t('queryChart.renderError')"/>
          <VisualView v-else
                      :configuration="configuration"
                      :type="report.type"
                      :realtime="report.realtime"
                      :share-token="token"
                      width="100%"
                      height="100%"/>
        </template>
        <div v-else class="flex h-full min-h-0 flex-col gap-2">
          <div v-if="tableLoading" class="flex h-full min-h-40 items-center justify-center">
            <MfSpinner/>
          </div>
          <MfAlert v-else-if="tableFailed" variant="warning" :title="$t('report.share.dataUnavailable')"/>
          <template v-else>
            <MfTable class="min-h-0 flex-1" fill :columns="tableColumns" :data-source="tableRows" row-key="__row" density="compact"
                     :empty-text="$t('report.share.noData')"/>
            <p v-if="truncated" class="shrink-0 text-xs text-content-tertiary">{{ $t('report.share.truncated', { count: tableRows.length }) }}</p>
          </template>
        </div>
      </main>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChartColumn, Download, Table2 } from '@lucide/vue'
import PublicReportService from '@/services/public-report'
import VisualView from '@/views/components/visual/VisualView.vue'

defineOptions({ name: 'PublicReport' })

const props = defineProps<{ token: string; embed: boolean }>()

interface PublicReportModel
{
  name: string
  description?: string
  type: string
  realtime: boolean
  configure: string
  embed: boolean
  showTitle: boolean
  showDescription: boolean
  allowData: boolean
  allowDownload: boolean
}

const loading = ref(true)
const report = ref<PublicReportModel | null>(null)
const view = ref<'chart' | 'data'>('chart')
const showDescription = computed(() => !!report.value?.showDescription && !!report.value.description)
const showHeader = computed(() => !!report.value?.showTitle || showDescription.value)
const hasTools = computed(() => !!report.value?.allowData || !!report.value?.allowDownload)
const configuration = computed(() => {
  try {
    return report.value ? JSON.parse(report.value.configure) : null
  }
  catch {
    return null
  }
})

const tableLoading = ref(false)
const tableFailed = ref(false)
const tableLoaded = ref(false)
const truncated = ref(false)
const tableColumns = ref<{ title: string; dataIndex: string; key: string }[]>([])
const tableRows = ref<Record<string, unknown>[]>([])

async function showData()
{
  view.value = 'data'
  if (tableLoaded.value || tableLoading.value) return
  tableLoading.value = true
  try {
    const response = await PublicReportService.table(props.token)
    if (!response.status || !response.data) {
      tableFailed.value = true
      return
    }
    const headers: string[] = response.data.headers ?? []
    // Positional keys keep any header text (dots, spaces) safe as a data index.
    tableColumns.value = headers.map((title, index) => ({ title, dataIndex: `c${ index }`, key: `c${ index }` }))
    tableRows.value = (response.data.rows ?? []).map((row: unknown[], rowIndex: number) =>
        Object.fromEntries([['__row', rowIndex], ...headers.map((_, index) => [`c${ index }`, row[index] ?? ''])]))
    truncated.value = !!response.data.truncated
    tableFailed.value = false
    tableLoaded.value = true
  }
  finally {
    tableLoading.value = false
  }
}

// The server sends the CSV as an attachment, so navigating to it downloads without leaving the page.
function download()
{
  window.location.assign(PublicReportService.downloadUrl(props.token))
}

onMounted(async () => {
  try {
    const response = await PublicReportService.view(props.token)
    report.value = response.status && response.data ? response.data : null
    if (report.value) document.title = `${ report.value.name } - MetaForge`
  }
  finally {
    loading.value = false
  }
})
</script>
