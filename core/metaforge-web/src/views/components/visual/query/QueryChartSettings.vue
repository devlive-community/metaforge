<template>
  <component :is="collapsible ? InspectorSection : 'section'" v-for="group in groups" :key="group.title"
             :title="collapsible ? label(group.title) : undefined" :icon="collapsible ? groupIcon(group.title) : undefined"
             :default-open="collapsible ? defaultOpen : undefined"
             :class="!collapsible && 'space-y-3 border-t border-line pt-4 first:border-0 first:pt-0'">
    <h4 v-if="!collapsible" class="flex items-center gap-2 font-semibold"><span class="size-1.5 rounded-full bg-primary"/>{{ label(group.title) }}</h4>
    <MfFormGrid>
    <div v-for="field in group.fields" :key="field.key" class="min-w-0" :class="(field.control === 'textarea' || field.key === 'colors') && 'col-span-full'">
      <label v-if="field.control === 'switch'" class="flex min-h-9 items-center justify-between gap-2 text-sm text-content-secondary">
        <span class="min-w-0">{{ label(field.key) }}</span>
        <MfSwitch class="shrink-0" :model-value="Boolean(options[field.key])" :aria-label="label(field.key)" @update:model-value="set(field.key, $event)"/>
      </label>
      <MfFormItem v-else :name="field.key" :label="label(field.key)">
        <MfSelect v-if="field.control === 'select'" :model-value="String(options[field.key] ?? '')" :options="field.choices"
                  :aria-label="label(field.key)" @update:model-value="set(field.key, $event)"/>
        <MfInputNumber v-else-if="field.control === 'number'" class="w-full" decimal-only commit-on-change :model-value="options[field.key] as number | undefined"
                       :min="field.min" :max="field.max" :precision="field.precision" :aria-label="label(field.key)" @update:model-value="set(field.key, $event)"/>
        <MfTextarea v-else-if="field.control === 'textarea'" :model-value="String(options[field.key] ?? '')" :placeholder="field.placeholder" :rows="3"
                    @update:model-value="set(field.key, $event)"/>
        <MfInput v-else :model-value="String(options[field.key] ?? '')" :placeholder="field.placeholder"
                 :aria-label="label(field.key)" @update:model-value="set(field.key, $event)"/>
      </MfFormItem>
    </div>
    </MfFormGrid>
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { isDiagnosticChart, snapshotRowLimit, type QueryChartOptions } from '@/model/query-chart'
import { queryChartGeometry } from '@/model/query-chart-data'
import { queryChartStyleFields } from '@/model/query-chart-style'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

type Key = keyof QueryChartOptions
interface Field {
  key: Key
  control: 'switch' | 'select' | 'number' | 'text' | 'textarea'
  choices?: { label: string; value: string }[]
  min?: number
  max?: number
  precision?: number
  placeholder?: string
}
const props = withDefaults(defineProps<{
  options: QueryChartOptions; panel: string; timeCategory: boolean; collapsible?: boolean; fields?: readonly Key[]; defaultOpen?: boolean
}>(), { collapsible: false, defaultOpen: true })
const emit = defineEmits<{ change: [key: Key, value: unknown] }>()
const { t } = useI18n()
const label = (key: string) => t('queryChart.' + key)
const groupIcon = (title: string) => /Axis/.test(title) ? 'layout' : /tooltip|Interaction/.test(title) ? 'interaction' : 'appearance'
const select = (key: Key, choices: string[], literal = false): Field => ({
  key, control: 'select', choices: choices.map(value => ({ value, label: literal ? value : label('choice.' + key + '.' + value) }))
})
const toggle = (key: Key): Field => ({ key, control: 'switch' })
const text = (key: Key, placeholder?: string): Field => ({ key, control: 'text', placeholder })
const number = (key: Key, min?: number, max?: number, precision = 0): Field => ({ key, control: 'number', min, max, precision })
const set = (key: Key, value: unknown) => {
  if (value === undefined && !['min', 'max', 'barMaxWidth', 'barCornerRadius', 'pieRadius', 'donutHole'].includes(key)) return
  emit('change', key, value)
}

const sections = computed(() => {
  const type = props.options.type
  const geometry = queryChartGeometry(type)
  const appearance = queryChartStyleFields(type)
  const cartesian = !!geometry || isDiagnosticChart(type) || ['scatter', 'effectScatter', 'bubble', 'pictorialBar', 'heatmap', 'candlestick', 'boxplot', 'lines', 'custom',
    'waterfall', 'lollipop', 'dumbbell', 'rangeBand', 'dualAxis', 'pareto', 'cumulativeLine', 'deviationBar',
    'mirrorBar', 'stripPlot', 'ecdf', 'scatterFit', 'frequencyPolygon', 'targetComparison', 'rankTrend', 'tukeyBox', 'violinPlot',
    'ridgePlot', 'quantileComparison', 'percentileProfile', 'lorenzCurve', 'ganttTimeline', 'signedShareBar', 'quadrantScatter', 'lagScatter', 'indexTrend', 'movingAverage'].includes(type)
  const markFields = [toggle('smooth'), toggle('dashed'), number('lineWidth', 1, 6), toggle('points'), number('pointSize', 1, 18)]
    .filter(field => appearance.some(key => key === field.key))
  const fillToggle = geometry?.series === 'line' && !geometry.area && !geometry.stacked
  const fillOpacity = appearance.includes('opacity')
  const variableScale = geometry?.percentage || ['dualAxis', 'pareto', 'ecdf', 'rankTrend', 'mirrorBar', 'lorenzCurve', 'signedShareBar', 'coverageHeatmap'].includes(type)
    ? [] : [number('min', undefined, undefined, 3), number('max', undefined, undefined, 3)]
  if (props.panel === 'axes') return [
    { title: 'xAxis', fields: [text('xTitle'), ...(props.timeCategory ? [select('dateFormat', ['short', 'full', 'time'])] : []),
      select('sort', ['asc', 'desc', 'original']), ...(cartesian ? [toggle('xVisible'), toggle('xTitleVisible'), toggle('hideOverlap')] : [])] },
    { title: 'yAxis', fields: [...(geometry?.percentage ? [] : [select('numberFormat', ['integer', 'decimal', 'percent', 'compact'])]),
      ...(cartesian || type === 'gauge' ? [text('yTitle'), ...variableScale, toggle('grid'), toggle('yVisible')] : [])] }
  ]
  if (props.panel === 'legend') return [
    { title: 'legendSettings', fields: [toggle('legend'), select('legendPosition', ['top', 'bottom', 'right']), text('seriesName'), toggle('legendInteractive')] },
    { title: 'labelSettings', fields: [toggle('labels'), select('labelPosition', ['auto', 'top', 'inside']), text('unit'), number('decimals', 0, 6), toggle('labelOverlap')] }
  ]
  if (props.panel === 'style') return [
    { title: 'paletteSettings', fields: [select('palette', ['green', 'blue', 'purple']), text('colors', '#0a8f67, #3b82f6')] },
    ...(markFields.length ? [{ title: 'marks', fields: markFields }] : []),
    ...(geometry?.series === 'bar' ? [{ title: 'barAppearance', fields: [number('barMaxWidth', 1, 160), number('barCornerRadius', 0, 24)] }] : []),
    ...(['pie', 'donut', 'rose'].includes(type) ? [{ title: 'pieAppearance', fields: [
      number('pieRadius', 30, 90), ...(type === 'donut' ? [number('donutHole', 15, 75), ...(props.fields ? [toggle('donutTotal')] : [])] : []),
    ] }] : []),
    { title: 'background', fields: [...(fillToggle ? [toggle('fill')] : []), ...(fillOpacity ? [number('opacity', 0, 100)] : []), toggle('transparent')] }
  ]
  if (props.panel === 'interaction') return [
    { title: 'tooltipSettings', fields: [select('tooltip', ['dimension', 'mark', 'off']), { key: 'tooltipTemplate', control: 'textarea', placeholder: '{x} · {series}: {value}' } as Field] },
    { title: 'chartInteraction', fields: [...(cartesian ? [toggle('zoom')] : []), toggle('download'), select('pointAction', ['details', 'filter', 'highlight', 'none']),
      ...(['details', 'filter'].includes(props.options.pointAction) ? [select('detailsMode', ['panel', 'dialog'])] : [])] }
  ]
  return [{ title: 'processing', fields: [select('nulls', ['ignore', 'zero', 'keep']),
    select('timezone', ['Asia/Shanghai', 'UTC', 'America/Los_Angeles'], true), number('maxRows', 1, snapshotRowLimit)] }]
})
const groups = computed(() => {
  const allowed = props.fields
  return allowed ? sections.value.map(group => ({ ...group, fields: group.fields.filter(field => {
    if (!allowed.includes(field.key)) return false
    if (field.key === 'legendPosition' || field.key === 'legendInteractive') return props.options.legend
    if (field.key === 'labelPosition' || field.key === 'labelOverlap') return props.options.labels
    if (field.key === 'opacity' && allowed.includes('fill')) return props.options.fill
    return true
  }) }))
    .filter(group => group.fields.length) : sections.value
})
</script>
