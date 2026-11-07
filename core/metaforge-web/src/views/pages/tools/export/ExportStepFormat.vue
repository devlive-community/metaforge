<template>
  <div class="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_24rem]">
    <section class="flex min-w-0 flex-col gap-4 rounded-mf-lg border border-line p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h3 class="m-0 flex items-center gap-2 text-base font-semibold text-content">{{ $t('export.format.title') }}
          <MfTag size="sm" variant="success">{{ $t('export.format.count', { count: formats.length }) }}</MfTag></h3>
        <MfButton :loading="refreshing" @click="refresh"><RefreshCw :size="15"/>{{ $t('export.format.refresh') }}</MfButton>
      </div>
      <MfInput v-model="keyword" :placeholder="$t('export.format.search')"><template #prefix><Search :size="15"/></template></MfInput>
      <MfEmpty v-if="!visible.length" :title="$t('export.format.none')"/>
      <div v-else class="grid grid-cols-1 gap-3 md:grid-cols-2">
        <button v-for="item in visible" :key="item.name" type="button" class="flex min-w-0 items-center gap-3 rounded-mf-lg border p-4 text-left transition-colors"
                :class="item.name === state.format ? 'border-primary bg-primary/5' : 'border-line hover:bg-surface-hover'" :aria-pressed="item.name === state.format"
                @click="chooseFormat(item.name)">
          <span class="inline-flex size-11 shrink-0 items-center justify-center rounded-mf-md bg-info/10 text-sm font-bold uppercase text-info">{{ badge(item.extension) }}</span>
          <span class="flex min-w-0 flex-col">
            <span class="flex items-center gap-2 font-semibold text-content">{{ item.name }}<MfTag v-if="item.name === state.format" size="sm" variant="success">{{ $t('export.format.chosen') }}</MfTag></span>
            <span class="truncate text-xs text-content-tertiary">.{{ item.extension }} · {{ describe(item.extension) }}</span>
          </span>
        </button>
      </div>
      <div v-if="format" class="flex flex-col gap-4 border-t border-line pt-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div><h4 class="m-0 text-base font-semibold text-content">{{ $t('export.format.options', { name: format.name }) }}</h4>
            <p class="m-0 mt-0.5 text-xs text-content-tertiary">{{ $t('export.format.optionsHint') }}</p></div>
          <MfTag size="sm" variant="info">{{ $t('export.format.extension', { extension: format.extension }) }}</MfTag>
        </div>
        <p v-if="!format.options.length" class="m-0 text-sm text-content-tertiary">{{ $t('export.format.noOptions') }}</p>
        <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <label v-for="option in format.options" :key="option.name" class="flex min-w-0 flex-col gap-1.5 text-sm font-semibold text-content">{{ label(option.labels, locale) }}
            <MfSelect v-if="option.choices.length" v-model="state.options[option.name]" :options="option.choices.map(choice => ({ label: label(choice.labels, locale), value: choice.value }))"/>
            <MfInput v-else v-model="state.options[option.name]"/>
          </label>
        </div>
      </div>
    </section>

    <aside class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <div class="flex items-center justify-between gap-3">
        <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.format.preview') }}</h3>
        <MfTag size="sm" variant="success">{{ $t('export.format.live') }}</MfTag>
      </div>
      <div class="flex items-center justify-between gap-3">
        <span class="truncate font-semibold text-content">{{ sample?.file || '-' }}</span>
        <MfButton size="sm" :disabled="!sample?.content" @click="copy"><Code2 :size="14"/>{{ $t('export.format.copy') }}</MfButton>
      </div>
      <div class="relative">
        <div v-if="sampleLoading" class="absolute right-3 top-3"><MfSpinner/></div>
        <MfAlert v-if="sampleError" variant="danger" :title="sampleError"/>
        <pre v-else class="m-0 max-h-80 min-h-40 overflow-auto rounded-mf-md bg-[#17262a] p-4 font-mono text-xs leading-5 text-[#d6e7e2]">{{ readable }}</pre>
      </div>
      <dl class="m-0 flex flex-col divide-y divide-line text-sm">
        <div class="flex items-center justify-between gap-3 py-2.5"><dt class="text-content-tertiary">{{ $t('export.format.plugin') }}</dt><dd class="m-0 font-semibold text-content">{{ format?.name || '-' }}</dd></div>
        <div class="flex items-center justify-between gap-3 py-2.5"><dt class="text-content-tertiary">{{ $t('export.format.version') }}</dt><dd class="m-0 font-semibold text-content">{{ format?.version || '-' }}</dd></div>
        <div class="flex items-center justify-between gap-3 py-2.5"><dt class="text-content-tertiary">{{ $t('export.format.output') }}</dt><dd class="m-0 font-semibold text-content">{{ format ? `${ format.extension.toUpperCase() } (.${ format.extension })` : '-' }}</dd></div>
      </dl>
      <MfAlert variant="success" :title="$t('export.format.note')"/>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MfMessage } from 'metaforge-ui'
import { Code2, RefreshCw, Search } from '@lucide/vue'
import ExportService from '@/services/export'
import { label } from '@/model/database-export'
import { readablePreview } from '@/model/source-export'
import { ObjectUtils } from '@/utils/object'
import type { ExportWizard } from '@/composables/useExportWizard'

const props = defineProps<{ wizard: ExportWizard }>()
const { state, formats, format, sample, sampleLoading, sampleError, chooseFormat } = props.wizard
const { t, locale } = useI18n()
const keyword = ref('')
const refreshing = ref(false)
const visible = computed(() => formats.value.filter(item => !keyword.value || `${ item.name } ${ item.extension }`.toLowerCase().includes(keyword.value.trim().toLowerCase())))
const known = ['json', 'csv', 'txt', 'xml']
const badge = (extension: string) => extension.length <= 4 ? extension : extension.slice(0, 4)
const describe = (extension: string) => known.includes(extension) ? t(`export.format.kind.${ extension }`) : t('export.format.kind.other')
// JSON and XML samples are indented for reading only when the plugin wrote them compact.
const readable = computed(() => {
  const content = sample.value?.content || ''
  const kind = format.value?.extension === 'json' || format.value?.extension === 'xml' ? format.value.extension as 'json' | 'xml' : null
  return content.includes('\n') ? content : readablePreview(kind, content)
})
async function refresh() {
  refreshing.value = true
  const response = await ExportService.formats()
  refreshing.value = false
  if (!response.status) { MfMessage.error(response.message || t('common.requestError')); return }
  formats.value = response.data || []
  if (!formats.value.some(item => item.name === state.value.format)) chooseFormat(formats.value[0]?.name || '')
}
function copy() {
  ObjectUtils.copy(sample.value?.content || '')
  MfMessage.success(t('query.tip.copySuccess'))
}
</script>
