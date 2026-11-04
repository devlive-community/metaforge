<template>
  <div class="flex flex-col gap-5 pb-6">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-content">{{ $t('store.history.title') }}</h1>
        <p class="mt-2 text-[13px] text-content-secondary">{{ $t('store.history.description') }}</p>
      </div>
      <MfButton variant="secondary" @click="store.switchView('market')">
        <Store :size="15"/>
        {{ $t('store.history.browse') }}
      </MfButton>
    </div>

    <div class="overflow-hidden rounded-2xl border border-line bg-surface">
      <MfTable :columns="columns"
               :data-source="store.logs.value"
               row-key="id"
               :empty-text="$t('store.history.empty')"
               empty-variant="data">
        <template #cell-time="{ record }">{{ formatTime(record.time) }}</template>
        <template #cell-action="{ record }">{{ $t(record.action === 'check' ? 'store.history.check' : 'store.action.' + record.action) }}</template>
        <template #cell-status="{ record }">
          <MfTag :variant="record.success ? 'success' : 'danger'" size="sm">
            {{ $t(record.success ? 'store.history.success' : 'store.history.failure') }}
          </MfTag>
        </template>
      </MfTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Store } from '@lucide/vue'
import type { PluginStoreContext } from '@/composables/usePluginStore'
import type { MfTableColumn } from 'metaforge-ui'

defineOptions({ name: 'HistoryView' })

const props = defineProps<{ store: PluginStoreContext }>()
const store = props.store
const { t, locale } = useI18n()
const formatTime = (time: string) => new Date(time).toLocaleString(locale.value, { hour12: false })

const columns = computed<MfTableColumn[]>(() => [
  { title: t('store.column.time'), dataIndex: 'time', key: 'time', width: 180 },
  { title: t('store.column.actionType'), dataIndex: 'action', key: 'action', width: 140 },
  { title: t('store.column.plugin'), dataIndex: 'plugin', key: 'plugin', width: 200, ellipsis: true },
  { title: t('store.column.status'), dataIndex: 'success', key: 'status', width: 90 },
  { title: t('store.column.note'), dataIndex: 'note', key: 'note', ellipsis: true },
])
</script>
