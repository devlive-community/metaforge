<template>
  <div class="grid grid-cols-1 lg:grid-cols-[1fr_5fr] gap-2">
    <div>
      <div class="dc-adhoc__side">
        <div class="dc-adhoc__dataset">
          <div class="dc-adhoc__dataset-icon">
            <DatabaseOutlined :style="{ fontSize: '18px', color: 'var(--dc-primary)' }"/>
          </div>
          <div class="min-w-0 flex-1">
            <div class="dc-adhoc__dataset-name">{{ dataInfo.name || code }}</div>
            <div class="dc-adhoc__dataset-state">数据源 · 已连接</div>
          </div>
        </div>

        <MfInput v-model="fieldSearch" class="dc-adhoc__search" :placeholder="$t('dataset.search.fields')">
          <template #prefix>
            <SearchOutlined :style="{ fontSize: '13px', color: 'var(--dc-text-secondary)' }"/>
          </template>
        </MfInput>

        <div class="dc-adhoc__field-tabs">
          <span class="dc-adhoc__field-tab"
                :class="{ 'dc-adhoc__field-tab--active': fieldTab === 'all' }"
                @click="fieldTab = 'all'">
            {{ $t('dataset.adhoc.all') }} ({{ originalMetrics.length + originalDimensions.length }})
          </span>
          <span class="dc-adhoc__field-tab"
                :class="{ 'dc-adhoc__field-tab--active': fieldTab === 'dimension' }"
                @click="fieldTab = 'dimension'">
            {{ $t('dataset.adhoc.dimensions') }} ({{ originalDimensions.length }})
          </span>
          <span class="dc-adhoc__field-tab"
                :class="{ 'dc-adhoc__field-tab--active': fieldTab === 'metric' }"
                @click="fieldTab = 'metric'">
            {{ $t('dataset.adhoc.measures') }} ({{ originalMetrics.length }})
          </span>
        </div>

        <div class="relative">
          <div v-if="!initialize && showMetrics" class="dc-adhoc__group">
            <div class="dc-adhoc__group-title">
              <BarChartOutlined class="dc-adhoc__group-icon"/>
              <span>{{ $t('dataset.common.columnModeMetric') }}</span>
            </div>
            <Draggable item-key="id"
                       :group="{ name: 'metrics', pull: 'clone', put: false }"
                       :list="visibleMetrics"
                       :clone="onClone"
                       @start="visibleHighlight(true, ColumnType.METRIC)"
                       @end="visibleHighlight(false, ColumnType.METRIC)">
              <template #item="{ element }">
                <div class="dc-adhoc__field">
                  <span class="dc-adhoc__field-badge">#</span>
                  <span class="dc-adhoc__field-name">{{ element.aliasName ? element.aliasName : element.name }}</span>
                </div>
              </template>
            </Draggable>
            <div v-if="!visibleMetrics.length" class="dc-adhoc__field-empty">{{ $t('common.noData') }}</div>
          </div>

          <div v-if="!initialize && showDimensions" class="dc-adhoc__group">
            <div class="dc-adhoc__group-title">
              <AppstoreOutlined class="dc-adhoc__group-icon"/>
              <span>{{ $t('dataset.common.columnModeDimension') }}</span>
            </div>
            <Draggable item-key="id"
                       :group="{ name: 'dimensions', pull: 'clone', put: false }"
                       :list="visibleDimensions"
                       :clone="onClone"
                       @start="visibleHighlight(true, ColumnType.DIMENSION)"
                       @end="visibleHighlight(false, ColumnType.DIMENSION)">
              <template #item="{ element }">
                <div class="dc-adhoc__field">
                  <span class="dc-adhoc__field-badge dc-adhoc__field-badge--text">Aa</span>
                  <span class="dc-adhoc__field-name">{{ element.aliasName ? element.aliasName : element.name }}</span>
                </div>
              </template>
            </Draggable>
            <div v-if="!visibleDimensions.length" class="dc-adhoc__field-empty">{{ $t('common.noData') }}</div>
          </div>

          <div v-if="initialize" class="absolute inset-0 z-10 flex items-center justify-center">
            <MfSpinner size="lg" label="Loading..."/>
          </div>
        </div>
      </div>
    </div>

    <div>
        <div class="dc-adhoc__main">
          <div class="dc-adhoc__head">
            <div class="min-w-0">
              <div class="dc-adhoc__title">{{ $t('dataset.adhoc.title') }}</div>
              <div class="dc-adhoc__desc">{{ $t('dataset.adhoc.desc') }}</div>
            </div>
            <div class="dc-adhoc__head-actions">
              <MfButton @click="onClear">
                <ClearOutlined/>
                {{ $t('dataset.adhoc.clear') }}
              </MfButton>
              <MfButton :disabled="!showSql.content || loading" @click="visibleShowSql(true)">
                <EyeOutlined/>
                {{ $t('dataset.adhoc.preview') }}
              </MfButton>
              <MfButton variant="primary" :disabled="loading" :loading="loading" @click="onApplyAdhoc">
                <PlayCircleOutlined/>
                {{ $t('dataset.adhoc.query') }}
              </MfButton>
              <MfDropdown placement="bottomRight">
                <MfButton><EllipsisOutlined/></MfButton>
                <template #overlay>
                  <MfMenu>
                    <MfMenuItem key_="publish" @click="publishVisible = true">{{ $t('common.publish') }}</MfMenuItem>
                    <MfMenuItem key_="limit">
                      <div class="flex items-center gap-2" @click.stop>
                        <span>{{ $t('dataset.common.showPageSize') }}</span>
                        <MfInputNumber v-model="configure.limit" :min="1" size="sm" style="width: 90px;"/>
                      </div>
                    </MfMenuItem>
                  </MfMenu>
                </template>
              </MfDropdown>
            </div>
          </div>

          <div class="relative">
            <div class="dc-adhoc__zone dc-adhoc__zone--metric"
                 :class="{ 'dc-adhoc__zone--highlight': highlight.active && highlight.type === ColumnType.METRIC }">
              <div class="dc-adhoc__zone-head dc-adhoc__zone-head--green">
                <BarChartOutlined class="dc-adhoc__zone-icon"/>
                <span class="dc-adhoc__zone-label">{{ $t('dataset.common.columnModeMetric') }} (Measures)</span>
                <span class="dc-adhoc__zone-hint">{{ $t('dataset.adhoc.dropMetric') }}</span>
              </div>
              <div class="dc-adhoc__zone-body">
                <Draggable group="metrics" item-key="id" :list="metrics" class="flex flex-wrap gap-2">
                  <template #item="{ element, index }">
                    <MfTag class="dc-adhoc__chip">
                      <span class="flex items-center">
                        <DatasetColumnMetric :element="element"/>
                      </span>
                      <span class="ml-2 flex items-center space-x-1">
                        <MfTooltip :content="$t('common.configure')">
                          <SettingOutlined class="cursor-pointer hover:text-primary" @click="onColumnConfigure(true, element, ColumnType.METRIC)" :style="{ fontSize: '13px' }"/>
                        </MfTooltip>
                        <CloseOutlined class="cursor-pointer text-red-400 hover:text-red-500" @click="onRemove(index, metrics)" :style="{ fontSize: '11px' }"/>
                      </span>
                    </MfTag>
                  </template>
                </Draggable>
              </div>
            </div>

            <div class="dc-adhoc__zone dc-adhoc__zone--dimension"
                 :class="{ 'dc-adhoc__zone--highlight': highlight.active && highlight.type === ColumnType.DIMENSION }">
              <div class="dc-adhoc__zone-head dc-adhoc__zone-head--blue">
                <AppstoreOutlined class="dc-adhoc__zone-icon"/>
                <span class="dc-adhoc__zone-label">{{ $t('dataset.common.columnModeDimension') }} (Dimensions)</span>
                <span class="dc-adhoc__zone-hint">{{ $t('dataset.adhoc.dropDimension') }}</span>
              </div>
              <div class="dc-adhoc__zone-body">
                <Draggable group="dimensions" item-key="id" :list="dimensions" class="flex flex-wrap gap-2">
                  <template #item="{ element, index }">
                    <MfTag class="dc-adhoc__chip">
                      <span class="flex items-center">
                        {{ element.aliasName ? element.aliasName : element.name }}
                      </span>
                      <span class="ml-2 flex items-center space-x-1">
                        <MfTooltip :content="$t('common.configure')">
                          <SettingOutlined class="cursor-pointer hover:text-primary" @click="onColumnConfigure(true, element, ColumnType.DIMENSION)" :style="{ fontSize: '13px' }"/>
                        </MfTooltip>
                        <CloseOutlined class="cursor-pointer text-red-400 hover:text-red-500" @click="onRemove(index, dimensions)" :style="{ fontSize: '11px' }"/>
                      </span>
                    </MfTag>
                  </template>
                </Draggable>
              </div>
            </div>

            <div class="dc-adhoc__zone dc-adhoc__zone--filter"
                 :class="{ 'dc-adhoc__zone--highlight': highlight.active && highlight.type === ColumnType.FILTER }">
              <div class="dc-adhoc__zone-head dc-adhoc__zone-head--purple">
                <FilterOutlined class="dc-adhoc__zone-icon"/>
                <span class="dc-adhoc__zone-label">{{ $t('dataset.common.columnModeFilter') }} (Filters)</span>
                <span class="dc-adhoc__zone-hint">{{ $t('dataset.adhoc.dropFilter') }}</span>
              </div>
              <div class="dc-adhoc__zone-body">
                <Draggable group="dimensions" item-key="id" :list="filters" class="flex flex-wrap gap-2">
                  <template #item="{ element, index }">
                    <MfTag class="dc-adhoc__chip">
                      <span class="flex items-center">
                        {{ element.aliasName ? element.aliasName : element.name }}
                      </span>
                      <span class="ml-2 flex items-center space-x-1">
                        <MfTooltip :content="$t('common.configure')">
                          <SettingOutlined class="cursor-pointer hover:text-primary" @click="onColumnConfigure(true, element, ColumnType.FILTER)" :style="{ fontSize: '13px' }"/>
                        </MfTooltip>
                        <CloseOutlined class="cursor-pointer text-red-400 hover:text-red-500" @click="onRemove(index, filters)" :style="{ fontSize: '11px' }"/>
                      </span>
                    </MfTag>
                  </template>
                </Draggable>
              </div>
            </div>

            <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center">
              <MfSpinner size="lg" label="Loading..."/>
            </div>
          </div>

          <div class="dc-adhoc__result">
            <MfTabs v-model:active-key="resultTab" class="dc-adhoc__result-tabs">
              <MfTabPane item-key="result" :tab="$t('dataset.adhoc.resultTab')">
                <VisualEditor :loading="loading" :configuration="configuration as any" @commitOptions="visibleCommitOptions"/>
              </MfTabPane>
              <MfTabPane item-key="log" :tab="$t('dataset.adhoc.logTab')">
                <pre class="dc-adhoc__log">{{ showSql.content || $t('common.noData') }}</pre>
              </MfTabPane>
            </MfTabs>

            <div class="dc-adhoc__result-footer">
              <span class="dc-adhoc__result-total">{{ $t('dataset.adhoc.totalRows', { limit: configure.limit }) }}</span>
              <MfButton size="sm" @click="exportResults">
                <DownloadOutlined/>
                {{ $t('dataset.adhoc.export') }}
              </MfButton>
            </div>
          </div>
        </div>
      </div>
  </div>

    <SqlInfo v-if="showSql.visible"
             :is-visible="showSql.visible"
             :content="showSql.content"
             @close="visibleShowSql(false)"/>

    <DatasetColumnConfigure v-if="columnContent.visible"
                            :is-visible="columnContent.visible"
                            :column-type="columnContent.type"
                            :content="columnContent.content"
                            :configure="columnContent.configure"
                            @close="onColumnConfigure(false, null, null)"
                            @commit="onCommitColumnConfigure"/>

    <DatasetReport v-if="publishVisible"
                   :info="dataInfo as any"
                   :code="reportCode"
                   :dimension="originalDimensions[0] as any"
                   :visible="publishVisible"
                   :commit-options="commitOptions as any"
                   :configure="configure as any"
                   @close="visiblePublish(false)"/>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import Draggable from 'vuedraggable'
import DatasetService from '@/services/dataset'
import { Type as ColumnType } from './Type'
import ReportService from '@/services/report'
import { cloneDeep } from 'lodash'
import { Configuration } from '@/views/components/visual/Configuration'
import VisualEditor from '@/views/components/visual/VisualEditor.vue'
import DatasetColumnMetric from '@/views/pages/admin/dataset/components/adhoc/DatasetColumnMetric.vue'
import DatasetColumnConfigure from '@/views/pages/admin/dataset/components/adhoc/DatasetColumnConfigure.vue'
import SqlInfo from '@/views/components/sql/SqlInfo.vue'
import DatasetReport from '@/views/pages/admin/dataset/components/DatasetReport.vue'
import {
  AppstoreOutlined,
  BarChartOutlined,
  ClearOutlined,
  CloseOutlined,
  DatabaseOutlined,
  DownloadOutlined,
  EllipsisOutlined,
  EyeOutlined,
  FilterOutlined,
  PlayCircleOutlined,
  SearchOutlined,
  SettingOutlined
} from '@ant-design/icons-vue'

defineOptions({ name: 'DatasetAdhoc' })

const { t } = useI18n()

const route = useRoute()

const loading = ref(false)
const code = ref<string | null>(null)
const reportCode = ref<any>(null)
const originalMetrics = ref<any[]>([])
const originalDimensions = ref<any[]>([])
const originalData = ref<any[]>([])
const metrics = ref<any[]>([])
const dimensions = ref<any[]>([])
const filters = ref<any[]>([])
const fieldSearch = ref('')
const fieldTab = ref<'all' | 'dimension' | 'metric'>('all')
const configure = reactive({
  columns: [] as any[],
  limit: 1000
})
const configuration = ref<Configuration | null>(null)
const showSql = reactive({
  visible: false,
  content: null as string | null
})
const columnContent = reactive({
  visible: false,
  type: null as ColumnType | null,
  content: [] as never[],
  configure: null as any | null
})
const commitOptions = ref<any>(null)
const publishVisible = ref(false)
const initialize = ref(false)
const highlight = reactive({
  active: false,
  type: 'METRIC' as any
})
const dataInfo = reactive({ name: null as string | null, description: null as string | null })
const resultTab = ref('result')

const matchesSearch = (item: any): boolean => {
  const keyword = fieldSearch.value?.toLowerCase() || ''
  if (!keyword) {
    return true
  }
  return (item.aliasName || '').toLowerCase().includes(keyword)
      || (item.name || '').toLowerCase().includes(keyword)
}

const visibleMetrics = computed(() => originalMetrics.value.filter(matchesSearch))
const visibleDimensions = computed(() => originalDimensions.value.filter(matchesSearch))
const showMetrics = computed(() => fieldTab.value === 'all' || fieldTab.value === 'metric')
const showDimensions = computed(() => fieldTab.value === 'all' || fieldTab.value === 'dimension')

const splitColumns = (original: any[]): any[] => {
  const array: any[] = []
  original.forEach((item: any) => array.push({
    id: item.id,
    mode: item.mode,
    alias: item.alias,
    expression: item.expression,
    name: item.name,
    function: item.function,
    value: item.value,
    order: item.order
  }))
  return array
}

const onAdhoc = () => {
  loading.value = true
  DatasetService.adhoc(code.value as string, configure)
                .then((response) => {
                  if (response.status) {
                    if (configuration.value) {
                      if (response.data.isSuccessful) {
                        configuration.value.headers = response.data.headers
                        configuration.value.columns = response.data.columns
                        showSql.content = response.data.content
                        configuration.value.message = null
                      }
                      else {
                        configuration.value.headers = []
                        configuration.value.columns = []
                        configuration.value.message = response.data.message
                      }
                    }
                  }
                  else {
                    MfMessage.error(response.message)
                  }
                })
                .finally(() => (loading.value = false))
}

const onApplyAdhoc = () => {
  filters.value.forEach((item: any) => (item.mode = ColumnType.FILTER))
  configure.columns = [...splitColumns(metrics.value), ...splitColumns(dimensions.value), ...splitColumns(filters.value)]
  onAdhoc()
}

const mergeColumns = (originalColumns: any[], array: any[], type?: ColumnType) => {
  originalColumns.filter((item: any) => {
    const column = originalData.value.filter((value: any) => value.id === item.id)[0]
    if (type) {
      if (item.mode === type) {
        Object.assign(column, item)
        array.push(column)
      }
    }
    else {
      Object.assign(column, item)
      array.push(column)
    }
  })
}

const handleInitialize = () => {
  setTimeout(() => {
    initialize.value = true
    const routeCode = route.params.code as string
    code.value = routeCode
    const id = route.params.id
    reportCode.value = id
    DatasetService.getColumnsByCode(code.value)
                  .then((response) => {
                    if (response.status) {
                      originalData.value = response.data
                      originalMetrics.value = response.data.filter((item: any) => item.mode === 'METRIC')
                      originalDimensions.value = response.data.filter((item: any) => item.mode === 'DIMENSION')
                      if (id) {
                        ReportService.getByCode(id as string)
                                     .then((reportResponse) => {
                                       if (reportResponse.status) {
                                         dataInfo.name = reportResponse.data.name
                                         dataInfo.description = reportResponse.data.description
                                         const query = JSON.parse(reportResponse.data.query)
                                         mergeColumns(query.columns, metrics.value, ColumnType.METRIC)
                                         mergeColumns(query.columns, dimensions.value, ColumnType.DIMENSION)
                                         mergeColumns(query.columns, filters.value, ColumnType.FILTER)
                                         configure.columns = query.columns
                                         configure.limit = query.limit
                                         configuration.value = JSON.parse(reportResponse.data.configure)
                                         onApplyAdhoc()
                                       }
                                     })
                      }
                    }
                    else {
                      MfMessage.error(response.message)
                    }
                  })
                  .finally(() => (initialize.value = false))
  }, 0)
}

const onClone = (value: any) => cloneDeep(value)

const onRemove = (index: number, array: any[]) => {
  array.splice(index, 1)
  onApplyAdhoc()
}

const onColumnConfigure = (opened: boolean, record: any, type: ColumnType | null) => {
  columnContent.visible = opened
  columnContent.type = type
  if (record) {
    columnContent.content = originalData.value.find((item: any) => item.id === record.id) as unknown as never[]
    const foundIndex = configure.columns.findIndex((item: any) => item.id === record.id)
    if (foundIndex !== -1) {
      const column = configure.columns[foundIndex]
      column.type = record.type
      columnContent.configure = column
    }
    else {
      columnContent.configure = { id: record.id, type: record.type }
    }
  }
  else {
    columnContent.configure = null
  }
}

const replaceColumn = (originalColumns: any[], originalValue: any) => {
  const index = originalColumns.findIndex((item: any) => item.id === originalValue.id)
  if (index !== -1) {
    const cloneValue = cloneDeep(originalValue)
    originalColumns[index] = Object.assign(originalValue, originalColumns[index], cloneValue)
  }
}

const onCommitColumnConfigure = (value: any) => {
  const clonedValue = cloneDeep(value)
  if (clonedValue.mode === ColumnType.METRIC) {
    replaceColumn(metrics.value, clonedValue)
  }
  else if (clonedValue.mode === ColumnType.DIMENSION) {
    replaceColumn(dimensions.value, clonedValue)
  }
  else if (clonedValue.mode === ColumnType.FILTER) {
    replaceColumn(filters.value, clonedValue)
  }
  onApplyAdhoc()
}

const visibleShowSql = (opened: boolean) => {
  showSql.visible = opened
}

const visibleCommitOptions = (value: any) => {
  commitOptions.value = value
}

const visibleHighlight = (opened: boolean, type: any) => {
  highlight.active = opened
  highlight.type = type
}

const visiblePublish = (opened: boolean) => {
  publishVisible.value = opened
}

const onClear = () => {
  metrics.value = []
  dimensions.value = []
  filters.value = []
}

const exportResults = () => {
  if (!configuration.value || !configuration.value.headers.length) {
    MfMessage.error(t('common.noData') as string)
    return
  }
  const headers = configuration.value.headers
  const columns = configuration.value.columns
  const escape = (v: any) => {
    const s = v === null || v === undefined ? '' : String(v)
    return /[",\n]/.test(s) ? `"${ s.replace(/"/g, '""') }"` : s
  }
  const csv = [headers.map((h: any) => escape(h)).join(','), ...columns.map((row: any) => headers.map((_header: any, index: number) => escape(row[index])).join(','))].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `adhoc-${ Date.now() }.csv`
  link.click()
  URL.revokeObjectURL(url)
}

configuration.value = new Configuration()
handleInitialize()
</script>

<style scoped>
.dc-adhoc__side {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    padding: 14px;
    min-height: calc(100vh - 200px);
}

.dc-adhoc__dataset {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-primary-surface);
    border: 1px solid var(--dc-border-light);
    margin-bottom: 10px;
}

.dc-adhoc__dataset-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: var(--dc-radius-md);
    background: var(--dc-surface);
    flex-shrink: 0;
}

.dc-adhoc__dataset-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-adhoc__dataset-state {
    font-size: 11px;
    color: var(--dc-success);
}

.dc-adhoc__search {
    margin-bottom: 10px;
    border-radius: var(--dc-radius-sm);
}

.dc-adhoc__field-tabs {
    display: flex;
    gap: 12px;
    border-bottom: 1px solid var(--dc-border-light);
    padding-bottom: 6px;
    margin-bottom: 8px;
}

.dc-adhoc__field-tab {
    font-size: 12px;
    color: var(--dc-text-secondary);
    cursor: pointer;
    white-space: nowrap;
}

.dc-adhoc__field-tab--active {
    color: var(--dc-primary);
    font-weight: 600;
}

.dc-adhoc__group {
    margin-bottom: 10px;
}

.dc-adhoc__group-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 600;
    color: var(--dc-text-title);
    padding: 4px 0;
}

.dc-adhoc__group-icon {
    color: var(--dc-text-secondary);
}

.dc-adhoc__field {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 6px;
    border-radius: var(--dc-radius-sm);
    cursor: grab;
    font-size: 12px;
    color: var(--dc-text-primary);
}

.dc-adhoc__field:hover {
    background: var(--dc-menu-hover);
}

.dc-adhoc__field-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 16px;
    border-radius: 3px;
    background: var(--dc-info-bg);
    color: var(--dc-info);
    font-size: 10px;
    font-weight: 600;
    flex-shrink: 0;
}

.dc-adhoc__field-badge--text {
    font-size: 9px;
}

.dc-adhoc__field-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.dc-adhoc__field-empty {
    font-size: 11px;
    color: var(--dc-text-placeholder);
    padding: 4px 6px;
}

.dc-adhoc__main {
    background: var(--dc-surface);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-lg);
    box-shadow: var(--dc-shadow-card);
    padding: 20px;
}

.dc-adhoc__head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
}

.dc-adhoc__title {
    font-size: 20px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-adhoc__desc {
    font-size: 12px;
    color: var(--dc-text-secondary);
    margin-top: 4px;
}

.dc-adhoc__head-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

.dc-adhoc__zone {
    border: 1px solid var(--dc-border-light);
    border-left: 3px solid var(--dc-border);
    border-radius: var(--dc-radius-md);
    background: var(--dc-bg);
    padding: 10px 14px;
    margin-bottom: 10px;
}

.dc-adhoc__zone--metric {
    border-left-color: var(--dc-success);
}

.dc-adhoc__zone--dimension {
    border-left-color: var(--dc-info);
}

.dc-adhoc__zone--filter {
    border-left-color: var(--dc-primary);
}

.dc-adhoc__zone--highlight {
    box-shadow: var(--dc-shadow-focus);
}

.dc-adhoc__zone-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}

.dc-adhoc__zone-icon {
    color: var(--dc-text-secondary);
    font-size: 14px;
}

.dc-adhoc__zone-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--dc-text-title);
}

.dc-adhoc__zone-hint {
    font-size: 11px;
    color: var(--dc-text-placeholder);
}

.dc-adhoc__zone-body {
    min-height: 36px;
    display: flex;
    align-items: center;
}

.dc-adhoc__chip {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: var(--dc-radius-sm);
    background: var(--dc-surface);
    border: 1px solid var(--dc-border);
    font-size: 12px;
    color: var(--dc-text-primary);
    margin-inline-end: 0;
}

.dc-adhoc__result-tabs {
    margin-bottom: 8px;
}

.dc-adhoc__result-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 10px;
}

.dc-adhoc__result-total {
    font-size: 12px;
    color: var(--dc-text-secondary);
}

.dc-adhoc__log {
    background: var(--dc-bg);
    border: 1px solid var(--dc-border-light);
    border-radius: var(--dc-radius-md);
    padding: 12px;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 400px;
    overflow: auto;
}
</style>
