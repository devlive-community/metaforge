<template>
  <DashboardInspectorGroup icon="data" :title="$t(key + 'settings')">
    <MfFormItem v-for="mapping in mappings" :key="mapping.field" :label="$t(key + mapping.label)" :required-mark="mapping.required">
      <MfSelect :model-value="fieldValue(mapping.field) || undefined" :options="mapping.options"
                :clearable="!mapping.required" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => changeField(mapping.field, String(value ?? ''))"/>
    </MfFormItem>
    <MfFormItem v-if="usesDoneValues && settings.statusField" :label="$t(key + 'doneValues')">
      <MfInput :model-value="settings.doneValues" :maxlength="400" @update:model-value="update({ doneValues: $event })"/>
      <p class="mt-1 text-xs text-content-tertiary">{{ $t(key + 'doneValuesHint') }}</p>
    </MfFormItem>
    <template v-if="kind === 'dueAgenda'">
      <MfFormItem :label="$t(key + 'horizonDays')">
        <MfInputNumber :model-value="settings.horizonDays" :min="1" :max="90" :precision="0" :aria-label="$t(key + 'horizonDays')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ horizonDays: value })"/>
      </MfFormItem>
      <label v-if="settings.statusField" class="flex min-w-0 items-center justify-between gap-3 text-sm">
        {{ $t(key + 'includeCompleted') }}
        <MfSwitch :model-value="settings.includeCompleted" @update:model-value="value => update({ includeCompleted: value })"/>
      </label>
    </template>
    <MfFormItem v-if="['taskBoard', 'dueAgenda', 'groupCompletion', 'onTimeCompletion'].includes(kind)" :label="limitLabel">
      <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" :aria-label="limitLabel"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && emit('change', { dataOptions: dashboardDataSettings({ ...data, limit: value }) })"/>
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
import { dashboardDataSettings, dashboardPlanningSettings, type DashboardPlanningOptions, type DashboardWidget } from '@/model/dashboard-layout'
import DashboardInspectorGroup from './DashboardInspectorGroup.vue'

type Field = 'statusField' | 'labelField' | 'ownerField' | 'groupField' | 'completedField' | 'timeField'
type Option = { value: string; label: string }
type Mapping = { field: Field; label: string; required?: boolean; options: Option[] }
const props = defineProps<{ widget: DashboardWidget; fields: Option[]; dates: Option[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const { t } = useI18n()
const key = 'dashboard.studio.planning.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardPlanningSettings(props.widget.planningOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const usesDoneValues = computed(() => ['dueAgenda', 'overdueAging', 'groupCompletion'].includes(kind.value))
const limitLabel = computed(() => t(kind.value === 'taskBoard' ? key + 'columnLimit' : 'dashboard.studio.displayLimit'))
const timeFields = computed(() => {
  const dates = new Set(props.dates.map(field => field.value))
  return [...props.dates, ...props.fields.filter(field => !dates.has(field.value))]
})
const mappings = computed<Mapping[]>(() => {
  const items: Mapping[] = []
  if (kind.value === 'taskBoard' || usesDoneValues.value) {
    items.push({ field: 'statusField', label: 'statusField', options: props.fields, required: kind.value !== 'dueAgenda' })
  }
  if (kind.value === 'taskBoard' || kind.value === 'dueAgenda') {
    items.push({ field: 'labelField', label: 'labelField', options: props.fields }, { field: 'ownerField', label: 'ownerField', options: props.fields })
  }
  if (kind.value !== 'groupCompletion') {
    items.push({ field: 'timeField', label: 'dueField', required: kind.value !== 'taskBoard',
      options: timeFields.value.filter(field => field.value !== settings.value.completedField) })
  }
  if (kind.value === 'onTimeCompletion') {
    items.push({ field: 'completedField', label: 'completedField', required: true, options: timeFields.value.filter(field => field.value !== props.widget.timeField) })
  }
  if (kind.value === 'groupCompletion' || kind.value === 'onTimeCompletion') {
    items.push({ field: 'groupField', label: 'groupField', options: props.fields })
  }
  return items
})
function fieldValue(field: Field) { return field === 'timeField' ? props.widget.timeField : settings.value[field] }
function update(change: Partial<DashboardPlanningOptions>) {
  emit('change', { planningOptions: dashboardPlanningSettings({ ...settings.value, ...change }) })
}
function changeField(field: Field, value: string) {
  if (field === 'timeField') emit('change', { timeField: value })
  else update({ [field]: value })
}
</script>
