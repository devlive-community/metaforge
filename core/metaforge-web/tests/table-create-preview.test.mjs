import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { effectScope, nextTick, ref } from 'vue'
import ts from 'typescript'

const moduleUrl = text => `data:text/javascript;base64,${Buffer.from(text).toString('base64')}`
const serviceUrl = moduleUrl(`export const state={calls:[],fetch:null};export default {previewCreateTableBatch(source,body){state.calls.push({source,body});return state.fetch(source,body)}}`)
const { state } = await import(serviceUrl)
let source = (await readFile(new URL('../src/composables/useTableCreatePreview.ts', import.meta.url), 'utf8'))
    .replace("from 'vue'", `from '${import.meta.resolve('vue')}'`)
    .replace("from '@/services/metadata'", `from '${serviceUrl}'`)
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } })
const { useTableCreatePreview } = await import(moduleUrl(outputText))
const values = suffix => Object.fromEntries(['full', 'columns', 'indexes', 'options'].map(kind => [kind, `${kind} SQL ${suffix}`]))
const ok = data => ({ status: true, data })
const settle = async () => { await new Promise(resolve => setImmediate(resolve)); await nextTick() }

function draft() {
  state.calls = []
  state.fetch = async (_source, body) => ok(values(body.name))
  const active = ref(false), source = ref('S'), database = ref('app'), kind = ref('full')
  const definition = ref({ name: 'users', columns: [{ name: 'id', type: 'BIGINT' }] })
  const continueIndex = ref(false)
  const scope = effectScope()
  const preview = scope.run(() => useTableCreatePreview({ active: () => active.value, source: () => source.value,
    database: () => database.value, definition: () => definition.value, kind, failureMessage: () => 'preview failed' }))
  return { active, source, database, kind, definition, continueIndex, preview, stop: () => scope.stop() }
}

test('one request supplies every tab; tabs, navigation toggles and unchanged step revisits reuse the batch', async () => {
  const stateView = draft()
  try {
    await settle()
    assert.equal(state.calls.length, 0)
    stateView.active.value = true
    await settle()
    assert.equal(state.calls.length, 1)
    assert.equal(state.calls[0].body.database, 'app')
    for (const kind of ['columns', 'indexes', 'options', 'full', 'indexes']) {
      stateView.kind.value = kind
      stateView.continueIndex.value = !stateView.continueIndex.value
      await settle()
      assert.equal(stateView.preview.sqlContent.value, `${kind} SQL users`)
      assert.equal(stateView.preview.sqlLoading.value, false)
    }
    stateView.active.value = false
    await settle()
    stateView.active.value = true
    await settle()
    assert.equal(state.calls.length, 1)
  }
  finally { stateView.stop() }
})

test('only an effective definition, source or database change invalidates cached SQL', async () => {
  const view = draft()
  try {
    view.active.value = true
    await settle()
    view.active.value = false
    await settle()
    view.definition.value.name = 'accounts'
    view.definition.value.columns[0].name = 'account_id'
    await settle()
    assert.equal(state.calls.length, 1)
    view.active.value = true
    await settle()
    assert.equal(state.calls.length, 2)
    assert.equal(view.preview.sqlContent.value, 'full SQL accounts')
    view.definition.value = { name: 'accounts', columns: [{ name: 'account_id', type: 'BIGINT' }] }
    await settle()
    assert.equal(state.calls.length, 2)
    view.database.value = 'other'
    await settle()
    assert.equal(state.calls.length, 3)
    view.source.value = 'T'
    await settle()
    assert.equal(state.calls.length, 4)
    assert.equal(state.calls.at(-1).source, 'T')
  }
  finally { view.stop() }
})

test('an older batch cannot overwrite newer SQL or stop the new loading state', async () => {
  const view = draft()
  try {
    const pending = []
    state.fetch = () => new Promise(resolve => pending.push(resolve))
    view.active.value = true
    await settle()
    view.definition.value.name = 'accounts'
    await settle()
    assert.equal(state.calls.length, 2)
    pending[0](ok(values('old')))
    await settle()
    assert.equal(view.preview.sqlLoading.value, true)
    assert.equal(view.preview.sqlContent.value, '')
    pending[1](ok(values('new')))
    await settle()
    assert.equal(view.preview.sqlContent.value, 'full SQL new')
    assert.equal(view.preview.sqlLoading.value, false)
  }
  finally { view.stop() }
})

test('tab changes and leaving then returning during an in-flight batch never start another request', async () => {
  const view = draft()
  try {
    let finish
    state.fetch = () => new Promise(resolve => { finish = resolve })
    view.active.value = true
    await settle()
    view.kind.value = 'columns'
    view.active.value = false
    await settle()
    view.kind.value = 'indexes'
    view.active.value = true
    await settle()
    assert.equal(state.calls.length, 1)
    finish(ok(values('users')))
    await settle()
    assert.equal(view.preview.sqlContent.value, 'indexes SQL users')
  }
  finally { view.stop() }
})

test('formatted SQL is cached for its original tab and never replaces another definition', async () => {
  const view = draft()
  try {
    view.active.value = true
    await settle()
    const context = view.preview.previewContext.value
    view.kind.value = 'columns'
    await settle()
    view.preview.updateSqlContent('formatted full SQL', context)
    assert.equal(view.preview.sqlContent.value, 'columns SQL users')
    view.kind.value = 'full'
    assert.equal(view.preview.sqlContent.value, 'formatted full SQL')
    view.definition.value.name = 'accounts'
    await settle()
    view.preview.updateSqlContent('stale format', context)
    assert.equal(view.preview.sqlContent.value, 'full SQL accounts')
  }
  finally { view.stop() }
})

test('partial or legacy successful envelopes are rejected and an explicit retry is deduplicated', async () => {
  const view = draft()
  try {
    state.fetch = async () => ({ status: true, message: 'Query successful', data: { full: 'only one tab' } })
    view.active.value = true
    await settle()
    assert.equal(view.preview.sqlError.value, 'preview failed')
    assert.equal(view.preview.sqlLoading.value, false)
    assert.equal(view.preview.sqlContent.value, '')
    state.fetch = async () => ok(values('retry'))
    await Promise.all([view.preview.retryPreview(), view.preview.retryPreview()])
    assert.equal(state.calls.length, 2)
    assert.equal(view.preview.sqlError.value, '')
    assert.equal(view.preview.sqlContent.value, 'full SQL retry')
  }
  finally { view.stop() }
})

test('disposing the drawer discards pending responses and clears its loading state', async () => {
  const view = draft()
  let finish
  state.fetch = () => new Promise(resolve => { finish = resolve })
  view.active.value = true
  await settle()
  assert.equal(view.preview.sqlLoading.value, true)
  view.stop()
  finish(ok(values('closed')))
  await settle()
  assert.equal(view.preview.sqlLoading.value, false)
  assert.equal(view.preview.sqlContent.value, '')
  assert.equal(view.preview.sqlError.value, '')
})
