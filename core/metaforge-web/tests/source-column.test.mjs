import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-column.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { mapSourceColumns, planColumnMoves, sourceColumnForm, sourceColumnPayload } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('metadata preserves zero defaults, decimal scale and column attributes', () => {
  const [row] = mapSourceColumns([
    { type_name: 'primary', object_name: 'amount' },
    { type_name: 'column', object_name: 'amount', object_data_type: 'decimal(12, 3)', object_nullable: 'NO', object_default_value: 0, object_position: 1, object_charset: 'utf8mb4', object_collation: 'utf8mb4_bin', object_autoincrement: 'NO' },
  ])
  assert.equal(row.default, '0')
  assert.equal(row.type, 'DECIMAL')
  assert.equal(row.length, '12,3')
  assert.equal(row.collate, 'utf8mb4_bin')
  const payload = sourceColumnPayload({ ...sourceColumnForm(), ...row, nullable: false, defaultMode: 'constant', defaultValue: row.default })
  assert.equal(payload.precision, 12)
  assert.equal(payload.scale, 3)
  assert.equal(payload.defaultValue, '0')
  assert.equal(payload.nullable, false)
})

test('empty-string defaults remain distinct from no default and timestamps', () => {
  assert.equal(sourceColumnPayload({ ...sourceColumnForm(), defaultMode: 'constant', defaultValue: '' }).defaultValue, '')
  assert.equal(sourceColumnPayload({ ...sourceColumnForm(), defaultMode: 'none' }).defaultValue, undefined)
  assert.equal(sourceColumnPayload({ ...sourceColumnForm(), defaultMode: 'timestamp' }).defaultValue, 'CURRENT_TIMESTAMP')
})

test('JDBC fallback preserves decimal scale when the type name has no precision', () => {
  const [row] = mapSourceColumns([{ type_name: 'column', object_name: 'amount', object_data_type: 'DECIMAL', object_length: 12, object_scale: 3, object_default_value: null }])
  assert.equal(row.length, '12,3')
  assert.equal(row.default, null)
})

test('unsupported modifiers remain visible rather than becoming a destructive type conversion', () => {
  const [row] = mapSourceColumns([{ type_name: 'column', object_name: 'id', object_data_type: 'bigint(20) unsigned', object_length: 20 }])
  assert.equal(row.type, 'BIGINT(20) UNSIGNED')
})

test('metadata position determines field order and JSON round-trips unchanged', () => {
  const rows = mapSourceColumns([
    { type_name: 'column', object_name: 'config', object_data_type: 'json', object_position: 2 },
    { type_name: 'column', object_name: 'id', object_data_type: 'bigint(20)', object_position: 1, object_autoincrement: 'YES' },
  ])
  assert.deepEqual(rows.map(row => row.name), ['id', 'config'])
  assert.equal(rows[0].autoincrement, 'YES')
  assert.equal(sourceColumnPayload({ ...sourceColumnForm(), type: rows[1].type }).type, 'JSON')
})

test('column moves alter only displaced columns and replay to the target order', () => {
  const replay = (current, moves) => moves.reduce((order, move) => {
    const next = order.filter(name => name !== move.name)
    next.splice(move.previous === null ? 0 : next.indexOf(move.previous) + 1, 0, move.name)
    return next
  }, current)
  const current = ['id', 'a', 'b', 'c', 'd', 'e']
  for (const target of [['id', 'a', 'b', 'c', 'd', 'e'], ['e', 'id', 'a', 'b', 'c', 'd'], ['id', 'c', 'a', 'b', 'e', 'd'], ['e', 'd', 'c', 'b', 'a', 'id']]) {
    const moves = planColumnMoves(current, target)
    assert.deepEqual(replay(current, moves), target)
  }
  assert.deepEqual(planColumnMoves(current, current), [])
  assert.deepEqual(planColumnMoves(current, ['e', 'id', 'a', 'b', 'c', 'd']), [{ name: 'e', previous: null }])
  assert.deepEqual(planColumnMoves(current, ['id', 'b', 'c', 'd', 'e', 'a']), [{ name: 'a', previous: 'e' }])
})
