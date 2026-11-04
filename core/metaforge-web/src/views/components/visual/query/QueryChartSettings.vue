<template>
  <section v-for="group in groups" :key="group.title" class="space-y-3 border-t border-line pt-4 first:border-0 first:pt-0">
    <h4 class="flex items-center gap-2 font-semibold"><span class="size-1.5 rounded-full bg-primary"/>{{ label(group.title) }}</h4>
    <div v-for="field in group.fields" :key="field.key" class="min-w-0">
      <label v-if="field.control === 'switch'" class="flex items-center justify-between gap-3">
        <span>{{ label(field.key) }}</span>
        <MfSwitch :model-value="Boolean(options[field.key])" :aria-label="label(field.key)" @update:model-value="set(field.key, $event)"/>
      </label>
      <MfFormItem v-else :name="field.key" :label="label(field.key)">
        <MfSelect v-if="field.control === 'select'" :model-value="String(options[field.key] ?? '')" :options="field.choices" @update:model-value="set(field.key, $event)"/>
        <MfInputNumber v-else-if="field.control === 'number'" class="w-full" :model-value="options[field.key] as number | undefined"
                       :min="field.min" :max="field.max" :precision="field.precision" @update:model-value="set(field.key, $event)"/>
        <MfTextarea v-else-if="field.control === 'textarea'" :model-value="String(options[field.key] ?? '')" :placeholder="field.placeholder" :rows="3"
                    @update:model-value="set(field.key, $event)"/>
        <MfInput v-else :model-value="String(options[field.key] ?? '')" :placeholder="field.placeholder" @update:model-value="set(field.key, $event)"/>
      </MfFormItem>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { snapshotRowLimit, type QueryChartOptions } from '@/model/query-chart'

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
const props = defineProps<{ options: QueryChartOptions; panel: string; timeCategory: boolean }>()
const emit = defineEmits<{ change: [key: Key, value: unknown] }>()
const { t } = useI18n()
const label = (key: string) => t('queryChart.' + key)
const select = (key: Key, choices: string[], literal = false): Field => ({
  key, control: 'select', choices: choices.map(value => ({ value, label: literal ? value : label('choice.' + key + '.' + value) }))
})
const toggle = (key: Key): Field => ({ key, control: 'switch' })
const text = (key: Key, placeholder?: string): Field => ({ key, control: 'text', placeholder })
const number = (key: Key, min?: number, max?: number, precision = 0): Field => ({ key, control: 'number', min, max, precision })
const set = (key: Key, value: unknown) => {
  if (value === undefined && key !== 'min' && key !== 'max') return
  emit('change', key, value)
}

const groups = computed(() => {
  const type = props.options.type
  const cartesian = ['line', 'bar', 'area', 'scatter', 'horizontal'].includes(type)
  const line = type === 'line' || type === 'area'
  if (props.panel === 'axes') return [
    { title: 'xAxis', fields: [text('xTitle'), ...(props.timeCategory ? [select('dateFormat', ['short', 'full', 'time'])] : []),
      select('sort', ['asc', 'desc', 'original']), ...(cartesian ? [toggle('xVisible'), toggle('xTitleVisible'), toggle('hideOverlap')] : [])] },
    { title: 'yAxis', fields: [select('numberFormat', ['integer', 'decimal', 'percent', 'compact']),
      ...(cartesian ? [text('yTitle'), number('min', undefined, undefined, 3), number('max', undefined, undefined, 3), toggle('grid'), toggle('yVisible')] : [])] }
  ]
  if (props.panel === 'legend') return [
    { title: 'legendSettings', fields: [toggle('legend'), select('legendPosition', ['top', 'bottom', 'right']), text('seriesName'), toggle('legendInteractive')] },
    { title: 'labelSettings', fields: [toggle('labels'), select('labelPosition', ['auto', 'top', 'inside']), text('unit'), number('decimals', 0, 6), toggle('labelOverlap')] }
  ]
  if (props.panel === 'style') return [
    { title: 'paletteSettings', fields: [select('palette', ['green', 'blue', 'purple']), text('colors', '#0a8f67, #3b82f6')] },
    ...(line || type === 'scatter' ? [{ title: 'marks', fields: [...(line ? [toggle('smooth'), toggle('dashed'), number('lineWidth', 1, 6), toggle('points')] : []), number('pointSize', 1, 18)] }] : []),
    { title: 'background', fields: [...(type === 'line' ? [toggle('fill')] : []), ...(line ? [number('opacity', 0, 100)] : []), toggle('transparent')] }
  ]
  if (props.panel === 'interaction') return [
    { title: 'tooltipSettings', fields: [select('tooltip', ['dimension', 'mark', 'off']), { key: 'tooltipTemplate', control: 'textarea', placeholder: '{x} · {series}: {value}' } as Field] },
    { title: 'chartInteraction', fields: [...(cartesian ? [toggle('zoom')] : []), toggle('download'), select('pointAction', ['details', 'filter', 'highlight', 'none']),
      ...(['details', 'filter'].includes(props.options.pointAction) ? [select('detailsMode', ['panel', 'dialog'])] : [])] }
  ]
  return [{ title: 'processing', fields: [select('nulls', ['ignore', 'zero', 'keep']),
    select('timezone', ['Asia/Shanghai', 'UTC', 'America/Los_Angeles'], true), number('maxRows', 1, snapshotRowLimit)] }]
})
</script>
