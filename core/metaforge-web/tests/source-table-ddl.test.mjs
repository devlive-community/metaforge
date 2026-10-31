import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-table-ddl.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { DDL_ACTIONS, DDL_CARDS, DEFAULT_DDL_CARDS, ddlActionOf, ddlFileName, joinDdl, mapDdlHistory } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('the quick menu follows the prototype and every card action has its blocks', () => {
  assert.deepEqual(DDL_ACTIONS, ['create', 'structure', 'indexes', 'constraints', 'sample-data', 'alter', 'compare'])
  assert.deepEqual(DEFAULT_DDL_CARDS, ['create', 'structure'])
  assert.deepEqual(DDL_CARDS.alter.kinds, ['alter', 'alter-table'])
  assert.equal(ddlActionOf('alter-table'), 'alter')
  assert.equal(ddlActionOf('indexes'), 'indexes')
})

test('history rows map with fallbacks, and cards join their non-empty blocks', () => {
  assert.deepEqual(mapDdlHistory({ id: '3', kind: 'indexes', content: 'ALTER TABLE x;', username: 'admin', createTime: '2026-10-30 10:00:00' }),
    { id: 3, kind: 'indexes', content: 'ALTER TABLE x;', username: 'admin', createTime: '2026-10-30 10:00:00' })
  assert.deepEqual(mapDdlHistory({ id: 4 }), { id: 4, kind: 'create', content: '', username: '-', createTime: '-' })
  assert.equal(joinDdl(['a;\n', '  ', 'b;']), 'a;\n\nb;')
  assert.equal(ddlFileName('my table/x', 'sample-data'), 'my_table_x-sample-data.sql')
})
