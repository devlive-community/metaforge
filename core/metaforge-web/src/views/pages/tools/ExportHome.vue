<template>
  <div class="flex h-full min-h-0 flex-col overflow-hidden rounded-mf-lg bg-surface/95">
    <header class="flex shrink-0 flex-wrap items-center justify-between gap-4 border-b border-line px-6 py-4">
      <div class="flex min-w-0 items-center gap-4">
        <span class="inline-flex size-12 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary"><Download :size="24"/></span>
        <div class="min-w-0">
          <h1 class="m-0 text-xl font-semibold text-content">{{ $t('navigation.dataExport') }}</h1>

        </div>
      </div>
      <span v-if="context.database" class="shrink-0 whitespace-nowrap rounded-mf-md border border-line bg-surface-hover px-3 py-1.5 text-sm text-content-secondary">
        {{ $t('export.currentDatabase') }} <b class="text-content">{{ context.database }}</b> · {{ $t('export.tableCount', { count: tableCount }) }}
      </span>
    </header>

    <!-- Steps: done ones can be revisited. -->
    <nav class="flex shrink-0 items-center gap-3 overflow-x-auto border-b border-line px-6 py-4" :aria-label="$t('navigation.dataExport')">
      <template v-for="(key, index) in EXPORT_STEPS" :key="key">
        <button type="button" class="flex shrink-0 items-center gap-3 text-left disabled:cursor-default" :disabled="index >= step || running || !!job" @click="goTo(index)">
          <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold"
                :class="index < step ? 'bg-primary/15 text-primary' : index === step ? 'bg-primary text-on-primary' : 'bg-surface-hover text-content-tertiary'">
            <Check v-if="index < step" :size="16"/><template v-else>{{ index + 1 }}</template>
          </span>
          <span class="whitespace-nowrap text-sm font-semibold" :class="index <= step ? 'text-primary' : 'text-content-secondary'">{{ $t(`export.step.${key}`) }}</span>
        </button>
        <span v-if="index < EXPORT_STEPS.length - 1" class="h-px min-w-8 flex-1 bg-line"/>
      </template>
    </nav>

    <main class="min-h-0 flex-1 overflow-y-auto px-6 py-5">
      <MfEmpty v-if="!context.database" class="h-full" variant="initial" :title="$t('export.chooseDatabase')"/>
      <template v-else>
        <div class="mb-4 flex items-start justify-between gap-3">
          <div class="min-w-0">
            <h2 class="m-0 text-lg font-semibold text-content">{{ $t('export.stepTitle', { index: $t(`export.number.${step + 1}`), title: $t(`export.stepHeading.${EXPORT_STEPS[step]}`) }) }}</h2>
            <p class="m-0 mt-1 text-sm text-content-tertiary">{{ $t(`export.stepDescription.${EXPORT_STEPS[step]}`) }}</p>
          </div>
          <MfTag class="shrink-0" size="sm">{{ $t('export.databaseTag', { database: context.database }) }}</MfTag>
        </div>
        <ExportStepObjects v-if="step === 0" :wizard="wizard"/>
        <ExportStepContent v-else-if="step === 1" :wizard="wizard"/>
        <ExportStepFormat v-else-if="step === 2" :wizard="wizard"/>
        <ExportStepOutput v-else-if="step === 3" :wizard="wizard"/>
        <ExportStepPreview v-else :wizard="wizard"/>
      </template>
    </main>

    <footer class="flex shrink-0 items-center justify-between gap-3 border-t border-line px-6 py-3">
      <div class="flex min-w-0 items-center gap-4">
        <MfButton :disabled="running" @click="back">{{ $t('export.cancel') }}</MfButton>
        <span class="truncate text-xs text-content-tertiary">{{ $t('export.current', { database: context.database || '-', count: tables.length }) }}</span>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <MfButton v-if="step > 0 && !job" @click="previous"><ArrowLeft :size="15"/>{{ $t('export.previous') }}</MfButton>
        <MfButton v-if="step < EXPORT_STEPS.length - 1" variant="primary" :disabled="!canNext" @click="next">{{ $t('export.next') }}<ArrowRight :size="15"/></MfButton>
        <MfButton v-else-if="!job" variant="primary" :loading="starting" :disabled="!canStart" @click="start"><Download :size="15"/>{{ $t('export.start') }}</MfButton>
        <template v-else>
          <MfButton :disabled="running || !job.file" @click="download"><Download :size="15"/>{{ $t('export.download') }}</MfButton>
          <MfButton variant="primary" :disabled="running" @click="back">{{ $t('export.finish') }}</MfButton>
        </template>
      </div>
    </footer>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { ArrowLeft, ArrowRight, Check, Download } from '@lucide/vue'
import { EXPORT_STEPS, useExportWizard } from '@/composables/useExportWizard'
import ExportStepObjects from './export/ExportStepObjects.vue'
import ExportStepContent from './export/ExportStepContent.vue'
import ExportStepFormat from './export/ExportStepFormat.vue'
import ExportStepOutput from './export/ExportStepOutput.vue'
import ExportStepPreview from './export/ExportStepPreview.vue'

defineOptions({ name: 'ExportHome' })
const wizard = useExportWizard()
const { context, step, objects, tables, job, running, starting, canNext, canStart, next, previous, goTo, back, start, download } = wizard
const tableCount = computed(() => objects.value.filter(object => object.type === 'table').length)
</script>
