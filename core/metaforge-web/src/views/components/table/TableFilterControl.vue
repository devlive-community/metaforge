<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { TableFilterField } from './tableFilters'

defineProps<{ field: TableFilterField; modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const { t, locale } = useI18n()
</script>

<template>
  <MfFormItem :label="field.label">
    <MfSelect v-if="field.type === 'select'" :model-value="modelValue" :options="field.options" :placeholder="t('dataTable.all')" @update:model-value="emit('update:modelValue', String($event))"/>
    <MfDatePicker v-else-if="field.type === 'date'" :model-value="modelValue" :locale="locale" :placeholder="t('dataTable.selectDate')"
                  :label="field.label" :today-text="t('dataTable.today')" :clear-text="t('dataTable.clear')"
                  :previous-text="t('dataTable.previousMonth')" :next-text="t('dataTable.nextMonth')"
                  @update:model-value="emit('update:modelValue', $event)"/>
    <MfInput v-else :model-value="modelValue" :placeholder="field.placeholder || field.label" clearable @update:model-value="emit('update:modelValue', $event)"/>
  </MfFormItem>
</template>
