<template>
  <InspectorSection icon="data" :title="$t('dashboard.studio.relation.settings')">
    <MfFormItem :label="$t('dashboard.studio.relation.' + rowLabel)" required-mark>
      <MfSelect :model-value="settings.rowField || undefined" :options="fields" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="setRowField"/>
    </MfFormItem>
    <MfFormItem v-if="cross" :label="$t('dashboard.studio.relation.columnField')" required-mark>
      <MfSelect :model-value="settings.columnField || undefined" :options="otherFields" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => update({ columnField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="transition || pairs" :label="$t('dashboard.studio.relation.entityField')" required-mark>
      <MfSelect :model-value="settings.entityField || undefined" :options="otherFields" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => update({ entityField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="transition" :label="$t('dashboard.studio.relation.timeField')" required-mark>
      <MfSelect :model-value="widget.timeField || undefined" :options="dates" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => emit('change', { timeField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="cross" :label="$t('dashboard.studio.dataAggregation')">
      <MfSelect :model-value="operation" :options="operations" @update:model-value="setOperation"/>
    </MfFormItem>
    <MfFormItem v-if="spread || cross && operation !== 'count'" :label="$t('dashboard.studio.relation.valueField')" required-mark>
      <MfSelect :model-value="widget.field || undefined" :options="numbers" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => emit('change', { field: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="!spread" :label="$t(pairs ? 'dashboard.studio.relation.itemLimit' : 'dashboard.studio.relation.axisLimit')">
      <MfInputNumber :model-value="settings.dimensionLimit" :min="2" :max="16" :precision="0" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && update({ dimensionLimit: value })"/>
    </MfFormItem>
    <MfFormItem v-if="pairs || spread" :label="$t('dashboard.studio.displayLimit')">
      <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
    </MfFormItem>
    <div v-if="spread || cross && operation !== 'count'" class="grid grid-cols-2 gap-2">
      <MfFormItem :label="$t('dashboard.studio.decimalPlaces')">
        <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.numberSuffix')">
        <MfInput :model-value="data.suffix" :maxlength="30" @update:model-value="value => updateData({ suffix: value })"/>
      </MfFormItem>
    </div>
    <label class="flex min-w-0 items-center justify-between gap-3 text-sm">
      {{ $t('dashboard.studio.relation.linkedFilter') }}
      <MfSwitch :model-value="widget.action === 'filter'" @update:model-value="value => emit('change', { action: value ? 'filter' : 'none' })"/>
    </label>
  </InspectorSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { dashboardDataSettings, dashboardRelationSettings, type DashboardDataOptions, type DashboardRelationOptions, type DashboardWidget } from '@/model/dashboard-layout'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

type Option = { value: string; label: string }
const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'dataOptions' | 'field' | 'kind' | 'operation' | 'relationOptions' | 'timeField'>; fields: Option[]; dates: Option[]; numbers: Option[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { t } = useI18n()
const settings = computed(() => dashboardRelationSettings(props.widget.relationOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const cross = computed(() => props.widget.kind === 'crossTabHeatmap')
const transition = computed(() => props.widget.kind === 'transitionMatrix')
const pairs = computed(() => props.widget.kind === 'cooccurrencePairs')
const spread = computed(() => props.widget.kind === 'groupSpread')
const rowLabel = computed(() => transition.value ? 'stateField' : pairs.value ? 'itemField' : spread.value ? 'groupField' : 'rowField')
const otherFields = computed(() => props.fields.filter(field => field.value !== settings.value.rowField))
const operation = computed(() => props.widget.operation === 'sum' || props.widget.operation === 'avg' ? props.widget.operation : 'count')
const operations = computed(() => ['count', 'sum', 'avg'].map(value => ({ value, label: t('queryChart.choice.aggregation.' + value) })))
function update(change: Partial<DashboardRelationOptions>) {
  emit('change', { relationOptions: dashboardRelationSettings({ ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) {
  emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) })
}
function setRowField(value: unknown) {
  const rowField = String(value ?? '')
  update({ rowField, columnField: rowField === settings.value.columnField ? '' : settings.value.columnField,
    entityField: rowField === settings.value.entityField ? '' : settings.value.entityField })
}
function setOperation(value: unknown) {
  emit('change', { operation: value === 'sum' || value === 'avg' ? value : 'count' })
}
</script>
