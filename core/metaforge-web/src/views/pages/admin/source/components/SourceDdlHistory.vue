<template>
  <MfModal :open="open" :title="$t('source.ddl.history.allTitle', { table })" width="760px" @update:open="value => !value && emit('close')">
    <div class="flex flex-col gap-3">
      <div class="flex max-h-[45vh] min-h-40 flex-col overflow-hidden rounded-mf-md border border-line">
        <MfTable fill :columns="columns" :data-source="entries" row-key="id" density="compact" :bordered="false" :loading="loading" :empty-text="$t('source.ddl.history.empty')"
                 :row-class="(entry: DdlHistoryEntry) => entry.id === detail?.id ? 'bg-primary/5 [&>td]:bg-primary/5' : ''">
          <template #cell-createTime="{ record }"><span class="tabular-nums">{{ record.createTime }}</span></template>
          <template #cell-kind="{ record }">{{ $t(`source.ddl.history.kind.${record.kind}`) }}</template>
          <template #cell-username="{ record }"><EllipsisCell :text="record.username"/></template>
          <template #cell-__action__="{ record }">
            <button type="button" class="text-xs text-primary hover:underline" @click="emit('detail', record)">{{ $t(record.id === detail?.id ? 'source.ddl.history.hide' : 'source.idx.view') }}</button>
          </template>
        </MfTable>
      </div>
      <div v-if="total > pageSize" class="flex justify-end">
        <MfPagination :current="page" :page-size="pageSize" :total="total" @change="(current: number) => emit('page', current)"/>
      </div>
      <SqlCodeBlock v-if="detail" :code="detail.content" :placeholder="$t(`source.ddl.empty.${detail.kind}`)" max-height="max-h-64"/>
    </div>
    <template #footer><MfButton variant="primary" @click="emit('close')">{{ $t('source.executed.close') }}</MfButton></template>
  </MfModal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import SqlCodeBlock from '@/views/components/sql/SqlCodeBlock.vue'
import type { DdlHistoryEntry } from '@/model/source-table-ddl'

defineProps<{
  open: boolean; table: string; entries: DdlHistoryEntry[]; loading: boolean; page: number; pageSize: number; total: number; detail: DdlHistoryEntry | null
}>()
const emit = defineEmits<{ close: []; page: [page: number]; detail: [entry: DdlHistoryEntry] }>()
const { t } = useI18n()
const columns = computed(() => [
  { title: t('source.ddl.history.time'), dataIndex: 'createTime' },
  { title: t('source.ddl.history.operation'), dataIndex: 'kind' },
  { title: t('source.ddl.history.userColumn'), dataIndex: 'username', width: 160, ellipsis: true },
  { title: t('common.action'), dataIndex: '__action__', fixed: 'right' as const },
])
</script>
