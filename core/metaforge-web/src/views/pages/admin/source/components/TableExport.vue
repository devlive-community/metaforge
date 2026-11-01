<template>
  <MfModal v-model:open="visible" width="1040px" height="min(780px, 92vh)" :title="$t('source.common.exportDataTable').replace('$VALUE', table)"
           :close-disabled="exporting" :close-on-overlay="!exporting" :close-on-esc="!exporting">
    <div v-if="loading" class="flex h-full min-h-80 items-center justify-center"><MfSpinner/></div>
    <MfEmpty v-else-if="!formats.length" class="h-full" :title="$t('source.export.noFormat')"/>
    <div v-else class="grid h-full min-h-0 grid-cols-1 gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <div class="flex min-w-0 flex-col gap-5 overflow-y-auto pr-1">
        <section>
          <h4 class="m-0 mb-2.5 text-base font-semibold text-content">{{ $t('source.export.format') }}</h4>
          <div class="grid grid-cols-5 gap-2">
            <button v-for="format in visibleFormats" :key="format.name" type="button" :aria-pressed="form.format === format.name"
                    class="flex flex-col items-center gap-1 rounded-mf-md border px-2 py-3 text-center transition-colors"
                    :class="form.format === format.name ? 'border-primary bg-primary/10' : 'border-line hover:bg-surface-hover'" :disabled="exporting" @click="form.format = format.name">
              <FormatLogo :format="format" :logo="logos[format.key]" :active="form.format === format.name"/>
              <span class="text-sm font-semibold text-content">{{ format.label }}</span>
              <span class="truncate text-xs" :class="form.format === format.name ? 'text-primary' : 'text-content-tertiary'">
                {{ format.known ? $t(`source.export.kind.${format.known}`) : $t('source.export.kind.other') }}
              </span>
            </button>
            <MfDropdown v-if="moreFormats.length" trigger="click" placement="bottomRight">
              <button type="button" class="flex h-full w-full flex-col items-center justify-center gap-1 rounded-mf-md border px-2 py-3 text-sm transition-colors"
                      :class="moreFormats.some(format => format.name === form.format) ? 'border-primary bg-primary/10 text-primary' : 'border-line text-content hover:bg-surface-hover'">
                <Ellipsis :size="18"/>
                <span class="inline-flex items-center gap-1">{{ moreFormats.find(format => format.name === form.format)?.label || $t('source.export.more') }}<ChevronRight :size="14"/></span>
              </button>
              <template #overlay>
                <MfMenu>
                  <MfMenuItem v-for="format in moreFormats" :key="format.name" @click="form.format = format.name">
                    <span class="flex items-center gap-2"><FormatLogo :format="format" :logo="logos[format.key]" small/>{{ format.label }}</span>
                  </MfMenuItem>
                </MfMenu>
              </template>
            </MfDropdown>
          </div>
        </section>

        <section>
          <h4 class="m-0 mb-2.5 text-base font-semibold text-content">{{ $t('source.export.scope') }}</h4>
          <MfRadioGroup v-model="form.scope" :disabled="exporting" class="flex gap-10">
            <MfRadio value="all">{{ $t('source.export.scopeAll') }}</MfRadio>
            <MfRadio value="range">{{ $t('source.export.scopeRange') }}</MfRadio>
          </MfRadioGroup>
        </section>

        <section class="flex flex-col gap-3">
          <div v-if="form.scope === 'range'">
            <h4 class="m-0 mb-2.5 text-base font-semibold text-content">{{ $t('source.export.start') }}</h4>
            <MfInputNumber v-model="form.start" :min="1" :precision="0" :disabled="exporting" class="w-56" :aria-label="$t('source.export.start')"/>
          </div>
          <div>
            <h4 class="m-0 mb-2.5 text-base font-semibold text-content">{{ $t('source.export.count') }}</h4>
            <div class="flex flex-wrap items-center gap-4">
              <div class="flex h-10 items-stretch overflow-hidden rounded-mf-md border border-line" :class="countDisabled && 'opacity-60'">
                <button type="button" class="w-11 text-content hover:bg-surface-hover disabled:cursor-not-allowed" :disabled="countDisabled || form.count <= 1"
                        :aria-label="$t('source.export.less')" @click="step(-STEP)"><Minus :size="16" class="mx-auto"/></button>
                <input v-model.number="countInput" type="text" inputmode="numeric" class="w-36 border-x border-line bg-transparent text-center text-sm tabular-nums text-content outline-none"
                       :disabled="countDisabled" :aria-label="$t('source.export.count')"/>
                <button type="button" class="w-11 text-content hover:bg-surface-hover disabled:cursor-not-allowed" :disabled="countDisabled"
                        :aria-label="$t('source.export.add')" @click="step(STEP)"><Plus :size="16" class="mx-auto"/></button>
              </div>
              <span class="flex items-center gap-1.5 text-sm text-content-secondary">
                {{ total === null ? $t('source.export.totalUnknown') : $t('source.export.total', { count: total.toLocaleString() }) }}
                <MfTooltip :content="$t('source.export.totalHint')"><CircleHelp :size="15" class="text-content-tertiary"/></MfTooltip>
              </span>
            </div>
          </div>
        </section>

        <section>
          <h4 class="m-0 mb-2.5 text-base font-semibold text-content">{{ $t('source.export.options') }}</h4>
          <div class="flex flex-col gap-2.5">
            <MfCheckbox v-model="form.includeStructure" :disabled="exporting">{{ $t('source.export.includeStructure') }}</MfCheckbox>
            <MfCheckbox v-model="form.compress" :disabled="exporting">{{ $t('source.export.compress') }}</MfCheckbox>
            <MfCheckbox v-model="form.excludeNull" :disabled="exporting">{{ $t('source.export.excludeNull') }}</MfCheckbox>
          </div>
        </section>

        <section>
          <h4 class="m-0 mb-2.5 text-base font-semibold text-content">{{ $t('source.export.download') }}</h4>
          <div class="flex items-center gap-3">
            <span class="shrink-0 text-sm text-content-secondary">{{ $t('source.export.path') }}</span>
            <MfInput :model-value="directory?.name || $t('source.export.browserDefault')" readonly class="min-w-0 flex-1" :aria-label="$t('source.export.path')"/>
            <MfTooltip v-if="canPickDirectory" :content="$t('source.export.pick')">
              <MfButton :disabled="exporting" :aria-label="$t('source.export.pick')" @click="pickDirectory"><FolderOpen :size="16"/></MfButton>
            </MfTooltip>
          </div>
        </section>
      </div>

      <section class="flex min-h-0 min-w-0 flex-col overflow-hidden rounded-mf-md border border-line">
        <div class="flex shrink-0 items-center justify-between gap-2 border-b border-line pr-3">
          <MfTabs v-model:active-key="panel" class="export-tabs">
            <MfTabPane item-key="preview" :tab="$t('source.export.preview')"/>
            <MfTabPane item-key="about" :tab="$t('source.export.aboutTab')"/>
          </MfTabs>
          <span v-if="panel === 'preview'" class="shrink-0 rounded-mf-sm bg-surface-hover px-2 py-1 text-xs text-content-secondary">{{ $t('source.export.previewRows') }}</span>
        </div>
        <div v-if="panel === 'preview'" class="relative min-h-0 flex-1 overflow-auto bg-surface-hover/40 p-3">
          <div v-if="previewLoading && !preview" class="flex h-full items-center justify-center"><MfSpinner/></div>
          <p v-else-if="previewError" class="m-0 text-sm text-danger">{{ previewError }}</p>
          <p v-else-if="!preview" class="m-0 text-sm text-content-tertiary">{{ $t('source.export.emptyPreview') }}</p>
          <div v-else class="font-mono text-[13px] leading-6" :class="previewLoading && 'opacity-60'">
            <div v-for="(line, number) in previewLines" :key="number" class="flex">
              <span class="w-9 shrink-0 select-none pr-3 text-right tabular-nums text-content-tertiary" aria-hidden="true">{{ number + 1 }}</span>
              <pre class="m-0 min-w-0 flex-1 whitespace-pre-wrap break-all text-content"><span v-for="(token, index) in line" :key="index" :class="TOKEN_CLASS[token.kind]">{{ token.text }}</span></pre>
            </div>
          </div>
        </div>
        <div v-else class="min-h-0 flex-1 overflow-auto p-4 text-sm">
          <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ selected?.label }}</h4>
          <ul class="m-0 flex list-disc flex-col gap-1.5 pl-5 text-content-secondary">
            <template v-if="selected?.known"><li v-for="index in 3" :key="index">{{ $t(`source.export.about.${selected.known}${index}`) }}</li></template>
            <li v-else>{{ $t('source.export.about.other', { name: selected?.name }) }}</li>
            <li>{{ $t('source.export.about.zip') }}</li>
          </ul>
        </div>
      </section>
    </div>

    <template #footer>
      <div class="flex w-full items-center justify-between">
        <MfButton :disabled="exporting" @click="visible = false">{{ $t('common.cancel') }}</MfButton>
        <MfButton variant="primary" :loading="exporting" :disabled="!canExport" @click="submit"><Download :size="16"/>{{ $t('source.export.submit') }}</MfButton>
      </div>
    </template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChevronRight, CircleHelp, Download, Ellipsis, FolderOpen, Minus, Plus } from '@lucide/vue'
import FormatLogo from './ExportFormatLogo.vue'
import type { PreviewTokenKind } from '@/model/source-export'
import { useTableExport } from '@/composables/useTableExport'

defineOptions({ name: 'TableExport' })

const props = withDefaults(defineProps<{ isVisible?: boolean }>(), { isVisible: false })
const emit = defineEmits<{ (e: 'close', value: boolean): void }>()

const visible = computed({
  get: () => props.isVisible,
  set: (value: boolean) => emit('close', value)
})
const STEP = 1000
const TOKEN_CLASS: Record<PreviewTokenKind, string> = { key: 'text-danger', string: 'text-success', number: 'text-info', literal: 'text-warning', tag: 'text-info', plain: '' }
const panel = ref<'preview' | 'about'>('preview')
const { loading, formats, visibleFormats, moreFormats, selected, logos, form, total, previewLines, preview, previewError, previewLoading, exporting,
  directory, canPickDirectory, canExport, table, step, pickDirectory, submit } = useTableExport(() => { visible.value = false })
// "All rows" exports the whole table, so the count shows the total and cannot be edited.
const countDisabled = computed(() => exporting.value || form.value.scope === 'all')
const countInput = computed({
  get: () => (countDisabled.value && form.value.scope === 'all' && total.value !== null ? total.value : form.value.count).toLocaleString(),
  set: (value: string | number) => {
    const number = Number(String(value).replace(/[^\d]/g, ''))
    if (Number.isFinite(number) && number >= 1) form.value.count = number
  },
})
</script>

<style scoped>
.export-tabs :deep([role='tablist']) { border-bottom: 0; }
</style>
