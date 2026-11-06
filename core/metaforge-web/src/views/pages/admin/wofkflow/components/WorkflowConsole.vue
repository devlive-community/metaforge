<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Activity, ChevronDown, ChevronUp, ExternalLink, FileText, Table2 } from '@lucide/vue'
import WorkflowService from '@/services/workflow'
import type { WorkflowCapabilities, WorkflowRun } from '@/model/workflow'
import WorkflowLog from './WorkflowLog.vue'
import ResultTable from '@/views/components/table/ResultTable.vue'
const props = defineProps<{ code?: string; capabilities?: WorkflowCapabilities }>()
const emit = defineEmits<{ state: [value: string] }>()
const { t } = useI18n(), tab = ref('logs'), collapsed = ref(false), height = ref(240), run = ref<WorkflowRun>()
const lines = ref<string[]>([]), loading = ref(false), failure = ref('')
const maximumHeight = ref(Math.floor(window.innerHeight * .55))
const onWindowResize = () => { maximumHeight.value = Math.floor(window.innerHeight * .55); height.value = Math.min(height.value, maximumHeight.value) }
window.addEventListener('resize', onWindowResize)
const headers = computed(() => [...new Set((run.value?.preview || []).flatMap(row => Object.keys(row)))])
let request = 0, timer: ReturnType<typeof setTimeout> | undefined, alive = true
async function refresh() {
  clearTimeout(timer)
  if (!props.code) { run.value = undefined; lines.value = []; return }
  const id = ++request, code = props.code; loading.value = true; failure.value = ''
  try {
    const records = await WorkflowService.runs(code)
    if (!alive || id !== request) return
    if (!records.status) throw Error(String(records.message))
    run.value = records.data?.[0]
    if (run.value) {
      const [details, logs] = await Promise.all([WorkflowService.getRun(code, run.value.id), WorkflowService.runLog(code, run.value.id)])
      if (!alive || id !== request) return
      if (!details.status) throw Error(String(details.message))
      run.value = details.data
      if (!alive || id !== request) return
      if (!logs.status) throw Error(String(logs.message))
      lines.value = logs.data || []
      emit('state', run.value.state)
    }
    else lines.value = []
  } catch (error) { if (alive && id === request) failure.value = error instanceof Error ? error.message : t('workflow.studio.loadFailed') }
  finally {
    if (alive && id === request) {
      loading.value = false
      if (run.value?.state === 'RUNNING') timer = setTimeout(() => { if (!document.hidden) refresh(); else scheduleHidden() }, 2500)
    }
  }
}
function scheduleHidden() { timer = setTimeout(() => { if (!document.hidden) refresh(); else scheduleHidden() }, 2500) }
let stopResize: (() => void) | undefined
function resize(event: PointerEvent) {
  event.preventDefault(); stopResize?.()
  const start = event.clientY, original = height.value
  const move = (next: PointerEvent) => { height.value = Math.max(120, Math.min(maximumHeight.value, original + start - next.clientY)) }
  stopResize = () => { window.removeEventListener('pointermove', move); window.removeEventListener('pointerup', end); window.removeEventListener('pointercancel', end); stopResize = undefined }
  const end = () => stopResize?.()
  window.addEventListener('pointermove', move); window.addEventListener('pointerup', end); window.addEventListener('pointercancel', end)
}
watch(() => props.code, () => { request++; run.value = undefined; lines.value = []; refresh() }, { immediate: true })
onBeforeUnmount(() => { window.removeEventListener('resize', onWindowResize); alive = false; request++; clearTimeout(timer); stopResize?.() })
defineExpose({ refresh, open: () => { collapsed.value = false } })
</script>
<template>
  <section class="relative flex min-h-0 shrink-0 flex-col border-t border-line bg-surface" :style="{ height: (collapsed ? 42 : height) + 'px' }">
    <div v-if="!collapsed" role="separator" tabindex="0" aria-orientation="horizontal" :aria-label="$t('workflow.studio.resizeConsole')" :aria-valuenow="Math.round(height)" aria-valuemin="120" :aria-valuemax="maximumHeight" class="absolute inset-x-0 -top-1 z-10 h-1.5 cursor-row-resize touch-none rounded-mf-md outline-none hover:bg-primary/20 focus-visible:ring-1 focus-visible:ring-inset focus-visible:ring-primary focus-visible:bg-primary/20" @pointerdown="resize" @keydown.up.prevent="height = Math.min(height + 20, maximumHeight)" @keydown.down.prevent="height = Math.max(height - 20, 120)"/>
    <header class="flex h-[42px] shrink-0 items-center justify-between gap-2 border-b border-line px-3"><div class="flex h-full gap-4"><button v-for="item in [{ key: 'logs', icon: FileText }, { key: 'output', icon: Table2 }, { key: 'metrics', icon: Activity }]" :key="item.key" type="button" class="flex items-center gap-1.5 border-b-2 px-1 text-xs font-medium" :class="tab === item.key ? 'border-primary text-primary' : 'border-transparent text-content-tertiary hover:text-content'" @click="tab = item.key; collapsed = false"><component :is="item.icon" :size="13"/>{{ $t('workflow.studio.' + item.key) }}</button></div><div class="flex items-center gap-2"><span v-if="run" class="hidden text-[10px] text-content-tertiary xl:block">{{ run.executor }} · {{ $t('dataTable.state' + run.state) }}</span><router-link v-if="code" :to="`/workflow/monitor/${code}`" class="text-content-tertiary hover:text-primary" :aria-label="$t('workflow.studio.runDetails')"><ExternalLink :size="14"/></router-link><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.collapseConsole')" @click="collapsed = !collapsed"><component :is="collapsed ? ChevronUp : ChevronDown" :size="14"/></MfButton></div></header>
    <div v-if="!collapsed" class="min-h-0 flex-1 overflow-hidden">
      <WorkflowLog v-if="tab === 'logs'" :lines="lines" :loading="loading" :error="failure" compact @refresh="refresh"/>
      <div v-else-if="tab === 'output'" class="flex h-full min-h-0 flex-col px-3 py-2"><template v-if="run?.preview"><p class="mb-2 shrink-0 text-[11px] text-content-tertiary">{{ $t('workflow.studio.outputHint') }}</p><ResultTable class="min-h-0 flex-1" fill :headers="headers" :rows="run.preview"/></template><MfEmpty v-else class="flex-1" size="sm" :title="$t('workflow.studio.noOutput')" :description="$t(capabilities && !capabilities.preview ? 'workflow.studio.outputUnsupported' : run?.state === 'RUNNING' ? 'workflow.studio.outputPending' : 'workflow.studio.outputUnavailable')"/></div>
      <div v-else class="h-full overflow-auto p-3"><template v-if="run?.metrics"><div class="grid grid-cols-3 gap-3"><div v-for="item in [{ key: 'read', value: run.metrics.rowsRead }, { key: 'written', value: run.metrics.rowsWritten }, { key: 'failedRows', value: run.metrics.rowsFailed }]" :key="item.key" class="rounded-mf-md bg-surface-hover px-3 py-2"><span class="text-xs text-content-tertiary">{{ $t('workflow.studio.' + item.key) }}</span><strong class="mt-1 block text-base text-content">{{ item.value }}</strong></div></div><div class="mt-3 flex flex-wrap gap-2"><MfTag v-for="stage in run.metrics.stages" :key="stage.name" :variant="stage.state === 'DONE' ? 'success' : 'default'">{{ stage.name }} · {{ stage.elapsedMs }}ms</MfTag></div></template><MfEmpty v-else class="h-full" size="sm" :title="$t('workflow.studio.noMetrics')" :description="$t(capabilities && !capabilities.metrics ? 'workflow.studio.metricsUnsupported' : 'workflow.studio.engineMetricsHint')"/></div>
    </div>
  </section>
</template>
