import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import ReportService from '@/services/report'
import { aggregateQueryChart, chartFieldKind, createQueryChartSnapshot, defaultQueryChart, isCompleteFilter,
  type ChartFilterGroup, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'

export interface QueryChartSource {
  headers: string[]
  rows: QueryChartRow[]
  types?: string[]
  code: string
  query: string
  elapsed: number
  label?: string
}

export function useQueryChartBuilder(source: QueryChartSource, close: () => void) {
  const { t } = useI18n()
  const router = useRouter()
  const fieldKinds = Object.fromEntries(source.headers.map((name, index) => [name, chartFieldKind(source.rows, name, source.types?.[index])]))
  const firstDate = source.headers.find(name => fieldKinds[name] === 'date')
  const options = ref(defaultQueryChart(source.headers, firstDate))
  const publishing = ref(false)
  const error = ref('')
  const result = computed(() => aggregateQueryChart(source.rows, options.value))
  const fieldOptions = computed(() => options.value.fields.map(name => ({ label: name, value: name })))
  const numericOptions = computed(() => fieldOptions.value.filter(option => fieldKinds[option.value] === 'number'))
  const timeCategory = computed(() => fieldKinds[options.value.category] === 'date')
  const ruleCount = computed(() => options.value.groups.reduce((count, group) => count + group.rules.length, 0))
  const metricLabel = computed(() => options.value.metric || t('queryChart.records'))
  const filterExpression = computed(() => options.value.groups.map(group => {
    const symbols = { eq: '=', ne: '!=', contains: 'LIKE', in: 'IN', null: 'IS NULL', notNull: 'IS NOT NULL', gte: '>=', lte: '<=', between: 'BETWEEN' }
    return '(' + group.rules.filter(isCompleteFilter).map(rule => {
      const value = rule.operator === 'null' || rule.operator === 'notNull' ? ''
          : rule.operator === 'between' ? ' ' + JSON.stringify(rule.value) + ' AND ' + JSON.stringify(rule.end)
          : ' ' + JSON.stringify(rule.value)
      return rule.field + ' ' + symbols[rule.operator] + value
    }).join(' ' + group.join.toUpperCase() + ' ') + ')'
  }).filter(expression => expression !== '()').join('\n' + options.value.filterJoin.toUpperCase() + ' '))

  function changeCategory(value: string) {
    options.value.category = value
    options.value.grain = fieldKinds[value] === 'date' ? 'day' : 'none'
  }

  function changeAggregation(value: QueryChartOptions['aggregation']) {
    options.value.aggregation = value
    if (value !== 'count' && !options.value.metric) {
      options.value.metric = (value === 'distinct' ? fieldOptions.value : numericOptions.value)[0]?.value || ''
    }
    if (value !== 'count' && value !== 'distinct' && fieldKinds[options.value.metric] !== 'number') {
      options.value.metric = numericOptions.value[0]?.value || ''
    }
  }

  function toggleField(name: string, checked: boolean) {
    if (publishing.value) return
    options.value.fields = source.headers.filter(field => field === name ? checked : options.value.fields.includes(field))
    if (options.value.category === name && !checked) changeCategory(options.value.fields[0] || '')
    if (options.value.metric === name && !checked) {
      options.value.metric = ''
      changeAggregation(options.value.aggregation)
    }
    if (options.value.series === name && !checked) options.value.series = ''
    if (!checked) options.value.groups.forEach(group => { group.rules = group.rules.filter(rule => rule.field !== name) })
  }

  function addRule(group: ChartFilterGroup) {
    group.rules.push({ field: options.value.fields[0] || '', operator: 'eq', value: '', end: '' })
  }

  function addGroup() {
    const group: ChartFilterGroup = { join: 'and', rules: [] }
    addRule(group)
    options.value.groups.push(group)
  }

  function reset() {
    options.value = defaultQueryChart(source.headers, firstDate)
    error.value = ''
  }

  async function copyConfig() {
    try {
      await navigator.clipboard.writeText(JSON.stringify({ ...options.value, query: source.query, mode: 'snapshot' }, null, 2))
      MfMessage.success(t('query.tip.copySuccess'))
    }
    catch { MfMessage.error(t('queryChart.copyError')) }
  }

  async function publish() {
    if (publishing.value) return
    const chart = options.value
    error.value = ''
    if (!chart.name.trim()) return
    if (!chart.fields.length || (chart.type !== 'table' && (!chart.category || (chart.aggregation !== 'count' && !chart.metric)))) {
      error.value = t('queryChart.requiredFields')
      return
    }
    if (chart.filtersEnabled && chart.groups.some(group => group.rules.some(rule => !isCompleteFilter(rule)))) {
      error.value = t('queryChart.incompleteFilter')
      return
    }
    if (chart.min !== undefined && chart.max !== undefined && chart.min >= chart.max) {
      error.value = t('queryChart.axisRange')
      return
    }
    if (chart.type !== 'table' && (!result.value.points.some(point => point.value !== null)
        || (['pie', 'donut', 'rose', 'funnel'].includes(chart.type) && (result.value.points.some(point => (point.value ?? 0) < 0)
            || !result.value.points.some(point => (point.value ?? 0) > 0))))) {
      error.value = t('queryChart.noUsableValues')
      return
    }
    const name = chart.name.trim()
    const configure = JSON.stringify(createQueryChartSnapshot({ ...chart, name }, source.rows))
    publishing.value = true
    try {
      const response = await ReportService.saveOrUpdate({
        name, description: chart.description, realtime: false, type: 'QUERY',
        configure, query: source.query, source: { code: source.code }
      })
      if (!response.status) {
        error.value = response.message || t('queryChart.saveError')
        return
      }
      MfMessage.success(t('report.tip.publishSuccess').replace('$VALUE', name))
      close()
      await router.push('/report')
    }
    catch { error.value = t('queryChart.saveError') }
    finally { publishing.value = false }
  }

  return { options, fieldKinds, publishing, error, result, fieldOptions, numericOptions, timeCategory,
    ruleCount, filterExpression, metricLabel, changeCategory, changeAggregation, toggleField, addGroup, addRule, reset, copyConfig, publish }
}
