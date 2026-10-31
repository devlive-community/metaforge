<template>
  <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
    <section class="flex flex-col gap-5 rounded-mf-lg border border-line p-5">
      <div v-for="group in CHOICES" :key="group.key" class="flex flex-col gap-2.5" :class="group.key === 'conflict' && 'border-t border-line pt-5'">
        <h3 class="m-0 text-base font-semibold text-content">{{ $t(`import.settings.${group.key}`) }}</h3>
        <button v-for="value in group.values" :key="value" type="button" class="flex items-start gap-3 rounded-mf-md border px-4 py-3 text-left transition-colors"
                :class="settings[group.key] === value ? 'border-primary/40 bg-primary/10' : 'border-line hover:bg-surface-hover'" :aria-pressed="settings[group.key] === value"
                @click="(settings as any)[group.key] = value">
          <span class="mt-0.5 inline-flex size-4 shrink-0 items-center justify-center rounded-full border-2" :class="settings[group.key] === value ? 'border-primary' : 'border-line-strong'">
            <span v-if="settings[group.key] === value" class="size-2 rounded-full bg-primary"/>
          </span>
          <span class="flex flex-col">
            <span class="text-sm font-semibold text-content">{{ $t(`import.settings.${group.key}Option.${value}`) }}</span>
            <span class="text-xs text-content-tertiary">{{ $t(`import.settings.${group.key}Hint.${value}`) }}</span>
          </span>
        </button>
      </div>
    </section>

    <section class="flex flex-col gap-5 rounded-mf-lg border border-line p-5">
      <div class="flex flex-col gap-3">
        <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.settings.performance') }}</h3>
        <div class="grid grid-cols-2 gap-3 text-sm">
          <label class="flex flex-col gap-1.5"><span class="text-content-secondary">{{ $t('import.settings.batchSize') }}</span>
            <MfInputNumber v-model="settings.batchSize" :min="1" :max="100000" :precision="0"/></label>
          <label class="flex flex-col gap-1.5"><span class="text-content-secondary">{{ $t('import.settings.parallel') }}</span>
            <MfInputNumber v-model="settings.parallel" :min="1" :max="4" :precision="0"/></label>
          <div class="flex flex-col gap-1.5"><span class="text-content-secondary">{{ $t('import.settings.timeFormat') }}</span>
            <MfSelect v-model="settings.timeFormat" :options="TIME_FORMATS.map(value => ({ label: value, value }))"/></div>
          <div class="flex flex-col gap-1.5"><span class="text-content-secondary">{{ $t('import.settings.nullMode') }}</span>
            <MfSelect v-model="settings.nullMode" :options="(['keep', 'emptyToNull', 'nullToEmpty'] as const).map(value => ({ label: $t(`import.settings.null.${value}`), value }))"/></div>
        </div>
      </div>
      <div class="flex flex-col gap-2.5 border-t border-line pt-5">
        <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.settings.policy') }}</h3>
        <MfCheckbox v-for="key in POLICIES" :key="key" v-model="settings[key]">{{ $t(`import.settings.${key}`) }}</MfCheckbox>
      </div>
      <MfAlert variant="warning" :title="$t('import.settings.ddlWarning')"/>
      <div class="flex flex-col gap-3 border-t border-line pt-5">
        <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.settings.overrides') }}</h3>
        <div class="flex flex-col overflow-hidden rounded-mf-md border border-line">
          <MfTable :columns="columns" :data-source="selectedPlans" row-key="source" density="compact" :bordered="false">
            <template #cell-target="{ record }"><EllipsisCell :text="record.target"/></template>
            <template #cell-writeMode="{ record }">
              <MfSelect class="w-40" size="sm" :model-value="record.writeMode" :options="modeOptions(record.action)" @update:model-value="value => (record.writeMode = String(value))"/>
            </template>
            <template #cell-conflict="{ record }">
              <MfSelect class="w-36" size="sm" :model-value="record.conflict" :options="conflictOptions" @update:model-value="value => (record.conflict = String(value))"/>
            </template>
          </MfTable>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import type { ImportWizard } from '@/composables/useImportWizard'

const props = defineProps<{ wizard: ImportWizard }>()
const { settings, selectedPlans } = props.wizard
const { t } = useI18n()
const CHOICES = [
  { key: 'writeMode', values: ['append', 'truncate', 'structure'] },
  { key: 'conflict', values: ['skip', 'upsert', 'error'] },
] as const
const POLICIES = ['checkBefore', 'transactional', 'logSkipped', 'continueOnError'] as const
const TIME_FORMATS = ['yyyy-MM-dd HH:mm:ss', 'yyyy/MM/dd HH:mm:ss', 'yyyy-MM-dd\'T\'HH:mm:ss', 'dd/MM/yyyy HH:mm:ss', 'MM/dd/yyyy HH:mm:ss']
const columns = computed(() => [
  { title: t('import.tables.target'), dataIndex: 'target', width: 220, ellipsis: true },
  { title: t('import.settings.mode'), dataIndex: 'writeMode' },
  { title: t('import.settings.conflictColumn'), dataIndex: 'conflict' },
])
// An empty choice follows the settings above; a new table is always created first.
const modeOptions = (action: string) => [
  { label: t(action === 'create' ? 'import.settings.inheritCreate' : 'import.settings.inherit'), value: '' },
  ...(['append', 'truncate', 'structure'] as const).map(value => ({ label: t(`import.settings.writeModeOption.${value}`), value })),
]
const conflictOptions = computed(() => [
  { label: t('import.settings.inherit'), value: '' },
  ...(['skip', 'upsert', 'error'] as const).map(value => ({ label: t(`import.settings.conflictOption.${value}`), value })),
])
</script>
