<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfForm } from 'metaforge-ui'
import { adhocColumnIssue, adhocOperators, isAdhocNumeric, fieldLabel, type AdhocColumn } from '@/model/dataset-adhoc'

const props = defineProps<{ column: AdhocColumn; disabled?: boolean }>()
const emit = defineEmits<{ change: [column: AdhocColumn] }>()
const { t, locale } = useI18n()
const form = ref<InstanceType<typeof MfForm>>()
const set = (key: keyof AdhocColumn, value: unknown) => emit('change', { ...props.column, [key]: value })
const choices = computed(() => (props.column.mode === 'FILTER'
  ? adhocOperators(props.column.type)
  : isAdhocNumeric(props.column.type) ? ['SUM', 'COUNT', 'MAX', 'MIN', 'AVG'] : ['COUNT', 'MAX', 'MIN'])
  .map(value => ({ value, label: t('dataset.adhoc.operator.' + value) })))
const needsValue = computed(() => props.column.mode === 'FILTER' && !['NULL', 'NNULL'].includes(props.column.expression || ''))
defineExpose({ validate: () => form.value?.validate() })
</script>

<template>
  <MfForm ref="form" :key="column.key" :model="{ ...column }" :disabled="disabled">
    <div class="mb-1 min-w-0"><h3 class="truncate text-sm font-semibold" :title="fieldLabel(column)">{{ fieldLabel(column) }}</h3><p class="mt-1 truncate text-xs text-content-tertiary">{{ column.name }} · {{ column.type }}</p></div>
    <MfFormItem v-if="column.mode !== 'DIMENSION'" name="expression" :label="$t(column.mode === 'FILTER' ? 'common.expression' : 'dataset.adhoc.aggregation')"
                :rules="[{ required: true, message: $t('dataset.tip.selectExpression') }]">
      <MfSelect :model-value="column.expression" :options="choices" :aria-label="$t('common.expression')" @update:model-value="set('expression', $event)"/>
    </MfFormItem>
    <MfFormItem v-if="needsValue" name="value" :label="$t('common.value')"
                :rules="[{ validator: () => { const issue = adhocColumnIssue(column); return issue ? $t(issue) : true } }]">
      <MfInputNumber v-if="isAdhocNumeric(column.type)" class="w-full" decimal-only commit-on-change :aria-label="$t('common.value')"
                     :model-value="column.value === '' ? undefined : Number(column.value)" @update:model-value="set('value', $event == null ? '' : String($event))"/>
      <MfDatePicker v-else-if="column.type === 'DATETIME'" :model-value="column.value" :locale="locale" :label="$t('common.value')" :placeholder="$t('dataTable.selectDate')"
                    :today-text="$t('dataTable.today')" :clear-text="$t('dataTable.clear')" :previous-text="$t('dataTable.previousMonth')" :next-text="$t('dataTable.nextMonth')" @update:model-value="set('value', $event)"/>
      <MfSelect v-else-if="column.type === 'BOOLEAN'" :model-value="column.value" :aria-label="$t('common.value')" :options="['true', 'false'].map(value => ({ value, label: $t('typedFilter.boolean.' + value) }))" @update:model-value="set('value', $event)"/>
      <MfInput v-else :model-value="column.value" :aria-label="$t('common.value')" @update:model-value="set('value', $event)"/>
    </MfFormItem>
    <template v-if="column.mode !== 'FILTER'">
      <MfFormItem name="alias" :label="$t('common.alias')"><MfInput :model-value="column.alias" :placeholder="fieldLabel(column)" @update:model-value="set('alias', $event)"/></MfFormItem>
      <MfFormItem name="order" :label="$t('common.sort')">
        <MfSelect :model-value="column.order || ''" :options="[{ value: '', label: $t('dataset.common.columnSortNone') }, { value: 'ASC', label: $t('dataset.common.columnOrderAsc') }, { value: 'DESC', label: $t('dataset.common.columnOrderDesc') }]" @update:model-value="set('order', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="column.mode === 'DIMENSION'" name="function" :label="$t('dataset.common.customFunction')"><MfInput :model-value="column.function" @update:model-value="set('function', $event)"/></MfFormItem>
    </template>
  </MfForm>
</template>
