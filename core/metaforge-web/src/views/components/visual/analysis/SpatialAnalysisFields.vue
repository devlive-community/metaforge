<template>
  <InspectorSection icon="data" :title="$t(key + 'settings')">
    <MfFormItem :label="$t(key + 'longitudeField')" required-mark>
      <MfSelect :model-value="settings.longitudeField || undefined" :options="coordinates.filter(field => field.value !== settings.latitudeField)" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ longitudeField: String(value ?? '') })"/>
    </MfFormItem>
    <MfFormItem :label="$t(key + 'latitudeField')" required-mark>
      <MfSelect :model-value="settings.latitudeField || undefined" :options="coordinates.filter(field => field.value !== settings.longitudeField)" searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="value => update({ latitudeField: String(value ?? '') })"/>
    </MfFormItem>
    <template v-if="kind === 'geoTrack'">
      <MfFormItem :label="$t(key + 'timeField')" required-mark>
        <MfSelect :model-value="widget.timeField || undefined" :options="timeFields" searchable
                  :placeholder="$t('queryChart.selectField')" @update:model-value="value => emit('change', { timeField: String(value ?? '') })"/>
      </MfFormItem>
      <MfFormItem :label="$t(key + 'groupField')">
        <MfSelect :model-value="settings.groupField || undefined" :options="fields" searchable clearable
                  @update:model-value="value => update({ groupField: String(value ?? '') })"/>
      </MfFormItem>
    </template>
    <MfFormItem v-if="kind === 'geoGrid'" :label="$t(key + 'gridDegrees')">
      <MfInputNumber :model-value="settings.gridDegrees" :min="0.01" :max="60" :step="0.1" :precision="2" :aria-label="$t(key + 'gridDegrees')"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ gridDegrees: value })"/>
    </MfFormItem>
    <MfFormItem v-if="['geoPoints', 'geoGrid', 'geoTrack'].includes(kind)" :label="$t(key + (kind === 'geoGrid' ? 'cellLimit' : 'pointLimit'))">
      <MfInputNumber :model-value="settings.pointLimit" :min="20" :max="2000" :precision="0" :aria-label="$t(key + (kind === 'geoGrid' ? 'cellLimit' : 'pointLimit'))"
                     decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ pointLimit: value })"/>
    </MfFormItem>
    <template v-if="kind === 'geoBounds'">
      <div class="grid grid-cols-2 gap-2">
        <MfFormItem v-for="bound in bounds" :key="bound.field" :label="$t(key + bound.field)">
          <MfInputNumber :model-value="settings[bound.field]" :min="-bound.maximum" :max="bound.maximum" :precision="6"
                         :aria-label="$t(key + bound.field)" :invalid="bound.maximum === 90 && settings.south > settings.north"
                         decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ [bound.field]: value })"/>
        </MfFormItem>
      </div>
      <p class="text-xs" :class="settings.south > settings.north ? 'text-danger' : 'text-content-tertiary'">
        {{ $t(key + (settings.south > settings.north ? 'invalidBounds' : 'boundsHint')) }}
      </p>
    </template>
    <template v-if="kind === 'geoHotspots'">
      <MfFormItem :label="$t(key + 'precision')">
        <MfInputNumber :model-value="settings.precision" :min="0" :max="6" :precision="0" :aria-label="$t(key + 'precision')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && update({ precision: value })"/>
      </MfFormItem>
      <MfFormItem :label="$t('dashboard.studio.displayLimit')">
        <MfInputNumber :model-value="Math.min(30, data.limit)" :min="1" :max="30" :precision="0" :aria-label="$t('dashboard.studio.displayLimit')"
                       decimal-only commit-on-change @update:model-value="value => value !== undefined && emit('change', { dataOptions: dashboardDataSettings({ ...data, limit: value }) })"/>
      </MfFormItem>
    </template>
    <template v-if="kind === 'geoDistanceBands'">
      <div class="grid grid-cols-2 gap-2">
        <MfFormItem v-for="reference in references" :key="reference.field" :label="$t(key + reference.field)" required-mark>
          <MfInputNumber :model-value="settings[reference.field] ?? undefined" :min="-reference.maximum" :max="reference.maximum" :precision="6"
                         :aria-label="$t(key + reference.field)" decimal-only commit-on-change @update:model-value="value => update({ [reference.field]: value ?? null })"/>
        </MfFormItem>
      </div>
      <MfFormItem :label="$t(key + 'distanceBands')" required-mark>
        <MfInput :model-value="settings.distanceBands" :maxlength="400" :invalid="!validBands" :aria-invalid="!validBands"
                 placeholder="1, 5, 10, 50, 100" @update:model-value="update({ distanceBands: $event })"/>
        <p class="mt-1 text-xs" :class="validBands ? 'text-content-tertiary' : 'text-danger'">{{ $t(key + 'bandHint') }}</p>
      </MfFormItem>
    </template>
    <label class="flex min-w-0 items-center justify-between gap-3 text-sm">
      {{ $t('dashboard.studio.relation.linkedFilter') }}
      <MfSwitch :model-value="widget.action === 'filter'" @update:model-value="value => emit('change', { action: value ? 'filter' : 'none' })"/>
    </label>
  </InspectorSection>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { dashboardDataSettings, dashboardSpatialSettings, type DashboardSpatialOptions, type DashboardWidget } from '@/model/dashboard-layout'
import { dashboardDistanceBoundaries } from '@/model/dashboard-spatial'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

type Option = { value: string; label: string }
const props = defineProps<{ widget: Pick<DashboardWidget, 'action' | 'dataOptions' | 'kind' | 'spatialOptions' | 'timeField'>; fields: Option[]; dates: Option[]; numbers: Option[] }>()
const emit = defineEmits<{ change: [Partial<DashboardWidget>] }>()
const key = 'dashboard.studio.spatial.'
const kind = computed(() => props.widget.kind)
const settings = computed(() => dashboardSpatialSettings(props.widget.spatialOptions))
const data = computed(() => dashboardDataSettings(props.widget.dataOptions))
const validBands = computed(() => !!dashboardDistanceBoundaries(settings.value.distanceBands))
const bounds = [{ field: 'west', maximum: 180 }, { field: 'east', maximum: 180 }, { field: 'south', maximum: 90 }, { field: 'north', maximum: 90 }] as const
const references = [{ field: 'referenceLon', maximum: 180 }, { field: 'referenceLat', maximum: 90 }] as const
const prioritize = (preferred: Option[]) => {
  const values = new Set(preferred.map(field => field.value))
  return [...preferred, ...props.fields.filter(field => !values.has(field.value))]
}
const coordinates = computed(() => prioritize(props.numbers))
const timeFields = computed(() => prioritize(props.dates))
function update(change: Partial<DashboardSpatialOptions>) {
  emit('change', { spatialOptions: dashboardSpatialSettings({ ...settings.value, ...change }) })
}
</script>
