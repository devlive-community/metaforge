import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/source-data-view.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { DEFAULT_TABLE_SETTINGS, INDEX_COLUMN, defaultColumnView, quickSorts, readColumnView, readDefaultSort, readTableSettings, sameSort } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const fields = [{ name: 'id', kind: 'number' }, { name: 'name', kind: 'text' }, { name: 'created_at', kind: 'datetime' }]

test('defaults put the pinned row number first and size columns by type', () => {
  const view = defaultColumnView(fields)
  assert.deepEqual(view.columns.map(column => [column.name, column.width, column.align, column.fixed]),
    [[INDEX_COLUMN, 60, 'center', true], ['id', 100, 'center', false], ['name', 160, 'left', false], ['created_at', 170, 'center', false]])
  assert.equal(view.stickyHeader, true)
})

test('saved views keep their order, drop removed columns and append new ones', () => {
  const saved = JSON.stringify({ columns: [{ name: 'name', visible: false, width: 9999, align: 'right', fixed: true }, { name: 'gone' }, { name: 'id', width: 'x' }], bordered: true })
  const view = readColumnView(saved, fields)
  assert.deepEqual(view.columns.map(column => column.name), ['name', 'id', INDEX_COLUMN, 'created_at'])
  assert.deepEqual(view.columns[0], { name: 'name', visible: false, width: 800, align: 'right', fixed: true })
  assert.equal(view.columns[1].width, 100)
  assert.equal(view.bordered, true)
  assert.deepEqual(readColumnView('not json', fields), defaultColumnView(fields))
})

test('quick sorts offer created, updated and key columns in both directions', () => {
  const columns = [{ name: 'uid', kind: 'number' }, { name: 'created_at', kind: 'datetime' }, { name: 'updated_at', kind: 'datetime' }, { name: 'created_by', kind: 'number' }]
  assert.deepEqual(quickSorts(columns, ['uid']).map(rule => `${rule.name} ${rule.order}`),
    ['created_at desc', 'created_at asc', 'updated_at desc', 'updated_at asc', 'uid desc', 'uid asc'])
  assert.deepEqual(quickSorts([{ name: 'name', kind: 'text' }], []), [])
  assert.ok(sameSort([{ name: 'a', order: 'asc' }], [{ name: 'a', order: 'asc' }]))
  assert.ok(!sameSort([{ name: 'a', order: 'asc' }], [{ name: 'a', order: 'desc' }]))
})

test('table settings fall back per field and only allow known page sizes', () => {
  assert.deepEqual(readTableSettings(null), DEFAULT_TABLE_SETTINGS)
  const settings = readTableSettings(JSON.stringify({ pageSize: 37, striped: true, exportFormat: 'xlsx', showActions: 'yes' }))
  assert.equal(settings.pageSize, 10)
  assert.equal(settings.striped, true)
  assert.equal(settings.exportFormat, 'csv')
  assert.equal(settings.showActions, true)
  assert.equal(readTableSettings(JSON.stringify({ pageSize: 50, rememberPageSize: true })).pageSize, 50)
})

test('a default sort must be one well-formed rule', () => {
  assert.deepEqual(readDefaultSort('{"name":"id","order":"desc"}'), { name: 'id', order: 'desc' })
  assert.equal(readDefaultSort('{"name":"id","order":"up"}'), null)
  assert.equal(readDefaultSort('oops'), null)
})
