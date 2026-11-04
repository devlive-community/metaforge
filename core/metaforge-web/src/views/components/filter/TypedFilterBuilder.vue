<template>
  <div class="flex min-w-0 flex-col gap-3">
    <slot name="toggle">
      <MfCheckbox :model-value="enabled" @update:model-value="emit('update:enabled', !!$event)">
        {{ $t('typedFilter.enabled') }}
      </MfCheckbox>
    </slot>
    <slot name="notice"/>
    <template v-if="enabled">
      <MfSelect v-if="groups.length > 1" :model-value="join" :options="joinOptions" :aria-label="$t('typedFilter.groupJoin')"
                @update:model-value="emit('update:join', $event as FilterJoin)"/>
      <section v-for="(group, groupIndex) in groups" :key="groupIndex" class="space-y-3 border-t border-line pt-4">
        <div class="flex items-center justify-between">
          <h4 class="font-semibold">{{ $t('typedFilter.group', { number: groupIndex + 1 }) }}</h4>
          <MfButton size="sm" variant="ghost" :aria-label="$t('typedFilter.deleteGroup')" @click="removeGroup(groupIndex)"><Trash2 :size="15"/></MfButton>
        </div>
        <MfSelect v-if="group.rules.length > 1" :model-value="group.join" :options="joinOptions" :aria-label="$t('typedFilter.ruleJoin')"
                  @update:model-value="replaceGroup(groupIndex, { ...group, join: $event as FilterJoin })"/>
        <div v-for="(rule, ruleIndex) in group.rules" :key="ruleIndex" class="space-y-2 rounded-mf-md border border-line p-2.5">
          <div class="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] items-center gap-2">
            <MfSelect :model-value="rule.field" searchable :options="fieldOptions" :aria-label="$t('typedFilter.field')"
                      @update:model-value="changeField(groupIndex, ruleIndex, String($event))"/>
            <MfSelect :model-value="rule.operator" :options="operatorOptions(rule)" :aria-label="$t('typedFilter.operator')"
                      @update:model-value="replaceRule(groupIndex, ruleIndex, { ...rule, operator: $event as FilterOperator, value: '', end: '' })"/>
            <MfButton size="sm" variant="ghost" :aria-label="$t('typedFilter.deleteRule')" @click="removeRule(groupIndex, ruleIndex)"><Trash2 :size="14"/></MfButton>
          </div>
          <div v-if="filterNeedsValue(rule.operator)" class="flex gap-2">
            <template v-if="kindOf(rule) === 'number'">
              <MfInputNumber class="min-w-0 flex-1" :model-value="rule.value === '' ? undefined : Number(rule.value)" :controls="false" decimal-only
                             :aria-label="$t('typedFilter.value')" :placeholder="$t('typedFilter.number')"
                             @update:model-value="changeValue(groupIndex, ruleIndex, 'value', $event)"/>
              <MfInputNumber v-if="rule.operator === 'between'" class="min-w-0 flex-1" :model-value="rule.end === '' ? undefined : Number(rule.end)" :controls="false" decimal-only
                             :aria-label="$t('typedFilter.end')" :placeholder="$t('typedFilter.end')"
                             @update:model-value="changeValue(groupIndex, ruleIndex, 'end', $event)"/>
            </template>
            <template v-else-if="kindOf(rule) === 'date'">
              <MfDatePicker class="min-w-0 flex-1" :model-value="rule.value" :locale="locale" :label="$t('typedFilter.value')" :placeholder="$t('dataTable.selectDate')"
                            :max="rule.operator === 'between' ? rule.end || undefined : undefined" v-bind="calendarLabels"
                            @update:model-value="changeValue(groupIndex, ruleIndex, 'value', $event)"/>
              <MfDatePicker v-if="rule.operator === 'between'" class="min-w-0 flex-1" :model-value="rule.end" :locale="locale" :label="$t('typedFilter.end')"
                            :placeholder="$t('typedFilter.end')" :min="rule.value || undefined" v-bind="calendarLabels"
                            @update:model-value="changeValue(groupIndex, ruleIndex, 'end', $event)"/>
            </template>
            <MfSelect v-else-if="kindOf(rule) === 'boolean'" :model-value="rule.value" :options="booleanOptions" :placeholder="$t('typedFilter.value')"
                      @update:model-value="changeValue(groupIndex, ruleIndex, 'value', String($event))"/>
            <MfInput v-else :model-value="rule.value" :placeholder="$t(rule.operator === 'in' ? 'typedFilter.inValues' : 'typedFilter.value')"
                     @update:model-value="changeValue(groupIndex, ruleIndex, 'value', $event)"/>
          </div>
        </div>
        <MfButton size="sm" :disabled="!fields.length" @click="addRule(groupIndex)"><Plus :size="14"/>{{ $t('typedFilter.addRule') }}</MfButton>
      </section>
      <MfButton size="sm" class="self-start" :disabled="!fields.length" @click="addGroup"><Plus :size="14"/>{{ $t('typedFilter.addGroup') }}</MfButton>
      <slot name="expression"/>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Plus, Trash2 } from '@lucide/vue'
import { filterNeedsValue, newFilterRule, operatorsByKind, type FilterField, type FilterGroup, type FilterJoin, type FilterOperator, type FilterRule } from '@/model/typed-filter'

const props = defineProps<{ enabled: boolean; groups: FilterGroup[]; join: FilterJoin; fields: FilterField[] }>()
const emit = defineEmits<{ 'update:enabled': [value: boolean]; 'update:groups': [value: FilterGroup[]]; 'update:join': [value: FilterJoin] }>()
const { t, locale } = useI18n()
const fieldOptions = computed(() => props.fields.map(field => ({ value: field.key, label: field.label })))
const fieldMap = computed(() => new Map(props.fields.map(field => [field.key, field])))
const joinOptions = computed(() => ['and', 'or'].map(value => ({ value, label: t('typedFilter.join.' + value) })))
const booleanOptions = computed(() => ['true', 'false'].map(value => ({ value, label: t('typedFilter.boolean.' + value) })))
const calendarLabels = computed(() => ({ todayText: t('dataTable.today'), clearText: t('dataTable.clear'),
  previousText: t('dataTable.previousMonth'), nextText: t('dataTable.nextMonth') }))
const kindOf = (rule: FilterRule) => fieldMap.value.get(rule.field)?.kind ?? rule.kind ?? 'text'
const operatorOptions = (rule: FilterRule) => operatorsByKind[kindOf(rule)].map(value => ({ value, label: t('typedFilter.operatorChoice.' + value) }))

function replaceGroup(index: number, group: FilterGroup) {
  if (props.enabled) emit('update:groups', props.groups.map((value, i) => i === index ? group : value))
}
function replaceRule(groupIndex: number, ruleIndex: number, rule: FilterRule) {
  const group = props.groups[groupIndex]
  replaceGroup(groupIndex, { ...group, rules: group.rules.map((value, i) => i === ruleIndex ? rule : value) })
}
function changeField(groupIndex: number, ruleIndex: number, key: string) {
  const field = fieldMap.value.get(key)
  if (field) replaceRule(groupIndex, ruleIndex, newFilterRule(field))
}
function changeValue(groupIndex: number, ruleIndex: number, part: 'value' | 'end', value: string | number | undefined) {
  const rule = props.groups[groupIndex].rules[ruleIndex]
  replaceRule(groupIndex, ruleIndex, { ...rule, kind: kindOf(rule), [part]: value === undefined ? '' : String(value) })
}
function removeRule(groupIndex: number, ruleIndex: number) {
  const group = props.groups[groupIndex]
  replaceGroup(groupIndex, { ...group, rules: group.rules.filter((_, i) => i !== ruleIndex) })
}
function removeGroup(index: number) {
  if (props.enabled) emit('update:groups', props.groups.filter((_, i) => i !== index))
}
function addRule(index: number) {
  const field = props.fields[0]
  const group = props.groups[index]
  if (field) replaceGroup(index, { ...group, rules: [...group.rules, newFilterRule(field)] })
}
function addGroup() {
  const field = props.fields[0]
  if (props.enabled && field) emit('update:groups', [...props.groups, { join: 'and', rules: [newFilterRule(field)] }])
}
</script>
