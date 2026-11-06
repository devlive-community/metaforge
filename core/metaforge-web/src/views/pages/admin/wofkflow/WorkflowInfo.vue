<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ArrowUpRight, CheckCheck, Play, Save, Settings2, Square } from '@lucide/vue'
import { MfMessage } from 'metaforge-ui'
import WorkflowService from '@/services/workflow'
import type { WorkflowExecutor, WorkflowGraph, WorkflowModel } from '@/model/workflow'
import type { ConfigureItem } from '@/views/components/editor/workflow/types'
import { defaultValues, fieldIssues, mergeDefinitions, validateWorkflow } from '@/views/components/editor/workflow/utils'
import WorkflowEditor from '@/views/components/editor/workflow/WorkflowEditor.vue'
import WorkflowField from '@/views/components/editor/workflow/WorkflowField.vue'
import WorkflowShell from './components/WorkflowShell.vue'
import WorkflowConsole from './components/WorkflowConsole.vue'
const route = useRoute(), router = useRouter(), { t, te } = useI18n()
const consolePanel = ref<InstanceType<typeof WorkflowConsole>>()
const loading = ref(true), failure = ref(''), busy = ref(false), checking = ref(false), settings = ref(false)
const form = ref<any>(), editor = ref<InstanceType<typeof WorkflowEditor>>()
const catalog = ref<WorkflowExecutor[]>([]), record = ref<WorkflowModel>({ name: '', executor: '' })
const graph = ref<WorkflowGraph>({ nodes: [], connections: [], timeout: 600, description: '' })
const current = computed(() => catalog.value.find(item => item.name === record.value.executor))
const code = computed(() => record.value.code)
const issues = ref<{ scope: string; message: string }[]>([]), checked = ref(false)
const publishing = ref(false)
const original = ref(''), ticket = ref(0), drafts = new Map<string, WorkflowGraph>()
const snapshot = () => JSON.stringify({ name: record.value.name, executor: record.value.executor, graph: graph.value })
const dirty = computed(() => !!original.value && snapshot() !== original.value)
const expanded = computed(() => route.query.view === 'config')
async function initialize() {
  const request = ++ticket.value; loading.value = true; failure.value = ''; drafts.clear()
  try {
    const responses = await Promise.all([WorkflowService.catalog(), route.params.code ? WorkflowService.getByCode(String(route.params.code)) : Promise.resolve(null)])
    if (request !== ticket.value) return
    const [definitions, workflow] = responses
    if (!definitions.status) throw Error(String(definitions.message))
    catalog.value = (definitions.data?.executors || []).map((executor: WorkflowExecutor) => ({ ...executor, nodes: executor.nodes.map(node => {
      const key = 'workflow.studio.node.' + node.id
      return { ...node, label: t(key) === key ? node.label : t(key) }
    }) }))
    if (workflow && !workflow.status) throw Error(String(workflow.message))
    record.value = workflow?.data || { name: '', executor: String(route.query.executor || catalog.value[0]?.name || '') }
    graph.value = workflow?.data?.configure || { nodes: [], connections: [], timeout: 600, description: '' }
    graph.value.nodes = Array.isArray(graph.value.nodes) ? graph.value.nodes : []
    graph.value.connections = Array.isArray(graph.value.connections) ? graph.value.connections : []
    graph.value.nodes = graph.value.nodes.map(node => {
      const originalDefinition = definitions.data?.executors?.find((item: WorkflowExecutor) => item.name === record.value.executor)?.nodes.find((item: any) => item.id === node.tid)
      const localized = current.value?.nodes.find(item => item.id === node.tid)
      return originalDefinition && localized && (!node.label || node.label === originalDefinition.label) ? { ...node, label: localized.label } : node
    })
    original.value = snapshot(); issues.value = []; checked.value = false
    if (!workflow && route.query.template && current.value) template()
  } catch (error) { failure.value = error instanceof Error ? error.message : t('workflow.studio.loadFailed') }
  finally { if (request === ticket.value) loading.value = false }
}
function template() {
  if (!current.value) return
  const input = current.value.nodes.find(node => node.category === 'source'), output = current.value.nodes.find(node => node.category === 'sink')
  if (!input || !output) return
  const nodes = [input, output].map((node, index) => ({ id: crypto.randomUUID(), tid: node.id, key: node.key, category: node.category, label: node.label, position: { x: 60 + index * 300, y: 160 }, data: defaultValues(node.configure) }))
  graph.value = { ...graph.value, nodes, connections: [{ id: crypto.randomUUID(), source: `${nodes[0].id}-output`, target: `${nodes[1].id}-input` }] }
}
function selectExecutor(value: string) {
  if (record.value.state === 'RUNNING' || record.value.executor === value) return
  if (record.value.executor) drafts.set(record.value.executor, JSON.parse(JSON.stringify(graph.value)))
  record.value.executor = value; graph.value = drafts.get(value) || { nodes: [], connections: [], timeout: 600, description: '' }; issues.value = []; checked.value = false
}
async function check() {
  issues.value = current.value
    ? [...fieldIssues(current.value.settings, graph.value.settings, t('workflow.studio.jobSettings')).map(message => ({ scope: '', message })),
      ...validateWorkflow(mergeDefinitions(current.value.nodes, graph.value.nodes), graph.value.connections, current.value.limits)]
    : [{ scope: '', message: 'workflow.studio.noExecutor' }]
  checked.value = true
  if (issues.value.length) return false
  checking.value = true
  try {
    const response = await WorkflowService.validate({ ...record.value, configure: graph.value })
    if (!response.status) throw Error(String(response.message))
    issues.value = (response.data || []).map((message: string) => ({ scope: '', message }))
    return !issues.value.length
  } catch (error) { issues.value = [{ scope: '', message: error instanceof Error ? error.message : t('workflow.studio.checkFailed') }]; return false }
  finally { checking.value = false }
}
async function save() {
  if (busy.value) return false
  try { await form.value?.validate() } catch { settings.value = true; return false }
  if (!record.value.name?.trim()) { settings.value = true; await nextTick(); try { await form.value?.validate() } catch { /* Show the inline name error. */ }; return false }
  busy.value = true
  try {
    const response = await WorkflowService.saveOrUpdate({ ...record.value, configure: graph.value })
    if (!response.status) throw Error(String(response.message))
    record.value = response.data; graph.value = response.data.configure || graph.value; original.value = snapshot(); settings.value = false
    MfMessage.success(t('workflow.studio.saved'))
    if (!route.params.code) await router.replace(`/workflow/info/${response.data.code}`)
    return true
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.saveFailed')); return false }
  finally { busy.value = false }
}
async function publish() {
  if (busy.value || publishing.value || !(await check())) return
  if ((!code.value || dirty.value) && !(await save())) return
  if (!code.value) return
  publishing.value = true
  try {
    const response = await WorkflowService.publish(code.value)
    if (!response.status) throw Error(String(response.message))
    record.value = response.data; graph.value = response.data.configure; original.value = snapshot()
    MfMessage.success(t('workflow.studio.published'))
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.publishFailed')) }
  finally { publishing.value = false }
}
async function stop() {
  if (!code.value || busy.value) return
  busy.value = true
  try {
    const response = await WorkflowService.stop(code.value)
    if (!response.status) throw Error(String(response.message))
    record.value.state = 'STOPPED'; await consolePanel.value?.refresh()
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.stopFailed')) }
  finally { busy.value = false }
}
async function run() {
  if (busy.value || checking.value || !(await check())) return
  if ((!code.value || dirty.value) && !(await save())) return
  if (!code.value) return
  busy.value = true
  try {
    const response = await WorkflowService.restart(code.value)
    if (!response.status) throw Error(String(response.message))
    record.value.state = 'RUNNING'
    consolePanel.value?.open(); await consolePanel.value?.refresh()
  } catch (error) { MfMessage.error(error instanceof Error ? error.message : t('workflow.studio.runFailed')) }
  finally { busy.value = false }
}
/** Job settings and executor descriptions come from the executor plugin; translations override them when present. */
const settingLabel = (field: ConfigureItem) => { const key = 'workflow.studio.setting.' + field.field; return te(key) ? t(key) : field.label }
const executorDescription = computed(() => { const key = 'workflow.studio.executorDescription.' + current.value?.name; return te(key) ? t(key) : current.value?.description })
function updateSetting(field: string, value: unknown) { graph.value = { ...graph.value, settings: { ...(graph.value.settings || {}), [field]: value } } }
const capabilityTags = computed(() => current.value ? [
  { key: 'stop.' + current.value.capabilities.stop, on: current.value.capabilities.stop !== 'none' },
  { key: 'rowCount', on: current.value.capabilities.rowCount },
  { key: 'metrics', on: current.value.capabilities.metrics },
  { key: 'preview', on: current.value.capabilities.preview },
] : [])
function expand(node?: string) { router.replace({ path: route.path, query: expanded.value ? {} : { view: 'config', ...(node ? { node } : {}) } }) }
watch(graph, () => { checked.value = false; issues.value = [] }, { deep: true })
watch(() => route.params.code, (next, previous) => { if (!previous && record.value.code === next) return; initialize() })
const leaving = ref(false); let leaveResolve: ((answer: boolean) => void) | undefined
onBeforeRouteLeave(() => { if (!dirty.value) return true; leaving.value = true; return new Promise<boolean>(resolve => { leaveResolve = resolve }) })
function leave(answer: boolean) { leaving.value = false; leaveResolve?.(answer); leaveResolve = undefined }
const beforeUnload = (event: BeforeUnloadEvent) => { if (dirty.value) { event.preventDefault(); event.returnValue = '' } }
window.addEventListener('beforeunload', beforeUnload)
onBeforeUnmount(() => { ticket.value++; window.removeEventListener('beforeunload', beforeUnload); leaveResolve?.(false) })
initialize()
</script>
<template>
  <WorkflowShell :code="code" studio>
    <div v-if="loading" class="flex flex-1 items-center justify-center"><MfSpinner :label="$t('common.loading')"/></div>
    <div v-else-if="failure" class="flex flex-1 flex-col items-center justify-center gap-4"><MfEmpty variant="error" :title="$t('workflow.studio.loadFailed')" :description="failure"/><MfButton @click="initialize">{{ $t('common.refresh') }}</MfButton></div>
    <template v-else>
      <header class="relative z-20 flex shrink-0 flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line bg-surface px-4 py-2 shadow-mf-sm">
        <div class="flex min-w-0 items-center gap-3"><router-link to="/workflow"><MfButton size="sm" :aria-label="$t('workflow.studio.list')"><ArrowLeft :size="15"/></MfButton></router-link><div class="min-w-0"><div class="flex items-center gap-2"><h1 class="truncate text-sm font-semibold text-content sm:text-base">{{ record.name || $t('workflow.text.create') }}</h1><MfTag size="sm" :variant="graph.publishedAt && !dirty ? 'success' : 'primary'">{{ $t(dirty ? 'workflow.studio.unsaved' : graph.publishedAt ? 'workflow.studio.publishedStatus' : 'workflow.studio.draft') }}</MfTag><span v-if="graph.revision" class="text-xs text-content-tertiary">v{{ graph.revision }}</span></div><p class="mt-1 text-xs text-content-tertiary">{{ graph.nodes.length }} {{ $t('workflow.studio.nodes') }} · {{ graph.connections.length }} {{ $t('workflow.studio.connections') }}</p></div></div>
        <div class="flex shrink-0 flex-wrap items-center gap-2 max-sm:w-full"><MfSelect :model-value="record.executor" :disabled="busy || publishing || record.state === 'RUNNING'" class="w-36" :options="catalog.map(item => ({ label: item.name, value: item.name }))" @update:model-value="selectExecutor(String($event))"/><MfButton size="sm" :disabled="busy || publishing || record.state === 'RUNNING'" :aria-label="$t('workflow.studio.settings')" @click="settings = true"><Settings2 :size="15"/></MfButton><MfButton size="sm" :loading="checking" :disabled="busy || publishing" @click="check"><CheckCheck :size="15"/>{{ $t('workflow.studio.validate') }}</MfButton><MfButton size="sm" :loading="busy" :disabled="publishing || checking || !current || record.state === 'RUNNING'" @click="save"><Save :size="15"/>{{ $t('workflow.studio.saveDraft') }}</MfButton><MfButton v-if="record.state !== 'RUNNING'" size="sm" :loading="busy" :disabled="publishing || !current" @click="run"><Play :size="15"/>{{ $t('workflow.studio.run') }}</MfButton><MfButton v-else variant="danger" size="sm" :loading="busy" :disabled="current?.capabilities.stop === 'none'" @click="stop"><Square :size="14"/>{{ $t('workflow.studio.stop') }}</MfButton><MfButton variant="primary" size="sm" :loading="publishing" :disabled="busy || checking || !current || record.state === 'RUNNING'" @click="publish"><ArrowUpRight :size="15"/>{{ $t('workflow.studio.publish') }}</MfButton></div>
      </header>
      <div id="workflow-tools" class="relative z-10 shrink-0 border-b border-line bg-surface"/>
      <MfAlert v-if="record.state === 'RUNNING'" variant="info" :title="$t('workflow.studio.runningLocked')"/>
      <WorkflowEditor v-if="current" ref="editor" v-model="graph" class="min-h-0 flex-1" :executor="current" :expanded="expanded" :requested-node="String(route.query.node || '')" :readonly="record.state === 'RUNNING' || busy || publishing" @expand="expand">
        <template #bottom>
          <div v-if="checked" class="max-h-32 shrink-0 overflow-y-auto border-t border-line bg-surface p-3"><MfAlert v-if="!issues.length" variant="success" :title="$t('workflow.studio.valid')"/><button v-for="(issue, index) in issues" :key="index" type="button" class="block w-full rounded-mf-md px-2 py-1 text-left text-xs text-danger hover:bg-danger-light" @click="issue.scope && editor?.select(issue.scope)">{{ issue.message.startsWith('workflow.') ? $t(issue.message) : issue.message }}</button></div>
          <WorkflowConsole ref="consolePanel" :code="code" :capabilities="current.capabilities" @state="record.state = $event"/>
        </template>
      </WorkflowEditor>
      <MfEmpty v-else class="flex-1" :title="$t('workflow.studio.noExecutor')" :description="$t('workflow.studio.installExecutor')"/>
    </template>
  </WorkflowShell>
  <MfModal v-model:open="settings" :title="$t('workflow.studio.settings')">
    <MfForm ref="form" :model="record" layout="vertical" @finish="save">
      <MfFormItem name="name" :label="$t('common.name')" :rules="[{ required: true, message: $t('workflow.studio.nameRequired') }]"><MfInput v-model="record.name" :disabled="busy || publishing" :maxlength="200"/></MfFormItem>
      <MfFormItem :label="$t('workflow.studio.description')"><MfTextarea v-model="graph.description" :disabled="busy || publishing" :rows="3"/></MfFormItem>
      <MfFormItem :label="$t('workflow.studio.timeout')"><MfInputNumber v-model="graph.timeout" :disabled="busy || publishing" class="w-full" :min="1" :max="86400" decimal-only commit-on-change/></MfFormItem>
      <template v-if="current">
        <div class="mb-4 border-t border-line pt-4"><strong class="text-sm text-content">{{ $t('workflow.studio.jobSettings') }} · {{ current.name }}</strong><p v-if="executorDescription" class="mt-1 text-xs leading-5 text-content-tertiary">{{ executorDescription }}</p><div class="mt-2 flex flex-wrap gap-1.5"><MfTag v-for="item in capabilityTags" :key="item.key" size="sm" :variant="item.on ? 'success' : 'default'">{{ $t('workflow.studio.capability.' + item.key) }}</MfTag></div></div>
        <MfFormItem v-for="field in current.settings" :key="field.field" :label="settingLabel(field)"><WorkflowField :field="field" :model-value="graph.settings?.[field.field]" :disabled="busy || publishing" @update:model-value="updateSetting(field.field, $event)"/></MfFormItem>
        <p v-if="!current.settings.length" class="mb-4 text-xs text-content-tertiary">{{ $t('workflow.studio.noJobSettings') }}</p>
      </template>
      <div class="flex justify-end"><MfButton type="submit" variant="primary" :loading="busy">{{ $t('common.save') }}</MfButton></div>
    </MfForm>
  </MfModal>
  <MfModal :open="leaving" :title="$t('workflow.studio.unsaved')" @close="leave(false)"><p class="text-sm text-content-secondary">{{ $t('workflow.studio.leaveHint') }}</p><template #footer><MfButton @click="leave(false)">{{ $t('common.cancel') }}</MfButton><MfButton variant="danger" @click="leave(true)">{{ $t('workflow.studio.discard') }}</MfButton></template></MfModal>
</template>
