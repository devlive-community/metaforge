import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-index.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { columnsWithOrder, formToDefinition, groupIndexDetails, indexToForm, validateIndexForm } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const rows = [
  { index_name: 'idx_status_created', non_unique: 1, seq_in_index: 2, column_name: 'created_at', collation: 'D', cardinality: 56230, sub_part: null, index_type: 'BTREE', index_comment: 'combo', key_length: 5, usage_count: 12, usage_rows: 99 },
  { index_name: 'idx_status_created', non_unique: 1, seq_in_index: 1, column_name: 'status', collation: 'A', cardinality: 3, sub_part: null, index_type: 'BTREE', index_comment: 'combo', key_length: 4, usage_count: 12, usage_rows: 99 },
  { index_name: 'PRIMARY', non_unique: 0, seq_in_index: 1, column_name: 'id', collation: 'A', cardinality: 100, sub_part: null, index_type: 'BTREE', index_comment: '', key_length: 8, usage_count: null, usage_rows: null },
  { index_name: 'ft_body', non_unique: 1, seq_in_index: 1, column_name: 'body', collation: null, cardinality: 7, sub_part: null, index_type: 'FULLTEXT', index_comment: '', key_length: null },
]

test('per-column rows group into indexes with order, cardinality and key length', () => {
  const indexes = groupIndexDetails(rows)
  assert.deepEqual(indexes.map(index => index.name), ['PRIMARY', 'idx_status_created', 'ft_body'])
  const combo = indexes[1]
  assert.deepEqual({ unique: combo.unique, cardinality: combo.cardinality, keyLength: combo.keyLength, usage: combo.usageCount }, { unique: false, cardinality: 56230, keyLength: 9, usage: 12 })
  assert.equal(columnsWithOrder(combo), 'status (ASC), created_at (DESC)')
  assert.equal(indexes[0].primary && indexes[0].unique, true)
  assert.equal(indexes[2].keyLength, null)
})

test('forms turn into definitions; FULLTEXT drops uniqueness and order', () => {
  const combo = groupIndexDetails(rows)[1]
  assert.deepEqual(formToDefinition({ ...indexToForm(combo), algorithm: 'INPLACE' }, 'idx_status_created'), {
    name: 'idx_status_created', type: 'BTREE', unique: false, algorithm: 'INPLACE', comment: 'combo', columns: ['status', 'created_at'],
    orders: ['ASC', 'DESC'], lengths: [null, null], originalName: 'idx_status_created',
  })
  const fulltext = formToDefinition({ name: 'ft', method: 'FULLTEXT', unique: true, algorithm: '', comment: ' ', columns: [{ name: 'body', order: 'DESC', subPart: null }, { name: '', order: 'ASC', subPart: null }] })
  assert.deepEqual({ unique: fulltext.unique, orders: fulltext.orders, columns: fulltext.columns, comment: fulltext.comment }, { unique: false, orders: [null], columns: ['body'], comment: undefined })
})

test('validation checks the name and the columns', () => {
  const form = (name, columns) => ({ name, method: 'BTREE', unique: false, algorithm: '', comment: '', columns: columns.map(name => ({ name, order: 'ASC', subPart: null })) })
  assert.deepEqual(validateIndexForm(form('', []), []), { name: 'nameRequired', columns: 'columnsRequired' })
  assert.deepEqual(validateIndexForm(form('IDX_A', ['a', 'a']), ['idx_a']), { name: 'nameTaken', columns: 'columnsRepeated' })
  assert.deepEqual(validateIndexForm(form('idx_a', ['a']), ['idx_a'], 'idx_a'), {})
  assert.deepEqual(validateIndexForm(form('bad`name', ['a']), []), { name: 'nameInvalid' })
})
