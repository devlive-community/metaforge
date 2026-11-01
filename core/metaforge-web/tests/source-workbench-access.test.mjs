import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const url = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
const transpile = async path => ts.transpileModule(await readFile(new URL(path, import.meta.url), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } }).outputText
const model = url(await transpile('../src/model/source-manage.ts'))
const store = url('export const state={profiles:{},loads:[]};export const useSourceCapabilitiesStore=()=>({entries:new Proxy({},{get:(_t,code)=>code in state.profiles?{profile:state.profiles[code]}:undefined}),load:async code=>{state.loads.push(code)}})')
const guard = (await transpile('../src/router/workbench.ts')).replace(/from (['"])([^'"]+)\1/g, (_m, _q, name) => `from ${JSON.stringify(name === '@/model/source-manage' ? model : name === '@/stores/source-capabilities' ? store : name)}`)
const { canManageSource, canOpenWorkbenchPage, compareExportCapabilities, pageCapabilitiesMet } = await import(model)
const { workbenchGuard } = await import(url(guard))
const { state } = await import(store)

const RELATIONAL = ['query:execute', 'database:list', 'table:list', 'column:read', 'index:read', 'tab:structure', 'tab:sql']
const FULL = [...RELATIONAL, 'table:read', 'table:statement', 'table:export', 'data:insert', 'database:info', 'tab:er-diagram']

test('a source declaring no capability has no management at all', () => {
  assert.equal(canManageSource([]), false)
  for (const path of ['/manage', '/import', '/export', '/compare', '/er']) assert.equal(canOpenWorkbenchPage(path, []), false)
  assert.equal(canManageSource(['query:execute', 'tab:sql']), true)
  assert.equal(canOpenWorkbenchPage('/manage', ['query:execute', 'tab:sql']), true)
})

test('each workbench page opens only with the capabilities its server calls need', () => {
  assert.equal(canOpenWorkbenchPage('/compare', RELATIONAL), true)
  assert.equal(canOpenWorkbenchPage('/import', RELATIONAL), false)
  assert.equal(canOpenWorkbenchPage('/export', RELATIONAL), false)
  assert.equal(canOpenWorkbenchPage('/er', RELATIONAL), false)
  for (const path of ['/import', '/export', '/er']) assert.equal(canOpenWorkbenchPage(path, FULL), true)
  assert.equal(pageCapabilitiesMet('/compare', capability => RELATIONAL.includes(capability)), true)
  assert.equal(pageCapabilitiesMet('/compare', capability => capability !== 'table:list'), false)
  assert.deepEqual(compareExportCapabilities('structure', 'sql'), ['table:statement'])
  assert.deepEqual(compareExportCapabilities('structure', 'csv'), [])
  assert.deepEqual(compareExportCapabilities('data', 'csv'), ['table:read'])
})

test('opening a page by URL falls back to the console or the source list', async () => {
  state.profiles = { full: { capabilities: FULL }, relational: { capabilities: RELATIONAL }, query: { capabilities: ['query:execute', 'tab:sql'] }, none: { capabilities: [] } }
  assert.equal(await workbenchGuard({ path: '/er', query: { id: 'full', database: 'app' } }), true)
  assert.deepEqual(await workbenchGuard({ path: '/er', query: { id: 'relational', database: 'app' } }), { path: '/manage', query: { type: 'source', id: 'relational', database: 'app' } })
  assert.deepEqual(await workbenchGuard({ path: '/export', query: { source: 'query' } }), { path: '/manage', query: { type: 'source', id: 'query' } })
  assert.equal(await workbenchGuard({ path: '/manage', query: { type: 'source', id: 'none' } }), '/source')
  assert.equal(await workbenchGuard({ path: '/import', query: { id: 'none' } }), '/source')
  assert.deepEqual(await workbenchGuard({ path: '/compare', query: { id: 'relational', database: 'app', table: 't', mode: 'data' } }),
      { path: '/compare', query: { id: 'relational', database: 'app', table: 't', mode: 'structure' } })
  assert.equal(await workbenchGuard({ path: '/compare', query: { id: 'full', database: 'app', table: 't', mode: 'data' } }), true)
})

test('pages without a source, other pages and failed loads are left to the page', async () => {
  state.loads = []
  assert.equal(await workbenchGuard({ path: '/source', query: { id: 'none' } }), true)
  assert.equal(await workbenchGuard({ path: '/manage', query: { type: 'dataset', id: 'none' } }), true)
  assert.equal(await workbenchGuard({ path: '/compare', query: {} }), true)
  assert.deepEqual(state.loads, [])
  assert.equal(await workbenchGuard({ path: '/er', query: { id: 'unreachable' } }), true)
  assert.deepEqual(state.loads, ['unreachable'])
})
