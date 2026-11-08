<template>
  <MfFormItem name="measure" :label="$t(key + 'measure')" required-mark>
    <MfSelect :model-value="modelValue.measure" :options="measures" searchable :placeholder="$t(key + 'chooseMeasure')"
              :aria-label="$t(key + 'measure')" @update:model-value="value => update({ measure: String(value ?? '') })"/>
  </MfFormItem>
  <template v-if="usesSpecification">
    <div class="grid grid-cols-2 gap-2">
      <MfFormItem v-for="bound in bounds" :key="bound" :name="bound" :label="$t(key + bound)">
        <MfInputNumber :model-value="modelValue[bound]" :aria-label="$t(key + bound)" :step="1" decimal-only commit-on-change
                       @update:model-value="value => update({ [bound]: value })"/>
      </MfFormItem>
    </div>
    <p class="text-xs text-content-tertiary">{{ $t(key + 'specificationHint') }}</p>
    <MfFormItem v-if="kind === 'qualityGroupYield'" name="group" :label="$t(key + 'group')" required-mark>
      <MfSelect :model-value="modelValue.group" :options="fields" searchable :placeholder="$t(key + 'chooseGroup')"
                :aria-label="$t(key + 'group')" @update:model-value="value => update({ group: String(value ?? '') })"/>
    </MfFormItem>
  </template>
  <template v-else>
    <MfFormItem name="order" :label="$t(key + 'order')">
      <MfSelect :model-value="modelValue.order?.field" :options="orders" searchable clearable :placeholder="$t(key + 'sourceOrder')"
                :aria-label="$t(key + 'order')" @update:model-value="setOrder"/>
    </MfFormItem>
    <p class="text-xs text-content-tertiary">{{ $t(key + 'orderHint') }}</p>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProcessQualityOptions, ProcessQualityView, QualityOrder } from '@/model/process-quality'

type FieldOption = { value: string; label: string }
const props = defineProps<{
  kind: ProcessQualityView; modelValue: ProcessQualityOptions; fields: FieldOption[]; measures: FieldOption[];
  orders: (FieldOption & { type: QualityOrder['type'] })[]
}>()
const emit = defineEmits<{ 'update:modelValue': [value: ProcessQualityOptions] }>()
const key = 'processQuality.'
const bounds = ['lower', 'upper'] as const
const usesSpecification = computed(() => props.kind === 'qualitySpecification' || props.kind === 'qualityGroupYield')
function update(change: Partial<ProcessQualityOptions>) { emit('update:modelValue', { ...props.modelValue, ...change }) }
function setOrder(value: unknown) {
  const order = props.orders.find(option => option.value === value)
  update({ order: order ? { field: order.value, type: order.type } : undefined })
}
</script>
