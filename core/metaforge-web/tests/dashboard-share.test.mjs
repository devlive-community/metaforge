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
