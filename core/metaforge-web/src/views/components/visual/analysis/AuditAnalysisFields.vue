<template>
  <InspectorSection icon="data" :title="$t('dashboard.studio.audit.settings')">
    <MfFormItem v-if="kind === 'duplicateKeys' || kind === 'missingPatterns'" :label="$t(kind === 'duplicateKeys' ? key + 'keyFields' : key + 'checkFields')" required-mark>
      <MfSelect :model-value="settings.fields" :options="fields" multiple searchable clearable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => update({ fields: Array.isArray(value) ? value.map(String) : [] })"/>
      <p class="mt-1 text-xs text-content-tertiary">{{ $t(kind === 'duplicateKeys' ? key + 'completeKeys' : key + 'fieldLimit') }}</p>
    </MfFormItem>
    <template v-if="kind === 'fieldComparison'">
      <MfFormItem v-for="(label, index) in ['leftField', 'rightField']" :key="label" :label="$t(key + label)" required-mark>
        <MfSelect :model-value="settings.fields[index] || undefined" :options="fields.filter(field => field.value !== settings.fields[1 - index])" searchable
                  :disabled="index === 1 && !settings.fields[0]"
                  :placeholder="$t('queryChart.selectField')" @update:model-value="value => comparisonField(index, String(value ?? ''))"/>
      </MfFormItem>
      <label class="flex min-w-0 items-center justify-between gap-3 text-sm">
        {{ $t(key + 'ignoreCase') }}
        <MfSwitch :model-value="settings.ignoreCase" @update:model-value="value => update({ ignoreCase: value })"/>
      </label>
    </template>
    <MfFormItem v-if="kind === 'valueFrequency' || kind === 'invalidValues'" :label="$t(key + 'valueField')" required-mark>
      <MfSelect :model-value="widget.field || undefined" :options="fields" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="value => emit('change', { field: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="kind === 'invalidValues'" :label="$t(key + 'expectedType')">
      <MfSelect :model-value="settings.rule" :options="dashboardAuditRules.map(value => ({ value, label: $t(key + 'rule.' + value) }))"
                @update:model-value="setRule"/>
      <p class="mt-1 text-xs text-content-tertiary">{{ $t(key + 'invalidHint') }}</p>
    </MfFormItem>
    <template v-if="kind === 'dateCoverage'">
      <MfFormItem :label="$t(key + 'dateField')" required-mark>
        <MfSelect :model-value="widget.timeField || undefined" :options="fields" searchable :placeholder="$t('queryChart.selectField')"
                  @update:model-value="value => emit('change', { timeField: String(value ?? '') })"/>
      </MfFormItem>
      <MfFormItem :label="$t(key + 'minGapDays')">
        <MfInputNumber :model-value="settings.minGapDays" :min="1" :max="365" :precision="0" decimal-only commit-on-change
                       @update:model-value="value => value !== undefined && update({ minGapDays: value })"/>
      </MfFormItem>
    </template>
    <MfFormItem :label="$t('dashboard.studio.displayLimit')">
      <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" decimal-only commit-on-change
                     @update:model-value="value => value !== undefined && emit('change', { dataOptions: dashboardDataSettings({ ...data, limit: value }) })"/>
    </MfFormItem>
    <label v-if="kind !== 'dateCoverage'" class="flex min-w-0 items-center justify-between gap-3 text-sm">
      {{ $t('dashboard.studio.relation.linkedFilter') }}
      <MfSwitch :model-value="widget.action === 'filter'" @update:model-value="value => emit('change', { action: value ? 'filter' : 'none' })"/>
    </label>
  </InspectorSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { dashboardAuditRules, dashboardAuditSettings, dashboardDataSettings, type DashboardAuditOptions, type DashboardWidget } from '@/model/dashboard-layout'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'auditOptions' | 'dataOptions' | 'field' | 'kind' | 'timeField'>; fields: { value: string; label: string }[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const key = 'dashboard.studio.audit.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardAuditSettings(props.widget.auditOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
function update(change: Partial<DashboardAuditOptions>) {
  emit('change', { auditOptions: dashboardAuditSettings({ ...settings.value, ...change }) })
}
function comparisonField(index: number, value: string) {
  const fields = settings.value.fields.slice(0, 2)
  fields[index] = value
  update({ fields })
}
function setRule(value: unknown) {
  const rule = dashboardAuditRules.find(option => option === value)
  if (rule) update({ rule })
}
</script>
