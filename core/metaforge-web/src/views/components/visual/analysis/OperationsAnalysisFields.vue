<template>
  <InspectorSection icon="data" :title="$t('dashboard.studio.ops.settings')">
    <MfFormItem v-for="item in mappings" :key="item.key" :label="$t('dashboard.studio.ops.' + item.key)" :required-mark="item.required">
      <MfSelect :model-value="item.key === 'field' || item.key === 'timeField' ? widget[item.key] || undefined : settings[item.key] || undefined"
                :options="item.options" searchable :clearable="!item.required" :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => changeField(item.key, String(value ?? ''))"/>
    </MfFormItem>
    <MfFormItem v-if="widget.kind === 'recordLookup'" :label="$t('dashboard.studio.ops.lookupFields')">
      <MfSelect :model-value="widget.displayFields ?? []" :options="fields" multiple searchable clearable :placeholder="$t('dashboard.studio.autoFields')"
                @update:model-value="value => emit('change', { displayFields: Array.isArray(value) ? value.map(String).slice(0, 12) : undefined })"/>
    </MfFormItem>
    <template v-if="widget.kind === 'incidentFeed' || widget.kind === 'runHistory'">
      <MfFormItem :label="$t('dashboard.studio.ops.healthyValues')">
        <MfInput :model-value="settings.healthyValues" :maxlength="500" @update:model-value="update({ healthyValues: $event })"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.ops.failureValues')">
        <MfInput :model-value="settings.failureValues" :maxlength="500" @update:model-value="update({ failureValues: $event })"/>
      </MfFormItem>
      <p class="text-xs text-content-tertiary">{{ $t('dashboard.studio.ops.statusHint') }}</p>
    </template>
    <template v-if="widget.kind === 'outlierQueue' || widget.kind === 'slaScorecard'">
      <MfFormItem :label="$t(widget.kind === 'slaScorecard' ? 'dashboard.studio.ops.passCondition' : 'dashboard.studio.ops.exceptionCondition')">
        <MfSelect :model-value="settings.comparator" :options="comparators" @update:model-value="setComparator"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.ops.threshold')">
        <MfInputNumber :model-value="settings.threshold" :min="-1000000000000" :max="1000000000000" :precision="4" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && update({ threshold: value })"/>
      </MfFormItem>
      <MfFormItem v-if="widget.kind === 'slaScorecard'" :label="$t('dashboard.studio.ops.goal')">
        <MfInputNumber :model-value="settings.goal" :min="0" :max="100" :precision="2" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && update({ goal: value })"/>
      </MfFormItem>
    </template>
    <div v-if="numeric" class="grid grid-cols-2 gap-2">
      <MfFormItem :label="$t('dashboard.studio.decimalPlaces')">
        <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.numberSuffix')">
        <MfInput :model-value="data.suffix" :maxlength="30" @update:model-value="updateData({ suffix: $event })"/>
      </MfFormItem>
    </div>
    <MfFormItem v-if="widget.kind !== 'slaScorecard' && widget.kind !== 'latencyPercentiles'" :label="$t('dashboard.studio.displayLimit')">
      <MfInputNumber :model-value="data.limit" :min="1" :max="100" :precision="0" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
    </MfFormItem>
  </InspectorSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardDataSettings, dashboardOperationsSettings, dashboardThresholdComparators, type DashboardDataOptions, type DashboardOperationsOptions, type DashboardWidget } from '@/model/dashboard-layout'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

type Option = { value: string; label: string }
type Field = 'field' | 'timeField' | 'labelField' | 'statusField' | 'detailsField' | 'endField'
const props = defineProps<{ widget: Pick<DashboardWidget, 'dataOptions' | 'displayFields' | 'kind' | 'operationsOptions'>; fields: Option[]; dates: Option[]; numbers: Option[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { t } = useI18n()
const settings = computed(() => dashboardOperationsSettings(props.widget.kind, props.widget.operationsOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const numeric = computed(() => ['outlierQueue', 'slaScorecard', 'latencyPercentiles'].includes(props.widget.kind))
const comparators = computed(() => dashboardThresholdComparators.map(value => ({ value, label: t('dashboard.studio.ops.' + value) })))
const mappings = computed(() => {
  const kind = props.widget.kind
  const fields: { key: Field; options: Option[]; required?: boolean }[] = []
  if (numeric.value) fields.push({ key: 'field', options: props.numbers, required: true })
  if (['incidentFeed', 'runHistory', 'maintenanceWindow', 'outlierQueue'].includes(kind))
    fields.push({ key: 'timeField', options: props.dates, required: kind !== 'outlierQueue' })
  if (kind === 'maintenanceWindow') fields.push({ key: 'endField', options: props.dates, required: true })
  if (['incidentFeed', 'runHistory', 'maintenanceWindow', 'outlierQueue', 'recordLookup'].includes(kind))
    fields.push({ key: 'labelField', options: props.fields, required: kind === 'recordLookup' })
  if (kind === 'incidentFeed' || kind === 'runHistory') fields.push({ key: 'statusField', options: props.fields, required: kind === 'runHistory' })
  if (kind === 'incidentFeed' || kind === 'maintenanceWindow') fields.push({ key: 'detailsField', options: props.fields })
  return fields
})
function update(change: Partial<DashboardOperationsOptions>) {
  emit('change', { operationsOptions: dashboardOperationsSettings(props.widget.kind, { ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) { emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) }) }
function changeField(key: Field, value: string) {
  if (key === 'field' || key === 'timeField') emit('change', { [key]: value })
  else update({ [key]: value })
}
function setComparator(value: unknown) {
  const comparator = dashboardThresholdComparators.find(item => item === value)
  if (comparator) update({ comparator })
}
</script>
