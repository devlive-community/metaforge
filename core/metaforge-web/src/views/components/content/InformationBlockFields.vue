<template>
  <div class="grid gap-2">
    <InspectorSection v-for="(item, index) in modelValue" :key="item.id"
                      :title="item.title.trim() || $t(key + 'item', { number: index + 1 })" :default-open="index === modelValue.length - 1">
      <MfFormItem :label="$t(key + fields.title)" required-mark>
        <MfInput :model-value="item.title" :aria-label="$t(key + fields.title)" :maxlength="160" @update:model-value="value => patch(index, { title: value })"/>
      </MfFormItem>
      <MfFormItem v-if="fields.value" :label="$t(key + fields.value)">
        <MfInputNumber v-if="numeric" :model-value="reportNumber(item.value)" :aria-label="$t(key + fields.value)"
                       :min="0" :max="kind === 'ratingCards' ? 5 : undefined" :step="kind === 'ratingCards' ? .1 : 1" decimal-only commit-on-change
                       @update:model-value="value => patch(index, { value: value === undefined ? '' : String(value) })"/>
        <MfInput v-else :model-value="item.value" :aria-label="$t(key + fields.value)" :maxlength="fields.value === 'url' ? 2048 : 250"
                 @update:model-value="value => patch(index, { value })"/>
      </MfFormItem>
      <MfFormItem v-if="fields.secondary" :label="$t(key + fields.secondary)">
        <MfInputNumber v-if="kind === 'capacityGrid'" :model-value="reportNumber(item.secondary)" :aria-label="$t(key + fields.secondary)"
                       :min="0" decimal-only commit-on-change
                       @update:model-value="value => patch(index, { secondary: value === undefined ? '' : String(value) })"/>
        <MfInput v-else :model-value="item.secondary" :aria-label="$t(key + fields.secondary)" :maxlength="250"
                 @update:model-value="secondary => patch(index, { secondary })"/>
      </MfFormItem>
      <MfFormItem v-if="fields.tone" :label="$t(key + fields.tone)">
        <MfSelect :model-value="item.tone" :aria-label="$t(key + fields.tone)" :options="tones" @update:model-value="value => setTone(index, value)"/>
      </MfFormItem>
      <MfFormItem v-if="fields.detail" :label="$t(key + fields.detail)">
        <MfTextarea :model-value="item.detail" :aria-label="$t(key + fields.detail)" :rows="2" :maxlength="2000"
                    @update:model-value="detail => patch(index, { detail })"/>
      </MfFormItem>
      <div class="flex flex-wrap items-center justify-end gap-1.5">
        <MfButton size="sm" variant="ghost" :disabled="index === 0" :aria-label="$t(key + 'moveUp')" @click="move(index, -1)"><ArrowUp :size="15"/></MfButton>
        <MfButton size="sm" variant="ghost" :disabled="index === modelValue.length - 1" :aria-label="$t(key + 'moveDown')" @click="move(index, 1)"><ArrowDown :size="15"/></MfButton>
        <MfButton size="sm" variant="ghost" :aria-label="$t(key + 'remove')" @click="remove(index)"><Trash2 :size="15"/></MfButton>
      </div>
    </InspectorSection>
    <MfButton size="sm" :disabled="modelValue.length >= informationItemLimit" @click="add"><Plus :size="15"/>{{ $t(key + 'add') }}</MfButton>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, ArrowUp, Plus, Trash2 } from '@lucide/vue'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { v4 as uuid } from 'uuid'
import { informationFields, informationItemLimit, informationTones, type InformationBlockKind, type InformationItem } from '@/model/information-block'
import { reportNumber } from '@/model/report-values'
import InspectorSection from '@/views/components/studio/InspectorSection.vue'

const props = defineProps<{ kind: InformationBlockKind; modelValue: InformationItem[] }>()
const emit = defineEmits<{ 'update:modelValue': [items: InformationItem[]] }>()
const { t } = useI18n()
const key = 'informationBlock.'
const fields = computed<Partial<Record<keyof Omit<InformationItem, 'id'>, string>>>(() => informationFields[props.kind])
const numeric = computed(() => props.kind === 'capacityGrid' || props.kind === 'ratingCards')
const tones = computed(() => informationTones.map(value => ({ value, label: t(key + fields.value.tone + '.' + value) })))
function patch(index: number, update: Partial<InformationItem>) {
  emit('update:modelValue', props.modelValue.map((item, position) => position === index ? { ...item, ...update } : item))
}
function setTone(index: number, input: unknown) {
  const tone = informationTones.find(value => value === input)
  if (tone) patch(index, { tone })
}
function move(index: number, offset: number) {
  const next = index + offset
  if (next < 0 || next >= props.modelValue.length) return
  const items = [...props.modelValue]
  const [item] = items.splice(index, 1)
  items.splice(next, 0, item)
  emit('update:modelValue', items)
}
function remove(index: number) { emit('update:modelValue', props.modelValue.filter((_, position) => position !== index)) }
function add() {
  if (props.modelValue.length >= informationItemLimit) return
  emit('update:modelValue', [...props.modelValue, { id: uuid(), title: '', value: '', detail: '', tone: 'info' }])
}
</script>
