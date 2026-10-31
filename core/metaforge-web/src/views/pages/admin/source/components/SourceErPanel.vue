<template>
  <MfDrawer :open="open" inline :container="container || undefined" :width="width + 'px'" :closable="false">
    <MfTabs v-model:active-key="tab" class="-mt-2 mb-4">
      <MfTabPane item-key="info" :tab="$t('source.er.tableInfo')"/>
      <MfTabPane item-key="settings" :tab="$t('source.er.settings')"/>
    </MfTabs>

    <div v-if="tab === 'info'" class="flex flex-col gap-5">
      <section>
        <h4 class="m-0 mb-3 flex items-center gap-2.5 text-base font-semibold text-content"><Table2 :size="20" class="text-primary"/><span class="truncate">{{ table }}</span></h4>
        <dl class="m-0 grid grid-cols-[6.5rem_minmax(0,1fr)] gap-y-2.5 text-sm">
          <template v-for="item in facts" :key="item.label">
            <dt class="text-content-secondary">{{ item.label }}</dt><dd class="m-0 break-words text-content">{{ item.value }}</dd>
          </template>
        </dl>
      </section>
      <section class="border-t border-line pt-4">
        <h4 class="m-0 mb-3 text-base font-semibold text-content">{{ $t('source.er.legend') }}</h4>
        <ul class="m-0 flex list-none flex-col gap-3 p-0 text-sm text-content">
          <li class="flex items-center gap-3"><span class="inline-flex w-9 justify-center"><KeyRound :size="18" class="text-warning"/></span>{{ $t('source.er.legendPrimary') }}</li>
          <li class="flex items-center gap-3"><span class="inline-flex w-9 justify-center"><Link2 :size="18" class="text-info"/></span>{{ $t('source.er.legendForeign') }}</li>
          <li class="flex items-center gap-3"><span class="inline-flex w-9 justify-center rounded-mf-sm bg-success/10 py-1 text-xs font-medium text-success">UK</span>{{ $t('source.er.legendUnique') }}</li>
          <li class="flex items-center gap-3"><span class="inline-flex w-9 justify-center rounded-mf-sm bg-danger/10 py-1 text-xs font-medium text-danger">NN</span>{{ $t('source.er.legendNotNull') }}</li>
          <li class="flex items-center gap-3"><span class="inline-flex w-9 justify-center rounded-mf-sm bg-surface-hover py-1 text-xs font-medium text-content-secondary">DF</span>{{ $t('source.er.legendDefault') }}</li>
        </ul>
      </section>
    </div>

    <ul v-else class="m-0 flex list-none flex-col gap-1 p-0">
      <li v-for="item in SETTINGS" :key="item" class="flex items-center justify-between gap-3 rounded-mf-md px-1 py-2">
        <span class="flex min-w-0 flex-col">
          <span class="text-sm text-content">{{ $t(`source.er.setting.${item}`) }}</span>
          <span class="text-xs text-content-tertiary">{{ $t(`source.er.setting.${item}Hint`) }}</span>
        </span>
        <MfSwitch :model-value="settings[item]" size="sm" :aria-label="$t(`source.er.setting.${item}`)" @update:model-value="value => settings = { ...settings, [item]: !!value }"/>
      </li>
    </ul>
  </MfDrawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { KeyRound, Link2, Table2 } from '@lucide/vue'
import type { ErSettings } from '@/model/source-er'

const props = defineProps<{ open: boolean; container: HTMLElement | null; width: number; table: string; database: string; info: any | null }>()
const tab = defineModel<'info' | 'settings'>('tab', { required: true })
const settings = defineModel<ErSettings>('settings', { required: true })
const { t } = useI18n()
const SETTINGS: (keyof ErSettings)[] = ['types', 'tags', 'notNull', 'comments', 'grid']

const sizeMb = (info: any) => {
  const total = Number(info?.object_data_size || 0) + Number(info?.object_index_size || 0)
  return info?.object_data_size == null && info?.object_index_size == null ? '-' : `${Math.round(total * 100) / 100} MB`
}
const facts = computed(() => {
  const info = props.info
  const collation = String(info?.object_collation || '')
  const rows = Number(info?.object_rows)
  return [
    { label: t('source.truncate.database'), value: props.database || '-' },
    { label: t('source.truncate.engine'), value: info?.object_engine || '-' },
    { label: t('source.dropTable.rows'), value: info?.object_rows == null || !Number.isFinite(rows) ? '-' : rows.toLocaleString() },
    { label: t('source.dropTable.size'), value: info ? sizeMb(info) : '-' },
    { label: t('source.er.charset'), value: collation ? collation.split('_')[0] : '-' },
    { label: t('source.er.collation'), value: collation || '-' },
    { label: t('source.dropTable.comment'), value: info?.object_comment || '-' },
  ]
})
</script>
