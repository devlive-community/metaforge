<template>
  <div class="mx-auto max-w-4xl">
    <div class="mb-5">
      <h1 class="m-0 text-xl font-semibold text-content">{{ $t('tools.export.title') }}</h1>
      <p class="mb-0 mt-1 text-sm text-content-secondary">{{ $t('tools.export.desc') }}</p>
    </div>

    <div class="rounded-mf-lg border border-line bg-surface p-5">
      <div class="grid grid-cols-3 gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.name') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="sourceCode" :options="sourceOptions"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.database') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="databaseName" :options="databaseOptions"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.tableName') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="tableName" :options="tableOptions"/>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('source.common.format') }} <span class="text-danger">*</span></label>
          <MfSelect v-model="format" :options="formatOptions"/>
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-content-secondary">{{ $t('tools.export.count') }}</label>
          <MfInput v-model="count" inputmode="numeric" :placeholder="$t('tools.export.countAll')"/>
        </div>
      </div>

      <div class="mt-5 flex items-center justify-end gap-3">
        <MfButton variant="primary" :loading="exporting" :disabled="!canExport" @click="onExport">
          <Download :size="14"/>
          {{ $t('tools.export.execute') }}
        </MfButton>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { Download } from '@lucide/vue'
import { useExportTool } from '@/composables/useExportTool'

defineOptions({ name: 'ExportHome' })

const { sourceCode, databaseName, tableName, sourceOptions, databaseOptions, tableOptions, format, formatOptions, count, exporting, canExport, onExport } = useExportTool()
</script>
