<template>
  <MfForm ref="form" :model="formModel" class="grid gap-3" @focusout="validateLinks">
    <div v-for="(link, index) in modelValue" :key="link.id + index" class="grid gap-2 rounded-mf-md border border-line p-2">
      <div class="flex items-center gap-2"><span class="flex-1 text-xs text-content-tertiary">{{ index + 1 }}</span><MfButton variant="ghost" size="sm" :aria-label="$t('common.delete')" @click="remove(index)"><Trash2 :size="13"/></MfButton></div>
      <MfFormItem :name="`links.${index}.label`" :label="$t(mode === 'links' ? 'dashboard.studio.linkLabel' : 'dashboard.studio.itemTitle')" :rules="[{ required: true, message: $t(mode === 'links' ? 'dashboard.studio.linkLabelRequired' : 'dashboard.studio.itemTitleRequired') }]">
        <MfInput :model-value="link.label" :maxlength="100" @update:model-value="update(index, 'label', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="mode === 'gallery'" :name="`links.${index}.image`" :label="$t('dashboard.studio.imageUrl')" :rules="[{ validator: value => !!dashboardResourceUrl(String(value || '')), message: $t('dashboard.studio.linkUrlRequired') }]">
        <MfInput :model-value="link.image ?? ''" placeholder="https://" @update:model-value="update(index, 'image', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="mode !== 'links'" :name="`links.${index}.text`" :label="$t('dashboard.studio.itemText')">
        <MfTextarea :model-value="link.text ?? ''" :rows="3" :maxlength="5000" @update:model-value="update(index, 'text', $event)"/>
      </MfFormItem>
      <MfFormItem v-if="mode === 'links'" :name="`links.${index}.url`" :label="$t('dashboard.studio.linkUrl')" :rules="[{ validator: value => !!dashboardResourceUrl(String(value || '')), message: $t('dashboard.studio.linkUrlRequired') }]">
        <MfInput :model-value="link.url" placeholder="https://" @update:model-value="update(index, 'url', $event)"/>
      </MfFormItem>
    </div>
    <MfButton size="sm" :disabled="modelValue.length >= 20" @click="add"><Plus :size="14"/>{{ $t(mode === 'links' ? 'dashboard.studio.addLink' : 'dashboard.studio.addContentItem') }}</MfButton>
  </MfForm>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { v4 as uuid } from 'uuid'
import { Plus, Trash2 } from '@lucide/vue'
import { dashboardResourceUrl, type DashboardLinkItem } from '@/model/dashboard-layout'
const props = withDefaults(defineProps<{ modelValue: DashboardLinkItem[]; mode?: 'links' | 'gallery' | 'entries' }>(), { mode: 'links' })
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
function remove(index: number) { emit('update:modelValue', props.modelValue.filter((_, position) => position !== index)) }
function add() { emit('update:modelValue', [...props.modelValue, { id: uuid(), label: '', url: '' }]) }
</script>
