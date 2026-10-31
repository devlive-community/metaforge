import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/table-import.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { defaultImportSettings, formatSize, importPlan, mapFields, mappingCounts, matchTables } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const table = (name, columns) => ({ name, rows: 1, hasDefinition: true, columns: columns.map(([column, type]) => ({ name: column, type, sample: [] })) })

test('source tables match existing names, prefixed copies, or become new tables', () => {
  const plans = matchTables([table('dataset', []), table('dataset_history', []), table('logs', [])], ['datacap_dataset', 'Logs'])
  assert.deepEqual(plans.map(plan => [plan.action, plan.target]), [['existing', 'datacap_dataset'], ['create', 'dataset_history'], ['existing', 'Logs']])
  assert.deepEqual(matchTables([table('x', [])], ['x_import', 'y']).map(plan => [plan.action, plan.target]), [['create', 'x']])
})

test('a source that can not create tables skips the tables without a target', () => {
  const plans = matchTables([table('dataset', []), table('logs', [])], ['dataset'], false)
  assert.deepEqual(plans.map(plan => [plan.action, plan.target, plan.selected]), [['existing', 'dataset', true], ['skip', '', false]])
})

test('fields map by name, parse text into time columns and leave the rest out', () => {
  const mappings = mapFields(table('t', [['ID', 'BIGINT'], ['created', 'VARCHAR(32)'], ['extra', 'JSON']]), [{ name: 'id', type: 'bigint' }, { name: 'created', type: 'datetime' }])
  assert.deepEqual(mappings, [{ source: 'ID', target: 'id', handling: 'direct' }, { source: 'created', target: 'created', handling: 'date' }, { source: 'extra', target: '', handling: 'ignore' }])
  const plan = { source: 't', selected: true, action: 'existing', target: 't', mappings, writeMode: '', conflict: '' }
  assert.deepEqual(mappingCounts(plan, new Set(['ID → id'])), { mapped: 2, total: 3, attention: 2 })
})

test('the plan skips unselected tables and clears ignored targets', () => {
  const plans = [{ source: 'a', selected: false, action: 'existing', target: 'a', mappings: [], writeMode: '', conflict: '' },
    { source: 'b', selected: true, action: 'create', target: 'b', mappings: [{ source: 'x', target: 'x', handling: 'ignore' }], writeMode: 'truncate', conflict: '' }]
  const body = importPlan('id1', plans, defaultImportSettings())
  assert.equal(body.tables[0].action, 'skip')
  assert.equal(body.tables[1].mappings[0].target, '')
  assert.equal(body.tables[1].writeMode, 'truncate')
  assert.equal(body.tables[1].conflict, null)
  assert.equal(formatSize(8.4 * 1024 * 1024), '8.4 MB')
})
