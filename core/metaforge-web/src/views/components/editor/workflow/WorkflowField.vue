<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SourceService from '@/services/source'
import { FilterModel } from '@/model/filter'
import type { ConfigureItem } from './types'
/**
 * Renders one declared executor field (node parameter or job setting) by its type, so every
 * executor plugin gets a consistent form without editor changes.
 */
const props = defineProps<{ field: ConfigureItem; modelValue: unknown; disabled?: boolean; rows?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()
const { t, te } = useI18n()

const mapText = (value: unknown) => typeof value === 'string' ? value : value && typeof value === 'object' ? Object.entries(value).map(([key, val]) => `${key}=${val}`).join('\n') : ''
const parseMap = (value: string) => Object.fromEntries(value.split('\n').filter(line => line.includes('=')).map(line => [line.slice(0, line.indexOf('=')).trim(), line.slice(line.indexOf('=') + 1).trim()]).filter(([key]) => key))
/** Valid text becomes a map; anything else stays text so validation can point at it. */
function mapValue(text: string) {
  const lines = text.split('\n').filter(line => line.trim())
  const keys = lines.map(line => line.slice(0, line.indexOf('=')).trim())
  return lines.every(line => line.indexOf('=') > 0 && line.slice(line.indexOf('=') + 1).trim()) && new Set(keys).size === keys.length ? parseMap(text) : text
}
const mapDraft = ref<string>()
watch(() => props.modelValue, value => { if (mapDraft.value !== undefined && JSON.stringify(mapValue(mapDraft.value)) !== JSON.stringify(value)) mapDraft.value = mapText(value) })
function updateMap(text: string) { mapDraft.value = text; emit('update:modelValue', mapValue(text)) }
const listValue = (value: unknown) => Array.isArray(value) ? value.map(String) : typeof value === 'string' ? value.split('\n').filter(Boolean) : []
/** Empty numbers stay empty so optional settings fall back to the executor configuration. */
const numberValue = computed(() => typeof props.modelValue === 'number' ? props.modelValue : props.modelValue === undefined || props.modelValue === null || props.modelValue === '' ? undefined : Number(props.modelValue))
const description = computed(() => { const key = 'workflow.studio.fieldDescription.' + props.field.field; return te(key) ? t(key) : props.field.description })

const sources = ref<{ label: string; value: string }[]>([]), sourcesBusy = ref(false), sourcesFailed = ref(false), sourceSearch = ref(''), sourcePage = ref(1), sourceTotal = ref(0)
let sourceTicket = 0
async function loadSources(reset = true) {
  const ticket = ++sourceTicket
  sourcesBusy.value = true; sourcesFailed.value = false
  const page = reset ? 1 : sourcePage.value + 1
  try {
    const response = await SourceService.getAll(Object.assign(new FilterModel(), { page, size: 20, keyword: sourceSearch.value }))
    if (ticket !== sourceTicket) return
    if (!response.status) throw Error(String(response.message))
    const options = (response.data?.content || []).map((row: any) => ({ label: `${row.name} · ${row.type}`, value: row.code }))
    sources.value = reset ? options : [...sources.value, ...options]; sourcePage.value = page; sourceTotal.value = response.data?.total || 0
  } catch { if (ticket === sourceTicket) sourcesFailed.value = true }
  finally { if (ticket === sourceTicket) sourcesBusy.value = false }
}
watch(() => props.field.type, type => { if (type === 'source' && !sources.value.length) loadSources() }, { immediate: true })
</script>
<template>
  <div class="min-w-0 w-full">
    <div v-if="field.type === 'source'" class="space-y-2">
      <div class="flex gap-2"><MfInput v-model="sourceSearch" clearable :placeholder="$t('workflow.studio.searchSources')" @enter="loadSources()"/><MfButton size="sm" class="shrink-0" :loading="sourcesBusy" @click="loadSources()">{{ $t('common.search') }}</MfButton></div>
      <MfSelect :model-value="modelValue" :disabled="disabled" class="w-full" :options="sources" :placeholder="$t('workflow.studio.selectSource')" @update:model-value="emit('update:modelValue', $event)"/>
      <p v-if="modelValue && !sources.some(source => source.value === modelValue)" class="break-all text-xs text-content-tertiary">{{ modelValue }}</p>
      <MfButton v-if="sources.length < sourceTotal" size="sm" :loading="sourcesBusy" @click="loadSources(false)">{{ $t('workflow.studio.loadMore') }}</MfButton>
      <MfAlert v-if="sourcesFailed" variant="danger" :title="$t('workflow.studio.sourcesFailed')"/>
    </div>
    <MfInputNumber v-else-if="field.type === 'number'" :model-value="numberValue" :disabled="disabled" decimal-only commit-on-change class="w-full" :step="field.step || 1" :min="field.min" :max="field.max" @update:model-value="emit('update:modelValue', $event)"/>
    <MfSwitch v-else-if="field.type === 'switch'" :model-value="modelValue === true" :disabled="disabled" @update:model-value="emit('update:modelValue', $event)"/>
    <MfSelect v-else-if="field.type === 'select'" :model-value="modelValue" :disabled="disabled" class="w-full" :options="field.options || []" @update:model-value="emit('update:modelValue', $event)"/>
    <MfSelect v-else-if="field.type === 'array'" :model-value="listValue(modelValue)" :disabled="disabled" tags searchable class="w-full" :options="listValue(modelValue).map(value => ({ label: value, value }))" @update:model-value="emit('update:modelValue', $event)"/>
    <MfTextarea v-else-if="field.type === 'map'" :model-value="mapDraft ?? mapText(modelValue)" :disabled="disabled" :rows="6" placeholder="source_column=target_column" @update:model-value="updateMap"/>
    <MfTextarea v-else-if="field.type === 'textarea'" :model-value="modelValue as string" :disabled="disabled" :rows="rows || 4" class="font-mono text-xs" :placeholder="field.placeholder" @update:model-value="emit('update:modelValue', $event)"/>
    <MfInput v-else :model-value="modelValue as string" :disabled="disabled" :type="field.type === 'password' ? 'password' : 'text'" :show-password="field.type === 'password'" :placeholder="field.placeholder" @update:model-value="emit('update:modelValue', $event)"/>
    <p v-if="description" class="mt-1.5 text-[11px] leading-5 text-content-tertiary">{{ description }}</p>
  </div>
</template>
