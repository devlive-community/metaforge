<template>
  <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]">
    <section class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.file.source') }}</h3>
      <label class="flex cursor-pointer flex-col items-center justify-center gap-3 rounded-mf-lg border border-dashed px-6 py-12 text-center transition-colors"
             :class="dragging ? 'border-primary bg-primary/10' : 'border-primary/40 bg-primary/5 hover:bg-primary/10'"
             @dragover.prevent="dragging = true" @dragleave.prevent="dragging = false" @drop.prevent="onDrop">
        <ArrowUpFromLine :size="40" class="text-primary"/>
        <span class="text-lg font-semibold text-content">{{ $t('import.file.drop') }}</span>
        <span class="text-sm text-content-tertiary">{{ $t('import.file.accepts', { list: accepted }) }}</span>
        <span class="inline-flex items-center gap-1.5 rounded-mf-md bg-primary px-4 py-2 text-sm font-medium text-on-primary"><Plus :size="15"/>{{ $t('import.file.choose') }}</span>
        <input type="file" class="sr-only" :accept="acceptAttribute" @change="onPick">
      </label>
      <div v-if="file" class="flex items-center gap-4 rounded-mf-md border border-line px-4 py-3">
        <span class="inline-flex size-10 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-sm font-bold uppercase text-primary">{{ extension || '?' }}</span>
        <div class="min-w-0 flex-1">
          <p class="m-0 truncate font-semibold text-content">{{ file.name }}</p>
          <p class="m-0 text-xs text-content-tertiary">{{ [formatSize(file.size), analysis?.file.parser, analysis?.file.encoding].filter(Boolean).join(' · ') }}</p>
        </div>
        <span v-if="analyzing" class="inline-flex shrink-0 items-center gap-1.5 text-sm text-content-secondary"><MfLoadingIcon :size="15"/>{{ $t('import.file.reading') }}</span>
        <span v-else-if="analyzeError" class="shrink-0 text-sm text-danger">{{ $t('import.file.failed') }}</span>
        <span v-else-if="analysis" class="inline-flex shrink-0 items-center gap-1 text-sm text-success"><Check :size="15"/>{{ $t('import.file.read') }}</span>
        <button type="button" class="inline-flex size-7 shrink-0 items-center justify-center rounded-mf-md text-content-tertiary hover:bg-surface-hover hover:text-content"
                :aria-label="$t('import.file.remove')" :disabled="analyzing" @click="removeFile"><X :size="16"/></button>
      </div>
      <MfAlert v-if="analyzeError" variant="danger" :title="analyzeError"/>
      <MfAlert v-else variant="info" :title="$t('import.file.note')"/>
    </section>

    <section class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('import.file.analysis') }}</h3>
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-content-secondary">{{ $t('import.file.parser') }}</span>
        <MfSelect :model-value="analysis?.file.parser || ''" :options="parserOptions" :disabled="!file || analyzing || extension === 'zip'"
                  :placeholder="$t('import.file.parserAuto')" @update:model-value="value => reparse(String(value))"/>
      </div>
      <dl class="m-0 grid grid-cols-[6rem_minmax(0,1fr)] gap-y-2 text-sm">
        <dt class="text-content-secondary">{{ $t('import.file.type') }}</dt><dd class="m-0 text-content">{{ extension ? '.' + extension : '-' }}</dd>
        <dt class="text-content-secondary">{{ $t('import.file.encoding') }}</dt><dd class="m-0 text-content">{{ analysis?.file.encoding || '-' }}</dd>
        <dt class="text-content-secondary">{{ $t('import.file.dialect') }}</dt><dd class="m-0 text-content">{{ analysis?.file.dialect || '-' }}</dd>
        <dt class="text-content-secondary">{{ $t('import.file.objects') }}</dt>
        <dd class="m-0 text-content">{{ analysis ? $t('import.file.objectsValue', { tables: totals.tables, definitions: totals.definitions }) : '-' }}</dd>
      </dl>
      <div class="grid grid-cols-2 gap-3 border-t border-line pt-4">
        <div v-for="tile in tiles" :key="tile.key" class="rounded-mf-md border border-line px-4 py-3">
          <p class="m-0 text-xs text-content-secondary">{{ $t(`import.file.tile.${tile.key}`) }}</p>
          <p class="m-0 mt-1 text-2xl font-semibold tabular-nums" :class="tile.key === 'anomalies' && tile.value ? 'text-warning' : 'text-content'">{{ tile.value.toLocaleString() }}</p>
        </div>
      </div>
      <div class="border-t border-line pt-4">
        <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('import.file.found') }}</h4>
        <p v-if="!analysis" class="m-0 text-sm text-content-tertiary">{{ $t('import.file.none') }}</p>
        <ul v-else class="m-0 flex list-none flex-col p-0">
          <li v-for="table in analysis.tables" :key="table.name" class="flex items-center gap-3 border-b border-line py-2.5 text-sm last:border-b-0">
            <MfCheckbox :model-value="isSelected(table.name)" :aria-label="table.name" @update:model-value="value => select(table.name, !!value)"/>
            <span class="min-w-0 flex-1 truncate"><span class="font-semibold text-content">{{ table.name }}</span>
              <span class="text-content-tertiary"> · {{ $t('import.file.columns', { count: table.columns.length }) }}</span></span>
            <span class="shrink-0 tabular-nums text-content-secondary">{{ $t('import.rows', { count: table.rows.toLocaleString() }) }}</span>
            <MfTag size="sm" variant="info">{{ $t('import.file.recognised') }}</MfTag>
          </li>
        </ul>
        <ul v-if="analysis?.anomalies.length" class="m-0 mt-3 flex list-disc flex-col gap-1 pl-5 text-xs text-warning">
          <li v-for="item in analysis.anomalies" :key="item">{{ item }}</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ArrowUpFromLine, Check, Plus, X } from '@lucide/vue'
import { formatSize } from '@/model/table-import'
import type { ImportWizard } from '@/composables/useImportWizard'

const props = defineProps<{ wizard: ImportWizard }>()
const { parsers, file, analysis, analyzing, analyzeError, plans, totals, analyze, reparse, removeFile } = props.wizard
const dragging = ref(false)
const extension = computed(() => file.value?.name.includes('.') ? file.value.name.split('.').pop()!.toLowerCase() : '')
const accepted = computed(() => parsers.value.map(parser => '.' + parser.extension).join(' / '))
const acceptAttribute = computed(() => parsers.value.map(parser => '.' + parser.extension).join(','))
// Only formats reading this file's kind are offered; a ZIP is read entry by entry.
const parserOptions = computed(() => parsers.value.filter(parser => parser.extension !== 'zip').map(parser => ({ label: `${ parser.name } (.${ parser.extension })`, value: parser.name })))
const tiles = computed(() => [
  { key: 'tables', value: totals.value.tables }, { key: 'rows', value: totals.value.rows },
  { key: 'definitions', value: totals.value.definitions }, { key: 'anomalies', value: totals.value.anomalies },
])
const isSelected = (name: string) => plans.value.find(plan => plan.source === name)?.selected ?? false
function select(name: string, value: boolean) {
  const plan = plans.value.find(item => item.source === name)
  if (plan) plan.selected = value
}
function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const chosen = input.files?.[0]
  if (chosen) analyze(chosen)
  input.value = ''
}
function onDrop(event: DragEvent) {
  dragging.value = false
  const chosen = event.dataTransfer?.files?.[0]
  if (chosen) analyze(chosen)
}
</script>
