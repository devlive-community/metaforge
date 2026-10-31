import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-table-drop.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { DEPENDENCY_KINDS, formatDateTime, groupDependencies, mapDropTableInfo } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('table facts add data and index sizes and normalise times', () => {
  const info = mapDropTableInfo({ object_engine: 'InnoDB', object_rows: '1256320', object_data_size: '100.25', object_index_size: 28.25,
    object_create_time: '2026-08-01T10:24:36.000+08:00', object_update_time: null, object_comment: '用户表' }, 'user', 'metaforge')
  assert.deepEqual(info, { name: 'user', database: 'metaforge', engine: 'InnoDB', rows: 1256320, sizeMb: 128.5, created: '2026-08-01 10:24:36', updated: '-', comment: '用户表' })
  assert.deepEqual(mapDropTableInfo(null, 't', 'd'), { name: 't', database: 'd', engine: '-', rows: null, sizeMb: null, created: '-', updated: '-', comment: '-' })
  assert.equal(formatDateTime('2026-10-28 15:21:07'), '2026-10-28 15:21:07')
})

test('dependencies group by kind with every kind present and unknown kinds dropped', () => {
  const groups = groupDependencies([
    { object_type: 'foreign_key', object_schema: 'app', object_name: 'fk_a', object_detail: 'post (user_id) -> (id)' },
    { object_type: 'view', object_schema: 'app', object_name: 'v_user' },
    { object_type: 'other', object_schema: 'app', object_name: 'x' },
  ])
  assert.deepEqual(Object.keys(groups), [...DEPENDENCY_KINDS])
  assert.equal(groups.foreign_key[0].detail, 'post (user_id) -> (id)')
  assert.equal(groups.view.length, 1)
  assert.equal(groups.routine.length, 0)
})
