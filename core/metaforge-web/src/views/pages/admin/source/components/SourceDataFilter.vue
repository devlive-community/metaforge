<template>
  <section class="shrink-0 rounded-mf-lg border border-line bg-surface-hover/40 px-4 pb-4 pt-3">
    <header class="mb-3 flex items-center justify-between gap-3">
      <h3 class="m-0 inline-flex items-center gap-2 text-sm font-semibold text-primary"><Filter :size="15"/>{{ $t('source.data.filterTitle') }}</h3>
      <div class="flex items-center gap-2">
        <MfButton size="sm" :disabled="!columns.length" @click="emit('add')"><Plus :size="14"/>{{ $t('source.data.addCondition') }}</MfButton>
        <MfDropdown trigger="click" placement="bottomRight">
          <MfButton size="sm" :aria-label="$t('source.data.more')"><Ellipsis :size="14"/></MfButton>
          <template #overlay>
            <MfMenu><MfMenuItem :disabled="!conditions.length" @click="emit('clear')">{{ $t('source.data.clearConditions') }}</MfMenuItem></MfMenu>
          </template>
        </MfDropdown>
      </div>
    </header>
    <!-- Three 36px rows and two 12px gaps fit; more conditions scroll here instead of pushing the table away. -->
    <div class="flex max-h-[8.25rem] flex-col gap-3 overflow-y-auto pr-1">
      <div v-for="condition in conditions" :key="condition.id" class="flex min-h-9 shrink-0 items-center gap-3">
        <div class="w-56 shrink-0">
          <MfSelect :model-value="condition.column" size="sm" searchable :options="columnOptions" :placeholder="$t('source.data.selectField')"
                    @update:model-value="value => emit('changeColumn', condition, String(value))">
            <template #prefix><component :is="kindIcon(condition.column)" :size="14"/></template>
          </MfSelect>
        </div>
        <template v-if="kindOf(condition.column) === 'date'">
          <div class="data-range flex h-9 w-[30rem] min-w-0 items-center rounded-mf-md border border-line bg-surface pr-2.5">
            <MfDatePicker v-model="condition.value[0]" class="min-w-0 flex-1" :locale="locale" :label="$t('source.data.startDate')" :placeholder="$t('source.data.startDate')" :max="condition.value[1] || undefined"/>
            <span class="px-1 text-content-tertiary">~</span>
            <MfDatePicker v-model="condition.value[1]" class="min-w-0 flex-1" :locale="locale" :label="$t('source.data.endDate')" :placeholder="$t('source.data.endDate')" :min="condition.value[0] || undefined"/>
            <CalendarDays :size="15" class="shrink-0 text-content-tertiary"/>
          </div>
          <div class="flex-1"/>
        </template>
        <template v-else>
          <div class="w-40 shrink-0">
            <MfSelect :model-value="condition.operator" size="sm" :options="operatorOptions(condition.column)" :disabled="!condition.column"
                      @update:model-value="value => emit('changeOperator', condition, value as DataOperator)"/>
          </div>
          <div class="min-w-0 flex-1">
            <MfSelect v-if="condition.operator === 'IN' || condition.operator === 'NOT IN'" v-model="condition.value" size="sm" multiple tags searchable
                      :options="(condition.value as string[]).map(value => ({ label: value, value }))" :placeholder="$t('source.data.tagsPlaceholder')" :empty-text="$t('source.data.tagsPlaceholder')"/>
            <MfInput v-else v-model="condition.value as string" size="sm" :disabled="!condition.column || !needsValue(condition.operator)"
                     :placeholder="placeholder(condition)" @keydown.enter="emit('search')"/>
          </div>
        </template>
        <MfDropdown trigger="click" placement="bottomRight">
          <button type="button" class="inline-flex size-7 shrink-0 items-center justify-center rounded-mf-md text-content-tertiary hover:bg-surface-hover hover:text-content" :aria-label="$t('source.data.conditionActions')"><EllipsisVertical :size="15"/></button>
          <template #overlay>
            <MfMenu><MfMenuItem @click="emit('remove', condition.id)">{{ $t('source.data.removeCondition') }}</MfMenuItem></MfMenu>
          </template>
        </MfDropdown>
      </div>
    </div>
    <footer class="mt-4 flex justify-end gap-2">
      <MfButton :disabled="loading" @click="emit('reset')"><RotateCw :size="14"/>{{ $t('common.reset') }}</MfButton>
      <MfButton variant="primary" :loading="loading" @click="emit('search')"><Search :size="14"/>{{ $t('source.data.query') }}</MfButton>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CalendarDays, Ellipsis, EllipsisVertical, Filter, Hash, Plus, RotateCw, Search, Type } from '@lucide/vue'
import { dataKind, needsValue, operatorsFor, type DataCondition, type DataOperator } from '@/model/source-data'

const props = defineProps<{ conditions: DataCondition[]; columns: string[]; typeOf: (name: string) => string; loading: boolean }>()
const emit = defineEmits<{
  add: []; clear: []; search: []; reset: []; remove: [id: number]
  changeColumn: [condition: DataCondition, column: string]; changeOperator: [condition: DataCondition, operator: DataOperator]
}>()
const { t, locale } = useI18n()
const columnOptions = computed(() => props.columns.map(name => ({ label: name, value: name })))
const kindOf = (name: string) => dataKind(props.typeOf(name))
const kindIcon = (name: string) => ({ date: CalendarDays, number: Hash, text: Type })[kindOf(name)]
const operatorOptions = (name: string) => operatorsFor(kindOf(name)).map(value => ({ label: value, value }))
function placeholder(condition: DataCondition) {
  if (!condition.column) return t('source.data.selectFieldFirst')
  if (!needsValue(condition.operator)) return t('source.data.noValueNeeded')
  return t(condition.operator === 'LIKE' ? 'source.data.likePlaceholder' : 'source.data.valuePlaceholder', { name: condition.column })
}
</script>

<style scoped>
/* The pickers sit borderless inside the range box, sized to its inner height. */
.data-range :deep(button) { height: 34px; border-color: transparent; box-shadow: none; }
</style>
