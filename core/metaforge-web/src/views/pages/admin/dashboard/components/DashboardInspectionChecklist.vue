<template>
  <MfEmpty v-if="!entries.length" class="h-full" size="sm" :title="$t('dashboard.studio.ops.checklistEmpty')"/>
  <div v-else class="flex h-full min-h-0 min-w-0 flex-col gap-2">
    <div class="flex shrink-0 flex-wrap items-center justify-between gap-1">
      <MfTag :variant="completed === entries.length ? 'success' : 'primary'">{{ $t('dashboard.studio.ops.completed', { completed, total: entries.length }) }}</MfTag>
      <MfButton size="sm" variant="ghost" :disabled="editable || !completed" @click="checked = []"><RotateCcw :size="14"/>{{ $t('common.reset') }}</MfButton>
    </div>
    <ul class="min-h-0 space-y-1.5 overflow-auto">
      <li v-for="entry in entries" :key="entry.key" class="rounded-mf-md border border-line px-2 py-1.5">
        <MfCheckbox :model-value="checked" :value="entry.key" :disabled="editable" :label="entry.label" @update:model-value="update"/>
        <p v-if="entry.text" class="ml-6 whitespace-pre-wrap break-words text-xs leading-5 text-content-secondary">{{ entry.text }}</p>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RotateCcw } from '@lucide/vue'
import type { DashboardWidget } from '@/model/dashboard-layout'

const props = defineProps<{ widget: DashboardWidget; editable?: boolean }>()
const entries = computed(() => (props.widget.links ?? []).map((entry, index) => ({ ...entry, key: entry.id + ':' + index })).filter(entry => entry.label.trim()))
const checked = ref<(string | number)[]>([])
const completed = computed(() => entries.value.filter(entry => checked.value.includes(entry.key)).length)
function update(value: boolean | (string | number)[]) { if (!props.editable && Array.isArray(value)) checked.value = value }
</script>
