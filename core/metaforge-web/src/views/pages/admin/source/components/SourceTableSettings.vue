<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :title="$t('source.data.tableSettings')" @update:open="value => !value && emit('close')">
    <div class="flex flex-col gap-5 text-sm">
      <section class="flex flex-col gap-3">
        <h4 class="m-0 border-b border-line pb-2 font-semibold text-content">{{ $t('source.data.pagingSection') }}</h4>
        <div class="grid grid-cols-[auto_minmax(0,1fr)] items-center gap-x-3 gap-y-1">
          <span class="text-content-secondary">{{ $t('source.data.pageSizeLabel') }}</span>
          <MfSelect :model-value="draft.pageSize" size="sm" :options="PAGE_SIZES.map(value => ({ label: String(value), value }))" @update:model-value="value => draft.pageSize = Number(value)"/>
          <span/><span class="text-xs text-content-tertiary">{{ $t('source.data.pageSizeHint', { sizes: PAGE_SIZES.join(' / ') }) }}</span>
        </div>
        <label class="flex items-start gap-3">
          <MfSwitch v-model="draft.rememberPageSize"/>
          <span class="flex flex-col"><span class="text-content">{{ $t('source.data.rememberPageSize') }}</span><span class="text-xs text-content-tertiary">{{ $t('source.data.rememberPageSizeHint') }}</span></span>
        </label>
      </section>
      <section class="flex flex-col gap-3">
        <h4 class="m-0 border-b border-line pb-2 font-semibold text-content">{{ $t('source.data.displaySection') }}</h4>
        <label class="flex items-center gap-3"><MfSwitch v-model="draft.striped"/>{{ $t('source.data.showStripes') }}</label>
        <label class="flex items-center gap-3"><MfSwitch v-model="bordered"/>{{ $t('source.data.showVerticalBorders') }}</label>
        <label class="flex items-center gap-3"><MfSwitch v-model="draft.rowBorders"/>{{ $t('source.data.showRowBorders') }}</label>
        <label class="flex items-center gap-3"><MfSwitch v-model="draft.hoverable"/>{{ $t('source.data.showHover') }}</label>
        <label class="flex items-center gap-3"><MfSwitch v-model="draft.showActions"/>{{ $t('source.data.showActions') }}</label>
      </section>
      <section class="flex flex-col gap-3">
        <h4 class="m-0 border-b border-line pb-2 font-semibold text-content">{{ $t('source.data.dataSection') }}</h4>
        <div class="grid grid-cols-[auto_minmax(0,1fr)] items-center gap-3">
          <span class="text-content-secondary">{{ $t('source.data.defaultSort') }}</span>
          <MfSelect v-model="sortKey" size="sm" :options="sortOptions"/>
        </div>
      </section>
      <section class="flex flex-col gap-3">
        <h4 class="m-0 border-b border-line pb-2 font-semibold text-content">{{ $t('source.data.exportSection') }}</h4>
        <div class="grid grid-cols-[auto_minmax(0,1fr)] items-center gap-3">
          <span class="text-content-secondary">{{ $t('source.data.defaultExport') }}</span>
          <MfSelect v-model="draft.exportFormat" size="sm" :options="formatOptions"/>
        </div>
        <MfCheckbox v-model="draft.exportWithFilters">{{ $t('source.data.exportWithFilters') }}</MfCheckbox>
      </section>
    </div>
    <template #footer>
      <MfButton size="sm" @click="emit('close')">{{ $t('common.cancel') }}</MfButton>
      <MfButton size="sm" variant="primary" @click="save">{{ $t('common.save') }}</MfButton>
    </template>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { PAGE_SIZES, type SortRule, type TableSettings } from '@/model/source-data-view'

const props = defineProps<{
  open: boolean; settings: TableSettings; bordered: boolean; defaultSort: SortRule | null; quick: SortRule[]; width: number; container: HTMLElement | null
}>()
const emit = defineEmits<{ close: []; apply: [settings: TableSettings, bordered: boolean, sort: SortRule | null] }>()
const { t } = useI18n()
const ruleKey = (rule: SortRule | null) => rule ? `${rule.name}\u0000${rule.order}` : ''
// The panel edits copies and saves them together.
const draft = ref<TableSettings>({ ...props.settings })
const bordered = ref(props.bordered)
const sortKey = ref(ruleKey(props.defaultSort))
watch(() => props.open, open => {
  if (!open) return
  draft.value = { ...props.settings }
  bordered.value = props.bordered
  sortKey.value = ruleKey(props.defaultSort)
})
/** Quick sorts, plus the saved default when it is not one of them. */
const sortOptions = computed(() => {
  const rules = [...props.quick]
  if (props.defaultSort && !rules.some(rule => ruleKey(rule) === ruleKey(props.defaultSort))) rules.push(props.defaultSort)
  return [{ label: t('source.data.noDefaultSort'), value: '' },
    ...rules.map(rule => ({ label: t(rule.order === 'desc' ? 'source.data.quickDesc' : 'source.data.quickAsc', { name: rule.name }), value: ruleKey(rule) }))]
})
const formatOptions = [{ label: 'CSV (.csv)', value: 'csv' }, { label: 'JSON (.json)', value: 'json' }]
function save() {
  const [name, order] = sortKey.value.split('\u0000')
  emit('apply', { ...draft.value }, bordered.value, name ? { name, order: order as SortRule['order'] } : null)
}
</script>
