import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/dataset-editor.ts', import.meta.url), 'utf8')
const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } }).outputText
const { reconcileDatasetColumns: reconcile, datasetColumnIssue: issue, parseDatasetQueryDraft: parse } = await import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}`)

test('refresh preserves persisted column identities, aliases and keys, adds fields and retains virtual columns', () => {
  const [first, removed] = reconcile(['id', 'old'], [])
  Object.assign(first, { id: 42, name: 'record_id', aliasName: 'ID', mode: 'METRIC', type: 'NUMBER', orderByKey: true })
  const virtual = { ...removed, name: 'custom', aliasName: 'custom', customColumn: true, virtualColumn: true }
  const columns = reconcile(['id', 'new'], [first, removed, virtual])
  assert.equal(columns[0].id, 42)
  assert.equal(columns[0].name, 'record_id')
  assert.equal(columns[0].aliasName, 'ID')
  assert.equal(columns[0].orderByKey, true)
  assert.equal(columns[0].mode, 'METRIC')
  assert.equal(columns[2].original, 'new')
  assert.equal(columns[2].id, null)
  assert.equal(columns[1].virtualColumn, true)
  assert.deepEqual(columns.map(column => column.position), [0, 1, 2])
})
test('new internal names cannot collide with existing or custom columns', () => {
  const [first] = reconcile(['id'], [])
  const custom = { ...first, name: 'column_2', customColumn: true, virtualColumn: true }
  const columns = reconcile(['id', 'another'], [first, custom])
  assert.equal(new Set(columns.map(column => column.name)).size, 3)
})
test('validation requires real columns, names, aliases and a physical storage key', () => {
  assert.equal(issue([]), 'dataset.editor.noColumns')
  const [column] = reconcile(['id'], [])
  assert.equal(issue([column]), 'dataset.validator.specifiedColumn')
  column.primaryKey = true
  assert.equal(issue([column]), null)
  assert.equal(issue([{ ...column, name: ' ' }]), 'dataset.editor.columnRequired')
  assert.equal(issue([{ ...column, aliasName: '' }]), 'dataset.editor.columnRequired')
  assert.equal(issue([{ ...column, virtualColumn: true }]), 'dataset.validator.specifiedColumn')
})
test('duplicate field names and aliases, and sampling keys outside order keys block save', () => {
  const [first, second] = reconcile(['id', 'name'], [])
  first.orderByKey = true
  assert.equal(issue([first, { ...second, name: first.name }]), 'dataset.editor.duplicateName')
  assert.equal(issue([first, { ...second, aliasName: first.aliasName }]), 'dataset.editor.duplicateAlias')
  second.samplingKey = true
  assert.equal(issue([first, second]), 'dataset.tip.validatorSampling')
  second.orderByKey = true
  assert.equal(issue([first, second]), null)
})
test('drafts accept only the expected version, source and field metadata', () => {
  const draft = { version: 1, sourceCode: 'source-1', query: 'select id from records', headers: ['id'] }
  assert.deepEqual(parse(JSON.stringify(draft), 'source-1'), draft)
  for (const value of [null, 'bad json', JSON.stringify({ ...draft, version: 2 }), JSON.stringify({ ...draft, headers: [1] })]) assert.equal(parse(value, 'source-1'), null)
  assert.equal(parse(JSON.stringify(draft), 'source-2'), null)
})


test('repreview preserves configured column order while appending newly discovered fields', () => {
  const [first, second] = reconcile(['id', 'name'], [])
  const custom = { ...first, name: 'custom', aliasName: 'custom', customColumn: true, virtualColumn: true }
  const columns = reconcile(['id', 'name', 'new'], [second, custom, first])
  assert.deepEqual(columns.map(column => column.name).slice(0, 3), [second.name, 'custom', first.name])
  assert.equal(columns[3].original, 'new')
  assert.deepEqual(columns.map(column => column.position), [0, 1, 2, 3])
})
