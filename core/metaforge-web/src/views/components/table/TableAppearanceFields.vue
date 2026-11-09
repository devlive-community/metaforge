<template>
  <MfFormItem name="density" :label="$t('queryChart.tableDensity')">
    <MfSelect :model-value="appearance?.density ?? 'compact'" :options="densities" :aria-label="$t('queryChart.tableDensity')" @update:model-value="changeDensity"/>
  </MfFormItem>
  <label class="flex items-center justify-between gap-2 text-sm">
    {{ $t('queryChart.tableStriped') }}
    <MfSwitch :model-value="appearance?.striped ?? false" :aria-label="$t('queryChart.tableStriped')" @update:model-value="update({ striped: $event })"/>
  </label>
  <label class="flex items-center justify-between gap-2 text-sm">
    {{ $t('queryChart.tableBorders') }}
    <MfSwitch :model-value="appearance?.bordered ?? bordered" :aria-label="$t('queryChart.tableBorders')" @update:model-value="update({ bordered: $event })"/>
  </label>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TableAppearance } from '@/model/table-appearance'

const props = withDefaults(defineProps<{ appearance?: TableAppearance; bordered?: boolean }>(), { bordered: false })
const emit = defineEmits<{ change: [appearance: TableAppearance] }>()
const { t } = useI18n()
const densities = computed(() => (['compact', 'default'] as const).map(value => ({ value, label: t('queryChart.tableDensity.' + value) })))
function update(change: TableAppearance) { emit('change', { ...props.appearance, ...change }) }
function changeDensity(value: unknown) {
  if (value === 'compact' || value === 'default') update({ density: value })
}
</script>
