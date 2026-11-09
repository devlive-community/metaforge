import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { effectScope, ref } from 'vue'
import ts from 'typescript'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'
const uri = source => `data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
async function compile(path, aliases = {}) {
  const source = await readFile(new URL('../src/' + path, import.meta.url), 'utf8')
  let { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } })
  for (const [from, to] of Object.entries(aliases)) outputText = outputText.replaceAll(`from '${from}'`, `from '${to}'`)
  return uri(outputText)
}
const apiUri = uri('export const hooks={};export default {getAll:filter=>hooks.mine(filter),shared:(keyword,sort)=>hooks.shared(keyword,sort)}')
const hooks = (await import(apiUri)).hooks
const { useScreenLibrary } = await import(await compile('composables/useScreenLibrary.ts', {
  vue: import.meta.resolve('vue'), '@/services/dashboard': apiUri, '@/model/filter': await compile('model/filter.ts'),
}))
const ok = data => ({ status: true, data }), page = (content = [], total = content.length) => ok({ content, total })
async function flush() { for (let i = 0; i < 12; i++) await Promise.resolve() }
function mount() { const view = ref('mine'), scope = effectScope(), state = scope.run(() => useScreenLibrary(view)); return { state, view, close: () => scope.stop() } }
function defaults() { hooks.mine = async () => page(); hooks.shared = async () => ok([]) }

test('server paging, search and sorting reset the page without fetching the entire library', async () => {
  defaults(); const calls = []; hooks.mine = async filter => { calls.push(filter); return page([{ code: 'a' }], 20) }
  const { state, close } = mount(); await flush(); state.changePage(2, 6); await flush()
  state.keyword.value = ' sales '; await new Promise(done => setTimeout(done, 280)); await flush()
  assert.equal(calls.at(-1).keyword, 'sales'); assert.equal(calls.at(-1).page, 1); assert.equal(calls.at(-1).size, 6)
  state.sort.value = 'name'; await flush()
  assert.deepEqual(calls.at(-1).orders, [{ column: 'name', order: 'ASC' }]); assert.equal(state.total.value, 20)
  state.changePage(2, 12); await flush(); assert.equal(calls.at(-1).page, 1); assert.equal(calls.at(-1).size, 12)
  close()
})
test('switching to shared uses the authorized server search, and ignores a stale owned list', async () => {
  defaults(); let resolve; const calls = []
  hooks.mine = () => new Promise(done => { resolve = done }); hooks.shared = async (...args) => { calls.push(args); return ok([{ code: 'shared', permission: 'VIEW' }]) }
  const { state, view, close } = mount(); view.value = 'shared'; await flush()
  resolve(page([{ code: 'late' }], 90)); await flush()
  assert.equal(state.data.value[0].code, 'shared'); assert.equal(state.total.value, 1)
  assert.deepEqual(calls[0], ['', 'updated']); assert.equal(state.loading.value, false)
  close()
})
test('deleting the final item of a page returns to the last existing page', async () => {
  defaults(); hooks.mine = async filter => filter.page > 1 ? page([], 6) : page([{ code: 'remaining' }], 6)
  const { state, close } = mount(); await flush(); state.changePage(2, 6); await flush()
  assert.equal(state.page.value, 1); assert.equal(state.data.value[0].code, 'remaining'); assert.equal(state.loading.value, false)
  close()
})
test('invalid response totals fail and disposal invalidates pending list data', async () => {
  defaults(); hooks.mine = async () => page([], '8')
  const { state, close } = mount(); await flush(); assert.equal(state.failed.value, true)
  let resolve; hooks.mine = () => new Promise(done => { resolve = done }); void state.load(); close(); resolve(page([{ code: 'late' }], 1)); await flush()
  assert.deepEqual(state.data.value, [])
})
const { screenPreview } = await sourceModule('../../metaforge-web/src/model/screen-preview.ts')
const { screenEntry } = await sourceModule('../../metaforge-web/src/model/screen-entry.ts')
const { emptyDashboard, dashboardWidget } = await sourceModule('../../metaforge-web/src/model/dashboard-layout.ts')
const { defaultQueryChart } = await sourceModule('../../metaforge-web/src/model/query-chart.ts')
test('thumbnail state preserves saved geometry, uses snapshots only and disables live behavior', () => {
  const design = emptyDashboard('Title'), board = design.boards.dashboard
  board.widgets = ['area', 'embed', 'rain'].map((kind, i) => ({ ...dashboardWidget('w' + i, kind, kind), reportCode: 'saved', animation: { effect: 'pulse', trigger: 'load' }, expandable: true }))
  const snapshot = { headers: ['value'], columns: [{ value: 9 }], queryChart: { version: 1, options: { ...defaultQueryChart(['value']), refreshSeconds: 5 } } }
  const screen = { name: 'Screen', configure: JSON.stringify({ version: 3, draft: design, published: design }), reports: [
    { code: 'saved', realtime: true, query: 'private SQL', configure: JSON.stringify(snapshot) }, { code: 'legacy', query: 'must never run', configure: '[]' },
  ] }
  const before = JSON.stringify(screen), preview = screenPreview(screen)
  assert.equal(JSON.stringify(screen), before, 'no mutation of editor state')
  assert.equal(preview.reports.length, 1); assert.equal(preview.reports[0].realtime, false); assert.equal(preview.reports[0].query, undefined)
  assert.deepEqual(JSON.parse(preview.reports[0].configure).columns, snapshot.columns)
  assert.equal(JSON.parse(preview.reports[0].configure).queryChart.options.refreshSeconds, 0)
  assert.equal(preview.design.boards.dashboard.refresh, 'off')
  assert.equal(preview.design.boards.dashboard.widgets.find(w => w.id === 'w1').kind, 'text')
  assert.equal(preview.design.boards.dashboard.widgets.some(w => w.kind === 'rain'), false)
  assert.equal(preview.design.boards.dashboard.widgets[0].animation.effect, 'none')
  assert.equal(preview.design.boards.dashboard.widgets[0].expandable, false)
  assert.equal(preview.width, 960); assert.ok(preview.height > 0)
})
test('screen routes and editing actions respect publication and share permission', () => {
  const draft = emptyDashboard('Draft'), configure = JSON.stringify({ version: 3, draft })
  assert.match(screenEntry({ code: 'x', configure }).link, /design$/)
  const published = JSON.stringify({ version: 3, draft, published: draft })
  const viewer = screenEntry({ code: 'x', configure: published, permission: 'VIEW' })
  assert.equal(viewer.link, '/screen/x'); assert.equal(viewer.editable, false); assert.equal(viewer.own, false)
  const editor = screenEntry({ code: 'x', configure: published, permission: 'EDIT' })
  assert.equal(editor.editable, true); assert.equal(editor.own, false)
})
