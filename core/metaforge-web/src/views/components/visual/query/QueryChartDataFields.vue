<template>
  <template v-if="spec">
    <p v-if="options.type === 'map' || options.type === 'custom'" class="rounded-mf-md bg-primary/8 p-2 text-xs leading-5 text-content-secondary">
      {{ $t('queryChart.mapping.help.' + options.type) }}
    </p>
    <MfFormItem v-if="spec.category" :label="$t('queryChart.mapping.' + spec.category)" required-mark>
      <MfSelect :model-value="options.category" :options="fields" :disabled="!fields.length" searchable
                @update:model-value="emit('change', { category: String($event ?? '') })"/>
    </MfFormItem>
    <MfFormItem v-if="spec.series" :label="$t('queryChart.mapping.' + spec.series)" :required-mark="requiresChartSeries(options.type)">
      <MfSelect :model-value="options.series || 0" :options="seriesOptions" :disabled="!fields.length" searchable @update:model-value="changeSeries"/>
    </MfFormItem>
    <MfFormItem v-for="key in spec.fields" :key="key" :label="$t('queryChart.mapping.' + key)" required-mark>
      <MfSelect :model-value="options.chartFields?.[key]" :options="spec.timeFields?.includes(key) ? fields : numericFields"
                :disabled="!(spec.timeFields?.includes(key) ? fields : numericFields).length" searchable :placeholder="$t('queryChart.selectField')"
                @update:model-value="changeField(key, $event)"/>
    </MfFormItem>
    <MfFormItem v-if="spec.parallel" :label="$t('queryChart.mapping.parallel')" required-mark>
      <MfSelect :model-value="options.parallelFields || []" :options="numericFields" multiple searchable
                :placeholder="$t('queryChart.selectField')" @update:model-value="changeParallel"/>
    </MfFormItem>
    <MfFormItem v-if="spec.map" :label="$t('queryChart.mapping.geoJson')" required-mark>
      <div class="grid w-full min-w-0 gap-2">
        <MfTextarea :model-value="options.mapGeoJson" :rows="3" :maxlength="geoJsonLimit"
                    :placeholder="$t('queryChart.mapping.geoJsonHint')" @update:model-value="emit('change', { mapGeoJson: $event })"/>
        <input type="file" accept=".json,.geojson,application/json,application/geo+json"
               class="w-full min-w-0 text-xs text-content-secondary" :aria-label="$t('queryChart.mapping.geoJsonFile')" @change="readMapFile">
        <span v-if="fileError" role="alert" class="text-xs text-danger">{{ $t('queryChart.issue.invalidMap') }}</span>
      </div>
    </MfFormItem>
  </template>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { chartDataSpec, geoJsonLimit, requiresChartSeries, type ChartMappings } from '@/model/query-chart-data'
import type { QueryChartType } from '@/model/query-chart'

type Field = { label: string; value: string }
const props = defineProps<{ options: ChartMappings & { type: QueryChartType }; fields: Field[]; numericFields: Field[] }>()
const emit = defineEmits<{ change: [value: Partial<ChartMappings>] }>()
const { t } = useI18n()
const spec = computed(() => chartDataSpec(props.options.type))
const seriesOptions = computed(() => [{ value: 0, label: t('queryChart.ungrouped') }, ...props.fields])
const fileError = ref(false)
function changeSeries(value: unknown) { emit('change', { series: typeof value === 'string' ? value : '' }) }
function changeField(key: string, value: unknown) {
  emit('change', { chartFields: { ...props.options.chartFields, [key]: typeof value === 'string' ? value : '' } })
}
function changeParallel(value: unknown) {
  if (Array.isArray(value)) emit('change', { parallelFields: value.filter((field): field is string => typeof field === 'string').slice(0, 12) })
}
async function readMapFile(event: Event) {
  const input = event.currentTarget as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  fileError.value = file.size > geoJsonLimit
  if (fileError.value) return
  try { emit('change', { mapGeoJson: await file.text() }) }
  catch { fileError.value = true }
}
</script>
