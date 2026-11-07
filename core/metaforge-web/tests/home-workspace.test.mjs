import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { effectScope } from 'vue'
import ts from 'typescript'

const uri = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
async function compile(path, aliases = {}) {
  const source = await readFile(new URL('../src/' + path, import.meta.url), 'utf8')
  let { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } })
  for (const [from, to] of Object.entries(aliases)) outputText = outputText.replaceAll(`from '${from}'`, `from '${to}'`)
  return uri(outputText)
}
const userUri = uri('export const hooks={};export default {getSourceCount:()=>hooks.sources(),getQueryCount:()=>hooks.queries()}')
const datasetUri = uri('export const hooks={};export default {getCount:()=>hooks.count(),getAll:filter=>hooks.datasets(filter)}')
const dashboardUri = uri('export const hooks={};export default {getAll:filter=>hooks.dashboards(filter)}')
const user = (await import(userUri)).hooks, dataset = (await import(datasetUri)).hooks, dashboard = (await import(dashboardUri)).hooks
const { useHomeWorkspace } = await import(await compile('composables/useHomeWorkspace.ts', {
  vue: import.meta.resolve('vue'), '@/services/user': userUri, '@/services/dataset': datasetUri,
  '@/services/dashboard': dashboardUri, '@/model/filter': await compile('model/filter.ts'),
}))
const ok = data => ({ status: true, data })
const page = (content = [], total = content.length) => ok({ content, total })
async function flush() { for (let i = 0; i < 8; i++) await Promise.resolve() }
function mount() {
  const scope = effectScope(), state = scope.run(() => useHomeWorkspace())
  return { state, close: () => scope.stop() }
}
function defaults() {
  user.sources = async () => ok(0); user.queries = async () => ok(0)
  dataset.count = async () => ok(0); dataset.datasets = async () => { throw Error("Home must not read dataset rows") }; dashboard.dashboards = async () => page()
}

test('zero resources remain real zero counts, and recent lists request a bounded sorted page', async () => {
  defaults(); const filters = []
  dashboard.dashboards = async filter => { filters.push(filter); return page() }
  const { state, close } = mount(); await flush()
  assert.equal(state.sources.value.value, 0)
  assert.equal(state.queries.value.value, 0)
  assert.equal(state.datasets.value.total, 0)
  assert.equal(state.visualizations.value.total, 0)
  assert.equal(state.loading.value, false)
  assert.equal(filters.length, 1)
  for (const filter of filters) {
    assert.equal(filter.page, 1); assert.equal(filter.size, 4)
    assert.deepEqual(filter.orders, [{ column: 'updateTime', order: 'DESC' }])
  }
  close()
})
test('a failed counter does not erase successfully loaded resources or pretend the count is zero', async () => {
  defaults(); user.sources = async () => ({ status: false, message: 'Unavailable' })
  user.queries = async () => ok(42)
  dataset.count = async () => ok(12)
  const { state, close } = mount(); await flush()
  assert.equal(state.sources.value.failed, true)
  assert.equal(state.sources.value.value, null)
  assert.equal(state.queries.value.value, 42)
  assert.equal(state.datasets.value.total, 12)
  assert.deepEqual(state.datasets.value.items, [])
  close()
})
test('a late dataset count cannot replace the results of a newer refresh', async () => {
  defaults(); let resolve
  dataset.count = () => new Promise(done => { resolve = done })
  const { state, close } = mount(); await flush()
  dataset.count = async () => ok(1)
  await state.loadResources('datasets')
  resolve(ok(99)); await flush()
  assert.deepEqual(state.datasets.value.items, [])
  assert.equal(state.datasets.value.total, 1)
  close()
})
test('retrying one list does not reload unrelated counters', async () => {
  defaults(); let sourceCalls = 0
  user.sources = async () => { sourceCalls++; return ok(3) }
  dataset.count = async () => { throw Error('Offline') }
  const { state, close } = mount(); await flush()
  assert.equal(state.datasets.value.failed, true)
  dataset.count = async () => ok(3)
  await state.loadResources('datasets')
  assert.equal(state.datasets.value.failed, false)
  assert.equal(state.datasets.value.total, 3)
  assert.equal(sourceCalls, 1)
  close()
})
test('disposed home requests cannot update their state after navigation', async () => {
  defaults(); let resolve
  user.sources = () => new Promise(done => { resolve = done })
  const { state, close } = mount(); const before = state.sources.value
  close(); resolve(ok(500)); await flush()
  assert.equal(state.sources.value, before)
})
test('invalid counts are unavailable instead of being silently coerced', async () => {
  for (const count of [-1, 1.5, '4', null]) {
    defaults(); user.sources = async () => ok(count)
    const { state, close } = mount(); await flush()
    assert.equal(state.sources.value.value, null)
    assert.equal(state.sources.value.failed, true)
    close()
  }
})
test('malformed resource identities produce a failure state instead of a false empty list', async () => {
  defaults(); dashboard.dashboards = async () => page([{ name: 'Missing code' }], 1)
  const { state, close } = mount(); await flush()
  assert.equal(state.visualizations.value.failed, true)
  assert.equal(state.visualizations.value.total, null)
  close()
})

test('a valid dataset count stays available even when legacy dataset rows cannot be read', async () => {
  defaults(); let listCalls = 0, countCalls = 0
  dataset.count = async () => { countCalls++; return ok(3) }
  dataset.datasets = async () => { listCalls++; return page([{ code: 'one' }, { code: null }, { code: 'three' }], 3) }
  const { state, close } = mount(); await flush()
  assert.equal(state.datasets.value.total, 3)
  assert.equal(state.datasets.value.failed, false)
  assert.equal(countCalls, 1); assert.equal(listCalls, 0)
  close()
})
test('invalid or failed dataset counts stay unavailable and are never converted to zero', async () => {
  for (const response of [ok(-1), ok('3'), ok(null), ok(1.5), { status: false, data: 0 }]) {
    defaults(); dataset.count = async () => response
    const { state, close } = mount(); await flush()
    assert.equal(state.datasets.value.total, null); assert.equal(state.datasets.value.failed, true)
    close()
  }
})
