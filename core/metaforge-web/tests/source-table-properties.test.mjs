import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-table-properties.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { allOptions, changedOptions, defaultCollation, formatBytes, mapCollations, mapTableProperties, propertiesToForm, validatePropertiesForm } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const table = mapTableProperties({ table_name: 'workflow', table_schema: 'app', table_type: 'BASE TABLE', engine: 'InnoDB', row_format: 'Dynamic', create_time: '2026-10-29T10:49:50',
  charset: 'utf8mb4', collation: 'utf8mb4_0900_ai_ci', auto_increment: 16, data_length: 16384, index_length: 49152, table_rows: 15, table_comment: '工作流定义表', index_count: 4, foreign_key_count: 1 })
const collations = mapCollations([{ charset: 'utf8mb4', collation: 'utf8mb4_0900_ai_ci', is_default: 'Yes' }, { charset: 'utf8mb4', collation: 'utf8mb4_bin', is_default: '' }, { charset: 'latin1', collation: 'latin1_bin', is_default: '' }])

test('properties map from the row and format sizes like the prototype', () => {
  assert.equal(table.created, '2026-10-29 10:49:50')
  assert.equal(formatBytes(table.dataLength), '16 KB')
  assert.equal(formatBytes(table.indexLength, 'MB'), '0.05 MB')
  assert.equal(formatBytes(null), '-')
  assert.equal(defaultCollation(collations, 'utf8mb4'), 'utf8mb4_0900_ai_ci')
  assert.equal(defaultCollation(collations, 'latin1'), 'latin1_bin')
})

test('only changed options are sent, and the current statement covers every option', () => {
  const form = propertiesToForm(table)
  assert.deepEqual(changedOptions(form, table), {})
  assert.deepEqual(changedOptions({ ...form, comment: '', autoIncrement: '20', rowFormat: 'COMPACT' }, table), { rowFormat: 'COMPACT', autoIncrement: 20, comment: '' })
  assert.deepEqual(allOptions(table), { engine: 'InnoDB', rowFormat: 'DYNAMIC', autoIncrement: 16, charset: 'utf8mb4', collate: 'utf8mb4_0900_ai_ci', comment: '工作流定义表' })
})

test('validation keeps collations within the character set and auto increment positive', () => {
  const form = propertiesToForm(table)
  assert.deepEqual(validatePropertiesForm(form, collations), {})
  assert.deepEqual(validatePropertiesForm({ ...form, charset: 'latin1', autoIncrement: '0' }, collations), { collation: 'collationMismatch', autoIncrement: 'autoIncrementInvalid' })
})
