import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const load = async name => {
  const source = await readFile(new URL(`../src/model/${name}.ts`, import.meta.url), 'utf8')
  return ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } }).outputText
}
// source-database imports source-table-drop; inline it so the module loads from a data URL.
const drop = await load('source-table-drop')
const database = (await load('source-database')).replace(/import \{ formatDateTime \} from '\.\/source-table-drop';?/, drop.replace(/export /g, ''))
const { mapDatabaseOverview, mapDatabaseTables, ringArcs, spaceSegments } = await import(`data:text/javascript;base64,${Buffer.from(database).toString('base64')}`)

test('the overview maps counts and byte sizes, and an empty database still reads', () => {
  const overview = mapDatabaseOverview({ object_name: 'datacap', object_charset: 'utf8mb4', object_collation: 'utf8mb4_0900_ai_ci', object_create_time: '2024-02-01T15:05:06',
    object_update_time: null, object_table_count: 38, object_column_count: '302', object_index_count: 10, object_event_count: 0, object_trigger_count: 1,
    object_foreign_key_count: 1, object_data_length: 618660, object_index_length: 167772, object_data_free: 0 }, 'datacap')
  assert.equal(overview.created, '2024-02-01 15:05:06')
  assert.equal(overview.updated, '')
  assert.equal(overview.columns, 302)
  assert.equal(overview.views, 0)
  assert.equal(mapDatabaseOverview(undefined, 'empty').tables, 0)
  assert.deepEqual(spaceSegments(overview).map(segment => segment.percent), [78.7, 21.3, 0])
})

test('tables fall back to the creation time and keep missing sizes empty', () => {
  assert.deepEqual(mapDatabaseTables([{ object_name: 'datacap_chat', object_engine: 'InnoDB', object_rows: 12860, object_data_length: null, object_index_length: 524288,
    object_create_time: '2024-02-01 15:05:06' }]), [{ name: 'datacap_chat', engine: 'InnoDB', rows: 12860, dataBytes: null, indexBytes: 524288, updated: '2024-02-01 15:05:06' }])
})

test('ring arcs skip empty segments, start where the previous one ended and leave a gap', () => {
  const arcs = ringArcs([{ key: 'data', bytes: 3, percent: 75 }, { key: 'index', bytes: 1, percent: 25 }, { key: 'other', bytes: 0, percent: 0 }], 100, 2)
  assert.deepEqual(arcs, [{ key: 'data', dash: '73 100', offset: -0 }, { key: 'index', dash: '23 100', offset: -75 }])
  assert.deepEqual(ringArcs([{ key: 'data', bytes: 5, percent: 100 }], 100, 2), [{ key: 'data', dash: '100 100', offset: -0 }])
})
