<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Database } from '@lucide/vue'
import type { HomeQuery } from '@/composables/useHomeAnalytics'
import SqlInline from '@/views/components/sql/SqlInline.vue'
const props = defineProps<{ queries: HomeQuery[] }>()
const { t, te } = useI18n()
const columns = computed(() => [
  { key: 'content', title: t('homeDashboard.queryContent'), ellipsis: true },
  { key: 'source', title: t('common.source'), width: 130, ellipsis: true },
  { key: 'state', title: t('common.state'), width: 80 },
  { key: 'elapsed', title: t('homeDashboard.elapsed'), width: 80 },
])
function status(state?: string) {
  if (state === 'SUCCESS') return t('common.success')
  if (state === 'FAILURE' || state === 'FAILED') return t('common.failure')
  if (state === 'RUNNING') return t('homeDashboard.running')
  const key = 'dataTable.state' + state
  return te(key) ? t(key) : t('homeDashboard.unknown')
}
</script>
<template>
  <MfTable :columns="columns" :data-source="props.queries" row-key="code" density="compact" :bordered="false" :empty-text="$t('homeDashboard.noQueries')">
    <template #cell-content="{ record }"><SqlInline :code="record.content || '--'"/></template>
    <template #cell-source="{ record }"><div class="flex min-w-0 items-center gap-1.5"><Database :size="13" class="shrink-0 text-content-tertiary"/><MfTooltip :content="record.source?.name || $t('homeDashboard.unknownSource')" class="min-w-0 flex-1"><span class="block truncate text-xs">{{ record.source?.name || $t('homeDashboard.unknownSource') }}</span></MfTooltip></div></template>
    <template #cell-state="{ record }"><MfTag size="sm" :variant="record.state === 'SUCCESS' ? 'success' : record.state === 'RUNNING' ? 'warning' : ['FAILURE','FAILED'].includes(record.state) ? 'danger' : 'default'">{{ status(record.state) }}</MfTag></template>
    <template #cell-elapsed="{ record }"><span class="whitespace-nowrap text-xs tabular-nums">{{ typeof record.elapsed === 'number' ? record.elapsed + ' ms' : '--' }}</span></template>
  </MfTable>
</template>
