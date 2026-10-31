import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/database-er.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { ER_CARD, drawingSize, layoutTables, mapDatabaseEr, relationPath } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const rows = [
  { object_type: 'table', table_name: 'user', object_comment: '用户' },
  { object_type: 'table', table_name: 'dept' },
  { object_type: 'column', table_name: 'user', object_name: 'dept_id', data_type: 'bigint', nullable: 'YES', is_primary: 0, position: 2 },
  { object_type: 'column', table_name: 'user', object_name: 'id', data_type: 'bigint', nullable: 'NO', is_primary: 1, position: 1 },
  { object_type: 'column', table_name: 'dept', object_name: 'id', data_type: 'bigint', nullable: 'NO', is_primary: 1, position: 1 },
  { object_type: 'foreign_key', table_name: 'user', object_name: 'fk_dept', data_type: 'dept_id', reference_table: 'dept', reference_column: 'id' },
  { object_type: 'foreign_key', table_name: 'user', object_name: 'fk_gone', data_type: 'x', reference_table: 'missing', reference_column: 'id' },
]

test('rows become tables with ordered columns and relations between known tables', () => {
  const { tables, relations } = mapDatabaseEr(rows)
  assert.deepEqual(tables.map(table => table.name), ['dept', 'user'])
  assert.deepEqual(tables[1].columns.map(column => [column.name, column.primary, column.type]), [['id', true, 'BIGINT'], ['dept_id', false, 'BIGINT']])
  assert.equal(tables[1].comment, '用户')
  assert.deepEqual(relations.map(relation => relation.name), ['fk_dept'])
})

test('cards sit on a square-ish grid and edges join the rows of the key', () => {
  const { tables, relations } = mapDatabaseEr(rows)
  const boxes = layoutTables(tables)
  assert.deepEqual(boxes.map(box => [box.table.name, box.x, box.y]), [['dept', 0, 0], ['user', ER_CARD.width + ER_CARD.gapX, 0]])
  assert.deepEqual(drawingSize(boxes), { width: 2 * ER_CARD.width + ER_CARD.gapX, height: ER_CARD.header + 2 * ER_CARD.row })
  const path = relationPath(new Map(boxes.map(box => [box.table.name, box])), relations[0])
  // From the left side of user's dept_id row to the right side of dept's id row.
  assert.ok(path.startsWith(`M ${ER_CARD.width + ER_CARD.gapX} ${ER_CARD.header + ER_CARD.row * 1.5}`), path)
  assert.ok(path.endsWith(`${ER_CARD.width} ${ER_CARD.header + ER_CARD.row / 2}`), path)
})
