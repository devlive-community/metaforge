import { computed, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import DatasetService from '@/services/dataset'
import ReportService from '@/services/report'
import { adhocColumnIssue, adhocQuery, adhocRows, createAdhocColumn, restoreAdhocQuery, type AdhocColumn, type AdhocField, type AdhocMode } from '@/model/dataset-adhoc'
import { defaultQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'
import type { Configuration } from '@/views/components/visual/Configuration'

export function useDatasetAdhoc(code: Ref<string>, reportCode: Ref<string>, t: (key: string) => string) {
  const fields = ref<AdhocField[]>([]), columns = ref<AdhocColumn[]>([])
  const dataset = ref({ name: '', description: '' }), report = ref({ name: '', description: '' })
  const limit = ref(1000), initializing = ref(false), loadError = ref(''), warning = ref('')
  const running = ref(false), queried = ref(false), failure = ref(''), sql = ref(''), elapsed = ref(0)
  const headers = ref<string[]>([]), rows = ref<QueryChartRow[]>([])
  const options = ref<QueryChartOptions>({ ...defaultQueryChart([]), type: 'table' })
  const legacy = ref<Configuration | null>(null)
  const executed = ref(''), selected = ref(''), resultTab = ref('data')
  let generation = 0, runId = 0
  const query = computed(() => adhocQuery(columns.value, limit.value))
  const stale = computed(() => queried.value && JSON.stringify(query.value) !== executed.value)
  const selectedColumn = computed(() => columns.value.find(column => column.key === selected.value))
  const canPublish = computed(() => queried.value && !running.value && !stale.value && !failure.value && headers.value.length > 0)
  const message = (error: unknown) => error instanceof Error ? error.message : String(error)
  const parse = (value: unknown) => typeof value === 'string' ? JSON.parse(value) : value

  async function load() {
    const version = ++generation
    runId++; initializing.value = true; loadError.value = ''; warning.value = ''; running.value = false
    fields.value = []; columns.value = []; headers.value = []; rows.value = []; sql.value = ''; queried.value = false
    selected.value = ''; limit.value = 1000; failure.value = ''; legacy.value = null; report.value = { name: '', description: '' }
    options.value = { ...defaultQueryChart([]), type: 'table' }
    try {
      const [info, catalog, saved] = await Promise.all([
        DatasetService.getByCode(code.value), DatasetService.getColumnsByCode(code.value),
        reportCode.value ? ReportService.getByCode(reportCode.value) : Promise.resolve(null)
      ])
      if (version !== generation) return
      for (const response of [info, catalog, saved]) if (response && !response.status) throw Error(response.message)
      if (!Array.isArray(catalog.data)) throw Error(t('dataset.adhoc.loadFailed'))
      dataset.value = info.data
      fields.value = catalog.data.filter((field: AdhocField) => field.mode === 'METRIC' || field.mode === 'DIMENSION')
      if (saved) {
        if (saved.data.dataset?.code && saved.data.dataset.code !== code.value) throw Error(t('dataset.adhoc.invalidReport'))
        report.value = { name: saved.data.name || '', description: saved.data.description || '' }
        const restored = restoreAdhocQuery(fields.value, saved.data.query)
        columns.value = restored.columns; limit.value = restored.limit
        if (restored.missing) warning.value = t('dataset.adhoc.missingFields')
        const config = parse(saved.data.configure)
        if (config?.queryChart?.version === 1 && Array.isArray(config.queryChart.options?.fields)) {
          options.value = { ...defaultQueryChart([]), ...config.queryChart.options }
        } else if (config?.type) legacy.value = config
        if (!restored.missing) await execute()
      }
    } catch (error) { if (version === generation) loadError.value = message(error).startsWith('dataset.') ? t(message(error)) : message(error) }
    finally { if (version === generation) initializing.value = false }
  }

  async function execute() {
    if (running.value || !columns.value.some(column => column.mode !== 'FILTER')) return false
    const invalid = columns.value.find(column => adhocColumnIssue(column))
    if (invalid) { selected.value = invalid.key; return false }
    if (!Number.isInteger(limit.value) || limit.value < 1) return false
    const version = generation, request = ++runId, body = JSON.parse(JSON.stringify(query.value))
    running.value = true; failure.value = ''; const start = performance.now()
    try {
      const response = await DatasetService.adhoc(code.value, body)
      if (version !== generation || request !== runId) return false
      sql.value = response.data?.content || ''
      if (!response.status || response.data?.isSuccessful === false) throw Error(response.data?.message || response.message)
      headers.value = Array.isArray(response.data?.headers) ? response.data.headers : []
      rows.value = adhocRows(headers.value, Array.isArray(response.data?.columns) ? response.data.columns : [])
      executed.value = JSON.stringify(body); queried.value = true
      const numeric = headers.value.find(header => rows.value.some(row => typeof row[header] === 'number')) || ''
      const validMetric = headers.value.includes(options.value.metric)
      options.value = { ...options.value, fields: [...headers.value],
        category: headers.value.includes(options.value.category) ? options.value.category : headers.value.find(header => header !== numeric) || headers.value[0] || '',
        metric: validMetric ? options.value.metric : numeric,
        series: headers.value.includes(options.value.series) ? options.value.series : '',
        aggregation: validMetric ? options.value.aggregation : numeric ? 'sum' : 'count' }
      resultTab.value = (legacy.value ? legacy.value.type === 'TABLE' : options.value.type === 'table') ? 'data' : 'chart'
      return true
    } catch (error) {
      if (version === generation && request === runId) {
        failure.value = message(error); queried.value = true; headers.value = []; rows.value = []; resultTab.value = 'message'
      }
      return false
    } finally {
      if (version === generation && request === runId) { running.value = false; elapsed.value = Math.round(performance.now() - start) }
    }
  }
  function add(field: AdhocField, mode: AdhocMode = field.mode) {
    const column = createAdhocColumn(field, mode)
    if (!columns.value.some(value => value.key === column.key)) columns.value.push(column)
    selected.value = column.key
  }
  function remove(key: string) { columns.value = columns.value.filter(column => column.key !== key); if (selected.value === key) selected.value = '' }
  function update(column: AdhocColumn) { columns.value = columns.value.map(value => value.key === column.key ? { ...column } : value) }
  function clear() {
    runId++; running.value = false; columns.value = []; selected.value = ''; headers.value = []; rows.value = []
    sql.value = ''; failure.value = ''; queried.value = false; warning.value = ''; resultTab.value = 'data'
  }
  watch([code, reportCode], load, { immediate: true })
  onBeforeUnmount(() => { generation++; runId++ })
  return { fields, columns, dataset, report, limit, initializing, loadError, warning, running, queried, failure, sql, elapsed,
    headers, rows, options, legacy, query, stale, selected, selectedColumn, resultTab, canPublish, load, execute, add, remove, update, clear }
}
