import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { h, nextTick, ref } from 'vue'
import { createI18n } from 'vue-i18n'
import { compileScript, parse } from 'vue/compiler-sfc'
import ts from 'typescript'
import { component } from '../../metaforge-ui/tests/component.mjs'
import { click, InteractionEvent, mountHost } from '../../metaforge-ui/tests/interaction-host.mjs'
const dataModule = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile = source => ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const apiUrl = dataModule(`export const state={params:{},query:{},pushes:[],saves:[],calls:[],guard:null,handle:async()=>({status:true})};export const request=(name,...args)=>{state.calls.push({name,args});return state.handle(name,...args)};`)
const routerUrl = dataModule(`import {state} from ${JSON.stringify(apiUrl)};export const useRoute=()=>({params:state.params,query:state.query});export const useRouter=()=>({push:async path=>state.pushes.push(path)});export const onBeforeRouteLeave=guard=>{state.guard=guard};`)
const utilityUrl = dataModule(compile(await readFile(new URL('../src/model/dataset-editor.ts', import.meta.url), 'utf8')))
const serviceUrl = name => dataModule(`import {state,request} from ${JSON.stringify(apiUrl)};export default new Proxy({}, {get:(_,method)=>(...args)=>{if(method==='saveOrUpdate')state.saves.push(args[0]);return request(${JSON.stringify(name)}+'.'+method,...args)}});`)
const stubUrl = dataModule(`import {h} from ${JSON.stringify(import.meta.resolve('vue'))};export const Ace={props:['value'],emits:['update:value'],setup(p,{emit}){return()=>h('textarea',{'aria-label':'SQL',value:p.value,onInput:e=>emit('update:value',e.target.value)})}};export const Picker={emits:['change','update:modelValue'],setup(_,{emit}){return()=>h('div',['A','B'].map(code=>h('button',{onClick:()=>{emit('update:modelValue','1:MySQL:'+code);emit('change','1:MySQL:'+code)}},'Source '+code)))}};export const Columns={props:['modelValue'],emits:['update:modelValue'],setup(p,{emit}){return()=>h('button',{onClick:()=>emit('update:modelValue',p.modelValue.map((column,index)=>({...column,orderByKey:index===0})))},'Set order key')}};export const MfMessage={success(){}};`)
async function localSfc(relative, mapping={}) {
 const filename=new URL(relative,import.meta.url)
 const {descriptor}=parse(await readFile(filename,'utf8'),{filename:filename.pathname})
 let source=compile(compileScript(descriptor,{id:relative,inlineTemplate:true}).content)
 source=source.replace(/from (['"])([^'"]+)\1/g,(_match,_quote,name)=>`from ${JSON.stringify(mapping[name] || import.meta.resolve(name))}`)
 return dataModule(source)
}
const shellUrl=await localSfc('../src/views/pages/admin/dataset/components/DatasetWizardShell.vue')
const previewTableUrl=await localSfc('../src/views/pages/admin/dataset/components/DatasetPreviewTable.vue')
const previewUrl=await localSfc('../src/views/pages/admin/dataset/components/DatasetSourcePreview.vue',{'./DatasetPreviewTable.vue':previewTableUrl})
if(!globalThis.requestAnimationFrame)globalThis.requestAnimationFrame=callback=>{queueMicrotask(()=>callback(0));return 0}
const filename = new URL('../src/views/pages/admin/dataset/DatasetWizard.vue', import.meta.url)
const { descriptor } = parse(await readFile(filename, 'utf8'), { filename: filename.pathname })
let source = compile(compileScript(descriptor, { id: 'dataset-wizard-test', inlineTemplate: true }).content)
const imports = { 'vue-router': routerUrl, 'metaforge-ui': stubUrl, '@/model/dataset-editor': utilityUrl,
  '@/views/components/editor/AceEditor.vue': dataModule(`export {Ace as default} from ${JSON.stringify(stubUrl)}`),
  '@/views/pages/admin/query/QuerySourcePicker.vue': dataModule(`export {Picker as default} from ${JSON.stringify(stubUrl)}`),
  './components/DatasetWizardShell.vue': shellUrl, './components/DatasetSourcePreview.vue':previewUrl, './components/DatasetPreviewTable.vue':previewTableUrl,
  './components/DatasetColumns.vue': dataModule(`export {Columns as default} from ${JSON.stringify(stubUrl)}`),
  ...Object.fromEntries(['dataset','source','metadata','plugin','execute'].map(name=>['@/services/'+name,serviceUrl(name)])) }
source = source.replace(/from (['"])([^'"]+)\1/g, (_match,_quote,name)=>`from ${JSON.stringify(imports[name] || import.meta.resolve(name))}`)
const { default: Wizard } = await import(dataModule(source))
const { state } = await import(apiUrl)
const components = Object.fromEntries(await Promise.all(['MfCheckbox','MfForm','MfFormItem','MfInput','MfTextarea','MfSelect','MfInputNumber','MfButton','MfCard','MfSpinner','MfAlert','MfTable','MfTag','MfModal','MfEmpty'].map(async name=>[name,await component(name)])))
const settle = () => new Promise(resolve=>setImmediate(resolve))
const success = data => ({status:true,data})
const field = { id:42, name:'record_id',aliasName:'ID',original:'id',comment:'',defaultValue:null,type:'NUMBER',mode:'DIMENSION',position:0,nullable:false,length:0,orderByKey:true,partitionKey:false,primaryKey:false,samplingKey:false,virtualColumn:false,customColumn:false }
function handler(name,...args) {
  if (name==='plugin.getPlugins') return success([{type:'EXECUTOR',name:'Local'},{type:'SCHEDULER',name:'Default'}])
  if (name==='source.getByCode') return success({id:1,code:args[0],name:'Source '+args[0],type:'MySQL'})
  if (name==='metadata.getDatabaseBySource') return success({isSuccessful:true,columns:[{object_name:'analytics'}]})
  if (name==='metadata.getTablesByDatabase') return success({isSuccessful:true,columns:[{object_name:'orders'}]})
  if (name==='metadata.queryTable') return success({isSuccessful:true,content:'SELECT * FROM orders'})
  if (name==='execute.execute') return success({isSuccessful:true,headers:['id'],columns:[]})
  if (name==='dataset.getByCode') return success({id:7,code:'DATASET',name:'Saved dataset',query:'select id from orders',source:{id:1,code:'A',name:'Source A',type:'MySQL'},executor:'Local',syncMode:'MANUAL'})
  if (name==='dataset.getColumnsByCode') return success([{...field}])
  return success({})
}
function mount(params={},query={},storage={},handleOverride=null,componentProps={}) {
  const previousSession = globalThis.sessionStorage
  const previousLocal = globalThis.localStorage
  const values = new Map(Object.entries(storage))
  globalThis.sessionStorage = {getItem:key=>values.get(key)||null,removeItem:key=>values.delete(key)}
  globalThis.localStorage = {getItem:()=>null,removeItem(){}}
  Object.assign(state,{params,query,pushes:[],saves:[],calls:[],guard:null,handle:handleOverride || (async(...args)=>handler(...args))})
  const host = mountHost(h(Wizard,componentProps),undefined,app=>{
    app.use(createI18n({legacy:false,locale:'en',missingWarn:false,fallbackWarn:false,messages:{en:{}}}))
    for (const [name,value] of Object.entries(components)) app.component(name,value)
  })
  return {...host,container:host.document.body,values,unmount(){host.unmount();globalThis.sessionStorage=previousSession;globalThis.localStorage=previousLocal}}
}
const button=(host,text)=>host.find(node=>node.tag==='button'&&node.textContent.trim().startsWith(text))
async function press(host,text) { const node=button(host,text); assert.ok(node,'Missing button '+text);assert.ok(!node.disabled);await click(host.document,node);await settle() }
async function input(host,node,value) {node.value=value;node.props.onInput(new InteractionEvent('input',[node]));await nextTick()}
async function fromQuery() {
  const host=mount({sourceCode:'A'},{tempId:'draft'},{DatasetQuery_draft:JSON.stringify({version:1,sourceCode:'A',query:'select id from orders',headers:['id']})})
  await settle()
  await input(host,host.find(node=>node.tag==='input'),'New dataset')
  await press(host,'dataset.editor.next')
  return host
}
test('query entry inherits source, executed SQL and fields; a zero-row result can be saved',async()=>{
  const host=await fromQuery()
  try {
    assert.equal(host.values.size,0)
    assert.equal(host.find(node=>node.tag==='textarea').value,'select id from orders')
    await press(host,'dataset.editor.next')
    assert.equal(state.calls.filter(call=>call.name==='execute.execute').length,1)
    await press(host,'Set order key')
    await press(host,'dataset.editor.next')
    assert.ok(host.container.textContent.includes('dataset.editor.emptyPreview'))
    await press(host,'common.save')
    assert.equal(state.saves[0].source.code,'A')
    assert.equal(state.saves[0].code,undefined)
    assert.equal(state.saves[0].query,'select id from orders')
    assert.equal(state.saves[0].columns[0].original,'id')
    assert.deepEqual(state.pushes,['/dataset'])
  } finally {host.unmount()}
})
test('failed preview preserves input and blocks progression; retry succeeds',async()=>{
  const host=await fromQuery()
  try {
    state.handle=async(name,...args)=>name==='execute.execute'?{status:false,message:'Query denied'}:handler(name,...args)
    await press(host,'dataset.editor.next')
    assert.ok(host.container.textContent.includes('Query denied'))
    assert.equal(host.find(node=>node.tag==='textarea').value,'select id from orders')
    assert.equal(state.saves.length,0)
    state.handle=async(...args)=>handler(...args)
    await press(host,'dataset.editor.next')
    assert.ok(button(host,'Set order key'))
  } finally {host.unmount()}
})
test('editing preserves dataset and column identities; failed saves preserve form and can retry',async()=>{
  const host=mount({code:'DATASET'})
  try {
    await settle()
    await press(host,'dataset.editor.next')
    await press(host,'dataset.editor.next')
    await press(host,'dataset.editor.next')
    state.handle=async(name,...args)=>name==='dataset.saveOrUpdate'?{status:false,message:'Save denied'}:handler(name,...args)
    await press(host,'common.save')
    assert.ok(host.container.textContent.includes('Save denied'))
    assert.equal(state.pushes.length,0)
    assert.equal(state.saves[0].columns[0].id,42)
    assert.equal(state.saves[0].columns[0].name,'record_id')
    assert.equal(state.saves[0].id,7)
    assert.equal(state.saves[0].code,'DATASET')
    state.handle=async(...args)=>handler(...args)
    await press(host,'common.save')
    assert.equal(state.saves.length,2)
    assert.deepEqual(state.pushes,['/dataset/detail/DATASET?submitted=1'])
  } finally {host.unmount()}
})
test('source changes invalidate in-flight previews, so stale responses cannot advance the wizard',async()=>{
  const host=await fromQuery()
  try {
    let resolve
    state.handle=(name,...args)=>name==='execute.execute'?new Promise(done=>{resolve=done}):Promise.resolve(handler(name,...args))
    await press(host,'dataset.editor.next')
    await press(host,'Source B')
    resolve(success({isSuccessful:true,headers:['stale'],columns:[]}))
    await settle()
    assert.ok(!button(host,'Set order key'))
    assert.equal(state.saves.length,0)
    state.handle=async(...args)=>handler(...args)
    await press(host,'dataset.editor.next')
    assert.equal(state.calls.filter(call=>call.name==='execute.execute').at(-1).args[0].name,'B')
    assert.ok(button(host,'Set order key'))
  } finally {host.unmount()}
})
test('unsaved navigation can be canceled or confirmed without losing entered values',async()=>{
  const host=mount()
  try {
    await settle()
    await input(host,host.find(node=>node.tag==='input'),'Keep me')
    const canceled=state.guard();await nextTick()
    await click(host.document,host.all(node=>node.tag==='button'&&node.textContent.trim()==='common.cancel').at(-1));await settle()
    assert.equal(await canceled,false)
    assert.equal(host.find(node=>node.tag==='input').value,'Keep me')
    const confirmed=state.guard();await nextTick()
    await press(host,'dataset.editor.discard')
    assert.equal(await confirmed,true)
  } finally {host.unmount()}
})

const columnsFile = new URL('../src/views/pages/admin/dataset/components/DatasetColumns.vue', import.meta.url)
const columnsDescriptor = parse(await readFile(columnsFile, 'utf8'), { filename: columnsFile.pathname }).descriptor
let columnSource = compile(compileScript(columnsDescriptor, { id: 'dataset-columns-test', inlineTemplate: true }).content)
columnSource = columnSource.replace(/from (['"])([^'"]+)\1/g, (_match,_quote,name)=>`from ${JSON.stringify(imports[name] || import.meta.resolve(name))}`)
const {default: Columns} = await import(dataModule(columnSource))
const extraComponents = Object.fromEntries(await Promise.all(['MfDrawer','MfSwitch','MfTooltip'].map(async name=>[name,await component(name)])))
function mountColumns() {
  state.handle=async(name,...args)=>name==='dataset.getColumnFunctions'?success({engine:'ClickHouse',functions:[{name:'round'}]}):handler(name,...args)
  state.calls=[]
  const model=ref([{...field},{...field,id:43,name:'second',aliasName:'Second',original:'second',position:1}])
  const host=mountHost(()=>h(Columns,{modelValue:model.value,'onUpdate:modelValue':value=>{model.value=value}}),undefined,app=>{
    app.use(createI18n({legacy:false,locale:'en',missingWarn:false,fallbackWarn:false,messages:{en:{}}}))
    for(const [name,value] of Object.entries({...components,...extraComponents}))app.component(name,value)
  })
  return {...host,model}
}
const inDialog=node=>{for(let parent=node.parent;parent;parent=parent.parent)if(parent.props.role==='dialog')return true;return false}
async function editFirst(host) { await click(host.document,host.find(node=>node.tag==='button'&&node.props['aria-label']==='dataset.editor.editColumn'));await settle() }
async function submitColumn(host) { const form=host.find(node=>node.tag==='form');await form.props.onSubmit(new InteractionEvent('submit',[form]));await settle() }

test('column drawer uses a draft: cancel leaves the persisted column untouched',async()=>{
 const host=mountColumns()
 try {
  await editFirst(host)
  const fields=host.all(node=>node.tag==='input')
  const name=fields.find(node=>node.value==='record_id')
  await input(host,name,'changed_name')
  assert.equal(host.model.value[0].name,'record_id')
  await press(host,'common.cancel')
  assert.equal(host.model.value[0].name,'record_id')
  await editFirst(host)
  assert.ok(host.all(node=>node.tag==='input').some(node=>node.value==='record_id'))
 } finally {host.unmount()}
})
test('column drawer blocks duplicate names and invalid sampling keys, then preserves identity on save',async()=>{
 const host=mountColumns()
 try {
  await editFirst(host)
  const name=host.all(node=>node.tag==='input').find(node=>node.value==='record_id')
  await input(host,name,'second')
  await submitColumn(host)
  assert.ok(host.document.body.textContent.includes('dataset.editor.duplicateName'))
  assert.equal(host.model.value[0].name,'record_id')
  await input(host,name,'renamed_id')
  const order=host.find(node=>node.props.role==='switch'&&inDialog(node)&&node.props['aria-label']==='dataset.common.columnIsOrderByKey')
  const sampling=host.find(node=>node.props.role==='switch'&&inDialog(node)&&node.props['aria-label']==='dataset.common.columnIsSampling')
  await click(host.document,order);await click(host.document,sampling)
  await submitColumn(host)
  assert.ok(host.document.body.textContent.includes('dataset.tip.validatorSampling'))
  await click(host.document,order)
  await submitColumn(host)
  assert.equal(host.model.value[0].id,42)
  assert.equal(host.model.value[0].name,'renamed_id')
  assert.equal(host.model.value[0].samplingKey,true)
 } finally {host.unmount()}
})
test('column reordering keeps stable identities and updates positions',async()=>{
 const host=mountColumns()
 try {
  await click(host.document,host.find(node=>node.tag==='button'&&node.props['aria-label']==='dataset.editor.moveDown'))
  assert.deepEqual(host.model.value.map(column=>column.id),[43,42])
  assert.deepEqual(host.model.value.map(column=>column.position),[0,1])
 } finally {host.unmount()}
})


test('missing executors are explained and refresh preserves form input and uses actual installed names',async()=>{
 const host=mount({}, {}, {}, async(name,...args)=>name==='plugin.getPlugins'?success([]):handler(name,...args))
 try {
  await settle()
  await input(host,host.find(node=>node.tag==='input'),'Keep this name')
  assert.ok(host.container.textContent.includes('dataset.editor.noExecutorsHint'))
  assert.ok(host.container.textContent.includes('dataset.editor.chooseExecutor'))
  await press(host,'dataset.editor.next')
  assert.ok(host.container.textContent.includes('dataset.editor.executorRequired'))
  state.handle=async(name,...args)=>name==='plugin.getPlugins'?success([{type:'EXECUTOR',name:'Seatunnel'},{type:'SCHEDULER',name:'LocalScheduler'}]):handler(name,...args)
  await press(host,'common.refresh')
  assert.equal(host.find(node=>node.tag==='input').value,'Keep this name')
  assert.ok(host.container.textContent.includes('Seatunnel'))
  assert.ok(!host.container.textContent.includes('dataset.editor.noExecutorsHint'))
  const syncMode = host.all(node=>node.tag==='div' && String(node.props.class).includes('mf-invalidable') && node.props.onClick)[1]
  await click(host.document,syncMode)
  await click(host.document,host.find(node=>node.props.role==='option' && node.textContent.trim()==='dataset.common.syncModeTiming'))
  assert.ok(host.container.textContent.includes('LocalScheduler'))
  assert.ok(!host.container.textContent.includes('Default'))
  // Return to manual mode so the missing cron expression does not block this test's next step.
  const syncModeAgain = host.all(node=>node.tag==='div' && String(node.props.class).includes('mf-invalidable') && node.props.onClick)[1]
  await click(host.document,syncModeAgain)
  await click(host.document,host.find(node=>node.props.role==='option' && node.textContent.trim()==='dataset.common.syncModeManual'))
  await press(host,'dataset.editor.next')
  assert.ok(button(host,'Source A'))
 } finally {host.unmount()}
})

test('editing shows an unavailable saved executor explicitly and never silently replaces it',async()=>{
 const host=mount({code:'DATASET'}, {}, {}, async(name,...args)=>name==='dataset.getByCode'?success({...handler(name,...args).data,executor:'UnavailableExecutor'}):handler(name,...args))
 try {
  await settle()
  assert.ok(host.container.textContent.includes('UnavailableExecutor (dataset.editor.pluginUnavailable)'))
  await press(host,'dataset.editor.next')
  assert.ok(host.container.textContent.includes('dataset.editor.executorRequired'))
  assert.equal(state.saves.length,0)
 } finally {host.unmount()}
})


test('virtual column expressions are required, checked by the server and committed only after success',async()=>{
 const host=mountColumns()
 try {
  await press(host,'dataset.editor.addColumn')
  await submitColumn(host)
  assert.equal(host.model.value.length,2)
  assert.ok(host.document.body.textContent.includes('dataset.editor.virtualExpressionRequired'))
  const expression=host.find(node=>node.tag==='textarea' && node.props.placeholder==='dataset.editor.expressionPlaceholder')
  await input(host,expression,'round(record_id, 2)')
  state.handle=async(name,...args)=>name==='dataset.validateColumns'?{status:false,message:'Unknown field'}:handler(name,...args)
  await submitColumn(host)
  assert.equal(host.model.value.length,2)
  assert.ok(host.document.body.textContent.includes('Unknown field'))
  state.handle=async(...args)=>handler(...args)
  await submitColumn(host)
  assert.equal(host.model.value.length,3)
  assert.equal(host.model.value[2].formula,'round(record_id, 2)')
  const request=state.calls.filter(call=>call.name==='dataset.validateColumns').at(-1)
  assert.equal(request.args[0].at(-1).virtualColumn,true)
  assert.equal(request.args[0].at(-1).formula,'round(record_id, 2)')
 } finally {host.unmount()}
})

test('function and field insertion compose a formula at the current cursor position',async()=>{
 const previousFrame=globalThis.requestAnimationFrame
 globalThis.requestAnimationFrame=callback=>{queueMicrotask(()=>callback(0));return 0}
 const host=mountColumns()
 try {
  await press(host,'dataset.editor.addColumn')
  const triggers=text=>host.all(node=>node.tag==='div' && String(node.props.class).includes('mf-invalidable') && node.props.onClick && String(node.textContent||'').includes(text))
  await click(host.document,triggers('dataset.editor.insertFunction')[0])
  await click(host.document,host.find(node=>node.props.role==='option' && node.textContent.trim()==='round'))
  const formula=host.find(node=>node.tag==='textarea' && node.props.placeholder==='dataset.editor.expressionPlaceholder')
  assert.equal(formula.value,'round()')
  formula.selectionStart=6
  formula.selectionEnd=6
  await click(host.document,triggers('dataset.editor.insertField')[0])
  await click(host.document,host.find(node=>node.props.role==='option' && node.textContent.trim()==='ID (record_id)'))
  assert.equal(formula.value,'round(`record_id`)')
  await submitColumn(host)
  assert.equal(host.model.value.at(-1).formula,'round(`record_id`)')
 } finally {host.unmount();if(previousFrame)globalThis.requestAnimationFrame=previousFrame;else delete globalThis.requestAnimationFrame}
})

const modeUrl=await localSfc('../src/views/pages/admin/dataset/components/DatasetCreateMode.vue')
const {default: CreationMode}=await import(modeUrl)
const filterUrl=dataModule(compile(await readFile(new URL('../src/model/filter.ts',import.meta.url),'utf8')))
const mappingUrl=await localSfc('../src/views/pages/admin/dataset/components/DatasetExistingMapping.vue',{'@/services/dataset':imports['@/services/dataset'],'@/model/filter':filterUrl})
const {default: ExistingMapping}=await import(mappingUrl)
const noopUrl=dataModule('export default {render:()=>null}')
const utilUrl=dataModule('export const useUtil=()=>({getDatasetStateText:value=>value})')
const detailUrl=await localSfc('../src/views/pages/admin/dataset/DatasetDetail.vue',{'vue-router':routerUrl,'@/services/dataset':imports['@/services/dataset'],'./DatasetWizard.vue':noopUrl,'./DatasetHistory.vue':noopUrl,'@/utils/common':utilUrl})
const {default: Detail}=await import(detailUrl)
function mountPage(Component,props={},handle=async(...args)=>handler(...args),params={}) {
 Object.assign(state,{params,query:{},pushes:[],saves:[],calls:[],guard:null,handle})
 return mountHost(h(Component,props),undefined,app=>{
  app.use(createI18n({legacy:false,locale:'en',missingWarn:false,fallbackWarn:false,messages:{en:{}}}))
  for(const [name,value] of Object.entries({...components,...extraComponents}))app.component(name,value)
 })
}
test('creation modes carry the selected SQL definition and allow manual creation without a result',async()=>{
 const creates=[]
 const result={origin:'result',sourceCode:'A',query:'select executed',headers:['id'],rows:[],rowCount:0}
 const sql={origin:'sql',sourceCode:'B',query:'select current'}
 const host=mountPage(CreationMode,{open:true,result,sql,onCreate:value=>creates.push(value)})
 try {
  await press(host,'dataset.prototype.sqlCreation')
  await press(host,'dataset.prototype.continueCreation')
  assert.deepEqual(creates,[sql])
  await press(host,'dataset.prototype.manualCreation')
  await press(host,'dataset.prototype.continueCreation')
  assert.deepEqual(creates[1],{origin:'manual'})
 } finally {host.unmount()}
})
test('inline result creation preserves the query page and reuses the executed field snapshot',async()=>{
 const submitted=[]
 const entry={origin:'result',sourceCode:'A',query:'select executed_id',headers:['id'],rows:[{id:1}],rowCount:1}
 const host=mount({}, {}, {},async(name,...args)=>name==='dataset.saveOrUpdate'?success({code:'CREATED'}):handler(name,...args),{embedded:true,entry,onSubmitted:value=>submitted.push(value)})
 try {
  await settle()
  await input(host,host.find(node=>node.tag==='input'),'Inline dataset')
  await press(host,'dataset.editor.next')
  await press(host,'dataset.editor.next')
  assert.equal(state.calls.filter(call=>call.name==='execute.execute').length,0)
  await press(host,'Set order key');await press(host,'dataset.editor.next');await press(host,'common.save')
  assert.deepEqual(state.pushes,[])
  assert.equal(submitted[0].code,'CREATED')
  assert.equal(state.saves[0].query,'select executed_id')
 } finally {host.unmount()}
})
test('inline alias edits and bulk settings update the actual columns while preserving identities',async()=>{
 const host=mountColumns()
 try {
  await input(host,host.find(node=>node.tag==='input' && node.value==='ID'),'Display ID')
  assert.equal(host.model.value[0].aliasName,'Display ID')
  const checkbox=host.all(node=>node.tag==='input' && node.props.type==='checkbox')[0]
  checkbox.checked=true;checkbox.props.onChange(new InteractionEvent('change',[checkbox]));await nextTick()
  await press(host,'dataset.prototype.bulkSettings')
  const trigger=host.find(node=>node.tag==='div' && inDialog(node) && String(node.props.class).includes('mf-invalidable') && node.props.onClick)
  await click(host.document,trigger)
  await click(host.document,host.find(node=>node.props.role==='option' && node.textContent.trim()==='dataset.common.columnModeMetric'))
  await press(host,'common.save')
  assert.ok(host.model.value.every(column=>column.mode==='METRIC'))
  assert.deepEqual(host.model.value.map(column=>column.id),[42,43])
 } finally {host.unmount()}
})
async function mappedPage(save=async()=>success({code:'DATASET'})) {
 const handle=async(name,...args)=>{
  if(name==='dataset.getAll')return success({content:[{code:'DATASET',name:'Saved dataset'}],page:1,totalPage:1})
  if(name==='dataset.saveOrUpdate')return save(...args)
  return handler(name,...args)
 }
 const submitted=[]
 const host=mountPage(ExistingMapping,{entry:{origin:'result',sourceCode:'B',query:'select id from incoming',headers:['id'],rows:[],rowCount:0},onSubmitted:value=>submitted.push(value)},handle)
 await settle()
 await click(host.document,host.find(node=>node.tag==='div' && String(node.props.class).includes('mf-invalidable') && node.props.onClick))
 await click(host.document,host.find(node=>node.props.role==='option' && node.textContent.trim()==='Saved dataset'))
 await settle()
 return {...host,submitted}
}
test('existing-dataset mapping replaces SQL using the original dataset and field identities',async()=>{
 const host=await mappedPage()
 try {
  await press(host,'dataset.prototype.confirmReplacement')
  assert.equal(state.saves[0].id,7)
  assert.equal(state.saves[0].code,'DATASET')
  assert.equal(state.saves[0].source.code,'B')
  assert.equal(state.saves[0].columns[0].id,42)
  assert.equal(state.saves[0].columns[0].original,'id')
  assert.equal(state.saves[0].query,'select id from incoming')
  assert.equal(host.submitted[0].code,'DATASET')
 } finally {host.unmount()}
})
test('a failed mapping save keeps the target and mapping available for retry',async()=>{
 const host=await mappedPage(async()=>({status:false,message:'Replacement failed'}))
 try {
  await press(host,'dataset.prototype.confirmReplacement')
  assert.ok(host.document.body.textContent.includes('Replacement failed'))
  assert.equal(host.submitted.length,0)
  assert.ok(!button(host,'dataset.prototype.confirmReplacement').disabled)
 } finally {host.unmount()}
})
test('dataset details preview stored rows through the dataset API, without rerunning the source SQL',async()=>{
 const host=mountPage(Detail,{},async(name,...args)=>{
  if(name==='dataset.getByCode')return success({...handler(name,...args).data,state:['TABLE_SUCCESS','COMPLETE_SUCCESS']})
  if(name==='dataset.preview')return success({headers:['ID'],columns:[{ID:123}]})
  return handler(name,...args)
 },{code:'DATASET'})
 try {
  await new Promise(resolve=>setTimeout(resolve,10));await settle()
  assert.ok(host.container.textContent.includes('Saved dataset'))
  assert.ok(host.container.textContent.includes('123'))
  assert.equal(state.calls.filter(call=>call.name==='dataset.preview')[0].args[0],'DATASET')
  assert.equal(state.calls.filter(call=>call.name==='execute.execute').length,0)
 } finally {host.unmount()}
})
test('dataset details do not claim a pending build is ready or query a table that is not built',async()=>{
 const host=mountPage(Detail,{},async(name,...args)=>name==='dataset.getByCode'?success({...handler(name,...args).data,state:['METADATA_START']}):handler(name,...args),{code:'DATASET'})
 try {
  await new Promise(resolve=>setTimeout(resolve,10));await settle()
  assert.ok(host.container.textContent.includes('dataset.prototype.buildingDataset'))
  assert.equal(state.calls.filter(call=>call.name==='dataset.preview').length,0)
  assert.equal(button(host,'dataset.common.adhoc').disabled,true)
 } finally {host.unmount()}
})


test('changing wizard steps resets the dialog scroll position to the next heading',async()=>{
 const host=await fromQuery()
 try {
  const body=host.find(node=>node.tag==='div' && String(node.props.class).includes('min-h-0 flex-1 overflow-y-auto'))
  body.scrollTop=200
  await press(host,'dataset.editor.next')
  assert.equal(body.scrollTop,0)
 } finally {host.unmount()}
})
