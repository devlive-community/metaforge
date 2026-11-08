<template>
  <DashboardInspectorGroup icon="data" :title="$t(key + 'settings')">
    <MfFormItem :label="$t(timeline ? key + 'startField' : key + 'timeField')" required-mark>
      <MfSelect :model-value="widget.timeField || undefined" :options="timeFields.filter(field => !timeline || field.value !== settings.endField)" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => emit('change', { timeField: String(value ?? '') })"/>
    </MfFormItem>
    <template v-if="timeline">
      <MfFormItem :label="$t(key + 'endField')" required-mark>
        <MfSelect :model-value="settings.endField || undefined" :options="timeFields.filter(field => field.value !== widget.timeField)" searchable :placeholder="$t('queryChart.selectField')"
                  @update:model-value="value => update({ endField: String(value ?? '') })"/>
      </MfFormItem>
      <MfFormItem :label="$t(key + 'labelField')">
        <MfSelect :model-value="settings.labelField || undefined" :options="fields" searchable clearable
                  @update:model-value="value => update({ labelField: String(value ?? '') })"/>
      </MfFormItem>
    </template>
    <MfFormItem v-if="intervals" :label="$t(key + 'entityField')">
      <MfSelect :model-value="settings.entityField || undefined" :options="fields" searchable clearable
                @update:model-value="value => update({ entityField: String(value ?? '') })"/>
      <p class="mt-1 text-xs text-content-tertiary">{{ $t(key + 'entityHint') }}</p>
    </MfFormItem>
    <template v-if="chart">
      <MfFormItem :label="$t('dashboard.studio.dataAggregation')">
        <MfSelect :model-value="operation" :options="operations" @update:model-value="value => emit('change', { operation: value === 'sum' ? 'sum' : 'count' })"/>
      </MfFormItem>
      <MfFormItem v-if="operation === 'sum'" :label="$t(key + 'valueField')" required-mark>
        <MfSelect :model-value="widget.field || undefined" :options="numbers" searchable :placeholder="$t('queryChart.selectField')"
                  @update:model-value="value => emit('change', { field: String(value ?? '') })"/>
      </MfFormItem>
    </template>
    <MfFormItem v-if="kind === 'cumulativeTrend' || kind === 'rollingAverageTrend'" :label="$t(key + 'historyDays')">
      <MfInputNumber :model-value="settings.historyDays" :min="7" :max="366" :precision="0" :aria-label="$t(key + 'historyDays')" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && update({ historyDays: value })"/>
    </MfFormItem>
    <MfFormItem v-if="kind === 'rollingAverageTrend'" :label="$t(key + 'windowDays')">
      <MfInputNumber :model-value="settings.windowDays" :min="2" :max="90" :precision="0" :aria-label="$t(key + 'windowDays')" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && update({ windowDays: value })"/>
    </MfFormItem>
    <MfFormItem v-if="kind === 'yearOverYearTrend'" :label="$t(key + 'year')">
      <MfInputNumber :model-value="settings.year || undefined" :min="1" :max="9999" :precision="0" :placeholder="$t(key + 'latestYear')" :aria-label="$t(key + 'year')"
                     decimal-only commit-on-change @update:model-value="value => update({ year: value ?? 0 })"/>
    </MfFormItem>
    <template v-if="!chart">
      <MfFormItem :label="$t(key + 'unit')">
        <MfSelect :model-value="settings.unit" :options="units" @update:model-value="setUnit"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.displayLimit')">
        <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" :aria-label="$t('dashboard.studio.displayLimit')" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
      </MfFormItem>
    </template>
    <MfFormItem v-if="!chart || operation === 'sum' || kind === 'rollingAverageTrend'" :label="$t('dashboard.studio.decimalPlaces')">
      <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" :aria-label="$t('dashboard.studio.decimalPlaces')" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
    </MfFormItem>
    <label class="flex min-w-0 items-center justify-between gap-3 text-sm">
      {{ $t('dashboard.studio.relation.linkedFilter') }}
      <MfSwitch :model-value="widget.action === 'filter'" @update:model-value="value => emit('change', { action: value ? 'filter' : 'none' })"/>
    </label>
  </DashboardInspectorGroup>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardDataSettings, dashboardTemporalSettings, type DashboardDataOptions, type DashboardTemporalOptions, type DashboardWidget } from '@/model/dashboard-layout'
import DashboardInspectorGroup from './DashboardInspectorGroup.vue'

type Option = { value: string; label: string }
const props = defineProps<{ widget: DashboardWidget; fields: Option[]; dates: Option[]; numbers: Option[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { t } = useI18n()
const key = 'dashboard.studio.temporal.'
const settings = computed(() => dashboardTemporalSettings(props.widget.temporalOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const kind = computed(() => props.widget.kind)
const timeline = computed(() => kind.value === 'intervalTimeline')
const intervals = computed(() => kind.value === 'eventIntervals')
const chart = computed(() => !timeline.value && !intervals.value)
const operation = computed(() => props.widget.operation === 'sum' ? 'sum' : 'count')
const operations = computed(() => ['count', 'sum'].map(value => ({ value, label: t('queryChart.choice.aggregation.' + value) })))
const units = computed(() => ['minutes', 'hours', 'days'].map(value => ({ value, label: t(key + 'unitLabel.' + value) })))
const timeFields = computed(() => {
  const dates = new Set(props.dates.map(field => field.value))
  return [...props.dates, ...props.fields.filter(field => !dates.has(field.value))]
})
function update(change: Partial<DashboardTemporalOptions>) {
  emit('change', { temporalOptions: dashboardTemporalSettings({ ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) {
  emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) })
}
function setUnit(value: unknown) {
  if (value === 'minutes' || value === 'hours' || value === 'days') update({ unit: value })
}
</script>
