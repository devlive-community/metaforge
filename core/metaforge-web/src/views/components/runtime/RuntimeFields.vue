<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { CircleHelp } from '@lucide/vue'
import { groupRuntimeFields, type RuntimeField } from '@/model/runtime-fields'
import DashboardInspectorGroup from '@/views/pages/admin/dashboard/components/DashboardInspectorGroup.vue'
/**
 * Runtime fields share the property inspector's collapsible cards. Slots stay mounted so folding
 * a group keeps its values; ungrouped fields remain available as basic settings.
 */
const props = defineProps<{ fields: RuntimeField[]; values: Record<string, string>; collapsible?: boolean }>()
const emit = defineEmits<{ (e: 'change', name: string, value: string): void }>()
const { t, te } = useI18n()

const groups = computed(() => groupRuntimeFields(props.fields))
// Known settings have translated labels; a plugin's own settings show as it declares them.
const label = (field: RuntimeField) => te('workflow.studio.setting.' + field.name) ? t('workflow.studio.setting.' + field.name) : field.name
const hint = (field: RuntimeField) => te('workflow.studio.fieldDescription.' + field.name) ? t('workflow.studio.fieldDescription.' + field.name) : field.description
const groupLabel = (group: string | null) => group === null ? t('settings.runtime.group.ungrouped')
  : te('settings.runtime.group.' + group) ? t('settings.runtime.group.' + group) : group
const groupIcon = (group: string | null) => group === 'parallel' ? 'layout' : group === 'throughput' ? 'refresh' : group === 'fault' ? 'actions' : 'info'
const numberOf = (value?: string) => value === undefined || value === '' || Number.isNaN(Number(value)) ? undefined : Number(value)
</script>
<template>
  <div class="flex flex-col gap-4">
    <component :is="collapsible ? DashboardInspectorGroup : 'section'" v-for="(item, index) in groups" :key="item.group ?? ''" class="min-w-0"
               v-bind="collapsible ? { title: groupLabel(item.group), icon: groupIcon(item.group), defaultOpen: index === 0 || item.group === null } : {}">
      <h3 v-if="!collapsible && item.group" class="mb-2 text-sm font-semibold text-content">{{ groupLabel(item.group) }}</h3>
      <MfFormGrid>
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
      </MfFormGrid>
    </component>
  </div>
</template>
