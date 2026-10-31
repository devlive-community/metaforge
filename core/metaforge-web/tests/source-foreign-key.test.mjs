import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-foreign-key.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { foreignKeyToForm, formToForeignKey, groupForeignKeys, validateForeignKeyForm } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const rows = [
  { constraint_name: 'fk_order_item', column_name: 'item_no', ordinal_position: 2, referenced_database: 'app', referenced_table: 'item', referenced_column: 'no', update_rule: 'CASCADE', delete_rule: 'RESTRICT', referenced_exists: 1 },
  { constraint_name: 'fk_order_item', column_name: 'item_shop', ordinal_position: 1, referenced_database: 'app', referenced_table: 'item', referenced_column: 'shop', update_rule: 'CASCADE', delete_rule: 'RESTRICT', referenced_exists: 1 },
  { constraint_name: 'fk_gone', column_name: 'x_id', ordinal_position: 1, referenced_database: 'old', referenced_table: 'x', referenced_column: 'id', update_rule: 'NO ACTION', delete_rule: 'SET NULL', referenced_exists: 0 },
]

test('per-column rows group into foreign keys keeping the column pairs in order', () => {
  const keys = groupForeignKeys(rows)
  assert.deepEqual(keys.map(key => key.name), ['fk_gone', 'fk_order_item'])
  assert.deepEqual({ columns: keys[1].columns, referenced: keys[1].referencedColumns, valid: keys[1].valid }, { columns: ['item_shop', 'item_no'], referenced: ['shop', 'no'], valid: true })
  assert.equal(keys[0].valid, false)
})

test('forms round-trip into definitions, dropping unfinished pairs', () => {
  const key = groupForeignKeys(rows)[1]
  const form = foreignKeyToForm(key)
  form.pairs.push({ column: '', referenced: '' })
  assert.deepEqual(formToForeignKey(form, 'fk_order_item'), {
    name: 'fk_order_item', columns: ['item_shop', 'item_no'], referencedDatabase: 'app', referencedTable: 'item', referencedColumns: ['shop', 'no'],
    onUpdate: 'CASCADE', onDelete: 'RESTRICT', originalName: 'fk_order_item',
  })
})

test('validation checks the name, the referenced table and the column pairs', () => {
  const form = (name, table, pairs) => ({ name, referencedDatabase: 'app', referencedTable: table, onUpdate: 'RESTRICT', onDelete: 'RESTRICT', pairs })
  assert.deepEqual(validateForeignKeyForm(form('', '', [{ column: '', referenced: '' }]), []), { name: 'nameRequired', table: 'tableRequired', columns: 'columnsRequired' })
  assert.deepEqual(validateForeignKeyForm(form('FK_A', 'item', [{ column: 'a', referenced: '' }]), ['fk_a']), { name: 'nameTaken', columns: 'columnsIncomplete' })
  assert.deepEqual(validateForeignKeyForm(form('fk_a', 'item', [{ column: 'a', referenced: 'id' }, { column: 'a', referenced: 'no' }]), ['fk_a'], 'fk_a'), { columns: 'columnsRepeated' })
})
