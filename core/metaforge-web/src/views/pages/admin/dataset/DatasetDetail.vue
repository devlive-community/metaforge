<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChartColumn, Database, Download, History, Pencil, RefreshCw, Search } from '@lucide/vue'
import DatasetService from '@/services/dataset'
import DatasetWizard from './DatasetWizard.vue'
import DatasetHistory from './DatasetHistory.vue'
import type { DatasetColumn } from '@/model/dataset-editor'
import { useUtil } from '@/utils/common'
const route=useRoute();const router=useRouter();const {t}=useI18n();const {getDatasetStateText}=useUtil()
const code=String(route.params.code || '')
const info=ref<any>(null)
const fields=ref<DatasetColumn[]>([])
const loading=ref(true)
const previewLoading=ref(false)
const error=ref('')
const previewError=ref('')
const preview=ref<any>(null)
const activeTab=ref('preview')
const keyword=ref('')
const page=ref(1)
const pageSize=ref(20)
const editing=ref(false)
const historyOpen=ref(false)
const banner=ref(route.query.submitted === '1')
let active=true
let request=0
let previewRequest=0
let polls=0
let timer: ReturnType<typeof setTimeout> | undefined
const latestState=computed(()=>info.value?.state?.at(-1) || '')
const failed=computed(()=>latestState.value.endsWith('FAILED'))
const complete=computed(()=>latestState.value==='COMPLETE_SUCCESS')
const tableAvailable=computed(()=>info.value?.state?.includes('TABLE_SUCCESS'))
const headers=computed<string[]>(()=>preview.value?.headers || [])
const previewColumns=computed(()=>headers.value.map((header,index)=>({title:header,key:`field_${index}`,dataIndex:`field_${index}`})))
const allRows=computed<Record<string,unknown>[]>(()=>preview.value?.columns || [])
const filteredRows=computed(()=>{
 const text=keyword.value.trim().toLowerCase()
 return allRows.value.filter(row=>!text || headers.value.some(header=>String(row[header]??'').toLowerCase().includes(text)))
})
const rows=computed(()=>filteredRows.value.slice((page.value-1)*pageSize.value,page.value*pageSize.value).map(row=>Object.fromEntries(headers.value.map((header,index)=>[`field_${index}`,row[header]]))))
const typeLabels=computed<Record<string,string>>(()=>({STRING:t('dataset.common.columnTypeString'),NUMBER:t('dataset.common.columnTypeNumber'),NUMBER_SIGNED:t('dataset.common.columnTypeNumberSigned'),BOOLEAN:t('dataset.common.columnTypeBoolean'),DATETIME:t('dataset.common.columnTypeDateTime')}))
const modeLabels=computed<Record<string,string>>(()=>({DIMENSION:t('dataset.common.columnModeDimension'),METRIC:t('dataset.common.columnModeMetric'),GROUP:t('dataset.common.columnModeGroup'),FILTER:t('dataset.common.columnModeFilter')}))
const fieldHeaders=computed(()=>[
 {title:t('dataset.common.columnName'),dataIndex:'name',key:'name'},
 {title:t('dataset.common.columnAlias'),dataIndex:'aliasName',key:'aliasName'},
 {title:t('dataset.common.columnType'),dataIndex:'type',key:'type'},
 {title:t('dataset.common.columnMode'),dataIndex:'mode',key:'mode'},
 {title:t('dataset.editor.sourceOrFormula'),dataIndex:'original',key:'original'}
])
const tabs=computed(()=>[{key:'preview',label:t('dataset.common.dataPreview')},{key:'fields',label:t('dataset.editor.columns')},{key:'sql',label:t('dataset.prototype.sqlDefinition')}])
async function loadPreview() {
 if(previewLoading.value || !tableAvailable.value)return
 const current=++previewRequest
 previewLoading.value=true;previewError.value=''
 try {
  const response=await DatasetService.preview(code)
  if(!active || current!==previewRequest)return
  if(!response.status || response.data?.isSuccessful===false)throw new Error(response.data?.message || response.message)
  preview.value=response.data;page.value=1
 }catch(cause){if(active && current===previewRequest)previewError.value=cause instanceof Error?cause.message:t('dataset.editor.requestFailed')}
 finally{if(active && current===previewRequest)previewLoading.value=false}
}
async function load() {
 const current=++request
 clearTimeout(timer)
 error.value=''
 try {
  const [detail,columns]=await Promise.all([DatasetService.getByCode(code),DatasetService.getColumnsByCode(code)])
  if(!active || current!==request)return
  if(!detail.status || !columns.status) {
    if(route.query.submitted==='1' && polls<8){polls++;timer=setTimeout(load,1200);return}
    throw new Error(detail.message || columns.message)
  }
  info.value=detail.data;fields.value=columns.data;loading.value=false
  if(tableAvailable.value && !preview.value)loadPreview()
  const final=complete.value || failed.value
  if(polls<40 && (!final || (route.query.submitted==='1' && polls<4))){polls++;timer=setTimeout(load,1500)}
 }catch(cause){if(active && current===request){error.value=cause instanceof Error?cause.message:t('dataset.editor.requestFailed');loading.value=false}}
}
function edited() {
 editing.value=false;polls=0;banner.value=true;preview.value=null
 timer=setTimeout(load,400)
}
function download() {
 const escape=(value:unknown)=>`"${String(value??'').replace(/"/g,'""')}"`
 const csv=[headers.value.map(escape).join(','),...allRows.value.map(row=>headers.value.map(header=>escape(row[header])).join(','))].join('\r\n')
 const url=URL.createObjectURL(new Blob(['\ufeff'+csv],{type:'text/csv;charset=utf-8'}))
 const link=document.createElement('a');link.href=url;link.download=`dataset-preview-${code}.csv`;link.click();URL.revokeObjectURL(url)
}
onMounted(()=>{timer=setTimeout(load,route.query.submitted==='1'?200:0)})
onBeforeUnmount(()=>{active=false;request++;previewRequest++;clearTimeout(timer)})
</script>
<template>
 <div class="mx-auto w-full max-w-[1600px] space-y-5">
  <MfSpinner v-if="loading" class="py-20" :label="t('dataset.editor.loading')"/>
  <MfAlert v-else-if="error" variant="danger" :title="error"><MfButton @click="load">{{ t('common.refresh') }}</MfButton></MfAlert>
  <template v-else-if="info">
   <div v-if="banner" class="flex flex-wrap items-center justify-between gap-3 rounded-mf-md border border-primary/20 bg-primary/5 px-5 py-4 text-sm text-primary"><span>{{ t(complete?'dataset.prototype.datasetReady':'dataset.editor.submitted') }}</span><MfButton size="sm" variant="ghost" :aria-label="t('common.cancel')" @click="banner=false">×</MfButton></div>
   <div class="flex flex-wrap items-center justify-between gap-4"><div class="flex min-w-0 items-center gap-3"><span class="flex size-12 shrink-0 items-center justify-center rounded-mf-md bg-primary/10 text-primary"><Database :size="25"/></span><div class="min-w-0"><h1 class="break-all text-xl leading-7 font-semibold">{{ info.name }}</h1><p class="mt-1 text-xs text-content-tertiary">{{ info.source?.name }} · {{ info.updateTime }}</p></div></div><div class="flex flex-wrap gap-2"><MfButton @click="router.push('/dataset')">{{ t('dataset.editor.backToList') }}</MfButton><MfButton @click="editing=true"><Pencil :size="15"/>{{ t('dataset.common.modify') }}</MfButton><MfButton variant="primary" :disabled="!complete" @click="router.push(`/dataset/adhoc/${code}`)"><ChartColumn :size="15"/>{{ t('dataset.common.adhoc') }}</MfButton></div></div>
   <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><div class="rounded-mf-lg border border-line bg-surface p-5"><p class="text-xs text-content-tertiary">{{ t('dataset.prototype.fieldCount') }}</p><p class="mt-2 text-2xl font-semibold">{{ fields.length }}</p></div><div class="rounded-mf-lg border border-line bg-surface p-5"><p class="text-xs text-content-tertiary">{{ t('dataset.common.totalRows') }}</p><p class="mt-2 text-2xl font-semibold">{{ info.totalRows ?? '—' }}</p></div><div class="rounded-mf-lg border border-line bg-surface p-5"><p class="text-xs text-content-tertiary">{{ t('dataset.common.syncMode') }}</p><p class="mt-3 text-sm font-semibold">{{ t(info.syncMode==='TIMING'?'dataset.common.syncModeTiming':info.syncMode==='OUT_SYNC'?'dataset.common.syncModeOutSync':'dataset.common.syncModeManual') }}</p></div><div class="rounded-mf-lg border border-line bg-surface p-5"><p class="text-xs text-content-tertiary">{{ t('common.state') }}</p><MfTag class="mt-3" :variant="complete?'primary':failed?'danger':'default'">{{ getDatasetStateText(latestState) }}</MfTag></div></div>
   <MfAlert v-if="failed" variant="danger" :title="info.message || t('dataset.common.failed')"/>
   <section class="overflow-hidden rounded-mf-lg border border-line bg-surface">
    <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line px-5 py-3"><div class="flex items-center gap-4"><button v-for="tab in tabs" :key="tab.key" type="button" class="border-b-2 py-2 text-sm font-semibold" :class="activeTab===tab.key?'border-primary text-primary':'border-transparent text-content-tertiary hover:text-content'" @click="activeTab=tab.key">{{ tab.label }}</button></div><div class="flex gap-2"><MfButton size="sm" @click="historyOpen=true"><History :size="14"/>{{ t('dataset.common.history') }}</MfButton><MfButton size="sm" :loading="previewLoading" @click="load();loadPreview()"><RefreshCw :size="14"/>{{ t('common.refresh') }}</MfButton><MfButton size="sm" :disabled="!allRows.length" @click="download"><Download :size="14"/>{{ t('common.export') }}</MfButton></div></div>
    <div class="p-5">
     <template v-if="activeTab==='preview'">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3"><p class="text-xs text-content-tertiary">{{ t('dataset.prototype.storedPreviewHint') }}</p><MfInput v-model="keyword" size="sm" class="w-64" clearable :placeholder="t('query.result.searchResults')" @update:model-value="page=1"><template #prefix><Search :size="14"/></template></MfInput></div>
      <MfAlert v-if="previewError" variant="danger" :title="previewError" class="mb-4"/>
      <MfEmpty v-if="!tableAvailable" variant="initial" :title="t('dataset.prototype.buildingDataset')" :description="t('dataset.editor.asyncHint')"/>
      <template v-else><MfTable :columns="previewColumns" :data-source="rows" :loading="previewLoading" :empty-text="t('dataset.editor.emptyPreview')"/><div v-if="filteredRows.length" class="mt-4 flex justify-end"><MfPagination :current="page" :page-size="pageSize" :total="filteredRows.length" @change="(current:number,size:number)=>{page=current;pageSize=size}"/></div></template>
     </template>
     <MfTable v-else-if="activeTab==='fields'" :columns="fieldHeaders" :data-source="fields as any" row-key="id"><template #cell-type="{record}"><MfTag>{{ typeLabels[record.type] || record.type }}</MfTag></template><template #cell-mode="{record}">{{ modeLabels[record.mode] || record.mode }}</template><template #cell-original="{record}"><code class="text-xs">{{ record.virtualColumn?record.formula:record.original }}</code></template></MfTable>
     <pre v-else class="overflow-x-auto whitespace-pre-wrap break-all font-mono text-sm leading-7 text-content-secondary">{{ info.query }}</pre>
    </div>
   </section>
  </template>
 </div>
 <DatasetWizard v-if="editing" embedded :dataset-code="code" @close="editing=false" @submitted="edited"/>
 <DatasetHistory v-if="historyOpen" :is-visible="historyOpen" :info="info" @close="historyOpen=false"/>
</template>
