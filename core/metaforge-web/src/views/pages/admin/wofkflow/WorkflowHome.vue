<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { CircleAlert, CircleCheck, Clock3, FilePlus2, GitBranch, MoreHorizontal, Pencil, Play, Plus, Square, Trash2 } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import WorkflowService from '@/services/workflow'
import { workflowStateVariant, type WorkflowExecutor, type WorkflowModel } from '@/model/workflow'
import { usePagedList } from '@/composables/usePagedList'
import DataTable from '@/views/components/table/DataTable.vue'
import { useTableFilters } from '@/views/components/table/tableFilters'
import WorkflowShell from './components/WorkflowShell.vue'
const { t } = useI18n(), router = useRouter()
const stats = ref<Record<string, number>>({}), statsFailed = ref(false), action = ref(''), deleting = ref<WorkflowModel>(), templates = ref(false), catalog = ref<WorkflowExecutor[]>([]), catalogLoading = ref(false), catalogFailure = ref('')
const listFailure = ref('')
const filters = useTableFilters('workflow')
const { loading, data, dataCount, pageIndex, pageSize, initialize, onSearch, onPageChange, onSizeChange } = usePagedList<WorkflowModel>(async filter => {
  try { return await WorkflowService.getAll(filter) }
  catch (error) { return { status: false, code: 500, message: error instanceof Error ? error.message : t('workflow.studio.loadFailed') } }
}, { orders: [{ column: 'updateTime', order: 'DESC' }], onFailure: message => { listFailure.value = message }, onData: () => { listFailure.value = '' } })
const tableRows = computed(() => data.value.map(row => ({ ...row, description: row.configure?.description })))
const columns = computed(() => [
  { title: t('common.name'), key: 'name', dataIndex: 'name', width: 260 },
  { title: t('workflow.studio.description'), key: 'description', width: 260 },
  { title: t('common.executor'), key: 'executor', dataIndex: 'executor', width: 130 },
  { title: t('common.state'), key: 'state', dataIndex: 'state', width: 120 },
  { title: t('common.updateTime'), key: 'updateTime', dataIndex: 'updateTime', width: 170 },
  { title: t('common.elapsed'), key: 'elapsed', dataIndex: 'elapsed', width: 120 },
  { title: t('common.action'), key: 'action', width: 180, fixed: 'right' as const },
])
async function refresh() {
  initialize()
  try { const response = await WorkflowService.statistics(); if (!response.status) throw Error(); stats.value = response.data || {}; statsFailed.value = false }
  catch { statsFailed.value = true }
}
async function run(row: WorkflowModel) {
  if (action.value || !row.code) return
  action.value = row.code
  try { const response = await WorkflowService.restart(row.code); if (!response.status) throw Error(String(response.message)); await router.push(`/workflow/monitor/${row.code}?run=${response.data}`) }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.runFailed')) }
  finally { action.value = '' }
}
async function stop(row: WorkflowModel) {
  if (action.value || !row.code) return
  action.value = row.code
  try { const response = await WorkflowService.stop(row.code); if (!response.status) throw Error(String(response.message)); refresh() }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.stopFailed')) }
  finally { action.value = '' }
}
async function remove() {
  if (!deleting.value?.code || action.value) return
  action.value = deleting.value.code
  try { const response = await WorkflowService.deleteByCode(deleting.value.code); if (!response.status) throw Error(String(response.message)); deleting.value = undefined; refresh() }
  catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.deleteFailed')) }
  finally { action.value = '' }
}
async function openTemplates() {
  templates.value = true; catalogLoading.value = true; catalogFailure.value = ''
  try { const response = await WorkflowService.catalog(); if (!response.status) throw Error(String(response.message)); catalog.value = response.data?.executors || [] }
  catch (error) { catalogFailure.value = error instanceof Error ? error.message : t('workflow.studio.loadFailed') }
  finally { catalogLoading.value = false }
}
const stateText = (state?: string) => t(`dataTable.state${state || 'CREATED'}`)
let timer: ReturnType<typeof setTimeout>
function schedule() { timer = setTimeout(() => { if (data.value.some(row => row.state === 'RUNNING') && !document.hidden) refresh(); schedule() }, 8000) }
onBeforeUnmount(() => clearTimeout(timer)); refresh(); schedule()
</script>
<template>
  <WorkflowShell>
    <div class="min-h-0 flex-1 overflow-y-auto p-4 md:p-6">
      <header class="mb-5 flex items-center justify-between gap-4"><div><h1 class="text-xl font-semibold text-content">{{ $t('common.workflow') }}</h1><p class="mt-2 text-xs text-content-tertiary">{{ $t('workflow.studio.listHint') }}</p></div><div class="flex gap-2"><MfButton @click="openTemplates"><FilePlus2 :size="15"/>{{ $t('workflow.studio.templates') }}</MfButton><router-link to="/workflow/info"><MfButton variant="primary"><Plus :size="15"/>{{ $t('workflow.text.create') }}</MfButton></router-link></div></header>
      <div class="mb-5 grid grid-cols-2 gap-4 xl:grid-cols-4"><div v-for="(icon, key) in { total: GitBranch, running: Play, success: CircleCheck, failure: CircleAlert }" :key="key" class="flex items-center gap-4 rounded-mf-lg border border-line bg-surface p-5 shadow-mf-sm"><span class="rounded-mf-lg p-3" :class="key === 'failure' ? 'bg-danger-light text-danger' : 'bg-primary-light text-primary'"><component :is="icon" :size="24"/></span><div><div class="text-xs text-content-tertiary">{{ $t('workflow.studio.stat.' + key) }}</div><div class="mt-1 text-2xl font-semibold text-content">{{ statsFailed ? '—' : stats[key] ?? '—' }}</div></div></div></div>
      <div class="mb-4 flex h-11 items-center border-b border-line"><span class="h-full border-b-2 border-primary px-1 py-2 text-sm font-semibold text-primary">{{ $t('workflow.studio.all') }} <span class="ml-1 text-xs">({{ dataCount }})</span></span></div>
      <MfAlert v-if="listFailure" class="mb-4" variant="danger" :title="listFailure"/>
      <DataTable filterable :filter-fields="filters" :columns="columns" :data-source="tableRows" :loading="loading" :page-index="pageIndex" :page-size="pageSize" :total="dataCount" @search="onSearch" @reset="onSearch" @refresh="refresh" @page-change="onPageChange" @size-change="onSizeChange">
        <template #actions><router-link to="/workflow/info"><MfButton variant="primary" size="sm"><Plus :size="14"/>{{ $t('workflow.text.create') }}</MfButton></router-link></template>
        <template #bodyCell="{ column, record: row }">
          <template v-if="column.key === 'name'"><router-link :to="`/workflow/info/${row.code}`" class="flex items-center gap-2 font-medium text-content hover:text-primary"><GitBranch :size="15" class="shrink-0 text-primary"/><span class="truncate">{{ row.name }}</span></router-link><div class="mt-1 pl-6 text-[11px] text-content-tertiary">{{ row.configure?.nodes?.length || 0 }} {{ $t('workflow.studio.nodes') }}</div></template>
          <template v-else-if="column.key === 'description'"><span class="block max-w-64 truncate text-xs text-content-secondary">{{ row.configure?.description || '—' }}</span></template>
          <template v-else-if="column.key === 'state'"><MfTag :variant="workflowStateVariant(row.state)">{{ stateText(row.state) }}</MfTag></template>
          <template v-else-if="column.key === 'elapsed'">{{ row.elapsed == null ? '—' : (row.elapsed / 1000).toFixed(1) + 's' }}</template>
          <template v-else-if="column.key === 'action'"><div class="flex items-center gap-2"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.design')" @click="router.push(`/workflow/info/${row.code}`)"><Pencil :size="14"/></MfButton><MfButton variant="ghost" size="sm" :loading="action === row.code" :aria-label="$t(row.state === 'RUNNING' ? 'workflow.studio.stop' : 'workflow.studio.run')" @click="row.state === 'RUNNING' ? stop(row) : run(row)"><component :is="row.state === 'RUNNING' ? Square : Play" :size="14"/></MfButton><MfDropdown trigger="click" placement="bottomRight"><MfButton variant="ghost" size="sm" :aria-label="$t('workflow.studio.more')"><MoreHorizontal :size="16"/></MfButton><template #overlay><MfMenu><MfMenuItem key_="runs" @click="router.push(`/workflow/monitor/${row.code}`)"><Clock3 :size="14"/>{{ $t('workflow.studio.runs') }}</MfMenuItem><MfMenuItem key_="delete" :disabled="row.state === 'RUNNING'" @click="deleting = row"><Trash2 :size="14"/>{{ $t('common.delete') }}</MfMenuItem></MfMenu></template></MfDropdown></div></template>
        </template>
      </DataTable>
      <div class="mt-5 flex items-center justify-between gap-4 rounded-mf-lg border border-primary/15 bg-primary-light/40 p-5"><div class="flex items-center gap-3"><FilePlus2 :size="24" class="text-primary"/><div><strong class="text-sm text-content">{{ $t('workflow.studio.templateHint') }}</strong><p class="mt-1 text-xs text-content-tertiary">{{ $t('workflow.studio.executorHint') }}</p></div></div><MfButton size="sm" @click="openTemplates">{{ $t('workflow.studio.templates') }}</MfButton></div>
    </div>
  </WorkflowShell>
  <MfModal :open="!!deleting" :title="$t('common.delete')" @close="deleting = undefined"><p class="text-sm text-content-secondary">{{ $t('workflow.studio.deleteHint', { name: deleting?.name }) }}</p><template #footer><MfButton @click="deleting = undefined">{{ $t('common.cancel') }}</MfButton><MfButton variant="danger" :loading="!!action" @click="remove">{{ $t('common.delete') }}</MfButton></template></MfModal>
  <MfModal v-model:open="templates" :title="$t('workflow.studio.templates')"><div v-if="catalogLoading" class="flex justify-center py-10"><MfSpinner/></div><MfAlert v-else-if="catalogFailure" variant="danger" :title="catalogFailure"/><div v-else class="space-y-3"><router-link v-for="executor in catalog" :key="executor.name" :to="{ path: '/workflow/info', query: { executor: executor.name, template: 'transfer' } }" class="flex items-center gap-3 rounded-mf-lg border border-line p-4 transition-colors hover:border-primary hover:bg-primary-light/30"><GitBranch :size="24" class="text-primary"/><div><strong class="text-sm">{{ executor.name }} · {{ $t('workflow.studio.transfer') }}</strong><p class="mt-1 text-xs text-content-tertiary">{{ $t('workflow.studio.templateDescription') }}</p></div></router-link><MfEmpty v-if="!catalog.length" :title="$t('workflow.studio.noExecutor')"/></div></MfModal>
</template>
