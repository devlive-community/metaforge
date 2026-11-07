<template>
  <MfEmpty v-if="!configured" class="h-full" size="sm"
           :title="$t(widget.kind === 'dateRangeFilter' ? 'dashboard.studio.chooseDateField' : 'queryChart.selectField')"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col justify-center gap-2">
    <MfSelect v-if="widget.kind === 'selectFilter'" :model-value="textValue || undefined" :options="choices" searchable clearable
              :disabled="disabled" :aria-label="widget.name" :placeholder="$t('dashboard.studio.filterAllValues')"
              @update:model-value="value => writeText('selectFilter', String(value ?? ''))"/>
    <MfSelect v-else-if="widget.kind === 'multiValueFilter'" :model-value="multiValues" :options="choices" multiple searchable clearable
              :disabled="disabled" :aria-label="widget.name" :placeholder="$t('dashboard.studio.filterAllValues')"
              @update:model-value="value => context?.write(widget, { kind: 'multiValueFilter', values: Array.isArray(value) ? value.map(String) : [] })"/>
    <MfSelect v-else-if="widget.kind === 'valueStateFilter'" :model-value="presence" :options="presenceOptions" clearable
              :disabled="disabled" :aria-label="widget.name" :placeholder="$t('dashboard.studio.filterAllValues')"
              @update:model-value="value => context?.write(widget, { kind: 'valueStateFilter', presence: value === 'present' || value === 'missing' ? value : '' })"/>
    <div v-else-if="widget.kind === 'numberRangeFilter'" class="flex flex-wrap gap-2">
      <MfInputNumber class="min-w-32 flex-1" :model-value="numberRange.min" :max="numberRange.max" :disabled="disabled"
                     :aria-label="$t('dashboard.studio.filterMinimum')" :placeholder="$t('dashboard.studio.filterMinimum')"
                     decimal-only @update:model-value="min => context?.write(widget, { kind: 'numberRangeFilter', min, max: numberRange.max })"/>
      <MfInputNumber class="min-w-32 flex-1" :model-value="numberRange.max" :min="numberRange.min" :disabled="disabled"
                     :aria-label="$t('dashboard.studio.filterMaximum')" :placeholder="$t('dashboard.studio.filterMaximum')"
                     decimal-only @update:model-value="max => context?.write(widget, { kind: 'numberRangeFilter', min: numberRange.min, max })"/>
    </div>
    <MfInput v-else-if="widget.kind === 'searchFilter'" :model-value="textValue" :maxlength="256" clearable :disabled="disabled"
             :aria-label="widget.name" :placeholder="$t('dashboard.studio.filterSearchPlaceholder')" @update:model-value="value => writeText('searchFilter', value)">
      <template #prefix><Search :size="15"/></template>
    </MfInput>
    <div v-else-if="widget.kind === 'dateRangeFilter'" class="flex flex-wrap gap-2">
      <MfDatePicker class="min-w-32 flex-1" :model-value="range.start" :max="range.end || undefined" :locale="locale"
                    :label="$t('dashboard.studio.filterStart')" :placeholder="$t('dashboard.studio.filterStart')"
                    :today-text="$t('dashboard.studio.filterToday')" :clear-text="$t('common.clear')" :disabled="disabled"
                    @update:model-value="value => writeDate(value, range.end)"/>
      <MfDatePicker class="min-w-32 flex-1" :model-value="range.end" :min="range.start || undefined" :locale="locale"
                    :label="$t('dashboard.studio.filterEnd')" :placeholder="$t('dashboard.studio.filterEnd')"
                    :today-text="$t('dashboard.studio.filterToday')" :clear-text="$t('common.clear')" :disabled="disabled"
                    @update:model-value="value => writeDate(range.start, value)"/>
    </div>
    <MfButton v-else class="self-start" :disabled="disabled || !context?.active(widget.reportCode!)" @click="context?.clear(widget.reportCode!)">
      <ListRestart :size="16"/>{{ $t('dashboard.studio.tool.clearFilters') }}
    </MfButton>
  </div>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { ListRestart, Search } from '@lucide/vue'
import { dashboardFiltersContext } from '@/composables/dashboardFiltersContext'
import { dashboardDateRangeValid, dashboardFilterChoices } from '@/model/dashboard-filters'
import type { DashboardWidget } from '@/model/dashboard-layout'
import { aggregateQueryChart, type QueryChartOptions, type QueryChartRow } from '@/model/query-chart'

const props = defineProps<{ widget: DashboardWidget; rows: QueryChartRow[]; options: QueryChartOptions; editable?: boolean }>()
const { t, locale } = useI18n()
const context = inject(dashboardFiltersContext, undefined)
const configured = computed(() => props.widget.kind === 'clearFilters' || props.options.fields.includes(props.widget.field))
const disabled = computed(() => props.editable || !context)
const state = computed(() => context?.read(props.widget))
const textValue = computed(() => state.value && 'value' in state.value ? state.value.value : '')
const range = computed(() => state.value?.kind === 'dateRangeFilter' ? state.value : { start: '', end: '' })
const numberRange = computed(() => state.value?.kind === 'numberRangeFilter' ? state.value : { min: undefined, max: undefined })
const multiValues = computed(() => state.value?.kind === 'multiValueFilter' ? state.value.values : [])
const presence = computed(() => state.value?.kind === 'valueStateFilter' ? state.value.presence || undefined : undefined)
const presenceOptions = computed(() => ['present', 'missing'].map(value => ({ value, label: t('dashboard.studio.presence.' + value) })))
const choices = computed(() => props.widget.kind === 'selectFilter' || props.widget.kind === 'multiValueFilter'
  ? dashboardFilterChoices(aggregateQueryChart(props.rows, { ...props.options, type: 'table' }).filtered, props.widget.field, locale.value)
  : [])
function writeText(kind: 'selectFilter' | 'searchFilter', value: string) {
  context?.write(props.widget, { kind, value })
}
function writeDate(start: string, end: string) {
  if (dashboardDateRangeValid(start, end)) context?.write(props.widget, { kind: 'dateRangeFilter', start, end })
}
</script>
