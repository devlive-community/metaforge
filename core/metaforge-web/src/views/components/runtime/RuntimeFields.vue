<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown, CircleHelp } from '@lucide/vue'
import { groupRuntimeFields, type RuntimeField } from '@/model/runtime-fields'
/**
 * Editors for the runtime settings a plugin declares, by group. The settings page shows every group
 * open; the sync dialog passes `collapsible` so only the first is open at the start.
 */
const props = defineProps<{ fields: RuntimeField[]; values: Record<string, string>; collapsible?: boolean }>()
const emit = defineEmits<{ (e: 'change', name: string, value: string): void }>()
const { t, te } = useI18n()

const groups = computed(() => groupRuntimeFields(props.fields))
// Groups the user toggled away from their starting state: the first group starts open, the others closed.
const toggled = ref<Set<string>>(new Set())
function isOpen(group: string | null) {
  if (!props.collapsible || group === null) return true
  return (groups.value[0]?.group === group) !== toggled.value.has(group)
}
function toggle(group: string) {
  const next = new Set(toggled.value)
  if (!next.delete(group)) next.add(group)
  toggled.value = next
}
// Known settings have translated labels; a plugin's own settings show as it declares them.
const label = (field: RuntimeField) => te('workflow.studio.setting.' + field.name) ? t('workflow.studio.setting.' + field.name) : field.name
const hint = (field: RuntimeField) => te('workflow.studio.fieldDescription.' + field.name) ? t('workflow.studio.fieldDescription.' + field.name) : field.description
const groupLabel = (group: string) => te('settings.runtime.group.' + group) ? t('settings.runtime.group.' + group) : group
const numberOf = (value?: string) => value === undefined || value === '' || Number.isNaN(Number(value)) ? undefined : Number(value)
</script>
<template>
  <div class="flex flex-col gap-4">
    <section v-for="item in groups" :key="item.group ?? ''" class="min-w-0">
      <button v-if="item.group && collapsible" type="button" class="mb-2 flex w-full items-center gap-1.5 text-sm font-semibold text-content" :aria-expanded="isOpen(item.group)" @click="toggle(item.group)">
        <ChevronDown :size="14" class="shrink-0 text-content-tertiary transition-transform" :class="isOpen(item.group) ? '' : '-rotate-90'"/>{{ groupLabel(item.group) }}
      </button>
      <h3 v-else-if="item.group" class="mb-2 text-sm font-semibold text-content">{{ groupLabel(item.group) }}</h3>
      <div v-show="isOpen(item.group)" class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div v-for="field in item.fields" :key="field.name" class="flex min-w-0 flex-col gap-1.5">
          <span class="flex items-center gap-1.5 text-sm font-medium text-content"><span class="min-w-0 truncate">{{ label(field) }}</span>
            <MfTooltip v-if="hint(field)" :content="hint(field)"><CircleHelp :size="13" class="text-content-tertiary"/></MfTooltip>
          </span>
          <MfSwitch v-if="field.type === 'BOOLEAN'" :model-value="values[field.name] === 'true'" :aria-label="field.name"
                    @update:model-value="value => emit('change', field.name, value ? 'true' : 'false')"/>
          <MfInputNumber v-else-if="field.type === 'NUMBER'" class="w-full" :model-value="numberOf(values[field.name])" :aria-label="field.name"
                         @update:model-value="value => emit('change', field.name, value === null || value === undefined ? '' : String(value))"/>
          <MfSelect v-else-if="field.type === 'SELECT'" class="w-full" :model-value="values[field.name]" :aria-label="field.name" :options="(field.options || []).map(value => ({ label: value, value }))"
                    @update:model-value="value => emit('change', field.name, String(value ?? ''))"/>
          <MfInput v-else :model-value="values[field.name]" :show-password="field.type === 'PASSWORD'" :type="field.type === 'PASSWORD' ? 'password' : undefined" :aria-label="field.name"
                   @update:model-value="value => emit('change', field.name, String(value ?? ''))"/>
        </div>
      </div>
    </section>
  </div>
</template>
