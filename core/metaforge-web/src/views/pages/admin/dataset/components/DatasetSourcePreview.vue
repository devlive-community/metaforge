<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import DatasetPreviewTable from './DatasetPreviewTable.vue'
withDefaults(defineProps<{ headers: string[], rows: Record<string, unknown>[], current: boolean, running?: boolean, count?: number }>(), { running: false, count: 0 })
const { t } = useI18n()
</script>
<template>
  <section class="min-w-0 space-y-3">
    <div class="flex items-center justify-between gap-3"><h3 class="text-sm font-semibold text-content">{{ t('dataset.common.dataPreview') }}</h3><MfTag v-if="current" variant="primary" size="sm">{{ t('dataset.prototype.sampleRows', [Math.min(rows.length, 5)]) }}</MfTag></div>
    <div class="min-h-44 overflow-hidden rounded-mf-lg border border-line">
      <div v-if="running" class="flex min-h-44 items-center justify-center"><MfSpinner :label="t('dataset.editor.runPreview')"/></div>
      <MfEmpty v-else-if="!current" variant="initial" size="sm" :title="t('dataset.prototype.waitPreview')" :description="t('dataset.prototype.waitPreviewHint')"/>
      <DatasetPreviewTable v-else :headers="headers" :rows="rows.slice(0, 5)" :empty-text="t('dataset.editor.emptyPreview')"/>
    </div>
  </section>
</template>
