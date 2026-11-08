<template>
  <DashboardInspectorGroup icon="data" :title="$t(key + 'settings')">
    <MfFormItem :label="$t(key + 'entityField')" required-mark>
      <MfSelect :model-value="settings.entityField || undefined" :options="fields.filter(field => field.value !== settings.stageField && field.value !== widget.timeField)" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ entityField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem :label="$t(key + 'stageField')" required-mark>
      <MfSelect :model-value="settings.stageField || undefined" :options="fields.filter(field => field.value !== settings.entityField && field.value !== widget.timeField)" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ stageField: String(value ?? ''), requiredStages: [] })"/>
    </MfFormItem>
    <MfFormItem :label="$t(key + 'timeField')" required-mark>
      <MfSelect :model-value="widget.timeField || undefined" :options="timeFields" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => emit('change', { timeField: String(value ?? '') })"/>
    </MfFormItem>
    <template v-if="kind === 'processCompliance' && settings.stageField">
      <MfFormItem :label="$t(key + 'requiredStages')" required-mark>
        <MfSelect :model-value="settings.requiredStages" :options="stageOptions" multiple searchable clearable :placeholder="$t(key + 'chooseRequired')"
                  @update:model-value="value => update({ requiredStages: Array.isArray(value) ? value.map(String) : [] })"/>
      </MfFormItem>
      <ol v-if="settings.requiredStages.length > 1" class="grid gap-1">
        <li v-for="(stage, index) in settings.requiredStages" :key="stage" class="flex min-w-0 items-center gap-1.5 rounded-mf-sm border border-line px-2 py-1">
          <span class="min-w-0 flex-1 truncate text-xs" :title="stage">{{ index + 1 }}. {{ stage }}</span>
          <MfButton size="sm" variant="ghost" :disabled="index === 0" :aria-label="$t('dashboard.studio.business.moveStageUp', { stage })"
                    @click="moveStage(index, -1)"><ChevronUp :size="14"/></MfButton>
          <MfButton size="sm" variant="ghost" :disabled="index === settings.requiredStages.length - 1" :aria-label="$t('dashboard.studio.business.moveStageDown', { stage })"
                    @click="moveStage(index, 1)"><ChevronDown :size="14"/></MfButton>
        </li>
      </ol>
      <p class="text-xs text-content-tertiary">{{ $t(key + 'orderHint') }}</p>
    </template>
    <template v-if="['processCycle', 'processWaits'].includes(kind)">
      <MfFormItem :label="$t('dashboard.studio.temporal.unit')">
        <MfSelect :model-value="settings.unit" :options="units" @update:model-value="setUnit"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.decimalPlaces')">
        <MfInputNumber :model-value="data.decimals" :min="0" :max="8" :precision="0" :aria-label="$t('dashboard.studio.decimalPlaces')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ decimals: value })"/>
      </MfFormItem>
    </template>
    <MfFormItem v-if="kind !== 'processCompliance'" :label="$t('dashboard.studio.displayLimit')">
      <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" :aria-label="$t('dashboard.studio.displayLimit')"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && updateData({ limit: value })"/>
    </MfFormItem>
    <label class="flex min-w-0 items-center justify-between gap-3 text-sm">
      {{ $t('dashboard.studio.relation.linkedFilter') }}
      <MfSwitch :model-value="widget.action === 'filter'" @update:model-value="value => emit('change', { action: value ? 'filter' : 'none' })"/>
    </label>
  </DashboardInspectorGroup>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ChevronDown, ChevronUp } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import { dashboardDataSettings, dashboardProcessSettings, type DashboardDataOptions, type DashboardProcessOptions, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardFilterChoices } from '@/model/dashboard-filters'
import type { QueryChartRow } from '@/model/query-chart'
import DashboardInspectorGroup from './DashboardInspectorGroup.vue'

type Option = { value: string; label: string }
const props = defineProps<{ widget: DashboardWidget; fields: Option[]; dates: Option[]; rows: QueryChartRow[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { t, locale } = useI18n()
const key = 'dashboard.studio.process.'
const settings = computed(() => dashboardProcessSettings(props.widget.processOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const kind = computed(() => props.widget.kind)
const units = computed(() => ['minutes', 'hours', 'days'].map(value => ({ value, label: t('dashboard.studio.temporal.unitLabel.' + value) })))
const timeFields = computed(() => {
  const dates = new Set(props.dates.map(field => field.value))
  return [...props.dates, ...props.fields.filter(field => !dates.has(field.value))]
    .filter(field => field.value !== settings.value.entityField && field.value !== settings.value.stageField)
})
const stageOptions = computed(() => {
  const choices = dashboardFilterChoices(props.rows, settings.value.stageField, locale.value).filter(choice => !!choice.value.trim())
  const existing = new Set(choices.map(choice => choice.value))
  return [...choices, ...settings.value.requiredStages.filter(stage => !existing.has(stage)).map(value => ({ value, label: value }))]
})
function update(change: Partial<DashboardProcessOptions>) {
  emit('change', { processOptions: dashboardProcessSettings({ ...settings.value, ...change }) })
}
function updateData(change: Partial<DashboardDataOptions>) {
  emit('change', { dataOptions: dashboardDataSettings({ ...data.value, ...change }) })
}
function moveStage(index: number, offset: number) {
  const requiredStages = [...settings.value.requiredStages], target = index + offset
  if (target < 0 || target >= requiredStages.length) return
  const [stage] = requiredStages.splice(index, 1)
  requiredStages.splice(target, 0, stage)
  update({ requiredStages })
}
function setUnit(value: unknown) {
  if (value === 'minutes' || value === 'hours' || value === 'days') update({ unit: value })
}
</script>
