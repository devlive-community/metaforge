import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-workspace-layout.ts', import.meta.url), 'utf8')
const {outputText} = ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.ESNext,target:ts.ScriptTarget.ES2020}})
const {readSourceWorkspaceLayout,fitSourceWorkspaceLayout} = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('untrusted layout preferences cannot restore huge, negative or invalid panel sizes',()=>{
  for(const raw of [null,'bad json','null','[]','{"left":"9999","right":false}']) assert.deepEqual(readSourceWorkspaceLayout(raw),{left:254,right:324})
  assert.deepEqual(readSourceWorkspaceLayout('{"left":-100,"right":99999}'),{left:180,right:560})
})

test('all panel combinations leave the center visible without overflowing the workspace',()=>{
  for(const width of [320,640,768,1024,1280,1536,2560]){
    for(const left of [180,254,480,9999])for(const right of [280,324,560,9999])for(const open of [false,true]){
      const fitted=fitSourceWorkspaceLayout(width,left,right,open)
      const available=width-(open?32:16)
      const center=available-fitted.left-fitted.right
      assert.ok(center>=Math.min(360,available*.35)-1,`${width}: center ${center}`)
      assert.ok(fitted.left>=fitted.leftMin && fitted.left<=fitted.leftMax)
      assert.ok(fitted.right>=fitted.rightMin && fitted.right<=fitted.rightMax)
      assert.ok(center+fitted.left+fitted.right<=width)
    }
  }
})

test('closing the editor frees the center without losing its preferred width',()=>{
  const open=fitSourceWorkspaceLayout(1536,300,420,true)
  const closed=fitSourceWorkspaceLayout(1536,300,420,false)
  assert.equal(open.left,300)
  assert.equal(open.right,420)
  assert.equal(closed.left,300)
  assert.equal(closed.right,0)
  assert.equal(fitSourceWorkspaceLayout(1536,300,420,true).right,420)
})
