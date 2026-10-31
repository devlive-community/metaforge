import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-er.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { ER_ZOOM, clampZoom, mapErColumns } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const columnRows = [
  { type_name: 'column', object_name: 'email', object_position: 3, object_data_type: 'varchar(100)', object_nullable: 'NO', object_comment: '邮箱' },
  { type_name: 'column', object_name: 'id', object_position: 1, object_data_type: 'bigint', object_nullable: 'NO', object_autoincrement: 'YES', object_comment: '用户ID' },
  { type_name: 'column', object_name: 'dept_id', object_position: 2, object_data_type: 'bigint', object_default_value: null },
  { type_name: 'column', object_name: 'status', object_position: 4, object_data_type: 'tinyint', object_default_value: 0 },
  { type_name: 'primary', object_name: 'id' },
]
const indexRows = [
  { object_name: 'PRIMARY', object_column: 'id', object_unique: true },
  { object_name: 'uk_email', object_column: 'email', object_unique: 'true' },
  { object_name: 'uk_pair', object_column: 'dept_id', object_unique: true },
  { object_name: 'uk_pair', object_column: 'status', object_unique: true },
]
const foreignKeyRows = [{ object_name: 'fk_dept', object_column: 'dept_id', object_reference_table: 'dept', object_reference_column: 'id' }]

test('columns keep their order and key roles: primary, auto, single-column unique, foreign key', () => {
  const columns = mapErColumns(columnRows, indexRows, foreignKeyRows)
  assert.deepEqual(columns.map(column => column.name), ['id', 'dept_id', 'email', 'status'])
  assert.deepEqual(columns[0], { name: 'id', type: 'BIGINT', primary: true, auto: true, unique: false, reference: null, nullable: false, defaultValue: null, comment: '用户ID' })
  assert.equal(columns[1].reference, 'dept.id')
  assert.equal(columns[1].unique, false)
  assert.equal(columns[2].unique, true)
  assert.equal(columns[2].type, 'VARCHAR(100)')
  assert.equal(columns[3].defaultValue, '0')
  assert.equal(columns[3].nullable, true)
})

test('zoom stays within its range on whole percents', () => {
  assert.equal(clampZoom(5), ER_ZOOM.max)
  assert.equal(clampZoom(0.01), ER_ZOOM.min)
  assert.equal(clampZoom(1.1 + 0.1), 1.2)
})
