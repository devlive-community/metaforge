<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRight, Database, RefreshCw } from '@lucide/vue'
import { useI18n } from 'vue-i18n'
import DatasetService from '@/services/dataset'
import { FilterModel } from '@/model/filter'
import type { DatasetColumn, DatasetCreationEntry } from '@/model/dataset-editor'
const props = defineProps<{ entry: DatasetCreationEntry }>()
const emit = defineEmits<{ close: [], submitted: [result: {code:string,name:string}] }>()
const { t } = useI18n()
const filter = new FilterModel()
const targets = ref<any[]>([])
const targetCode = ref('')
const target = ref<any>(null)
const columns = ref<DatasetColumn[]>([])
const mapping = ref<Record<string,string>>({})
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const page = ref(1)
const totalPages = ref(1)
let request = 0
let active = true
const physical = computed(()=>columns.value.filter(column=>!column.virtualColumn))
const sourceRows = computed(()=>(props.entry.headers || []).map(name=>({name,target:mapping.value[name] || ''})))
const selectedTargets = computed(()=>new Set(Object.values(mapping.value).filter(Boolean)))
const complete = computed(()=>!!target.value && physical.value.length > 0 && physical.value.every(column=>selectedTargets.value.has(column.name)))
const headers = computed(()=>[{title:t('dataset.prototype.sourceField'),key:'name',dataIndex:'name'},{title:'',key:'arrow',dataIndex:'arrow'},{title:t('dataset.prototype.targetField'),key:'target',dataIndex:'target'},{title:t('common.state'),key:'state',dataIndex:'state'}])
async function loadTargets(more = false) {
  if (loading.value) return
  loading.value=true;error.value=''
  try {
    filter.page=more?page.value+1:1
    const response=await DatasetService.getAll({...filter})
    if (!active) return
    if (!response.status) throw new Error(response.message)
    targets.value=more?[...targets.value,...response.data.content]:response.data.content
    page.value=response.data.page;totalPages.value=response.data.totalPage
  } catch(cause) {if(active)error.value=cause instanceof Error?cause.message:t('dataset.editor.requestFailed')}
  finally {if(active)loading.value=false}
}
async function chooseTarget() {
  const current=++request
  target.value=null;columns.value=[];mapping.value={};error.value=''
  if (!targetCode.value) return
  loading.value=true
  try {
    const [info,fields]=await Promise.all([DatasetService.getByCode(targetCode.value),DatasetService.getColumnsByCode(targetCode.value)])
    if(!active || current!==request)return
    if(!info.status || !fields.status)throw new Error(info.message || fields.message)
    target.value=info.data;columns.value=fields.data
    const used=new Set<string>()
    for(const source of props.entry.headers || []) {
      const match=physical.value.find(column=>!used.has(column.name) && [column.original,column.name,column.aliasName].some(name=>name?.toLowerCase()===source.toLowerCase()))
      if(match){mapping.value[source]=match.name;used.add(match.name)}
    }
  }catch(cause){if(active && current===request)error.value=cause instanceof Error?cause.message:t('dataset.editor.requestFailed')}
  finally {if(active && current===request)loading.value=false}
}
function setMapping(source:string,value:string) {
  if(value && Object.entries(mapping.value).some(([key,target])=>key!==source && target===value)){error.value=t('dataset.prototype.uniqueMapping');return}
  error.value='';mapping.value={...mapping.value,[source]:value}
}
async function submit() {
  if(saving.value || loading.value)return
  if(!complete.value){error.value=t('dataset.prototype.mappingRequired');return}
  saving.value=true;error.value=''
  try {
    const updated=columns.value.map((column,position)=>({...column,position,original:column.virtualColumn?column.original:Object.entries(mapping.value).find(([,target])=>target===column.name)![0]}))
    const payload={...target.value,query:props.entry.query,source:{code:props.entry.sourceCode},columns:updated}
    const response=await DatasetService.saveOrUpdate(payload)
    if(!active)return
    if(!response.status)throw new Error(response.message)
    emit('submitted',{code:targetCode.value,name:target.value.name})
  }catch(cause){if(active)error.value=cause instanceof Error?cause.message:t('dataset.editor.requestFailed')}
  finally {if(active)saving.value=false}
}
onMounted(()=>loadTargets())
onBeforeUnmount(()=>{active=false;request++})
</script>
<template>
  <MfModal :open="true" :title="t('dataset.prototype.applyExisting')" :subtitle="t('dataset.prototype.mappingHint')" width="1180px" panel-class="md:w-[80vw]!" height="min(850px, calc(100dvh - 96px))" body-class="px-6 py-6" :close-on-overlay="false" :close-on-esc="!saving" :close-disabled="saving" trap-focus @close="!saving && emit('close')">
    <template #after-header><ol class="flex shrink-0 flex-wrap items-center justify-center gap-3 border-b border-line px-5 py-5 text-xs sm:gap-7"><li v-for="(label,index) in [t('dataset.editor.basic'),t('dataset.editor.source'),t('dataset.editor.columns'),t('dataset.editor.preview')]" :key="label" class="flex items-center gap-2" :class="index<=2?'font-semibold text-primary':'text-content-tertiary'"><span class="flex size-6 items-center justify-center rounded-mf-full border" :class="index===2?'border-primary bg-primary text-on-primary':index<2?'border-primary/20 bg-primary/10':'border-line'">{{ index<2?'✓':index+1 }}</span>{{ label }}</li></ol></template>
    <MfAlert v-if="error" variant="danger" :title="error" class="mb-4"/>
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3"><h2 class="text-base font-semibold">{{ t('dataset.prototype.applyExisting') }}</h2><MfTag variant="primary">{{ t('dataset.prototype.resultSize',[entry.rowCount || 0,entry.headers?.length || 0]) }}</MfTag></div>
    <div class="grid items-start gap-6 lg:grid-cols-[minmax(260px,0.8fr)_minmax(0,1.2fr)]">
      <div class="space-y-5">
        <MfFormItem name="targetDataset" :label="t('dataset.prototype.targetDataset')"><MfSelect v-model="targetCode" searchable :options="targets.map(item=>({label:item.name,value:item.code}))" :loading="loading" :disabled="saving" @change="chooseTarget"/></MfFormItem>
        <MfButton v-if="page < totalPages" size="sm" :loading="loading" @click="loadTargets(true)">{{ t('dataset.prototype.loadMore') }}</MfButton>
        <section v-if="target" class="space-y-3 rounded-mf-lg border border-line bg-surface-hover/40 p-5"><p class="text-xs text-content-tertiary">{{ t('dataset.prototype.targetDataset') }}</p><h3 class="flex items-center gap-2 text-sm font-semibold"><Database :size="20" class="text-primary"/>{{ target.name }}</h3><p class="text-xs text-content-tertiary">{{ target.source?.name }} · {{ physical.length }} {{ t('dataset.prototype.fieldUnit') }}</p></section>
        <section class="rounded-mf-lg border border-primary/30 bg-primary/5 p-4"><h3 class="flex items-center gap-2 text-sm font-semibold text-primary"><RefreshCw :size="16"/>{{ t('dataset.prototype.replaceDefinition') }}</h3><p class="mt-2 text-xs leading-6 text-content-secondary">{{ t('dataset.prototype.replaceDefinitionHint') }}</p></section>
      </div>
      <div class="min-w-0 space-y-3"><div class="flex items-center justify-between"><h3 class="text-sm font-semibold">{{ t('dataset.prototype.fieldMapping') }}</h3><MfTag v-if="target" :variant="complete?'primary':'default'" size="sm">{{ selectedTargets.size }} / {{ physical.length }}</MfTag></div><MfEmpty v-if="!target" variant="initial" :title="t('dataset.prototype.chooseTarget')"/><MfTable v-else :columns="headers" :data-source="sourceRows" row-key="name" :bordered="false"><template #cell-arrow><ArrowRight :size="14" class="text-content-tertiary"/></template><template #cell-target="{record}"><MfSelect :model-value="mapping[record.name] || ''" clearable size="sm" :disabled="saving" :options="physical.map(column=>({label:column.aliasName || column.name,value:column.name}))" @update:model-value="setMapping(record.name,String($event))"/></template><template #cell-state="{record}"><MfTag size="sm" :variant="mapping[record.name]?'primary':'default'">{{ t(mapping[record.name]?'dataset.prototype.mapped':'dataset.prototype.ignored') }}</MfTag></template></MfTable></div>
    </div>
    <template #footer><div class="flex w-full flex-wrap items-center justify-between gap-3"><p class="max-w-xl text-xs leading-5 text-content-tertiary">{{ t('dataset.prototype.mappingFooter') }}</p><div class="ml-auto flex gap-2"><MfButton :disabled="saving" @click="emit('close')">{{ t('common.cancel') }}</MfButton><MfButton variant="primary" :disabled="!complete || loading" :loading="saving" @click="submit">{{ t('dataset.prototype.confirmReplacement') }}</MfButton></div></div></template>
  </MfModal>
</template>
