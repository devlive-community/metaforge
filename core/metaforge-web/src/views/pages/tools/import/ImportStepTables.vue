<template>
  <div class="flex flex-col gap-4">
    <section class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div class="flex min-w-0 items-center gap-3">
          <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-sm font-bold uppercase text-primary">{{ analysis?.file.extension || '?' }}</span>
          <div class="min-w-0">
            <p class="m-0 truncate font-semibold text-content">{{ analysis?.file.name }}</p>
            <p class="m-0 text-xs text-content-tertiary">{{ $t('import.tables.summary', { tables: totals.tables, rows: totals.rows.toLocaleString(), database: context.database }) }}</p>
          </div>
        </div>
        <div class="flex shrink-0 gap-2">
          <MfButton @click="autoMatch"><Check :size="15"/>{{ $t('import.tables.autoMatch') }}</MfButton>
          <MfButton @click="resetSelection"><RotateCcw :size="15"/>{{ $t('import.tables.reset') }}</MfButton>
        </div>
      </div>
      <MfAlert variant="success" :title="$t('import.tables.matched', { existing: counts.existing, created: counts.create })"/>
      <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
        <MfTable :columns="columns" :data-source="plans" row-key="source" density="compact" :bordered="false">
          <template #cell-selected="{ record }">
            <MfCheckbox :model-value="record.selected" :aria-label="record.source" @update:model-value="value => toggle(record, !!value)"/>
          </template>
          <template #cell-source="{ record }">
            <span class="flex flex-col"><span class="font-semibold text-content">{{ record.source }}</span>
              <span class="text-xs text-content-tertiary">{{ $t('import.file.columns', { count: sourceColumns(record.source) }) }}</span></span>
          </template>
          <template #cell-rows="{ record }"><span class="tabular-nums">{{ $t('import.rows', { count: sourceRows(record.source).toLocaleString() }) }}</span></template>
          <template #cell-action="{ record }">
            <MfSelect class="w-56" :model-value="record.action" :options="actionOptions" @update:model-value="value => setAction(record, value as any)"/>
          </template>
          <template #cell-target="{ record }">
            <MfSelect v-if="record.action === 'existing'" class="w-72" searchable :model-value="record.target" :options="targetOptions" :placeholder="$t('import.tables.chooseTarget')"
                      @update:model-value="value => setTarget(record, String(value))"/>
            <MfInput v-else-if="record.action === 'create'" v-model="record.target" class="w-72" :aria-label="$t('import.tables.target')"/>
            <span v-else class="text-content-tertiary">-</span>
          </template>
          <template #cell-status="{ record }">
            <MfTag size="sm" :variant="statusOf(record).variant">{{ $t(`import.tables.status.${statusOf(record).key}`) }}</MfTag>
          </template>
        </MfTable>
      </div>
      <p class="m-0 flex flex-wrap items-center gap-3 text-xs text-content-tertiary">
        <MfTag size="sm" variant="info">{{ $t('import.tables.selected', { selected: selectedPlans.length, total: plans.length }) }}</MfTag>{{ $t('import.tables.selectedHint') }}
      </p>
    </section>
    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      <section v-for="(key, index) in ['existing', 'create', 'skip']" :key="key" class="rounded-mf-lg border border-line p-5">
        <h3 class="m-0 mb-2 text-base font-semibold text-content">{{ index + 1 }}. {{ $t(`import.tables.action.${key}`) }}</h3>
        <p class="m-0 text-xs text-content-tertiary">{{ $t(`import.tables.actionHint.${key}`) }}</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, RotateCcw } from '@lucide/vue'
import type { TablePlan } from '@/model/table-import'
import type { ImportWizard } from '@/composables/useImportWizard'

const props = defineProps<{ wizard: ImportWizard }>()
const { context, analysis, totals, plans, selectedPlans, targetTables, actions, autoMatch, resetSelection, setAction, setTarget } = props.wizard
const { t } = useI18n()
const columns = computed(() => [
  { title: t('import.tables.select'), dataIndex: 'selected', width: 64 },
  { title: t('import.tables.source'), dataIndex: 'source', width: 220, ellipsis: true },
  { title: t('import.tables.rows'), dataIndex: 'rows' },
  { title: t('import.tables.actionColumn'), dataIndex: 'action' },
  { title: t('import.tables.targetColumn', { database: context.value.database }), dataIndex: 'target' },
  { title: t('import.tables.statusColumn'), dataIndex: 'status' },
])
const actionOptions = computed(() => actions.value.map(value => ({ label: t(`import.tables.action.${value}`), value })))
const targetOptions = computed(() => targetTables.value.map(name => ({ label: name, value: name })))
const counts = computed(() => ({
  existing: selectedPlans.value.filter(plan => plan.action === 'existing' && plan.target).length,
  create: selectedPlans.value.filter(plan => plan.action === 'create').length,
}))
const sourceTable = (name: string) => analysis.value?.tables.find(table => table.name === name)
const sourceColumns = (name: string) => sourceTable(name)?.columns.length || 0
const sourceRows = (name: string) => sourceTable(name)?.rows || 0
function statusOf(plan: TablePlan): { key: string; variant: 'success' | 'warning' | 'default' | 'danger' } {
  if (!plan.selected || plan.action === 'skip') return { key: 'skipped', variant: 'default' }
  if (!plan.target.trim()) return { key: 'noTarget', variant: 'danger' }
  return plan.action === 'create' ? { key: 'create', variant: 'warning' } : { key: 'matched', variant: 'success' }
}
function toggle(plan: TablePlan, value: boolean) {
  plan.selected = value
  if (value && plan.action === 'skip') setAction(plan, plan.target ? 'existing' : 'create')
}
</script>
