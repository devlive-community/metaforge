<template>
  <div class="flex flex-col gap-4">
    <section v-if="currentPlan" class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div class="flex flex-wrap items-end gap-3">
          <div class="flex flex-col gap-1.5">
            <span class="text-sm text-content-secondary">{{ $t('import.mapping.current', { index: current + 1, total: selectedPlans.length }) }}</span>
            <MfSelect class="w-72" :model-value="current" :options="planOptions" @update:model-value="value => (current = Number(value))"/>
          </div>
          <ArrowRight :size="18" class="mb-2.5 text-content-tertiary"/>
          <div class="flex flex-col gap-1.5">
            <span class="text-sm text-content-secondary">{{ $t('import.tables.target') }}</span>
            <MfInput class="w-72" :model-value="currentPlan.target" readonly :aria-label="$t('import.tables.target')"/>
          </div>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <MfTag size="sm" variant="success">{{ $t('import.mapping.mapped', { mapped: currentCounts.mapped, total: currentCounts.total }) }}</MfTag>
          <MfTag v-if="currentCounts.attention" size="sm" variant="warning">{{ $t('import.mapping.attention', { count: currentCounts.attention }) }}</MfTag>
          <MfButton @click="remap(currentPlan)"><RotateCcw :size="15"/>{{ $t('import.mapping.auto') }}</MfButton>
        </div>
      </div>
      <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
        <MfTable :columns="columns" :data-source="rows" row-key="source" density="compact" :bordered="false">
          <template #cell-index="{ record }"><span class="tabular-nums text-content-secondary">{{ record.index + 1 }}</span></template>
          <template #cell-source="{ record }"><span class="font-semibold text-content">{{ record.source }}</span></template>
          <template #cell-sample="{ record }"><EllipsisCell :text="record.sample"/></template>
          <template #cell-type="{ record }"><MfTag size="sm">{{ record.type || '-' }}</MfTag></template>
          <template #cell-target="{ record }">
            <MfSelect class="w-56" searchable :model-value="record.mapping.target" :options="targetFieldOptions" @update:model-value="value => setField(record.mapping, String(value))"/>
          </template>
          <template #cell-check="{ record }">
            <MfTag size="sm" :variant="needsAttention(record.mapping) ? 'warning' : 'success'">{{ $t(needsAttention(record.mapping) ? 'import.mapping.attentionOne' : 'import.mapping.compatible') }}</MfTag>
          </template>
          <template #cell-handling="{ record }">
            <MfSelect class="w-40" :model-value="record.mapping.handling" :options="handlingOptions" @update:model-value="value => (record.mapping.handling = value as Handling)"/>
          </template>
        </MfTable>
      </div>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex items-center gap-5">
          <MfCheckbox v-model="matchSame" @update:model-value="value => value && remap(currentPlan!)">{{ $t('import.mapping.matchSame') }}</MfCheckbox>
          <MfCheckbox v-model="skipUnmapped">{{ $t('import.mapping.skipUnmapped') }}</MfCheckbox>
        </div>
        <div class="flex shrink-0 gap-2">
          <MfButton :disabled="current === 0" @click="current--">{{ $t('import.mapping.previousTable') }}</MfButton>
          <MfButton :disabled="current >= selectedPlans.length - 1" @click="current++">{{ $t('import.mapping.nextTable') }}<ArrowRight :size="15"/></MfButton>
        </div>
      </div>
    </section>
    <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <MfAlert v-if="passed.length" variant="success" :title="$t('import.mapping.passed', { tables: passed.join(' / ') })"/>
      <MfAlert v-if="flaggedFields.length" variant="warning" :title="$t('import.mapping.flagged', { fields: flaggedFields.join('、') })"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowRight, RotateCcw } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import type { FieldMapping, Handling } from '@/model/table-import'
import type { ImportWizard } from '@/composables/useImportWizard'

const props = defineProps<{ wizard: ImportWizard }>()
const { analysis, selectedPlans, current, currentPlan, currentCounts, flagged, preview, targetOptions, remap } = props.wizard
const { t } = useI18n()
const matchSame = ref(true)
const skipUnmapped = ref(true)
const columns = computed(() => [
  { title: '#', dataIndex: 'index' },
  { title: t('import.mapping.source'), dataIndex: 'source', width: 160, ellipsis: true },
  { title: t('import.mapping.sample'), dataIndex: 'sample', width: 200, ellipsis: true },
  { title: t('import.mapping.type'), dataIndex: 'type' },
  { title: t('import.mapping.target'), dataIndex: 'target' },
  { title: t('import.mapping.check'), dataIndex: 'check' },
  { title: t('import.mapping.handling'), dataIndex: 'handling' },
])
const planOptions = computed(() => selectedPlans.value.map((plan, index) => ({ label: `${ plan.source } · ${ t('import.rows', { count: (analysis.value?.tables.find(table => table.name === plan.source)?.rows || 0).toLocaleString() }) }`, value: index })))
const sourceColumns = computed(() => analysis.value?.tables.find(table => table.name === currentPlan.value?.source)?.columns || [])
const rows = computed(() => (currentPlan.value?.mappings || []).map((mapping, index) => {
  const column = sourceColumns.value.find(item => item.name === mapping.source)
  return { index, source: mapping.source, sample: (column?.sample || []).join('、') + ((column?.sample.length || 0) >= 3 ? '…' : ''), type: column?.type || '', mapping }
}))
const targetFieldOptions = computed(() => [
  { label: t('import.mapping.unmapped'), value: '' },
  ...(currentPlan.value ? targetOptions(currentPlan.value) : []).map(column => ({ label: column.name, value: column.name })),
])
const handlingOptions = computed(() => (['keep', 'direct', 'date', 'ignore'] as const).map(value => ({ label: t(`import.mapping.handle.${value}`), value })))
const needsAttention = (mapping: FieldMapping) => !mapping.target || mapping.handling === 'ignore' || flagged.value.has(`${ mapping.source } → ${ mapping.target }`)
function setField(mapping: FieldMapping, target: string) {
  mapping.target = target
  if (!target && skipUnmapped.value) mapping.handling = 'ignore'
  else if (target && mapping.handling === 'ignore') mapping.handling = 'direct'
}
// Unmapped fields are left out while "skip unmapped" is on.
watch([skipUnmapped, currentPlan], () => {
  if (skipUnmapped.value) currentPlan.value?.mappings.forEach(mapping => { if (!mapping.target) mapping.handling = 'ignore' })
}, { immediate: true })
const passed = computed(() => (preview.value?.tables || []).filter((table: any) => table.status === 'pass').map((table: any) => table.source))
const flaggedFields = computed(() => (currentPlan.value?.mappings || []).filter(needsAttention).map(mapping => mapping.source))
</script>
