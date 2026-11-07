import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'

const read = path => readFile(new URL('../src/' + path, import.meta.url), 'utf8')

test('the metadata tree reloads for another source or database, not for another table', async () => {
  const sidebar = await read('composables/useMetadataSidebar.ts')
  // route.params is rebuilt on every navigation; comparing a fresh array would always look changed.
  assert.match(sidebar, /watch\(\(\) => \[route\.params\.source, route\.params\.database, can\('table:list'\)\]\.join\(/)
  assert.match(sidebar, /watch\(treeVersion, onChangeDatabase\)/, 'structural changes reload it')
  assert.doesNotMatch(sidebar, /if \(!opened\) \{\s*onChangeDatabase\(\)/, 'closing a dialog does not reload it')
  const view = await read('views/layouts/metadata/components/MetadataSidebar.vue')
  assert.doesNotMatch(view, /default-expand-all/, 'tables stay collapsed; expanding one loads its columns')
})

test('every structural change reports to the tree', async () => {
  for (const path of ['views/pages/admin/source/components/TableCreate.vue', 'views/pages/admin/source/components/ColumnCreate.vue',
    'views/pages/admin/source/components/ColumnChange.vue', 'views/pages/admin/source/components/ColumnDrop.vue', 'composables/useTableDrop.ts']) {
    assert.match(await read(path), /treeChanged\(\)/, path)
  }
})
