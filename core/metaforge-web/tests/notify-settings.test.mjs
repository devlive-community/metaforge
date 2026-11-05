import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'

const read = path => readFile(new URL('../src/' + path, import.meta.url), 'utf8')

test('the notify page configures and subscribes only enabled channels', async () => {
  const page = await read('views/pages/admin/user/profile/components/NotifyForm.vue')
  // Settings forms render only while their channel is on.
  assert.match(page, /<MfForm v-if="settingOf\(channel\.type\)\.enabled"/)
  // Scene columns are the enabled channels only.
  assert.match(page, /const enabledChannels = computed\(\(\) => channels\.value\.filter\(channel => settingOf\(channel\.type\)\.enabled\)\)/)
  assert.match(page, /v-for="channel in enabledChannels"/)
  // A stored secret may stay empty and is never shown.
  assert.match(page, /required: field\.required && !\(field\.type === 'SECRET' && isConfigured\(type, field\)\)/)
  assert.doesNotMatch(page, /changeNotify|notifyConfigure/)
})

test('the notify page talks to the per-user notify endpoints', async () => {
  const service = await read('services/user.ts')
  for (const path of ['/notify/catalog', '/notify`', '/notify/test']) {
    assert.ok(service.includes(path), path)
  }
  assert.doesNotMatch(service, /change-notify/)
})

test('every scene and category the server offers has a label in both languages', async () => {
  const scenes = (await readFile(new URL('../../metaforge-service/src/main/java/org/devlive/metaforge/service/enums/NotificationScene.java', import.meta.url), 'utf8'))
  const names = [...scenes.matchAll(/^\s{4}([A-Z_]+)\(Category\.([A-Z]+)/gm)]
  assert.ok(names.length > 20)
  for (const locale of ['zh-cn', 'en']) {
    const messages = JSON.parse(await read(`i18n/locales/${ locale }.json`))
    for (const [, scene, category] of names) {
      assert.ok(messages[`notify.scene.${ scene }`], `${ locale } notify.scene.${ scene }`)
      assert.ok(messages[`notify.category.${ category.toLowerCase() }`], `${ locale } notify.category.${ category }`)
    }
  }
})

test('both headers show notifications through the shared scene-aware item', async () => {
  for (const header of ['views/layouts/common/components/LayoutHeader.vue', 'views/layouts/metadata/components/WorkbenchHeader.vue']) {
    const source = await read(header)
    assert.match(source, /<NotificationItem :item="item"\/>/)
    assert.doesNotMatch(source, /common\.\$\{ item\.type/)
  }
})
