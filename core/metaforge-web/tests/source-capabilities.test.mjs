import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import test from 'node:test'
import {createPinia,setActivePinia} from 'pinia'
import ts from 'typescript'
const url=source=>`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const service=url('export const state={calls:[],request:()=>Promise.resolve({status:true,data:{kind:"key-value",capabilities:["tab:sql","query:execute"]}})};export default {getCapabilities(id){state.calls.push(id);return state.request(id)}}')
const source=await readFile(new URL('../src/stores/source-capabilities.ts',import.meta.url),'utf8')
let {outputText}=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.ESNext}})
outputText=outputText.replace(/from (['"])([^'"]+)\1/g,(_m,_q,name)=>`from ${JSON.stringify(name==='@/services/metadata'?service:import.meta.resolve(name))}`)
const {useSourceCapabilitiesStore}=await import(url(outputText))
const {state}=await import(service)

test('parallel header/workspace loads share a request and profiles stay scoped to the source',async()=>{
  setActivePinia(createPinia());const store=useSourceCapabilitiesStore();state.calls=[]
  state.request=async id=>({status:true,data:{kind:id==='redis'?'key-value':'storage',capabilities:['tab:sql','query:execute']}})
  await Promise.all([store.load('redis'),store.load('redis'),store.load('oss')])
  assert.deepEqual(state.calls,['redis','oss'])
  assert.equal(store.entries.redis.profile.kind,'key-value')
  assert.equal(store.entries.oss.profile.kind,'storage')
  assert.equal(store.entries.redis.profile.capabilities.includes('column:modify'),false)
  await store.load('redis');assert.equal(state.calls.length,2)
})

test('failed or malformed capability data never enables operations and can be retried',async()=>{
  setActivePinia(createPinia());const store=useSourceCapabilitiesStore()
  state.request=async()=>({status:true,data:{capabilities:'all'}})
  await store.load('source');assert.equal(store.entries.source.profile,null);assert.ok(store.entries.source.error)
  state.request=async()=>({status:true,data:{kind:'future-family',capabilities:['tab:sql',null,3,'tab:sql','*']}})
  await store.load('source')
  assert.deepEqual(store.entries.source.profile,{kind:'unknown',capabilities:['tab:sql']})
  assert.equal(store.entries.source.loading,false)
})


test('a successful legacy metadata response cannot become a red Query successful error',async()=>{
  setActivePinia(createPinia());const store=useSourceCapabilitiesStore()
  state.request=async()=>({status:true,message:'Query successful',data:{isSuccessful:true,columns:[]}})
  await store.load('legacy')
  assert.equal(store.entries.legacy.contractError,true)
  assert.notEqual(store.entries.legacy.error,'Query successful')
  assert.equal(store.entries.legacy.profile,null)
})
