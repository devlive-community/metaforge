import assert from 'node:assert/strict'
import { readFile, readdir } from 'node:fs/promises'
import test from 'node:test'
import ts from 'typescript'

// Every management capability is registered once in ManagementCapability.java, declared by plugins,
// checked by the server and the UI. This keeps the four from drifting apart.
const root = new URL('../../../', import.meta.url)
const read = path => readFile(new URL(path, root), 'utf8')
const registry = [...(await read('core/metaforge-spi/src/main/java/org/devlive/metaforge/spi/model/ManagementCapability.java'))
  .matchAll(/[A-Z_]+\("([a-z-]+:[a-z-]+)"/g)].map(match => match[1])
const KEY = /['"]((?:query|namespace|key|object|database|engine|datatype|table|column|index|foreign-key|trigger|data|tab|view):[a-z-]+)['"]/g

async function files(dir, extensions) {
  const found = []
  for (const entry of await readdir(new URL(dir, root), { withFileTypes: true })) {
    const path = dir + entry.name
    if (entry.isDirectory()) found.push(...await files(path + '/', extensions))
    else if (extensions.some(extension => entry.name.endsWith(extension))) found.push(path)
  }
  return found
}
async function keysIn(dir, extensions) {
  const keys = new Map()
  for (const path of await files(dir, extensions)) {
    for (const match of (await read(path)).matchAll(KEY)) keys.set(match[1], path)
  }
  return keys
}

const source = await readFile(new URL('../src/model/source-manage.ts', import.meta.url), 'utf8')
const { outputText } = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.ESNext } })
const { sidebarActions, hasSidebarActions, sourceTabCapability } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)

test('the UI and the server check only registered capabilities', async () => {
  for (const [dir, extensions] of [['core/metaforge-web/src/', ['.ts', '.vue']], ['core/metaforge-service/src/main/', ['.java']], ['core/metaforge-server/src/main/', ['.java']]]) {
    for (const [key, path] of await keysIn(dir, extensions)) assert.ok(registry.includes(key), `${key} in ${path} is not registered in ManagementCapability`)
  }
})

test('every registered capability controls something in the UI', async () => {
  const used = new Set((await keysIn('core/metaforge-web/src/', ['.ts', '.vue'])).keys())
  // Tabs are checked through sourceTabCapability(tab.key) for the keys of the tab lists.
  const content = await read('core/metaforge-web/src/composables/useMetadataContent.ts')
  for (const match of content.matchAll(/key: '([A-Za-z]+)'/g)) used.add(sourceTabCapability(match[1]))
  for (const key of registry) assert.ok(used.has(key), `${key} is registered but no page, tab, dialog, drawer or button checks it`)
})

test('the side tree menu offers only declared actions and opens only when it has one', () => {
  const none = () => false
  assert.equal(hasSidebarActions(sidebarActions({ table: true, column: false }, none)), false)
  const readOnly = new Set(['table:list', 'column:read'])
  assert.equal(hasSidebarActions(sidebarActions({ table: true, column: false }, key => readOnly.has(key))), false)
  const mysql = new Set(['table:create', 'table:export', 'table:drop', 'column:create', 'column:modify'])
  assert.deepEqual(sidebarActions({ table: true, column: false }, key => mysql.has(key)),
    { newTable: true, newColumn: true, exportData: true, truncateTable: false, dropTable: true, changeColumn: false, dropColumn: false })
  assert.deepEqual(sidebarActions({ table: false, column: true }, key => mysql.has(key)),
    { newTable: false, newColumn: true, exportData: false, truncateTable: false, dropTable: false, changeColumn: true, dropColumn: false })
})
