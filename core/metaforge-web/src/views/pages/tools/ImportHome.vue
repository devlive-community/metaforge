<template>
  <div class="flex h-full min-h-0 flex-col overflow-hidden rounded-mf-lg bg-surface/95">
    <header class="flex shrink-0 flex-wrap items-center justify-between gap-4 border-b border-line px-6 py-4">
      <div class="flex min-w-0 items-center gap-4">
        <span class="inline-flex size-12 shrink-0 items-center justify-center rounded-mf-lg bg-primary/10 text-primary"><ArrowUpFromLine :size="24"/></span>
        <div class="min-w-0">
          <h1 class="m-0 text-xl font-semibold text-content">{{ $t('navigation.dataImport') }}</h1>
          <p class="m-0 mt-0.5 text-sm text-content-tertiary">{{ $t('import.subtitle') }}</p>
        </div>
      </div>
      <div class="flex shrink-0 items-center gap-3 text-sm">
        <span class="text-content-secondary">{{ $t('import.targetDatabase') }}</span>
        <span class="inline-flex items-center gap-1.5 rounded-mf-md border border-line px-3 py-1.5 font-semibold text-content"><Database :size="15"/>{{ context.database || '-' }}</span>
        <MfTag v-if="context.database" size="sm" variant="success"><span class="mr-1 inline-block size-1.5 rounded-full bg-success"/>{{ $t('import.connected') }}</MfTag>
      </div>
    </header>

    <!-- Steps: done ones can be revisited. -->
    <nav class="flex shrink-0 items-center gap-3 overflow-x-auto border-b border-line px-6 py-4" :aria-label="$t('navigation.dataImport')">
      <template v-for="(key, index) in IMPORT_STEPS" :key="key">
        <button type="button" class="flex shrink-0 items-center gap-3 text-left disabled:cursor-default" :disabled="index >= step || running" @click="goTo(index)">
          <span class="inline-flex size-8 shrink-0 items-center justify-center rounded-full text-sm font-semibold"
                :class="index < step ? 'bg-primary/15 text-primary' : index === step ? 'bg-primary text-on-primary' : 'bg-surface-hover text-content-tertiary'">
            <Check v-if="index < step" :size="16"/><template v-else>{{ index + 1 }}</template>
          </span>
          <span class="flex flex-col">
            <span class="whitespace-nowrap text-sm font-semibold" :class="index <= step ? 'text-primary' : 'text-content-secondary'">{{ $t(`import.step.${key}`) }}</span>
            <span class="whitespace-nowrap text-xs text-content-tertiary">{{ $t(`import.stepHint.${key}`) }}</span>
          </span>
        </button>
        <span v-if="index < IMPORT_STEPS.length - 1" class="h-px min-w-8 flex-1 bg-line"/>
      </template>
    </nav>

    <main class="min-h-0 flex-1 overflow-y-auto px-6 py-5">
      <MfEmpty v-if="!context.database" class="h-full" variant="initial" :title="$t('import.chooseDatabase')"/>
      <template v-else>
        <h2 class="m-0 text-lg font-semibold text-content">{{ $t('import.stepTitle', { index: step + 1, title: $t(`import.stepHeading.${IMPORT_STEPS[step]}`) }) }}</h2>
        <p class="m-0 mb-4 mt-1 text-sm text-content-tertiary">{{ $t(`import.stepDescription.${IMPORT_STEPS[step]}`) }}</p>
        <ImportStepFile v-if="step === 0" :wizard="wizard"/>
        <ImportStepTables v-else-if="step === 1" :wizard="wizard"/>
        <ImportStepMapping v-else-if="step === 2" :wizard="wizard"/>
        <ImportStepSettings v-else-if="step === 3" :wizard="wizard"/>
        <ImportStepPreview v-else :wizard="wizard"/>
      </template>
    </main>

    <footer class="flex shrink-0 items-center justify-between gap-3 border-t border-line px-6 py-3">
      <div class="flex min-w-0 items-center gap-4">
        <MfButton :disabled="running" @click="back">{{ $t('import.cancel') }}</MfButton>
        <span class="truncate text-xs text-content-tertiary">{{ $t('import.current', { database: context.database || '-', type: analysis?.file.dialect || sourceType || '-' }) }}</span>
      </div>
      <div class="flex shrink-0 items-center gap-2">
        <MfButton v-if="step > 0" :disabled="running || !!job" @click="previous"><ArrowLeft :size="15"/>{{ $t('import.previous') }}</MfButton>
        <MfButton v-if="step < IMPORT_STEPS.length - 1" variant="primary" :disabled="!canNext" @click="next">{{ $t('import.next') }}<ArrowRight :size="15"/></MfButton>
        <MfButton v-else-if="!job" variant="primary" :loading="starting" :disabled="!canStart" @click="start"><Play :size="15"/>{{ $t('import.start') }}</MfButton>
        <MfButton v-else variant="primary" :disabled="running" @click="back">{{ $t('import.finish') }}</MfButton>
      </div>
    </footer>
  </div>
</template>

<script lang="ts" setup>
import { ArrowLeft, ArrowRight, ArrowUpFromLine, Check, Database, Play } from '@lucide/vue'
import { IMPORT_STEPS, useImportWizard } from '@/composables/useImportWizard'
import ImportStepFile from './import/ImportStepFile.vue'
import ImportStepTables from './import/ImportStepTables.vue'
import ImportStepMapping from './import/ImportStepMapping.vue'
import ImportStepSettings from './import/ImportStepSettings.vue'
import ImportStepPreview from './import/ImportStepPreview.vue'

defineOptions({ name: 'ImportHome' })
const wizard = useImportWizard()
const { context, step, analysis, sourceType, job, running, starting, canNext, canStart, next, previous, goTo, back, start } = wizard
</script>
