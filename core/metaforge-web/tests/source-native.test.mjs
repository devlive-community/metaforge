import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source=await readFile(new URL('../src/model/source-native.ts',import.meta.url),'utf8')
const {outputText}=ts.transpileModule(source,{compilerOptions:{target:ts.ScriptTarget.ES2020,module:ts.ModuleKind.ESNext}})
const {nativePage,nativeDetail,parentObjectPrefix,safeDownloadUrl}=await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('empty scan batches can still have a valid continuation and are not mistaken for the end',()=>{
  const page=nativePage({status:true,data:{entries:[],hasMore:true,nextCursor:'42'}})
  assert.equal(page.hasMore,true)
  assert.equal(page.nextCursor,'42')
  assert.throws(()=>nativePage({status:true,data:{entries:[],hasMore:true}}),/Invalid/)
})

test('success envelopes cannot hide a missing native payload or a backend failure',()=>{
  assert.throws(()=>nativePage({status:true,message:'Query successful',data:{isSuccessful:true,columns:[]}}),/Invalid native/)
  assert.throws(()=>nativeDetail({status:false,message:'Key expired'}),/Key expired/)
  const detail=nativeDetail({status:true,data:{entry:{id:'key'},rows:[],content:'',hasMore:false}})
  assert.equal(detail.content,'')
})

test('directory parents preserve literal object prefixes and unicode',()=>{
  assert.equal(parentObjectPrefix('资料/2026/'),'资料/')
  assert.equal(parentObjectPrefix('资料/'),'')
  assert.equal(parentObjectPrefix('docs/file'),'docs/')
  assert.equal(parentObjectPrefix(''),'')
})

test('signed download links preserve parameters and reject executable URL schemes',()=>{
  assert.equal(safeDownloadUrl('https://objects.example.test/a%20b?signature=x%2By'),'https://objects.example.test/a%20b?signature=x%2By')
  for(const value of ['javascript:alert(1)','data:text/html,test','file:///tmp/file',null])assert.throws(()=>safeDownloadUrl(value))
})
