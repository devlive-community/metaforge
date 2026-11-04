import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { formatPackageSize, hasNewerPluginVersion, mergeStorePlugins, readStoreCatalogue } = await sourceModule('../../metaforge-web/src/model/store.ts')

test('only a newer published version is offered as an update', () => {
  assert.equal(hasNewerPluginVersion('2026.0.0', '2025.10.0'), false)
  assert.equal(hasNewerPluginVersion('2026.0.0', '2026.0.0-SNAPSHOT'), false)
  assert.equal(hasNewerPluginVersion('2026.0.0-SNAPSHOT', '2026.0.0'), true)
  assert.equal(hasNewerPluginVersion('2.9.0', '2.10.0'), true)
  assert.equal(hasNewerPluginVersion('', '2.10.0'), false)
})

test('catalogue and runtime aliases merge once and only server references are shown', () => {
  const catalogue = readStoreCatalogue({ children: [
    { key: 'metaforge-plugin-mysql', label: 'MySQL', type: 'Connector', version: '2026.0.0', url: 'https://example.test/plugin.tar.gz', refs: [{ name: 'demo' }] },
    { key: 'metaforge-convert-json', label: 'JSON', type: 'Converter' },
  ] })
  const rows = mergeStorePlugins(catalogue, [
    { name: 'MySQL', key: 'mysql', type: 'CONNECTOR', version: '2025.0.0', state: 'STOPPED' },
    { name: 'Redis', key: 'redis', type: 'CONNECTOR', version: '2026.0.0', state: 'CREATED' },
  ], { MySQL: [{ kind: 'source', name: 'Warehouse' }] })
  assert.deepEqual(rows.map(row => row.type), ['Connector', 'Convert', 'Connector'])
  assert.equal(rows[0].registeredName, 'MySQL')
  assert.equal(rows[0].hasUpdate, true)
  assert.equal(rows[0].enabled, false)
  assert.deepEqual(rows[0].refs, [{ kind: 'source', name: 'Warehouse' }])
  assert.equal(rows[2].refs, undefined)
})

test('built catalogues carry size, checksum, sources and exact download counts', () => {
  const sha = 'a'.repeat(64)
  const catalogue = readStoreCatalogue({ children: [
    { key: 'metaforge-plugin-mysql', label: 'MySQL', version: '2026.0.0', size: 26568847, sha256: sha, requires: '2026.0.0',
      features: ['query', 'export'], sources: { maven: 'https://repo1.maven.org/x.tar.gz', cdn: 'https://cdn.example/x.tar.gz', evil: 'javascript:alert(1)' } },
    { key: 'metaforge-plugin-old', label: 'Old', size: '12 MB', sha256: 'not-a-sha' },
  ] })
  assert.equal(catalogue[0].sha256, sha)
  assert.deepEqual(Object.keys(catalogue[0].sources), ['maven', 'cdn'])
  assert.equal(catalogue[1].sha256, undefined)
  assert.equal(formatPackageSize(catalogue[0].size), '25.3 MB')
  assert.equal(formatPackageSize(catalogue[1].size), '12 MB')
  assert.equal(formatPackageSize(undefined), '--')
  assert.equal(formatPackageSize(512), '512 B')
  const rows = mergeStorePlugins(catalogue, [], {}, { 'metaforge-plugin-mysql': '9007199254740993' })
  assert.equal(rows[0].downloads, '9007199254740993')
  assert.equal(rows[1].downloads, undefined, 'unknown counts stay unknown, never zero')
})

test('installed plugins describe themselves when the catalogue lacks details', () => {
  const catalogue = readStoreCatalogue({ children: [{ key: 'metaforge-plugin-mysql', label: 'MySQL', description: 'From catalogue' }] })
  const rows = mergeStorePlugins(catalogue, [
    { name: 'MySql', key: 'metaforge-plugin-mysql', store: { label: 'Ignored', description: 'Ignored', logo: 'mysql.svg', features: ['query'] } },
    { name: 'Custom', key: 'acme-plugin-x', store: { label: 'Acme X', description: 'Self built', type: 'Connector', features: ['export'] } },
  ], {})
  assert.equal(rows[0].label, 'MySQL', 'catalogue values win')
  assert.equal(rows[0].description, 'From catalogue')
  assert.equal(rows[0].logo, 'mysql.svg', 'missing catalogue fields come from the plugin')
  assert.deepEqual(rows[0].features, ['query'])
  assert.equal(rows[1].label, 'Acme X')
  assert.equal(rows[1].description, 'Self built')
  assert.equal(rows[1].type, 'Connector')
  assert.deepEqual(rows[1].features, ['export'])
})
