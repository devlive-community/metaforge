import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from 'typescript'

const source = await readFile(new URL('../src/model/dataset-adhoc.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } })
const { createAdhocColumn, restoreAdhocQuery, adhocQuery, adhocColumnIssue, adhocRows, adhocCsv } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)
const amount = { id: 1, name: 'amount', aliasName: 'Amount', type: 'NUMBER_SIGNED', mode: 'METRIC' }
const city = { id: 2, name: 'city', type: 'STRING', mode: 'DIMENSION' }

test('restoring a report keeps the catalog and each query role independent', () => {
  const catalog = [structuredClone(amount), structuredClone(city)]
  const before = structuredClone(catalog)
  const saved = { columns: [{ id: 1, mode: 'METRIC', expression: 'SUM', alias: 'Sales' }, { id: 1, mode: 'FILTER', expression: 'GT', value: '0' }], limit: 50 }
  const restored = restoreAdhocQuery(catalog, JSON.stringify(saved))
  restored.columns[0].alias = 'Changed'
  assert.deepEqual(catalog, before)
  assert.notEqual(restored.columns[0].key, restored.columns[1].key)
  assert.equal(restored.columns[1].alias, undefined)
  assert.equal(restored.limit, 50)
})
test('removed report fields are reported rather than rebound to another field', () => {
  const restored = restoreAdhocQuery([city], { columns: [{ id: 99, mode: 'DIMENSION' }, { id: 2, mode: 'DIMENSION' }], limit: -1 })
  assert.equal(restored.missing, 1)
  assert.equal(restored.columns.length, 1)
  assert.equal(restored.limit, 1000)
  assert.throws(() => restoreAdhocQuery([city], 'null'))
})
test('numeric, boolean, text and date filters use compatible values and operators', () => {
  const filter = createAdhocColumn(amount, 'FILTER')
  assert.equal(createAdhocColumn(amount).expression, 'SUM')
  assert.ok(adhocColumnIssue({ ...filter, expression: 'LIKE', value: '12' }))
  assert.ok(adhocColumnIssue({ ...filter, value: 'hello' }))
  assert.equal(adhocColumnIssue({ ...filter, value: '0' }), undefined)
  assert.equal(adhocColumnIssue({ ...filter, expression: 'NULL' }), undefined)
  assert.ok(adhocColumnIssue({ ...filter, type: 'BOOLEAN', value: '1' }))
  assert.equal(adhocColumnIssue({ ...filter, type: 'BOOLEAN', value: 'false' }), undefined)
  assert.ok(adhocColumnIssue({ ...filter, type: 'STRING', expression: 'GT', value: 'A' }))
  assert.ok(adhocColumnIssue({ ...filter, type: 'DATETIME', value: 'invalid-date' }))
})
test('request serialization omits UI metadata and null-filter values', () => {
  const filter = { ...createAdhocColumn(city, 'FILTER'), expression: 'NULL', value: 'stale' }
  const request = adhocQuery([filter], 100)
  assert.equal(request.columns[0].value, undefined)
  assert.equal(request.columns[0].mode, 'FILTER')
  assert.equal('name' in request.columns[0], false)
  assert.equal('key' in request.columns[0], false)
})
test('array and object result rows normalize identically without dropping zero or null', () => {
  assert.deepEqual(adhocRows(['a', 'b'], [[0, null]]), adhocRows(['a', 'b'], [{ a: 0, b: null }]))
})
test('CSV export follows header order and escapes commas, quotes and carriage returns', () => {
  const csv = adhocCsv(['b', 'a'], [{ a: 'A\rB', b: 'say "yes", again' }])
  assert.equal(csv, '\uFEFFb,a\r\n"say ""yes"", again","A\rB"')
})
