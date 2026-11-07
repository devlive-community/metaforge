<template>
  <DashboardInspectorGroup icon="data" :title="$t('dashboard.studio.business.settings')">
    <MfFormItem v-for="mapping in mappings" :key="mapping.key" :label="$t('dashboard.studio.business.' + mapping.label)" :required-mark="mapping.required">
      <MfSelect :model-value="mapping.key === 'field' || mapping.key === 'timeField' ? widget[mapping.key] || undefined : settings[mapping.key] || undefined"
                :options="mapping.options" searchable :clearable="!mapping.required" :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => setField(mapping.key, String(value ?? ''))"/>
    </MfFormItem>
    <template v-if="widget.kind === 'stageConversion' && settings.groupField">
      <MfFormItem :label="$t('dashboard.studio.business.stageOrder')" required-mark>
        <MfSelect :model-value="settings.stageOrder" :options="stages" multiple tags searchable clearable
                  :placeholder="$t('dashboard.studio.business.chooseStages')"
                  @update:model-value="value => update({ stageOrder: Array.isArray(value) ? value.map(String) : [] })"/>
      </MfFormItem>
      <ol v-if="settings.stageOrder.length > 1" class="grid gap-1">
        <li v-for="(stage, index) in settings.stageOrder" :key="stage" class="flex min-w-0 items-center gap-1.5 rounded-mf-sm border border-line px-2 py-1">
          <span class="min-w-0 flex-1 truncate text-xs" :title="stage">{{ index + 1 }}. {{ stage }}</span>
          <MfButton size="sm" variant="ghost" :disabled="index === 0" :aria-label="$t('dashboard.studio.business.moveStageUp', { stage })" @click="moveStage(index, -1)"><ChevronUp :size="14"/></MfButton>
          <MfButton size="sm" variant="ghost" :disabled="index === settings.stageOrder.length - 1" :aria-label="$t('dashboard.studio.business.moveStageDown', { stage })" @click="moveStage(index, 1)"><ChevronDown :size="14"/></MfButton>
        </li>
      </ol>
      <p class="text-xs text-content-tertiary">{{ $t(settings.entityField ? 'dashboard.studio.business.entityFunnelHint' : 'dashboard.studio.business.volumeFunnelHint') }}</p>
    </template>
    <MfFormItem v-if="widget.kind === 'targetGap'" :label="$t('dashboard.studio.business.targetAggregation')">
      <MfSelect :model-value="settings.targetAggregation" :options="['sum', 'max'].map(value => ({ value, label: $t('dashboard.studio.business.targetMode.' + value) }))"
                @update:model-value="value => update({ targetAggregation: value === 'max' ? 'max' : 'sum' })"/>
    </MfFormItem>
    <MfFormItem v-if="widget.kind === 'segmentSummary'" :label="$t('dashboard.studio.dataAggregation')">
      <MfSelect :model-value="operation" :options="['count', 'sum', 'avg'].map(value => ({ value, label: $t('queryChart.choice.aggregation.' + value) }))"
                @update:model-value="value => emit('change', { operation: value === 'sum' || value === 'avg' ? value : 'count' })"/>
    </MfFormItem>
    <MfFormItem v-if="widget.kind === 'paretoTable'" :label="$t('dashboard.studio.business.paretoCutoff')">
      <MfInputNumber :model-value="settings.paretoCutoff" :min="1" :max="100" :precision="0" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && update({ paretoCutoff: value })"/>
    </MfFormItem>
    <div v-if="widget.kind === 'cohortRetention'" class="grid grid-cols-2 gap-2">
      <MfFormItem :label="$t('dashboard.studio.business.period')">
        <MfSelect :model-value="settings.period" :options="['day', 'week'].map(value => ({ value, label: $t('dashboard.studio.business.periodMode.' + value) }))"
                  @update:model-value="value => update({ period: value === 'day' ? 'day' : 'week' })"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.business.periods')">
        <MfInputNumber :model-value="settings.periods" :min="2" :max="12" :precision="0" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && update({ periods: value })"/>
      </MfFormItem>
    </div>
    <MfFormItem v-if="widget.kind === 'valueBands'" :label="$t('dashboard.studio.business.boundaries')" required-mark>
      <MfInput :model-value="settings.boundaries" :maxlength="400" :invalid="!boundaries" :aria-invalid="!boundaries"
               placeholder="0, 25, 50, 100" @update:model-value="update({ boundaries: $event })"/>
      <p class="mt-1 text-xs" :class="boundaries ? 'text-content-tertiary' : 'text-danger'">{{ $t('dashboard.studio.business.boundaryHint') }}</p>
    </MfFormItem>
    <div v-if="usesNumbers" class="grid grid-cols-2 gap-2">
      <MfFormItem :label="$t('dashboard.studio.decimalPlaces')">
        <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.numberSuffix')">
        <MfInput :model-value="data.suffix" :maxlength="30" @update:model-value="updateData({ suffix: $event })"/>
      </MfFormItem>
    </div>
    <MfFormItem v-if="['targetGap', 'segmentSummary', 'paretoTable', 'varianceDrivers', 'cohortRetention'].includes(widget.kind)" :label="$t('dashboard.studio.displayLimit')">
      <MfInputNumber :model-value="Math.min(data.limit, 30)" :min="1" :max="30" :precision="0" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
    </MfFormItem>
  </DashboardInspectorGroup>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ChevronDown, ChevronUp } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { dashboardBusinessSettings, dashboardDataSettings, type DashboardBusinessOptions, type DashboardDataOptions, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardBandBoundaries } from '@/model/dashboard-business'
import { dashboardFilterChoices } from '@/model/dashboard-filters'
import type { QueryChartRow } from '@/model/query-chart'
import DashboardInspectorGroup from './DashboardInspectorGroup.vue'

type Option = { value: string; label: string }
type Field = 'groupField' | 'subgroupField' | 'referenceField' | 'entityField' | 'field' | 'timeField'
const props = defineProps<{ widget: DashboardWidget; fields: Option[]; dates: Option[]; numbers: Option[]; rows: QueryChartRow[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { locale } = useI18n()
const settings = computed(() => dashboardBusinessSettings(props.widget.businessOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const operation = computed(() => ['sum', 'avg'].includes(props.widget.operation) ? props.widget.operation : 'count')
const boundaries = computed(() => dashboardBandBoundaries(settings.value.boundaries))
const usesNumbers = computed(() => !['recencyDistribution', 'cohortRetention'].includes(props.widget.kind))
const mappings = computed(() => {
  const kind = props.widget.kind
  const list: { key: Field; label: string; options: Option[]; required?: boolean }[] = []
  if (['stageConversion', 'targetGap', 'segmentSummary', 'paretoTable', 'varianceDrivers'].includes(kind))
    list.push({ key: 'groupField', label: kind === 'stageConversion' ? 'stageField' : 'groupField', options: props.fields, required: true })
  if (kind === 'segmentSummary') list.push({ key: 'subgroupField', label: 'subgroupField', options: props.fields.filter(field => field.value !== settings.value.groupField) })
  if (['stageConversion', 'cohortRetention', 'recencyDistribution'].includes(kind))
    list.push({ key: 'entityField', label: 'entityField', options: props.fields, required: kind !== 'stageConversion' })
  if (['cohortRetention', 'recencyDistribution'].includes(kind))
    list.push({ key: 'timeField', label: 'timeField', options: props.dates, required: true })
  if (kind === 'targetGap' || kind === 'varianceDrivers')
    list.push({ key: 'referenceField', label: kind === 'targetGap' ? 'targetField' : 'baselineField', options: props.numbers, required: true })
  if (['targetGap', 'varianceDrivers', 'valueBands', 'paretoTable'].includes(kind) || kind === 'stageConversion' && !settings.value.entityField || kind === 'segmentSummary' && operation.value !== 'count')
    list.push({ key: 'field', label: ['targetGap', 'varianceDrivers'].includes(kind) ? 'actualField' : 'metricField', options: props.numbers,
      required: !['stageConversion', 'paretoTable'].includes(kind) })
  return list
})
const stages = computed(() => {
  const options = dashboardFilterChoices(props.rows, settings.value.groupField, locale.value).slice(0, 150)
  return [...new Map([...options, ...settings.value.stageOrder.map(value => ({ value, label: value }))].map(option => [option.value, option])).values()]
})
function update(change: Partial<DashboardBusinessOptions>) {
  emit('change', { businessOptions: dashboardBusinessSettings({ ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) { emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) }) }
function setField(key: Field, value: string) {
  if (key === 'field' || key === 'timeField') emit('change', { [key]: value })
  else if (key === 'groupField') update({ groupField: value, stageOrder: [], subgroupField: value === settings.value.subgroupField ? '' : settings.value.subgroupField })
  else update({ [key]: value })
}
function moveStage(index: number, offset: number) {
  const stageOrder = [...settings.value.stageOrder], to = index + offset
  if (to < 0 || to >= stageOrder.length) return
  const [stage] = stageOrder.splice(index, 1)
  stageOrder.splice(to, 0, stage)
  update({ stageOrder })
}
</script>
