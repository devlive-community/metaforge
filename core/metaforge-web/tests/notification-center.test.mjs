import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { groupByDay, linkOf, sceneKey, toneOf, NOTIFICATION_CATEGORIES } from '../src/model/notification.ts'

const read = path => readFile(new URL('../src/' + path, import.meta.url), 'utf8')

const item = (fields) => ({ code: 'c', title: 't', content: '', scene: 'TEST', category: 'SYSTEM', read: false, createTime: '', entityExists: true, ...fields })

test('notifications split into today and earlier, keeping newest first', () => {
  const now = new Date(2026, 10, 5, 23, 0, 0)
  const groups = groupByDay([
    item({ code: 'a', createTime: '2026-11-05 23:23:40' }),
    item({ code: 'b', createTime: '2026-11-05 09:30:05' }),
    item({ code: 'c', createTime: '2026-11-04 15:36:24' })
  ], now)
  assert.deepEqual(groups.map(group => [group.key, group.items.map(value => value.code)]), [['today', ['a', 'b']], ['earlier', ['c']]])
  assert.deepEqual(groupByDay([item({ createTime: '2026-11-01 00:00:00' })], now).map(group => group.key), ['earlier'])
})

test('failures stand out and entities link only while they exist', () => {
  assert.equal(toneOf(item({ scene: 'DATASET_SYNC_FAILED', category: 'DATASET' })), 'danger')
  assert.equal(toneOf(item({ scene: 'DATASET_CREATED', category: 'DATASET' })), 'primary')
  // A dataset opens its detail page; a sync notice, including legacy SYNCDATA ones, opens its sync records.
  assert.equal(linkOf(item({ entityType: 'DATASET', entityCode: 'x' })), '/dataset/detail/x')
  assert.equal(linkOf(item({ scene: 'DATASET_SYNC_SUCCEEDED', entityType: 'DATASET', entityCode: 'x' })), '/dataset/runs/x')
  assert.equal(linkOf(item({ scene: 'DATASET_SYNC_FAILED', entityType: 'DATASET', entityCode: 'x' })), '/dataset/runs/x')
  assert.equal(linkOf(item({ scene: 'SYNCDATA', entityType: 'DATASET', entityCode: 'x' })), '/dataset/runs/x')
  assert.equal(linkOf(item({ scene: 'DATASET_SYNC_FAILED', entityType: 'DATASET', entityCode: 'x', entityExists: false })), '')
  assert.equal(linkOf(item({ entityType: 'DATASET', entityCode: 'x', entityExists: false })), '')
  assert.equal(linkOf(item({ entityType: 'USER', entityCode: 'x' })), '')
  assert.equal(sceneKey('SYNCDATA'), 'DATASET_SYNC_SUCCEEDED')
})

test('every category the center offers has a label in both languages', async () => {
  for (const locale of ['zh-cn', 'en']) {
    const messages = JSON.parse(await read(`i18n/locales/${ locale }.json`))
    for (const key of NOTIFICATION_CATEGORIES) {
      assert.ok(messages[`notify.category.${ key.toLowerCase() }`], `${ locale } ${ key }`)
    }
    for (const key of ['notify.center.today', 'notify.center.earlier', 'notify.center.tab.all', 'notify.center.tab.unread', 'notify.center.tab.read']) {
      assert.ok(messages[key], `${ locale } ${ key }`)
    }
  }
})

test('both headers use the shared bell, which talks only to the per-user notification api', async () => {
  for (const header of ['views/layouts/common/components/LayoutHeader.vue', 'views/layouts/metadata/components/WorkbenchHeader.vue']) {
    const source = await read(header)
    assert.match(source, /<NotificationBell\/>/, header)
    assert.doesNotMatch(source, /NotificationService/, header)
  }
  assert.doesNotMatch(await read('composables/useWorkbenchHeader.ts'), /NotificationService|fetchMessages/)
  const service = await read('services/notification.ts')
  assert.doesNotMatch(service, /BaseService/)
  for (const path of ['/list', '/summary', '/read`', '/read-all', '/read`']) {
    assert.ok(service.includes(path), path)
  }
})

test('removing a notification asks first, and the center page has its own route', async () => {
  const bell = await read('views/components/notify/NotificationBell.vue')
  assert.match(bell, /<MfModal :open="!!store\.pendingRemoval"/)
  const row = await read('views/components/notify/NotificationRow.vue')
  assert.match(row, /emit\('remove', item\)/)
  assert.doesNotMatch(row, /NotificationService/)
  const router = await read('router/default.ts')
  assert.match(router, /path: 'notifications'.*NotificationCenter\.vue/)
})
