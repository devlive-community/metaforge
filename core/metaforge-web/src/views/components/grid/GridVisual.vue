<template>
  <MfModal :open="isVisible" :title="$t('queryChart.createTitle')" width="min(1536px, 96vw)" height="min(1260px, 96dvh)"
           body-class="overflow-hidden" panel-class="[&>header>div]:flex-1" overlay-class="backdrop-blur-[2px]" trap-focus :close-on-overlay="false" :close-on-esc="!publishing" :close-disabled="publishing" @close="close">
    <template #title>
      <span class="flex items-center gap-3">
        <span class="flex size-10 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary"><ChartColumn :size="22"/></span>
        <span class="min-w-0">
          <span class="block text-lg">{{ $t('queryChart.createTitle') }}</span>
          <span class="block truncate text-xs font-normal text-content-tertiary">{{ sourceLabel }} · {{ sizeText }}</span>
        </span>
        <MfTag class="ml-auto hidden sm:inline-flex" size="sm" variant="success">{{ $t('query.message.success') }}</MfTag>
        <MfTag class="hidden sm:inline-flex" size="sm">{{ $t('query.message.elapsed') }} {{ source.elapsed }} ms</MfTag>
        <MfTooltip :content="$t('queryChart.help')"><MfButton size="sm" variant="ghost"><Info :size="15"/><span class="hidden sm:inline">{{ $t('queryChart.helpTitle') }}</span></MfButton></MfTooltip>
      </span>
    </template>

    <div class="grid h-full min-h-0 grid-cols-[minmax(140px,14%)_minmax(270px,34%)_minmax(0,1fr)] max-md:grid-cols-1 max-md:overflow-auto">
      <aside class="flex min-h-0 flex-col justify-between gap-4 overflow-auto border-r border-line bg-surface-hover/40 p-3 max-md:shrink-0">
        <div>
          <div class="px-2 py-2 text-xs text-content-tertiary">{{ $t('queryChart.settings') }}</div>
          <nav :aria-label="$t('queryChart.settings')" class="flex flex-col gap-1 max-md:flex-row max-md:overflow-x-auto">
            <button v-for="item in panels" :key="item.key" type="button" :aria-current="panel === item.key ? 'true' : undefined"
                    class="flex shrink-0 items-center gap-2 whitespace-nowrap rounded-mf-md px-3 py-2.5 text-left font-medium"
                    :class="panel === item.key ? 'bg-surface-active text-primary' : 'text-content-secondary hover:bg-surface-hover'"
                    @click="setPanel(item.key)">
              <component :is="item.icon" :size="16"/>{{ $t('queryChart.panel.' + item.key) }}
            </button>
          </nav>
        </div>
        <div class="rounded-mf-md border border-line bg-primary/5 p-3 text-xs text-content-secondary max-md:hidden">
          <strong class="block text-primary">{{ $t('queryChart.locked') }}</strong>{{ sourceLabel }} · {{ sizeText }}
        </div>
      </aside>

      <main ref="editor" class="min-h-0 min-w-0 overflow-y-auto border-r border-line p-5 max-md:overflow-visible">
        <div class="mb-4 flex flex-wrap items-start justify-between gap-2">
          <div>
            <h2 class="text-base font-semibold">{{ $t('queryChart.panel.' + panel) }}</h2>
            <p class="mt-1 text-xs text-content-tertiary">{{ $t('queryChart.desc.' + panel) }}</p>
          </div>
          <MfTag v-if="panel === 'fields'" size="sm">{{ options.fields.length }} / {{ source.headers.length }}</MfTag>
          <MfTag v-if="panel === 'filters'" size="sm">{{ $t('queryChart.ruleCount', { count: ruleCount }) }}</MfTag>
        </div>

        <MfForm ref="form" :model="options" :disabled="publishing" class="gap-4" @finish="publish">
          <template v-if="panel === 'chart'">
            <section class="space-y-3">
              <h4 class="font-semibold">{{ $t('queryChart.chartType') }}</h4>
              <div class="grid grid-cols-4 gap-2">
                <button v-for="kind in chartCards" :key="kind" type="button" :aria-pressed="options.type === kind"
                        class="flex min-w-0 flex-col items-center justify-center gap-2 rounded-mf-md border px-1 py-4 text-center text-xs"
                        :class="options.type === kind ? 'border-primary bg-primary/5 text-primary' : 'border-line hover:bg-surface-hover'"
                        @click="selectChartType(kind)">
                  <QueryChartTypeIcon :type="kind" :size="21" class="text-primary"/>{{ $t('queryChart.type.' + kind) }}
                </button>
                <MfDropdown trigger="click" placement="bottomRight">
                  <button type="button" :aria-label="$t('queryChart.more')" class="flex h-full w-full flex-col items-center justify-center gap-2 rounded-mf-md border border-line px-1 py-4 text-xs hover:bg-surface-hover">
                    <QueryChartTypeIcon v-if="moreTypes.includes(options.type)" :type="options.type" :size="21" class="text-primary"/>
                    <Ellipsis v-else :size="21" class="text-primary"/>
                    {{ moreTypes.includes(options.type) ? $t('queryChart.type.' + options.type) : $t('queryChart.more') }}
                  </button>
                  <template #overlay>
                    <div class="flex max-h-[45dvh] min-w-40 flex-col gap-1 overflow-y-auto">
                      <MfButton v-for="kind in moreTypes" :key="kind" size="sm" :variant="kind === options.type ? 'secondary' : 'ghost'"
                                class="shrink-0 justify-start!" :aria-pressed="kind === options.type" @click="selectChartType(kind)">
                        <QueryChartTypeIcon :type="kind" class="shrink-0 text-primary"/>{{ $t('queryChart.type.' + kind) }}
                      </MfButton>
                    </div>
                  </template>
                </MfDropdown>
              </div>
            </section>

            <section class="space-y-3 border-t border-line pt-4">
              <h4 class="font-semibold">{{ $t('queryChart.basic') }}</h4>
              <MfFormItem name="name" :label="$t('queryChart.name')" :rules="[{ required: true, validator: value => !!String(value ?? '').trim(), message: $t('report.validator.name.required') }]">
                <MfInput v-model="options.name" :maxlength="50" :placeholder="$t('report.placeholder.name')"/>
              </MfFormItem>
              <MfFormItem name="description" :label="$t('common.description')">
                <MfTextarea v-model="options.description" :rows="2" :maxlength="500" :placeholder="$t('report.placeholder.description')"/>
              </MfFormItem>
            </section>

            <section v-if="options.type !== 'table'" class="space-y-3 border-t border-line pt-4">
              <h4 class="font-semibold">{{ $t(options.type === 'map' || options.type === 'custom' ? 'queryChart.mapping.title.' + options.type : 'queryChart.dimensions') }}</h4>
              <QueryChartDataFields v-if="specializedData" :options="options" :fields="fieldOptions"
                                    :numeric-fields="numericOptions" @change="changeMappings"/>
              <MfFormItem v-else name="category" :label="$t('queryChart.category')" required-mark>
                <div class="grid w-full grid-cols-2 gap-2">
                  <MfSelect :model-value="options.category" :options="fieldOptions" searchable @update:model-value="changeCategory(String($event))"/>
                  <MfSelect v-model="options.grain" :disabled="!timeCategory" :options="choices('grain', ['none', 'hour', 'day', 'week', 'month'])"/>
                </div>
              </MfFormItem>
              <MfFormItem v-if="!specializedData?.raw" name="metric" :label="$t('queryChart.metric')" required-mark>
                <div class="grid w-full grid-cols-2 gap-2">
                  <MfSelect :model-value="options.aggregation === 'count' && !options.metric ? 0 : options.metric" :options="metricOptions" searchable
                            :placeholder="$t('queryChart.selectField')" @update:model-value="options.metric = typeof $event === 'string' ? $event : ''"/>
                  <MfSelect :model-value="options.aggregation" :options="choices('aggregation', aggregations)" @update:model-value="changeAggregation($event as QueryChartOptions['aggregation'])"/>
                </div>
              </MfFormItem>
              <MfFormItem v-if="!specializedData" name="series" :label="$t('queryChart.series')">
                <MfSelect :model-value="options.series || 0" :options="[{ value: 0, label: $t('queryChart.ungrouped') }, ...fieldOptions]" searchable
                          @update:model-value="options.series = typeof $event === 'string' ? $event : ''"/>
              </MfFormItem>
            </section>
          </template>

          <template v-else-if="panel === 'fields'">
            <section class="space-y-3">
              <h4 class="font-semibold">{{ $t('queryChart.source') }}</h4>
              <div class="grid grid-cols-2 gap-3">
                <div v-for="stat in sourceStats" :key="stat.label" class="rounded-mf-md border border-line bg-primary/5 p-3">
                  <p class="text-xs text-content-tertiary">{{ stat.label }}</p><strong class="text-xl">{{ stat.value }}</strong>
                </div>
              </div>
              <h4 class="text-xs font-medium text-content-secondary">{{ $t('queryChart.readOnlySql') }}</h4>
              <pre class="max-h-36 overflow-auto whitespace-pre-wrap break-all rounded-mf-md border border-line bg-surface-hover p-3 text-xs">{{ source.query }}</pre>
            </section>
            <section class="space-y-2 border-t border-line pt-4">
              <h4 class="font-semibold">{{ $t('queryChart.saveMode') }}</h4>
              <MfSelect v-model="saveMode" :options="saveModes" :aria-label="$t('queryChart.saveMode')"/>
              <p class="text-xs text-content-tertiary">{{ $t('queryChart.dynamicHelp') }}</p>
              <MfFormItem v-if="saveMode === 'dynamic'" :label="$t('queryChart.refreshInterval')">
                <MfSelect v-model="options.refreshSeconds" :options="refreshIntervals"/>
              </MfFormItem>
            </section>
            <section class="space-y-3 border-t border-line pt-4">
              <h4 class="font-semibold">{{ $t('queryChart.fieldList') }}</h4>
              <div class="divide-y divide-line rounded-mf-md border border-line">
                <div v-for="field in source.headers" :key="field" class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 px-3 py-2">
                  <MfCheckbox :model-value="options.fields.includes(field)" @update:model-value="toggleField(field, !!$event)"><span class="break-all">{{ field }}</span></MfCheckbox>
                  <MfTag size="sm">{{ source.types?.[source.headers.indexOf(field)] || $t('queryChart.fieldKind.' + fieldKinds[field]) }}</MfTag>
                </div>
              </div>
            </section>
          </template>

          <TypedFilterBuilder v-else-if="panel === 'filters'" v-model:enabled="options.filtersEnabled"
                              v-model:groups="options.groups" v-model:join="options.filterJoin" :fields="filterFields">
            <template #toggle>
              <MfCheckbox v-model="options.filtersEnabled">{{ $t('queryChart.filtersEnabled') }}</MfCheckbox>
            </template>
            <template #notice><MfAlert variant="info" :title="$t('queryChart.filterNotice')"/></template>
            <template #expression>
              <section v-if="filterExpression" class="space-y-2 border-t border-line pt-4">
                <h4 class="font-semibold">{{ $t('queryChart.filterExpression') }}</h4>
                <pre class="whitespace-pre-wrap break-all rounded-mf-md bg-surface-hover p-3 text-xs">{{ filterExpression }}</pre>
              </section>
            </template>
          </TypedFilterBuilder>

          <QueryChartSettings v-else :panel="panel" :options="options" :time-category="timeCategory" @change="setOption"/>
        </MfForm>
      </main>

      <aside class="min-h-0 min-w-0 space-y-4 overflow-y-auto bg-surface-hover/40 p-5 max-md:overflow-visible">
        <div class="h-[clamp(340px,46dvh,590px)]">
          <QueryChartPreview :options="options" :rows="source.rows" :aggregation="result" heading linked @filter="emit('filter', $event)"/>
        </div>
        <section class="space-y-2 rounded-mf-lg border border-line bg-primary/5 p-4">
          <h3 class="font-semibold">{{ $t('queryChart.summary') }}</h3>
          <dl class="divide-y divide-line text-xs">
            <div v-for="item in summary" :key="item.label" class="flex items-start justify-between gap-3 py-2">
              <dt class="text-content-tertiary">{{ item.label }}</dt><dd class="break-all text-right font-semibold">{{ item.value }}</dd>
            </div>
          </dl>
          <pre class="max-h-28 overflow-auto whitespace-pre-wrap break-all rounded-mf-md border border-line bg-surface p-2 text-xs">{{ source.query }}</pre>
        </section>
      </aside>
    </div>

    <template #footer>
      <div class="flex w-full flex-col gap-2">
        <MfAlert v-if="error" variant="danger" :title="error"/>
        <div class="flex flex-wrap items-center justify-between gap-2">
          <MfButton :disabled="publishing" @click="reset"><RotateCcw :size="14"/>{{ $t('queryChart.reset') }}</MfButton>
          <div class="flex flex-wrap items-center gap-2">
            <MfButton :disabled="publishing" @click="copyConfig">{{ $t('queryChart.copyConfig') }}</MfButton>
            <MfButton :disabled="publishing" @click="close">{{ $t('common.cancel') }}</MfButton>
            <MfButton variant="primary" type="button" :loading="publishing" @click="submit"><Check :size="15"/>{{ $t('queryChart.create') }}</MfButton>
          </div>
        </div>
      </div>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Axis3D, ChartColumn, Check, Ellipsis, Filter, Info, List, MousePointer2, Palette, RotateCcw, Settings2, Table2 } from '@lucide/vue'
import { queryChartTypes, type QueryChartOptions, type QueryChartRow, type QueryChartType } from '@/model/query-chart'
import { chartDataSpec, suggestChartFields, type ChartMappings } from '@/model/query-chart-data'
import { useQueryChartBuilder, type QueryChartSource } from '@/composables/useQueryChartBuilder'
import TypedFilterBuilder from '@/views/components/filter/TypedFilterBuilder.vue'
import QueryChartPreview from '@/views/components/visual/query/QueryChartPreview.vue'
import QueryChartTypeIcon from '@/views/components/visual/query/QueryChartTypeIcon.vue'
import QueryChartDataFields from '@/views/components/visual/query/QueryChartDataFields.vue'
import QueryChartSettings from '@/views/components/visual/query/QueryChartSettings.vue'

const props = defineProps<{ isVisible: boolean; source: QueryChartSource }>()
const emit = defineEmits<{ close: [value: boolean]; filter: [rows: QueryChartRow[]] }>()
const { t } = useI18n()
const source = props.source
const close = () => emit('close', false)
const { options, fieldKinds, publishing, error, result, fieldOptions, numericOptions, timeCategory, ruleCount, saveMode,
  filterExpression, metricLabel, changeCategory, changeAggregation, toggleField, reset, copyConfig, publish } = useQueryChartBuilder(source, close)
const panels = [
  { key: 'chart', icon: ChartColumn }, { key: 'fields', icon: Table2 }, { key: 'filters', icon: Filter },
  { key: 'axes', icon: Axis3D }, { key: 'legend', icon: List }, { key: 'style', icon: Palette },
  { key: 'interaction', icon: MousePointer2 }, { key: 'advanced', icon: Settings2 }
] as const
const panel = ref<typeof panels[number]['key']>('chart')
const editor = ref<HTMLElement>()
const form = ref<{ validate: () => Promise<unknown> }>()
const chartCards = queryChartTypes.slice(0, 7)
const moreTypes = queryChartTypes.slice(chartCards.length)
const specializedData = computed(() => chartDataSpec(options.value.type))
function selectChartType(type: QueryChartType) {
  if (type === options.value.type) return
  const suggested = suggestChartFields(type, numericOptions.value.map(field => field.value))
  options.value.type = type
  if (suggested.chartFields) options.value.chartFields = { ...suggested.chartFields, ...options.value.chartFields }
  options.value.parallelFields ??= suggested.parallelFields
}
function changeMappings(change: Partial<ChartMappings>) {
  if (change.category !== undefined) changeCategory(change.category)
  Object.assign(options.value, change)
}
const aggregations = ['count', 'distinct', 'sum', 'avg', 'min', 'max']
const filterFields = computed(() => fieldOptions.value.map(field => ({ key: field.value, label: field.label, kind: fieldKinds[field.value] })))
const choices = (key: string, values: readonly string[]) => values.map(value => ({ value, label: t('queryChart.choice.' + key + '.' + value) }))
const metricOptions = computed(() => options.value.aggregation === 'count'
    ? [{ value: 0, label: t('queryChart.records') }, ...fieldOptions.value]
    : options.value.aggregation === 'distinct' ? fieldOptions.value : numericOptions.value)
const saveModes = computed(() => ['snapshot', 'dynamic'].map(value => ({ value, label: t('queryChart.' + value) })))
const refreshIntervals = computed(() => [
  { value: 0, label: t('queryChart.manualRefresh') },
  ...[60, 300, 900].map(value => ({ value, label: t('queryChart.everyMinutes', { count: value / 60 }) }))
])
const sourceLabel = computed(() => (source.label ? source.label + ' · ' : '') + t('queryChart.result'))
const sizeText = computed(() => t('queryChart.size', { rows: source.rows.length, fields: source.headers.length }))
const sourceStats = computed(() => [{ label: t('queryChart.rows'), value: source.rows.length }, { label: t('queryChart.fieldList'), value: source.headers.length }])
const summary = computed(() => [
  { label: t('queryChart.source'), value: sourceLabel.value },
  { label: t('queryChart.resultSize'), value: sizeText.value },
  { label: t('queryChart.chartType'), value: t('queryChart.type.' + options.value.type) },
  { label: t('queryChart.metric'), value: t('queryChart.choice.aggregation.' + options.value.aggregation) + ' · ' + metricLabel.value },
  { label: t('queryChart.saveMode'), value: t('queryChart.' + saveMode.value) },
  { label: t('queryChart.filteredRows'), value: t('queryChart.rowCount', { count: result.value.rowCount }) }
])

function setPanel(key: typeof panel.value) {
  panel.value = key
  if (editor.value) editor.value.scrollTop = 0
}
function setOption<K extends keyof QueryChartOptions>(key: K, value: unknown) {
  options.value[key] = value as QueryChartOptions[K]
}
async function submit() {
  if (!options.value.name.trim()) {
    setPanel('chart')
    await nextTick()
    try { await form.value?.validate() }
    catch { /* The name field displays its inline validation. */ }
    return
  }
  await publish()
}
</script>
