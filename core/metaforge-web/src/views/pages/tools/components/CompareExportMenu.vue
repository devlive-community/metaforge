<template>
  <MfDropdown v-if="!disabled && !loading && formats.length" trigger="click" placement="topRight">
    <MfButton variant="primary" :loading="loading" :disabled="disabled"><Download :size="16"/>{{ label }}<ChevronDown :size="14"/></MfButton>
    <template #overlay>
      <MfMenu>
        <MfMenuItem v-if="formats.includes('sql')" @click="emit('export', 'sql')">
          <span class="flex flex-col py-0.5"><span class="flex items-center gap-2"><FileCode :size="15"/>{{ $t('compare.exportSql') }}</span>
            <span class="pl-[23px] text-xs text-content-tertiary">{{ $t('compare.exportSqlHint') }}</span></span>
        </MfMenuItem>
        <MfMenuItem v-if="formats.includes('csv')" @click="emit('export', 'csv')">
          <span class="flex flex-col py-0.5"><span class="flex items-center gap-2"><FileSpreadsheet :size="15"/>{{ $t('compare.exportCsv') }}</span>
            <span class="pl-[23px] text-xs text-content-tertiary">{{ $t('compare.exportCsvHint') }}</span></span>
        </MfMenuItem>
      </MfMenu>
    </template>
  </MfDropdown>
  <MfButton v-else variant="primary" :loading="loading" disabled><Download :size="16"/>{{ label }}<ChevronDown :size="14"/></MfButton>
</template>

<script setup lang="ts">
import { ChevronDown, Download, FileCode, FileSpreadsheet } from '@lucide/vue'

/** Export button offering the sync statements or the CSV report of a comparison, as far as the source supports them. */
withDefaults(defineProps<{ label: string; formats?: readonly ('sql' | 'csv')[]; disabled?: boolean; loading?: boolean }>(), { formats: () => ['sql', 'csv'] })
const emit = defineEmits<{ export: [format: 'sql' | 'csv'] }>()
</script>
