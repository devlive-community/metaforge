<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ArrowDown, ArrowUp, Pencil, Plus, Search, Settings, Sparkles, Trash2 } from '@lucide/vue'
import type { DatasetColumn } from '@/model/dataset-editor'
import DatasetService from '@/services/dataset'

const props = withDefaults(defineProps<{ modelValue: DatasetColumn[], refreshing?: boolean, externalIssue?: string }>(), { refreshing: false, externalIssue: '' })
const emit = defineEmits<{ 'update:modelValue': [columns: DatasetColumn[]], 'refresh-columns': [] }>()
const { t } = useI18n()
const keyword = ref('')
const selected = ref<Set<string>>(new Set())
const bulkOpen = ref(false)
const bulk = ref({mode:'keep',nullable:'keep',orderByKey:'keep'})
const opened = ref(false)
const editingIndex = ref(-1)
const draft = ref<DatasetColumn | null>(null)
const issue = ref('')
const validating = ref(false)
const catalogLoading = ref(false)
const catalogLoaded = ref(false)
const catalogError = ref('')
const engine = ref('')
const functions = ref<{ name: string, example?: string, description?: string }[]>([])
const expressionInput = ref<{ $el: HTMLTextAreaElement } | null>(null)
const selectedField = ref('')
const selectedFunction = ref('')
let validationRequest = 0
let active = true
const physicalFields = computed(() => props.modelValue.filter(column => !column.virtualColumn))
async function loadFunctions() {
  if (catalogLoading.value || catalogLoaded.value) return
  catalogLoading.value = true
  catalogError.value = ''
  try {
    const response = await DatasetService.getColumnFunctions()
    if (!active) return
    if (!response.status) throw new Error(response.message)
    engine.value = response.data?.engine || ''
    functions.value = (response.data?.functions || []).filter((item: any) => typeof item.name === 'string' && /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(item.name))
    catalogLoaded.value = true
  }
  catch (cause) { if (active) catalogError.value = cause instanceof Error ? cause.message : t('dataset.editor.requestFailed') }
  finally { if (active) catalogLoading.value = false }
}
watch(opened, open => {
  if (open && draft.value?.virtualColumn) loadFunctions()
  if (!open) { validationRequest++; validating.value = false }
})
onBeforeUnmount(() => { active = false; validationRequest++ })
function insertToken(token: string, cursorOffset = token.length) {
  if (!draft.value) return
  const value = draft.value.formula || ''
  const input = expressionInput.value?.$el
  const start = input?.selectionStart ?? value.length
  const end = input?.selectionEnd ?? start
  draft.value.formula = value.slice(0, start) + token + value.slice(end)
  nextTick(() => {
    expressionInput.value?.$el.focus()
    expressionInput.value?.$el.setSelectionRange?.(start + cursorOffset, start + cursorOffset)
  })
}
function appendField(value: string) {
  insertToken('`' + value.replace(/`/g, '``') + '`')
}

function appendFunction(value: string) {
  const input = expressionInput.value?.$el
  const selected = (draft.value?.formula || '').slice(input?.selectionStart ?? 0, input?.selectionEnd ?? 0)
  insertToken(`${value}(${selected})`, selected ? value.length + selected.length + 2 : value.length + 1)
}
const filtered = computed(() => props.modelValue.map((column, index) => ({ ...column, index }))
  .filter(column => [column.name, column.aliasName, column.original].some(value => value?.toLowerCase().includes(keyword.value.toLowerCase()))))
const headers = computed(() => [
  {key:'selected',dataIndex:'selected',title:'',width:'36px'},
  { key: 'name', dataIndex: 'name', title: t('dataset.common.columnName') },
  { key: 'aliasName', dataIndex: 'aliasName', title: t('dataset.common.columnAlias') },
  { key: 'type', dataIndex: 'type', title: t('dataset.common.columnType') },
  { key: 'mode', dataIndex: 'mode', title: t('dataset.common.columnMode') },
  {key:'nullable',dataIndex:'nullable',title:t('dataset.common.columnIsNullable')},
  {key:'orderByKey',dataIndex:'orderByKey',title:t('dataset.common.columnIsOrderByKey')},
  {key:'primaryKey',dataIndex:'primaryKey',title:t('dataset.common.columnIsPrimaryKey')},
  { key: 'action', dataIndex: 'action', title: t('common.action') }
])
const typeOptions = computed(() => [
  ['STRING', 'String'], ['NUMBER', 'Number'], ['NUMBER_SIGNED', 'NumberSigned'], ['BOOLEAN', 'Boolean'], ['DATETIME', 'DateTime']
].map(([value, label]) => ({ value, label: t(`dataset.common.columnType${label}`) })))
const modeOptions = computed(() => ['DIMENSION', 'METRIC'].map(value => ({ value, label: t(`dataset.common.columnMode${value === 'METRIC' ? 'Metric' : 'Dimension'}`) })))
const keyFields = ['orderByKey', 'primaryKey', 'partitionKey', 'samplingKey'] as const
const keyLabels = { orderByKey: 'columnIsOrderByKey', primaryKey: 'columnIsPrimaryKey', partitionKey: 'columnIsPartitionKey', samplingKey: 'columnIsSampling' }

const allSelected = computed(()=>filtered.value.length>0 && filtered.value.every(column=>selected.value.has(column.name)))
watch(()=>props.modelValue,columns=>{const names=new Set(columns.map(column=>column.name));selected.value=new Set([...selected.value].filter(name=>names.has(name)))})
function selectAll(value: boolean) { const next=new Set(selected.value);for(const column of filtered.value){if(value)next.add(column.name);else next.delete(column.name)}selected.value=next }
function toggleSelected(name:string,value:boolean){const next=new Set(selected.value);if(value)next.add(name);else next.delete(name);selected.value=next}
function updateColumn(index:number,patch:Partial<DatasetColumn>) {
  const column={...props.modelValue[index],...patch}
  if(column.samplingKey && !column.orderByKey){issue.value=t('dataset.tip.validatorSampling');return}
  issue.value=''
  emit('update:modelValue',props.modelValue.map((value,position)=>position===index?column:value))
}
function applyBulk() {
  const next=props.modelValue.map(column=>{
    if(!selected.value.has(column.name))return column
    return {...column,mode:bulk.value.mode==='keep'?column.mode:bulk.value.mode,
      nullable:!column.virtualColumn && bulk.value.nullable!=='keep'?bulk.value.nullable==='true':column.nullable,
      orderByKey:!column.virtualColumn && bulk.value.orderByKey!=='keep'?bulk.value.orderByKey==='true':column.orderByKey}
  })
  if(next.some(column=>column.samplingKey && !column.orderByKey)){issue.value=t('dataset.tip.validatorSampling');return}
  issue.value='';emit('update:modelValue',next);bulkOpen.value=false
}
function edit(index: number) {
  editingIndex.value = index
  draft.value = { ...props.modelValue[index], comment: props.modelValue[index].comment || '', aliasName: props.modelValue[index].aliasName || '', formula: props.modelValue[index].formula || '' }
  issue.value = ''
  opened.value = true
}
function add() {
  let sequence = props.modelValue.length + 1
  while (props.modelValue.some(column => column.name === `column_${sequence}`)) sequence++
  draft.value = {
    id: null, name: `column_${sequence}`, aliasName: `column_${sequence}`, original: null,
    type: 'STRING', mode: 'DIMENSION', formula: '', comment: '', defaultValue: null, position: props.modelValue.length,
    nullable: false, length: 0, orderByKey: false, partitionKey: false, primaryKey: false, samplingKey: false,
    virtualColumn: true, customColumn: true
  }
  editingIndex.value = -1
  issue.value = ''
  opened.value = true
}
async function save() {
  if (!draft.value || validating.value) return
  const column = { ...draft.value, name: draft.value.name.trim(), aliasName: draft.value.aliasName.trim() }
  if (!column.name || !column.aliasName) { issue.value = t('dataset.editor.columnRequired'); return }
  if (props.modelValue.some((value, index) => index !== editingIndex.value && value.name === column.name)) { issue.value = t('dataset.editor.duplicateName'); return }
  if (props.modelValue.some((value, index) => index !== editingIndex.value && value.aliasName === column.aliasName)) { issue.value = t('dataset.editor.duplicateAlias'); return }
  if (column.samplingKey && !column.orderByKey) { issue.value = t('dataset.tip.validatorSampling'); return }
  if (column.virtualColumn) {
    if (!column.formula?.trim()) { issue.value = t('dataset.editor.virtualExpressionRequired'); return }
    const request = ++validationRequest
    validating.value = true
    try {
      const response = await DatasetService.validateColumns([...physicalFields.value, { ...column, formula: column.formula.trim() }])
      if (!active || request !== validationRequest || !opened.value) return
      if (!response.status) { issue.value = response.message; return }
    }
    catch (cause) { if (active && request === validationRequest) issue.value = cause instanceof Error ? cause.message : t('dataset.editor.requestFailed'); return }
    finally { if (active && request === validationRequest) validating.value = false }
  }
  const next = [...props.modelValue]
  if (editingIndex.value < 0) next.push(column)
  else next[editingIndex.value] = column
  emit('update:modelValue', next.map((value, position) => ({ ...value, position })))
  // A stale validation banner must not survive a successful save.
  issue.value = ''
  opened.value = false
}
function remove(index: number) {
  if (!props.modelValue[index].customColumn) return
  emit('update:modelValue', props.modelValue.filter((_, position) => position !== index).map((value, position) => ({ ...value, position })))
}
function move(index: number, offset: number) {
  const next = [...props.modelValue]
  const target = index + offset
  if (target < 0 || target >= next.length) return
  ;[next[index], next[target]] = [next[target], next[index]]
  emit('update:modelValue', next.map((value, position) => ({ ...value, position })))
}
</script>

<template>
  <div class="space-y-4">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div><h2 class="text-base font-semibold text-content">{{ t('dataset.editor.columns') }}</h2><p class="mt-1 text-xs leading-5 text-content-tertiary">{{ t('dataset.prototype.configureColumnsHint') }}</p></div>
      <div class="flex flex-wrap gap-2"><MfButton v-if="refreshing !== undefined" size="sm" :loading="refreshing" @click="emit('refresh-columns')"><Sparkles :size="14"/>{{ t('dataset.prototype.identifyAgain') }}</MfButton><MfButton size="sm" :disabled="!selected.size" @click="issue='';bulkOpen=true"><Settings :size="14"/>{{ t('dataset.prototype.bulkSettings') }}</MfButton><MfButton size="sm" variant="primary" @click="add"><Plus :size="14"/>{{ t('dataset.editor.addColumn') }}</MfButton></div>
    </div>
    <div class="flex flex-wrap items-center gap-3"><MfInput v-model="keyword" class="w-64" :placeholder="t('dataset.editor.searchColumns')" clearable><template #prefix><Search :size="14"/></template></MfInput><MfTag variant="primary">{{ t('dataset.prototype.fieldsCount',[modelValue.length]) }}</MfTag><MfTag v-if="selected.size">{{ t('dataset.prototype.selectedCount',[selected.size]) }}</MfTag><p class="ml-auto text-xs text-content-tertiary">{{ t('dataset.editor.keyHint') }}</p></div>
    <MfAlert v-if="!opened && (externalIssue || issue)" variant="danger" :title="externalIssue || issue"/>
    <MfTable :columns="headers" :data-source="filtered" row-key="index" :empty-text="t('common.noData')">
      <template #header-selected><MfCheckbox :model-value="allSelected" :indeterminate="selected.size>0 && !allSelected" :aria-label="t('dataset.prototype.selectAllFields')" @update:model-value="selectAll(!!$event)"/></template>
      <template #cell-selected="{record}"><MfCheckbox :model-value="selected.has(record.name)" :aria-label="record.original || record.name" @update:model-value="toggleSelected(record.name,!!$event)"/></template>
      <template #cell-name="{ record }"><MfTooltip :content="record.virtualColumn?record.formula:t('dataset.prototype.storageColumn',[record.name])"><span class="block max-w-36 truncate text-sm font-medium">{{ record.virtualColumn?record.name:(record.original || record.name) }}</span></MfTooltip></template>
      <template #cell-aliasName="{record}"><MfInput size="sm" class="min-w-28" :model-value="record.aliasName" @update:model-value="updateColumn(record.index,{aliasName:String($event)})"/></template>
      <template #cell-nullable="{record}"><MfSwitch size="sm" :model-value="record.nullable" :disabled="record.virtualColumn" :aria-label="t('dataset.common.columnIsNullable')" @update:model-value="updateColumn(record.index,{nullable:!!$event})"/></template>
      <template #cell-orderByKey="{record}"><MfSwitch size="sm" :model-value="record.orderByKey" :disabled="record.virtualColumn" :aria-label="t('dataset.common.columnIsOrderByKey')" @update:model-value="updateColumn(record.index,{orderByKey:!!$event})"/></template>
      <template #cell-primaryKey="{record}"><MfSwitch size="sm" :model-value="record.primaryKey" :disabled="record.virtualColumn" :aria-label="t('dataset.common.columnIsPrimaryKey')" @update:model-value="updateColumn(record.index,{primaryKey:!!$event})"/></template>
      <template #cell-original="{ record }"><span class="block max-w-xs truncate text-content-secondary" :title="record.virtualColumn ? record.formula : record.original">{{ record.virtualColumn ? (record.formula || t('dataset.editor.customColumn')) : record.original }}</span></template>
      <template #cell-type="{ record }"><MfSelect size="sm" class="min-w-24" :model-value="record.type" :options="typeOptions" :aria-label="t('dataset.common.columnType')" @update:model-value="updateColumn(record.index,{type:String($event)})"/></template>
      <template #cell-mode="{ record }"><MfSelect size="sm" class="min-w-24" :model-value="record.mode" :options="modeOptions" :aria-label="t('dataset.common.columnMode')" @update:model-value="updateColumn(record.index,{mode:String($event)})"/></template>
      <template #cell-keys="{ record }">
        <div class="flex flex-wrap gap-1"><MfTag v-for="field in keyFields.filter(field => record[field])" :key="field">{{ t(`dataset.common.${keyLabels[field]}`) }}</MfTag></div>
      </template>
      <template #cell-action="{ record }">
        <div class="flex items-center gap-1">
          <MfButton size="sm" variant="ghost" :aria-label="t('dataset.editor.editColumn')" @click="edit(record.index)"><Pencil :size="14"/></MfButton>
          <MfButton size="sm" variant="ghost" :disabled="record.index === 0" :aria-label="t('dataset.editor.moveUp')" @click="move(record.index, -1)"><ArrowUp :size="14"/></MfButton>
          <MfButton size="sm" variant="ghost" :disabled="record.index === modelValue.length - 1" :aria-label="t('dataset.editor.moveDown')" @click="move(record.index, 1)"><ArrowDown :size="14"/></MfButton>
          <MfButton v-if="record.customColumn" size="sm" variant="ghost" class="text-danger" :aria-label="t('dataset.editor.deleteColumn')" @click="remove(record.index)"><Trash2 :size="14"/></MfButton>
        </div>
      </template>
    </MfTable>
  </div>
  <MfModal :open="bulkOpen" :title="t('dataset.prototype.bulkSettings')" trap-focus @close="bulkOpen=false"><div class="space-y-4"><MfAlert v-if="issue" variant="danger" :title="issue"/><p class="text-xs text-content-tertiary">{{ t('dataset.prototype.selectedCount',[selected.size]) }}</p><MfFormItem name="bulkMode" :label="t('dataset.common.columnMode')"><MfSelect v-model="bulk.mode" :options="[{label:t('dataset.prototype.keepExisting'),value:'keep'},...modeOptions]"/></MfFormItem><MfFormItem name="bulkNullable" :label="t('dataset.common.columnIsNullable')"><MfSelect v-model="bulk.nullable" :options="[{label:t('dataset.prototype.keepExisting'),value:'keep'},{label:t('dataset.prototype.enabled'),value:'true'},{label:t('dataset.prototype.disabled'),value:'false'}]"/></MfFormItem><MfFormItem name="bulkOrder" :label="t('dataset.common.columnIsOrderByKey')"><MfSelect v-model="bulk.orderByKey" :options="[{label:t('dataset.prototype.keepExisting'),value:'keep'},{label:t('dataset.prototype.enabled'),value:'true'},{label:t('dataset.prototype.disabled'),value:'false'}]"/></MfFormItem></div><template #footer><MfButton @click="bulkOpen=false">{{ t('common.cancel') }}</MfButton><MfButton variant="primary" @click="applyBulk">{{ t('common.save') }}</MfButton></template></MfModal>

  <MfDrawer v-model:open="opened" :title="t(editingIndex < 0 ? 'dataset.editor.addColumn' : 'dataset.editor.editColumn')" width="min(680px, 100vw)" :close-on-overlay="false">
    <MfForm v-if="draft" id="dataset-column-form" :model="draft" @finish="save">
      <MfAlert v-if="issue" variant="danger" :title="issue"/>
      <MfFormItem v-if="!draft.virtualColumn" name="original" :label="t('dataset.editor.sourceField')"><MfInput :model-value="draft.original || t('dataset.editor.customColumn')" disabled/></MfFormItem>
      <div class="grid gap-4 sm:grid-cols-2">
        <MfFormItem name="name" :label="t('dataset.common.columnName')" :rules="[{ required: true, message: t('dataset.editor.columnRequired') }]"><MfInput v-model="draft.name"/></MfFormItem>
        <MfFormItem name="aliasName" :label="t('dataset.common.columnAlias')" :rules="[{ required: true, message: t('dataset.editor.columnRequired') }]"><MfInput v-model="draft.aliasName"/></MfFormItem>
        <MfFormItem name="type" :label="t('dataset.common.columnType')"><MfSelect v-model="draft.type" :options="typeOptions"/></MfFormItem>
        <MfFormItem name="mode" :label="t('dataset.common.columnMode')"><MfSelect v-model="draft.mode" :options="modeOptions"/></MfFormItem>
        <MfFormItem v-if="!draft.virtualColumn" name="defaultValue" :label="t('dataset.common.columnDefaultValue')"><MfInput :model-value="draft.defaultValue ?? ''" @update:model-value="draft.defaultValue = $event" :disabled="draft.virtualColumn"/></MfFormItem>
        <MfFormItem v-if="!draft.virtualColumn" name="length" :label="t('dataset.common.columnLength')"><MfInputNumber v-model="draft.length" :min="0" :disabled="draft.virtualColumn || ['BOOLEAN', 'DATETIME'].includes(draft.type)"/></MfFormItem>
      </div>
      <div v-if="draft.virtualColumn" class="space-y-3 rounded-mf-lg border border-line p-4">
        <div><h3 class="text-sm font-semibold">{{ t('dataset.editor.virtualExpression') }}</h3><p class="mt-1 text-xs text-content-secondary">{{ t('dataset.editor.virtualEngine', [engine || '…']) }}</p></div>
        <div class="grid gap-3 sm:grid-cols-2">
          <MfFormItem name="referenceField" :label="t('dataset.editor.insertField')"><MfSelect v-model="selectedField" searchable :placeholder="t('dataset.editor.insertField')" :options="physicalFields.map(column => ({label: `${column.aliasName || column.name} (${column.name})`, value: column.name}))" @change="appendField(String($event))"/></MfFormItem>
          <MfFormItem name="referenceFunction" :label="t('dataset.editor.insertFunction')"><MfSelect v-model="selectedFunction" searchable :placeholder="t('dataset.editor.insertFunction')" :options="functions.map(item => ({label: item.name, value: item.name}))" :loading="catalogLoading" :empty-text="t('dataset.editor.noFunctions')" @change="appendFunction(String($event))"/></MfFormItem>
        </div>
        <MfAlert v-if="catalogError" variant="danger" :title="catalogError"><MfButton size="sm" :loading="catalogLoading" @click="loadFunctions">{{ t('common.refresh') }}</MfButton></MfAlert>
        <MfFormItem name="formula" :label="t('dataset.editor.expressionLabel')" :rules="[{required: true, message: t('dataset.editor.virtualExpressionRequired')}]"><MfTextarea ref="expressionInput" :model-value="draft.formula || ''" @update:model-value="draft.formula = $event" :rows="4" :maxlength="4000" class="font-mono text-xs" :placeholder="t('dataset.editor.expressionPlaceholder')"/></MfFormItem>
        <p class="text-xs text-content-secondary">{{ t('dataset.editor.expressionHint') }}</p>
      </div>
      <MfFormItem name="comment" :label="t('dataset.common.columnComment')"><MfTextarea v-model="draft.comment" :rows="3"/></MfFormItem>
      <h3 v-if="!draft.virtualColumn" class="text-sm font-semibold">{{ t('dataset.editor.storageProperties') }}</h3>
      <div v-if="!draft.virtualColumn" class="rounded-mf-md border border-line p-4 space-y-4">
        <div class="flex items-center justify-between gap-3"><span>{{ t('dataset.common.columnIsNullable') }}</span><MfSwitch v-model="draft.nullable" :disabled="draft.virtualColumn" :aria-label="t('dataset.common.columnIsNullable')"/></div>
        <div v-for="field in keyFields" :key="field" class="flex items-center justify-between gap-3"><span>{{ t(`dataset.common.${keyLabels[field]}`) }}</span><MfSwitch v-model="draft[field]" :disabled="draft.virtualColumn" :aria-label="t(`dataset.common.${keyLabels[field]}`)"/></div>
      </div>
      <p v-if="draft.virtualColumn" class="text-xs text-content-secondary">{{ t('dataset.editor.virtualHint') }}</p>
    </MfForm>
    <template #footer><MfButton @click="opened = false">{{ t('common.cancel') }}</MfButton><MfButton variant="primary" type="submit" form="dataset-column-form" :loading="validating">{{ t('common.save') }}</MfButton></template>
  </MfDrawer>
</template>
