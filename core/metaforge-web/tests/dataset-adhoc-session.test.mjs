import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { createRenderer, nextTick, ref } from 'vue'
import ts from 'typescript'

const uri = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
async function compile(path, aliases = {}) {
  const source = await readFile(new URL('../src/' + path, import.meta.url), 'utf8')
  let { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } })
  for (const [from, to] of Object.entries(aliases)) outputText = outputText.replaceAll(`from '${from}'`, `from '${to}'`)
  return uri(outputText)
}
const datasetUri = uri('export const hooks = {}; export default {getByCode:(...args)=>hooks.info(...args),getColumnsByCode:(...args)=>hooks.columns(...args),adhoc:(...args)=>hooks.query(...args)}')
const reportUri = uri('export const hooks = {}; export default {getByCode:(...args)=>hooks.info(...args)}')
const dataset = (await import(datasetUri)).hooks, report = (await import(reportUri)).hooks
const filter = await compile('model/typed-filter.ts')
const chart = await compile('model/query-chart.ts', { './typed-filter': filter })
const model = await compile('model/dataset-adhoc.ts')
const { useDatasetAdhoc } = await import(await compile('composables/useDatasetAdhoc.ts', {
  vue: import.meta.resolve('vue'), '@/services/dataset': datasetUri, '@/services/report': reportUri,
  '@/model/dataset-adhoc': model, '@/model/query-chart': chart,
}))
const ok = data => ({ status: true, message: 'OK', data })
const amount = { id: 1, name: 'amount', type: 'NUMBER', mode: 'METRIC' }
const renderer = createRenderer({ insert(child, parent) { child.parent = parent }, remove() {}, createComment: () => ({}), createText: () => ({}),
  createElement: () => ({}), setText() {}, setElementText() {}, parentNode: node => node.parent, nextSibling: () => null, patchProp() {} })
function mount(reportCode = '') {
  dataset.info = async code => ok({ name: code })
  dataset.columns = async () => ok([structuredClone(amount)])
  const code = ref('demo'); let state
  const app = renderer.createApp({ setup() { state = useDatasetAdhoc(code, ref(reportCode), key => key); return () => null } })
  app.mount({})
  return { state, code, close: () => app.unmount() }
}
async function flush() { for (let i = 0; i < 8; i++) await Promise.resolve(); await nextTick() }

test('editing fields and chart settings does not execute a query; responses keep the submitted query identity', async () => {
  let calls = 0, resolve
  dataset.query = () => { calls++; return new Promise(done => { resolve = done }) }
  const { state, close } = mount(); await flush()
  state.add(amount)
  assert.equal(calls, 0)
  const running = state.execute()
  state.update({ ...state.columns.value[0], alias: 'changed' })
  resolve(ok({ isSuccessful: true, headers: ['amount'], columns: [[12]], content: 'SELECT SUM(amount)' }))
  await running
  assert.equal(state.rows.value[0].amount, 12)
  assert.equal(state.stale.value, true)
  assert.equal(state.canPublish.value, false)
  state.options.value.type = 'bar'; await flush()
  assert.equal(calls, 1)
  close()
})
test('a late response from a previous dataset never overwrites the newly opened dataset', async () => {
  let resolve
  dataset.query = () => new Promise(done => { resolve = done })
  const { state, code, close } = mount(); await flush(); state.add(amount)
  const oldRun = state.execute()
  code.value = 'next'; await flush()
  resolve(ok({ isSuccessful: true, headers: ['old'], columns: [[99]] })); await oldRun
  assert.equal(state.dataset.value.name, 'next')
  assert.deepEqual(state.rows.value, [])
  assert.equal(state.queried.value, false)
  close()
})
test('saved chart type and aggregation survive report restoration and re-query', async () => {
  report.info = async () => ok({ dataset: { code: 'demo' }, name: 'Sales', query: JSON.stringify({ columns: [{ id: 1, mode: 'METRIC', expression: 'SUM' }], limit: 50 }),
    configure: JSON.stringify({ queryChart: { version: 1, options: { type: 'bar', fields: ['amount'], metric: 'amount', category: 'amount', aggregation: 'avg' } } }) })
  dataset.query = async () => ok({ isSuccessful: true, headers: ['amount'], columns: [{ amount: 5 }] })
  const { state, close } = mount('report'); await flush()
  assert.equal(state.options.value.type, 'bar')
  assert.equal(state.options.value.aggregation, 'avg')
  assert.equal(state.resultTab.value, 'chart')
  assert.equal(state.canPublish.value, true)
  await state.execute()
  assert.equal(state.options.value.aggregation, 'avg')
  close()
})
test('a failed execution cannot leave successful rows available for publishing', async () => {
  dataset.query = async () => ({ status: false, message: 'Connection failed' })
  const { state, close } = mount(); await flush(); state.add(amount)
  await state.execute()
  assert.equal(state.failure.value, 'Connection failed')
  assert.deepEqual(state.rows.value, [])
  assert.equal(state.canPublish.value, false)
  assert.equal(state.resultTab.value, 'message')
  close()
})
