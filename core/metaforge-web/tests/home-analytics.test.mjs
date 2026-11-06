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
const apiUri = uri('export const hooks={};export default {getHomeActivity:days=>hooks.activity(days),getAll:filter=>hooks.queries(filter)}')
const hooks = (await import(apiUri)).hooks
const { useHomeAnalytics } = await import(await compile('composables/useHomeAnalytics.ts', {
  vue: import.meta.resolve('vue'), '@/services/user': apiUri, '@/services/audit': apiUri, '@/model/filter': await compile('model/filter.ts'),
}))
const ok = data => ({ status: true, data })
const activity = days => ({ days, total: 0, trend: Array.from({ length: days }, (_, i) => ({ date: `day-${i}`, count: 0 })), sources: [] })
async function flush() { for (let i = 0; i < 8; i++) await Promise.resolve() }
function mount() { const scope = effectScope(), state = scope.run(useHomeAnalytics); return { state, close: () => scope.stop() } }
function defaults() { hooks.activity = async days => ok(activity(days)); hooks.queries = async () => ok({ content: [] }) }

test('period changes reload a common chart response but do not refetch recent SQL', async () => {
  defaults(); const calls = []; let queryCalls = 0
  hooks.activity = async days => { calls.push(days); return ok(activity(days)) }
  hooks.queries = async filter => { queryCalls++; assert.equal(filter.size, 4); assert.deepEqual(filter.orders, [{ column: 'createTime', order: 'DESC' }]); return ok({ content: [] }) }
  const { state, close } = mount(); await flush()
  assert.equal(state.activity.value.total, 0); assert.equal(state.activity.value.trend.length, 7)
  state.period.value = 30; await flush()
  assert.deepEqual(calls, [7, 30]); assert.equal(state.activity.value.days, 30); assert.equal(queryCalls, 1)
  close()
})
test('an earlier period cannot overwrite a later selection', async () => {
  defaults(); let resolve
  hooks.activity = () => new Promise(done => { resolve = done })
  const { state, close } = mount()
  hooks.activity = async days => ok(activity(days)); state.period.value = 30; await flush()
  resolve(ok(activity(7))); await flush()
  assert.equal(state.activity.value.days, 30); assert.equal(state.activityLoading.value, false)
  close()
})
test('failed statistics keep recent queries and recover without invented values', async () => {
  defaults(); hooks.activity = async () => ({ status: false }); hooks.queries = async () => ok({ content: [{ code: 'SQL' }] })
  const { state, close } = mount(); await flush()
  assert.equal(state.activity.value, undefined); assert.equal(state.activityFailed.value, true)
  assert.equal(state.queries.value[0].code, 'SQL')
  hooks.activity = async days => ok(activity(days)); await state.loadActivity()
  assert.equal(state.activityFailed.value, false); assert.equal(state.activity.value.total, 0)
  close()
})
test('wrong periods and malformed chart responses are visibly unavailable', async () => {
  for (const value of [activity(30), { ...activity(7), total: -1 }, { ...activity(7), trend: [] }, { ...activity(7), sources: [{ name: 'x', count: '5' }] }]) {
    defaults(); hooks.activity = async () => ok(value)
    const { state, close } = mount(); await flush()
    assert.equal(state.activityFailed.value, true); assert.equal(state.activity.value, undefined); close()
  }
})
test('navigation invalidates both unfinished requests', async () => {
  defaults(); let resolveActivity, resolveQueries
  hooks.activity = () => new Promise(done => { resolveActivity = done }); hooks.queries = () => new Promise(done => { resolveQueries = done })
  const { state, close } = mount(); close()
  resolveActivity(ok(activity(7))); resolveQueries(ok({ content: [{ code: 'late' }] })); await flush()
  assert.equal(state.activity.value, undefined); assert.deepEqual(state.queries.value, [])
})
