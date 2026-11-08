<template>
  <MfForm ref="form" :model="formModel" class="grid gap-3" @focusout="validateLinks">
    <div v-for="(link, index) in modelValue" :key="link.id + index" class="grid gap-2 rounded-mf-md border border-line p-2">
      <div class="flex items-center gap-2"><span class="flex-1 text-xs text-content-tertiary">{{ index + 1 }}</span><MfButton variant="ghost" size="sm" :aria-label="$t('common.delete')" @click="remove(index)"><Trash2 :size="13"/></MfButton></div>
      <MfFormItem :name="`links.${index}.label`" :label="$t(linkMode ? 'dashboard.studio.linkLabel' : 'dashboard.studio.itemTitle')" :rules="[{ required: true, message: $t(linkMode ? 'dashboard.studio.linkLabelRequired' : 'dashboard.studio.itemTitleRequired') }]">
        <MfInput :model-value="link.label" :maxlength="100" @update:model-value="update(index, 'label', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="mode === 'gallery' || mode === 'icons'" :name="`links.${index}.image`" :label="$t('dashboard.studio.imageUrl')" :rules="[{ validator: value => (mode === 'icons' && !value) || !!dashboardResourceUrl(String(value || '')), message: $t('dashboard.studio.linkUrlRequired') }]">
        <MfInput :model-value="link.image ?? ''" placeholder="https://" @update:model-value="update(index, 'image', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="mode !== 'links'" :name="`links.${index}.text`" :label="$t('dashboard.studio.itemText')">
        <MfTextarea :model-value="link.text ?? ''" :rows="3" :maxlength="5000" @update:model-value="update(index, 'text', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="mode === 'legend'" :name="`links.${index}.tone`" :label="$t('dashboard.studio.legendTone')">
        <MfSelect :model-value="link.tone ?? 'default'" :options="dashboardLegendTones.map(value => ({ value, label: $t('dashboard.studio.legendColor.' + value) }))"
                  @update:model-value="value => updateTone(index, value)"/>
      </MfFormItem>
      <MfFormItem v-if="linkMode" :name="`links.${index}.url`" :label="$t('dashboard.studio.linkUrl')" :rules="[{ validator: value => (mode === 'icons' && !value) || !!dashboardResourceUrl(String(value || '')), message: $t('dashboard.studio.linkUrlRequired') }]">
        <MfInput :model-value="link.url" placeholder="https://" @update:model-value="update(index, 'url', $event)"/>
      </MfFormItem>
    </div>
    <MfButton size="sm" :disabled="modelValue.length >= maxItems" @click="add"><Plus :size="14"/>{{ $t(linkMode ? 'dashboard.studio.addLink' : 'dashboard.studio.addContentItem') }}</MfButton>
  </MfForm>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { v4 as uuid } from 'uuid'
import { Plus, Trash2 } from '@lucide/vue'
import { dashboardLegendTones, dashboardResourceUrl, type DashboardLinkItem } from '@/model/dashboard-layout'
const props = withDefaults(defineProps<{ modelValue: DashboardLinkItem[]; mode?: 'links' | 'gallery' | 'entries' | 'icons' | 'legend'; maxItems?: number }>(), { mode: 'links', maxItems: 20 })
const linkMode = computed(() => props.mode === 'links' || props.mode === 'icons')
const emit = defineEmits<{ 'update:modelValue': [items: DashboardLinkItem[]] }>()
const form = ref<{ validate: () => Promise<unknown> }>()
const formModel = new Proxy({} as Record<string, unknown>, {
  get(_target, path) {
    const [, index, key] = String(path).split('.')
    return props.modelValue[Number(index)]?.[key as keyof DashboardLinkItem]
  },
})
async function validateLinks() {
  await nextTick()
  try { await form.value?.validate() }
  catch { /* Field errors are rendered by MfFormItem. */ }
}
function update(index: number, key: 'label' | 'url' | 'image' | 'text', value: string) {
  emit('update:modelValue', props.modelValue.map((item, position) => position === index ? { ...item, [key]: value } : item))
}
function updateTone(index: number, value: unknown) {
  const tone = dashboardLegendTones.find(item => item === value)
  if (tone) emit('update:modelValue', props.modelValue.map((item, position) => position === index ? { ...item, tone } : item))
}
function remove(index: number) { emit('update:modelValue', props.modelValue.filter((_, position) => position !== index)) }
function add() { emit('update:modelValue', [...props.modelValue, { id: uuid(), label: '', url: '' }]) }
</script>
