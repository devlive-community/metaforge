import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'
const source = await readFile(new URL('../src/model/database-export.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
const { defaultExportState, defaultFileName, exportPlan, exportedTables, configured, formatOptions, outputName, label } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

const objects = [{ name: 'chat', type: 'table', rows: 3, bytes: 1, comment: '' }, { name: 'logs', type: 'table', rows: 1, bytes: 1, comment: '' }]
const columns = { chat: [{ name: 'id', type: 'BIGINT' }, { name: 'title', type: 'VARCHAR' }], logs: [{ name: 'id', type: 'BIGINT' }] }

test('the file name carries the database and the day', () => {
  assert.equal(defaultFileName('app', new Date(2026, 9, 31)), 'app_export_20261031')
})

test('a whole-database export takes every object; a table selection only the checked ones', () => {
  const state = defaultExportState('app')
  state.selected = ['logs']
  assert.deepEqual(exportedTables(state, objects), ['logs'])
  state.objects = 'database'
  assert.deepEqual(exportedTables(state, objects), ['chat', 'logs'])
})

test('the plan sends every field as empty, keeps chosen fields and only complete filters', () => {
  const state = defaultExportState('app')
  state.selected = ['chat', 'logs']
  state.settings.chat = { fields: ['id'], scope: 'filtered', filters: [{ column: 'id', operator: '>=', value: '2' }, { column: 'title', operator: '=', value: '' }, { column: 'title', operator: 'IS NULL', value: '' }] }
  state.settings.logs = { fields: ['id'], scope: 'all', filters: [{ column: 'id', operator: '=', value: '1' }] }
  const plan = exportPlan(state, objects, columns)
  assert.deepEqual(plan.tables[0], { name: 'chat', fields: ['id'], filters: [{ column: 'id', operator: '>=', value: '2' }, { column: 'title', operator: 'IS NULL', value: '' }] })
  assert.deepEqual(plan.tables[1], { name: 'logs', fields: [], filters: [] })
  assert.equal(configured(state.settings.chat), true)
  assert.equal(configured({ fields: [], filters: [], scope: 'all' }), false)
})

test('format options start from the plugin defaults and keep chosen values', () => {
  const format = { name: 'JsonConvert', options: [{ name: 'indent', labels: { zh: '缩进', en: 'Indent' }, defaultValue: '2', choices: [] }, { name: 'root', labels: {}, defaultValue: 'array', choices: [] }] }
  assert.deepEqual(formatOptions(format, { indent: '4', stale: 'x' }), { indent: '4', root: 'array' })
  assert.equal(label({ zh: '缩进', en: 'Indent' }, 'zh-cn'), '缩进')
  assert.equal(label({ zh: '缩进', en: 'Indent' }, 'en'), 'Indent')
})

test('the output is a ZIP unless one uncompressed file is all there is', () => {
  const state = defaultExportState('app')
  state.fileName = 'app export'
  assert.equal(outputName(state, ['chat', 'logs'], 'json'), 'app_export.zip')
  state.content = 'data'
  state.compress = false
  assert.equal(outputName(state, ['chat'], 'json'), 'chat.json')
  state.compress = true
  assert.equal(outputName(state, ['chat'], 'json'), 'app_export.zip')
})
