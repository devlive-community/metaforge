import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-data.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { buildDataFilters, dataFields, dataKind, defaultOperator, formToJson, formToRow, jsonToRow, operatorsFor, rowToForm, toCsv, toJson, validateForm } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('column types pick the filter kind and default operator', () => {
  assert.equal(dataKind('DATETIME'), 'date')
  assert.equal(dataKind('BIGINT'), 'number')
  assert.equal(dataKind('VARCHAR'), 'text')
  assert.equal(defaultOperator('date'), 'BETWEEN')
  assert.equal(defaultOperator('text'), 'LIKE')
  assert.ok(!operatorsFor('number').includes('LIKE'))
})

test('conditions become server filters and incomplete rows are skipped', () => {
  const filters = buildDataFilters([
    { id: 1, column: 'created_at', operator: 'BETWEEN', value: ['2026-10-01', '2026-10-31'] },
    { id: 2, column: 'type', operator: 'IN', value: ['SYSTEM', '', 'AI'] },
    { id: 3, column: 'name', operator: 'LIKE', value: 'flow' },
    { id: 4, column: 'code', operator: 'LIKE', value: 'user_%' },
    { id: 5, column: 'deleted', operator: 'IS NULL', value: '' },
    { id: 6, column: 'status', operator: '=', value: '' },
    { id: 7, column: '', operator: '=', value: 'x' },
    { id: 8, column: 'updated_at', operator: 'BETWEEN', value: ['', '2026-10-31'] },
  ])
  assert.deepEqual(filters, [
    { name: 'created_at', operator: 'GE', value: '2026-10-01 00:00:00' },
    { name: 'created_at', operator: 'LE', value: '2026-10-31 23:59:59' },
    { name: 'type', operator: 'IN', value: ['SYSTEM', 'AI'] },
    { name: 'name', operator: 'LIKE', value: '%flow%' },
    { name: 'code', operator: 'LIKE', value: 'user_%' },
    { name: 'deleted', operator: 'IS_NULL' },
    { name: 'updated_at', operator: 'LE', value: '2026-10-31 23:59:59' },
  ])
})

test('exports keep column order, quote CSV cells and write nulls', () => {
  const rows = [{ id: 1, name: 'a,"b"', note: null }]
  assert.equal(toCsv(['id', 'name', 'note'], rows), 'id,name,note\r\n1,"a,""b""",')
  assert.deepEqual(JSON.parse(toJson(['name', 'id'], rows)), [{ name: 'a,"b"', id: 1 }])
})

const columnRow = (name, type, extra = {}) => ({ type_name: 'column', object_name: name, object_data_type: type, object_nullable: 'YES', object_position: 0, ...extra })
const fields = dataFields([
  columnRow('id', 'bigint', { object_nullable: 'NO', object_autoincrement: 'YES', object_position: 1 }),
  columnRow('name', 'varchar(100)', { object_nullable: 'NO', object_position: 2 }),
  columnRow('type', "enum('SYSTEM','DATA','O''K')", { object_position: 3 }),
  columnRow('active', 'tinyint(1)', { object_nullable: 'NO', object_default_value: '1', object_position: 4 }),
  columnRow('config', 'json', { object_position: 5 }),
  columnRow('created_at', 'datetime', { object_position: 6 }),
])

test('column types render as typed fields with required flags and enum options', () => {
  assert.deepEqual(fields.map(field => field.kind), ['number', 'text', 'enum', 'boolean', 'json', 'datetime'])
  assert.deepEqual(fields.map(field => field.required), [false, true, false, false, false, false])
  assert.deepEqual(fields[2].options, ['SYSTEM', 'DATA', "O'K"])
})

test('form rows let the database fill generated and default values', () => {
  const values = { ...rowToForm(fields, null), name: 'backup', config: '{ "a": 1 }', created_at: '' }
  assert.deepEqual(formToRow(fields, values), { id: null, name: 'backup', type: null, config: '{"a":1}', created_at: null })
  assert.deepEqual(validateForm(fields, { ...values, name: null, created_at: '2026-13' }), { name: 'required', created_at: 'datetime' })
})

test('rows round-trip between the form and the JSON editor', () => {
  const values = rowToForm(fields, { id: 7, name: 'a', type: 'DATA', active: true, config: '{"a":1}', created_at: null })
  assert.equal(values.active, '1')
  assert.equal(values.config, '{\n  "a": 1\n}')
  assert.deepEqual(formToJson(fields, values), { id: 7, name: 'a', type: 'DATA', active: 1, config: { a: 1 }, created_at: null })
  assert.deepEqual(jsonToRow(fields, { config: { a: 1 }, name: 'b' }), { config: '{"a":1}', name: 'b' })
})
