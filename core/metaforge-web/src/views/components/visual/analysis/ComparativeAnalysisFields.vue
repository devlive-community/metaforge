<template>
  <InspectorSection icon="data" :title="$t(key + 'settings')">
    <template v-if="correlation">
      <MfFormItem :label="$t(key + 'correlationFields')" required-mark>
        <MfSelect :model-value="settings.fields" :options="numbers" multiple searchable clearable
                  :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ fields: Array.isArray(value) ? value.map(String) : [] })"/>
      </MfFormItem>
      <MfFormItem :label="$t(key + 'minSamples')">
        <MfInputNumber :model-value="settings.minSamples" :min="2" :max="100" :precision="0" :aria-label="$t(key + 'minSamples')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ minSamples: value })"/>
      </MfFormItem>
    </template>
    <template v-else>
      <MfFormItem :label="$t(key + (quadrants ? 'xField' : distribution ? 'metricField' : 'currentField'))" required-mark>
        <MfSelect :model-value="widget.field || undefined" :options="numbers.filter(field => field.value !== settings.secondField)" searchable
                  :placeholder="$t('queryChart.selectField')" @update:model-value="value => emit('change', { field: String(value ?? '') })"/>
      </MfFormItem>
      <MfFormItem v-if="!distribution" :label="$t(key + (quadrants ? 'yField' : 'baselineField'))" required-mark>
        <MfSelect :model-value="settings.secondField || undefined" :options="numbers.filter(field => field.value !== widget.field)" searchable
                  :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ secondField: String(value ?? '') })"/>
      </MfFormItem>
      <MfFormItem v-if="distribution || grouped" :label="$t(key + 'groupField')" required-mark>
        <MfSelect :model-value="settings.groupField || undefined" :options="fields" searchable :placeholder="$t('queryChart.selectField')"
                  @update:model-value="value => update({ groupField: String(value ?? ''), leftGroup: '', rightGroup: '' })"/>
      </MfFormItem>
      <div v-if="distribution && settings.groupField" class="grid grid-cols-2 gap-2">
        <MfFormItem :label="$t(key + 'firstGroup')" required-mark>
          <MfSelect :model-value="settings.leftGroup || undefined" :options="groups.filter(group => group.value !== settings.rightGroup)" searchable
                    @update:model-value="value => update({ leftGroup: String(value ?? '') })"/>
        </MfFormItem>
        <MfFormItem :label="$t(key + 'secondGroup')" required-mark>
          <MfSelect :model-value="settings.rightGroup || undefined" :options="groups.filter(group => group.value !== settings.leftGroup)" searchable
                    @update:model-value="value => update({ rightGroup: String(value ?? '') })"/>
        </MfFormItem>
      </div>
      <template v-if="quadrants">
        <div class="grid grid-cols-2 gap-2">
          <MfFormItem v-for="axis in ['xThreshold', 'yThreshold'] as const" :key="axis" :label="$t(key + axis)">
            <MfInputNumber :model-value="settings[axis] ?? undefined" :aria-label="$t(key + axis)" :placeholder="$t(key + 'median')"
                           decimal-only commit-on-change @update:model-value="value => update({ [axis]: value ?? null })"/>
          </MfFormItem>
        </div>
        <p class="text-xs text-content-tertiary">{{ $t(key + 'thresholdHint') }}</p>
      </template>
      <MfFormItem v-if="distribution || kind === 'pairedDifference'" :label="$t('dashboard.studio.business.boundaries')" required-mark>
        <MfInput :model-value="settings.boundaries" :maxlength="400" :invalid="!validBoundaries" :aria-invalid="!validBoundaries"
                 placeholder="0, 25, 50, 100" @update:model-value="update({ boundaries: $event })"/>
        <p class="mt-1 text-xs" :class="validBoundaries ? 'text-content-tertiary' : 'text-danger'">{{ $t('dashboard.studio.business.boundaryHint') }}</p>
      </MfFormItem>
      <MfFormItem v-if="grouped" :label="$t('dashboard.studio.displayLimit')">
        <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" :aria-label="$t('dashboard.studio.displayLimit')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
      </MfFormItem>
    </template>
    <MfFormItem :label="$t('dashboard.studio.decimalPlaces')">
      <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" :aria-label="$t('dashboard.studio.decimalPlaces')"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
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
import { dashboardComparativeSettings, dashboardDataSettings, type DashboardComparativeOptions, type DashboardDataOptions, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardBandBoundaries } from '@/model/dashboard-business'
import { dashboardFilterChoices } from '@/model/dashboard-filters'
import type { QueryChartRow } from '@/model/query-chart'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

type Option = { value: string; label: string }
const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'comparativeOptions' | 'dataOptions' | 'field' | 'kind'>; fields: Option[]; numbers: Option[]; rows: QueryChartRow[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { locale } = useI18n()
const key = 'dashboard.studio.comparative.'
const settings = computed(() => dashboardComparativeSettings(props.widget.comparativeOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const kind = computed(() => props.widget.kind)
const correlation = computed(() => kind.value === 'metricCorrelation')
const quadrants = computed(() => kind.value === 'metricQuadrants')
const distribution = computed(() => kind.value === 'comparisonDistribution')
const grouped = computed(() => kind.value === 'rankMovement' || kind.value === 'shareMovement')
const validBoundaries = computed(() => !!dashboardBandBoundaries(settings.value.boundaries))
const groups = computed(() => dashboardFilterChoices(props.rows, settings.value.groupField, locale.value).filter(group => !!group.value.trim()))
function update(change: Partial<DashboardComparativeOptions>) {
  emit('change', { comparativeOptions: dashboardComparativeSettings({ ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) {
  emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) })
}
</script>
