import assert from 'node:assert/strict'
import test from 'node:test'
import { sourceModule } from '../../metaforge-ui/tests/component.mjs'

const { permits } = await sourceModule('../../metaforge-web/src/model/dashboard.ts')

test('screen permissions order VIEW < EDIT < PUBLISH < OWNER', () => {
  assert.equal(permits('VIEW', 'VIEW'), true)
  assert.equal(permits('VIEW', 'EDIT'), false)
  assert.equal(permits('EDIT', 'EDIT'), true)
  assert.equal(permits('EDIT', 'PUBLISH'), false)
  assert.equal(permits('PUBLISH', 'PUBLISH'), true)
  assert.equal(permits('PUBLISH', 'OWNER'), false)
  assert.equal(permits('OWNER', 'PUBLISH'), true)
  // Screens read before sharing existed carry no permission and belong to the user.
  assert.equal(permits(undefined, 'OWNER'), true)
})

const { sharedScreenPath, sharedReportPath, screenShareUrl, screenEmbedCode } = await sourceModule('../../metaforge-web/src/router/shared.ts')
const TOKEN = 'abcdefghijklmnopqrstuvwxyz012-_9'

test('public screen links open on their own page, apart from reports', () => {
  assert.deepEqual(sharedScreenPath(`/share/screen/${TOKEN}`), { token: TOKEN, embed: false })
  assert.deepEqual(sharedScreenPath(`/embed/screen/${TOKEN}/`), { token: TOKEN, embed: true })
  assert.equal(sharedScreenPath(`/share/report/${TOKEN}`), null)
  assert.equal(sharedReportPath(`/share/screen/${TOKEN}`), null)
  assert.equal(sharedScreenPath('/share/screen/short'), null)
  assert.equal(sharedScreenPath(`/screen/${TOKEN}`), null)
})

test('public screen addresses escape the title in the embed code', () => {
  assert.equal(screenShareUrl('https://bi.example.com', TOKEN), `https://bi.example.com/share/screen/${TOKEN}`)
  const code = screenEmbedCode('https://bi.example.com', TOKEN, 'Sales "Q1" <b>')
  assert.match(code, new RegExp(`src="https://bi.example.com/embed/screen/${TOKEN}"`))
  assert.match(code, /title="Sales &quot;Q1&quot; &lt;b>"/)
})
