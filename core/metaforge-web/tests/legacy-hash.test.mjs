import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { legacyHashTarget, upgradeLegacyHashRoute } = await sourceModule('../../metaforge-web/src/router/legacy-hash.ts')

test('old hash routes become paths, plain anchors stay', () => {
  assert.equal(legacyHashTarget('#/query?x=1'), '/query?x=1')
  assert.equal(legacyHashTarget('#/auth/oauth?ticket=a#b'), '/auth/oauth?ticket=a#b')
  assert.equal(legacyHashTarget('#section'), null)
  assert.equal(legacyHashTarget(''), null)
  const calls = []
  const history = { state: { s: 1 }, replaceState: (state, _title, url) => calls.push([state, url]) }
  upgradeLegacyHashRoute({ hash: '#/admin/user' }, history)
  upgradeLegacyHashRoute({ hash: '' }, history)
  assert.deepEqual(calls, [[{ s: 1 }, '/admin/user']])
})

test('history routing, with the upgrade running before the router module', async () => {
  const main = await readFile(new URL('../src/main.ts', import.meta.url), 'utf8')
  assert.equal([...main.matchAll(/^import .*$/gm)][0][0], "import './legacy-hash-entry'")
  const router = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
  assert.match(router, /createWebHistory\(\)/)
  assert.doesNotMatch(router, /createWebHashHistory/)
})
