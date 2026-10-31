import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const load = async path => {
  const source = await readFile(new URL(`../src/${path}`, import.meta.url), 'utf8')
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)
}
const { cellText, dataBody, emptyDataForm, structureSides } = await load('model/table-compare.ts')
const { compareLocation, isSourceWorkbenchRoute, sourceManageContext } = await load('model/source-manage.ts')

const column = name => ({ name, type: 'int', nullable: false, defaultValue: null, comment: '' })
const rows = [
  { name: 'id', status: 'same', source: column('id'), target: column('id'), changes: [] },
  { name: 'type', status: 'changed', source: column('type'), target: column('type'), changes: ['nullable'] },
  { name: 'status', status: 'source_only', source: column('status'), target: null, changes: [] },
  { name: 'is_deleted', status: 'target_only', source: null, target: column('is_deleted'), changes: [] },
]

test('each side lists the columns it has, optionally only the differing ones', () => {
  const all = structureSides(rows, false)
  assert.deepEqual(all.source.map(row => row.name), ['id', 'type', 'status'])
  assert.deepEqual(all.target.map(row => row.name), ['id', 'type', 'is_deleted'])
  assert.deepEqual(structureSides(rows, true).source.map(row => row.name), ['type', 'status'])
})

test('the data request turns an inclusive row range into offset and count', () => {
  const tables = { sourceDatabase: 'a', sourceTable: 't', targetDatabase: 'b', targetTable: 't' }
  const body = dataBody(tables, { ...emptyDataForm(), start: 1, end: 5000 })
  assert.equal(body.offset, 0)
  assert.equal(body.limit, 5000)
  assert.deepEqual(body.keys, [])
  assert.equal(dataBody(tables, { ...emptyDataForm(), mode: 'fields', keys: ['code'], start: 10, end: 5 }).limit, 1)
  assert.equal(cellText(null), 'NULL')
})

test('the compare page carries the workbench context in its address', () => {
  const location = compareLocation({ source: 'demo', database: 'db', table: 'orders', mode: 'data' })
  assert.deepEqual(location, { path: '/compare', query: { id: 'demo', database: 'db', table: 'orders', mode: 'data' } })
  assert.equal(sourceManageContext({ path: '/compare', params: {}, query: { source: 'old', database: 'db' } }).source, 'old')
  assert.equal(isSourceWorkbenchRoute({ path: '/compare', query: { id: 'demo' } }), true)
  assert.equal(isSourceWorkbenchRoute({ path: '/compare', query: {} }), false)
})
