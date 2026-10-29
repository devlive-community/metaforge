<template>
  <div class="flex min-h-0 flex-1 flex-col overflow-hidden">
    <div class="flex shrink-0 items-center border-b border-line px-1">
      <MfTooltip v-for="tab in tabs"
                 :key="tab.key"
                 :content="tab.label"
                 class="min-w-0 flex-1">
        <button type="button"
                class="w-full truncate whitespace-nowrap border-b-2 px-1 py-2.5 text-center text-sm font-medium transition-[color,border-color] duration-(--mf-duration-base) ease-(--mf-easing)"
                :class="activeTab === tab.key ? 'border-b-primary text-primary' : 'border-b-transparent text-content-secondary hover:text-content'"
                @click="activeTab = tab.key">
          {{ tab.label }}
        </button>
      </MfTooltip>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-2.5 overflow-y-auto p-3">
      <template v-if="activeTab === 'datasets'">
        <MfButton variant="primary" size="sm" block :disabled="!sourceCode" @click="emit('add-dataset')">
          <Plus :size="14"/>
          {{ $t('query.side.addDataset') }}
        </MfButton>

        <MfInput v-model="datasetKeyword" size="sm" :placeholder="$t('query.side.searchDataset')">
          <template #prefix>
            <Search :size="13"/>
          </template>
        </MfInput>

        <div class="flex flex-col gap-2">
          <div v-for="item in filteredDatasets"
               :key="item.code"
               class="group rounded-mf-md border border-line px-2.5 py-2 transition-[border-color,background-color] duration-(--mf-duration-fast) hover:border-line-strong hover:bg-surface-hover">
            <div class="flex items-center gap-2">
              <Database :size="14" class="shrink-0 text-content-tertiary"/>
              <span class="min-w-0 flex-1 cursor-pointer truncate text-sm font-medium text-content" @click="emit('load-sql', item.query ?? '')">
                {{ item.name }}
              </span>
              <MfTooltip :content="$t('query.side.loadAndRun')">
                <button type="button"
                        class="inline-flex size-6 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary hover:bg-surface-active hover:text-primary"
                        @click="emit('run-sql', item.query ?? '')">
                  <Play :size="13"/>
                </button>
              </MfTooltip>
              <MfDropdown trigger="click" placement="bottomRight">
                <button type="button" class="inline-flex size-6 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary hover:bg-surface-hover hover:text-content">
                  <EllipsisVertical :size="14"/>
                </button>
                <template #overlay>
                  <div class="flex flex-col">
                    <button type="button" class="rounded-mf-md px-2.5 py-1.5 text-left text-sm text-content-secondary hover:bg-surface-hover hover:text-content" @click="onCopySql(item.query)">
                      {{ $t('query.side.copySql') }}
                    </button>
                    <button v-if="item.code" type="button" class="rounded-mf-md px-2.5 py-1.5 text-left text-sm text-content-secondary hover:bg-surface-hover hover:text-content" @click="router.push(`/dataset/detail/${ item.code }`)">
                      {{ $t('query.side.openDetail') }}
                    </button>
                  </div>
                </template>
              </MfDropdown>
            </div>
            <div class="mt-1 truncate font-mono text-xs text-content-tertiary">{{ item.query }}</div>
          </div>

          <MfEmpty v-if="!loadingDatasets && filteredDatasets.length === 0" size="sm" :title="$t('query.side.empty')"/>
        </div>
      </template>

      <template v-else-if="activeTab === 'snippets'">
        <MfButton size="sm" block @click="emit('create-snippet')">
          <Plus :size="14"/>
          {{ $t('template.common.create') }}
        </MfButton>

        <MfInput v-model="snippetKeyword" size="sm" :placeholder="$t('query.side.searchTemplate')">
          <template #prefix>
            <Search :size="13"/>
          </template>
        </MfInput>

        <div class="flex flex-col gap-2">
          <div v-for="item in filteredSnippets"
               :key="item.code"
               class="group rounded-mf-md border border-line px-2.5 py-2 transition-[border-color,background-color] duration-(--mf-duration-fast) hover:border-line-strong hover:bg-surface-hover">
            <div class="flex items-center gap-2">
              <SquareDashedBottomCode :size="14" class="shrink-0 text-content-tertiary"/>
              <span class="min-w-0 flex-1 cursor-pointer truncate text-sm font-medium text-content" @click="emit('load-sql', item.context ?? '')">
                {{ item.name }}
              </span>
              <MfTooltip :content="$t('query.side.loadAndRun')">
                <button type="button"
                        class="inline-flex size-6 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary hover:bg-surface-active hover:text-primary"
                        @click="emit('run-sql', item.context ?? '')">
                  <Play :size="13"/>
                </button>
              </MfTooltip>
              <MfDropdown trigger="click" placement="bottomRight">
                <button type="button" class="inline-flex size-6 shrink-0 items-center justify-center rounded-mf-sm text-content-tertiary hover:bg-surface-hover hover:text-content">
                  <EllipsisVertical :size="14"/>
                </button>
                <template #overlay>
                  <div class="flex flex-col">
                    <button type="button" class="rounded-mf-md px-2.5 py-1.5 text-left text-sm text-content-secondary hover:bg-surface-hover hover:text-content" @click="emit('edit-snippet', item)">
                      {{ $t('query.side.edit') }}
                    </button>
                    <button type="button" class="rounded-mf-md px-2.5 py-1.5 text-left text-sm text-content-secondary hover:bg-surface-hover hover:text-content" @click="onCopySql(item.context)">
                      {{ $t('query.side.copySql') }}
                    </button>
                  </div>
                </template>
              </MfDropdown>
            </div>
            <div class="mt-1 truncate font-mono text-xs text-content-tertiary">{{ item.context }}</div>
          </div>

          <MfEmpty v-if="!loadingSnippets && filteredSnippets.length === 0" size="sm" :title="$t('query.side.empty')"/>
        </div>
      </template>

      <template v-else-if="activeTab === 'structure'">
        <MfEmpty v-if="!selectedTable" size="sm" variant="initial" :title="$t('query.side.structureTip')"/>

        <template v-else>
          <div class="flex items-center gap-1.5 text-sm text-content-secondary">
            <Table :size="14" class="text-content-tertiary"/>
            <span class="truncate font-medium text-content">{{ selectedTable.database }}.{{ selectedTable.table }}</span>
          </div>

          <div v-if="loadingStructure" class="flex justify-center py-6">
            <MfSpinner size="md"/>
          </div>

          <div v-else class="flex flex-col gap-1">
            <div v-for="column in structureColumns"
                 :key="column.object_name"
                 class="flex cursor-pointer items-center gap-2 rounded-mf-md px-2 py-1.5 hover:bg-surface-hover"
                 @click="onCopySql(column.object_name)">
              <Columns3 :size="13" class="shrink-0 text-content-tertiary"/>
              <span class="min-w-0 flex-1 truncate font-mono text-xs text-content">{{ column.object_name }}</span>
              <MfTag size="sm">{{ column.type_name }}</MfTag>
            </div>

            <MfEmpty v-if="structureColumns.length === 0" size="sm" :title="$t('query.side.empty')"/>
          </div>
        </template>
      </template>

      <template v-else>
        <MfInput v-model="historyKeyword" size="sm" :placeholder="$t('query.side.searchHistory')">
          <template #prefix>
            <Search :size="13"/>
          </template>
        </MfInput>

        <div class="flex flex-col gap-2">
          <div v-for="item in filteredHistories"
               :key="item.code"
               class="group cursor-pointer rounded-mf-md border border-line px-2.5 py-2 transition-[border-color,background-color] duration-(--mf-duration-fast) hover:border-line-strong hover:bg-surface-hover"
               @click="emit('load-sql', item.content ?? '')">
            <div class="flex items-center gap-2">
              <span class="size-1.5 shrink-0 rounded-full" :class="item.state === 'SUCCESS' ? 'bg-success' : 'bg-danger'"></span>
              <span class="min-w-0 flex-1 truncate font-mono text-xs text-content">{{ item.content }}</span>
            </div>
            <div class="mt-1 flex items-center gap-2 pl-3.5 text-xs text-content-tertiary">
              <span>{{ item.createTime }}</span>
              <span v-if="item.elapsed !== null && item.elapsed !== undefined">{{ item.elapsed }} ms</span>
            </div>
          </div>

          <MfEmpty v-if="!loadingHistories && filteredHistories.length === 0" size="sm" :title="$t('query.side.empty')"/>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { MfMessage } from 'metaforge-ui'
import {
  Columns3,
  Database,
  EllipsisVertical,
  Play,
  Plus,
  Search,
  SquareDashedBottomCode,
  Table
} from '@lucide/vue'
import DatasetService from '@/services/dataset'
import SnippetService from '@/services/snippet'
import AuditService from '@/services/audit'
import MetadataService from '@/services/metadata.ts'
import { FilterModel } from '@/model/filter.ts'
import { DatasetModel } from '@/model/dataset'
import { SnippetModel } from '@/model/snippet'
import { HistoryModel } from '@/model/history'
import { ObjectUtils } from '@/utils/object'

defineOptions({ name: 'QuerySidePanel' })

const props = withDefaults(defineProps<{
  sourceCode?: string
  selectedTable?: { database: string; table: string } | null
  /** Incremented by the parent when snippet/dataset mutations happen elsewhere. */
  refreshed?: number
}>(), {
  sourceCode: undefined,
  selectedTable: null,
  refreshed: 0
})

const emit = defineEmits<{
  (e: 'run-sql', sql: string): void
  (e: 'load-sql', sql: string): void
  (e: 'add-dataset'): void
  (e: 'create-snippet'): void
  (e: 'edit-snippet', snippet: SnippetModel): void
}>()

const { t } = useI18n()
const router = useRouter()

const activeTab = ref<'datasets' | 'snippets' | 'structure' | 'history'>('datasets')

const tabs = computed(() => [
  { key: 'datasets' as const, label: t('common.dataset') },
  { key: 'snippets' as const, label: t('common.template') },
  { key: 'structure' as const, label: t('source.common.structure') },
  { key: 'history' as const, label: t('common.history') }
])

const datasets = ref<DatasetModel[]>([])
const snippets = ref<SnippetModel[]>([])
const histories = ref<HistoryModel[]>([])
const structureColumns = ref<any[]>([])

const loadingDatasets = ref(false)
const loadingSnippets = ref(false)
const loadingHistories = ref(false)
const loadingStructure = ref(false)

const datasetKeyword = ref('')
const snippetKeyword = ref('')
const historyKeyword = ref('')

const historyFilter: FilterModel = new FilterModel()
historyFilter.size = 50

const filteredDatasets = computed(() => filterList(datasets.value, datasetKeyword.value, ['name', 'query']))
const filteredSnippets = computed(() => filterList(snippets.value, snippetKeyword.value, ['name', 'context']))
const filteredHistories = computed(() => filterList(histories.value, historyKeyword.value, ['content']))

const filterList = <T extends Record<string, any>>(list: T[], keyword: string, fields: string[]): T[] => {
  const kw = keyword.trim().toLowerCase()
  if (!kw) {
    return list
  }
  return list.filter(item => fields.some(field => String(item[field] ?? '').toLowerCase().includes(kw)))
}

const handleInitialize = () => {
  loadingDatasets.value = true
  DatasetService.getAll(new FilterModel())
                .then((response) => {
                  if (response.status) {
                    datasets.value = response.data.content ?? []
                  }
                })
                .finally(() => (loadingDatasets.value = false))

  loadingSnippets.value = true
  SnippetService.getAll(new FilterModel())
                .then((response) => {
                  if (response.status) {
                    snippets.value = response.data.content ?? []
                  }
                })
                .finally(() => (loadingSnippets.value = false))

  loadingHistories.value = true
  AuditService.getAll(historyFilter)
              .then((response) => {
                if (response.status) {
                  histories.value = response.data.content ?? []
                }
              })
              .finally(() => (loadingHistories.value = false))
}

const loadStructure = () => {
  if (!props.sourceCode || !props.selectedTable) {
    structureColumns.value = []
    return
  }
  loadingStructure.value = true
  MetadataService.getColumnsByTable(props.sourceCode, props.selectedTable.database, props.selectedTable.table)
                 .then((response) => {
                   if (response.status && response.data && response.data.isSuccessful) {
                     structureColumns.value = response.data.columns.filter((value: any) => value.type_name === 'column')
                   }
                   else {
                     structureColumns.value = []
                   }
                 })
                 .finally(() => (loadingStructure.value = false))
}

const onCopySql = (value?: string) => {
  if (value) {
    ObjectUtils.copy(value)
    MfMessage.success(t('query.tip.copySuccess'))
  }
}

watch(() => props.selectedTable, () => loadStructure())

/** Reload the panel lists when the data source changes so structure/history stay relevant. */
watch(() => props.sourceCode, () => handleInitialize())

watch(() => props.refreshed, () => handleInitialize())

handleInitialize()
</script>
