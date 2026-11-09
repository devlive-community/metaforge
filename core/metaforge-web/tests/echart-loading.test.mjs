import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { effectScope, reactive, shallowRef } from 'vue'
import ts from 'typescript'
const uri = code => `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`
const loaderUri = uri('export const hooks={};export const loadECharts=()=>hooks.load()')
const hooks = (await import(loaderUri)).hooks
const compilerOptions = { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext }
const layoutSource = await readFile(new URL('../src/model/chart-card-layout.ts', import.meta.url), 'utf8')
const layoutUri = uri(ts.transpileModule(layoutSource, { compilerOptions }).outputText)
let source = await readFile(new URL('../src/composables/useEChart.ts', import.meta.url), 'utf8')
let { outputText } = ts.transpileModule(source, { compilerOptions })
outputText = outputText.replaceAll("from 'vue'", `from '${import.meta.resolve('vue')}'`).replaceAll("from '@/utils/echarts'", `from '${loaderUri}'`)
  .replaceAll("from '@/model/chart-card-layout'", `from '${layoutUri}'`)
const { useEChart } = await import(uri(outputText))
const flush = async () => { for (let i = 0; i < 10; i++) await Promise.resolve() }
function environment() {
  const frames = new Map(), observers = [], charts = [], maps = new Map(), links = []
  let nextFrame = 0, error = false
  globalThis.requestAnimationFrame = callback => { const id = ++nextFrame; frames.set(id, callback); return id }
  globalThis.cancelAnimationFrame = id => frames.delete(id)
  globalThis.ResizeObserver = class {
    constructor(callback) { this.callback = callback; observers.push(this) }
    observe(element) { this.element = element }
    disconnect() { this.disconnected = true }
    trigger() { this.callback([]) }
  }
  const engine = {
    init(element, _theme, options) {
      const events = new Map(), applied = []
      const chart = { element, options, applied, events, disposed: false, resized: 0,
        on: (name, listener) => events.set(name, listener),
        setOption(option) { if (error) throw Error('Render failed'); applied.push(option) },
        resize() { this.resized++ }, dispose() { this.disposed = true },
        getOption: () => applied.at(-1), getWidth: () => 320, getHeight: () => 200,
        getDataURL: () => 'data:image/png;base64,test', renderToSVGString: () => '<svg/>' }
      charts.push(chart); return chart
    },
    getMap: name => maps.get(name), registerMap: (name, data) => maps.set(name, data),
  }
  hooks.load = async () => engine
  function frame() { const batch = [...frames.values()]; frames.clear(); batch.forEach(callback => callback()) }
  async function paint() { await flush(); frame(); frame(); await flush() }
  function mount(loading = false, width = 320) {
    const scope = effectScope(), host = shallowRef({ clientWidth: width, clientHeight: 200 })
    const state = reactive({ option: { series: [{ type: 'line', data: [1, 2] }] }, loading })
    const errors = [], points = []
    const view = scope.run(() => useEChart(host, state, { error: () => errors.push(true), point: (...args) => points.push(args) }))
    return { host, state, view, errors, points, close: () => scope.stop() }
  }
  return { engine, charts, maps, links, observers, mount, paint, frame, fail: value => { error = value } }
}

test('initial loading remains active until the engine is ready and a chart frame has painted', async () => {
  const env = environment(); let resolve
  hooks.load = () => new Promise(done => { resolve = done })
  const item = env.mount()
  assert.equal(item.view.pending.value, true); assert.equal(env.charts.length, 0)
  resolve(env.engine); await flush()
  assert.equal(env.charts.length, 1); assert.equal(item.view.pending.value, true)
  env.frame(); assert.equal(item.view.pending.value, true)
  env.frame(); assert.equal(item.view.pending.value, false)
  item.close(); assert.equal(env.charts[0].disposed, true)
})
test('external data loading defers initialization and option edits do not flash the loader', async () => {
  const env = environment(); let calls = 0
  hooks.load = async () => { calls++; return env.engine }
  const item = env.mount(true); await flush()
  assert.equal(calls, 0); assert.equal(item.view.pending.value, true)
  item.state.loading = false; await env.paint()
  assert.equal(item.view.pending.value, false); assert.equal(calls, 1)
  item.state.loading = true; await flush()
  item.state.option = { series: [{ type: 'bar', data: [3] }] }; await flush()
  assert.equal(item.view.pending.value, true); assert.equal(env.charts[0].applied.length, 1)
  item.state.loading = false; await env.paint()
  assert.equal(env.charts[0].applied.at(-1).series[0].type, 'bar')
  assert.equal(item.view.pending.value, false)
  item.state.option = { series: [{ type: 'line', data: [4] }] }; await flush()
  assert.equal(item.view.pending.value, false); assert.equal(env.charts.length, 1)
  item.close()
})
test('only the latest options are drawn when changes arrive during engine loading', async () => {
  const env = environment(); let resolve
  const waiting = new Promise(done => { resolve = done }); hooks.load = () => waiting
  const item = env.mount()
  item.state.option = { series: [{ type: 'bar', data: [9] }] }; await flush()
  resolve(env.engine); await env.paint()
  assert.equal(env.charts.length, 1)
  assert.deepEqual(env.charts[0].applied.map(option => option.series[0].data), [[9]])
  item.close()
})
test('hidden charts initialize when sized and reuse their instance on resize', async () => {
  const env = environment(); const item = env.mount(false, 0); await flush()
  assert.equal(env.charts.length, 0); assert.equal(item.view.pending.value, true)
  item.host.value.clientWidth = 320; env.observers[0].trigger(); await env.paint()
  assert.equal(item.view.pending.value, false)
  item.host.value.clientWidth = 600; env.observers[0].trigger()
  assert.equal(env.charts.length, 1); assert.equal(env.charts[0].resized, 1)
  env.charts[0].events.get('click')({ data: { value: 4 }, name: 'Sales' })
  assert.deepEqual(item.points, [[{ value: 4 }, 'Sales']])
  item.close(); assert.equal(env.observers[0].disconnected, true)
})
test('a request finishing in a hidden tab renders its new data and clears loading when shown', async () => {
  const env = environment(), item = env.mount(); await env.paint()
  item.state.loading = true; await flush()
  item.host.value.clientWidth = 0
  item.state.option = { series: [{ type: 'bar', data: [42] }] }
  item.state.loading = false; await flush()
  assert.equal(item.view.pending.value, true)
  item.host.value.clientWidth = 320; env.observers[0].trigger(); await env.paint()
  assert.equal(item.view.pending.value, false)
  assert.deepEqual(env.charts[0].applied.at(-1).series[0].data, [42])
  assert.equal(env.charts.length, 1)
  item.close()
})
test('unmounting during loading never creates a late chart or emits an error', async () => {
  const env = environment(); let resolve
  hooks.load = () => new Promise(done => { resolve = done })
  const item = env.mount(); item.close(); resolve(env.engine); await env.paint()
  assert.equal(env.charts.length, 0); assert.equal(item.errors.length, 0)
})
test('engine or rendering failures clear loading and can recover with new options', async () => {
  const env = environment(); hooks.load = async () => { throw Error('Download failed') }
  const item = env.mount(); await flush()
  assert.equal(item.view.pending.value, false); assert.equal(item.view.failed.value, true); assert.equal(item.errors.length, 1)
  hooks.load = async () => env.engine
  item.state.option = { series: [] }; await env.paint()
  assert.equal(item.view.failed.value, false); assert.equal(item.view.pending.value, false)
  env.fail(true); item.state.option = { series: [{ type: 'bar' }] }; await flush()
  assert.equal(item.view.pending.value, false); assert.equal(item.view.failed.value, true)
  assert.equal(item.errors.length, 2); assert.equal(env.charts[0].disposed, true)
  item.close()
})
test('loading one chart leaves another chart ready', async () => {
  const env = environment(), first = env.mount(), second = env.mount()
  await env.paint()
  first.state.loading = true; await flush()
  assert.equal(first.view.pending.value, true); assert.equal(second.view.pending.value, false)
  assert.equal(env.charts.length, 2)
  first.close(); second.close()
})
test('map registration and PNG/SVG exports remain available after loading', async () => {
  const env = environment(); const item = env.mount(true)
  item.state.map = { name: 'region', source: { type: 'FeatureCollection', features: [] } }
  item.state.loading = false; await env.paint()
  assert.equal(env.maps.has('region'), true)
  const originalDocument = globalThis.document, originalWindow = globalThis.window
  const create = URL.createObjectURL, revoke = URL.revokeObjectURL
  globalThis.document = { createElement: () => ({ click() { env.links.push({ name: this.download, href: this.href }) } }) }
  globalThis.window = { setTimeout: callback => callback() }
  URL.createObjectURL = () => 'blob:svg'; URL.revokeObjectURL = () => {}
  try {
    item.view.exportImage('chart', 'png'); item.view.exportImage('chart', 'svg')
    assert.deepEqual(env.links.map(link => link.name), ['chart.png', 'chart.svg'])
    assert.equal(env.charts[1].options.renderer, 'svg'); assert.equal(env.charts[1].disposed, true)
    assert.equal(env.charts[1].applied[0].animation, false)
  } finally { globalThis.document = originalDocument; globalThis.window = originalWindow; URL.createObjectURL = create; URL.revokeObjectURL = revoke; item.close() }
})
