import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import test from 'node:test'
import {h,nextTick,ref} from 'vue'
import ts from 'typescript'
import {mountHost} from '../../metaforge-ui/tests/interaction-host.mjs'
const url=source=>`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const compile=source=>ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.ESNext}}).outputText
const vue=import.meta.resolve('vue')
const routeUrl=url(`import {reactive} from ${JSON.stringify(vue)};export const route=reactive({params:{source:'A',database:'0'},query:{},meta:{type:'keys'}});export const useSourceManageRoute=()=>route;export const useRouter=()=>({push:async location=>{route.query=Object.fromEntries(Object.entries(location.query).filter(([,value])=>value!==undefined))},replace:async location=>{route.query=Object.fromEntries(Object.entries(location.query).filter(([,value])=>value!==undefined))}})`)
const serviceUrl=url(`export const state={calls:[],browse:async()=>({status:true,data:{entries:[],hasMore:false}}),detail:async(_id,request)=>({status:true,data:{entry:{id:request.key,name:request.key,type:'string',attributes:{},size:1},content:'value',rows:[],hasMore:false}})};export default {nativeBrowse(id,request){state.calls.push({operation:'browse',id,request});return state.browse(id,request)},nativeDetail(id,request){state.calls.push({operation:'detail',id,request});return state.detail(id,request)}}`)
let model=compile(await readFile(new URL('../src/model/source-native.ts',import.meta.url),'utf8'))
const modelUrl=url(model)
let source=compile(await readFile(new URL('../src/composables/useNativeBrowser.ts',import.meta.url),'utf8'))
const imports={'vue-router':routeUrl,'./useSourceManageRoute':routeUrl,'@/services/metadata':serviceUrl,'@/model/source-native':modelUrl,'vue-i18n':url(`import {ref} from ${JSON.stringify(vue)};export const useI18n=()=>({t:key=>key,locale:ref('en')})`),'metaforge-ui':url('export const MfMessage={error(){}}')}
source=source.replace(/from (['"])([^'"]+)\1/g,(_m,_q,name)=>`from ${JSON.stringify(imports[name]||import.meta.resolve(name))}`)
const {useNativeBrowser}=await import(url(source))
const {route}=await import(routeUrl)
const {state}=await import(serviceUrl)
const settle=async()=>{await new Promise(resolve=>setImmediate(resolve));await nextTick()}
function mount(storage=false){
  route.params.source='A';route.params.database=storage?'bucket':'0';route.query={};state.calls=[]
  state.browse=async()=>({status:true,data:{entries:[],hasMore:false}})
  state.detail=async(_id,request)=>({status:true,data:{entry:{id:request.key,name:request.key,type:'string',attributes:{},size:1},content:'value',rows:[],hasMore:false}})
  let hook
  const panelOpen=ref(false)
  const host=mountHost(h({setup(){hook=useNativeBrowser();return()=>h('div')}}),undefined,app=>{
    app.provide('metadataCapabilityState',{profile:ref({kind:storage?'storage':'key-value'}),can:()=>true})
    app.provide('metadataFieldPanelOpen',panelOpen)
  })
  return {host,hook,panelOpen}
}

test('SCAN continuation is sent back verbatim, including an empty intermediate batch',async()=>{
  const {host,hook}=mount()
  try{
    await settle()
    state.browse=async(_id,request)=>({status:true,data:{entries:[],hasMore:request.cursor!=='42',nextCursor:request.cursor!=='42'?'42':null}})
    hook.resetList();await settle()
    assert.equal(hook.hasMore.value,true)
    hook.next();await settle()
    assert.equal(state.calls.at(-1).request.cursor,'42')
    assert.equal(hook.page.value,2)
    hook.previous();await settle();assert.equal(state.calls.at(-1).request.cursor,'')
    assert.equal(hook.page.value,1)
  }finally{host.unmount()}
})

test('older source responses never overwrite a different source or its loading state',async()=>{
  const {host,hook}=mount()
  try{
    await settle();let finish
    state.browse=id=>id==='A'?new Promise(resolve=>{finish=resolve}):Promise.resolve({status:true,data:{entries:[{id:'B',name:'B',type:'string'}],hasMore:false}})
    hook.resetList();route.params.source='B';await settle()
    finish({status:true,data:{entries:[{id:'A',name:'A',type:'string'}],hasMore:false}});await settle()
    assert.equal(hook.entries.value[0].id,'B');assert.equal(hook.loading.value,false)
  }finally{host.unmount()}
})

test('rapid selections and closing a detail pane cannot reveal a previous key value',async()=>{
  const {host,hook,panelOpen}=mount()
  try{
    await settle();let first
    state.detail=(_id,request)=>request.key==='first'?new Promise(resolve=>{first=resolve}):Promise.resolve({status:true,data:{entry:{id:'second',name:'second',type:'string'},content:'new',rows:[],hasMore:false}})
    hook.open({id:'first',name:'first',type:'string'});await settle()
    hook.open({id:'second',name:'second',type:'string'});await settle()
    first({status:true,data:{entry:{id:'first',name:'first',type:'string'},content:'old',rows:[],hasMore:false}});await settle()
    assert.equal(hook.detail.value.content,'new');assert.equal(panelOpen.value,true)
    hook.closeDetail();await settle();assert.equal(panelOpen.value,false);assert.equal(hook.detail.value,null)
  }finally{host.unmount()}
})

test('opening a directory changes its prefix without treating it as an object',async()=>{
  const {host,hook}=mount(true)
  try{
    await settle();hook.open({id:'资料/2026/',name:'2026/',type:'folder'});await settle()
    assert.equal(route.query.prefix,'资料/2026/')
    assert.equal(state.calls.at(-1).request.prefix,'资料/2026/')
    assert.equal(state.calls.some(call=>call.operation==='detail'),false)
    hook.up();await settle();assert.equal(route.query.prefix,'资料/')
  }finally{host.unmount()}
})
