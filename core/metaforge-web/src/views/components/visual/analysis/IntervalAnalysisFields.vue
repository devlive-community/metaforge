<template>
  <InspectorSection icon="data" :title="$t(key + 'settings')">
    <MfFormItem :label="$t(key + 'startField')" required-mark>
      <MfSelect :model-value="widget.timeField || undefined" :options="timeFields.filter(field => field.value !== settings.endField)" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => emit('change', { timeField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem :label="$t(key + 'endField')" required-mark>
      <MfSelect :model-value="settings.endField || undefined" :options="timeFields.filter(field => field.value !== widget.timeField)" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ endField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="kind === 'intervalOverlaps' || kind === 'intervalGroupCoverage'" :label="$t(key + 'groupField')" :required-mark="kind === 'intervalGroupCoverage'">
      <MfSelect :model-value="settings.groupField || undefined" :options="fields" searchable :clearable="kind === 'intervalOverlaps'"
                @update:model-value="value => update({ groupField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="kind === 'intervalOverlaps'" :label="$t(key + 'minConcurrent')">
      <MfInputNumber :model-value="settings.minConcurrent" :min="2" :max="20" :precision="0" :aria-label="$t(key + 'minConcurrent')"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ minConcurrent: value })"/>
    </MfFormItem>
    <template v-if="kind !== 'intervalConcurrency'">
      <MfFormItem :label="$t('dashboard.studio.temporal.unit')">
        <MfSelect :model-value="settings.unit" :options="units" @update:model-value="setUnit"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.decimalPlaces')">
        <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" :aria-label="$t('dashboard.studio.decimalPlaces')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
      </MfFormItem>
    </template>
    <MfFormItem v-if="kind === 'intervalDurationBands'" :label="$t(key + 'boundaries')" required-mark>
      <MfInput :model-value="settings.durationBands" :maxlength="400" :invalid="!validBands" :aria-invalid="!validBands"
               placeholder="0.5, 1, 2, 4, 8" @update:model-value="update({ durationBands: $event })"/>
      <p class="mt-1 text-xs" :class="validBands ? 'text-content-tertiary' : 'text-danger'">{{ $t(key + 'bandHint') }}</p>
    </MfFormItem>
    <MfFormItem v-if="['intervalOverlaps', 'intervalGroupCoverage', 'intervalGaps'].includes(kind)" :label="$t('dashboard.studio.displayLimit')">
      <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" :aria-label="$t('dashboard.studio.displayLimit')"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
    </MfFormItem>
    <label class="flex min-w-0 items-center justify-between gap-3 text-sm">
      {{ $t('dashboard.studio.relation.linkedFilter') }}
      <MfSwitch :model-value="widget.action === 'filter'" @update:model-value="value => emit('change', { action: value ? 'filter' : 'none' })"/>
    </label>
  </InspectorSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardDataSettings, dashboardIntervalSettings, type DashboardDataOptions, type DashboardIntervalOptions, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardIntervalBandBoundaries, dashboardIntervalUnitMs } from '@/model/dashboard-intervals'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

type Option = { value: string; label: string }
const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'dataOptions' | 'intervalOptions' | 'kind' | 'timeField'>; fields: Option[]; dates: Option[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { t } = useI18n()
const key = 'dashboard.studio.interval.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardIntervalSettings(props.widget.intervalOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const validBands = computed(() => !!dashboardIntervalBandBoundaries(settings.value.durationBands, dashboardIntervalUnitMs[settings.value.unit]))
const units = computed(() => ['minutes', 'hours', 'days'].map(value => ({ value, label: t('dashboard.studio.temporal.unitLabel.' + value) })))
const timeFields = computed(() => {
  const dates = new Set(props.dates.map(field => field.value))
  return [...props.dates, ...props.fields.filter(field => !dates.has(field.value))]
})
function update(change: Partial<DashboardIntervalOptions>) {
  emit('change', { intervalOptions: dashboardIntervalSettings({ ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) {
  emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) })
}
function setUnit(value: unknown) {
  if (value === 'minutes' || value === 'hours' || value === 'days') update({ unit: value })
}
</script>
