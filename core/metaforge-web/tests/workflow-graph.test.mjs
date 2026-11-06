import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'
const { canConnect, flattenNodes, mergeDefinitions, nodeForPort, validateWorkflow, arrangeNodes, fieldGroup, fieldIssues, defaultValues } = await sourceModule('../../metaforge-web/src/views/components/editor/workflow/utils.ts')
const definition = (id, category, configure = []) => ({ id, tid: id, key: id, label: id, category, configure,
  ports: [...(category !== 'source' ? [{ id: 'input', type: 'input', required: true }] : []), ...(category !== 'sink' ? [{ id: 'output', type: 'output', required: true }] : [])] })
const definitions = [definition('reader', 'source', [{ field: 'query', label: 'SQL', required: true }]), definition('sql', 'transform'), definition('writer', 'sink')]
const instances = ['reader', 'sql', 'writer'].map((tid, i) => ({ id: `67b2a012-90a5-44da-85b2-${i}`, tid, label: i === 0 ? 'My reader' : undefined, data: { query: 'select 1' }, position: { x: i * 250, y: 100 } }))
const nodes = mergeDefinitions(definitions, instances)
const edge = (from, to) => ({ id: `${from}-${to}`, source: nodes[from].id + '-output', target: nodes[to].id + '-input' })

test('UUID node identifiers survive connection matching and deletion', () => {
  assert.equal(nodeForPort(nodes[0].id + '-output', nodes)?.id, nodes[0].id)
  assert.equal(nodeForPort(nodes[0].id + '-missing', nodes), undefined)
  const remaining = [edge(0, 1), edge(1, 2)].filter(e => nodeForPort(e.source, nodes)?.id !== nodes[1].id && nodeForPort(e.target, nodes)?.id !== nodes[1].id)
  assert.deepEqual(remaining, [])
})
test('connections reject duplicate ports, reverse directions, self edges and cycles', () => {
  assert.equal(canConnect(nodes, [], edge(0, 1).source, edge(0, 1).target), true)
  assert.equal(canConnect(nodes, [edge(0, 1)], edge(0, 2).source, edge(0, 2).target), false)
  assert.equal(canConnect(nodes, [], nodes[0].id + '-input', nodes[1].id + '-output'), false)
  assert.equal(canConnect(nodes, [], nodes[1].id + '-output', nodes[1].id + '-input'), false)
  const transforms = mergeDefinitions([definition('sql', 'transform')], [{ ...instances[1] }, { ...instances[1], id: 'another-node' }])
  assert.equal(canConnect(transforms, [{ id: 'a', source: transforms[0].id + '-output', target: transforms[1].id + '-input' }], transforms[1].id + '-output', transforms[0].id + '-input'), false)
})
test('validation respects executor limits and checks disconnected and unknown nodes', () => {
  const limits = { sources: 1, transforms: 1, sinks: 1 }
  assert.deepEqual(validateWorkflow(nodes, [edge(0, 1), edge(1, 2)], limits), [])
  assert.ok(validateWorkflow(nodes, [edge(0, 1), edge(1, 2)], { ...limits, transforms: 0 }).some(issue => issue.message.includes('transformLimit')))
  assert.ok(validateWorkflow(nodes, [edge(0, 1), edge(1, 2)]).some(issue => issue.message.includes('transformLimit')), 'transforms default to none')
  assert.ok(validateWorkflow(nodes.slice(1), [edge(1, 2)], limits).some(issue => issue.message.includes('needEndpoints')))
  assert.ok(validateWorkflow(nodes, [], limits).some(issue => issue.message.includes('disconnected')))
  const unknown = mergeDefinitions([], instances)
  assert.ok(validateWorkflow(unknown, [], limits).some(issue => issue.message.includes('unsupportedNode')))
  assert.ok(validateWorkflow([{ ...nodes[0], data: { query: '   ' } }, ...nodes.slice(1)], [edge(0, 1), edge(1, 2)], limits).some(issue => issue.type === 'configure'))
})
test('saving preserves custom labels and nested typed configuration without mutating definitions', () => {
  assert.equal(nodes[0].label, 'My reader')
  const persisted = flattenNodes(nodes)
  assert.equal(persisted[0].label, 'My reader')
  assert.equal(persisted[0].data.query, 'select 1')
  assert.equal(definitions[0].configure[0].value, undefined)
  assert.equal('configure' in persisted[0], false)
})
test('auto arrangement follows the graph instead of the storage order', () => {
  assert.deepEqual(arrangeNodes([...nodes].reverse(), [edge(0, 1), edge(1, 2)]).map(node => node.id), nodes.map(node => node.id))
})

test('legacy drafts with missing positions remain usable on the canvas', () => {
  const restored = mergeDefinitions(definitions, [{ ...instances[0], position: null }])
  assert.deepEqual(restored[0].position, { x: 60, y: 180 })
})

test('declared fields drive editor tabs, defaults and bounds', () => {
  assert.equal(fieldGroup({ field: 'query', type: 'textarea' }), 'sql')
  assert.equal(fieldGroup({ field: 'host', type: 'input', required: true }), 'basic')
  assert.equal(fieldGroup({ field: 'retries', type: 'number' }), 'advanced')
  assert.equal(fieldGroup({ field: 'columns', type: 'map', group: 'basic' }), 'basic')
  assert.deepEqual(defaultValues([{ field: 'a', value: 1 }, { field: 'b' }]), { a: 1 })
  const settings = [{ field: 'batchSize', label: 'Batch size', type: 'number', min: 1 }]
  assert.deepEqual(fieldIssues(settings, {}, 'Settings'), [], 'optional settings may stay empty')
  assert.deepEqual(fieldIssues(settings, { batchSize: 0 }, 'Settings'), ['Settings: Batch size'])
  assert.deepEqual(fieldIssues(settings, { batchSize: 10 }, 'Settings'), [])
})
