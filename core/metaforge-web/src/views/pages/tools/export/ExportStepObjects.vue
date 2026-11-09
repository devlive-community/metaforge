<template>
  <div class="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
    <section class="flex min-w-0 flex-col gap-4 rounded-mf-lg border border-line p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h3 class="m-0 flex items-center gap-2 text-base font-semibold text-content"><Table2 :size="17"/>{{ $t('export.objects.title') }}
          <MfTag size="sm" variant="success">{{ $t('export.objects.selected', { count: tables.length }) }}</MfTag></h3>
        <div class="flex shrink-0 gap-2">
          <MfButton :disabled="state.objects === 'database'" @click="selectAll(visible.map(item => item.name))">{{ $t('export.objects.selectAll') }}</MfButton>
          <MfButton :disabled="state.objects === 'database'" @click="clearSelection">{{ $t('export.objects.clear') }}</MfButton>
        </div>
      </div>
      <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
        <ExportChoice :icon="LayoutPanelTop" :title="$t('export.objects.tables')" :description="$t('export.objects.tablesHint', { database: context.database, count: objects.length })"
                      :selected="state.objects === 'tables'" @select="state.objects = 'tables'"/>
        <ExportChoice :icon="Database" :title="$t('export.objects.database')" :description="$t('export.objects.databaseHint')"
                      :selected="state.objects === 'database'" @select="state.objects = 'database'"/>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <MfInput v-model="keyword" class="w-72" :placeholder="$t('export.objects.search')"><template #prefix><Search :size="15"/></template></MfInput>
        <MfButton v-for="kind in (['table', 'view'] as const)" :key="kind" :variant="filter === kind ? 'primary' : 'outline'" @click="filter = filter === kind ? '' : kind">
          {{ $t(`export.objects.kind.${kind}`) }} ({{ objects.filter(item => item.type === kind).length }})
        </MfButton>
      </div>
      <div class="overflow-hidden rounded-mf-md border border-line">
        <MfTable :columns="columns" :data-source="visible" row-key="name" density="compact" :bordered="false" :loading="objectsLoading">
          <template #header-check>
            <MfCheckbox :model-value="allChecked" :indeterminate="!allChecked && visible.some(item => checked(item.name))" :disabled="state.objects === 'database'"
                        :aria-label="$t('export.objects.selectAll')" @update:model-value="value => value ? selectAll(visible.map(item => item.name)) : visible.forEach(item => toggle(item.name, false))"/>
          </template>
          <template #cell-check="{ record }">
            <MfCheckbox :model-value="checked(record.name)" :disabled="state.objects === 'database'" :aria-label="record.name" @update:model-value="value => toggle(record.name, !!value)"/>
          </template>
          <template #cell-name="{ record }"><span class="flex min-w-0 items-center gap-2 font-medium text-primary"><component :is="record.type === 'view' ? Eye : Table2" :size="14" class="shrink-0"/>
            <EllipsisCell :text="record.name"/></span></template>
          <template #cell-type="{ record }">{{ $t(`export.objects.type.${record.type}`) }}</template>
          <template #cell-rows="{ record }"><span class="tabular-nums">{{ record.rows === null ? '-' : record.rows.toLocaleString() }}</span></template>
          <template #cell-bytes="{ record }"><span class="tabular-nums">{{ formatBytes(record.bytes) }}</span></template>
          <template #cell-comment="{ record }"><EllipsisCell :text="record.comment || '-'"/></template>
        </MfTable>
      </div>
    </section>

    <aside class="flex flex-col gap-4 rounded-mf-lg border border-line p-5">
      <h3 class="m-0 text-base font-semibold text-content">{{ $t('export.objects.summary') }}</h3>
      <dl class="m-0 flex flex-col divide-y divide-line text-sm">
        <div v-for="row in summary" :key="row.key" class="flex items-center justify-between gap-3 py-2.5">
          <dt class="text-content-tertiary">{{ $t(`export.objects.summaryRow.${row.key}`) }}</dt><dd class="m-0 font-semibold text-content">{{ row.value }}</dd>
        </div>
      </dl>
      <div>
        <h4 class="m-0 mb-2 text-sm font-semibold text-content">{{ $t('export.objects.chosen') }}</h4>
        <p v-if="!tables.length" class="m-0 text-xs text-content-tertiary">{{ $t('export.objects.none') }}</p>
        <ul v-else class="m-0 flex max-h-72 list-none flex-col gap-1.5 overflow-y-auto p-0">
          <li v-for="name in tables" :key="name" class="flex min-w-0 items-center gap-2 text-sm text-content"><CheckSquare :size="15" class="shrink-0 text-primary"/><span class="truncate">{{ name }}</span></li>
        </ul>
      </div>
      <MfAlert variant="info" :title="$t('export.objects.note')"/>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckSquare, Database, Eye, LayoutPanelTop, Search, Table2 } from '@lucide/vue'
import EllipsisCell from '@/views/components/table/EllipsisCell.vue'
import ExportChoice from './ExportChoice.vue'
import { formatBytes } from '@/model/database-export'
import type { ExportWizard } from '@/composables/useExportWizard'

const props = defineProps<{ wizard: ExportWizard }>()
const { context, state, objects, objectsLoading, tables, format, toggle, selectAll, clearSelection } = props.wizard
const { t } = useI18n()
const keyword = ref('')
const filter = ref<'' | 'table' | 'view'>('')
const visible = computed(() => objects.value.filter(item => (!filter.value || item.type === filter.value)
    && (!keyword.value || item.name.toLowerCase().includes(keyword.value.trim().toLowerCase()))))
const checked = (name: string) => tables.value.includes(name)
const allChecked = computed(() => visible.value.length > 0 && visible.value.every(item => checked(item.name)))
const columns = computed(() => [
  { title: '', dataIndex: 'check', width: 44 },
  { title: t('export.objects.column.name'), dataIndex: 'name', ellipsis: true },
  { title: t('export.objects.column.type'), dataIndex: 'type', width: 110 },
  { title: t('export.objects.column.rows'), dataIndex: 'rows', width: 120 },
  { title: t('export.objects.column.size'), dataIndex: 'bytes', width: 120 },
  { title: t('export.objects.column.comment'), dataIndex: 'comment', width: 200, ellipsis: true },
])
const summary = computed(() => [
  { key: 'database', value: context.value.database },
  { key: 'available', value: t('export.objects.tablesUnit', { count: objects.value.length }) },
  { key: 'chosen', value: t('export.objects.tablesUnit', { count: tables.value.length }) },
  { key: 'format', value: format.value?.name || t('export.objects.formatLater') },
])
</script>
