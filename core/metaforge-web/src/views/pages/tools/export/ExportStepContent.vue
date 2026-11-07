<template>
  <div class="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
    <section class="flex min-w-0 flex-col gap-5 rounded-mf-lg border border-line p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.content.title') }}</h3>
        <MfTag size="sm" variant="info">{{ $t('export.content.configured', { done: configuredCount, total: tables.length }) }}</MfTag>
      </div>
      <div class="flex flex-col gap-2.5">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.content.what') }}</h4>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <ExportChoice v-if="canStructure" :icon="Layers" :title="$t('export.content.all')" :description="$t('export.content.allHint')"
                        :selected="state.content === 'all'" @select="state.content = 'all'"/>
          <ExportChoice :icon="LayoutPanelTop" :title="$t('export.content.data')" :description="$t('export.content.dataHint')"
                        :selected="state.content === 'data'" @select="state.content = 'data'"/>
        </div>
      </div>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <label class="flex items-center gap-3 text-sm font-semibold text-content">{{ $t('export.content.currentTable') }}
          <MfSelect class="w-72" searchable :model-value="current" :options="tables.map(name => ({ label: name, value: name }))" @update:model-value="value => configure(String(value))"/></label>
        <MfTag size="sm" variant="success">{{ $t('export.content.perTable') }}</MfTag>
      </div>
      <div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div class="flex flex-col gap-2.5">
          <h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.content.range') }}</h4>
          <ExportChoice :icon="Database" :title="$t('export.content.allRows')" :description="$t('export.content.allRowsHint')"
                        :selected="currentSetting.scope === 'all'" @select="setScope('all')"/>
          <ExportChoice :icon="Filter" :title="$t('export.content.filtered')" :description="$t('export.content.filteredHint')"
                        :selected="currentSetting.scope === 'filtered'" @select="setScope('filtered')"/>
        </div>
        <div class="flex flex-col gap-2.5">
          <h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.content.fields') }}</h4>
          <div class="flex flex-col gap-1 rounded-mf-lg border border-line bg-surface-hover/40 p-4">
            <label v-for="column in columns[current] || []" :key="column.name" class="flex min-w-0 items-center gap-2 py-1 text-sm text-content">
              <MfCheckbox :model-value="currentSetting.fields.includes(column.name)" @update:model-value="value => toggleField(column.name, !!value)"/>
              <span class="min-w-0 flex-1 truncate">{{ column.name }}</span>
              <MfTag size="sm">{{ column.type }}</MfTag>
            </label>
            <p class="m-0 mt-1 text-xs text-content-tertiary">{{ $t('export.content.fieldsHint') }}</p>
          </div>
        </div>
      </div>
      <div v-if="currentSetting.scope === 'filtered'" class="flex flex-col gap-3 border-t border-line pt-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div><h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.content.conditions') }}</h4>
            <p class="m-0 mt-0.5 text-xs text-content-tertiary">{{ $t('export.content.conditionsHint') }}</p></div>
          <MfButton @click="addFilter"><Plus :size="15"/>{{ $t('export.content.addCondition') }}</MfButton>
        </div>
        <div v-for="(item, index) in currentSetting.filters" :key="index" class="flex flex-wrap items-center gap-2">
          <MfSelect v-model="item.column" class="w-56" searchable :options="(columns[current] || []).map(column => ({ label: column.name, value: column.name }))"/>
          <MfSelect v-model="item.operator" class="w-40" :options="OPERATORS.map(value => ({ label: $t(`export.operator.${operatorKey(value)}`), value }))"/>
          <MfInput v-if="needsValue(item.operator)" v-model="item.value" class="w-64" :placeholder="$t('export.content.value')"/>
          <MfButton @click="removeFilter(item)">{{ $t('export.content.remove') }}</MfButton>
        </div>
        <p v-if="!currentSetting.filters.length" class="m-0 text-xs text-content-tertiary">{{ $t('export.content.noCondition') }}</p>
      </div>
    </section>

    <aside class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.content.list') }}</h3>
      <ul class="m-0 flex max-h-80 list-none flex-col divide-y divide-line overflow-y-auto p-0">
        <li v-for="name in tables" :key="name">
          <button type="button" class="flex w-full min-w-0 items-center justify-between gap-2 py-2.5 text-left" @click="configure(name)">
            <span class="flex min-w-0 flex-col"><span class="truncate font-semibold text-content">{{ name }}</span>
              <span class="text-xs text-content-tertiary">{{ $t(isFiltered(name) ? 'export.content.hasFilter' : 'export.content.everyRow') }}</span></span>
            <MfTag size="sm" :variant="name === current ? 'success' : 'default'">{{ $t(name === current ? 'export.content.currentTag' : isConfigured(name) ? 'export.content.configuredTag' : 'export.content.defaultTag') }}</MfTag>
          </button>
        </li>
      </ul>
      <template v-if="canStructure && state.content === 'all'">
        <h3 class="m-0 border-t border-line pt-4 text-base font-semibold text-content">{{ $t('export.content.structure') }}</h3>
        <MfCheckbox v-model="state.structure.create">{{ $t('export.content.create') }}</MfCheckbox>
        <MfCheckbox v-model="state.structure.indexes" :disabled="!state.structure.create">{{ $t('export.content.indexes') }}</MfCheckbox>
        <MfCheckbox v-model="state.structure.drop">{{ $t('export.content.drop') }}</MfCheckbox>
        <MfAlert variant="warning" :title="$t('export.content.structureNote')"/>
      </template>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Database, Filter, Layers, LayoutPanelTop, Plus } from '@lucide/vue'
import ExportChoice from './ExportChoice.vue'
import { needsValue, OPERATORS, setting } from '@/model/database-export'
import type { ExportWizard } from '@/composables/useExportWizard'

const props = defineProps<{ wizard: ExportWizard }>()
const { state, tables, columns, current, currentSetting, canStructure, configure, toggleField, addFilter, removeFilter, isConfigured } = props.wizard
const configuredCount = computed(() => tables.value.filter(isConfigured).length)
const isFiltered = (name: string) => setting(state.value, name).scope === 'filtered' && setting(state.value, name).filters.length > 0
const operatorKey = (operator: string) => ({ '=': 'eq', '!=': 'ne', '>': 'gt', '>=': 'ge', '<': 'lt', '<=': 'le', LIKE: 'like', 'NOT LIKE': 'notLike', 'IS NULL': 'isNull', 'IS NOT NULL': 'notNull' } as Record<string, string>)[operator]
function setScope(scope: 'all' | 'filtered') {
  const value = state.value.settings[current.value]
  if (!value) return
  value.scope = scope
  if (scope === 'filtered' && !value.filters.length) addFilter()
}
</script>
