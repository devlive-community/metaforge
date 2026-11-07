<template>
  <div class="grid grid-cols-1 items-start gap-4 xl:grid-cols-2">
    <section class="flex min-w-0 flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.output.file') }}</h3>
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <label class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ $t('export.output.fileName') }}<MfInput v-model="state.fileName"/></label>
        <label class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ $t('export.output.extension') }}
          <MfSelect disabled :model-value="format?.extension || ''" :options="[{ label: $t('export.output.extensionValue', { extension: format?.extension || '-', plugin: format?.name || '-' }), value: format?.extension || '' }]"/></label>
      </div>
      <div class="flex flex-col gap-2.5">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.output.organize') }}</h4>
        <ExportChoice :icon="Archive" :title="$t('export.output.zip')" :description="$t(structureFile ? 'export.output.zipHint' : 'export.output.zipHintData', { count: tables.length, extension: (format?.extension || '').toUpperCase() })"
                      :selected="state.organize === 'zip'" @select="state.organize = 'zip'"/>
        <ExportChoice :icon="FileText" :title="$t('export.output.merge')" :description="$t(format?.merge ? 'export.output.mergeHint' : 'export.output.mergeUnsupported', { plugin: format?.name || '-' })"
                      :selected="state.organize === 'merge'" :disabled="!format?.merge" @select="state.organize = 'merge'"/>
      </div>
      <div class="flex flex-col gap-2.5 border-t border-line pt-4">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.output.download') }}</h4>
        <div class="flex items-start gap-3 rounded-mf-lg border border-primary bg-primary/5 p-4">
          <Download :size="18" class="mt-0.5 shrink-0 text-primary"/>
          <span class="flex min-w-0 flex-1 flex-col"><span class="font-semibold text-content">{{ $t('export.output.browser') }}</span>
            <span class="mt-0.5 text-xs text-content-tertiary">{{ $t('export.output.browserHint') }}</span></span>
          <CheckSquare :size="17" class="shrink-0 text-primary"/>
        </div>
      </div>
      <div class="flex items-center justify-between gap-3 border-t border-line pt-4">
        <span class="flex min-w-0 flex-col"><span class="text-sm font-semibold text-content">{{ $t('export.output.compress') }}</span>
          <span class="text-xs text-content-tertiary">{{ $t('export.output.compressHint', { file: output }) }}</span></span>
        <MfSwitch v-model="state.compress"/>
      </div>
    </section>

    <section class="flex min-w-0 flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.output.execution') }}</h3>
      <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
        <label class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ $t('export.output.batchSize') }}
          <MfInputNumber v-model="state.batchSize" class="w-full" :min="1" :max="100000" :precision="0" :disabled="state.reading === 'single'"/></label>
        <label class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ $t('export.output.concurrency') }}
          <MfSelect v-model="state.concurrency" :options="[1, 2, 3, 4].map(value => ({ label: $t(value === 1 ? 'export.output.serial' : 'export.output.parallel', { count: value }), value }))"/></label>
        <label class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ $t('export.output.reading') }}
          <MfSelect v-model="state.reading" :options="(['batch', 'single'] as const).map(value => ({ label: $t(`export.output.readingOption.${value}`), value }))"/></label>
        <label class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ $t('export.output.failure') }}
          <MfSelect v-model="state.failure" :options="(['stop', 'skip'] as const).map(value => ({ label: $t(`export.output.failureOption.${value}`), value }))"/></label>
      </div>
      <div class="flex flex-col gap-4 border-t border-line pt-4">
        <h4 class="m-0 text-sm font-semibold text-content">{{ $t('export.output.consistency') }}</h4>
        <div v-for="item in switches" :key="item.key" class="flex items-center justify-between gap-3">
          <span class="flex min-w-0 flex-col"><span class="text-sm font-semibold text-content">{{ $t(`export.output.switch.${item.key}`) }}</span>
            <span class="text-xs text-content-tertiary">{{ $t(`export.output.switchHint.${item.key}`) }}</span></span>
          <MfSwitch v-if="item.key === 'snapshot'" :model-value="false" disabled/>
          <MfSwitch v-else v-model="state[item.key]"/>
        </div>
      </div>
      <MfAlert variant="warning" :title="$t('export.output.note')"/>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Archive, CheckSquare, Download, FileText } from '@lucide/vue'
import ExportChoice from './ExportChoice.vue'
import type { ExportWizard } from '@/composables/useExportWizard'

const props = defineProps<{ wizard: ExportWizard }>()
const { state, format, tables, output } = props.wizard
const structureFile = computed(() => state.value.content === 'all' && (state.value.structure.create || state.value.structure.drop))
// Snapshots across tables need one connection the plugins do not share yet, so the switch stays off.
const switches = [{ key: 'snapshot' }, { key: 'autoDownload' }, { key: 'keepRecord' }] as const
</script>
