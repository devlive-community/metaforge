import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import ts from 'typescript'
import { mountHost } from '../../metaforge-ui/tests/interaction-host.mjs'
const moduleUrl = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const vueUrl = import.meta.resolve('vue')
const routerUrl = moduleUrl(`import {reactive} from ${JSON.stringify(vueUrl)}; export const route=reactive({params:{source:'demo',database:'db',table:'orders'}});export const useRoute=()=>route;export const useSourceManageRoute=()=>route`)
const serviceUrl = moduleUrl(`export const state={calls:[],responses:{}};const request=(name,args)=>{state.calls.push({name,args});return state.responses[name]?.(...args)||Promise.resolve({status:true,data:{isSuccessful:true,columns:['DECIMAL','VARCHAR','BIGINT']}})};export default Object.fromEntries(['getDataTypes','createColumn','changeColumn','createIndex','dropColumn','getColumnDependencies'].map(name=>[name,(...args)=>request(name,args)]))`)
const uiUrl = moduleUrl('export const notices=[];export const MfMessage=Object.fromEntries(["success","warning","error"].map(name=>[name,message=>notices.push({name,message})]))')
async function local(path, imports = {}) {
  let code = compile(await readFile(new URL('../src/'+path, import.meta.url), 'utf8'))
  code = code.replace(/from (['"])([^'"]+)\1/g, (_m, _q, name) => `from ${JSON.stringify(imports[name] || import.meta.resolve(name))}`)
  return moduleUrl(code)
}
const sourceUrl = await local('model/source-column.ts')
const hookUrl = await local('composables/useSourceTableStructure.ts', {
  'vue-router':routerUrl,'@/composables/useSourceManageRoute':routerUrl,'vue-i18n':moduleUrl('export const useI18n=()=>({t:key=>key})'),'metaforge-ui':uiUrl,
  '@/services/metadata':serviceUrl,'@/model/source-column':sourceUrl,'@/model/column-dependency':await local('model/column-dependency.ts'),
  './useRowSelection':await local('composables/useRowSelection.ts'),'./useDragSort':await local('composables/useDragSort.ts'),'./useExecutedSql':await local('composables/useExecutedSql.ts',{'@/model/executed-sql':await local('model/executed-sql.ts')}),'./usePanelState':await local('composables/usePanelState.ts'),
  './useWorkspaceLoading':await local('composables/useWorkspaceLoading.ts'),
})
const {useSourceTableStructure} = await import(hookUrl)
const {state} = await import(serviceUrl)
const {notices} = await import(uiUrl)
const {route} = await import(routerUrl)
const settle = async () => { await new Promise(resolve=>setImmediate(resolve));await nextTick() }
function mount(allowed = true) {
  route.params.table='orders';state.calls=[];state.responses={};notices.length=0
  let hook
  const refresh=ref(0)
  const host=mountHost(h({setup(){hook=useSourceTableStructure();return ()=>h('div')}}),undefined,app=>{
    app.provide('metadataColumnRows',ref([{type_name:'column',object_name:'amount',object_data_type:'decimal(12,3)',object_nullable:'NO',object_default_value:'',object_position:1,object_charset:'utf8mb4',object_collation:'utf8mb4_bin'},{type_name:'column',object_name:'id',object_data_type:'BIGINT',object_position:2}]))
    app.provide('metadataCan',()=>allowed)
    app.provide('metadataLoading',ref(false));app.provide('metadataRefreshKey',refresh)
  })
  return {host,hook,refresh}
}

test('an inner DDL failure preserves the form and never reports success',async()=>{
  const {host,hook,refresh}=mount()
  try {
    await settle();hook.openPanel('add');hook.panelForm.value.name='new_column'
    state.responses.createColumn=async()=>({status:true,data:{isSuccessful:false,message:'Permission denied'}})
    await hook.onSubmitPanel()
    assert.equal(hook.panelVisible.value,true)
    assert.equal(hook.panelForm.value.name,'new_column')
    assert.equal(hook.panelIssues.value.name,'Permission denied')
    assert.equal(refresh.value,0)
    assert.equal(notices.some(item=>item.name==='success'),false)
  }finally{host.unmount()}
})

test('retrying a failed index never creates the already saved column twice',async()=>{
  const {host,hook,refresh}=mount()
  try{
    await settle();hook.openPanel('add');Object.assign(hook.panelForm.value,{name:'new_column',createIndex:true,indexName:'idx_new'})
    state.responses.createIndex=async()=>({status:true,data:{isSuccessful:false,message:'Index failed'}})
    await hook.onSubmitPanel()
    assert.equal(hook.indexOnly.value,true)
    assert.equal(hook.panelVisible.value,true)
    state.responses.createIndex=async()=>({status:true,data:{isSuccessful:true}})
    await hook.onSubmitPanel()
    assert.equal(state.calls.filter(call=>call.name==='createColumn').length,1)
    assert.equal(state.calls.filter(call=>call.name==='createIndex').length,2)
    assert.equal(state.calls.filter(call=>call.name==='changeColumn').length,0)
    assert.equal(hook.panelVisible.value,false)
    assert.equal(refresh.value,2)
  }finally{host.unmount()}
})

test('moving a field preserves precision, empty default and charset',async()=>{
  const {host,hook}=mount()
  try{
    await settle();await hook.onMove(1)
    assert.equal(hook.executed.value.operation,'source.executed.reorderField')
    assert.equal(hook.executed.value.target.value,'amount')
    const payload=state.calls.find(call=>call.name==='changeColumn').args[3].columns[0]
    assert.deepEqual({precision:payload.precision,scale:payload.scale,defaultValue:payload.defaultValue,charset:payload.charset,collate:payload.collate,after:payload.after},{precision:12,scale:3,defaultValue:'',charset:'utf8mb4',collate:'utf8mb4_bin',after:'id'})
  }finally{host.unmount()}
})

test('a pending save cannot refresh or reopen a different table',async()=>{
  const {host,hook,refresh}=mount()
  try{
    await settle();let finish
    state.responses.createColumn=()=>new Promise(resolve=>{finish=resolve})
    hook.openPanel('add');hook.panelForm.value.name='new_column'
    const pending=hook.onSubmitPanel()
    route.params.table='other';await nextTick()
    finish({status:true,data:{isSuccessful:true}});await pending
    assert.equal(hook.panelVisible.value,false)
    assert.equal(refresh.value,0)
    assert.equal(notices.some(item=>item.name==='success'),false)
  }finally{host.unmount()}
})


test('unsupported operations never open an editor or send DDL requests',async()=>{
  const {host,hook}=mount(false)
  try{
    await settle();hook.openPanel('add');assert.equal(hook.panelVisible.value,false)
    await hook.onMove(1)
    hook.confirmDrop(['amount']);await hook.onDropFields()
    assert.equal(state.calls.length,0)
  }finally{host.unmount()}
})

test('dropping looks up dependencies first and forces the drop only when asked',async()=>{
  const {host,hook}=mount()
  try{
    await settle()
    state.responses.getColumnDependencies=()=>Promise.resolve({status:true,data:{isSuccessful:true,columns:[{object_type:'index',object_name:'idx_amount',object_kind:'NORMAL',object_columns:'amount',object_schema:'db',object_table:'orders'},{object_type:'unknown',object_name:'x'}]}})
    await hook.confirmDrop(['amount'])
    assert.equal(hook.dropVisible.value,true)
    assert.deepEqual(hook.dropNames.value,['amount'])
    assert.deepEqual(hook.dropDependencies.value.map(item=>item.name),['idx_amount'])
    assert.equal(state.calls.some(call=>call.name==='dropColumn'),false)
    await hook.onDropFields(true)
    const drop=state.calls.find(call=>call.name==='dropColumn').args[3]
    assert.deepEqual({columns:drop.columns,force:drop.force},{columns:[{name:'amount'}],force:true})
    assert.equal(hook.dropVisible.value,false)
  }finally{host.unmount()}
})
