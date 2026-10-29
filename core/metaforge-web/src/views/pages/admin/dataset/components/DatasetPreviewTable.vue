<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ headers: string[], rows: Record<string, unknown>[], loading?: boolean, emptyText?: string }>()
const columns = computed(() => props.headers.map((name, index) => ({ key: `field_${index}`, dataIndex: `field_${index}`, title: name })))
const display = (record: Record<string, unknown>, key: string) => {
  const value = record[key]
  return value === null || value === undefined ? '' : String(value)
}
</script>

<template>
  <MfTable :columns="columns" :data-source="rows" :loading="loading" :empty-text="emptyText">
    <template v-for="col in columns" :key="col.key" #[`cell-${col.key}`]="{ record }">
      <MfTooltip :content="display(record, col.dataIndex)">
        <span class="block max-w-56 truncate">{{ display(record, col.dataIndex) }}</span>
      </MfTooltip>
    </template>
  </MfTable>
</template>
