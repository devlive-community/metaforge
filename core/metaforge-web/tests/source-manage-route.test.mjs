import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
import {createRouter,createMemoryHistory} from 'vue-router'
const source=await readFile(new URL('../src/model/source-manage.ts',import.meta.url),'utf8')
const {outputText}=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.ESNext}})
const {sourceManageContext,sourceManageLocation,isSourceManagementRoute}=await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('manage parameters round-trip names containing spaces, slashes, unicode and query delimiters',async()=>{
  const router=createRouter({history:createMemoryHistory(),routes:[{path:'/manage',component:{render:()=>null}}]})
  const expected={source:'5202f',database:'库 /+&',table:'表?name#part',tab:'structure'}
  await router.push(sourceManageLocation(expected))
  assert.deepEqual(sourceManageContext(router.currentRoute.value),expected)
  assert.equal(router.currentRoute.value.path,'/manage')
})

test('malformed repeated query values never become source identifiers',()=>{
  assert.equal(sourceManageContext({path:'/manage',params:{},query:{type:'source',id:['a','b'],database:['wrong']}}).source,'')
})

test('legacy parameter context and new query context represent the same source',()=>{
  const legacy=sourceManageContext({path:'/source/S/d/DB/t/data/T',params:{source:'S',database:'DB',table:'T'},meta:{type:'data'}})
  const current=sourceManageContext({path:'/manage',params:{},query:{type:'source',id:'S',database:'DB',table:'T',tab:'data'}})
  assert.deepEqual(legacy,current)
  assert.equal(sourceManageLocation(current,{limit:'10'}).query.limit,'10')
})


test('the list and historical URLs use the global header, only a valid manage route uses the workbench header',()=>{
  for(const route of [{path:'/source',query:{}},{path:'/source/code',query:{}},{path:'/manage',query:{type:'source'}},{path:'/manage',query:{type:'source',id:['wrong']}}])assert.equal(isSourceManagementRoute(route),false)
  assert.equal(isSourceManagementRoute({path:'/manage',query:{type:'source',id:'source-code'}}),true)
})
