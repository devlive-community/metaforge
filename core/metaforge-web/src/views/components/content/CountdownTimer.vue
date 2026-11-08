<template>
  <MfEmpty v-if="deadlineTime === undefined" class="h-full" size="sm" :title="$t('dashboard.studio.chooseDeadline')"/>
  <div v-else class="flex h-full min-w-0 flex-col items-center justify-center gap-2 overflow-hidden [container-type:inline-size]">
    <div class="grid w-full grid-cols-4 gap-1 tabular-nums">
      <div v-for="part in parts" :key="part.unit" class="min-w-0 rounded-mf-md bg-surface-hover px-1 py-1.5 text-center">
        <strong class="block text-[clamp(12px,8cqw,24px)] leading-tight font-semibold">{{ String(part.value).padStart(2, '0') }}</strong>
        <span class="block truncate text-[10px] text-content-tertiary" :title="$t('dashboard.studio.duration.' + part.unit)">{{ $t('dashboard.studio.duration.' + part.unit) }}</span>
      </div>
    </div>
    <span class="max-w-full truncate text-[10px] text-content-secondary" :title="deadlineLabel">{{ remaining === 0 ? $t('dashboard.studio.deadlineReached') : deadlineLabel }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onScopeDispose, ref } from 'vue'
import { dashboardDeadlineTime, type DashboardDeadline } from '@/model/dashboard-layout'
const props = defineProps<{ deadline?: DashboardDeadline }>()
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => { timer = setInterval(() => { now.value = Date.now() }, 1000) })
onScopeDispose(() => clearInterval(timer))
const deadlineTime = computed(() => dashboardDeadlineTime(props.deadline))
const remaining = computed(() => Math.max(0, Math.ceil(((deadlineTime.value ?? now.value) - now.value) / 1000)))
const parts = computed(() => [
  { unit: 'days', value: Math.floor(remaining.value / 86400) },
  { unit: 'hours', value: Math.floor(remaining.value / 3600) % 24 },
  { unit: 'minutes', value: Math.floor(remaining.value / 60) % 60 },
  { unit: 'seconds', value: remaining.value % 60 },
])
const deadlineLabel = computed(() => props.deadline ? `${props.deadline.date} ${String(props.deadline.hour).padStart(2, '0')}:${String(props.deadline.minute).padStart(2, '0')}` : '')
</script>
